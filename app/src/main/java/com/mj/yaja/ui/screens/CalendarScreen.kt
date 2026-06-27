package com.mj.yaja.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.rememberAppEntrance
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class CalendarViewMode {
        DAYS,
        MONTHS,
        YEARS
}

enum class CalendarGraphMode {
        BAR,
        LINE
}

enum class CalendarGraphFrequency {
        YEAR,
        MONTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit,
        onNavigateBack: () -> Unit,
        onNavigateToTimeline: () -> Unit,
        onDateSelected: (LocalDate) -> Unit,
        onDateSelectedForEntry: (LocalDate) -> Unit
) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val firstDayOfWeekPref by viewModel.firstDayOfWeek.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val entranceTriggered = rememberAppEntrance()

        var currentMonth by remember { mutableStateOf(YearMonth.from(uiState.selectedDate)) }
        var currentYearWindowStart by remember {
                mutableStateOf(currentMonth.year - (currentMonth.year % 10))
        }
        var viewMode by remember { mutableStateOf(CalendarViewMode.DAYS) }
        var graphMode by remember { mutableStateOf(CalendarGraphMode.BAR) }
        var graphFrequency by remember { mutableStateOf(CalendarGraphFrequency.YEAR) }

        val allowFutureEntries by viewModel.allowFutureEntries.collectAsStateWithLifecycle()
        val favoritedDates by viewModel.favoritedDates.collectAsStateWithLifecycle()
        val revisitTargetDates by viewModel.revisitTargetDates.collectAsStateWithLifecycle()
        var showFutureDateDialog by remember { mutableStateOf(false) }
        var showEnableFutureDateDialog by remember { mutableStateOf(false) }
        var pendingFutureDate by remember { mutableStateOf<LocalDate?>(null) }
        var showJumpToDate by remember { mutableStateOf(false) }
        var jumpDateValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
        var jumpDateError by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val today = remember { LocalDate.now() }
        val futureEntryEnabledText = stringResource(R.string.calendar_future_entry_enabled)
        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                stringResource(R.string.nav_calendar),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                },
                                navigationIcon = {
                                        com.mj.yaja.ui.components.AnimatedMenuButton(
                                                onClick = onOpenDrawer,
                                                modifier = Modifier.padding(start = 8.dp)
                                        )
                                },
                                colors =
                                        TopAppBarDefaults.topAppBarColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.background,
                                                titleContentColor =
                                                        MaterialTheme.colorScheme.primary,
                                                navigationIconContentColor =
                                                        MaterialTheme.colorScheme.onSurface,
                                                actionIconContentColor =
                                                        MaterialTheme.colorScheme.onSurface
                                        )
                        )
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
        ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        AppScreenReveal(
                                visible = true,
                                key = uiState.selectedDate,
                                modifier = Modifier.fillMaxSize()
                        ) {
                                Column(
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .padding(horizontal = 24.dp)
                                                        .verticalScroll(rememberScrollState())
                                ) {
                                        Spacer(modifier = Modifier.height(22.dp))

                                        // Calendar Area Card
                                        AppStaggeredEntrance(
                                                visible = entranceTriggered,
                                                index = 0,
                                                strength = AppEntranceStrength.HERO
                                        ) {
                                                androidx.compose.material3.ElevatedCard(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors =
                                                                androidx.compose.material3.CardDefaults
                                                                        .elevatedCardColors(
                                                                                containerColor =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .surfaceContainerLow
                                                                        ),
                                                        elevation =
                                                                androidx.compose.material3.CardDefaults
                                                                        .elevatedCardElevation(0.dp),
                                                        shape = MaterialTheme.shapes.large
                                                ) {
                                                        Column(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .padding(horizontal = 20.dp, vertical = 22.dp)
                                                        ) {
                                                                CalendarHeaderNavigator(
                                                                        viewMode = viewMode,
                                                                        currentMonth = currentMonth,
                                                                        currentYearWindowStart =
                                                                                currentYearWindowStart,
                                                                        headerSubtitle = null,
                                                                        allowFutureEntries =
                                                                                allowFutureEntries,
                                                                        onPreviousClick = {
                                                                                when (viewMode) {
                                                                                        CalendarViewMode
                                                                                                .DAYS ->
                                                                                                currentMonth =
                                                                                                        currentMonth
                                                                                                                .minusMonths(
                                                                                                                        1
                                                                                                                )
                                                                                        CalendarViewMode
                                                                                                .MONTHS ->
                                                                                                currentMonth =
                                                                                                        currentMonth
                                                                                                                .minusYears(
                                                                                                                        1
                                                                                                                )
                                                                                        CalendarViewMode
                                                                                                .YEARS ->
                                                                                                currentYearWindowStart -=
                                                                                                        10
                                                                                }
                                                                        },
                                                                        onNextClick = {
                                                                                when (viewMode) {
                                                                                        CalendarViewMode
                                                                                                .DAYS ->
                                                                                                currentMonth =
                                                                                                        currentMonth
                                                                                                                .plusMonths(
                                                                                                                        1
                                                                                                                )
                                                                                        CalendarViewMode
                                                                                                .MONTHS ->
                                                                                                currentMonth =
                                                                                                        currentMonth
                                                                                                                .plusYears(
                                                                                                                        1
                                                                                                                )
                                                                                        CalendarViewMode
                                                                                                .YEARS ->
                                                                                                currentYearWindowStart +=
                                                                                                        10
                                                                                }
                                                                        },
                                                                        onHeaderClick = {
                                                                                viewMode =
                                                                                        when (viewMode) {
                                                                                                CalendarViewMode
                                                                                                        .DAYS ->
                                                                                                        CalendarViewMode
                                                                                                                .MONTHS
                                                                                                CalendarViewMode
                                                                                                        .MONTHS -> {
                                                                                                        currentYearWindowStart =
                                                                                                                currentMonth
                                                                                                                        .year -
                                                                                                                        (currentMonth
                                                                                                                                .year %
                                                                                                                                10)
                                                                                                        CalendarViewMode
                                                                                                                .YEARS
                                                                                                }
                                                                                                CalendarViewMode
                                                                                                        .YEARS ->
                                                                                                        CalendarViewMode
                                                                                                                .DAYS
                                                                                        }
                                                                        }
                                                                )

                                                                AnimatedVisibility(
                                                                        visible = viewMode == CalendarViewMode.DAYS && currentMonth != YearMonth.now(),
                                                                        enter = fadeIn() + expandVertically(),
                                                                        exit = fadeOut() + shrinkVertically(),
                                                                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                                                                ) {
                                                                        SuggestionChip(
                                                                                onClick = {
                                                                                        currentMonth = YearMonth.now()
                                                                                },
                                                                                label = {
                                                                                        Text(
                                                                                                text = stringResource(R.string.shortcut_today_short),
                                                                                                style = MaterialTheme.typography.labelMedium,
                                                                                                fontWeight = FontWeight.Bold
                                                                                        )
                                                                                },
                                                                                icon = {
                                                                                        Icon(
                                                                                                imageVector = Icons.Rounded.Today,
                                                                                                contentDescription = stringResource(R.string.calendar_cd_go_to_current_month),
                                                                                                modifier = Modifier.size(16.dp)
                                                                                        )
                                                                                },
                                                                                shape = RoundedCornerShape(12.dp),
                                                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                                                        labelColor = MaterialTheme.colorScheme.primary,
                                                                                        iconContentColor = MaterialTheme.colorScheme.primary
                                                                                ),
                                                                                border = BorderStroke(
                                                                                        1.dp,
                                                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                                                )
                                                                        )
                                                                }

                                                                Spacer(modifier = Modifier.height(24.dp))

                                                                AnimatedVisibility(
                                                                        visible =
                                                                                viewMode ==
                                                                                        CalendarViewMode
                                                                                                .DAYS
                                                                ) {
                                                                        CalendarWeekdayHeader(
                                                                                firstDayOfWeekPref =
                                                                                        firstDayOfWeekPref
                                                                        )
                                                                }

                                                                CalendarViewModeBody(
                                                                        viewMode = viewMode,
                                                                        currentMonth = currentMonth,
                                                                        currentYearWindowStart =
                                                                                currentYearWindowStart,
                                                                        selectedDate =
                                                                                uiState.selectedDate,
                                                                        datesWithEntries =
                                                                                uiState.datesWithEntries,
                                                                        allowFutureEntries =
                                                                                allowFutureEntries,
                                                                        favoritedDates = favoritedDates,
                                                                        revisitTargetDates =
                                                                                revisitTargetDates,
                                                                        firstDayOfWeekPref =
                                                                                firstDayOfWeekPref,
                                                                        onSelectDate = onDateSelected,
                                                                        onSelectFutureDateWithEntries =
                                                                                onDateSelected,
                                                                        onFutureDateRequest = {
                                                                                pendingFutureDate = it
                                                                                showFutureDateDialog = true
                                                                        },
                                                                        onEnableFutureDateRequest = {
                                                                                pendingFutureDate = it
                                                                                showEnableFutureDateDialog =
                                                                                        true
                                                                        },
                                                                        onMonthPicked = {
                                                                                currentMonth =
                                                                                        currentMonth
                                                                                                .withMonth(
                                                                                                        it
                                                                                                )
                                                                                viewMode =
                                                                                        CalendarViewMode
                                                                                                .DAYS
                                                                        },
                                                                        onYearPicked = {
                                                                                currentMonth =
                                                                                        currentMonth
                                                                                                .withYear(
                                                                                                        it
                                                                                                )
                                                                                viewMode =
                                                                                        CalendarViewMode
                                                                                                .MONTHS
                                                                        }
                                                                )

                                                                CalendarMonthCoverageSummary(
                                                                        viewMode = viewMode,
                                                                        currentMonth = currentMonth,
                                                                        datesWithEntries =
                                                                                uiState.datesWithEntries
                                                                )
                                                        }
                                                }
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        AppStaggeredEntrance(
                                                visible = entranceTriggered,
                                                index = 1,
                                                strength = AppEntranceStrength.SECTION
                                        ) {
                                                CalendarQuickActions(
                                                        onNavigateToTimeline = onNavigateToTimeline,
                                                        onJumpToDate = {
                                                                jumpDateValue =
                                                                        androidx.compose.ui.text.input
                                                                                .TextFieldValue("")
                                                                jumpDateError = false
                                                                showJumpToDate = true
                                                        }
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        AppStaggeredEntrance(
                                                visible = entranceTriggered,
                                                index = 2,
                                                strength = AppEntranceStrength.SECTION
                                        ) {
                                                CalendarConsistencySection(
                                                        monthlyStats = uiState.monthlyStats,
                                                        yearlyStats = uiState.yearlyStats,
                                                        graphMode = graphMode,
                                                        graphFrequency = graphFrequency,
                                                        onGraphModeChange = { graphMode = it },
                                                        onGraphFrequencyChange = { graphFrequency = it }
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(32.dp))

                                        AppStaggeredEntrance(
                                                visible = entranceTriggered,
                                                index = 3,
                                                strength = AppEntranceStrength.SECTION
                                        ) {
                                                CalendarMemorySummary(
                                                        memoryCount = uiState.datesWithEntries.size
                                                )
                                        } // end AnimatedVisibility (memory blob)
                                }
                        }
                }
        }

        // ── Jump to Date Dialog ──
        if (showJumpToDate) {
                CalendarJumpToDateDialog(
                        jumpDateValue = jumpDateValue,
                        jumpDateError = jumpDateError,
                        onValueChange = { newVal ->
                                val filtered =
                                        newVal.text.filter { it.isDigit() || it == '-' }.take(8)
                                val isAppending =
                                        newVal.selection.end == newVal.text.length &&
                                                filtered.length > jumpDateValue.text.length
                                val autoText =
                                        if (
                                                isAppending &&
                                                        (filtered.length == 2 ||
                                                                filtered.length == 5) &&
                                                        filtered.last().isDigit()
                                        ) {
                                                "$filtered-"
                                        } else {
                                                filtered
                                        }
                                jumpDateValue =
                                        androidx.compose.ui.text.input.TextFieldValue(
                                                text = autoText,
                                                selection =
                                                        androidx.compose.ui.text.TextRange(
                                                                autoText.length
                                                        )
                                        )
                                jumpDateError = false
                        },
                        onDismiss = { showJumpToDate = false },
                        onInvalidDate = { jumpDateError = true },
                        onDatePicked = onDateSelected,
                        onCurrentMonthChange = { currentMonth = it },
                        onViewModeChange = { viewMode = it }
                )
        }

        CalendarFutureDateDialogs(
                showFutureDateDialog = showFutureDateDialog,
                showEnableFutureDateDialog = showEnableFutureDateDialog,
                pendingFutureDate = pendingFutureDate,
                onDismissFutureDate = { showFutureDateDialog = false },
                onDismissEnableFutureDate = { showEnableFutureDateDialog = false },
                onConfirmFutureDate = onDateSelectedForEntry,
                onEnableFutureDate = {
                        viewModel.setAllowFutureEntries(true)
                        android.widget.Toast.makeText(
                                        context,
                                        futureEntryEnabledText,
                                        android.widget.Toast.LENGTH_SHORT
                                )
                                .show()
                        onDateSelectedForEntry(it)
                }
        )
}

/** Parses DD-MM-YY or DD-MM-YYYY input into a LocalDate, returns null if invalid. */
internal fun parseJumpDate(input: String): LocalDate? {
        val cleaned = input.trim()
        val formatters = listOf(
                DateTimeFormatter.ofPattern("dd-MM-yy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d-M-yy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
        )
        for (fmt in formatters) {
                try { return LocalDate.parse(cleaned, fmt) } catch (_: Exception) { }
        }
        return null
}

