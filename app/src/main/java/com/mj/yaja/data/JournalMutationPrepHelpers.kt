package com.mj.yaja.data

import android.content.Context
import com.mj.yaja.ui.widget.WidgetRefreshCoordinator
import java.time.LocalDate

internal data class PreviousOptionalValue<V>(
    val hadValue: Boolean,
    val value: V?
)

internal data class PreviousRevisitState(
    val revisit: PreviousOptionalValue<LocalDate>,
    val note: PreviousOptionalValue<String>
)

internal data class PreviousStarLabelState(
    val star: PreviousOptionalValue<String>,
    val label: PreviousOptionalValue<String>
)

internal fun <K, V> captureOptionalMapValue(
    key: K,
    target: Map<K, V>
): PreviousOptionalValue<V> =
    PreviousOptionalValue(
        hadValue = target.containsKey(key),
        value = target[key]
    )

internal fun <K, V> restoreOptionalMapValue(
    key: K,
    hadOldValue: Boolean,
    oldValue: V?,
    target: MutableMap<K, V>
) {
    if (hadOldValue && oldValue != null) {
        target[key] = oldValue
    } else {
        target.remove(key)
    }
}

internal fun <K, V> restoreCapturedOptionalValue(
    key: K,
    previousValue: PreviousOptionalValue<V>,
    target: MutableMap<K, V>
) {
    restoreOptionalMapValue(
        key = key,
        hadOldValue = previousValue.hadValue,
        oldValue = previousValue.value,
        target = target
    )
}

internal fun loadEntriesForMutation(
    date: LocalDate,
    cachedEntries: List<String>?,
    diskEntries: List<String>,
    logCacheMismatch: (String) -> Unit
): List<String> {
    if (cachedEntries != null && cachedEntries != diskEntries) {
        logCacheMismatch(
            "Cache mismatch before mutation: date=$date cached=${cachedEntries.size} disk=${diskEntries.size}"
        )
    }
    return diskEntries
}

internal fun captureRevisitState(
    date: LocalDate,
    revisitDates: Map<LocalDate, LocalDate>,
    revisitNotes: Map<LocalDate, String>
): PreviousRevisitState =
    PreviousRevisitState(
        revisit = captureOptionalMapValue(date, revisitDates),
        note = captureOptionalMapValue(date, revisitNotes)
    )

internal fun captureStarLabelState(
    date: LocalDate,
    starredDates: Map<LocalDate, String>,
    dayLabels: Map<LocalDate, String>
): PreviousStarLabelState =
    PreviousStarLabelState(
        star = captureOptionalMapValue(date, starredDates),
        label = captureOptionalMapValue(date, dayLabels)
    )

internal fun restoreCapturedRevisitState(
    date: LocalDate,
    previousState: PreviousRevisitState,
    revisitDates: MutableMap<LocalDate, LocalDate>,
    revisitNotes: MutableMap<LocalDate, String>
) {
    restoreCapturedOptionalValue(date, previousState.revisit, revisitDates)
    restoreCapturedOptionalValue(date, previousState.note, revisitNotes)
}

internal fun restoreCapturedStarLabelState(
    date: LocalDate,
    previousState: PreviousStarLabelState,
    starredDates: MutableMap<LocalDate, String>,
    dayLabels: MutableMap<LocalDate, String>
) {
    restoreCapturedOptionalValue(date, previousState.star, starredDates)
    restoreCapturedOptionalValue(date, previousState.label, dayLabels)
}

internal fun applyRevisitMutationToMemoryState(
    date: LocalDate,
    revisitOn: LocalDate?,
    note: String,
    revisitDates: MutableMap<LocalDate, LocalDate>,
    revisitNotes: MutableMap<LocalDate, String>
) {
    if (revisitOn == null) {
        revisitDates.remove(date)
        revisitNotes.remove(date)
    } else {
        revisitDates[date] = revisitOn
        if (note.isBlank()) {
            revisitNotes.remove(date)
        } else {
            revisitNotes[date] = note.take(80)
        }
    }
}

internal fun applyDayLabelMutationToMemoryState(
    date: LocalDate,
    label: String,
    dayLabels: MutableMap<LocalDate, String>
) {
    if (label.isEmpty()) {
        dayLabels.remove(date)
    } else {
        dayLabels[date] = label
    }
}

internal fun applyStarredMutationToMemoryState(
    date: LocalDate,
    starred: Boolean,
    label: String,
    starredDates: MutableMap<LocalDate, String>,
    dayLabels: MutableMap<LocalDate, String>
) {
    if (starred) {
        starredDates[date] = label
        if (label.isNotEmpty()) dayLabels[date] = label else dayLabels.remove(date)
    } else {
        starredDates.remove(date)
        if (dayLabels[date] == label) dayLabels.remove(date)
    }
}

