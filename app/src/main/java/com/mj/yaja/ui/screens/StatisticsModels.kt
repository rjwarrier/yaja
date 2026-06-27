package com.mj.yaja.ui.screens

data class MonthlyStatsData(
    val entriesCount: Int,
    val wordCount: Int,
    val mostActiveDay: String?,
    val longestStreak: Int
)

data class StatsComparisonWindow(
    val currentLabel: String,
    val previousLabel: String,
    val currentEntries: Int,
    val previousEntries: Int,
    val currentWords: Int,
    val previousWords: Int,
    val currentWritingDays: Int,
    val previousWritingDays: Int
)

data class KeywordDeltaData(
    val name: String,
    val currentMentions: Int,
    val previousMentions: Int,
    val delta: Int
)

data class StatisticsComparisonData(
    val monthWindow: StatsComparisonWindow,
    val yearWindow: StatsComparisonWindow,
    val topPersonDelta: KeywordDeltaData?,
    val topPlaceDelta: KeywordDeltaData?
)

data class TemplateUsageData(
    val id: String,
    val name: String,
    val count: Int
)

data class TemplateCategoryMixItem(
    val category: String,
    val count: Int
)

data class TemplateInsightsData(
    val topTemplates: List<TemplateUsageData>,
    val categoryMix: List<TemplateCategoryMixItem>,
    val followUpLeaders: List<TemplateUsageData>,
    val templateEntryCount: Int,
    val blankEntryCount: Int
)
