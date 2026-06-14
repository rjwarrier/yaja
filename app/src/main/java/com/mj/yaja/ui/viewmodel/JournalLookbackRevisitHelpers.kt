package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.DueRevisitItem
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.RevisitMarker
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal data class RevisitStateSnapshot(
    val markers: List<RevisitMarker>,
    val targetDates: Set<LocalDate>,
    val currentRevisitDate: LocalDate?,
    val currentRevisitNote: String,
    val dueRevisits: List<DueRevisitItem>
)

internal fun invalidateLookbackSnapshotCache(
    cache: MutableMap<LocalDate, Map<Int, List<String>>>,
    changedDate: LocalDate
) {
    synchronized(cache) {
        cache.remove(changedDate)
        for (yearsAhead in 1..10) {
            cache.remove(changedDate.plusYears(yearsAhead.toLong()))
        }
    }
}

internal fun clearLookbackSnapshotCache(
    cache: MutableMap<LocalDate, Map<Int, List<String>>>
) {
    synchronized(cache) {
        cache.clear()
    }
}

internal suspend fun buildLookbackSnapshot(
    date: LocalDate,
    availableDates: Set<LocalDate>,
    entriesForDateProvider: suspend (LocalDate) -> List<String>
): Map<Int, List<String>> = coroutineScope {
    val matchingPastDates =
        availableDates
            .asSequence()
            .filter { candidate ->
                candidate.isBefore(date) &&
                    candidate.month == date.month &&
                    candidate.dayOfMonth == date.dayOfMonth
            }
            .sortedDescending()
            .toList()

    val results =
        matchingPastDates
            .map { pastDate ->
                async(Dispatchers.IO) {
                    val yearsAgo = date.year - pastDate.year
                    if (yearsAgo <= 0) return@async null
                    val entries = entriesForDateProvider(pastDate)
                    if (entries.isNotEmpty()) yearsAgo to entries else null
                }
            }
            .awaitAll()
            .filterNotNull()

    results.toMap().toSortedMap().toMap()
}

internal fun loadRevisitStateSnapshot(
    fileManager: MarkdownFileManager,
    selectedDate: LocalDate
): RevisitStateSnapshot {
    // Single corpus scan for markers, target dates and due items together.
    val overview = fileManager.getRevisitOverview(selectedDate)
    return RevisitStateSnapshot(
        markers = overview.markers,
        targetDates = overview.targetDates,
        currentRevisitDate = fileManager.getRevisitDate(selectedDate),
        currentRevisitNote = fileManager.getRevisitNote(selectedDate),
        dueRevisits = overview.dueItems
    )
}

internal fun buildFavoritedHighlights(starredDates: Set<LocalDate>): List<LocalDate> =
    starredDates.sortedDescending()

internal fun applyRevisitStateSnapshot(
    snapshot: RevisitStateSnapshot,
    revisitMarkers: MutableStateFlow<List<RevisitMarker>>,
    revisitTargetDates: MutableStateFlow<Set<LocalDate>>,
    currentRevisitDate: MutableStateFlow<LocalDate?>,
    currentRevisitNote: MutableStateFlow<String>,
    dueRevisits: MutableStateFlow<List<DueRevisitItem>>
) {
    revisitMarkers.value = snapshot.markers
    revisitTargetDates.value = snapshot.targetDates
    currentRevisitDate.value = snapshot.currentRevisitDate
    currentRevisitNote.value = snapshot.currentRevisitNote
    dueRevisits.value = snapshot.dueRevisits
}

internal fun refreshLookbackForDateWorkflow(
    date: LocalDate,
    uiState: MutableStateFlow<JournalUiState>,
    lookbackSnapshotCache: MutableMap<LocalDate, Map<Int, List<String>>>,
    cancelExistingJob: () -> Unit,
    launchRefresh: () -> Job
): Job? {
    synchronized(lookbackSnapshotCache) {
        lookbackSnapshotCache[date]?.let { cached ->
            uiState.value = uiState.value.copy(lookbackEntries = cached)
            return null
        }
    }
    cancelExistingJob()
    return launchRefresh()
}

internal fun refreshFavoritedHighlightsWorkflow(
    scope: CoroutineScope,
    currentJob: Job?,
    starredDates: Set<LocalDate>,
    uiState: MutableStateFlow<JournalUiState>
): Job {
    currentJob?.cancel()
    return launchFavoritedHighlightsRefresh(
        scope = scope,
        starredDates = starredDates,
        uiState = uiState
    )
}

internal fun ensureLookbackLoadedWorkflow(
    date: LocalDate,
    force: Boolean,
    lookbackSnapshotCache: MutableMap<LocalDate, Map<Int, List<String>>>,
    refreshLookbackForDate: (LocalDate) -> Unit,
    refreshFavoritedHighlights: () -> Unit
) {
    if (force) {
        synchronized(lookbackSnapshotCache) {
            lookbackSnapshotCache.remove(date)
        }
    }
    refreshLookbackForDate(date)
    refreshFavoritedHighlights()
}

internal fun launchLookbackRefreshWorkflow(
    scope: CoroutineScope,
    currentJob: Job?,
    date: LocalDate,
    uiState: MutableStateFlow<JournalUiState>,
    lookbackSnapshotCache: MutableMap<LocalDate, Map<Int, List<String>>>,
    fileManager: MarkdownFileManager
): Job? =
    refreshLookbackForDateWorkflow(
        date = date,
        uiState = uiState,
        lookbackSnapshotCache = lookbackSnapshotCache,
        cancelExistingJob = { currentJob?.cancel() },
        launchRefresh = {
            launchLookbackRefresh(
                scope = scope,
                date = date,
                uiState = uiState,
                lookbackSnapshotCache = lookbackSnapshotCache,
                fileManager = fileManager
            )
        }
    )

internal fun refreshRevisitStateWorkflow(
    fileManager: MarkdownFileManager,
    selectedDate: LocalDate,
    revisitMarkers: MutableStateFlow<List<RevisitMarker>>,
    revisitTargetDates: MutableStateFlow<Set<LocalDate>>,
    currentRevisitDate: MutableStateFlow<LocalDate?>,
    currentRevisitNote: MutableStateFlow<String>,
    dueRevisits: MutableStateFlow<List<DueRevisitItem>>
) {
    val snapshot = loadRevisitStateSnapshot(fileManager, selectedDate)
    applyRevisitStateSnapshot(
        snapshot = snapshot,
        revisitMarkers = revisitMarkers,
        revisitTargetDates = revisitTargetDates,
        currentRevisitDate = currentRevisitDate,
        currentRevisitNote = currentRevisitNote,
        dueRevisits = dueRevisits
    )
}
