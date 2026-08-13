package com.mj.yaja.ui.screens

import androidx.annotation.StringRes
import com.mj.yaja.R
import com.mj.yaja.data.DailyJournalMetrics
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs

enum class TimelineFilter(@param:StringRes val labelRes: Int) {
    ALL(R.string.timeline_filter_all),
    FAVORITES(R.string.timeline_filter_favorites),
    LABELED(R.string.timeline_filter_labeled),
    WITH_ENTRIES(R.string.timeline_filter_with_entries)
}

enum class TimelineDensity(@param:StringRes val labelRes: Int, val trackHeight: Int, val verticalPadding: Int) {
    COMFORTABLE(R.string.timeline_density_comfortable, 64, 6),
    COMPACT(R.string.timeline_density_compact, 50, 2)
}

enum class TimelineStyle(@param:StringRes val labelRes: Int) {
    TRACK(R.string.timeline_style_track),
    CARD(R.string.timeline_style_card)
}

const val ALL_YEARS = "__all_years"

data class TimelineDateNode(
    val date: LocalDate,
    val label: String?,
    val hasEntries: Boolean,
    val entryCount: Int,
    val isFavorite: Boolean,
    val hasFollowUp: Boolean,
    val isToday: Boolean
)

data class TimelineMonthSection(
    val yearMonth: YearMonth,
    val nodes: List<TimelineDateNode>,
    val entryDayCount: Int,
    val labeledCount: Int,
    val favoriteCount: Int
)

fun buildKnownTimelineDates(
    datesWithEntries: Collection<LocalDate>,
    starredLabels: Map<LocalDate, String>,
    revisitTargetDates: Set<LocalDate>,
    today: LocalDate
): List<LocalDate> {
    val entryDates = datesWithEntries.filterNot { it.isAfter(today) }.toSet()
    val labeledDates = starredLabels.keys.filterNot { it.isAfter(today) }.toSet()
    return (entryDates + labeledDates + revisitTargetDates).sortedDescending()
}

fun buildTimelineDates(
    knownDates: List<LocalDate>,
    showAllDates: Boolean,
    latestTimelineDate: LocalDate
): List<LocalDate> {
    return if (showAllDates && knownDates.isNotEmpty()) {
        generateSequence(knownDates.last()) { current ->
            current.plusDays(1).takeIf { !it.isAfter(latestTimelineDate) }
        }.toList()
    } else {
        knownDates
    }
}

fun buildTimelineMonthSections(
    timelineDates: List<LocalDate>,
    datesWithEntriesSource: Collection<LocalDate>,
    metricsByDate: Map<LocalDate, DailyJournalMetrics>,
    favoritedDates: Set<String>,
    starredLabels: Map<LocalDate, String>,
    revisitTargetDates: Set<LocalDate>,
    selectedFilter: TimelineFilter,
    selectedYear: String,
    labelQuery: String,
    today: LocalDate
): List<TimelineMonthSection> {
    val datesWithEntries = datesWithEntriesSource.filterNot { it.isAfter(today) }.toSet()
    val normalizedQuery = labelQuery.trim().lowercase(Locale.getDefault())

    return timelineDates
        .asSequence()
        .filter { date ->
            when (selectedFilter) {
                TimelineFilter.ALL -> true
                TimelineFilter.FAVORITES -> favoritedDates.contains(date.toString())
                TimelineFilter.LABELED -> !starredLabels[date].isNullOrBlank()
                TimelineFilter.WITH_ENTRIES -> datesWithEntries.contains(date)
            }
        }
        .filter { date -> selectedYear == ALL_YEARS || date.year.toString() == selectedYear }
        .filter { date ->
            normalizedQuery.isBlank() ||
                starredLabels[date].orEmpty().lowercase(Locale.getDefault()).contains(normalizedQuery)
        }
        .sortedDescending()
        .groupBy { YearMonth.from(it) }
        .map { (yearMonth, dates) ->
            val nodes = dates.map { date ->
                val metrics = metricsByDate[date]
                TimelineDateNode(
                    date = date,
                    label = starredLabels[date]?.takeIf { it.isNotBlank() },
                    hasEntries = datesWithEntries.contains(date),
                    entryCount = metrics?.entryCount ?: if (datesWithEntries.contains(date)) 1 else 0,
                    isFavorite = favoritedDates.contains(date.toString()),
                    hasFollowUp = revisitTargetDates.contains(date),
                    isToday = date == today
                )
            }
            TimelineMonthSection(
                yearMonth = yearMonth,
                nodes = nodes,
                entryDayCount = nodes.count { it.hasEntries },
                labeledCount = nodes.count { !it.label.isNullOrBlank() },
                favoriteCount = nodes.count { it.isFavorite }
            )
        }
}

fun findNearestVisibleDate(dates: List<LocalDate>, targetDate: LocalDate): LocalDate? =
    dates.minByOrNull { date -> abs(java.time.temporal.ChronoUnit.DAYS.between(date, targetDate).toInt()) }

fun findDateItemIndex(
    sections: List<TimelineMonthSection>,
    collapsedMonths: Map<YearMonth, Boolean>,
    targetDate: LocalDate
): Int? {
    var index = 0
    sections.forEach { section ->
        index += 1
        if (collapsedMonths[section.yearMonth] == true) return@forEach
        section.nodes.forEach { node ->
            if (node.date == targetDate) return index
            index += 1
        }
    }
    return null
}

fun findMonthHeaderIndex(
    sections: List<TimelineMonthSection>,
    targetMonth: YearMonth
): Int? {
    var index = 0
    sections.forEach { section ->
        if (section.yearMonth == targetMonth) return index
        index += 1 + section.nodes.size
    }
    return null
}
