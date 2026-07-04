package com.mj.yaja.ui.viewmodel

import android.util.Log
import com.mj.yaja.data.HomeScreenSnapshot
import com.mj.yaja.data.MarkdownFileManager
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

internal data class StartupBootstrapSnapshot(
    val loadedFromDiskCache: Boolean,
    val cachedDates: Set<LocalDate>,
    val cachedSelectedEntries: List<String>,
    val cachedSelectedDayLabel: String,
    val initialDates: Set<LocalDate>,
    val initialDateCount: Int,
    val currentEntryCount: Int?,
    val shouldShowCacheAnomaly: Boolean
)

internal data class CacheRefreshProgressState(
    val lastBucket: Int = -1,
    val lastUpdateAt: Long = 0L
)

internal data class CacheRefreshProgressUpdate(
    val mappedProgress: Float,
    val nextState: CacheRefreshProgressState
)

internal enum class CacheRefreshStage(val progress: Float) {
    START(0.04f),
    REFRESH_COMPLETE(0.84f),
    ENTRIES_RELOADED(0.88f),
    CALENDAR_REFRESHED(0.92f),
    STARRED_REFRESHED(0.95f),
    WARMUP_STARTED(0.97f),
    DEFERRED_QUEUED(0.985f),
    DONE(1f)
}

internal suspend fun loadStartupBootstrapSnapshot(
    fileManager: MarkdownFileManager,
    savedHomeSnapshot: HomeScreenSnapshot?,
    today: LocalDate,
    lastKnownEntryCount: Int,
    largeJournalThreshold: Int,
    logTag: String,
    logPerf: (String, Long) -> Unit
): StartupBootstrapSnapshot {
    val primeStartedAt = System.currentTimeMillis()
    val loadedFromDiskCache = withContext(Dispatchers.IO) { fileManager.primeCachesFromDisk() }
    logPerf("startup.primeCachesFromDisk", System.currentTimeMillis() - primeStartedAt)

    val cachedDatesStartedAt = System.currentTimeMillis()
    val cachedDates = fileManager.getCachedJournalDates()
    val cachedSelectedEntries =
        (savedHomeSnapshot?.entries ?: fileManager.getCachedEntriesForDate(today)).orEmpty()
    val cachedSelectedDayLabel = savedHomeSnapshot?.dayLabel ?: fileManager.getCachedDayLabel(today)
    logPerf("startup.readCachedState", System.currentTimeMillis() - cachedDatesStartedAt)
    Log.d(
        logTag,
        "startup.cachePrimed=$loadedFromDiskCache cachedDates=${cachedDates.size} " +
            "cachedSelectedEntries=${cachedSelectedEntries.size} " +
            "cachedDayLabelPresent=${cachedSelectedDayLabel.isNotEmpty()}"
    )

    val datesStartedAt = System.currentTimeMillis()
    val initialDates =
        if (cachedDates.isNotEmpty()) {
            cachedDates
        } else {
            fileManager.getAllJournalDatesLightweight()
        }
    logPerf(
        if (cachedDates.isNotEmpty()) "startup.useCachedDates" else "startup.loadLightweightDates",
        System.currentTimeMillis() - datesStartedAt
    )

    val currentEntryCount =
        if (!loadedFromDiskCache && initialDates.size < largeJournalThreshold) {
            val countCheckStartedAt = System.currentTimeMillis()
            val total = fileManager.getTotalEntryCount()
            logPerf("startup.getTotalEntryCount", System.currentTimeMillis() - countCheckStartedAt)
            total
        } else {
            Log.d(
                logTag,
                "startup.skippedTotalEntryCount cachePrimed=$loadedFromDiskCache dateCount=${initialDates.size}"
            )
            null
        }

    val shouldShowCacheAnomaly =
        currentEntryCount != null &&
            lastKnownEntryCount > 0 &&
            (currentEntryCount == 0 || currentEntryCount < (lastKnownEntryCount * 0.8)) &&
            initialDates.isEmpty()

    return StartupBootstrapSnapshot(
        loadedFromDiskCache = loadedFromDiskCache,
        cachedDates = cachedDates,
        cachedSelectedEntries = cachedSelectedEntries,
        cachedSelectedDayLabel = cachedSelectedDayLabel,
        initialDates = initialDates,
        initialDateCount = initialDates.size,
        currentEntryCount = currentEntryCount,
        shouldShowCacheAnomaly = shouldShowCacheAnomaly
    )
}

internal fun computeCacheRefreshProgressUpdate(
    current: Int,
    total: Int,
    now: Long,
    state: CacheRefreshProgressState
): CacheRefreshProgressUpdate? {
    if (total <= 0) return null

    val scanProgress = current.toFloat() / total.toFloat()
    val mappedProgress = 0.04f + (scanProgress * 0.78f)
    val progressBucket = (mappedProgress * 100).toInt()
    if (
        progressBucket != state.lastBucket &&
            (progressBucket == 100 || now - state.lastUpdateAt >= 120L)
    ) {
        return CacheRefreshProgressUpdate(
            mappedProgress = mappedProgress,
            nextState = CacheRefreshProgressState(
                lastBucket = progressBucket,
                lastUpdateAt = now
            )
        )
    }

    return null
}

internal fun progressFor(stage: CacheRefreshStage): Float = stage.progress

internal fun applyStartupBootstrapSnapshot(
    bootstrap: StartupBootstrapSnapshot,
    startupDate: LocalDate,
    uiState: MutableStateFlow<JournalUiState>,
    currentDayLabel: MutableStateFlow<String>,
    persistHomeScreenSnapshot: (LocalDate, List<String>, String) -> Unit,
    calculateMonthlyStats: (Set<LocalDate>) -> List<Pair<YearMonth, Int>>,
    calculateYearlyStats: (Set<LocalDate>) -> List<Pair<Int, Float>>,
    refreshSelectedDateOnStartup: (LocalDate) -> Unit,
    publishCachedTodos: () -> Unit,
    onCacheAnomalyDetected: () -> Unit,
    onEntryCountConfirmed: (Int) -> Unit,
    onFallbackImmediateLoad: () -> Unit,
    logTag: String
) {
    if (bootstrap.shouldShowCacheAnomaly) {
        onCacheAnomalyDetected()
    } else if (bootstrap.currentEntryCount != null) {
        onEntryCountConfirmed(bootstrap.currentEntryCount)
    }

    if (bootstrap.cachedDates.isNotEmpty()) {
        uiState.update {
            it.copy(
                datesWithEntries = bootstrap.cachedDates,
                monthlyStats = calculateMonthlyStats(bootstrap.cachedDates),
                yearlyStats = calculateYearlyStats(bootstrap.cachedDates)
            )
        }
    }

    // Seed the selected date and its cached content before the async refresh starts so the
    // request is not discarded as stale and Home has something stable to render immediately.
    currentDayLabel.value = bootstrap.cachedSelectedDayLabel
    uiState.update {
        it.copy(
            selectedDate = startupDate,
            entries = bootstrap.cachedSelectedEntries,
            isLoading = true
        )
    }

    Log.d(
        logTag,
        "startup.refreshingSelectedDateFromDisk cachePrimed=${bootstrap.loadedFromDiskCache} " +
            "cachedSelectedEntries=${bootstrap.cachedSelectedEntries.size}"
    )
    onFallbackImmediateLoad()
    refreshSelectedDateOnStartup(startupDate)

    publishCachedTodos()
}
