package com.mj.yaja.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val languageDistribution: Map<String, Int> // ISO 639-1 code → entry count, sorted by count desc
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
    LANGUAGES("Languages")
}

/** Number of non-reorderable items pinned above the section list in the LazyColumn. */
private const val STATS_FIXED_TOP = 5

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    viewModel: JournalViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToLookback: () -> Unit,
    onNavigateToShortcodes: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    val allTimeStats by viewModel.allTimeStats.collectAsState()
    val heatmapData by viewModel.heatmapData.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val datesWithEntries = uiState.datesWithEntries
    val highlightedDays = uiState.favoritedHighlights.size
    var selectedPeriod by remember { mutableStateOf(StatisticsPeriod.ALL_TIME) }

    // Section order — restored from prefs, falls back to default, new sections appended at end
    val savedSectionOrder by viewModel.statisticsSectionOrder.collectAsState()
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
                        "Statistics",
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
                            when (selectedPeriod) {
                                StatisticsPeriod.ALL_TIME -> "All-Time Statistics"
                                StatisticsPeriod.CURRENT_YEAR -> "Current Year Statistics"
                                StatisticsPeriod.PREVIOUS_YEAR -> "Previous Year Statistics"
                                StatisticsPeriod.CURRENT_MONTH -> "Current Month Statistics"
                                StatisticsPeriod.PREVIOUS_MONTH -> "Previous Month Statistics"
                                StatisticsPeriod.CUSTOM -> "Custom Period Statistics"
                            },
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
                            title = "Total Entries",
                            value = allTimeStats!!.totalEntries.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatisticCard(
                            icon = Icons.Rounded.Edit,
                            title = "Total Words",
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
                            title = "Avg. Words/Entry",
                            value = String.format("%.1f", allTimeStats!!.averageWordsPerEntry),
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        StatisticCard(
                            icon = Icons.Rounded.LocalFireDepartment,
                            title = "Current Streak",
                            value = "${allTimeStats!!.currentStreak} days",
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
                            title = "Pages Written",
                            value = "~ ${(allTimeStats!!.totalWords / 250).coerceAtLeast(0)}",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatisticCard(
                            icon = Icons.Rounded.Star,
                            title = "Days Highlighted",
                            value = highlightedDays.toString(),
                            color = MaterialTheme.colorScheme.secondary,
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                .graphicsLayer { shadowElevation = dragElevation.toPx() },
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Section header row — drag handle on the right
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    section.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.Rounded.DragIndicator,
                                    contentDescription = "Long press to reorder",
                                    tint = if (isDragging) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .longPressDraggableHandle(
                                            onDragStarted = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        )
                                )
                            }

                            // Section card — rendered by section type
                            when (section) {
                                StatisticsSection.WRITING_INSIGHTS ->
                                    WritingInsightsCard(stats = allTimeStats!!)
                                StatisticsSection.DISTRIBUTION ->
                                    WritingDistributionCard(stats = allTimeStats!!)
                                StatisticsSection.WHEN_YOU_WRITE ->
                                    WritingTimeCard(dist = allTimeStats!!.writingTimeDistribution)
                                StatisticsSection.MONTHLY_ACTIVITY ->
                                    MonthlyActivityChart(trend = allTimeStats!!.monthlyEntryTrend)
                                StatisticsSection.HEATMAP ->
                                    EntryHeatmap(
                                        datesWithEntries = datesWithEntries,
                                        entryLengthMap = heatmapData
                                    )
                                StatisticsSection.LANGUAGES -> {
                                    val useMLKit by viewModel.useMLKitDetection.collectAsState()
                                    LanguagesCard(
                                        distribution = allTimeStats!!.languageDistribution,
                                        useMLKitDetection = useMLKit,
                                        onToggleMLKit = { viewModel.setUseMLKitDetection(it) }
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "spacer") {
                    Spacer(modifier = Modifier.height(120.dp))
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
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showStartDatePicker = false
                    selectedPeriod = StatisticsPeriod.ALL_TIME
                }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = startDatePickerState,
                showModeToggle = false,  // hide text-input mode — bypasses SelectableDates
                headline = { Text("Select Start Date", modifier = androidx.compose.ui.Modifier.padding(start = 24.dp, bottom = 8.dp)) }
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
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = endDatePickerState,
                showModeToggle = false,  // hide text-input mode — bypasses SelectableDates
                headline = { Text("Select End Date", modifier = androidx.compose.ui.Modifier.padding(start = 24.dp, bottom = 8.dp)) }
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
            text = "Filter by Period",
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
                StatisticsPeriod.ALL_TIME to "All Time",
                StatisticsPeriod.CURRENT_YEAR to "Current Year",
                StatisticsPeriod.PREVIOUS_YEAR to "Prev Year",
                StatisticsPeriod.CURRENT_MONTH to "This Month",
                StatisticsPeriod.PREVIOUS_MONTH to "Prev Month",
                StatisticsPeriod.CUSTOM to "Custom"
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

@Composable
private fun EntryLengthBar(
    label: String,
    count: Int,
    percentage: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "$count (${String.format("%.0f", percentage)}%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage / 100f)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun WritingInsightsCard(stats: AllTimeStatsData) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            @Composable
            fun InsightRow(
                icon: androidx.compose.ui.graphics.vector.ImageVector,
                tint: androidx.compose.ui.graphics.Color,
                label: String,
                value: String
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            InsightRow(Icons.Rounded.EmojiEvents, MaterialTheme.colorScheme.primary,
                "Longest Streak", "${stats.longestStreakAllTime} days")
            HorizontalDivider()
            InsightRow(Icons.Rounded.DateRange, MaterialTheme.colorScheme.secondary,
                "Most Active Day", stats.mostActiveDay ?: "—")
            HorizontalDivider()
            InsightRow(Icons.Rounded.CheckCircle, MaterialTheme.colorScheme.tertiary,
                "Writing Consistency", "${String.format("%.0f", stats.writingConsistencyScore)}%")
            HorizontalDivider()
            InsightRow(Icons.Rounded.DateRange, MaterialTheme.colorScheme.error,
                "Days With Entries", stats.totalDaysWithEntries.toString())
            HorizontalDivider()
            InsightRow(Icons.Rounded.CalendarMonth, MaterialTheme.colorScheme.tertiary,
                "Best Month",
                if (stats.bestMonthLabel != null) "${stats.bestMonthLabel}  ·  ${stats.bestMonthCount} entries" else "—")
            HorizontalDivider()
            InsightRow(Icons.Rounded.CalendarViewWeek, MaterialTheme.colorScheme.primary,
                "Avg. Writing Days / Week",
                String.format("%.1f days", stats.averageDaysPerWeek))
        }
    }
}

@Composable
private fun WritingDistributionCard(stats: AllTimeStatsData) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val dist = stats.entriesByLength
            val total = dist.light + dist.moderate + dist.heavy + dist.intense
            fun pct(n: Int) = if (total == 0) 0f else n.toFloat() / total * 100f

            Text(
                text = "Based on total words written per day",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            EntryLengthBar("Light  (< 50 words)",     dist.light,    pct(dist.light),    MaterialTheme.colorScheme.primary)
            EntryLengthBar("Moderate  (50–200 words)", dist.moderate, pct(dist.moderate), MaterialTheme.colorScheme.secondary)
            EntryLengthBar("Heavy  (200–500 words)",   dist.heavy,    pct(dist.heavy),    MaterialTheme.colorScheme.tertiary)
            EntryLengthBar("Intense  (500+ words)",    dist.intense,  pct(dist.intense),  MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun LanguagesCard(
    distribution: Map<String, Int>,
    useMLKitDetection: Boolean,
    onToggleMLKit: (Boolean) -> Unit
) {
    val sorted = distribution.entries
        .sortedByDescending { it.value }
        .take(10)

    val totalEntries = distribution.values.sum().coerceAtLeast(1)
    val distinctLangs = distribution.size

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary line
            Text(
                text = if (distinctLangs == 0) "Not enough text to detect languages"
                       else "$distinctLangs ${if (distinctLangs == 1) "language" else "languages"} detected",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!useMLKitDetection) {
                Text(
                    text = "Latin scripts are shown as English",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // ML Kit toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Use more accurate detection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = useMLKitDetection,
                    onCheckedChange = onToggleMLKit
                )
            }

            if (sorted.isEmpty()) {
                Text(
                    "Write more entries to see language stats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val barColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary
                )
                sorted.forEachIndexed { index, (name, count) ->
                    val pct = count.toFloat() / totalEntries * 100f
                    val color = barColors[index.coerceAtMost(barColors.lastIndex)]
                    EntryLengthBar(
                        label = name,
                        count = count,
                        percentage = pct,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
private fun WritingTimeCard(dist: TimeDistribution) {
    val total = (dist.morning + dist.afternoon + dist.evening + dist.night).coerceAtLeast(1)
    fun pct(n: Int) = n.toFloat() / total * 100f

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Based on entry time",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            EntryLengthBar(
                label = "🌅  Morning  (5am–noon)",
                count = dist.morning,
                percentage = pct(dist.morning),
                color = MaterialTheme.colorScheme.primary
            )
            EntryLengthBar(
                label = "☀️  Afternoon  (noon–5pm)",
                count = dist.afternoon,
                percentage = pct(dist.afternoon),
                color = MaterialTheme.colorScheme.secondary
            )
            EntryLengthBar(
                label = "🌆  Evening  (5pm–9pm)",
                count = dist.evening,
                percentage = pct(dist.evening),
                color = MaterialTheme.colorScheme.tertiary
            )
            EntryLengthBar(
                label = "🌙  Night  (9pm–5am)",
                count = dist.night,
                percentage = pct(dist.night),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun MonthlyActivityChart(trend: List<Pair<String, Int>>) {
    if (trend.isEmpty()) return
    val maxCount = trend.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val barAreaHeight = 80.dp

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barAreaHeight + 32.dp), // bars + label row
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                trend.forEach { (monthKey, count) ->
                    val fraction = count.toFloat() / maxCount

                    // Parse year and month from monthKey (format: "YYYY-MM")
                    val (year, month) = try {
                        val parts = monthKey.split("-")
                        Pair(parts[0].toInt(), parts[1].toInt())
                    } catch (e: Exception) { Pair(2024, 1) }

                    // Check if this month has entries for all days
                    val maxDaysInMonth = try {
                        java.time.YearMonth.of(year, month).lengthOfMonth()
                    } catch (e: Exception) { 31 }
                    val isCompleteMonth = count >= maxDaysInMonth

                    val monthAbbrev = try {
                        val m = java.time.Month.of(month)
                        m.name[0] + m.name.substring(1, 3).lowercase()
                    } catch (e: Exception) { "?" }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Fixed-height bar container — bar grows upward from the bottom
                        Box(
                            modifier = Modifier
                                .height(barAreaHeight)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val barHeight = barAreaHeight * fraction.coerceAtLeast(
                                if (count > 0) 0.04f else 0f
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(barHeight)
                                    .background(
                                        when {
                                            count == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                            isCompleteMonth -> MaterialTheme.colorScheme.tertiary  // Perfect month
                                            else -> MaterialTheme.colorScheme.secondary           // Partial month
                                        },
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                        }
                        // Month abbreviation label
                        Text(
                            monthAbbrev,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryHeatmap(
    datesWithEntries: Set<LocalDate> = emptySet(),
    entryLengthMap: Map<LocalDate, Int> = emptyMap()
) {
    // Generate last 52 weeks of data for heatmap
    val today = LocalDate.now()
    val startDate = today.minusWeeks(51)

    val weeks = mutableListOf<List<LocalDate>>()
    var currentDate = startDate

    // Group dates by week (Sunday to Saturday)
    while (currentDate <= today) {
        val weekDates = mutableListOf<LocalDate>()
        for (i in 0..6) {
            if (currentDate <= today) {
                weekDates.add(currentDate)
                currentDate = currentDate.plusDays(1)
            }
        }
        if (weekDates.isNotEmpty()) {
            weeks.add(weekDates)
        }
    }

    val scrollState = rememberScrollState()

    // Scroll to the right to show recent dates once layout is complete
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Heatmap grid
            Row(
                modifier = Modifier.horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                weeks.forEach { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        week.forEach { date ->
                            // Get word count for this date
                            val wordCount = entryLengthMap[date] ?: 0
                            val hasEntry = datesWithEntries.contains(date)

                            // Color based on total words written that day
                            val cellColor = when {
                                !hasEntry -> MaterialTheme.colorScheme.surfaceVariant
                                wordCount < 50  -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)  // Light
                                wordCount < 200 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)  // Moderate
                                wordCount < 500 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)  // Heavy
                                else            -> MaterialTheme.colorScheme.error                                     // Intense 500+
                            }

                            Surface(
                                modifier = Modifier
                                    .size(12.dp),
                                shape = RoundedCornerShape(2.dp),
                                color = cellColor
                            ) {}
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}

                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ) {}

                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                ) {}

                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                ) {}

                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.error
                ) {}

                Text(
                    "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
