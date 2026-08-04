package com.mj.yaja.data

import android.util.Log
import com.mj.yaja.data.database.JournalCacheDao
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal class JournalQueryService(
    private val cache: ConcurrentHashMap<LocalDate, List<String>>,
    private val entryCountCache: ConcurrentHashMap<LocalDate, Int>,
    private val wordCountCache: ConcurrentHashMap<LocalDate, Int>,
    private val dao: JournalCacheDao,
    private val isCachePopulated: () -> Boolean,
    private val allJournalDatesProvider: (forceRefresh: Boolean) -> Set<LocalDate>,
    private val entriesForDateProvider: (LocalDate) -> List<String>,
    private val loadEntryCountCache: () -> Boolean,
    private val loadWordCountCache: () -> Boolean,
    private val saveEntryCountCache: () -> Unit,
    private val saveWordCountCache: () -> Unit
) {
    fun getAllJournalDatesWithData(): Set<LocalDate> {
        return if (isCachePopulated()) cache.keys.toSet() else allJournalDatesProvider(false)
    }

    fun getCacheSnapshot(): Map<LocalDate, List<String>> {
        if (isCachePopulated()) {
            return synchronized(cache) { cache.mapValues { (_, entries) -> entries.toList() } }
        }
        try {
            val allDays = runRoomOffMain { dao.getAllDaysSync(DEFAULT_JOURNAL_ID) }
            if (allDays.isNotEmpty()) {
                val snapshot = linkedMapOf<LocalDate, List<String>>()
                allDays.forEach { entity ->
                    val date = LocalDate.parse(entity.date)
                    snapshot[date] = entity.entries
                }
                return snapshot
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cache snapshot from Room database", e)
        }
        val snapshot = linkedMapOf<LocalDate, List<String>>()
        allJournalDatesProvider(false).sortedDescending().forEach { date ->
            val entries = entriesForDateProvider(date)
            if (entries.isNotEmpty()) {
                snapshot[date] = entries
            }
        }
        return snapshot
    }

    /** Snapshot only the requested dates, without forcing a full-journal snapshot. */
    fun getEntriesSnapshotForDates(dates: Iterable<LocalDate>): Map<LocalDate, List<String>> {
        val snapshot = linkedMapOf<LocalDate, List<String>>()
        dates.forEach { date ->
            val entries = entriesForDateProvider(date)
            if (entries.isNotEmpty()) {
                snapshot[date] = entries
            }
        }
        return snapshot
    }

    fun getDailyMetricsSnapshotForDates(dates: Iterable<LocalDate>): Map<LocalDate, DailyJournalMetrics> {
        if (entryCountCache.isEmpty()) {
            loadEntryCountCache()
        }
        if (wordCountCache.isEmpty()) {
            loadWordCountCache()
        }
        val snapshot = linkedMapOf<LocalDate, DailyJournalMetrics>()
        var didMutateCache = false
        dates.forEach { date ->
            val cachedEntryCount = entryCountCache[date]
            val cachedWordCount = wordCountCache[date]
            if (cachedEntryCount != null && cachedWordCount != null) {
                snapshot[date] = DailyJournalMetrics(
                    entryCount = cachedEntryCount,
                    wordCount = cachedWordCount
                )
            } else {
                val entries = entriesForDateProvider(date)
                if (entries.isNotEmpty()) {
                    val metrics = DailyJournalMetrics(
                        entryCount = entries.size,
                        wordCount = countWordsIgnoringChecklistMarkers(entries)
                    )
                    entryCountCache[date] = metrics.entryCount
                    wordCountCache[date] = metrics.wordCount
                    didMutateCache = true
                    snapshot[date] = metrics
                }
            }
        }
        if (didMutateCache) {
            saveEntryCountCache()
            saveWordCountCache()
        }
        return snapshot
    }

    fun getWordCountSnapshotSince(startDate: LocalDate): Map<LocalDate, Int> {
        if (wordCountCache.isEmpty()) {
            loadWordCountCache()
        }
        val snapshot = linkedMapOf<LocalDate, Int>()
        var didMutateCache = false
        allJournalDatesProvider(true)
            .asSequence()
            .filter { !it.isBefore(startDate) }
            .sorted()
            .forEach { date ->
                val cachedCount = wordCountCache[date]
                if (cachedCount != null) {
                    snapshot[date] = cachedCount
                } else {
                    val entries = entriesForDateProvider(date)
                    if (entries.isNotEmpty()) {
                        val count = countWordsIgnoringChecklistMarkers(entries)
                        entryCountCache[date] = entries.size
                        wordCountCache[date] = count
                        didMutateCache = true
                        snapshot[date] = count
                    }
                }
            }
        if (didMutateCache) {
            saveEntryCountCache()
            saveWordCountCache()
        }
        return snapshot
    }

    private fun <T> runRoomOffMain(block: () -> T): T =
        if (Thread.currentThread().name == MAIN_THREAD_NAME) {
            runBlocking { withContext(Dispatchers.IO) { block() } }
        } else {
            block()
        }

    private companion object {
        private const val TAG = "JournalQueryService"
        private const val DEFAULT_JOURNAL_ID = "default"
        private const val MAIN_THREAD_NAME = "main"
    }
}
