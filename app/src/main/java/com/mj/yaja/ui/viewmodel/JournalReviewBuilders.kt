package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.DailyJournalMetrics
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.KeywordMatch
import com.mj.yaja.data.KeywordType
import com.mj.yaja.ui.screens.ReviewDaySummary
import com.mj.yaja.ui.screens.ReviewKeywordSummary
import com.mj.yaja.ui.screens.ReviewMomentSummary
import com.mj.yaja.ui.screens.ReviewPeriodType
import com.mj.yaja.ui.screens.ReviewSummaryData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun buildReviewSummaryData(
    period: ReviewPeriodType,
    anchorDate: LocalDate,
    firstDayOfWeek: DayOfWeek,
    allJournalDates: List<LocalDate>,
    metricsSnapshotProvider: (List<LocalDate>) -> LinkedHashMap<LocalDate, DailyJournalMetrics>,
    entrySnapshotProvider: (List<LocalDate>) -> LinkedHashMap<LocalDate, List<String>>,
    allLabels: Map<LocalDate, String>,
    starredDates: List<LocalDate>,
    keywordsById: Map<String, KeywordDefinition>,
    matchesForDateProvider: (LocalDate) -> List<KeywordMatch>,
    detectScript: (String) -> String
): ReviewSummaryData {
    val startDate =
        when (period) {
            ReviewPeriodType.WEEKLY -> {
                var cursor = anchorDate
                while (cursor.dayOfWeek != firstDayOfWeek) {
                    cursor = cursor.minusDays(1)
                }
                cursor
            }
            ReviewPeriodType.MONTHLY -> anchorDate.withDayOfMonth(1)
        }
    val endDate =
        when (period) {
            ReviewPeriodType.WEEKLY -> startDate.plusDays(6)
            ReviewPeriodType.MONTHLY -> anchorDate.withDayOfMonth(anchorDate.lengthOfMonth())
        }

    val datesInRange =
        allJournalDates
            .filter { !it.isBefore(startDate) && !it.isAfter(endDate) }
            .sorted()
    val metricsSnapshot = metricsSnapshotProvider(datesInRange)
    val entrySnapshot = entrySnapshotProvider(datesInRange)
    val starredInRange =
        starredDates
            .filter { !it.isBefore(startDate) && !it.isAfter(endDate) }
            .sortedDescending()
    val labeledInRange =
        allLabels.entries
            .filter { !it.key.isBefore(startDate) && !it.key.isAfter(endDate) && it.value.isNotBlank() }
            .sortedByDescending { it.key }

    val peopleCounts = linkedMapOf<String, Int>()
    val placeCounts = linkedMapOf<String, Int>()
    val languageCounts = linkedMapOf<String, Int>()

    datesInRange.forEach { date ->
        matchesForDateProvider(date).forEach { match ->
            val keyword = keywordsById[match.keywordId] ?: return@forEach
            when (keyword.type) {
                KeywordType.PERSON -> peopleCounts[keyword.name] = (peopleCounts[keyword.name] ?: 0) + 1
                KeywordType.PLACE -> placeCounts[keyword.name] = (placeCounts[keyword.name] ?: 0) + 1
            }
        }

        entrySnapshot[date].orEmpty().forEach { entry ->
            val cleanText = entry.replace(Regex("<!--.*?-->\\n?"), "").trim()
            if (cleanText.isNotBlank()) {
                val detected = detectScript(cleanText)
                languageCounts[detected] = (languageCounts[detected] ?: 0) + 1
            }
        }
    }

    val totalEntries = metricsSnapshot.values.sumOf { it.entryCount }
    val totalWords = metricsSnapshot.values.sumOf { it.wordCount }
    val writingDays = metricsSnapshot.size

    val mostActiveDays =
        metricsSnapshot.entries
            .sortedWith(
                compareByDescending<Map.Entry<LocalDate, DailyJournalMetrics>> { it.value.wordCount }
                    .thenByDescending { it.value.entryCount }
                    .thenByDescending { it.key }
            )
            .take(3)
            .map { (date, metrics) ->
                ReviewDaySummary(
                    date = date,
                    entryCount = metrics.entryCount,
                    wordCount = metrics.wordCount,
                    label = allLabels[date].orEmpty()
                )
            }

    val favoriteMoments =
        starredInRange.take(5).map { date ->
            ReviewMomentSummary(
                date = date,
                label = allLabels[date].orEmpty()
            )
        }

    val labelsCreated =
        labeledInRange.take(6).map { (date, label) ->
            ReviewMomentSummary(
                date = date,
                label = label
            )
        }

    val longestStreak =
        if (datesInRange.isEmpty()) {
            0
        } else {
            var best = 0
            var current = 0
            var previous: LocalDate? = null
            datesInRange.forEach { date ->
                current =
                    if (previous?.plusDays(1) == date || previous == null) {
                        current + 1
                    } else {
                        1
                    }
                best = maxOf(best, current)
                previous = date
            }
            best
        }

    val longestGap =
        if (datesInRange.isEmpty()) {
            ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        } else {
            val points = listOf(startDate.minusDays(1)) + datesInRange + listOf(endDate.plusDays(1))
            points.zipWithNext()
                .maxOfOrNull { (left, right) ->
                    (ChronoUnit.DAYS.between(left, right).toInt() - 1).coerceAtLeast(0)
                } ?: 0
        }

    return ReviewSummaryData(
        period = period,
        startDate = startDate,
        endDate = endDate,
        totalEntries = totalEntries,
        totalWords = totalWords,
        writingDays = writingDays,
        longestStreak = longestStreak,
        longestGap = longestGap,
        mostActiveDays = mostActiveDays,
        topPeople = topReviewKeywordItems(peopleCounts),
        topPlaces = topReviewKeywordItems(placeCounts),
        favoriteMoments = favoriteMoments,
        labelsCreated = labelsCreated,
        languageDistribution = languageCounts.entries.sortedByDescending { it.value }.associate { it.toPair() }
    )
}

private fun topReviewKeywordItems(source: Map<String, Int>): List<ReviewKeywordSummary> =
    source.entries
        .sortedByDescending { it.value }
        .take(5)
        .map {
            ReviewKeywordSummary(
                name = it.key,
                count = it.value
            )
        }
