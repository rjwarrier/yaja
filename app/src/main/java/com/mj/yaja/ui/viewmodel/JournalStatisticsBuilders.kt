package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.EntryTemplates
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.KeywordMatch
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.DailyJournalMetrics
import com.mj.yaja.data.countWordsIgnoringChecklistMarkers
import com.mj.yaja.ui.screens.AllTimeStatsData
import com.mj.yaja.ui.screens.DayDistribution
import com.mj.yaja.ui.screens.KeywordDeltaData
import com.mj.yaja.ui.screens.StatsComparisonWindow
import com.mj.yaja.ui.screens.TemplateCategoryMixItem
import com.mj.yaja.ui.screens.TemplateInsightsData
import com.mj.yaja.ui.screens.TemplateUsageData
import com.mj.yaja.ui.screens.TimeDistribution
import com.mj.yaja.ui.screens.StatisticsPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.LinkedHashMap

internal data class StatisticsDateRange(
    val start: LocalDate?,
    val end: LocalDate?
)

internal data class EntryStatisticsAnalysis(
    val morningCount: Int = 0,
    val afternoonCount: Int = 0,
    val eveningCount: Int = 0,
    val nightCount: Int = 0,
    val mlKitText: String? = null,
    val detectedScript: String? = null
)

internal data class DayStatisticsAnalysis(
    val totalEntriesDelta: Int = 0,
    val totalWordsDelta: Int = 0,
    val dayOfWeek: DayOfWeek? = null,
    val monthKey: String? = null,
    val shortCountDelta: Int = 0,
    val mediumCountDelta: Int = 0,
    val longCountDelta: Int = 0,
    val intenseCountDelta: Int = 0,
    val morningCountDelta: Int = 0,
    val afternoonCountDelta: Int = 0,
    val eveningCountDelta: Int = 0,
    val nightCountDelta: Int = 0,
    val mlKitTexts: List<String> = emptyList(),
    val detectedScripts: List<String> = emptyList()
)

internal fun emptyAllTimeStatsSnapshot(): AllTimeStatsData =
    AllTimeStatsData(
        totalEntries = 0,
        totalWords = 0,
        averageWordsPerEntry = 0f,
        currentStreak = 0,
        longestStreakAllTime = 0,
        mostActiveDay = null,
        totalDaysWithEntries = 0,
        writingConsistencyScore = 0f,
        monthlyEntryTrend = emptyList(),
        entriesByLength = DayDistribution(0, 0, 0, 0),
        totalHighlightedDays = 0,
        bestMonthLabel = null,
        bestMonthCount = 0,
        averageDaysPerWeek = 0f,
        writingTimeDistribution = TimeDistribution(0, 0, 0, 0),
        languageDistribution = emptyMap(),
        templateInsights = null
    )

internal fun resolveStatisticsDateRange(
    period: StatisticsPeriod,
    startDate: LocalDate?,
    endDate: LocalDate?,
    now: LocalDate = LocalDate.now()
): StatisticsDateRange =
    when (period) {
        StatisticsPeriod.ALL_TIME -> StatisticsDateRange(start = null, end = null)
        StatisticsPeriod.CURRENT_YEAR -> StatisticsDateRange(
            start = LocalDate.of(now.year, 1, 1),
            end = LocalDate.of(now.year, 12, 31)
        )
        StatisticsPeriod.PREVIOUS_YEAR -> StatisticsDateRange(
            start = LocalDate.of(now.year - 1, 1, 1),
            end = LocalDate.of(now.year - 1, 12, 31)
        )
        StatisticsPeriod.CURRENT_MONTH -> StatisticsDateRange(
            start = LocalDate.of(now.year, now.monthValue, 1),
            end = LocalDate.of(now.year, now.monthValue, now.dayOfMonth)
        )
        StatisticsPeriod.PREVIOUS_MONTH -> {
            val prevMonth = now.minusMonths(1)
            val lastDay = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth())
            StatisticsDateRange(
                start = LocalDate.of(prevMonth.year, prevMonth.monthValue, 1),
                end = lastDay
            )
        }
        StatisticsPeriod.CUSTOM -> StatisticsDateRange(start = startDate, end = endDate)
    }

internal fun filterDatesForStatisticsRange(
    knownDates: Set<LocalDate>,
    range: StatisticsDateRange
): List<LocalDate> =
    knownDates
        .sorted()
        .filter { date ->
            (range.start == null || date >= range.start) &&
                (range.end == null || date <= range.end)
        }

