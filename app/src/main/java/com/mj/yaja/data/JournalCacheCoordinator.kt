package com.mj.yaja.data

import android.content.Context
import com.mj.yaja.data.storage.JournalCacheMetadataStore
import com.mj.yaja.data.storage.JournalStorage
import com.mj.yaja.data.storage.JournalStorageFingerprint
import java.io.File
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class JournalCacheCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val journalStorage: JournalStorage,
    private val metadataStore: JournalCacheMetadataStore,
    private val cache: ConcurrentHashMap<LocalDate, List<String>>,
    private val dayLabels: ConcurrentHashMap<LocalDate, String>,
    private val settingsRepository: SettingsRepository,
    private val cachedDatesProvider: () -> Set<LocalDate>?,
    private val setCachedDates: (Set<LocalDate>?) -> Unit,
    private val isCachePopulated: () -> Boolean,
    private val totalCachedEntriesProvider: () -> Int,
    private val runRoomCount: () -> Int,
    private val markTodoFingerprint: (JournalStorageFingerprint?) -> Unit
) {
    private var fingerprintRefreshJob: Job? = null

    fun getCachedJournalDates(): Set<LocalDate> =
        cachedDatesProvider() ?: cache.keys.toSet()

    fun getJournalCacheAgeMillis(): Long? {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) return null
        return (System.currentTimeMillis() - dbFile.lastModified()).coerceAtLeast(0L)
    }

    fun getDatabaseSize(): Long {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) return 0L
        var totalSize = dbFile.length()
        val walFile = File(dbFile.path + "-wal")
        if (walFile.exists()) {
            totalSize += walFile.length()
        }
        val shmFile = File(dbFile.path + "-shm")
        if (shmFile.exists()) {
            totalSize += shmFile.length()
        }
        return totalSize
    }

    fun getCachedDaysCount(): Int =
        runCatching { runRoomCount() }.getOrDefault(0)

    fun computeCurrentJournalFingerprint(
        knownDates: Collection<LocalDate> = getCachedJournalDates(),
        anchorDate: LocalDate = LocalDate.now()
    ): JournalStorageFingerprint? =
        journalStorage.computeSampledStorageFingerprint(
            uriString = settingsRepository.storageUri.value,
            knownDates = knownDates,
            anchorDate = anchorDate
        )

    fun persistCurrentJournalFingerprint(
        immediate: Boolean = false,
        knownDates: Collection<LocalDate> = getCachedJournalDates(),
        anchorDate: LocalDate = LocalDate.now()
    ): JournalStorageFingerprint? {
        val fingerprint = computeCurrentJournalFingerprint(knownDates, anchorDate)
        storeJournalFingerprint(fingerprint, immediate)
        return fingerprint
    }

    fun storeJournalFingerprint(
        fingerprint: JournalStorageFingerprint?,
        immediate: Boolean = false
    ) {
        if (immediate) {
            metadataStore.saveFingerprintBlocking(fingerprint)
        } else {
            metadataStore.saveFingerprint(fingerprint)
        }
    }

    fun scheduleFingerprintRefresh(delayMillis: Long = 600L) {
        fingerprintRefreshJob?.cancel()
        fingerprintRefreshJob = scope.launch {
            delay(delayMillis)
            val fingerprint = persistCurrentJournalFingerprint(
                immediate = false,
                knownDates = getCachedJournalDates(),
                anchorDate = LocalDate.now()
            )
            markTodoFingerprint(fingerprint)
        }
    }

    fun cancelFingerprintRefresh() {
        fingerprintRefreshJob?.cancel()
        fingerprintRefreshJob = null
    }

    fun getCachedEntriesForDate(date: LocalDate): List<String>? =
        cache[date]?.toList()

    fun getCachedDayLabel(date: LocalDate): String =
        dayLabels[date].orEmpty()

    fun getAllJournalDatesLightweight(forceRefresh: Boolean = false): Set<LocalDate> {
        if (!forceRefresh) {
            cachedDatesProvider()?.let { return it }
        }
        val dates = journalStorage.listJournalDates(settingsRepository.storageUri.value)
        setCachedDates(dates)
        return dates
    }

    fun updateCachedDatePresence(date: LocalDate, hasEntries: Boolean) {
        val base = cachedDatesProvider()
        if (base != null && base.contains(date) == hasEntries) return
        val next = base?.toMutableSet() ?: mutableSetOf()
        if (hasEntries) next.add(date) else next.remove(date)
        setCachedDates(next)
    }

    fun updatePersistedEntryCount(delta: Int) {
        val current = settingsRepository.lastKnownEntryCount.value
        val nextCount = when {
            current >= 0 -> (current + delta).coerceAtLeast(0)
            isCachePopulated() -> totalCachedEntriesProvider()
            else -> return
        }
        settingsRepository.setLastKnownEntryCount(nextCount)
    }

    private companion object {
        private const val DATABASE_NAME = "journal_database"
    }
}
