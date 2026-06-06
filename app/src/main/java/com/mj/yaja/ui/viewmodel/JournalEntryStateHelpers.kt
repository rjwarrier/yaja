package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.DueRevisitItem
import com.mj.yaja.data.MarkdownFileManager
import java.time.LocalDate

internal data class LoadedDateState(
    val entries: List<String>,
    val dayLabel: String,
    val revisitDate: LocalDate?,
    val revisitNote: String,
    val dueRevisits: List<DueRevisitItem>
)

internal data class StarredStateSnapshot(
    val starredDates: Set<LocalDate>,
    val favoritedDates: Set<String>,
    val starredLabels: Map<LocalDate, String>
)

internal fun loadDateStateSnapshot(
    fileManager: MarkdownFileManager,
    date: LocalDate
): LoadedDateState =
    LoadedDateState(
        entries = fileManager.getEntriesForDate(date),
        dayLabel = fileManager.getDayLabel(date),
        revisitDate = fileManager.getRevisitDate(date),
        revisitNote = fileManager.getRevisitNote(date),
        dueRevisits = fileManager.getDueRevisitItems(date)
    )

internal fun loadStarredStateSnapshot(
    fileManager: MarkdownFileManager
): StarredStateSnapshot {
    val starredDates = fileManager.getStarredDates().toSet()
    return StarredStateSnapshot(
        starredDates = starredDates,
        favoritedDates = starredDates.mapTo(linkedSetOf()) { it.toString() },
        starredLabels = fileManager.getAllStarredLabels()
    )
}