internal fun analyzeEntryForStatistics(
    entry: String,
    useMLKit: Boolean,
    timeRegex: Regex,
    metadataRegex: Regex
): EntryStatisticsAnalysis {
    var morningCount = 0
    var afternoonCount = 0
    var eveningCount = 0
    var nightCount = 0

    val hourStr = timeRegex.find(entry)?.groupValues?.getOrNull(1)
    if (hourStr != null) {
        val hour = hourStr.toIntOrNull() ?: -1
        when (hour) {
            in 5..11 -> morningCount++
            in 12..16 -> afternoonCount++
            in 17..20 -> eveningCount++
            in 0..4, in 21..23 -> nightCount++
        }
    }

    val cleanText = entry.replace(metadataRegex, "").trim()
    if (cleanText.length < 10) {
        return EntryStatisticsAnalysis(
            morningCount = morningCount,
            afternoonCount = afternoonCount,
            eveningCount = eveningCount,
            nightCount = nightCount
        )
    }

    return if (useMLKit) {
        EntryStatisticsAnalysis(
            morningCount = morningCount,
            afternoonCount = afternoonCount,
            eveningCount = eveningCount,
            nightCount = nightCount,
            mlKitText = cleanText
        )
    } else {
        EntryStatisticsAnalysis(
            morningCount = morningCount,
            afternoonCount = afternoonCount,
            eveningCount = eveningCount,
            nightCount = nightCount,
            detectedScript = detectDominantScriptSnapshot(cleanText)
        )
    }
}

internal fun analyzeDayForStatistics(
    date: LocalDate,
    metrics: DailyJournalMetrics,
    entries: List<String>,
    useMLKit: Boolean,
    timeRegex: Regex,
    metadataRegex: Regex
): DayStatisticsAnalysis {
    if (entries.isEmpty() || metrics.entryCount <= 0) {
        return DayStatisticsAnalysis()
    }

    var morningCount = 0
    var afternoonCount = 0
    var eveningCount = 0
    var nightCount = 0
    val mlKitTexts = mutableListOf<String>()
    val detectedScripts = mutableListOf<String>()

    entries.forEach { entry ->
        val analysis = analyzeEntryForStatistics(
            entry = entry,
            useMLKit = useMLKit,
            timeRegex = timeRegex,
            metadataRegex = metadataRegex
        )
        morningCount += analysis.morningCount
        afternoonCount += analysis.afternoonCount
        eveningCount += analysis.eveningCount
        nightCount += analysis.nightCount
        analysis.mlKitText?.let { mlKitTexts += it }
        analysis.detectedScript?.let { detectedScripts += it }
    }

    return DayStatisticsAnalysis(
        totalEntriesDelta = metrics.entryCount,
        totalWordsDelta = metrics.wordCount,
        dayOfWeek = date.dayOfWeek,
        monthKey = "${date.year}-${date.monthValue.toString().padStart(2, '0')}",
        shortCountDelta = if (metrics.wordCount < 50) 1 else 0,
        mediumCountDelta = if (metrics.wordCount in 50..199) 1 else 0,
        longCountDelta = if (metrics.wordCount in 200..499) 1 else 0,
        intenseCountDelta = if (metrics.wordCount >= 500) 1 else 0,
        morningCountDelta = morningCount,
        afternoonCountDelta = afternoonCount,
        eveningCountDelta = eveningCount,
        nightCountDelta = nightCount,
        mlKitTexts = mlKitTexts,
        detectedScripts = detectedScripts
    )
}

internal fun prepareEntriesSnapshot(
    fileManager: MarkdownFileManager,
    dates: List<LocalDate>
): LinkedHashMap<LocalDate, List<String>> {
    if (dates.isEmpty()) return linkedMapOf()
    return LinkedHashMap<LocalDate, List<String>>().apply {
        dates.forEach { date ->
            val entries = fileManager.getEntriesForDate(date)
            if (entries.isNotEmpty()) put(date, entries)
        }
    }
}

internal fun prepareDailyMetricsSnapshot(
    fileManager: MarkdownFileManager,
    dates: List<LocalDate>
): LinkedHashMap<LocalDate, DailyJournalMetrics> {
    if (dates.isEmpty()) return linkedMapOf()
    return LinkedHashMap(fileManager.getDailyMetricsSnapshotForDates(dates))
}

private fun countWordsForStatistics(entries: List<String>): Int =
    countWordsIgnoringChecklistMarkers(entries)

