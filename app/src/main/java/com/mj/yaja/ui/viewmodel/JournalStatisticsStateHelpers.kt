package com.mj.yaja.ui.viewmodel

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.mj.yaja.data.DailyJournalMetrics
import com.mj.yaja.data.KeywordType
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.ui.screens.StatisticsComparisonData
import com.mj.yaja.ui.screens.KeywordDeltaData
import com.mj.yaja.ui.screens.MonthlyStatsData
import com.mj.yaja.ui.screens.StatsComparisonWindow
import com.mj.yaja.ui.screens.StatisticsPeriod
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.coroutines.resume

internal data class StatisticsLoadDecision(
    val requestKey: String,
    val shouldLoad: Boolean
)

internal fun buildStatisticsRequestKey(
    period: StatisticsPeriod,
    startDate: LocalDate?,
    endDate: LocalDate?,
    useMLKit: Boolean
): String =
    listOf(
        period.name,
        startDate?.toString().orEmpty(),
        endDate?.toString().orEmpty(),
        useMLKit.toString()
    ).joinToString("|")

internal fun buildStatisticsLoadDecision(
    period: StatisticsPeriod,
    startDate: LocalDate?,
    endDate: LocalDate?,
    useMLKit: Boolean,
    force: Boolean,
    hasStats: Boolean,
    isSettling: Boolean,
    lastRequestKey: String?,
    now: Long,
    lastCompletedAt: Long,
    freshnessMs: Long
): StatisticsLoadDecision {
    val requestKey = buildStatisticsRequestKey(
        period = period,
        startDate = startDate,
        endDate = endDate,
        useMLKit = useMLKit
    )
    return StatisticsLoadDecision(
        requestKey = requestKey,
        shouldLoad = shouldLoadStatistics(
            force = force,
            hasStats = hasStats,
            isSettling = isSettling,
            lastRequestKey = lastRequestKey,
            requestKey = requestKey,
            now = now,
            lastCompletedAt = lastCompletedAt,
            freshnessMs = freshnessMs
        )
    )
}

internal fun shouldLoadStatistics(
    force: Boolean,
    hasStats: Boolean,
    isSettling: Boolean,
    lastRequestKey: String?,
    requestKey: String,
    now: Long,
    lastCompletedAt: Long,
    freshnessMs: Long
): Boolean {
    if (force) return true
    if (isSettling && lastRequestKey == requestKey) return false
    return !hasStats ||
        lastRequestKey != requestKey ||
        now - lastCompletedAt >= freshnessMs
}

internal fun shouldLoadHeatmap(
    force: Boolean,
    hasHeatmap: Boolean,
    now: Long,
    lastRefreshAt: Long,
    freshnessMs: Long,
    heatmapJobActive: Boolean
): Boolean =
    force ||
        !hasHeatmap ||
        now - lastRefreshAt >= freshnessMs ||
        heatmapJobActive

internal fun shouldLoadStatisticsComparison(
    force: Boolean,
    currentComparison: StatisticsComparisonData?
): Boolean =
    force || currentComparison == null

internal suspend fun buildStatisticsComparisonData(
    knownDates: Set<LocalDate>,
    metricsSnapshotProvider: (List<LocalDate>) -> LinkedHashMap<LocalDate, DailyJournalMetrics>,
    comparisonWindowBuilder: (
        metricsSnapshot: Map<LocalDate, DailyJournalMetrics>,
        currentStart: LocalDate,
        currentEnd: LocalDate,
        previousStart: LocalDate,
        previousEnd: LocalDate,
        currentLabel: String,
        previousLabel: String
    ) -> StatsComparisonWindow,
    keywordDeltaBuilder: (
        type: KeywordType,
        currentStart: LocalDate,
        currentEnd: LocalDate,
        previousStart: LocalDate,
        previousEnd: LocalDate
    ) -> KeywordDeltaData?
): StatisticsComparisonData? {
    val sortedDates = knownDates.sorted()
    if (sortedDates.isEmpty()) return null

    val metricsSnapshot = metricsSnapshotProvider(sortedDates)
    if (metricsSnapshot.isEmpty()) return null

    val today = LocalDate.now()
    val currentMonth = YearMonth.from(today)
    val previousMonth = currentMonth.minusMonths(1)
    val currentMonthStart = currentMonth.atDay(1)
    val previousMonthStart = previousMonth.atDay(1)
    val previousMonthComparableEnd =
        previousMonth.atDay(minOf(today.dayOfMonth, previousMonth.lengthOfMonth()))
    val currentYearStart = LocalDate.of(today.year, 1, 1)
    val previousYearStart = LocalDate.of(today.year - 1, 1, 1)
    val previousYearComparableEnd = today.minusYears(1)

    val monthWindow =
        comparisonWindowBuilder(
            metricsSnapshot,
            currentMonthStart,
            today,
            previousMonthStart,
            previousMonthComparableEnd,
            "This month",
            "Last month"
        )
    val yearWindow =
        comparisonWindowBuilder(
            metricsSnapshot,
            currentYearStart,
            today,
            previousYearStart,
            previousYearComparableEnd,
            "This year",
            "Last year"
        )

    val topPersonDelta =
        keywordDeltaBuilder(
            KeywordType.PERSON,
            currentMonthStart,
            today,
            previousMonthStart,
            previousMonthComparableEnd
        )
    val topPlaceDelta =
        keywordDeltaBuilder(
            KeywordType.PLACE,
            currentMonthStart,
            today,
            previousMonthStart,
            previousMonthComparableEnd
        )

    return StatisticsComparisonData(
        monthWindow = monthWindow,
        yearWindow = yearWindow,
        topPersonDelta = topPersonDelta,
        topPlaceDelta = topPlaceDelta
    )
}

