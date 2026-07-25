package com.mj.yaja.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.yaja.R
import com.mj.yaja.data.CardSchedule
import com.mj.yaja.data.RecurringTaskEndMode
import com.mj.yaja.data.RecurringTaskItemType
import com.mj.yaja.data.RecurringTaskFrequency
import com.mj.yaja.data.RecurringTaskItem
import com.mj.yaja.data.RecurringTaskScheduleMode
import com.mj.yaja.data.NavigationChromeMode
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTasksScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    val items by viewModel.recurringTasks.collectAsStateWithLifecycle()
    val sortedItems = remember(items) {
        items.sortedWith(compareBy({ !it.isActive }, { it.title.lowercase() }))
    }
    val showBottomBar by viewModel.showBottomBar.collectAsStateWithLifecycle()
    val navigationChromeMode by viewModel.navigationChromeMode.collectAsStateWithLifecycle()
    val showBottomPanelLabels by viewModel.showBottomPanelLabels.collectAsStateWithLifecycle()
    val fabPlacement by viewModel.fabPlacement.collectAsStateWithLifecycle()
    var editorItem by remember { mutableStateOf<RecurringTaskItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editorItemType by remember { mutableStateOf(RecurringTaskItemType.TASK) }
    var deleteConfirmItem by remember { mutableStateOf<RecurringTaskItem?>(null) }
    val bottomChromePadding = remember(showBottomBar, navigationChromeMode, showBottomPanelLabels) {
        if (showBottomBar) {
            when (navigationChromeMode) {
                NavigationChromeMode.EXPRESSIVE_PANEL -> if (showBottomPanelLabels) 116.dp else 100.dp
                NavigationChromeMode.FLOATING_BAR -> 104.dp
            }
        } else {
            24.dp
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            when {
                                showEditor && editorItem == null -> {
                                    if (editorItemType == RecurringTaskItemType.EVENT)
                                        R.string.recurring_task_add_event
                                    else R.string.recurring_task_add
                                }
                                showEditor -> {
                                    if (editorItemType == RecurringTaskItemType.EVENT)
                                        R.string.recurring_task_edit_event
                                    else R.string.recurring_task_edit
                                }
                                else -> R.string.recurring_task_title
                            }
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (showEditor) {
                                showEditor = false
                                editorItem = null
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            if (!showEditor) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = bottomChromePadding)
                ) {
                    FloatingActionButton(
                        onClick = {
                            editorItem = null
                            editorItemType = RecurringTaskItemType.TASK
                            showEditor = true
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.align(fabPlacement.fabAlignment())
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.recurring_task_add)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        AnimatedContent(
            targetState = showEditor,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut(animationSpec = tween(300)))
                } else {
                    (slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(300)))
                }
            },
            label = "editor_transition"
        ) { editorActive ->
            if (editorActive) {
                  RecurringTaskEditorPage(
                     item = editorItem,
                     paddingValues = paddingValues,
                     bottomChromePadding = bottomChromePadding,
                     currentItemType = editorItemType,
                     onItemTypeChange = { editorItemType = it },
                     computeUpcomingDates = { viewModel.getRecurringTaskUpcomingDates(it, 5) },
                    onDismiss = {
                        showEditor = false
                        editorItem = null
                    },
                    onSave = { title, description, itemType, scheduleMode, frequency, dueDay, dueWeekday, leadDays, endMode, endDate, endCount, startDate, startMonth, startTime ->
                        viewModel.upsertRecurringTask(
                            id = editorItem?.id,
                            title = title,
                            description = description,
                            isActive = editorItem?.isActive ?: true,
                            itemType = itemType,
                            scheduleMode = scheduleMode,
                            frequency = frequency,
                            dueDayOfMonth = dueDay,
                            dueDayOfWeek = dueWeekday,
                            leadDays = leadDays,
                            endMode = endMode,
                            endDate = endDate,
                            endCount = endCount,
                            anchorDate = startDate,
                            startMonth = startMonth,
                            startTime = startTime
                        )
                        showEditor = false
                        editorItem = null
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 12.dp,
                            end = 16.dp,
                            bottom = bottomChromePadding + 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (items.isEmpty()) {
                            item("intro") {
                                Surface(
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.recurring_task_intro_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = stringResource(R.string.recurring_task_intro_body),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        if (items.isEmpty()) {
                            item("empty") {
                                Surface(
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Text(
                                        text = stringResource(R.string.recurring_task_empty),
                                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(
                                sortedItems,
                                key = { it.id }
                            ) { item ->
                                val schedule = remember(item) {
                                    viewModel.getRecurringTaskCardSchedule(item)
                                }
                                Box(modifier = Modifier.animateItem()) {
                                    RecurringTaskCard(
                                        item = item,
                                        nextDate = schedule.nextDate,
                                        endDate = schedule.endDate,
                                        remaining = schedule.remaining,
                                         onEdit = {
                                             editorItem = item
                                             editorItemType = item.itemType
                                             showEditor = true
                                         },
                                        onDelete = { deleteConfirmItem = item },
                                        onToggleActive = { active -> viewModel.toggleRecurringTaskActive(item.id, active) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteConfirmItem?.let { pending ->
        AlertDialog(
            onDismissRequest = { deleteConfirmItem = null },
            title = {
                Text(
                    stringResource(
                        if (pending.itemType == RecurringTaskItemType.EVENT)
                            R.string.recurring_task_delete_confirm_title_event
                        else R.string.recurring_task_delete_confirm_title
                    )
                )
            },
            text = {
                Text(stringResource(R.string.recurring_task_delete_confirm_body, pending.title))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecurringTask(pending.id)
                    deleteConfirmItem = null
                }) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmItem = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurringTaskCard(
    item: RecurringTaskItem,
    nextDate: LocalDate?,
    endDate: LocalDate?,
    remaining: Int?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    val animatedColor by animateColorAsState(
        targetValue = if (item.isActive) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.54f),
        animationSpec = tween(durationMillis = 250),
        label = "card_color"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (item.isActive) 1f else 0.62f,
        animationSpec = tween(durationMillis = 250),
        label = "content_alpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = animatedColor
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Title, Optional Deactivated Badge, and Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!item.isActive) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Text(
                                text = stringResource(R.string.recurring_task_status_deactivated),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Switch(
                    checked = item.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }

            // Description if present
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }

            // Metadata info
            Text(
                text = recurringTaskSummary(item),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )

            // Schedule chips: next due date + end condition
            val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
            val showNextChip = nextDate != null && item.isActive
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Type chip: Task vs Event
                RecurringTaskInfoChip(
                    icon = if (item.itemType == RecurringTaskItemType.EVENT)
                        Icons.Rounded.Event else Icons.Rounded.TaskAlt,
                    text = stringResource(
                        if (item.itemType == RecurringTaskItemType.EVENT)
                            R.string.recurring_task_type_event
                        else R.string.recurring_task_type_task
                    ),
                    container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                    alpha = contentAlpha
                )
                val formattedTime = remember(item.startTime) {
                    item.startTime?.let { timeStr ->
                        runCatching {
                            LocalTime.parse(timeStr).format(DateTimeFormatter.ofPattern("hh:mm a"))
                        }.getOrNull()
                    }
                }
                if (formattedTime != null) {
                    RecurringTaskInfoChip(
                        icon = Icons.Rounded.AccessTime,
                        text = formattedTime,
                        container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                        alpha = contentAlpha
                    )
                }
                if (showNextChip) {
                    RecurringTaskInfoChip(
                        icon = Icons.Rounded.Event,
                        text = stringResource(
                            R.string.recurring_task_next_date,
                            nextDate!!.format(dateFormatter)
                        ),
                        container = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                        alpha = contentAlpha
                    )
                }
                // End condition: ∞ for never, date for on-date, live countdown for occurrences
                when (item.endMode) {
                    RecurringTaskEndMode.NEVER -> {
                        RecurringTaskInfoChip(
                            icon = Icons.Rounded.AllInclusive,
                            text = "",
                            container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            content = MaterialTheme.colorScheme.onSecondaryContainer,
                            alpha = contentAlpha
                        )
                    }
                    RecurringTaskEndMode.ON_DATE -> {
                        if (endDate != null) {
                            RecurringTaskInfoChip(
                                icon = Icons.Rounded.EventBusy,
                                text = stringResource(
                                    R.string.recurring_task_ends_on,
                                    endDate.format(dateFormatter)
                                ),
                                container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                content = MaterialTheme.colorScheme.onSecondaryContainer,
                                alpha = contentAlpha
                            )
                        }
                    }
                    RecurringTaskEndMode.AFTER_OCCURRENCES -> {
                        if (remaining != null) {
                            if (remaining == 0) {
                                RecurringTaskInfoChip(
                                    icon = Icons.Rounded.EventBusy,
                                    text = stringResource(R.string.recurring_task_ended),
                                    container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                                    content = MaterialTheme.colorScheme.onErrorContainer,
                                    alpha = contentAlpha
                                )
                            } else {
                                RecurringTaskInfoChip(
                                    icon = Icons.Rounded.EventBusy,
                                    text = stringResource(
                                        R.string.recurring_task_ends_remaining,
                                        remaining
                                    ),
                                    container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                                    alpha = contentAlpha
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Buttons Row: Full width pill buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_edit),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.action_delete),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringTaskInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    alpha: Float = 1f
) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = container.copy(alpha = container.alpha * alpha),
        contentColor = content.copy(alpha = alpha)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp)
            )
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringTaskEditorPage(
    item: RecurringTaskItem?,
    paddingValues: PaddingValues,
    bottomChromePadding: Dp,
    currentItemType: RecurringTaskItemType,
    onItemTypeChange: (RecurringTaskItemType) -> Unit,
    computeUpcomingDates: (RecurringTaskItem) -> List<LocalDate>,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String, // description
        RecurringTaskItemType,
        RecurringTaskScheduleMode,
        RecurringTaskFrequency,
        Int?,
        Int?,
        Int,
        RecurringTaskEndMode,
        LocalDate?,
        Int?,
        LocalDate,
        YearMonth,
        String?
    ) -> Unit
) {
    var title by remember(item?.id) { mutableStateOf(item?.title.orEmpty()) }
    var description by remember(item?.id) { mutableStateOf(item?.description.orEmpty()) }
    val itemType = currentItemType
    var scheduleMode by remember(item?.id) {
        mutableStateOf(item?.scheduleMode ?: RecurringTaskScheduleMode.DAY_OF_MONTH)
    }
    var frequency by remember(item?.id) {
        mutableStateOf(item?.frequency ?: RecurringTaskFrequency.MONTHLY)
    }
    var revealDayText by remember(item?.id) { mutableStateOf((item?.leadDays ?: 0).toString()) }
    var endMode by remember(item?.id) {
        mutableStateOf(item?.endMode ?: RecurringTaskEndMode.NEVER)
    }
    var endDate by remember(item?.id) {
        mutableStateOf(item?.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() })
    }
    var endCountText by remember(item?.id) {
        mutableStateOf((item?.endCount ?: 12).toString())
    }
    var startDate by remember(item?.id) {
        mutableStateOf(
            item?.anchorDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        )
    }
    var startTime by remember(item?.id) {
        mutableStateOf(
            item?.startTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }

    val previewDueDay = startDate.dayOfMonth
    val previewDueWeekday = startDate.dayOfWeek.value
    val previewLeadDays = revealDayText.toIntOrNull()?.coerceIn(0, 30) ?: 0
    val previewEndCount = endCountText.toIntOrNull()?.coerceIn(1, 999)
    val previewDates = remember(scheduleMode, frequency, previewDueDay, previewDueWeekday, endMode, endDate, previewEndCount, startDate) {
        val tempItem = RecurringTaskItem(
            id = "preview",
            title = "",
            scheduleMode = scheduleMode,
            frequency = frequency,
            dueDayOfMonth = previewDueDay.takeIf { scheduleMode == RecurringTaskScheduleMode.DAY_OF_MONTH },
            dueDayOfWeek = previewDueWeekday.takeIf { scheduleMode == RecurringTaskScheduleMode.DAY_OF_WEEK },
            leadDays = 0,
            anchorDate = startDate.toString(),
            startMonth = YearMonth.from(startDate).toString(),
            startTime = startTime?.format(DateTimeFormatter.ofPattern("HH:mm")),
            endMode = endMode,
            endDate = endDate?.toString().takeIf { endMode == RecurringTaskEndMode.ON_DATE },
            endCount = previewEndCount.takeIf { endMode == RecurringTaskEndMode.AFTER_OCCURRENCES }
        )
        computeUpcomingDates(tempItem)
    }

    fun save() {
        val parsedLeadDays = revealDayText.toIntOrNull()?.coerceIn(0, 30) ?: 0
        val parsedEndCount = endCountText.toIntOrNull()?.coerceIn(1, 999) ?: 1
        onSave(
            title,
            description,
            itemType,
            scheduleMode,
            frequency,
            startDate.dayOfMonth.takeIf { scheduleMode == RecurringTaskScheduleMode.DAY_OF_MONTH },
            startDate.dayOfWeek.value.takeIf { scheduleMode == RecurringTaskScheduleMode.DAY_OF_WEEK },
            parsedLeadDays,
            endMode,
            endDate.takeIf { endMode == RecurringTaskEndMode.ON_DATE },
            parsedEndCount.takeIf { endMode == RecurringTaskEndMode.AFTER_OCCURRENCES },
            startDate,
            YearMonth.from(startDate),
            startTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = bottomChromePadding + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item("task") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.recurring_task_editor_task_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.recurring_task_type_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RecurringTaskSegmentedSelector(
                        options = listOf(
                            RecurringTaskItemType.TASK to stringResource(R.string.recurring_task_type_task),
                            RecurringTaskItemType.EVENT to stringResource(R.string.recurring_task_type_event)
                        ),
                        selected = itemType,
                        onSelected = onItemTypeChange
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        label = {
                            Text(
                                stringResource(
                                    if (itemType == RecurringTaskItemType.EVENT)
                                        R.string.recurring_task_field_title_event
                                    else R.string.recurring_task_field_title
                                )
                            )
                        },
                        placeholder = {
                            Text(
                                stringResource(
                                    if (itemType == RecurringTaskItemType.EVENT)
                                        R.string.recurring_task_field_title_event_hint
                                    else R.string.recurring_task_field_title_hint
                                )
                            )
                        }
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = false,
                        maxLines = 3,
                        label = { Text(stringResource(R.string.recurring_task_field_description)) },
                        placeholder = {
                            Text(stringResource(R.string.recurring_task_field_description_hint))
                        }
                    )
                }
            }
        }

        item("schedule_mode") {
            RecurringTaskEditorCard {
                RecurringTaskEditorSectionHeader(
                    step = 1,
                    title = stringResource(R.string.recurring_task_field_schedule_mode),
                    description = stringResource(R.string.recurring_task_schedule_mode_help)
                )
                Text(
                    text = stringResource(R.string.recurring_task_select_schedule_type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RecurringTaskSegmentedSelector(
                    options = recurringTaskModeOptions().map { option ->
                        option.first to stringResource(option.titleRes)
                    },
                    selected = scheduleMode,
                    onSelected = { selectedMode ->
                        scheduleMode = selectedMode
                        frequency = defaultFrequencyFor(selectedMode)
                    }
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    AnimatedContent(
                        targetState = scheduleMode,
                        transitionSpec = { recurringTaskContentTransition() },
                        label = "schedule_mode_description"
                    ) { mode ->
                        Text(
                            text = stringResource(
                                recurringTaskModeOptions()
                                    .first { it.first == mode }
                                    .descriptionRes
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item("frequency") {
            RecurringTaskEditorCard {
                RecurringTaskEditorSectionHeader(
                    step = 2,
                    title = stringResource(R.string.recurring_task_field_frequency),
                    description = stringResource(
                        if (itemType == RecurringTaskItemType.EVENT)
                            R.string.recurring_task_frequency_help_event
                        else R.string.recurring_task_frequency_help
                    )
                )
                Text(
                    text = stringResource(R.string.recurring_task_select_frequency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RecurringTaskSegmentedSelector(
                    options = allowedFrequencies(scheduleMode).map { option ->
                        option to stringResource(frequencyLabelRes(option))
                    },
                    selected = frequency,
                    onSelected = { frequency = it }
                )
            }
        }

        item("start_schedule") {
            RecurringTaskEditorCard {
                RecurringTaskEditorSectionHeader(
                    step = 3,
                    title = stringResource(R.string.recurring_task_start_section),
                    description = stringResource(R.string.recurring_task_start_help)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val startFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Event,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                        Text(text = startDate.format(startFormatter))
                    }

                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                        val formattedTime = remember(startTime) {
                            startTime?.format(DateTimeFormatter.ofPattern("hh:mm a"))
                        }
                        Text(text = formattedTime ?: stringResource(R.string.addentry_set_time))
                    }
                }
            }
        }

        item("preview") {
            RecurringTaskExecutionPreviewCard(
                description = recurringTaskExecutionDescription(
                    itemType = itemType,
                    scheduleMode = scheduleMode,
                    frequency = frequency,
                    dueDayOfMonth = previewDueDay,
                    dueDayOfWeek = previewDueWeekday,
                    revealDayOfMonth = previewLeadDays
                ),
                upcomingDates = previewDates,
                startTime = startTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
            )
        }

        item("details") {
            RecurringTaskEditorCard {
                RecurringTaskEditorSectionHeader(
                    step = 4,
                    title = stringResource(R.string.recurring_task_schedule_details),
                    description = stringResource(R.string.recurring_task_details_boundary_help)
                )
                OutlinedTextField(
                    value = revealDayText,
                    onValueChange = { value ->
                        if (value.all(Char::isDigit) && value.length <= 2) {
                            revealDayText = value
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    label = {
                        Text(stringResource(R.string.recurring_task_field_reveal_day))
                    },
                    supportingText = {
                        Text(stringResource(R.string.recurring_task_reveal_day_help))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        item("end") {
            RecurringTaskEditorCard {
                RecurringTaskEditorSectionHeader(
                    step = 5,
                    title = stringResource(R.string.recurring_task_end_section),
                    description = stringResource(
                        if (itemType == RecurringTaskItemType.EVENT)
                            R.string.recurring_task_end_help_event
                        else R.string.recurring_task_end_help
                    )
                )
                RecurringTaskSegmentedSelector(
                    options = recurringTaskEndModeOptions().map { option ->
                        option.first to stringResource(option.second)
                    },
                    selected = endMode,
                    onSelected = { endMode = it }
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    AnimatedContent(
                        targetState = endMode,
                        transitionSpec = { recurringTaskContentTransition() },
                        label = "end_mode_content"
                    ) { mode ->
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (mode) {
                                RecurringTaskEndMode.NEVER -> {
                                    Text(
                                        text = stringResource(R.string.recurring_task_end_never_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RecurringTaskEndMode.ON_DATE -> {
                                    val endFormatter = remember {
                                        DateTimeFormatter.ofPattern("d MMM yyyy")
                                    }
                                    OutlinedButton(
                                        onClick = { showDatePicker = true },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                        shape = MaterialTheme.shapes.large
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Event,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        androidx.compose.foundation.layout.Spacer(
                                            modifier = Modifier.width(8.dp)
                                        )
                                        Text(
                                            text = endDate?.format(endFormatter)
                                                ?: stringResource(R.string.recurring_task_end_date_select)
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.recurring_task_end_date_help),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RecurringTaskEndMode.AFTER_OCCURRENCES -> {
                                    OutlinedTextField(
                                        value = endCountText,
                                        onValueChange = { value ->
                                            if (value.all(Char::isDigit) && value.length <= 3) {
                                                endCountText = value
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        singleLine = true,
                                        label = {
                                            Text(stringResource(R.string.recurring_task_end_count_label))
                                        },
                                        supportingText = {
                                            Text(
                                                stringResource(
                                                    if (itemType == RecurringTaskItemType.EVENT)
                                                        R.string.recurring_task_end_count_help_event
                                                    else R.string.recurring_task_end_count_help
                                                )
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    if (showDatePicker) {
        val currentEndDate = endDate
        val initialDate = if (currentEndDate == null || currentEndDate.isBefore(startDate)) {
            startDate.plusMonths(1)
        } else {
            currentEndDate
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(ZoneOffset.UTC).toLocalDate().isBefore(startDate)

                override fun isSelectableYear(year: Int): Boolean = year >= startDate.year
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            endDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showStartDatePicker) {
        val today = remember { LocalDate.now() }
        val startPickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startPickerState.selectedDateMillis?.let { millis ->
                            startDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = startPickerState)
        }
    }

    if (showStartTimePicker) {
        val context = LocalContext.current
        val is24Hour = remember(context) {
            android.text.format.DateFormat.is24HourFormat(context)
        }
        val initialTime = startTime ?: LocalTime.now()
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = is24Hour
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            startTime = null
                            showStartTimePicker = false
                        }
                    ) {
                        Text(stringResource(R.string.action_clear))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showStartTimePicker = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            startTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showStartTimePicker = false
                        }
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            },
            title = { Text(stringResource(R.string.addentry_pick_time_title)) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    // Sticky Save/Cancel bar
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 12.dp + bottomChromePadding
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            val currentEndDate = endDate
            val isEndDateInvalid = endMode == RecurringTaskEndMode.ON_DATE && (currentEndDate == null || currentEndDate.isBefore(startDate))
            Button(
                onClick = ::save,
                enabled = title.isNotBlank() && !isEndDateInvalid,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                )
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
    } // end Box
}

@Composable
private fun RecurringTaskEditorCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun RecurringTaskEditorSectionHeader(
    step: Int,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Text(
                text = step.toString(),
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecurringTaskExecutionPreviewCard(
    description: String,
    upcomingDates: List<LocalDate> = emptyList(),
    startTime: String? = null
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.recurring_task_execution_title).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                AnimatedContent(
                    targetState = description,
                    transitionSpec = { recurringTaskContentTransition() },
                    label = "recurring_task_preview_description"
                ) { previewText ->
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (upcomingDates.isNotEmpty()) {
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.height(4.dp)
                    )
                    Text(
                        text = stringResource(R.string.recurring_task_upcoming).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    val formattedTime = remember(startTime) {
                        startTime?.let { timeStr ->
                            runCatching {
                                LocalTime.parse(timeStr).format(DateTimeFormatter.ofPattern("hh:mm a"))
                            }.getOrNull()
                        }
                    }
                    upcomingDates.forEach { date ->
                        val dateText = date.format(dateFormatter)
                        val text = if (formattedTime != null) {
                            "• $dateText at $formattedTime"
                        } else {
                            "• $dateText"
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun recurringTaskContentTransition() =
    (fadeIn(
        animationSpec = tween(durationMillis = 170, delayMillis = 60, easing = FastOutSlowInEasing)
    ) + scaleIn(
        initialScale = 0.985f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
    )).togetherWith(
        fadeOut(animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)) +
            scaleOut(
                targetScale = 0.985f,
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
            )
    )

@Composable
private fun <T> RecurringTaskSegmentedSelector(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    if (options.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.54f)
    ) {
        BoxWithConstraints(modifier = Modifier.padding(4.dp)) {
            val gap = 4.dp
            val controlHeight = 42.dp
            val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
            val slotWidth = (maxWidth - gap * (options.size - 1)) / options.size
            val pillOffset by animateDpAsState(
                targetValue = (slotWidth + gap) * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "recurring_task_segment_offset"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(controlHeight)
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(slotWidth)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(999.dp)
                        )
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    options.forEach { (value, label) ->
                        val isSelected = value == selected
                        val interactionSource = remember { MutableInteractionSource() }
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                            label = "recurring_task_segment_text_color"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onSelected(value) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringTaskOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.66f)
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "chip_text"
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

private data class RecurringTaskModeOption(
    val first: RecurringTaskScheduleMode,
    val titleRes: Int,
    val descriptionRes: Int
)

private fun recurringTaskModeOptions(): List<RecurringTaskModeOption> =
    listOf(
        RecurringTaskModeOption(
            RecurringTaskScheduleMode.DAY_OF_MONTH,
            R.string.recurring_task_mode_day_of_month,
            R.string.recurring_task_mode_day_of_month_desc
        ),
        RecurringTaskModeOption(
            RecurringTaskScheduleMode.DAY_OF_WEEK,
            R.string.recurring_task_mode_day_of_week,
            R.string.recurring_task_mode_day_of_week_desc
        ),
        RecurringTaskModeOption(
            RecurringTaskScheduleMode.FIRST_DAY_OF_MONTH,
            R.string.recurring_task_mode_first_day,
            R.string.recurring_task_mode_first_day_desc
        ),
        RecurringTaskModeOption(
            RecurringTaskScheduleMode.LAST_DAY_OF_MONTH,
            R.string.recurring_task_mode_last_day,
            R.string.recurring_task_mode_last_day_desc
        )
    )

private fun recurringTaskEndModeOptions(): List<Pair<RecurringTaskEndMode, Int>> =
    listOf(
        RecurringTaskEndMode.NEVER to R.string.recurring_task_end_never,
        RecurringTaskEndMode.ON_DATE to R.string.recurring_task_end_on_date,
        RecurringTaskEndMode.AFTER_OCCURRENCES to R.string.recurring_task_end_after
    )

private fun allowedFrequencies(mode: RecurringTaskScheduleMode): List<RecurringTaskFrequency> =
    when (mode) {
        RecurringTaskScheduleMode.DAY_OF_MONTH,
        RecurringTaskScheduleMode.FIRST_DAY_OF_MONTH,
        RecurringTaskScheduleMode.LAST_DAY_OF_MONTH ->
            listOf(
                RecurringTaskFrequency.MONTHLY,
                RecurringTaskFrequency.QUARTERLY,
                RecurringTaskFrequency.HALF_YEARLY,
                RecurringTaskFrequency.ANNUAL
            )
        RecurringTaskScheduleMode.DAY_OF_WEEK ->
            listOf(
                RecurringTaskFrequency.WEEKLY,
                RecurringTaskFrequency.BIWEEKLY,
                RecurringTaskFrequency.MONTHLY
            )
    }

private fun defaultFrequencyFor(mode: RecurringTaskScheduleMode): RecurringTaskFrequency =
    allowedFrequencies(mode).first()

private fun frequencyLabelRes(frequency: RecurringTaskFrequency): Int =
    when (frequency) {
        RecurringTaskFrequency.WEEKLY -> R.string.recurring_task_frequency_weekly
        RecurringTaskFrequency.BIWEEKLY -> R.string.recurring_task_frequency_biweekly
        RecurringTaskFrequency.MONTHLY -> R.string.recurring_task_frequency_monthly
        RecurringTaskFrequency.QUARTERLY -> R.string.recurring_task_frequency_quarterly
        RecurringTaskFrequency.HALF_YEARLY -> R.string.recurring_task_frequency_half_yearly
        RecurringTaskFrequency.ANNUAL -> R.string.recurring_task_frequency_annual
    }

private fun weekdayLabels(): List<Int> =
    listOf(
        R.string.calendar_weekday_mon_short,
        R.string.calendar_weekday_tue_short,
        R.string.calendar_weekday_wed_short,
        R.string.calendar_weekday_thu_short,
        R.string.calendar_weekday_fri_short,
        R.string.calendar_weekday_sat_short,
        R.string.calendar_weekday_sun_short
    )

@Composable
private fun recurringTaskSummary(item: RecurringTaskItem): String {
    val frequency = stringResource(frequencyLabelRes(item.frequency))
    return when (item.scheduleMode) {
        RecurringTaskScheduleMode.DAY_OF_MONTH ->
            stringResource(
                R.string.recurring_task_summary_day_of_month,
                item.dueDayOfMonth ?: 1,
                frequency,
                item.leadDays
            )
        RecurringTaskScheduleMode.DAY_OF_WEEK -> {
            val weekdayRes = weekdayLabels()[(item.dueDayOfWeek ?: 1) - 1]
            stringResource(
                R.string.recurring_task_summary_day_of_week,
                stringResource(weekdayRes),
                frequency
            )
        }
        RecurringTaskScheduleMode.FIRST_DAY_OF_MONTH ->
            stringResource(R.string.recurring_task_summary_first_day, frequency, item.leadDays)
        RecurringTaskScheduleMode.LAST_DAY_OF_MONTH ->
            stringResource(R.string.recurring_task_summary_last_day, frequency, item.leadDays)
    }
}

@Composable
private fun recurringTaskExecutionDescription(
    itemType: RecurringTaskItemType,
    scheduleMode: RecurringTaskScheduleMode,
    frequency: RecurringTaskFrequency,
    dueDayOfMonth: Int?,
    dueDayOfWeek: Int?,
    revealDayOfMonth: Int
): String {
    val recurrence = stringResource(recurrenceLabelRes(frequency))
    val isEvent = itemType == RecurringTaskItemType.EVENT
    return when (scheduleMode) {
        RecurringTaskScheduleMode.DAY_OF_MONTH ->
            stringResource(
                if (isEvent) R.string.recurring_task_execution_day_of_month_event
                else R.string.recurring_task_execution_day_of_month,
                recurrence,
                dueDayOfMonth ?: 1,
                revealDayOfMonth
            )
        RecurringTaskScheduleMode.DAY_OF_WEEK -> {
            val weekdayRes = weekdayLabels()[(dueDayOfWeek ?: 1).coerceIn(1, 7) - 1]
            val weekday = stringResource(weekdayRes)
            if (frequency == RecurringTaskFrequency.MONTHLY) {
                stringResource(
                    if (isEvent) R.string.recurring_task_execution_monthly_weekday_event
                    else R.string.recurring_task_execution_monthly_weekday,
                    weekday,
                    revealDayOfMonth
                )
            } else {
                stringResource(
                    if (isEvent) R.string.recurring_task_execution_weekday_event
                    else R.string.recurring_task_execution_weekday,
                    recurrence,
                    weekday,
                    revealDayOfMonth
                )
            }
        }
        RecurringTaskScheduleMode.FIRST_DAY_OF_MONTH ->
            stringResource(
                if (isEvent) R.string.recurring_task_execution_month_boundary_event
                else R.string.recurring_task_execution_month_boundary,
                recurrence,
                stringResource(R.string.recurring_task_boundary_first),
                revealDayOfMonth
            )
        RecurringTaskScheduleMode.LAST_DAY_OF_MONTH ->
            stringResource(
                if (isEvent) R.string.recurring_task_execution_month_boundary_event
                else R.string.recurring_task_execution_month_boundary,
                recurrence,
                stringResource(R.string.recurring_task_boundary_last),
                revealDayOfMonth
            )
    }
}

private fun recurrenceLabelRes(frequency: RecurringTaskFrequency): Int =
    when (frequency) {
        RecurringTaskFrequency.WEEKLY -> R.string.recurring_task_recurrence_weekly
        RecurringTaskFrequency.BIWEEKLY -> R.string.recurring_task_recurrence_biweekly
        RecurringTaskFrequency.MONTHLY -> R.string.recurring_task_recurrence_monthly
        RecurringTaskFrequency.QUARTERLY -> R.string.recurring_task_recurrence_quarterly
        RecurringTaskFrequency.HALF_YEARLY -> R.string.recurring_task_recurrence_half_yearly
        RecurringTaskFrequency.ANNUAL -> R.string.recurring_task_recurrence_annual
    }