internal fun buildAllTimeStatsSnapshot(
    statsDates: List<LocalDate>,
    statsDateSet: Set<LocalDate>,
    totalEntries: Int,
    totalWords: Int,
    dayEntryCounts: Map<DayOfWeek, Int>,
    monthEntryCounts: Map<String, Int>,
    shortCount: Int,
    mediumCount: Int,
    longCount: Int,
    intenseCount: Int,
    morningCount: Int,
    afternoonCount: Int,
    eveningCount: Int,
    nightCount: Int,
    favoritedInPeriod: Int,
    languageDistribution: Map<String, Int>,
    templateInsightsProvider: (Int) -> TemplateInsightsData?
): AllTimeStatsData {
    if (statsDates.isEmpty()) return emptyAllTimeStatsSnapshot()

    val mostActiveDay = dayEntryCounts.maxByOrNull { it.value }?.key?.name
    val averageWordsPerEntry = if (totalEntries > 0) totalWords.toFloat() / totalEntries else 0f

    var currentStreak = 0
    var tempDate = LocalDate.now()
    while (statsDateSet.contains(tempDate)) {
        currentStreak++
        tempDate = tempDate.minusDays(1)
    }

    var longestStreakAllTime = 0
    var currentStreakTemp = 0
    for (i in statsDates.indices) {
        val date = statsDates[i]
        val previousDate = if (i > 0) statsDates[i - 1] else null
        val isConsecutive = previousDate?.plusDays(1) == date || previousDate == null
        if (isConsecutive) {
            currentStreakTemp++
        } else {
            longestStreakAllTime = maxOf(longestStreakAllTime, currentStreakTemp)
            currentStreakTemp = 1
        }
    }
    longestStreakAllTime = maxOf(longestStreakAllTime, currentStreakTemp)

    val daysDiff = ChronoUnit.DAYS.between(statsDates.first(), statsDates.last()).toInt() + 1
    val consistencyScore = if (daysDiff > 0) {
        (statsDates.size.toFloat() / daysDiff) * 100
    } else {
        0f
    }

    val totalWeeks = (daysDiff / 7.0).coerceAtLeast(1.0)
    val averageDaysPerWeek = (statsDates.size.toFloat() / totalWeeks).toFloat()

    val bestMonthEntry = monthEntryCounts.maxByOrNull { it.value }
    val bestMonthLabel: String? = bestMonthEntry?.key?.let { key ->
        try {
            val parts = key.split("-")
            val ym = YearMonth.of(parts[0].toInt(), parts[1].toInt())
            ym.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"))
        } catch (_: Exception) {
            null
        }
    }
    val bestMonthCount = bestMonthEntry?.value ?: 0

    val monthlyTrend = mutableListOf<Pair<String, Int>>()
    val currentMonth = YearMonth.now()
    repeat(12) { i ->
        val month = currentMonth.minusMonths(i.toLong())
        val monthKey = "${month.year}-${month.monthValue.toString().padStart(2, '0')}"
        monthlyTrend.add(monthKey to (monthEntryCounts[monthKey] ?: 0))
    }
    monthlyTrend.reverse()

    return AllTimeStatsData(
        totalEntries = totalEntries,
        totalWords = totalWords,
        averageWordsPerEntry = averageWordsPerEntry,
        currentStreak = currentStreak,
        longestStreakAllTime = longestStreakAllTime,
        mostActiveDay = mostActiveDay,
        totalDaysWithEntries = statsDates.size,
        writingConsistencyScore = consistencyScore,
        monthlyEntryTrend = monthlyTrend,
        entriesByLength = DayDistribution(shortCount, mediumCount, longCount, intenseCount),
        totalHighlightedDays = favoritedInPeriod,
        bestMonthLabel = bestMonthLabel,
        bestMonthCount = bestMonthCount,
        averageDaysPerWeek = averageDaysPerWeek,
        writingTimeDistribution = TimeDistribution(
            morning = morningCount,
            afternoon = afternoonCount,
            evening = eveningCount,
            night = nightCount
        ),
        languageDistribution = languageDistribution,
        templateInsights = templateInsightsProvider(totalEntries)
    )
}

