package com.mj.yaja.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.ui.viewmodel.JournalViewModel
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
    val writingTimeDistribution: TimeDistribution // when during the day the user writes
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
    var customStartDate by remember { mutableStateOf(LocalDate.now().minusYears(1)) }
    var customEndDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = customStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = customEndDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
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

                item {
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

                // Overview Cards (2x2 grid)
                item {
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

                item {
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

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatisticCard(
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            title = "Pages Written",
                            value = "~${(allTimeStats!!.totalWords / 250).coerceAtLeast(0)}",
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

                // Detailed Stats Section
                item {
                    Text(
                        "Writing Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Longest Streak
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Rounded.EmojiEvents,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Longest Streak",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${allTimeStats!!.longestStreakAllTime} days",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

                            // Most Active Day
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Rounded.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Most Active Day",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            allTimeStats!!.mostActiveDay ?: "—",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

                            // Writing Consistency
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Writing Consistency",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${String.format("%.0f", allTimeStats!!.writingConsistencyScore)}%",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

                            // Days with Entries
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Rounded.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Days With Entries",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            allTimeStats!!.totalDaysWithEntries.toString(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

                            // Best Month
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Best Month",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val best = allTimeStats!!
                                        Text(
                                            if (best.bestMonthLabel != null)
                                                "${best.bestMonthLabel}  ·  ${best.bestMonthCount} entries"
                                            else "—",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

                            // Average Days per Week
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Rounded.CalendarViewWeek,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Avg. Writing Days / Week",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            String.format("%.1f days", allTimeStats!!.averageDaysPerWeek),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily Writing Distribution
                item {
                    Text(
                        "Daily Writing Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val dist = allTimeStats!!.entriesByLength
                            val total = dist.light + dist.moderate + dist.heavy + dist.intense

                            fun getPercentage(count: Int): Float =
                                if (total == 0) 0f else (count.toFloat() / total) * 100

                            Text(
                                text = "Based on total words written per day",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            EntryLengthBar(
                                label = "Light  (< 50 words)",
                                count = dist.light,
                                percentage = getPercentage(dist.light),
                                color = MaterialTheme.colorScheme.primary
                            )

                            EntryLengthBar(
                                label = "Moderate  (50–200 words)",
                                count = dist.moderate,
                                percentage = getPercentage(dist.moderate),
                                color = MaterialTheme.colorScheme.secondary
                            )

                            EntryLengthBar(
                                label = "Heavy  (200–500 words)",
                                count = dist.heavy,
                                percentage = getPercentage(dist.heavy),
                                color = MaterialTheme.colorScheme.tertiary
                            )

                            EntryLengthBar(
                                label = "Intense  (500+ words)",
                                count = dist.intense,
                                percentage = getPercentage(dist.intense),
                                color = MaterialTheme.colorScheme.error // deep orange — complements the green theme
                            )
                        }
                    }
                }

                // When You Write — time of day distribution
                item {
                    Text(
                        "When You Write",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    WritingTimeCard(dist = allTimeStats!!.writingTimeDistribution)
                }

                // Monthly Activity — bar chart of the last 12 months
                item {
                    Text(
                        "Monthly Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    MonthlyActivityChart(trend = allTimeStats!!.monthlyEntryTrend)
                }

                item {
                    Text(
                        "Writing Activity Heatmap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    EntryHeatmap(
                        datesWithEntries = datesWithEntries,
                        entryLengthMap = heatmapData
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
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
        DatePickerDialog(
            onDismissRequest = {
                showStartDatePicker = false
                // If user cancels without picking, revert period selection
                selectedPeriod = StatisticsPeriod.ALL_TIME
            },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { millis ->
                        customStartDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showStartDatePicker = false
                    showEndDatePicker = true  // Chain to step 2
                }) { Text("Next") }
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
                headline = { Text("Select Start Date", modifier = androidx.compose.ui.Modifier.padding(start = 24.dp, bottom = 8.dp)) }
            )
        }
    }

    // Step 2: Pick end date, then apply
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { millis ->
                        customEndDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showEndDatePicker = false
                    viewModel.calculateStatsByPeriod(StatisticsPeriod.CUSTOM, customStartDate, customEndDate)
                    viewModel.updateHeatmapData()
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = endDatePickerState,
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
                text = "Based on entry timestamps",
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
                    val monthAbbrev = try {
                        val m = java.time.Month.of(monthKey.split("-")[1].toInt())
                        m.name[0] + m.name.substring(1, 3).lowercase()
                    } catch (e: Exception) { "?" }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
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
                                        if (count > 0) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                    )
                            )
                            // Show count above bar when it's tall enough to read
                            if (count > 0 && fraction >= 0.18f) {
                                Text(
                                    count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(bottom = 2.dp)
                                )
                            }
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
