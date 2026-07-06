package com.mj.yaja.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Note
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.R
import com.mj.yaja.data.estimateReadingTimeMinutes
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.viewmodel.JournalViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class StatisticsPeriod {
    ALL_TIME,
    CURRENT_YEAR,
    PREVIOUS_YEAR,
    CURRENT_MONTH,
    PREVIOUS_MONTH,
    CUSTOM
}

data class AllTimeStatsData(
    val totalEntries: Int,
    val totalWords: Int,
    val averageWordsPerEntry: Float,
    val currentStreak: Int,
    val longestStreakAllTime: Int,
    val mostActiveDay: String?,
    val totalDaysWithEntries: Int,
    val writingConsistencyScore: Float, // 0-100
    val monthlyEntryTrend: List<Pair<String, Int>>, // Month, count
    val entriesByLength: DayDistribution,
    val totalHighlightedDays: Int,            // favorited days with entries in this period
    val bestMonthLabel: String?,              // e.g. "Mar 2025"
    val bestMonthCount: Int,                  // entries in that best month
    val averageDaysPerWeek: Float,            // writing days per week on average
    val writingTimeDistribution: TimeDistribution, // when during the day the user writes
    val languageDistribution: Map<String, Int>, // ISO 639-1 code → entry count, sorted by count desc
    val templateInsights: TemplateInsightsData? = null
)

/** Daily writing volume categories. Each field counts days that fall in that bracket. */
data class DayDistribution(
    val light: Int,    // < 50 words
    val moderate: Int, // 50–200 words
    val heavy: Int,    // 200–500 words
    val intense: Int   // 500+ words
)

/** How entries are distributed across times of day. */
data class TimeDistribution(
    val morning: Int,   // 05:00–11:59
    val afternoon: Int, // 12:00–16:59
    val evening: Int,   // 17:00–20:59
    val night: Int      // 21:00–04:59
)

/** The 6 sections below the overview cards that the user can reorder. */
enum class StatisticsSection(val displayName: String) {
    WRITING_INSIGHTS("Writing Insights"),
    DISTRIBUTION("Daily Writing Distribution"),
    WHEN_YOU_WRITE("When You Write"),
    MONTHLY_ACTIVITY("Monthly Activity"),
    HEATMAP("Writing Activity Heatmap"),
    LANGUAGES("Languages"),
    PEOPLE_PLACES("People & Places"),
    TEMPLATE_INSIGHTS("Structured Writing")
}

