package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.SettingsRepository
import java.time.LocalDate

internal data class DateLabelMutationResult(
    val selectedDayLabel: String?,
    val starredState: StarredStateSnapshot?,
    val starredLabels: Map<LocalDate, String>?
)

internal suspend fun setStarredWithLabelMutation(
    fileManager: MarkdownFileManager,
    settingsRepository: SettingsRepository,
    date: LocalDate,
    label: String,
    selectedDate: LocalDate
): DateLabelMutationResult {
    val truncatedLabel = label.take(30)
    val saved = fileManager.setStarred(date, true, truncatedLabel)
    if (saved) settingsRepository.setFavoritedDate(date, true)
    return DateLabelMutationResult(
        selectedDayLabel = fileManager.getDayLabel(date).takeIf { date == selectedDate },
        starredState = loadStarredStateSnapshot(fileManager),
        starredLabels = null
    )
}

internal suspend fun unsetStarredDateMutation(
    fileManager: MarkdownFileManager,
    settingsRepository: SettingsRepository,
    date: LocalDate,
    selectedDate: LocalDate
): DateLabelMutationResult {
    val saved = fileManager.setStarred(date, false, "")
    if (saved) settingsRepository.setFavoritedDate(date, false)
    return DateLabelMutationResult(
        selectedDayLabel = if (date == selectedDate) fileManager.getDayLabel(date) else null,
        starredState = loadStarredStateSnapshot(fileManager),
        starredLabels = null
    )
}

internal suspend fun applyDayLabelMutation(
    fileManager: MarkdownFileManager,
    date: LocalDate,
    label: String,
    selectedDate: LocalDate
): DateLabelMutationResult {
    fileManager.setDayLabel(date, label.take(30))
    return DateLabelMutationResult(
        selectedDayLabel = if (date == selectedDate) fileManager.getDayLabel(date) else null,
        starredState = null,
        starredLabels = fileManager.getAllStarredLabels()
    )
}