internal fun buildTemplateInsightsData(
    totalEntries: Int,
    usageCounts: Map<String, Int>,
    followUpCounts: Map<String, Int>
): TemplateInsightsData? {
    if (usageCounts.isEmpty()) return null

    val templatesById = EntryTemplates.builtIns.associateBy { it.id }
    val topTemplates =
        usageCounts.entries
            .sortedByDescending { it.value }
            .mapNotNull { (id, count) ->
                templatesById[id]?.let {
                    TemplateUsageData(
                        id = id,
                        name = it.name,
                        count = count
                    )
                }
            }
            .take(5)

    val categoryMix =
        usageCounts.entries
            .mapNotNull { (id, count) -> templatesById[id]?.category?.let { it to count } }
            .groupBy({ it.first }, { it.second })
            .map { (category, counts) ->
                TemplateCategoryMixItem(
                    category = category,
                    count = counts.sum()
                )
            }
            .sortedByDescending { it.count }

    val followUpLeaders =
        followUpCounts.entries
            .sortedByDescending { it.value }
            .mapNotNull { (id, count) ->
                templatesById[id]?.let {
                    TemplateUsageData(
                        id = id,
                        name = it.name,
                        count = count
                    )
                }
            }
            .take(3)

    val templateEntryCount = usageCounts.values.sum()
    val blankEntryCount = (totalEntries - templateEntryCount).coerceAtLeast(0)

    return TemplateInsightsData(
        topTemplates = topTemplates,
        categoryMix = categoryMix,
        followUpLeaders = followUpLeaders,
        templateEntryCount = templateEntryCount,
        blankEntryCount = blankEntryCount
    )
}

internal fun buildTemplateInsightsSnapshot(
    totalEntries: Int,
    usageCounts: Map<String, Int>,
    followUpCounts: Map<String, Int>
): TemplateInsightsData? =
    buildTemplateInsightsData(
        totalEntries = totalEntries,
        usageCounts = usageCounts,
        followUpCounts = followUpCounts
    )

internal fun detectDominantScriptSnapshot(text: String): String {
    val counts = mutableMapOf<Character.UnicodeScript, Int>()
    for (char in text) {
        if (char.isLetter()) {
            val script = Character.UnicodeScript.of(char.code)
            if (script != Character.UnicodeScript.COMMON && script != Character.UnicodeScript.INHERITED) {
                counts[script] = (counts[script] ?: 0) + 1
            }
        }
    }
    val dominant = counts.maxByOrNull { it.value }?.key ?: return "Not Detected"
    return when (dominant) {
        Character.UnicodeScript.LATIN -> "English"
        Character.UnicodeScript.MALAYALAM -> "Malayalam"
        Character.UnicodeScript.DEVANAGARI -> "Hindi"
        Character.UnicodeScript.ARABIC -> "Arabic"
        Character.UnicodeScript.CYRILLIC -> "Cyrillic"
        Character.UnicodeScript.TAMIL -> "Tamil"
        Character.UnicodeScript.TELUGU -> "Telugu"
        Character.UnicodeScript.KANNADA -> "Kannada"
        Character.UnicodeScript.BENGALI -> "Bengali"
        Character.UnicodeScript.GURMUKHI -> "Punjabi"
        Character.UnicodeScript.GUJARATI -> "Gujarati"
        Character.UnicodeScript.THAI -> "Thai"
        Character.UnicodeScript.HAN -> "Chinese"
        Character.UnicodeScript.HANGUL -> "Korean"
        Character.UnicodeScript.HIRAGANA -> "Japanese"
        Character.UnicodeScript.KATAKANA -> "Japanese"
        Character.UnicodeScript.GEORGIAN -> "Georgian"
        Character.UnicodeScript.ARMENIAN -> "Armenian"
        Character.UnicodeScript.ETHIOPIC -> "Ethiopic"
        Character.UnicodeScript.GREEK -> "Greek"
        Character.UnicodeScript.HEBREW -> "Hebrew"
        Character.UnicodeScript.MYANMAR -> "Myanmar"
        Character.UnicodeScript.SINHALA -> "Sinhala"
        Character.UnicodeScript.KHMER -> "Khmer"
        Character.UnicodeScript.LAO -> "Lao"
        Character.UnicodeScript.TIBETAN -> "Tibetan"
        Character.UnicodeScript.ORIYA -> "Odia"
        else -> dominant.name.lowercase().replaceFirstChar { it.uppercase() }
    }
}

internal fun buildLanguageDistributionSnapshot(
    languageCounts: Map<String, Int>,
    totalEntries: Int
): Map<String, Int> {
    val detected = languageCounts.filterKeys { it != "Not Detected" }
    val notDetectedCount = (totalEntries - detected.values.sum()).coerceAtLeast(0)
    val sorted =
        detected.entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
            .toMutableMap()
    if (notDetectedCount > 0) {
        sorted["Not Detected"] = notDetectedCount
    }
    return sorted
}

internal fun shouldPublishPartialStatisticsSnapshot(
    processedCount: Int,
    totalCount: Int
): Boolean =
    processedCount == totalCount ||
        processedCount == 1 ||
        processedCount % 120 == 0