internal suspend fun buildStatisticsComparisonSnapshot(
    fileManager: MarkdownFileManager,
    knownDates: Set<LocalDate>,
    comparisonWindowBuilder: (
        metricsSnapshot: Map<LocalDate, DailyJournalMetrics>,
        currentStart: LocalDate,
        currentEnd: LocalDate,
        previousStart: LocalDate,
        previousEnd: LocalDate,
        currentLabel: String,
        previousLabel: String
    ) -> StatsComparisonWindow,
    keywordDeltaBuilder: (
        type: KeywordType,
        currentStart: LocalDate,
        currentEnd: LocalDate,
        previousStart: LocalDate,
        previousEnd: LocalDate
    ) -> KeywordDeltaData?
): StatisticsComparisonData? = withContext(Dispatchers.IO) {
    val dates =
        if (knownDates.isNotEmpty()) {
            knownDates
        } else {
            fileManager.getAllJournalDatesLightweight()
        }
    buildStatisticsComparisonData(
        knownDates = dates,
        metricsSnapshotProvider = { snapshotDates -> prepareDailyMetricsSnapshot(fileManager, snapshotDates) },
        comparisonWindowBuilder = comparisonWindowBuilder,
        keywordDeltaBuilder = keywordDeltaBuilder
    )
}

internal fun shouldRefreshMonthlyStats(
    force: Boolean,
    currentStats: MonthlyStatsData?,
    lastMonthlyStatsMonth: YearMonth?,
    currentMonth: YearMonth,
    now: Long,
    lastMonthlyStatsRefreshAt: Long,
    freshnessMs: Long
): Boolean =
    force ||
        currentStats == null ||
        lastMonthlyStatsMonth != currentMonth ||
        now - lastMonthlyStatsRefreshAt >= freshnessMs

internal suspend fun buildMonthlyStatsSnapshot(
    fileManager: MarkdownFileManager,
    currentMonth: YearMonth
): MonthlyStatsData = withContext(Dispatchers.IO) {
    val monthStart = currentMonth.atDay(1)
    val monthEnd = currentMonth.atEndOfMonth()
    val monthDates = buildList {
        var cursor = monthStart
        while (cursor <= monthEnd) {
            add(cursor)
            cursor = cursor.plusDays(1)
        }
    }
    val monthMetrics = prepareDailyMetricsSnapshot(fileManager, monthDates)
    buildCurrentMonthStats(
        currentMonth = currentMonth,
        monthMetrics = monthMetrics
    )
}

internal suspend fun buildHeatmapSnapshot(
    fileManager: MarkdownFileManager,
    knownDates: Set<LocalDate>
): Map<LocalDate, Int> = withContext(Dispatchers.IO) {
    val map = mutableMapOf<LocalDate, Int>()
    val dates =
        if (knownDates.isNotEmpty()) {
            knownDates
        } else {
            fileManager.getAllJournalDatesLightweight()
        }
    val metricsSnapshot = prepareDailyMetricsSnapshot(fileManager, dates.sorted())
    metricsSnapshot.forEach { (date, metrics) ->
        map[date] = metrics.wordCount
    }
    map
}

internal fun pickSurpriseJournalDate(
    datesWithEntries: Set<LocalDate>,
    today: LocalDate = LocalDate.now(),
    minimumPastDates: Int = 50
): LocalDate? {
    val candidates = datesWithEntries.filter { it.isBefore(today) }
    return if (candidates.size < minimumPastDates) null else candidates.random()
}

