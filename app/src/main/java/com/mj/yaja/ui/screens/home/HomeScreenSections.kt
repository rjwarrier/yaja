package com.mj.yaja.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.data.KeywordMatch
import com.mj.yaja.ui.components.AnimatedMenuButton
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.expressiveFabMotion
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.ui.design.floatSpring
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.scaledDelay
import com.mj.yaja.ui.design.scaledDuration
import com.mj.yaja.ui.theme.metaSmallTextStyle
import com.mj.yaja.ui.theme.metaTextStyle
import com.mj.yaja.ui.utils.MarkdownUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DayLabelDialog(
    currentDayLabel: String,
    dayLabelInput: String,
    onDayLabelInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentDayLabel.isEmpty()) "Add Label" else "Edit Label") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Add a short label for this day (max 30 characters).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextField(
                    value = dayLabelInput,
                    onValueChange = { if (it.length <= 30) onDayLabelInputChange(it) },
                    placeholder = { Text("e.g., First day at school") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("${dayLabelInput.length}/30") }
                )
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("Save") } },
        dismissButton = {
            if (currentDayLabel.isNotEmpty()) {
                TextButton(onClick = onRemove) { Text("Remove") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun StarLabelDialog(
    labelInput: String,
    onLabelInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a Label for This Day") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Give this day a memorable label (max 30 characters)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextField(
                    value = labelInput,
                    onValueChange = { if (it.length <= 30) onLabelInputChange(it) },
                    placeholder = { Text("e.g., My Birthday, At school") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("${labelInput.length}/30") }
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Star") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun UndoDeleteBar(
    visible: Boolean,
    deletedEntryCount: Int,
    undoCountdownValue: Float,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motionPreference = LocalAnimationPreference.current
    AnimatedVisibility(
        visible = visible,
        enter = motionPreference.enterOrNone(
            slideInVertically(
                tween(durationMillis = motionPreference.scaledDuration(220))
            ) { it } + fadeIn(motionPreference.floatTween(200))
        ),
        exit = motionPreference.exitOrNone(
            slideOutVertically(
                tween(durationMillis = motionPreference.scaledDuration(180))
            ) { it } + fadeOut(motionPreference.floatTween(150))
        ),
        modifier = modifier
    ) {
        val containerColor = MaterialTheme.colorScheme.errorContainer
        val contentColor = MaterialTheme.colorScheme.onErrorContainer
        val actionColor = MaterialTheme.colorScheme.error
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = MaterialTheme.shapes.large,
            color = containerColor,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                    CircularProgressIndicator(
                        progress = { undoCountdownValue },
                        modifier = Modifier.size(36.dp),
                        color = contentColor,
                        trackColor = contentColor.copy(alpha = 0.22f),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "${kotlin.math.ceil((undoCountdownValue * 5).toDouble()).toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text =
                        if (deletedEntryCount > 1) {
                            "$deletedEntryCount entries deleted"
                        } else {
                            "Entry deleted"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.86f),
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onUndo,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = actionColor
                    )
                ) {
                    Text(
                        text = "RESTORE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    selectedDate: LocalDate,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onOpenDrawer: () -> Unit,
    isFavorited: Boolean,
    onToggleStar: () -> Unit,
    entryCount: Int,
    totalWords: Int,
    totalChars: Int,
    showDayHeaderStats: Boolean,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    swipeToNavigateDatesEnabled: Boolean,
    onEditDayLabel: () -> Unit,
    dayLabel: String,
    starredLabels: Map<LocalDate, String>,
    onOpenVersionSnapshots: () -> Unit,
    showVersionSnapshotsButton: Boolean,
    versionSnapshotsCount: Int
) {
    val motionPreference = LocalAnimationPreference.current
    val dayFormatter = remember { DateTimeFormatter.ofPattern("dd") }
    val monthYearFormatter = remember { DateTimeFormatter.ofPattern("MMMM, yyyy") }
    val weekdayFormatter = remember { DateTimeFormatter.ofPattern("EEEE") }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedMenuButton(onClick = onOpenDrawer)

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = {
                        Text(
                            "Search entries...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Clear Search",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    singleLine = true
                )

                val starScale = remember { Animatable(1f) }
                LaunchedEffect(isFavorited, motionPreference) {
                    if (motionPreference == AnimationPreference.OFF) {
                        starScale.snapTo(1f)
                    } else {
                        starScale.snapTo(1f)
                        starScale.animateTo(
                            targetValue = 1f,
                            animationSpec = keyframes {
                                durationMillis = if (motionPreference == AnimationPreference.REDUCED) 260 else 400
                                if (motionPreference == AnimationPreference.REDUCED) {
                                    1.18f at 70
                                    0.96f at 160
                                    1f at 260
                                } else {
                                    1.45f at 80
                                    0.85f at 180
                                    1.15f at 270
                                    1f at 400
                                }
                            }
                        )
                    }
                }

                Surface(
                    color = if (isFavorited) Color(0xFFFFD700).copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(46.dp),
                    onClick = onToggleStar
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = isFavorited,
                            transitionSpec = {
                                if (motionPreference == AnimationPreference.OFF) {
                                    androidx.compose.animation.EnterTransition.None togetherWith
                                        androidx.compose.animation.ExitTransition.None
                                } else {
                                    (scaleIn(
                                        motionPreference.floatSpring(
                                            dampingRatio = 0.5f,
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                        )
                                    ) + fadeIn(motionPreference.floatTween(150))) togetherWith
                                        (scaleOut(
                                            motionPreference.floatSpring(
                                                dampingRatio = 0.8f,
                                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                            )
                                        ) + fadeOut(motionPreference.floatTween(100)))
                                }
                            },
                            contentAlignment = Alignment.Center,
                            label = "StarIconMorph"
                        ) { starred ->
                            Icon(
                                imageVector = if (starred) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                contentDescription = "Favorite",
                                tint = if (starred) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = starScale.value
                                    scaleY = starScale.value
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = searchQuery.isEmpty(),
                enter = motionPreference.enterOrNone(
                    fadeIn(motionPreference.floatTween(180)) + slideInVertically(
                        tween(durationMillis = motionPreference.scaledDuration(220))
                    ) { -it / 3 }
                ),
                exit = motionPreference.exitOrNone(
                    fadeOut(motionPreference.floatTween(140)) + slideOutVertically(
                        tween(durationMillis = motionPreference.scaledDuration(180))
                    ) { -it / 3 }
                )
            ) {
                HomeDateNavigator(
                    selectedDate = selectedDate,
                    onPreviousDate = onPreviousDate,
                    onNextDate = onNextDate,
                    onSelectDate = onSelectDate,
                    swipeToNavigateDatesEnabled = swipeToNavigateDatesEnabled,
                    onEditDayLabel = onEditDayLabel,
                    dayLabel = dayLabel,
                    starredLabels = starredLabels,
                    onOpenVersionSnapshots = onOpenVersionSnapshots,
                    showVersionSnapshotsButton = showVersionSnapshotsButton,
                    versionSnapshotsCount = versionSnapshotsCount,
                    entryCount = entryCount,
                    totalWords = totalWords,
                    totalChars = totalChars,
                    showDayHeaderStats = showDayHeaderStats,
                    dayFormatter = dayFormatter,
                    weekdayFormatter = weekdayFormatter,
                    monthYearFormatter = monthYearFormatter
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeDailyInsightsRow(entryCount: Int, totalWords: Int, totalChars: Int) {
    HomeDailyInsightsRow(
        entryCount = entryCount,
        totalWords = totalWords,
        totalChars = totalChars,
        versionCount = 0,
        onOpenVersionSnapshots = {},
        showVersionSnapshotsButton = false,
        isTodayHighlighted = false
    )
}

@Composable
private fun HomeDailyInsightsRow(
    entryCount: Int,
    totalWords: Int,
    totalChars: Int,
    versionCount: Int,
    onOpenVersionSnapshots: () -> Unit,
    showVersionSnapshotsButton: Boolean,
    isTodayHighlighted: Boolean = false
) {
    val stats = buildList {
        add("Entries" to entryCount.toString())
        add("Words" to totalWords.toString())
        add("Chars" to totalChars.toString())
        if (showVersionSnapshotsButton && versionCount > 0) {
            add("Vers" to versionCount.toString())
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isTodayHighlighted) {
            Color.White.copy(alpha = 0.05f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.06f)
        },
        border = BorderStroke(
            1.dp,
            if (isTodayHighlighted) {
                Color.White.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
            }
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stats.forEachIndexed { index, (label, value) ->
                val statModifier =
                    Modifier
                        .weight(1f)
                        .let {
                            if (label == "Vers" && showVersionSnapshotsButton && versionCount > 0) {
                                it.clickable(onClick = onOpenVersionSnapshots)
                            } else {
                                it
                            }
                        }
                        .padding(vertical = 2.dp)
                Column(
                    modifier = statModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.metaSmallTextStyle().copy(fontSize = 10.sp),
                        color = if (isTodayHighlighted) {
                            Color.White.copy(alpha = 0.78f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                        },
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isTodayHighlighted) {
                            Color.White.copy(alpha = 0.96f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        softWrap = false
                    )
                }
                if (index != stats.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(
                                if (isTodayHighlighted) {
                                    Color.White.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeDateNavigator(
    selectedDate: LocalDate,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    swipeToNavigateDatesEnabled: Boolean,
    onEditDayLabel: () -> Unit,
    dayLabel: String,
    starredLabels: Map<LocalDate, String>,
    onOpenVersionSnapshots: () -> Unit,
    showVersionSnapshotsButton: Boolean,
    versionSnapshotsCount: Int,
    entryCount: Int,
    totalWords: Int,
    totalChars: Int,
    showDayHeaderStats: Boolean,
    dayFormatter: DateTimeFormatter,
    weekdayFormatter: DateTimeFormatter,
    monthYearFormatter: DateTimeFormatter
) {
    val motionPreference = LocalAnimationPreference.current
    val today = remember { LocalDate.now() }
    val dateStripAnchor = remember { today.minusDays(3650) }
    val dateStripCount = 7301
    val coroutineScope = rememberCoroutineScope()
    val selectedDateIndex =
        remember(selectedDate, dateStripAnchor) {
            ChronoUnit.DAYS.between(dateStripAnchor, selectedDate).toInt().coerceIn(0, dateStripCount - 1)
        }
    val dateStripState = rememberLazyListState()
    LaunchedEffect(selectedDate) {
        val targetIndex = (selectedDateIndex - 3).coerceIn(0, dateStripCount - 7)
        dateStripState.animateScrollToItem(targetIndex)
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 6.dp)
    ) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val compact = maxWidth < 360.dp
        val dateBlockWidth = if (compact) 68.dp else 80.dp
        val horizontalGap = if (compact) 8.dp else 12.dp
        val headerPaddingHorizontal = if (compact) 12.dp else 16.dp
        val headerPaddingVertical = if (compact) 7.dp else 8.dp
        val heroChipHeight = 40.dp
        val dateCellSpacing = 6.dp
        val dateCellWidth = (maxWidth - (dateCellSpacing * 6)) / 7
        val isTodaySelected = selectedDate == today
        val useLightTodayForeground = isTodaySelected
        val heroContainerColor =
            if (isTodaySelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    .compositeOver(MaterialTheme.colorScheme.background)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        val heroBorderColor =
            if (isTodaySelected) {
                Color.White.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f)
            }
        val heroPrimaryText =
            if (useLightTodayForeground) {
                Color.White.copy(alpha = 0.96f)
            } else if (isTodaySelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        val heroSecondaryText =
            if (useLightTodayForeground) {
                Color.White.copy(alpha = 0.72f)
            } else if (isTodaySelected) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
            }
        val dateText = selectedDate.format(dayFormatter)
        val baseDateStyle =
            if (compact) {
                MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
            } else {
                MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold)
            }
        val dateFontSize = remember(dateText, compact, dateBlockWidth) {
            fitDateFontSize(
                textMeasurer = textMeasurer,
                text = dateText,
                baseStyle = baseDateStyle,
                maxWidthPx = with(density) { (dateBlockWidth - 6.dp).roundToPx() },
                minFontSizeSp = if (compact) 24f else 28f,
                maxFontSizeSp = if (compact) 58f else 68f
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            var totalDrag by remember { mutableStateOf(0f) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (swipeToNavigateDatesEnabled) {
                            Modifier.pointerInput(selectedDate, swipeToNavigateDatesEnabled) {
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDrag += dragAmount
                                    },
                                    onDragEnd = {
                                        if (totalDrag < -50f) {
                                            onNextDate()
                                        } else if (totalDrag > 50f) {
                                            onPreviousDate()
                                        }
                                        totalDrag = 0f
                                    },
                                    onDragCancel = {
                                        totalDrag = 0f
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    ),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = heroContainerColor,
                    border = BorderStroke(
                        width = 1.dp,
                        color = heroBorderColor
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .padding(
                                horizontal = headerPaddingHorizontal,
                                vertical = headerPaddingVertical
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(dateBlockWidth)
                                    .padding(top = 2.dp, bottom = 2.dp, end = 2.dp)
                                    .heightIn(min = if (compact) 86.dp else 92.dp),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = if (showDayHeaderStats) {
                                    Arrangement.SpaceBetween
                                } else {
                                    Arrangement.Center
                                }
                            ) {
                                Text(
                                    text = dateText,
                                    style = baseDateStyle.copy(fontSize = dateFontSize),
                                    color = heroPrimaryText,
                                    textAlign = TextAlign.Start,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (showDayHeaderStats) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Text(
                                            text = selectedDate.format(weekdayFormatter),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = heroPrimaryText.copy(alpha = 0.96f),
                                            textAlign = TextAlign.Start,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = selectedDate.format(monthYearFormatter).uppercase(),
                                            style = MaterialTheme.typography.metaSmallTextStyle().copy(fontSize = 10.sp),
                                            color = heroSecondaryText,
                                            textAlign = TextAlign.Start,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(horizontalGap))
                            Box(modifier = Modifier.weight(1f)) {
                                AnimatedContent(
                                    targetState = selectedDate,
                                    transitionSpec = {
                                        if (motionPreference == AnimationPreference.OFF) {
                                            androidx.compose.animation.EnterTransition.None togetherWith
                                                androidx.compose.animation.ExitTransition.None
                                        } else {
                                            val goingForward = targetState > initialState
                                            (androidx.compose.animation.slideInHorizontally(
                                                tween(durationMillis = motionPreference.scaledDuration(280))
                                            ) {
                                                if (goingForward) it else -it
                                            } + fadeIn(motionPreference.floatTween(200))) togetherWith
                                                (androidx.compose.animation.slideOutHorizontally(
                                                    tween(durationMillis = motionPreference.scaledDuration(280))
                                                ) {
                                                    if (goingForward) -it else it
                                                } + fadeOut(motionPreference.floatTween(160)))
                                        }
                                    },
                                    contentAlignment = Alignment.CenterStart,
                                    label = "DateHeaderCrossfade"
                                ) { date ->
                                    val labelTextStyle =
                                        MaterialTheme.typography.titleSmall.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    val labelTextWidth =
                                        with(density) {
                                            textMeasurer
                                                .measure(
                                                    text = dayLabel.ifBlank { "Add label" },
                                                    style = labelTextStyle,
                                                    maxLines = 1
                                                )
                                                .size
                                                .width
                                                .toDp()
                                        }
                                    val labelWidth =
                                        (labelTextWidth + if (dayLabel.isNotEmpty()) 64.dp else 54.dp)
                                            .coerceIn(
                                                if (compact) 92.dp else 100.dp,
                                                if (compact) 260.dp else 360.dp
                                            )

                                    if (showDayHeaderStats) {
                                        Column(
                                            modifier = Modifier.pointerInput(Unit) {
                                                detectTapGestures(onLongPress = { onEditDayLabel() })
                                            },
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            if (dayLabel.isNotEmpty()) {
                                                Surface(
                                                    modifier = Modifier
                                                        .height(heroChipHeight)
                                                        .width(labelWidth),
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = if (isTodaySelected) {
                                                        if (useLightTodayForeground) Color.White.copy(alpha = 0.08f)
                                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                    } else {
                                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                                                    },
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isTodaySelected) {
                                                            if (useLightTodayForeground) Color.White.copy(alpha = 0.18f)
                                                            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                                                        } else {
                                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)
                                                        }
                                                    ),
                                                    tonalElevation = 0.dp,
                                                    shadowElevation = 0.dp
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = 16.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.EditCalendar,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp),
                                                            tint = if (isTodaySelected) {
                                                                heroPrimaryText
                                                            } else {
                                                                MaterialTheme.colorScheme.primary
                                                            }
                                                        )
                                                        Text(
                                                            text = dayLabel,
                                                            style = labelTextStyle,
                                                            color = if (isTodaySelected) {
                                                                heroPrimaryText
                                                            } else {
                                                                MaterialTheme.colorScheme.onTertiaryContainer
                                                            },
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            } else {
                                                AssistChip(
                                                    onClick = onEditDayLabel,
                                                    label = {
                                                        Text(
                                                            text = "Add label",
                                                            style = MaterialTheme.typography.metaSmallTextStyle(),
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.EditCalendar,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    },
                                                    colors = AssistChipDefaults.assistChipColors(
                                                        containerColor = if (isTodaySelected) {
                                                            if (useLightTodayForeground) Color.White.copy(alpha = 0.08f)
                                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                        } else {
                                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                                        },
                                                        labelColor = if (isTodaySelected) {
                                                            heroPrimaryText
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        leadingIconContentColor = if (isTodaySelected) {
                                                            heroPrimaryText
                                                        } else {
                                                            MaterialTheme.colorScheme.primary
                                                        }
                                                    ),
                                                    border = AssistChipDefaults.assistChipBorder(
                                                        enabled = true,
                                                        borderColor = if (isTodaySelected) {
                                                            if (useLightTodayForeground) Color.White.copy(alpha = 0.18f)
                                                            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                                                        } else {
                                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                                        }
                                                    ),
                                                    modifier = Modifier.height(heroChipHeight)
                                                )
                                            }
                                            HomeDailyInsightsRow(
                                                entryCount = entryCount,
                                                totalWords = totalWords,
                                                totalChars = totalChars,
                                                versionCount = versionSnapshotsCount,
                                                onOpenVersionSnapshots = onOpenVersionSnapshots,
                                                showVersionSnapshotsButton = showVersionSnapshotsButton,
                                                isTodayHighlighted = useLightTodayForeground
                                            )
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .pointerInput(Unit) {
                                                    detectTapGestures(onLongPress = { onEditDayLabel() })
                                                }
                                                .heightIn(min = if (compact) 86.dp else 92.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = date.format(weekdayFormatter),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = heroPrimaryText,
                                                textAlign = TextAlign.Start,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = date.format(monthYearFormatter).uppercase(),
                                                style = MaterialTheme.typography.metaSmallTextStyle().copy(fontSize = 11.sp),
                                                color = heroSecondaryText,
                                                textAlign = TextAlign.Start,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            if (dayLabel.isNotEmpty()) {
                                                Surface(
                                                    modifier = Modifier
                                                        .height(heroChipHeight)
                                                        .width(labelWidth),
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = if (isTodaySelected) {
                                                        if (useLightTodayForeground) Color.White.copy(alpha = 0.08f)
                                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                    } else {
                                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                                                    },
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isTodaySelected) {
                                                            if (useLightTodayForeground) Color.White.copy(alpha = 0.18f)
                                                            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                                                        } else {
                                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)
                                                        }
                                                    ),
                                                    tonalElevation = 0.dp,
                                                    shadowElevation = 0.dp
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = 16.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.EditCalendar,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp),
                                                            tint = if (isTodaySelected) heroPrimaryText else MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = dayLabel,
                                                            style = labelTextStyle,
                                                            color = if (isTodaySelected) heroPrimaryText else MaterialTheme.colorScheme.onTertiaryContainer,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            } else {
                                                AssistChip(
                                                    onClick = onEditDayLabel,
                                                    label = {
                                                        Text(
                                                            text = "Add label",
                                                            style = MaterialTheme.typography.metaSmallTextStyle(),
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.EditCalendar,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    },
                                                    colors = AssistChipDefaults.assistChipColors(
                                                        containerColor = if (isTodaySelected) {
                                                            if (useLightTodayForeground) Color.White.copy(alpha = 0.08f)
                                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                        } else {
                                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                                        },
                                                        labelColor = if (isTodaySelected) heroPrimaryText else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        leadingIconContentColor = if (isTodaySelected) heroPrimaryText else MaterialTheme.colorScheme.primary
                                                    ),
                                                    border = AssistChipDefaults.assistChipBorder(
                                                        enabled = true,
                                                        borderColor = if (isTodaySelected) {
                                                            if (useLightTodayForeground) Color.White.copy(alpha = 0.18f)
                                                            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                                                        } else {
                                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                                        }
                                                    ),
                                                    modifier = Modifier.height(heroChipHeight)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LazyRow(
                state = dateStripState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dateCellSpacing),
                contentPadding = PaddingValues(horizontal = 0.dp),
                userScrollEnabled = true
            ) {
                items(
                    count = dateStripCount,
                    key = { index -> index }
                ) { index ->
                    val date = dateStripAnchor.plusDays(index.toLong())
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val hasLabel = starredLabels[date]?.isNotBlank() == true
                    val targetContainerColor =
                        when {
                            isSelected && isToday -> MaterialTheme.colorScheme.primary
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
                            hasLabel -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f)
                        }
                    val targetContentColor =
                        when {
                            isSelected && isToday -> MaterialTheme.colorScheme.onPrimary
                            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                            hasLabel -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    val targetBorderColor =
                        when {
                            isSelected -> Color.Transparent
                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            hasLabel -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)
                        }
                    val containerColor by animateColorAsState(
                        targetValue = targetContainerColor,
                        animationSpec = tween(durationMillis = motionPreference.scaledDuration(220)),
                        label = "date_chip_container"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = targetContentColor,
                        animationSpec = tween(durationMillis = motionPreference.scaledDuration(220)),
                        label = "date_chip_content"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = targetBorderColor,
                        animationSpec = tween(durationMillis = motionPreference.scaledDuration(220)),
                        label = "date_chip_border"
                    )
                    val chipScale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.97f,
                        animationSpec = motionPreference.floatSpring(
                            dampingRatio = 0.82f,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        ),
                        label = "date_chip_scale"
                    )

                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                dateStripState.animateScrollToItem((index - 3).coerceIn(0, dateStripCount - 7))
                            }
                            onSelectDate(date)
                        },
                        modifier = Modifier
                            .width(dateCellWidth)
                            .animateItem()
                            .graphicsLayer {
                                scaleX = chipScale
                                scaleY = chipScale
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = containerColor,
                        border = BorderStroke(
                            1.dp,
                            borderColor
                        ),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = date.dayOfWeek.name.take(1),
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor.copy(alpha = 0.82f)
                            )
                            Text(
                                text = date.format(dayFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateNavButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 36.dp
) {
    val motionPreference = LocalAnimationPreference.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = motionPreference.floatSpring(
            dampingRatio = 0.5f,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "date_nav_scale"
    )
    val iconSize = size / 2

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
        shape = CircleShape,
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun SearchResultsContent(
    searchResults: List<com.mj.yaja.data.SearchResult>,
    onResultClicked: (LocalDate) -> Unit
) {
    Column {
        Text(
            text = "${searchResults.size} Matches Found",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
        ) {
            itemsIndexed(
                items = searchResults,
                key = { _, it -> "${it.date}_${it.entryPreview.hashCode()}" }
            ) { index, result ->
                AppStaggeredEntrance(
                    visible = true,
                    index = index,
                    modifier = Modifier.animateItem(
                        fadeInSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
                        fadeOutSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
                        placementSpec = spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    )
                ) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clickable { onResultClicked(result.date) },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            val resultFormatter = remember { DateTimeFormatter.ofPattern("dd-MMM-yyyy") }
                            Text(
                                text = result.date.format(resultFormatter),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = MarkdownUtils.parseMarkdown(result.entryPreview),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun KeywordFilterResultsContent(
    keywordFilteredEntries: List<Pair<LocalDate, List<KeywordMatch>>>,
    onDateLinkClick: (LocalDate) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = WindowInsets.navigationBars.asPaddingValues()
    ) {
        if (keywordFilteredEntries.isEmpty()) {
            item("empty-filter") {
                Text(
                    text = "No matches found for this keyword yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        } else {
            items(keywordFilteredEntries, key = { it.first.toString() }) { (date, matches) ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clickable { onDateLinkClick(date) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        matches.take(3).forEach { match ->
                            Text(
                                text = "• ${match.snippet.ifBlank { match.matchedText }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun EmptyEntriesState(
    selectedDate: LocalDate,
    dayLabel: String,
    onIconClick: () -> Unit
) {
    val motionPreference = LocalAnimationPreference.current
    val infiniteTransition = rememberInfiniteTransition(label = "EmptyBreathing")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = if (motionPreference == AnimationPreference.OFF) 1f else 0.97f,
        targetValue = when (motionPreference) {
            AnimationPreference.FULL -> 1.06f
            AnimationPreference.REDUCED -> 1.02f
            AnimationPreference.OFF -> 1f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (motionPreference == AnimationPreference.REDUCED) 1100 else 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IconScale"
    )

    AppStaggeredEntrance(
        visible = true,
        index = 0,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 108.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 48.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .size(100.dp)
                        .clickable(onClick = onIconClick)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                selectedDate.isAfter(LocalDate.now()) -> Icons.Rounded.EditCalendar
                                selectedDate.isBefore(LocalDate.now()) -> Icons.Rounded.HistoryEdu
                                else -> Icons.Rounded.AutoStories
                            },
                            contentDescription = "Empty Status Icon",
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = when {
                        selectedDate.isAfter(LocalDate.now()) -> "This is the future. Record a reminder!"
                        selectedDate.isBefore(LocalDate.now()) -> "You are in the Past. Record a Memory before it fades..."
                        else -> "No entries yet. Start your day."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun HomeFabCluster(
    selectedDate: LocalDate,
    drawerOpen: Boolean,
    allowFutureEntries: Boolean,
    onJumpToToday: () -> Unit,
    onConfirmAddEntry: () -> Unit,
    onFutureDateAttempt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motionPreference = LocalAnimationPreference.current
    var fabEntryRotation by remember { mutableStateOf(0f) }
    val animatedEntryRotation by animateFloatAsState(
        targetValue = fabEntryRotation,
        animationSpec = motionPreference.floatTween(500),
        label = "fab_entry_rotation"
    )
    LaunchedEffect(Unit) { fabEntryRotation = 360f }

    var fabPressed by remember { mutableStateOf(false) }
    LaunchedEffect(fabPressed) {
        if (fabPressed) {
            delay(motionPreference.scaledDelay(380).toLong())
            onConfirmAddEntry()
            fabPressed = false
        }
    }

    val fabClusterTransition = updateTransition(targetState = drawerOpen, label = "fab_cluster_transition")
    val fabClusterScale by fabClusterTransition.animateFloat(
        transitionSpec = {
            if (motionPreference == AnimationPreference.OFF) {
                tween(0)
            } else if (targetState) {
                keyframes {
                    durationMillis = if (motionPreference == AnimationPreference.REDUCED) 220 else 360
                    1f at 0
                    0.5f at ((durationMillis * 0.68f).toInt())
                    0.14f at durationMillis
                }
            } else {
                keyframes {
                    durationMillis = if (motionPreference == AnimationPreference.REDUCED) 200 else 320
                    0.14f at 0
                    0.58f at ((durationMillis * 0.55f).toInt())
                    1f at durationMillis
                }
            }
        },
        label = "fab_cluster_scale"
    ) { isOpen ->
        if (isOpen) 0.14f else 1f
    }
    val fabClusterAlpha by fabClusterTransition.animateFloat(
        transitionSpec = {
            if (motionPreference == AnimationPreference.OFF) {
                tween(0)
            } else if (targetState) {
                keyframes {
                    durationMillis = if (motionPreference == AnimationPreference.REDUCED) 220 else 360
                    1f at 0
                    1f at ((durationMillis * 0.72f).toInt())
                    0f at durationMillis
                }
            } else {
                keyframes {
                    durationMillis = if (motionPreference == AnimationPreference.REDUCED) 200 else 320
                    0f at 0
                    0.8f at ((durationMillis * 0.45f).toInt())
                    1f at durationMillis
                }
            }
        },
        label = "fab_cluster_alpha"
    ) { isOpen ->
        if (isOpen) 0f else 1f
    }
    val fabClusterRotation by fabClusterTransition.animateFloat(
        transitionSpec = {
            if (motionPreference == AnimationPreference.OFF) {
                tween(0)
            } else if (targetState) {
                tween(
                    durationMillis = if (motionPreference == AnimationPreference.REDUCED) 220 else 360,
                    easing = FastOutSlowInEasing
                )
            } else {
                tween(
                    durationMillis = if (motionPreference == AnimationPreference.REDUCED) 200 else 320,
                    easing = FastOutSlowInEasing
                )
            }
        },
        label = "fab_cluster_rotation"
    ) { isOpen ->
        if (isOpen) -540f else 0f
    }
    val showTodayFab = selectedDate != LocalDate.now()
    val todayFabOffset by animateFloatAsState(
        targetValue = if (showTodayFab) 0f else 88f,
        animationSpec = if (motionPreference == AnimationPreference.OFF) {
            tween(0)
        } else {
            motionPreference.floatSpring(
                dampingRatio = if (motionPreference == AnimationPreference.REDUCED) 0.92f else 0.58f,
                stiffness = if (motionPreference == AnimationPreference.REDUCED) 760f else 420f
            )
        },
        label = "today_fab_offset"
    )
    val todayFabAlpha by animateFloatAsState(
        targetValue = if (showTodayFab) 1f else 0f,
        animationSpec = if (motionPreference == AnimationPreference.OFF) {
            tween(0)
        } else {
            tween(
                durationMillis = if (motionPreference == AnimationPreference.REDUCED) 120 else 180,
                easing = FastOutSlowInEasing
            )
        },
        label = "today_fab_alpha"
    )
    val todayFabScale by animateFloatAsState(
        targetValue = if (showTodayFab) 1f else 0.56f,
        animationSpec = if (motionPreference == AnimationPreference.OFF) {
            tween(0)
        } else {
            motionPreference.floatSpring(
                dampingRatio = if (motionPreference == AnimationPreference.REDUCED) 0.82f else 0.46f,
                stiffness = if (motionPreference == AnimationPreference.REDUCED) 620f else 360f
            )
        },
        label = "today_fab_scale"
    )
    val todayFabRotation by animateFloatAsState(
        targetValue = if (showTodayFab) 0f else 28f,
        animationSpec = if (motionPreference == AnimationPreference.OFF) {
            tween(0)
        } else {
            motionPreference.floatSpring(
                dampingRatio = if (motionPreference == AnimationPreference.REDUCED) 0.9f else 0.62f,
                stiffness = if (motionPreference == AnimationPreference.REDUCED) 680f else 430f
            )
        },
        label = "today_fab_rotation"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = fabClusterScale
                scaleY = fabClusterScale
                alpha = fabClusterAlpha
                rotationZ = fabClusterRotation
                transformOrigin = TransformOrigin.Center
            }
    ) {
        val todayFabInteraction = remember { MutableInteractionSource() }
        val addFabInteraction = remember { MutableInteractionSource() }
        if (showTodayFab || todayFabAlpha > 0.01f) {
            FloatingActionButton(
                onClick = {
                    if (!drawerOpen) onJumpToToday()
                },
                interactionSource = todayFabInteraction,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(y = (-88).dp)
                    .size(64.dp)
                    .expressiveFabMotion(todayFabInteraction)
                    .graphicsLayer {
                        translationY = todayFabOffset
                        alpha = todayFabAlpha
                        scaleX = todayFabScale
                        scaleY = todayFabScale
                        rotationZ = todayFabRotation
                    },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Rounded.Today, contentDescription = "Jump to Today")
            }
        }

        AnimatedVisibility(
            visible = !selectedDate.isAfter(LocalDate.now()) || allowFutureEntries,
            enter = motionPreference.enterOrNone(
                scaleIn(
                    motionPreference.floatSpring(
                        dampingRatio = 0.6f,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    )
                ) + fadeIn(motionPreference.floatTween(180))
            ),
            exit = motionPreference.exitOrNone(
                scaleOut(
                    motionPreference.floatSpring(
                        dampingRatio = 0.85f,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    )
                ) + fadeOut(motionPreference.floatTween(120))
            )
        ) {
            FloatingActionButton(
                onClick = {
                    if (!drawerOpen && !fabPressed) {
                        if (selectedDate.isAfter(LocalDate.now())) onFutureDateAttempt()
                        else fabPressed = true
                    }
                },
                interactionSource = addFabInteraction,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .size(64.dp)
                    .expressiveFabMotion(addFabInteraction),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                AnimatedContent(
                    targetState = fabPressed,
                    transitionSpec = {
                        if (motionPreference == AnimationPreference.OFF) {
                            androidx.compose.animation.EnterTransition.None togetherWith
                                androidx.compose.animation.ExitTransition.None
                        } else {
                            (scaleIn(
                                motionPreference.floatSpring(
                                    dampingRatio = 0.5f,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                )
                            ) + fadeIn(motionPreference.floatTween(180))) togetherWith
                                (scaleOut(
                                    motionPreference.floatSpring(
                                        dampingRatio = 0.85f,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                    )
                                ) + fadeOut(motionPreference.floatTween(120)))
                        }
                    },
                    contentAlignment = Alignment.Center,
                    label = "FabIconMorph"
                ) { isPressed ->
                    if (isPressed) {
                        Icon(Icons.Rounded.Check, contentDescription = "Saving")
                    } else {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Add Entry",
                            modifier = Modifier.graphicsLayer {
                                rotationZ = animatedEntryRotation
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteSelectedEntriesPill(
    selectedEntryCount: Int,
    onDeleteSelectedEntries: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motionPreference = LocalAnimationPreference.current
    AnimatedVisibility(
        visible = selectedEntryCount > 0,
        enter = motionPreference.enterOrNone(
            slideInVertically(
                animationSpec = tween(
                    durationMillis = motionPreference.scaledDuration(260),
                    easing = FastOutSlowInEasing
                )
            ) { it + 80 } +
                fadeIn(motionPreference.floatTween(170)) +
                scaleIn(
                    animationSpec = motionPreference.floatSpring(
                        dampingRatio = 0.56f,
                        stiffness = 380f
                    ),
                    initialScale = 0.82f
                )
        ),
        exit = motionPreference.exitOrNone(
            slideOutVertically(
                animationSpec = tween(
                    durationMillis = motionPreference.scaledDuration(180),
                    easing = FastOutSlowInEasing
                )
            ) { it + 56 } +
                fadeOut(motionPreference.floatTween(120)) +
                scaleOut(
                    animationSpec = motionPreference.floatSpring(
                        dampingRatio = 0.88f,
                        stiffness = 520f
                    ),
                    targetScale = 0.88f
                )
        ),
        modifier = modifier
    ) {
        Surface(
            onClick = {
                onDeleteSelectedEntries()
            },
            modifier = Modifier.height(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Delete $selectedEntryCount",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun FutureDateDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Future Date Entry") },
        text = {
            Text(
                "You are adding an entry for a future date (${selectedDate.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))}). Do you want to continue?"
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Yes") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("No") } }
    )
}

@Composable
fun CacheAnomalyDialog(
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Data Cache Issue Detected") },
        text = {
            Text(
                "It appears a substantial number of journal entries are missing from the cache. Would you like to perform a full data cache refresh to attempt to restore them?"
            )
        },
        confirmButton = { TextButton(onClick = onRefresh) { Text("Refresh") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ignore") } }
    )
}

private fun fitDateFontSize(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    baseStyle: TextStyle,
    maxWidthPx: Int,
    minFontSizeSp: Float,
    maxFontSizeSp: Float
) =
    generateSequence(maxFontSizeSp) { current ->
        (current - 1f).takeIf { it >= minFontSizeSp }
    }.firstOrNull { candidate ->
        val result =
            textMeasurer.measure(
                text = text,
                style = baseStyle.copy(fontSize = candidate.sp),
                maxLines = 1,
                softWrap = false,
                constraints = Constraints(maxWidth = maxWidthPx)
            )
        result.size.width <= maxWidthPx
    }?.sp ?: minFontSizeSp.sp