internal fun buildStatisticsProgressSnapshot(
    processedCount: Int,
    totalCount: Int,
    useMLKit: Boolean
): Float =
    if (useMLKit) {
        processedCount.toFloat() / totalCount.toFloat() * 0.85f
    } else {
        processedCount.toFloat() / totalCount.toFloat()
    }

internal fun buildStatsComparisonWindowData(
    metricsSnapshot: Map<LocalDate, DailyJournalMetrics>,
    currentStart: LocalDate,
    currentEnd: LocalDate,
    previousStart: LocalDate,
    previousEnd: LocalDate,
    currentLabel: String,
    previousLabel: String
): StatsComparisonWindow {
    fun aggregate(start: LocalDate, end: LocalDate): Triple<Int, Int, Int> {
        val periodMetrics =
            metricsSnapshot
                .filterKeys { date -> !date.isBefore(start) && !date.isAfter(end) }
                .values
        val entries = periodMetrics.sumOf { it.entryCount }
        val words = periodMetrics.sumOf { it.wordCount }
        val writingDays = periodMetrics.count { it.entryCount > 0 }
        return Triple(entries, words, writingDays)
    }

    val (currentEntries, currentWords, currentDays) = aggregate(currentStart, currentEnd)
    val (previousEntries, previousWords, previousDays) = aggregate(previousStart, previousEnd)

    return StatsComparisonWindow(
        currentLabel = currentLabel,
        previousLabel = previousLabel,
        currentEntries = currentEntries,
        previousEntries = previousEntries,
        currentWords = currentWords,
        previousWords = previousWords,
        currentWritingDays = currentDays,
        previousWritingDays = previousDays
    )
}

internal fun buildStatisticsComparisonWindowSnapshot(
    metricsSnapshot: Map<LocalDate, DailyJournalMetrics>,
    currentStart: LocalDate,
    currentEnd: LocalDate,
    previousStart: LocalDate,
    previousEnd: LocalDate,
    currentLabel: String,
    previousLabel: String
): StatsComparisonWindow =
    buildStatsComparisonWindowData(
        metricsSnapshot = metricsSnapshot,
        currentStart = currentStart,
        currentEnd = currentEnd,
        previousStart = previousStart,
        previousEnd = previousEnd,
        currentLabel = currentLabel,
        previousLabel = previousLabel
    )

internal fun buildKeywordDeltaData(
    matchingKeywords: List<KeywordDefinition>,
    keywordMatchesProvider: (String) -> List<KeywordMatch>,
    currentStart: LocalDate,
    currentEnd: LocalDate,
    previousStart: LocalDate,
    previousEnd: LocalDate
): KeywordDeltaData? {
    if (matchingKeywords.isEmpty()) return null

    return matchingKeywords
        .map { keyword ->
            val matches = keywordMatchesProvider(keyword.id)
            val currentMentions =
                matches.count { match ->
                    val matchDate = runCatching { LocalDate.parse(match.date) }.getOrNull()
                    matchDate != null && !matchDate.isBefore(currentStart) && !matchDate.isAfter(currentEnd)
                }
            val previousMentions =
                matches.count { match ->
                    val matchDate = runCatching { LocalDate.parse(match.date) }.getOrNull()
                    matchDate != null && !matchDate.isBefore(previousStart) && !matchDate.isAfter(previousEnd)
                }
            KeywordDeltaData(
                name = keyword.name,
                currentMentions = currentMentions,
                previousMentions = previousMentions,
                delta = currentMentions - previousMentions
            )
        }
        .filter { it.currentMentions > 0 || it.previousMentions > 0 }
        .sortedWith(
            compareByDescending<KeywordDeltaData> { it.currentMentions }
                .thenByDescending { it.delta }
                .thenBy { it.name.lowercase() }
        )
        .firstOrNull()
}

internal fun buildKeywordDeltaSnapshot(
    type: com.mj.yaja.data.KeywordType,
    allKeywords: List<KeywordDefinition>,
    keywordMatchesProvider: (String) -> List<KeywordMatch>,
    currentStart: LocalDate,
    currentEnd: LocalDate,
    previousStart: LocalDate,
    previousEnd: LocalDate
): KeywordDeltaData? =
    buildKeywordDeltaData(
        matchingKeywords = allKeywords.filter { it.type == type && it.isEnabled },
        keywordMatchesProvider = keywordMatchesProvider,
        currentStart = currentStart,
        currentEnd = currentEnd,
        previousStart = previousStart,
        previousEnd = previousEnd
    )
