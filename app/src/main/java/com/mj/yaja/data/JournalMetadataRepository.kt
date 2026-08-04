package com.mj.yaja.data

import android.content.Context
import com.mj.yaja.data.storage.JournalStorage
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

internal class JournalMetadataRepository(
    private val context: Context,
    private val journalStorage: JournalStorage,
    private val cache: ConcurrentHashMap<LocalDate, List<String>>,
    private val entryCountCache: ConcurrentHashMap<LocalDate, Int>,
    private val wordCountCache: ConcurrentHashMap<LocalDate, Int>,
    private val starredDates: ConcurrentHashMap<LocalDate, String>,
    private val dayLabels: ConcurrentHashMap<LocalDate, String>,
    private val revisitDates: ConcurrentHashMap<LocalDate, LocalDate>,
    private val revisitNotes: ConcurrentHashMap<LocalDate, String>,
    private val isCachePopulated: () -> Boolean,
    private val ensureFrontmatterPopulated: () -> Unit,
    private val populateFrontmatterData: () -> Unit,
    private val entriesForDateProvider: (LocalDate) -> List<String>,
    private val entriesForMutationProvider: (LocalDate) -> List<String>,
    private val contentBuilder: (LocalDate, List<String>) -> String,
    private val snapshotBeforeMutation: (LocalDate, String) -> Boolean,
    private val updateCachedDatePresence: (LocalDate, Boolean) -> Unit,
    private val saveDayToDb: (LocalDate) -> Unit,
    private val deleteDayFromDb: (LocalDate) -> Unit,
    private val scheduleFingerprintRefresh: () -> Unit,
    private val scheduleTodoIndexSyncForDate: (LocalDate, List<String>) -> Unit,
    private val removeTodoDate: (LocalDate) -> Unit,
    private val rebuildSnapshotProvider: () -> Map<LocalDate, List<String>>,
    private val entriesSnapshotForDatesProvider: (Iterable<LocalDate>) -> Map<LocalDate, List<String>>,
    private val allJournalDatesProvider: () -> Set<LocalDate>
) {
    fun isDateStarred(date: LocalDate): Boolean {
        ensureFrontmatterPopulated()
        return starredDates.containsKey(date)
    }

    fun getStarredLabel(date: LocalDate): String {
        ensureFrontmatterPopulated()
        return starredDates[date] ?: ""
    }

    fun getDayLabel(date: LocalDate): String {
        ensureFrontmatterPopulated()
        return dayLabels[date] ?: ""
    }

    fun getRevisitDate(date: LocalDate): LocalDate? {
        ensureFrontmatterPopulated()
        return revisitDates[date]
    }

    fun getRevisitNote(date: LocalDate): String {
        ensureFrontmatterPopulated()
        return revisitNotes[date].orEmpty()
    }

    fun getEntryRevisit(date: LocalDate, entryIndex: Int): EntryRevisitMetadata {
        val entry = entriesForDateProvider(date).getOrNull(entryIndex).orEmpty()
        return parseEntryRevisitMetadata(entry)
    }

    fun setRevisit(date: LocalDate, revisitOn: LocalDate?, note: String = ""): Boolean {
        ensureFrontmatterPopulated()
        val entries = entriesForMutationProvider(date)
        val currentContent = journalStorage.readDateContent(date)
        if (entries.isEmpty() && currentContent.isNullOrBlank() && revisitOn == null) {
            return false
        }
        if (!snapshotBeforeMutation(date, "set_revisit")) return false

        val previousState = captureRevisitState(
            date = date,
            revisitDates = revisitDates,
            revisitNotes = revisitNotes
        )
        applyRevisitMutationToMemoryState(
            date = date,
            revisitOn = revisitOn,
            note = note,
            revisitDates = revisitDates,
            revisitNotes = revisitNotes
        )

        val content = contentBuilder(date, entries)
        val shouldCreateFile = currentContent.isNullOrBlank()
        if (journalStorage.writeDateContent(date, content, createIfNotExists = shouldCreateFile)) {
            finalizeFrontmatterWriteSuccess(
                date = date,
                entries = entries,
                saveLabelsCacheToDisk = { saveDayToDb(date) },
                scheduleFingerprintRefresh = scheduleFingerprintRefresh
            )
            if (entries.isEmpty()) {
                finalizeLabelOnlyDayWriteSuccess(
                    date = date,
                    context = context,
                    cache = cache,
                    entryCountCache = entryCountCache,
                    wordCountCache = wordCountCache,
                    updateCachedDatePresence = updateCachedDatePresence,
                    saveCacheToDisk = { saveDayToDb(date) },
                    saveEntryCountCacheToDisk = {},
                    saveWordCountCacheToDisk = {}
                )
            }
            return true
        }

        return finalizeFrontmatterWriteFailure(
            saveLabelsCacheToDisk = { saveDayToDb(date) }
        ) {
            restoreCapturedRevisitState(
                date = date,
                previousState = previousState,
                revisitDates = revisitDates,
                revisitNotes = revisitNotes
            )
        }
    }

    fun setDayLabel(date: LocalDate, label: String): Boolean {
        ensureFrontmatterPopulated()
        val entries = entriesForMutationProvider(date)
        val trimmed = label.take(30)
        val previousLabel = captureOptionalMapValue(date, dayLabels)
        val currentContent = journalStorage.readDateContent(date)
        if (!snapshotBeforeMutation(date, "set_day_label")) return false

        applyDayLabelMutationToMemoryState(
            date = date,
            label = trimmed,
            dayLabels = dayLabels
        )

        if (entries.isEmpty() && trimmed.isEmpty()) {
            val deleted = if (currentContent.isNullOrBlank()) {
                true
            } else {
                journalStorage.deleteDateFile(date)
            }
            if (deleted) {
                finalizeEmptyDayLabelDeletionSuccess(
                    date = date,
                    context = context,
                    cache = cache,
                    entryCountCache = entryCountCache,
                    wordCountCache = wordCountCache,
                    updateCachedDatePresence = updateCachedDatePresence,
                    saveCacheToDisk = { deleteDayFromDb(date) },
                    saveEntryCountCacheToDisk = {},
                    saveWordCountCacheToDisk = {},
                    saveLabelsCacheToDisk = { deleteDayFromDb(date) },
                    removeTodoDate = removeTodoDate,
                    scheduleFingerprintRefresh = scheduleFingerprintRefresh
                )
                return true
            }

            return finalizeFrontmatterWriteFailure(
                saveLabelsCacheToDisk = { saveDayToDb(date) }
            ) {
                restoreCapturedOptionalValue(date, previousLabel, dayLabels)
            }
        }

        val content = contentBuilder(date, entries)
        val shouldCreateFile = currentContent.isNullOrBlank()
        if (journalStorage.writeDateContent(date, content, createIfNotExists = shouldCreateFile)) {
            finalizeFrontmatterWriteSuccess(
                date = date,
                entries = entries,
                saveLabelsCacheToDisk = { saveDayToDb(date) },
                scheduleFingerprintRefresh = scheduleFingerprintRefresh,
                syncTodoIndexForDate = scheduleTodoIndexSyncForDate
            )
            if (entries.isEmpty()) {
                finalizeLabelOnlyDayWriteSuccess(
                    date = date,
                    context = context,
                    cache = cache,
                    entryCountCache = entryCountCache,
                    wordCountCache = wordCountCache,
                    updateCachedDatePresence = updateCachedDatePresence,
                    saveCacheToDisk = { saveDayToDb(date) },
                    saveEntryCountCacheToDisk = {},
                    saveWordCountCacheToDisk = {}
                )
            }
            return true
        }

        return finalizeFrontmatterWriteFailure(
            saveLabelsCacheToDisk = { saveDayToDb(date) }
        ) {
            restoreCapturedOptionalValue(date, previousLabel, dayLabels)
        }
    }

    fun setStarred(date: LocalDate, starred: Boolean, label: String = ""): Boolean {
        ensureFrontmatterPopulated()
        val entries = entriesForMutationProvider(date)
        val currentContent = journalStorage.readDateContent(date)
        if (entries.isEmpty() && currentContent.isNullOrBlank() && !starred) {
            return false
        }
        if (!snapshotBeforeMutation(date, "set_starred")) return false

        val previousState = captureStarLabelState(
            date = date,
            starredDates = starredDates,
            dayLabels = dayLabels
        )
        applyStarredMutationToMemoryState(
            date = date,
            starred = starred,
            label = label,
            starredDates = starredDates,
            dayLabels = dayLabels
        )

        val content = contentBuilder(date, entries)
        val shouldCreateFile = currentContent.isNullOrBlank()
        if (journalStorage.writeDateContent(date, content, createIfNotExists = shouldCreateFile)) {
            finalizeFrontmatterWriteSuccess(
                date = date,
                entries = entries,
                saveLabelsCacheToDisk = { saveDayToDb(date) },
                scheduleFingerprintRefresh = scheduleFingerprintRefresh,
                syncTodoIndexForDate = scheduleTodoIndexSyncForDate
            )
            if (entries.isEmpty()) {
                finalizeLabelOnlyDayWriteSuccess(
                    date = date,
                    context = context,
                    cache = cache,
                    entryCountCache = entryCountCache,
                    wordCountCache = wordCountCache,
                    updateCachedDatePresence = updateCachedDatePresence,
                    saveCacheToDisk = { saveDayToDb(date) },
                    saveEntryCountCacheToDisk = {},
                    saveWordCountCacheToDisk = {}
                )
            }
            return true
        }

        return finalizeFrontmatterWriteFailure(
            saveLabelsCacheToDisk = { saveDayToDb(date) }
        ) {
            restoreCapturedStarLabelState(
                date = date,
                previousState = previousState,
                starredDates = starredDates,
                dayLabels = dayLabels
            )
        }
    }

    fun getStarredDates(): List<LocalDate> {
        ensureFrontmatterPopulated()
        return starredDates.keys.sorted()
    }

    fun refreshFrontmatterSnapshot(lock: Any) {
        synchronized(lock) {
            populateFrontmatterData()
        }
    }

    fun getAllStarredLabels(): Map<LocalDate, String> {
        ensureFrontmatterPopulated()
        return dayLabels.toMap()
    }

    fun getRevisitOverview(targetDate: LocalDate): MarkdownFileManager.RevisitOverview {
        ensureFrontmatterPopulated()
        val markers = mutableListOf<RevisitMarker>()
        val dueItems = mutableListOf<DueRevisitItem>()

        revisitDates.forEach { (sourceDate, revisitOn) ->
            val note = revisitNotes[sourceDate].orEmpty()
            markers += RevisitMarker(sourceDate = sourceDate, revisitOn = revisitOn, note = note)
            if (revisitOn == targetDate) {
                dueItems +=
                    DueRevisitItem(
                        sourceDate = sourceDate,
                        revisitOn = revisitOn,
                        label = dayLabels[sourceDate].orEmpty(),
                        note = note
                    )
            }
        }

        forEachEntryRevisit { date, index, revisitOn, note ->
            markers +=
                RevisitMarker(
                    sourceDate = date,
                    revisitOn = revisitOn,
                    entryIndex = index,
                    note = note
                )
            if (revisitOn == targetDate) {
                dueItems +=
                    DueRevisitItem(
                        sourceDate = date,
                        revisitOn = targetDate,
                        entryIndex = index,
                        label = dayLabels[date].orEmpty(),
                        note = note
                    )
            }
        }

        return MarkdownFileManager.RevisitOverview(
            markers = markers.sortedWith(REVISIT_MARKER_ORDER),
            targetDates = markers.mapTo(linkedSetOf()) { it.revisitOn },
            dueItems = dueItems.sortedWith(DUE_REVISIT_ORDER)
        )
    }

    fun getAllRevisitMarkers(): List<RevisitMarker> {
        ensureFrontmatterPopulated()
        val markers = mutableListOf<RevisitMarker>()
        revisitDates.forEach { (sourceDate, revisitOn) ->
            markers +=
                RevisitMarker(
                    sourceDate = sourceDate,
                    revisitOn = revisitOn,
                    note = revisitNotes[sourceDate].orEmpty()
                )
        }
        forEachEntryRevisit { date, index, revisitOn, note ->
            markers +=
                RevisitMarker(
                    sourceDate = date,
                    revisitOn = revisitOn,
                    entryIndex = index,
                    note = note
                )
        }
        return markers.sortedWith(REVISIT_MARKER_ORDER)
    }

    fun getRevisitTargetDates(): Set<LocalDate> {
        ensureFrontmatterPopulated()
        val targetDates = revisitDates.values.toMutableSet()
        forEachEntryRevisit { _, _, revisitOn, _ -> targetDates += revisitOn }
        return targetDates
    }

    fun getDueRevisitItems(targetDate: LocalDate): List<DueRevisitItem> {
        ensureFrontmatterPopulated()
        val items = mutableListOf<DueRevisitItem>()

        revisitDates.forEach { (sourceDate, revisitOn) ->
            if (revisitOn == targetDate) {
                items +=
                    DueRevisitItem(
                        sourceDate = sourceDate,
                        revisitOn = revisitOn,
                        label = dayLabels[sourceDate].orEmpty(),
                        note = revisitNotes[sourceDate].orEmpty()
                    )
            }
        }

        forEachEntryRevisit { date, index, revisitOn, note ->
            if (revisitOn == targetDate) {
                items +=
                    DueRevisitItem(
                        sourceDate = date,
                        revisitOn = targetDate,
                        entryIndex = index,
                        label = dayLabels[date].orEmpty(),
                        note = note
                    )
            }
        }

        return items.sortedWith(DUE_REVISIT_ORDER)
    }

    private fun forEachEntryRevisit(action: (LocalDate, Int, LocalDate, String) -> Unit) {
        val snapshot: Map<LocalDate, List<String>> =
            if (isCachePopulated()) {
                cacheSnapshot()
            } else {
                rebuildSnapshotProvider().ifEmpty {
                    entriesSnapshotForDatesProvider(allJournalDatesProvider())
                }
            }
        snapshot.forEach { (date, entries) ->
            entries.forEachIndexed { index, entry ->
                val revisit = parseEntryRevisitMetadata(entry)
                val revisitOn = revisit.revisitOn ?: return@forEachIndexed
                action(date, index, revisitOn, revisit.note)
            }
        }
    }

    private fun cacheSnapshot(): Map<LocalDate, List<String>> =
        cache

    private companion object {
        private val REVISIT_MARKER_ORDER =
            compareByDescending<RevisitMarker> { it.sourceDate }
                .thenByDescending { it.entryIndex ?: -1 }
        private val DUE_REVISIT_ORDER =
            compareByDescending<DueRevisitItem> { it.sourceDate }
                .thenByDescending { it.entryIndex ?: -1 }
    }
}