/** Number of non-reorderable items pinned above the section list in the LazyColumn. */
private const val STATS_FIXED_TOP = 6

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    viewModel: JournalViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToJournal: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToLookback: () -> Unit = {},
    onNavigateToShortcodes: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {}
) {
    val allTimeStats by viewModel.allTimeStats.collectAsStateWithLifecycle()
    val heatmapData by viewModel.heatmapData.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val datesWithEntries = uiState.datesWithEntries
    val highlightedDays = uiState.favoritedHighlights.size
    var selectedPeriod by remember { mutableStateOf(StatisticsPeriod.ALL_TIME) }

    val keywordIndexingIds by viewModel.keywordIndexingIds.collectAsStateWithLifecycle()
    val keywordMatchState by viewModel.keywordMatchState.collectAsStateWithLifecycle()
    val keywords by viewModel.keywords.collectAsStateWithLifecycle()
    val statisticsSettling by viewModel.statisticsSettling.collectAsStateWithLifecycle()

    // Section order — restored from prefs, falls back to default, new sections appended at end
    val savedSectionOrder by viewModel.statisticsSectionOrder.collectAsStateWithLifecycle()
    val sectionOrder = remember(savedSectionOrder) {
        val parsed = savedSectionOrder.mapNotNull { name ->
            runCatching { StatisticsSection.valueOf(name) }.getOrNull()
        }
        val allSections = StatisticsSection.values().toList()
        val withMissing = parsed + allSections.filter { it !in parsed }
        mutableStateListOf(*withMissing.toTypedArray())
    }

    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            val sectionFrom = from.index - STATS_FIXED_TOP
            val sectionTo   = to.index   - STATS_FIXED_TOP
            if (sectionFrom in 0 until sectionOrder.size &&
                sectionTo   in 0 until sectionOrder.size) {
                sectionOrder.add(sectionTo, sectionOrder.removeAt(sectionFrom))
                viewModel.setStatisticsSectionOrder(sectionOrder.map { it.name })
            }
        }
    )
    var customStartDate by remember { mutableStateOf(LocalDate.now().minusYears(1)) }
    var customEndDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val yesterday = remember { today.minusDays(1) }
    val todayMillis = remember { today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    val yesterdayMillis = remember { yesterday.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }

    // --- Start date picker: only dates up to yesterday ---
    val startSelectableDates = remember(yesterdayMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis <= yesterdayMillis
            override fun isSelectableYear(year: Int): Boolean =
                year <= yesterday.year
        }
    }
    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = customStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = IntRange(2000, yesterday.year),
        selectableDates = startSelectableDates
    )
    // Hard guard: if M3 ever allows a bad selection, immediately revert it
    LaunchedEffect(startDatePickerState.selectedDateMillis) {
        val sel = startDatePickerState.selectedDateMillis ?: return@LaunchedEffect
        if (sel > yesterdayMillis) {
            startDatePickerState.selectedDateMillis = yesterdayMillis
        }
    }

    // --- End date picker: only dates from start date to today ---
    val endDatePickerState = key(customStartDate) {
        val fromMillis = customStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        rememberDatePickerState(
            initialSelectedDateMillis = customEndDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = IntRange(customStartDate.year, today.year),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis in fromMillis..todayMillis
                override fun isSelectableYear(year: Int): Boolean =
                    year in customStartDate.year..today.year
            }
        )
    }
    // Hard guard for end date
    LaunchedEffect(endDatePickerState.selectedDateMillis, customStartDate) {
        val sel = endDatePickerState.selectedDateMillis ?: return@LaunchedEffect
        val fromMillis = customStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        if (sel < fromMillis) {
            endDatePickerState.selectedDateMillis = fromMillis
        } else if (sel > todayMillis) {
            endDatePickerState.selectedDateMillis = todayMillis
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_nav_statistics_title),
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (allTimeStats == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AppScreenReveal(
                visible = true,
                key = selectedPeriod.name,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "period_selector") {
                    PeriodSelector(
                        selectedPeriod = selectedPeriod,
                        onPeriodChange = { period ->
                            selectedPeriod = period
                            if (period == StatisticsPeriod.CUSTOM) {
                                showStartDatePicker = true
                            } else {
                                viewModel.calculateStatsByPeriod(period)
                                viewModel.updateHeatmapData()
                            }
                        }
                    )
                }

                item(key = "period_title") {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            selectedPeriod.titleText(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (selectedPeriod == StatisticsPeriod.CUSTOM) {
                            val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
                            Text(
                                "${customStartDate.format(fmt)} — ${customEndDate.format(fmt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // Overview Cards (fixed, non-reorderable)
                item(key = "overview_1") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatisticCard(
                            icon = Icons.AutoMirrored.Rounded.Note,
                            title = stringResource(R.string.statistics_total_entries),
                            value = allTimeStats!!.totalEntries.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatisticCard(
                            icon = Icons.Rounded.Edit,
                            title = stringResource(R.string.statistics_total_words),
                            value = allTimeStats!!.totalWords.toString(),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item(key = "overview_2") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatisticCard(
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            title = stringResource(R.string.statistics_avg_words_per_entry),
                            value = String.format("%.1f", allTimeStats!!.averageWordsPerEntry),
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        StatisticCard(
                            icon = Icons.Rounded.LocalFireDepartment,
                            title = stringResource(R.string.statistics_current_streak),
                            value = pluralStringResource(R.plurals.statistics_days_count, allTimeStats!!.currentStreak, allTimeStats!!.currentStreak),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item(key = "overview_3") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatisticCard(
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            title = stringResource(R.string.statistics_pages_written),
                            value = "~ ${(allTimeStats!!.totalWords / 250).coerceAtLeast(0)}",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatisticCard(
                            icon = Icons.Rounded.Schedule,
                            title = stringResource(R.string.statistics_reading_time),
                            value = pluralStringResource(
                                R.plurals.statistics_minutes_count,
                                estimateReadingTimeMinutes(allTimeStats!!.totalWords),
                                estimateReadingTimeMinutes(allTimeStats!!.totalWords)
                            ),
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item(key = "overview_4") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatisticCard(
                            icon = Icons.Rounded.Star,
                            title = stringResource(R.string.statistics_days_highlighted),
                            value = highlightedDays.toString(),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        StatisticCard(
                            icon = Icons.Rounded.HourglassBottom,
                            title = stringResource(R.string.statistics_avg_reading_time_per_entry),
                            value = pluralStringResource(
                                R.plurals.statistics_minutes_count,
                                estimateReadingTimeMinutes(allTimeStats!!.averageWordsPerEntry.toInt()),
                                estimateReadingTimeMinutes(allTimeStats!!.averageWordsPerEntry.toInt())
                            ),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Reorderable sections — long-press the ≡ handle to drag
                items(
                    items = sectionOrder,
                    key = { it }
                ) { section ->
                    ReorderableItem(
                        state = reorderState,
                        key = section
                    ) { isDragging ->
                        val dragElevation by animateDpAsState(
                            targetValue = if (isDragging) 6.dp else 0.dp,
                            label = "drag_elevation"
                        )
                        StatisticsSectionContainer(
                            section = section,
                            entranceTriggered = true,
                            entranceIndex = sectionOrder.indexOf(section) + STATS_FIXED_TOP,
                            haptics = haptics,
                            viewModel = viewModel,
                            allTimeStats = allTimeStats!!,
                            datesWithEntries = datesWithEntries,
                            heatmapData = heatmapData,
                            statisticsSettling = statisticsSettling,
                            keywordIndexingIds = keywordIndexingIds,
                            keywordMatchState = keywordMatchState,
                            keywords = keywords,
                            containerModifier = Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                .graphicsLayer { shadowElevation = dragElevation.toPx() },
                            dragHandleModifier = Modifier
                                .size(20.dp)
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                        )
                    }
                }

                item(key = "spacer") {
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
            }
        }
    }

    // Calculate all-time stats and heatmap data on screen load
    LaunchedEffect(Unit) {
        viewModel.calculateStatsByPeriod(StatisticsPeriod.ALL_TIME)
        viewModel.updateHeatmapData()
    }

    // Step 1: Pick start date
    if (showStartDatePicker) {
        val startConfirmEnabled = startDatePickerState.selectedDateMillis
            ?.let { it <= yesterdayMillis } == true
        DatePickerDialog(
            onDismissRequest = {
                showStartDatePicker = false
                selectedPeriod = StatisticsPeriod.ALL_TIME
            },
            confirmButton = {
                TextButton(
                    enabled = startConfirmEnabled,
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { millis ->
                            // Double-validate: only accept dates up to yesterday
                            if (millis <= yesterdayMillis) {
                                customStartDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC).toLocalDate()
                            }
                        }
                        showStartDatePicker = false
                        showEndDatePicker = true
                    }
                ) { Text(stringResource(R.string.action_next)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showStartDatePicker = false
                    selectedPeriod = StatisticsPeriod.ALL_TIME
                }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(
                state = startDatePickerState,
                showModeToggle = false,  // hide text-input mode — bypasses SelectableDates
                headline = {
                    Text(
                        stringResource(R.string.statistics_select_start_date),
                        modifier = androidx.compose.ui.Modifier.padding(start = 24.dp, bottom = 8.dp)
                    )
                }
            )
        }
    }

    // Step 2: Pick end date, then apply
    if (showEndDatePicker) {
        val fromMillis = customStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endConfirmEnabled = endDatePickerState.selectedDateMillis
            ?.let { it in fromMillis..todayMillis } == true
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = endConfirmEnabled,
                    onClick = {
                        endDatePickerState.selectedDateMillis?.let { millis ->
                            // Double-validate: only accept dates in [startDate, today]
                            if (millis in fromMillis..todayMillis) {
                                customEndDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC).toLocalDate()
                            }
                        }
                        showEndDatePicker = false
                        viewModel.calculateStatsByPeriod(StatisticsPeriod.CUSTOM, customStartDate, customEndDate)
                        viewModel.updateHeatmapData()
                    }
                ) { Text(stringResource(R.string.action_apply)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(
                state = endDatePickerState,
                showModeToggle = false,  // hide text-input mode — bypasses SelectableDates
                headline = {
                    Text(
                        stringResource(R.string.statistics_select_end_date),
                        modifier = androidx.compose.ui.Modifier.padding(start = 24.dp, bottom = 8.dp)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodSelector(
    selectedPeriod: StatisticsPeriod,
    onPeriodChange: (StatisticsPeriod) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.statistics_filter_by_period),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                StatisticsPeriod.ALL_TIME to stringResource(R.string.statistics_period_chip_all_time),
                StatisticsPeriod.CURRENT_YEAR to stringResource(R.string.statistics_period_chip_current_year),
                StatisticsPeriod.PREVIOUS_YEAR to stringResource(R.string.statistics_period_chip_previous_year),
                StatisticsPeriod.CURRENT_MONTH to stringResource(R.string.statistics_period_chip_current_month),
                StatisticsPeriod.PREVIOUS_MONTH to stringResource(R.string.statistics_period_chip_previous_month),
                StatisticsPeriod.CUSTOM to stringResource(R.string.statistics_period_chip_custom)
            ).forEach { (period, label) ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { onPeriodChange(period) },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
private fun StatisticsPeriod.titleText(): String =
    when (this) {
        StatisticsPeriod.ALL_TIME -> stringResource(R.string.statistics_period_all_time)
        StatisticsPeriod.CURRENT_YEAR -> stringResource(R.string.statistics_period_current_year)
        StatisticsPeriod.PREVIOUS_YEAR -> stringResource(R.string.statistics_period_previous_year)
        StatisticsPeriod.CURRENT_MONTH -> stringResource(R.string.statistics_period_current_month)
        StatisticsPeriod.PREVIOUS_MONTH -> stringResource(R.string.statistics_period_previous_month)
        StatisticsPeriod.CUSTOM -> stringResource(R.string.statistics_period_custom)
    }

@Composable
private fun StatisticCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}