internal fun calculateMonthlyEntryStats(
    dates: Set<LocalDate>,
    currentMonth: YearMonth = YearMonth.now()
): List<Pair<YearMonth, Int>> {
    val countsByMonth = dates.groupingBy { YearMonth.from(it) }.eachCount()
    val stats = ArrayList<Pair<YearMonth, Int>>(12)

    for (i in 11 downTo 0) {
        val month = currentMonth.minusMonths(i.toLong())
        val writtenDays = countsByMonth[month] ?: 0
        val totalDays = month.lengthOfMonth()
        val coveragePercent =
            if (totalDays > 0) {
                ((writtenDays.toFloat() / totalDays.toFloat()) * 100f).toInt()
            } else {
                0
            }
        stats.add(month to coveragePercent)
    }
    return stats
}

internal fun calculateYearlyEntryStats(
    dates: Set<LocalDate>
): List<Pair<Int, Float>> {
    if (dates.isEmpty()) return emptyList()

    val countsByYear = dates.groupingBy { it.year }.eachCount()
    return countsByYear
        .toSortedMap()
        .map { (year, count) -> year to count.toFloat() }
}

internal fun refreshMonthlyStatsWorkflow(
    force: Boolean,
    currentStats: MonthlyStatsData?,
    lastMonthlyStatsMonth: YearMonth?,
    lastMonthlyStatsRefreshAt: Long,
    freshnessMs: Long,
    fileManager: MarkdownFileManager,
    monthlyStatsState: MutableStateFlow<MonthlyStatsData?>,
    scope: CoroutineScope,
    currentJob: Job?,
    onRefreshStarted: () -> Unit = {},
    onStatsLoaded: (YearMonth) -> Unit,
    logPerf: (String, Long) -> Unit
): Job? {
    val now = System.currentTimeMillis()
    val currentMonth = YearMonth.now()
    if (!shouldRefreshMonthlyStats(
            force = force,
            currentStats = currentStats,
            lastMonthlyStatsMonth = lastMonthlyStatsMonth,
            currentMonth = currentMonth,
            now = now,
            lastMonthlyStatsRefreshAt = lastMonthlyStatsRefreshAt,
            freshnessMs = freshnessMs
        )
    ) {
        return currentJob
    }

    currentJob?.cancel()
    onRefreshStarted()
    return scope.launch {
        val startedAt = System.currentTimeMillis()
        val stats = buildMonthlyStatsSnapshot(
            fileManager = fileManager,
            currentMonth = currentMonth
        )
        monthlyStatsState.value = stats
        onStatsLoaded(currentMonth)
        logPerf("monthlyStats", System.currentTimeMillis() - startedAt)
    }
}

internal suspend fun detectLanguageWithMlKitSnapshot(
    identifier: LanguageIdentifier,
    text: String,
    executor: Executor
): String? = suspendCancellableCoroutine { cont ->
    try {
        identifier.identifyLanguage(text)
            .addOnSuccessListener(executor) { code ->
                val name =
                    if (code == null || code == "und") {
                        null
                    } else {
                        Locale(code).getDisplayLanguage(Locale.ENGLISH)
                            .replaceFirstChar { it.uppercase() }
                    }
                if (cont.isActive) cont.resume(name)
            }
            .addOnFailureListener(executor) {
                if (cont.isActive) cont.resume(null)
            }
            .addOnCanceledListener(executor) {
                if (cont.isActive) cont.resume(null)
            }
    } catch (_: Exception) {
        if (cont.isActive) cont.resume(null)
    }
}

internal suspend fun buildMlKitLanguageDistributionSnapshot(
    texts: List<String>,
    totalEntries: Int,
    executor: Executor
): Map<String, Int> {
    val langIdentifier = LanguageIdentification.getClient(
        com.google.mlkit.nl.languageid.LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(0.95f)
            .build()
    )

    val langCounts = try {
        withContext(Dispatchers.IO) {
            val counts = mutableMapOf<String, Int>()
            for (text in texts) {
                val name = try {
                    detectLanguageWithMlKitSnapshot(
                        identifier = langIdentifier,
                        text = text.take(100),
                        executor = executor
                    )
                } catch (_: Exception) {
                    null
                }
                val label = name ?: detectDominantScriptSnapshot(text)
                counts[label] = (counts[label] ?: 0) + 1
            }
            counts
        }
    } finally {
        try {
            langIdentifier.close()
        } catch (_: Exception) {
        }
    }

    return buildLanguageDistributionSnapshot(
        languageCounts = langCounts,
        totalEntries = totalEntries
    )
}

internal fun buildFallbackLanguageDistributionSnapshot(
    texts: List<String>,
    totalEntries: Int
): Map<String, Int> {
    val fallbackCounts = mutableMapOf<String, Int>()
    texts.filter { it.length >= 10 }.forEach { text ->
        val script = detectDominantScriptSnapshot(text)
        fallbackCounts[script] = (fallbackCounts[script] ?: 0) + 1
    }
    return buildLanguageDistributionSnapshot(
        languageCounts = fallbackCounts,
        totalEntries = totalEntries
    )
}
