package com.mj.yaja.ui.screens

import java.time.LocalDate

enum class ReviewPeriodType(val routeValue: String, val title: String) {
    WEEKLY("weekly", "Weekly Review"),
    MONTHLY("monthly", "Monthly Review");

    companion object {
        fun fromRoute(value: String?): ReviewPeriodType =
            entries.firstOrNull { it.routeValue.equals(value, ignoreCase = true) } ?: WEEKLY
    }
}

data class ReviewDaySummary(
    val date: LocalDate,
    val entryCount: Int,
    val wordCount: Int,
    val label: String
)

data class ReviewKeywordSummary(
    val name: String,
    val count: Int
)

data class ReviewMomentSummary(
    val date: LocalDate,
    val label: String
)

data class ReviewSummaryData(
    val period: ReviewPeriodType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalEntries: Int,
    val totalWords: Int,
    val writingDays: Int,
    val longestStreak: Int,
    val longestGap: Int,
    val mostActiveDays: List<ReviewDaySummary>,
    val topPeople: List<ReviewKeywordSummary>,
    val topPlaces: List<ReviewKeywordSummary>,
    val favoriteMoments: List<ReviewMomentSummary>,
    val labelsCreated: List<ReviewMomentSummary>,
    val languageDistribution: Map<String, Int>
)

internal data class ReviewDateRow(
    val date: LocalDate,
    val title: String,
    val subtitle: String
)
