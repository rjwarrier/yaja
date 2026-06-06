package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.MarkdownFileManager
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun launchLookbackRefresh(
    scope: CoroutineScope,
    date: LocalDate,
    uiState: MutableStateFlow<JournalUiState>,
    lookbackSnapshotCache: MutableMap<LocalDate, Map<Int, List<String>>>,
    fileManager: MarkdownFileManager
): Job =
    scope.launch {
        val availableDates =
            withContext(Dispatchers.IO) {
                fileManager.getAllJournalDatesLightweight(forceRefresh = true)
            }
        val lookbackMap = buildLookbackSnapshot(
            date = date,
            availableDates = availableDates,
            entriesForDateProvider = fileManager::getEntriesForDateFromDisk
        )
        synchronized(lookbackSnapshotCache) {
            lookbackSnapshotCache[date] = lookbackMap
        }
        uiState.update { it.copy(lookbackEntries = lookbackMap) }
    }

internal fun launchFavoritedHighlightsRefresh(
    scope: CoroutineScope,
    starredDates: Set<LocalDate>,
    uiState: MutableStateFlow<JournalUiState>
): Job =
    scope.launch {
        val highlights = buildFavoritedHighlights(starredDates)
        if (highlights.isEmpty()) {
            uiState.update { it.copy(favoritedHighlights = emptyList()) }
            return@launch
        }
        uiState.update { it.copy(favoritedHighlights = highlights) }
    }
