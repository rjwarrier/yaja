package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.MarkdownFileManager
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

internal data class CacheRefreshOutcome(
    val completedAt: Long
)

internal suspend fun runCacheRefreshWorkflow(
    fileManager: MarkdownFileManager,
    selectedDate: LocalDate,
    backgroundWorkLabel: MutableStateFlow<String?>,
    uiState: MutableStateFlow<JournalUiState>,
    syncProgress: MutableStateFlow<Float?>,
    toastEvents: MutableSharedFlow<String>,
    emitBackgroundToast: suspend (String) -> Unit,
    runSequence: suspend (() -> Unit) -> CacheRefreshOutcome,
    onRefreshCompleted: (Long) -> Unit,
    logError: (Exception) -> Unit,
    isDeferredStartupActive: () -> Boolean
) {
    try {
        backgroundWorkLabel.value = "Rebuilding cache"
        // emitBackgroundToast("Rebuilding cache...")
        uiState.update { it.copy(isLoading = true) }
        val outcome = runSequence { }
        onRefreshCompleted(outcome.completedAt)
        // toastEvents.emit("Cache rebuilt.")
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        logError(e)
        // toastEvents.emit("Cache rebuild failed. Yaja kept the current data safely.")
    } finally {
        uiState.update { it.copy(isLoading = false) }
        syncProgress.value = null
        if (!isDeferredStartupActive()) {
            backgroundWorkLabel.value = null
        }
    }
}

internal suspend fun runCacheRefreshSequence(
    fileManager: MarkdownFileManager,
    selectedDate: LocalDate,
    updateProgress: (Float) -> Unit,
    clearLookbackCache: () -> Unit,
    reloadEntries: (LocalDate) -> Unit,
    refreshCalendarDates: () -> Unit,
    refreshStarredLabels: () -> Unit,
    queueDeferredStartup: (LocalDate, Int) -> Unit
): CacheRefreshOutcome {
    updateProgress(progressFor(CacheRefreshStage.START))
    clearLookbackCache()

    var progressState = CacheRefreshProgressState()
    withContext(Dispatchers.IO) {
        fileManager.forceRefresh { current, total ->
            val update = computeCacheRefreshProgressUpdate(
                current = current,
                total = total,
                now = System.currentTimeMillis(),
                state = progressState
            )
            if (update != null) {
                progressState = update.nextState
                updateProgress(update.mappedProgress)
            }
        }
    }

    updateProgress(progressFor(CacheRefreshStage.REFRESH_COMPLETE))
    reloadEntries(selectedDate)
    updateProgress(progressFor(CacheRefreshStage.ENTRIES_RELOADED))
    refreshCalendarDates()
    updateProgress(progressFor(CacheRefreshStage.CALENDAR_REFRESHED))
    refreshStarredLabels()
    updateProgress(progressFor(CacheRefreshStage.STARRED_REFRESHED))
    fileManager.startIncrementalWarmup(latestFirst = true)
    updateProgress(progressFor(CacheRefreshStage.WARMUP_STARTED))

    val dateCount = withContext(Dispatchers.IO) { fileManager.getAllJournalDatesLightweight().size }
    updateProgress(progressFor(CacheRefreshStage.DEFERRED_QUEUED))
    queueDeferredStartup(selectedDate, dateCount)
    updateProgress(progressFor(CacheRefreshStage.DONE))

    return CacheRefreshOutcome(completedAt = System.currentTimeMillis())
}
