package com.mj.yaja.ui.viewmodel

import com.mj.yaja.util.PerformanceTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

internal data class ResumeRefreshPlan(
    val shouldSkip: Boolean,
    val shouldForceDateRefresh: Boolean,
    val shouldRebuildKeywords: Boolean,
    val monthlyStatsDelayMs: Long
)

internal data class DeferredStartupPlan(
    val initialDelayMs: Long,
    val betweenPhaseDelayMs: Long,
    val announceMessage: String?,
    val shouldRebuildKeywords: Boolean
)

internal fun planResumeRefresh(
    now: Long,
    lastResumeRefreshAt: Long,
    lastForcedDateRefreshAt: Long,
    lastKeywordIndexAt: Long?,
    dateCount: Int,
    largeJournalDateThreshold: Int,
    largeJournalSafeModeEnabled: Boolean,
    resumeRefreshMinIntervalMs: Long,
    resumeKeywordReindexIntervalMs: Long,
    resumeForceDateRefreshIntervalMs: Long
): ResumeRefreshPlan {
    if (now - lastResumeRefreshAt < resumeRefreshMinIntervalMs) {
        return ResumeRefreshPlan(
            shouldSkip = true,
            shouldForceDateRefresh = false,
            shouldRebuildKeywords = false,
            monthlyStatsDelayMs = 0L
        )
    }

    val largeJournal = dateCount >= largeJournalDateThreshold
    val safeMode = largeJournal && largeJournalSafeModeEnabled
    val shouldForceDateRefresh = now - lastForcedDateRefreshAt > resumeForceDateRefreshIntervalMs
    val shouldRebuildKeywords = lastKeywordIndexAt == null

    return ResumeRefreshPlan(
        shouldSkip = false,
        shouldForceDateRefresh = shouldForceDateRefresh,
        shouldRebuildKeywords = shouldRebuildKeywords,
        monthlyStatsDelayMs = if (safeMode) 750L else 250L
    )
}

internal fun shouldScheduleBackgroundFullRefresh(
    now: Long,
    lastRefreshAt: Long,
    backgroundFullRefreshIntervalMs: Long,
    maintenanceJobActive: Boolean
): Boolean =
    !maintenanceJobActive && now - lastRefreshAt >= backgroundFullRefreshIntervalMs

internal fun planDeferredStartupWork(
    dateCount: Int,
    announceLargeJournal: Boolean,
    largeJournalDateThreshold: Int,
    largeJournalSafeModeEnabled: Boolean,
    isKeywordCacheLoaded: Boolean
): DeferredStartupPlan {
    val largeJournal = dateCount >= largeJournalDateThreshold
    val safeMode = largeJournal && largeJournalSafeModeEnabled
    val announceMessage =
        if (announceLargeJournal && largeJournal) {
            if (safeMode) {
                "Large journal detected. Loading recent data first to keep Yaja stable."
            } else {
                "Large journal detected. Some data may take a little longer to settle."
            }
        } else {
            null
        }

    return DeferredStartupPlan(
        initialDelayMs = if (safeMode) 1200L else 250L,
        betweenPhaseDelayMs = if (safeMode) 180L else 80L,
        announceMessage = announceMessage,
        shouldRebuildKeywords = !largeJournal && !isKeywordCacheLoaded
    )
}

internal fun runResumeRefreshWorkflow(
    now: Long,
    selectedDate: java.time.LocalDate,
    dateCount: Int,
    lastResumeRefreshAt: Long,
    lastForcedDateRefreshAt: Long,
    lastKeywordIndexAt: Long?,
    largeJournalDateThreshold: Int,
    largeJournalSafeModeEnabled: Boolean,
    resumeRefreshMinIntervalMs: Long,
    resumeKeywordReindexIntervalMs: Long,
    resumeForceDateRefreshIntervalMs: Long,
    updateLastResumeRefreshAt: (Long) -> Unit,
    markForcedDateRefresh: (Long) -> Unit,
    refreshSelectedDateOnResume: (java.time.LocalDate, Boolean, () -> Unit) -> Unit
) {
    val resumePlan = planResumeRefresh(
        now = now,
        lastResumeRefreshAt = lastResumeRefreshAt,
        lastForcedDateRefreshAt = lastForcedDateRefreshAt,
        lastKeywordIndexAt = lastKeywordIndexAt,
        dateCount = dateCount,
        largeJournalDateThreshold = largeJournalDateThreshold,
        largeJournalSafeModeEnabled = largeJournalSafeModeEnabled,
        resumeRefreshMinIntervalMs = resumeRefreshMinIntervalMs,
        resumeKeywordReindexIntervalMs = resumeKeywordReindexIntervalMs,
        resumeForceDateRefreshIntervalMs = resumeForceDateRefreshIntervalMs
    )
    if (resumePlan.shouldSkip) return

    updateLastResumeRefreshAt(now)
    refreshSelectedDateOnResume(selectedDate, resumePlan.shouldForceDateRefresh) {
        markForcedDateRefresh(now)
    }
}