internal fun applyNonEmptyMutationSuccessState(
    date: LocalDate,
    entries: List<String>,
    cache: MutableMap<LocalDate, List<String>>,
    entryCountCache: MutableMap<LocalDate, Int>,
    wordCountCache: MutableMap<LocalDate, Int>,
    countWords: (List<String>) -> Int,
    updateCachedDatePresence: (LocalDate, Boolean) -> Unit,
    saveCacheToDisk: () -> Unit,
    saveEntryCountCacheToDisk: () -> Unit,
    saveWordCountCacheToDisk: () -> Unit,
    scheduleFingerprintRefresh: () -> Unit,
    updatePersistedEntryCount: (Int) -> Unit,
    entryCountDelta: Int,
    updateTodoIndexRows: (LocalDate, List<String>) -> Unit,
    scheduleEntryMutationRefresh: (LocalDate, List<String>) -> Unit
) {
    cache[date] = entries
    entryCountCache[date] = entries.size
    wordCountCache[date] = countWords(entries)
    updateCachedDatePresence(date, true)
    saveCacheToDisk()
    saveEntryCountCacheToDisk()
    saveWordCountCacheToDisk()
    scheduleFingerprintRefresh()
    if (entryCountDelta != 0) {
        updatePersistedEntryCount(entryCountDelta)
    }
    updateTodoIndexRows(date, entries)
    scheduleEntryMutationRefresh(date, entries)
}

internal fun applyDeleteMutationSuccessState(
    date: LocalDate,
    newEntries: List<String>,
    cache: MutableMap<LocalDate, List<String>>,
    entryCountCache: MutableMap<LocalDate, Int>,
    wordCountCache: MutableMap<LocalDate, Int>,
    countWords: (List<String>) -> Int,
    updateCachedDatePresence: (LocalDate, Boolean) -> Unit,
    saveCacheToDisk: () -> Unit,
    saveEntryCountCacheToDisk: () -> Unit,
    saveWordCountCacheToDisk: () -> Unit,
    scheduleFingerprintRefresh: () -> Unit,
    updatePersistedEntryCount: (Int) -> Unit,
    dateMetadataRemoved: () -> Unit,
    updateTodoIndexRows: (LocalDate, List<String>?) -> Unit,
    scheduleEntryMutationRefresh: (LocalDate, List<String>?) -> Unit
) {
    if (newEntries.isEmpty()) {
        cache.remove(date)
        entryCountCache.remove(date)
        wordCountCache.remove(date)
    } else {
        cache[date] = newEntries
        entryCountCache[date] = newEntries.size
        wordCountCache[date] = countWords(newEntries)
    }

    updateCachedDatePresence(date, newEntries.isNotEmpty())
    saveCacheToDisk()
    saveEntryCountCacheToDisk()
    saveWordCountCacheToDisk()
    scheduleFingerprintRefresh()
    updatePersistedEntryCount(-1)
    if (newEntries.isEmpty()) {
        dateMetadataRemoved()
    }
    updateTodoIndexRows(date, newEntries.takeIf { it.isNotEmpty() })
    scheduleEntryMutationRefresh(date, newEntries.takeIf { it.isNotEmpty() })
}

internal fun finalizeFrontmatterWriteSuccess(
    date: LocalDate,
    entries: List<String>,
    saveLabelsCacheToDisk: () -> Unit,
    scheduleFingerprintRefresh: () -> Unit,
    syncTodoIndexForDate: ((LocalDate, List<String>) -> Unit)? = null
) {
    saveLabelsCacheToDisk()
    syncTodoIndexForDate?.invoke(date, entries)
    scheduleFingerprintRefresh()
}

internal fun finalizeEmptyDayLabelDeletionSuccess(
    date: LocalDate,
    context: Context,
    cache: MutableMap<LocalDate, List<String>>,
    entryCountCache: MutableMap<LocalDate, Int>,
    wordCountCache: MutableMap<LocalDate, Int>,
    updateCachedDatePresence: (LocalDate, Boolean) -> Unit,
    saveCacheToDisk: () -> Unit,
    saveEntryCountCacheToDisk: () -> Unit,
    saveWordCountCacheToDisk: () -> Unit,
    saveLabelsCacheToDisk: () -> Unit,
    removeTodoDate: (LocalDate) -> Unit,
    scheduleFingerprintRefresh: () -> Unit
) {
    cache.remove(date)
    entryCountCache.remove(date)
    wordCountCache.remove(date)
    updateCachedDatePresence(date, false)
    saveCacheToDisk()
    saveEntryCountCacheToDisk()
    saveWordCountCacheToDisk()
    saveLabelsCacheToDisk()
    removeTodoDate(date)
    WidgetRefreshCoordinator.requestTodoListUpdate(context)
    scheduleFingerprintRefresh()
    WidgetRefreshCoordinator.requestHeatmapUpdate(context, invalidateCache = true)
}

internal fun finalizeLabelOnlyDayWriteSuccess(
    date: LocalDate,
    context: Context,
    cache: MutableMap<LocalDate, List<String>>,
    entryCountCache: MutableMap<LocalDate, Int>,
    wordCountCache: MutableMap<LocalDate, Int>,
    updateCachedDatePresence: (LocalDate, Boolean) -> Unit,
    saveCacheToDisk: () -> Unit,
    saveEntryCountCacheToDisk: () -> Unit,
    saveWordCountCacheToDisk: () -> Unit
) {
    cache.remove(date)
    entryCountCache.remove(date)
    wordCountCache.remove(date)
    updateCachedDatePresence(date, false)
    saveCacheToDisk()
    saveEntryCountCacheToDisk()
    saveWordCountCacheToDisk()
    WidgetRefreshCoordinator.requestTodoListUpdate(context)
    WidgetRefreshCoordinator.requestHeatmapUpdate(context, invalidateCache = true)
}

internal fun finalizeFrontmatterWriteFailure(
    saveLabelsCacheToDisk: () -> Unit,
    restoreState: () -> Unit
): Boolean {
    restoreState()
    saveLabelsCacheToDisk()
    return false
}
