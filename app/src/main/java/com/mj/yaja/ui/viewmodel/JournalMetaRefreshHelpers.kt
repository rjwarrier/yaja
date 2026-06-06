package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.DailyJournalMetrics
import com.mj.yaja.data.MarkdownFileManager
import java.time.LocalDate
import com.mj.yaja.ui.screens.MonthlyStatsData
import java.time.DayOfWeek
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal data class JournalMetaRefreshSnapshot(
    val starredState: StarredStateSnapshot,
    val revisitState: RevisitStateSnapshot
)

internal fun loadJournalMetaRefreshSnapshot(
    fileManager: MarkdownFileManager,
    selectedDate: LocalDate
): JournalMetaRefreshSnapshot =
    JournalMetaRefreshSnapshot(
        starredState = loadStarredStateSnapshot(fileManager),
        revisitState = loadRevisitStateSnapshot(fileManager, selectedDate)
    )

internal fun applyJournalMetaRefreshSnapshot(
    snapshot: JournalMetaRefreshSnapshot,
    starredDates: MutableStateFlow<Set<LocalDate>>,
    favoritedDates: MutableStateFlow<Set<String>>,
    starredLabels: MutableStateFlow<Map<LocalDate, String>>,
    revisitMarkers: MutableStateFlow<List<com.mj.yaja.data.RevisitMarker>>,
    revisitTargetDates: MutableStateFlow<Set<LocalDate>>,
    currentRevisitDate: MutableStateFlow<LocalDate?>,
    currentRevisitNote: MutableStateFlow<String>,
    dueRevisits: MutableStateFlow<List<com.mj.yaja.data.DueRevisitItem>>
) {
    starredDates.value = snapshot.starredState.starredDates
    favoritedDates.value = snapshot.starredState.favoritedDates
    starredLabels.value = snapshot.starredState.starredLabels
    applyRevisitStateSnapshot(
        snapshot = snapshot.revisitState,
        revisitMarkers = revisitMarkers,
        revisitTargetDates = revisitTargetDates,
        currentRevisitDate = currentRevisitDate,
        currentRevisitNote = currentRevisitNote,
        dueRevisits = dueRevisits
    )
}

internal fun refreshJournalMetaWorkflow(
    scope: CoroutineScope,
    fileManager: MarkdownFileManager,
    selectedDate: LocalDate,
    starredDates: MutableStateFlow<Set<LocalDate>>,
    favoritedDates: MutableStateFlow<Set<String>>,
    starredLabels: MutableStateFlow<Map<LocalDate, String>>,
    revisitMarkers: MutableStateFlow<List<com.mj.yaja.data.RevisitMarker>>,
    revisitTargetDates: MutableStateFlow<Set<LocalDate>>,
    currentRevisitDate: MutableStateFlow<LocalDate?>,
    currentRevisitNote: MutableStateFlow<String>,
    dueRevisits: MutableStateFlow<List<com.mj.yaja.data.DueRevisitItem>>
) {
    scope.launch(Dispatchers.IO) {
        val snapshot = loadJournalMetaRefreshSnapshot(fileManager, selectedDate)
        applyJournalMetaRefreshSnapshot(
            snapshot = snapshot,
            starredDates = starredDates,
            favoritedDates = favoritedDates,
            starredLabels = starredLabels,
            revisitMarkers = revisitMarkers,
            revisitTargetDates = revisitTargetDates,
            currentRevisitDate = currentRevisitDate,
            currentRevisitNote = currentRevisitNote,
            dueRevisits = dueRevisits
        )
    }
}

internal fun buildCurrentMonthStats(
    currentMonth: YearMonth,
    monthMetrics: Map<LocalDate, DailyJournalMetrics>
): MonthlyStatsData {
    val monthStart = currentMonth.atDay(1)
    val monthEnd = currentMonth.atEndOfMonth()

    var wordCount = 0
    var entriesCount = 0
    val dayEntryCounts = mutableMapOf<DayOfWeek, Int>()
    val dateCounts = mutableMapOf<LocalDate, Int>()

    var date = monthStart
    while (date <= monthEnd) {
        val metrics = monthMetrics[date]
        if (metrics != null && metrics.entryCount > 0) {
            entriesCount += metrics.entryCount
            wordCount += metrics.wordCount
            val dayOfWeek = date.dayOfWeek
            dayEntryCounts[dayOfWeek] = (dayEntryCounts[dayOfWeek] ?: 0) + metrics.entryCount
            dateCounts[date] = metrics.entryCount
        }
        date = date.plusDays(1)
    }

    val mostActiveDay = dayEntryCounts.maxByOrNull { it.value }?.key

    var longestStreak = 0
    var currentStreak = 0
    date = monthStart
    while (date <= monthEnd) {
        if (dateCounts.containsKey(date)) {
            currentStreak++
            longestStreak = maxOf(longestStreak, currentStreak)
        } else {
            currentStreak = 0
        }
        date = date.plusDays(1)
    }

    return MonthlyStatsData(
        entriesCount = entriesCount,
        wordCount = wordCount,
        mostActiveDay = mostActiveDay?.name,
        longestStreak = longestStreak
    )
}
