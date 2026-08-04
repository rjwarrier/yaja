package com.mj.yaja.data

import android.util.Log
import com.mj.yaja.data.storage.JournalStorage
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

internal class JournalMutationService(
    private val journalStorage: JournalStorage,
    private val cache: ConcurrentHashMap<LocalDate, List<String>>,
    private val entryCountCache: ConcurrentHashMap<LocalDate, Int>,
    private val wordCountCache: ConcurrentHashMap<LocalDate, Int>,
    private val starredDates: ConcurrentHashMap<LocalDate, String>,
    private val revisitDates: ConcurrentHashMap<LocalDate, LocalDate>,
    private val revisitNotes: ConcurrentHashMap<LocalDate, String>,
    private val entriesForMutationProvider: (LocalDate) -> List<String>,
    private val directEntriesProvider: (LocalDate) -> List<String>,
    private val contentBuilder: (LocalDate, List<String>) -> String,
    private val snapshotBeforeMutation: (LocalDate, String) -> Boolean,
    private val updateCachedDatePresence: (LocalDate, Boolean) -> Unit,
    private val saveDayToDb: (LocalDate) -> Unit,
    private val deleteDayFromDb: (LocalDate) -> Unit,
    private val scheduleFingerprintRefresh: () -> Unit,
    private val updatePersistedEntryCount: (Int) -> Unit,
    private val updateTodoIndexRows: (LocalDate, List<String>?) -> Unit,
    private val scheduleTodoIndexRowsUpdate: (LocalDate, List<String>?) -> Unit,
    private val scheduleEntryMutationRefresh: (LocalDate, List<String>?) -> Unit,
    private val removeDateMetadata: (LocalDate) -> Unit
) {
    fun tryAddEntryForDate(date: LocalDate, entry: String): MarkdownFileManager.EntryMutationResult {
        if (entry.isBlank()) return MarkdownFileManager.EntryMutationResult(entriesForMutationProvider(date), false)
        val existingEntries = traceMutationStage("addEntry", date, "read") {
            entriesForMutationProvider(date)
        }
        val newEntries = existingEntries + entry
        val content = contentBuilder(date, newEntries)
        val snapshotCreated = traceMutationStage("addEntry", date, "snapshot") {
            snapshotBeforeMutation(date, "add_entry")
        }
        if (!snapshotCreated) {
            return MarkdownFileManager.EntryMutationResult(existingEntries, false)
        }
        val written = traceMutationStage("addEntry", date, "write") {
            journalStorage.writeDateContent(date, content, createIfNotExists = true)
        }
        if (!written) {
            return MarkdownFileManager.EntryMutationResult(existingEntries, false)
        }
        Log.d(TODO_PIPELINE_TAG, "File written: addEntry date=$date entries=${newEntries.size}")
        traceMutationStage("addEntry", date, "state") {
            applyNonEmptyMutationSuccessState(
                date = date,
                entries = newEntries,
                cache = cache,
                entryCountCache = entryCountCache,
                wordCountCache = wordCountCache,
                countWords = ::countWords,
                updateCachedDatePresence = updateCachedDatePresence,
                saveCacheToDisk = { saveDayToDb(date) },
                saveEntryCountCacheToDisk = {},
                saveWordCountCacheToDisk = {},
                scheduleFingerprintRefresh = scheduleFingerprintRefresh,
                updatePersistedEntryCount = updatePersistedEntryCount,
                entryCountDelta = 1,
                updateTodoIndexRows = scheduleTodoIndexRowsUpdate,
                scheduleEntryMutationRefresh = scheduleEntryMutationRefresh
            )
        }
        return MarkdownFileManager.EntryMutationResult(newEntries, true)
    }

    fun tryInsertEntryAtPosition(date: LocalDate, entry: String, index: Int): MarkdownFileManager.EntryMutationResult {
        if (entry.isBlank()) return MarkdownFileManager.EntryMutationResult(entriesForMutationProvider(date), false)
        val existingEntries = entriesForMutationProvider(date)
        val mutableEntries = existingEntries.toMutableList()
        val clampedIndex = index.coerceIn(0, mutableEntries.size)
        mutableEntries.add(clampedIndex, entry)

        val content = contentBuilder(date, mutableEntries)
        if (!snapshotBeforeMutation(date, "insert_entry")) {
            return MarkdownFileManager.EntryMutationResult(existingEntries, false)
        }
        if (!journalStorage.writeDateContent(date, content, createIfNotExists = true)) {
            return MarkdownFileManager.EntryMutationResult(existingEntries, false)
        }
        Log.d(TODO_PIPELINE_TAG, "File written: insertEntry date=$date entries=${mutableEntries.size}")
        applyNonEmptyMutationSuccessState(
            date = date,
            entries = mutableEntries,
            cache = cache,
            entryCountCache = entryCountCache,
            wordCountCache = wordCountCache,
            countWords = ::countWords,
            updateCachedDatePresence = updateCachedDatePresence,
            saveCacheToDisk = { saveDayToDb(date) },
            saveEntryCountCacheToDisk = {},
            saveWordCountCacheToDisk = {},
            scheduleFingerprintRefresh = scheduleFingerprintRefresh,
            updatePersistedEntryCount = updatePersistedEntryCount,
            entryCountDelta = 1,
            updateTodoIndexRows = scheduleTodoIndexRowsUpdate,
            scheduleEntryMutationRefresh = scheduleEntryMutationRefresh
        )
        return MarkdownFileManager.EntryMutationResult(mutableEntries, true)
    }

    fun tryDeleteEntryForDate(date: LocalDate, indexToDelete: Int): MarkdownFileManager.EntryMutationResult {
        val currentEntries = traceMutationStage("deleteEntry", date, "read") {
            entriesForMutationProvider(date)
        }
        if (currentEntries.isEmpty()) return MarkdownFileManager.EntryMutationResult(currentEntries, false)
        if (indexToDelete !in currentEntries.indices) return MarkdownFileManager.EntryMutationResult(currentEntries, false)

        val newEntries = currentEntries.toMutableList()
        newEntries.removeAt(indexToDelete)

        val snapshotCreated = traceMutationStage("deleteEntry", date, "snapshot") {
            snapshotBeforeMutation(date, "delete_entry")
        }
        if (!snapshotCreated) {
            return MarkdownFileManager.EntryMutationResult(currentEntries, false)
        }
        if (newEntries.isEmpty()) {
            val deleted = traceMutationStage("deleteEntry", date, "deleteFile") {
                journalStorage.deleteDateFile(date)
            }
            if (!deleted) return MarkdownFileManager.EntryMutationResult(currentEntries, false)
            Log.d(TODO_PIPELINE_TAG, "File deleted: deleteEntry date=$date")
            starredDates.remove(date)
            revisitDates.remove(date)
            revisitNotes.remove(date)
        } else {
            val content = contentBuilder(date, newEntries)
            val written = traceMutationStage("deleteEntry", date, "write") {
                journalStorage.writeDateContent(date, content, createIfNotExists = false)
            }
            if (!written) {
                return MarkdownFileManager.EntryMutationResult(currentEntries, false)
            }
            Log.d(TODO_PIPELINE_TAG, "File written: deleteEntry date=$date entries=${newEntries.size}")
        }
        traceMutationStage("deleteEntry", date, "state") {
            applyDeleteMutationSuccessState(
                date = date,
                newEntries = newEntries,
                cache = cache,
                entryCountCache = entryCountCache,
                wordCountCache = wordCountCache,
                countWords = ::countWords,
                updateCachedDatePresence = updateCachedDatePresence,
                saveCacheToDisk = { if (newEntries.isEmpty()) deleteDayFromDb(date) else saveDayToDb(date) },
                saveEntryCountCacheToDisk = {},
                saveWordCountCacheToDisk = {},
                scheduleFingerprintRefresh = scheduleFingerprintRefresh,
                updatePersistedEntryCount = updatePersistedEntryCount,
                dateMetadataRemoved = {
                    removeDateMetadata(date)
                    deleteDayFromDb(date)
                },
                updateTodoIndexRows = scheduleTodoIndexRowsUpdate,
                scheduleEntryMutationRefresh = scheduleEntryMutationRefresh
            )
        }
        return MarkdownFileManager.EntryMutationResult(newEntries, true)
    }

    fun tryUpdateEntryForDate(date: LocalDate, indexToUpdate: Int, newEntry: String): MarkdownFileManager.EntryMutationResult {
        val currentEntries = traceMutationStage("updateEntry", date, "read") {
            entriesForMutationProvider(date)
        }
        if (currentEntries.isEmpty()) return MarkdownFileManager.EntryMutationResult(currentEntries, false)
        if (indexToUpdate !in currentEntries.indices) return MarkdownFileManager.EntryMutationResult(currentEntries, false)

        val updatedEntries = currentEntries.toMutableList()
        updatedEntries[indexToUpdate] = newEntry

        val content = contentBuilder(date, updatedEntries)
        val snapshotCreated = traceMutationStage("updateEntry", date, "snapshot") {
            snapshotBeforeMutation(date, "update_entry")
        }
        if (!snapshotCreated) {
            return MarkdownFileManager.EntryMutationResult(currentEntries, false)
        }
        val written = traceMutationStage("updateEntry", date, "write") {
            journalStorage.writeDateContent(date, content, createIfNotExists = false)
        }
        if (!written) {
            return MarkdownFileManager.EntryMutationResult(currentEntries, false)
        }
        Log.d(TODO_PIPELINE_TAG, "File written: updateEntry date=$date entries=${updatedEntries.size}")
        traceMutationStage("updateEntry", date, "state") {
            applyNonEmptyMutationSuccessState(
                date = date,
                entries = updatedEntries,
                cache = cache,
                entryCountCache = entryCountCache,
                wordCountCache = wordCountCache,
                countWords = ::countWords,
                updateCachedDatePresence = updateCachedDatePresence,
                saveCacheToDisk = { saveDayToDb(date) },
                saveEntryCountCacheToDisk = {},
                saveWordCountCacheToDisk = {},
                scheduleFingerprintRefresh = scheduleFingerprintRefresh,
                updatePersistedEntryCount = updatePersistedEntryCount,
                entryCountDelta = 0,
                updateTodoIndexRows = scheduleTodoIndexRowsUpdate,
                scheduleEntryMutationRefresh = scheduleEntryMutationRefresh
            )
        }
        return MarkdownFileManager.EntryMutationResult(updatedEntries, true)
    }

    fun trySetEntriesForDate(
        date: LocalDate,
        newEntries: List<String>,
        preserveMissingDiskEntries: Boolean = true
    ): MarkdownFileManager.EntryMutationResult {
        if (newEntries.isEmpty()) {
            val diskEntries = entriesForMutationProvider(date)
            return MarkdownFileManager.EntryMutationResult(diskEntries, false)
        }
        val diskEntries = entriesForMutationProvider(date)
        val entriesToWrite =
            if (!preserveMissingDiskEntries || diskEntries.isEmpty()) {
                newEntries
            } else {
                mergeEntriesPreservingMissingCounts(newEntries, diskEntries)
            }

        val content = contentBuilder(date, entriesToWrite)
        if (!snapshotBeforeMutation(date, "set_entries")) {
            return MarkdownFileManager.EntryMutationResult(diskEntries, false)
        }
        if (!journalStorage.writeDateContent(date, content, createIfNotExists = true)) {
            return MarkdownFileManager.EntryMutationResult(diskEntries, false)
        }
        Log.d(TODO_PIPELINE_TAG, "File written: setEntries date=$date entries=${entriesToWrite.size}")
        val previousCount = diskEntries.size
        applyNonEmptyMutationSuccessState(
            date = date,
            entries = entriesToWrite,
            cache = cache,
            entryCountCache = entryCountCache,
            wordCountCache = wordCountCache,
            countWords = ::countWords,
            updateCachedDatePresence = updateCachedDatePresence,
            saveCacheToDisk = { saveDayToDb(date) },
            saveEntryCountCacheToDisk = {},
            saveWordCountCacheToDisk = {},
            scheduleFingerprintRefresh = scheduleFingerprintRefresh,
            updatePersistedEntryCount = updatePersistedEntryCount,
            entryCountDelta = entriesToWrite.size - previousCount,
            updateTodoIndexRows = scheduleTodoIndexRowsUpdate,
            scheduleEntryMutationRefresh = scheduleEntryMutationRefresh
        )
        return MarkdownFileManager.EntryMutationResult(entriesToWrite, true)
    }

    fun toggleTodoLine(
        date: LocalDate,
        entryIndex: Int,
        lineIndex: Int,
        expectedLineHash: String? = null,
        expectedDisplayText: String? = null
    ): Boolean {
        val entries = directEntriesProvider(date).toMutableList()
        val target = findTodoTarget(entries, entryIndex, lineIndex, expectedLineHash, expectedDisplayText)
        if (target == null) {
            updateTodoIndexRows(date, entries)
            scheduleEntryMutationRefresh(date, entries)
            Log.w(
                TODO_PIPELINE_TAG,
                "Todo toggle target missing: date=$date entry=$entryIndex line=$lineIndex hash=$expectedLineHash"
            )
            return false
        }
        val (targetEntryIndex, targetLineIndex, line) = target
        val lines = entries[targetEntryIndex].lines().toMutableList()
        val toggledLine = TodoParser.toggleLine(line) ?: return false
        lines[targetLineIndex] = toggledLine
        entries[targetEntryIndex] = lines.joinToString("\n")
        val result = trySetEntriesForDate(date, entries, preserveMissingDiskEntries = false)
        if (result.success) {
            updateTodoIndexRows(date, result.entries)
        }
        return result.success
    }

    private fun mergeEntriesPreservingMissingCounts(
        requestedEntries: List<String>,
        diskEntries: List<String>
    ): List<String> {
        val requestedCounts = requestedEntries.groupingBy { it }.eachCount().toMutableMap()
        val missingFromRequested = mutableListOf<String>()
        diskEntries.forEach { diskEntry ->
            val remaining = requestedCounts[diskEntry] ?: 0
            if (remaining > 0) {
                requestedCounts[diskEntry] = remaining - 1
            } else {
                missingFromRequested += diskEntry
            }
        }
        return requestedEntries + missingFromRequested
    }

    private data class TodoLineTarget(
        val entryIndex: Int,
        val lineIndex: Int,
        val line: String
    )

    private fun findTodoTarget(
        entries: List<String>,
        entryIndex: Int,
        lineIndex: Int,
        expectedLineHash: String?,
        expectedDisplayText: String?
    ): TodoLineTarget? {
        val candidateLine =
            entries.getOrNull(entryIndex)
                ?.lines()
                ?.getOrNull(lineIndex)
        val parsedCandidate = candidateLine?.let(TodoParser::parseLine)
        if (parsedCandidate != null) {
            val hashMatches =
                expectedLineHash.isNullOrBlank() || parsedCandidate.lineHash == expectedLineHash
            val textMatches =
                expectedDisplayText.isNullOrBlank() ||
                    parsedCandidate.displayText.trim().equals(
                        expectedDisplayText.trim(),
                        ignoreCase = true
                    )
            if (hashMatches || textMatches) {
                return TodoLineTarget(entryIndex, lineIndex, candidateLine)
            }
        }
        if (!expectedLineHash.isNullOrBlank()) {
            entries.forEachIndexed { currentEntryIndex, entry ->
                entry.lines().forEachIndexed { currentLineIndex, line ->
                    val parsed = TodoParser.parseLine(line) ?: return@forEachIndexed
                    if (parsed.lineHash == expectedLineHash) {
                        return TodoLineTarget(currentEntryIndex, currentLineIndex, line)
                    }
                }
            }
        }
        if (!expectedDisplayText.isNullOrBlank()) {
            val normalizedExpected = expectedDisplayText.trim().lowercase()
            entries.forEachIndexed { currentEntryIndex, entry ->
                entry.lines().forEachIndexed { currentLineIndex, line ->
                    val parsed = TodoParser.parseLine(line) ?: return@forEachIndexed
                    if (parsed.displayText.trim().lowercase() == normalizedExpected) {
                        return TodoLineTarget(currentEntryIndex, currentLineIndex, line)
                    }
                }
            }
        }
        return parsedCandidate?.let { TodoLineTarget(entryIndex, lineIndex, candidateLine!!) }
    }

    private inline fun <T> traceMutationStage(
        operation: String,
        date: LocalDate,
        stage: String,
        block: () -> T
    ): T {
        val startedAt = System.currentTimeMillis()
        return block().also {
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed >= SLOW_MUTATION_STAGE_MS) {
                Log.w(TODO_PIPELINE_TAG, "Slow $operation stage: date=$date stage=$stage elapsed=${elapsed}ms")
            } else {
                Log.d(TODO_PIPELINE_TAG, "Perf $operation stage: date=$date stage=$stage elapsed=${elapsed}ms")
            }
        }
    }

    private fun countWords(entries: List<String>): Int =
        countWordsIgnoringChecklistMarkers(entries)

    private companion object {
        private const val TODO_PIPELINE_TAG = "YajaTodoPipeline"
        private const val SLOW_MUTATION_STAGE_MS = 500L
    }
}
