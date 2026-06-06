package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.HomeScreenSnapshot
import com.mj.yaja.data.DueRevisitItem
import com.mj.yaja.data.MarkdownFileManager
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun navigateToSelectedDate(
    date: LocalDate,
    uiState: MutableStateFlow<JournalUiState>,
    loadEntries: (LocalDate) -> Unit
) {
    uiState.update {
        it.copy(
            selectedDate = date,
            searchResults = emptyList()
        )
    }
    loadEntries(date)
}

internal fun refreshSelectedDateOnStartup(
    date: LocalDate,
    loadEntries: (LocalDate) -> Unit
) {
    loadEntries(date)
}

internal fun refreshSelectedDateOnResume(
    date: LocalDate,
    shouldForceDateRefresh: Boolean,
    loadEntries: (LocalDate, Boolean) -> Unit,
    refreshCalendarDates: (Boolean) -> Unit,
    markForcedDateRefresh: () -> Unit,
    refreshStarredLabels: () -> Unit
) {
    loadEntries(date, false)
    refreshCalendarDates(false)
    refreshStarredLabels()
}

internal fun shouldReloadCurrentTodayAfterExternalEntry(
    entryDate: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate = LocalDate.now()
): Boolean =
    entryDate == today && selectedDate == today

internal fun persistHomeSnapshotIfChanged(
    selectedDate: LocalDate,
    entries: List<String>,
    dayLabel: String,
    lastPersistedSnapshot: HomeScreenSnapshot?,
    persistSnapshot: (HomeScreenSnapshot) -> Unit,
    updateLastPersistedSnapshot: (HomeScreenSnapshot) -> Unit
) {
    val snapshot = HomeScreenSnapshot(
        selectedDate = selectedDate,
        entries = entries,
        dayLabel = dayLabel
    )
    if (snapshot == lastPersistedSnapshot) return
    persistSnapshot(snapshot)
    updateLastPersistedSnapshot(snapshot)
}

internal fun launchSelectedDateLoad(
    scope: CoroutineScope,
    fileManager: MarkdownFileManager,
    date: LocalDate,
    showLoading: Boolean,
    isRequestStillCurrent: () -> Boolean,
    uiState: MutableStateFlow<JournalUiState>,
    currentDayLabel: MutableStateFlow<String>,
    currentRevisitDate: MutableStateFlow<LocalDate?>,
    currentRevisitNote: MutableStateFlow<String>,
    dueRevisits: MutableStateFlow<List<DueRevisitItem>>,
    persistHomeScreenSnapshot: (LocalDate, List<String>, String) -> Unit,
    logPerf: (String, Long) -> Unit
): Job =
    scope.launch {
        val startedAt = System.currentTimeMillis()
        val loaded = withContext(Dispatchers.IO) {
            fileManager.revalidateDateCache(date)
            loadDateStateSnapshot(fileManager, date)
        }
        if (!isRequestStillCurrent()) {
            return@launch
        }
        currentDayLabel.value = loaded.dayLabel
        currentRevisitDate.value = loaded.revisitDate
        currentRevisitNote.value = loaded.revisitNote
        dueRevisits.value = loaded.dueRevisits
        uiState.update { current ->
            current.copy(
                entries = loaded.entries,
                isLoading = if (showLoading) false else current.isLoading
            )
        }
        persistHomeScreenSnapshot(
            date,
            loaded.entries,
            currentDayLabel.value
        )
        logPerf("loadEntries", System.currentTimeMillis() - startedAt)
    }