internal fun dismissCacheAnomalyWorkflow(
    uiState: kotlinx.coroutines.flow.MutableStateFlow<JournalUiState>,
    scope: CoroutineScope,
    persistLastKnownEntryCount: suspend () -> Unit
) {
    uiState.update { it.copy(showCacheAnomalyDialog = false) }
    scope.launch(Dispatchers.IO) {
        persistLastKnownEntryCount()
    }
}

internal fun acceptCacheAnomalyRefreshWorkflow(
    uiState: kotlinx.coroutines.flow.MutableStateFlow<JournalUiState>,
    refreshCache: () -> Unit
) {
    uiState.update { it.copy(showCacheAnomalyDialog = false) }
    refreshCache()
}

internal fun launchDeferredStartupWorkflow(
    currentJob: Job?,
    scope: CoroutineScope,
    date: LocalDate,
    dateCount: Int,
    announceLargeJournal: Boolean,
    largeJournalDateThreshold: Int,
    largeJournalSafeModeEnabled: Boolean,
    isKeywordCacheLoaded: Boolean,
    backgroundWorkLabel: kotlinx.coroutines.flow.MutableStateFlow<String?>,
    emitBackgroundToast: suspend (String) -> Unit,
    emitToast: suspend (String) -> Unit,
    launchLookbackRefresh: () -> Job?,
    refreshHighlights: () -> Job?,
    refreshMonthlyStats: () -> Job?,
    rebuildKeywords: suspend () -> Unit,
    logPerf: (String, Long) -> Unit
): Job {
    val deferredPlan = planDeferredStartupWork(
        dateCount = dateCount,
        announceLargeJournal = announceLargeJournal,
        largeJournalDateThreshold = largeJournalDateThreshold,
        largeJournalSafeModeEnabled = largeJournalSafeModeEnabled,
        isKeywordCacheLoaded = isKeywordCacheLoaded
    )

    currentJob?.cancel()
    return scope.launch {
        try {
            val startedAt = System.currentTimeMillis()
            backgroundWorkLabel.value = "Finalizing in background"
            // emitBackgroundToast("Finishing background refresh...")
            // deferredPlan.announceMessage?.let { message ->
            //     emitToast(message)
            // }

            delay(deferredPlan.initialDelayMs)

            timedPhaseWorkflow("deferred.lookback", logPerf) {
                launchLookbackRefresh()?.join()
            }
            delay(deferredPlan.betweenPhaseDelayMs)
            timedPhaseWorkflow("deferred.highlights", logPerf) {
                refreshHighlights()?.join()
            }
            delay(deferredPlan.betweenPhaseDelayMs)
            timedPhaseWorkflow("deferred.monthlyStats", logPerf) {
                refreshMonthlyStats()?.join()
            }
            if (deferredPlan.shouldRebuildKeywords) {
                backgroundWorkLabel.value = "Indexing keywords in background"
                // emitBackgroundToast("Indexing people & places...")
                rebuildKeywords()
            }
            logPerf("deferred.total", System.currentTimeMillis() - startedAt)
        } finally {
            backgroundWorkLabel.value = null
        }
    }
}

internal suspend fun timedPhaseWorkflow(
    name: String,
    logPerf: (String, Long) -> Unit,
    block: suspend () -> Unit
) {
    PerformanceTrace.measureSuspend(name, logPerf) {
        block()
    }
}
