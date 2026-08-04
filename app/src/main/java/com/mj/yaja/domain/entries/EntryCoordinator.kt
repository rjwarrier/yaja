package com.mj.yaja.domain.entries

import android.util.Log
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.SettingsRepository
import com.mj.yaja.domain.keywords.KeywordCoordinator
import com.mj.yaja.ui.viewmodel.JournalUiState
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class DeletedEntryBatch(
    val date: LocalDate,
    val entries: List<DeletedEntry>,
    val originalEntryCount: Int = 0,
    val pendingCommit: Boolean = false
)

data class DeletedEntry(
    val entry: String,
    val originalIndex: Int
)

data class DeleteCommitResult(
    val date: LocalDate,
    val deletedCount: Int
)

// Compiled once instead of per updateEntry call.
private val ENTRY_TIME_METADATA_REGEX =
    Regex("^<!--time:(\\d{2}:\\d{2})(?:, added on (.*?))?-->\\n?")

class EntryCoordinator(
    private val fileManager: MarkdownFileManager,
    private val settingsRepository: SettingsRepository,
    private val keywordCoordinator: KeywordCoordinator,
    private val scope: CoroutineScope,
    private val uiStateFlow: MutableStateFlow<JournalUiState>,
    private val lastDeletedFlow: MutableStateFlow<DeletedEntryBatch?>
) {
    private val deletedEntryBatchMutex = Mutex()

    suspend fun addEntry(entry: String, customTime: String? = null) {
        val currentDate = uiStateFlow.value.selectedDate

        var finalEntry = entry
        if (!finalEntry.startsWith("<!--time:")) {
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val timeString = customTime ?: LocalTime.now().format(timeFormatter)
            val timestamp = buildTimestampMarker(currentDate, timeString)
            finalEntry = "$timestamp\n$finalEntry"
        }

        val mutation = runStorageMutation("addEntry", currentDate) {
            fileManager.tryAddEntryForDate(currentDate, finalEntry)
        }
        if (!mutation.success) throw IOException("Failed to save entry for $currentDate")
        val freshEntries = mutation.entries
        uiStateFlow.value = uiStateFlow.value.copy(entries = freshEntries)
        addDateToCalendar(currentDate)
        reindexKeywordsForDateInBackground(currentDate, freshEntries)
        requestHeatmapWidgetUpdateInBackground()
    }

    fun startEditing(entry: String, index: Int) {
        uiStateFlow.value = uiStateFlow.value.copy(editingEntry = entry, editingIndex = index)
    }

    fun clearEditing() {
        uiStateFlow.value = uiStateFlow.value.copy(editingEntry = null, editingIndex = -1)
    }

    suspend fun updateEntry(newEntry: String, customTime: String? = null) {
        val currentDate = uiStateFlow.value.selectedDate
        val oldEntry = uiStateFlow.value.editingEntry ?: return
        val index = uiStateFlow.value.editingIndex
        if (index == -1) return

        var finalNewEntry = newEntry
        val match = ENTRY_TIME_METADATA_REGEX.find(oldEntry)
        if (!finalNewEntry.startsWith("<!--time:")) {
            finalNewEntry =
                when {
                    customTime != null -> {
                        val addedOn = match?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() }
                        buildTimestampMarker(currentDate, customTime, addedOn) + "\n" + finalNewEntry
                    }
                    match != null -> "${match.value}$finalNewEntry"
                    else -> {
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        buildTimestampMarker(
                            currentDate,
                            LocalTime.now().format(timeFormatter)
                        ) + "\n" + finalNewEntry
                    }
                }
        }

        val mutation = runStorageMutation("updateEntry", currentDate) {
            fileManager.tryUpdateEntryForDate(currentDate, index, finalNewEntry)
        }
        if (!mutation.success) throw IOException("Failed to update entry for $currentDate")
        val freshEntries = mutation.entries
        clearEditing()
        uiStateFlow.value = uiStateFlow.value.copy(entries = freshEntries)
        reindexKeywordsForDateInBackground(currentDate, freshEntries)
        requestHeatmapWidgetUpdateInBackground()
    }

    suspend fun deleteEntry(index: Int): Boolean {
        val currentDate = uiStateFlow.value.selectedDate
        val entries = uiStateFlow.value.entries
        val entry = entries.getOrNull(index) ?: return false

        val mutation = runStorageMutation("deleteEntry", currentDate) {
            fileManager.tryDeleteEntryForDate(currentDate, index)
        }
        if (!mutation.success) {
            lastDeletedFlow.value = null
            uiStateFlow.value =
                uiStateFlow.value.copy(
                    entries = withContext(Dispatchers.IO) {
                        fileManager.getEntriesForDateFromDisk(currentDate)
                    }
                )
            Log.w(TAG, "deleteEntry failed safely for $currentDate index=$index")
            return false
        }
        val freshEntries = mutation.entries
        deletedEntryBatchMutex.withLock {
            lastDeletedFlow.value =
                DeletedEntryBatch(
                    date = currentDate,
                    entries = listOf(DeletedEntry(entry = entry, originalIndex = index))
                )
        }
        uiStateFlow.value = uiStateFlow.value.copy(entries = freshEntries)
        if (entries.size == 1) removeDateFromCalendar(currentDate)
        reindexKeywordsForDateInBackground(currentDate, freshEntries)
        requestHeatmapWidgetUpdateInBackground()
        return true
    }

    suspend fun deleteEntries(indices: Set<Int>): Boolean {
        val currentDate = uiStateFlow.value.selectedDate
        val entries = uiStateFlow.value.entries
        val validIndices = indices.filter { it in entries.indices }.distinct().sorted()
        if (validIndices.isEmpty()) return false

        val deletedEntries = validIndices.map { index ->
            DeletedEntry(entry = entries[index], originalIndex = index)
        }
        deletedEntryBatchMutex.withLock {
            lastDeletedFlow.value =
                DeletedEntryBatch(
                    date = currentDate,
                    entries = deletedEntries,
                    originalEntryCount = entries.size,
                    pendingCommit = true
                )
        }
        return true
    }

    suspend fun commitPendingDelete(): DeleteCommitResult? {
        val batch = deletedEntryBatchMutex.withLock {
            val pendingBatch = lastDeletedFlow.value?.takeIf { it.pendingCommit }
            if (pendingBatch != null) lastDeletedFlow.value = null
            pendingBatch
        } ?: return null

        val date = batch.date
        val selectedDate = uiStateFlow.value.selectedDate
        val freshEntries = runStorageMutation("commitPendingDelete", date) {
            var currentEntries = fileManager.getEntriesForDateFromDisk(date)
            batch.entries.sortedByDescending { it.originalIndex }.forEach { deleted ->
                val targetIndex =
                    if (currentEntries.getOrNull(deleted.originalIndex) == deleted.entry) {
                        deleted.originalIndex
                    } else {
                        currentEntries.indexOf(deleted.entry)
                    }
                if (targetIndex >= 0) {
                    val mutation = fileManager.tryDeleteEntryForDate(date, targetIndex)
                    if (!mutation.success) {
                        throw IOException("Failed to delete pending entry for $date at $targetIndex")
                    }
                    currentEntries = mutation.entries
                } else {
                    Log.w(TAG, "Pending delete skipped; entry no longer found for $date")
                }
            }
            fileManager.getEntriesForDateFromDisk(selectedDate)
        }

        val verifiedEntries = withContext(Dispatchers.IO) { fileManager.getEntriesForDateFromDisk(date) }
        val expectedCount = (batch.originalEntryCount - batch.entries.size).coerceAtLeast(0)
        if (verifiedEntries.size != expectedCount) {
            Log.e(
                TAG,
                "Bulk deletion count verification failed for $date. " +
                    "old=${batch.originalEntryCount}, selected=${batch.entries.size}, " +
                    "expected=$expectedCount, actual=${verifiedEntries.size}"
            )
            uiStateFlow.value =
                uiStateFlow.value.copy(
                    entries = if (selectedDate == date) verifiedEntries else freshEntries
                )
            throw IOException("Bulk Deletion Failed")
        }

        uiStateFlow.value = uiStateFlow.value.copy(entries = freshEntries)
        val remainingEntries = verifiedEntries
        if (remainingEntries.isEmpty()) removeDateFromCalendar(date) else addDateToCalendar(date)
        reindexKeywordsForDateInBackground(date, remainingEntries)
        requestHeatmapWidgetUpdateInBackground()
        return DeleteCommitResult(date = date, deletedCount = batch.entries.size)
    }

    suspend fun undoDelete(): Int {
        val batch = deletedEntryBatchMutex.withLock {
            val pendingBatch = lastDeletedFlow.value
            if (pendingBatch != null) lastDeletedFlow.value = null
            pendingBatch
        } ?: return 0
        if (batch.pendingCommit) {
            val selectedDate = uiStateFlow.value.selectedDate
            uiStateFlow.value =
                uiStateFlow.value.copy(
                    entries = withContext(Dispatchers.IO) {
                        fileManager.getEntriesForDateFromDisk(selectedDate)
                    }
                )
            return batch.entries.size
        }
        val date = batch.date
        val selectedDate = uiStateFlow.value.selectedDate
        val freshEntries =
            try {
                runStorageMutation("undoDelete", date) {
                    val restoredEntries = fileManager.getEntriesForDateFromDisk(date).toMutableList()
                    batch.entries.sortedBy { it.originalIndex }.forEach { deleted ->
                        val alreadyRestoredAtOriginalIndex =
                            restoredEntries.getOrNull(deleted.originalIndex) == deleted.entry
                        if (!alreadyRestoredAtOriginalIndex) {
                            val insertAt = deleted.originalIndex.coerceIn(0, restoredEntries.size)
                            restoredEntries.add(insertAt, deleted.entry)
                        }
                    }
                    val mutation = fileManager.trySetEntriesForDate(date, restoredEntries)
                    if (!mutation.success) throw IOException("Failed to restore deleted entry for $date")
                    fileManager.getEntriesForDateFromDisk(selectedDate)
                }
            } catch (e: Exception) {
                deletedEntryBatchMutex.withLock {
                    if (lastDeletedFlow.value == null) lastDeletedFlow.value = batch
                }
                throw e
            }
        uiStateFlow.value = uiStateFlow.value.copy(entries = freshEntries)
        addDateToCalendar(date)
        reindexKeywordsForDateInBackground(
            date,
            withContext(Dispatchers.IO) { fileManager.getEntriesForDate(date) }
        )
        requestHeatmapWidgetUpdateInBackground()
        return 0
    }

    fun clearLastDeleted() {
        lastDeletedFlow.value = null
    }

    suspend fun reorderEntries(reorderedEntries: List<String>) {
        val currentDate = uiStateFlow.value.selectedDate
        val mutation = withContext(Dispatchers.IO) {
            fileManager.trySetEntriesForDate(currentDate, reorderedEntries)
        }
        if (!mutation.success) throw IOException("Failed to reorder entries for $currentDate")
        reindexKeywordsForDateInBackground(currentDate, mutation.entries)
        uiStateFlow.value = uiStateFlow.value.copy(entries = mutation.entries)
    }

    private fun reindexKeywordsForDateInBackground(date: LocalDate, entries: List<String>) {
        scope.launch(Dispatchers.Default) {
            keywordCoordinator.rebuildKeywordIndexForDate(date, entries)
        }
    }

    private fun requestHeatmapWidgetUpdateInBackground() {
        scope.launch(Dispatchers.Default) {
            settingsRepository.requestHeatmapWidgetUpdate()
        }
    }

    private suspend fun <T> runStorageMutation(
        operation: String,
        date: LocalDate,
        block: () -> T
    ): T {
        return try {
            withTimeout(MUTATION_TIMEOUT_MS) {
                withContext(Dispatchers.IO) { block() }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "$operation timed out for $date after ${MUTATION_TIMEOUT_MS}ms", e)
            throw IOException("$operation timed out. Please reopen the day and retry.", e)
        }
    }

    private fun addDateToCalendar(date: LocalDate) {
        val updated = uiStateFlow.value.datesWithEntries + date
        uiStateFlow.value = uiStateFlow.value.copy(
            datesWithEntries = updated,
            monthlyStats = calculateMonthlyStats(updated),
            yearlyStats = calculateYearlyStats(updated)
        )
    }

    private fun removeDateFromCalendar(date: LocalDate) {
        val updated = uiStateFlow.value.datesWithEntries - date
        uiStateFlow.value = uiStateFlow.value.copy(
            datesWithEntries = updated,
            monthlyStats = calculateMonthlyStats(updated),
            yearlyStats = calculateYearlyStats(updated)
        )
    }

    private fun calculateMonthlyStats(dates: Set<LocalDate>): List<Pair<YearMonth, Int>> {
        val currentMonth = YearMonth.now()
        val stats = mutableListOf<Pair<YearMonth, Int>>()

        for (i in 11 downTo 0) {
            val month = currentMonth.minusMonths(i.toLong())
            val count = dates.count { YearMonth.from(it) == month }
            stats.add(month to count)
        }
        return stats
    }

    private fun calculateYearlyStats(dates: Set<LocalDate>): List<Pair<Int, Float>> {
        if (dates.isEmpty()) return emptyList()

        val minYear = dates.minOf { it.year }
        val maxYear = maxOf(dates.maxOf { it.year }, LocalDate.now().year)
        return (minYear..maxYear).map { year ->
            val count = dates.count { it.year == year }.toFloat()
            year to count
        }
    }

    private fun buildTimestampMarker(
        currentDate: LocalDate,
        timeString: String,
        addedOnOverride: String? = null
    ): String {
        val today = LocalDate.now()
        val addedOn =
            addedOnOverride ?: if (currentDate != today) {
                val dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")
                today.format(dateFormatter)
            } else {
                null
            }

        return if (addedOn != null) {
            "<!--time:$timeString, added on $addedOn-->"
        } else {
            "<!--time:$timeString-->"
        }
    }

    private companion object {
        private const val TAG = "EntryCoordinator"
        private const val MUTATION_TIMEOUT_MS = 15_000L
    }
}
