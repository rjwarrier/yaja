package com.mj.yaja.data

import android.content.Context
import android.util.Log
import com.mj.yaja.data.database.JournalCacheDao
import com.mj.yaja.data.database.JournalDatabase
import com.mj.yaja.data.database.JournalDayCacheEntity
import com.mj.yaja.data.storage.JournalCacheMetadataStore
import com.mj.yaja.data.storage.JournalStorage
import com.mj.yaja.data.storage.JournalStorageFingerprint
import java.io.File
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal class JournalCacheCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val journalStorage: JournalStorage,
    private val metadataStore: JournalCacheMetadataStore,
    private val dao: JournalCacheDao,
    private val database: JournalDatabase,
    private val cache: ConcurrentHashMap<LocalDate, List<String>>,
    private val entryCountCache: ConcurrentHashMap<LocalDate, Int>,
    private val wordCountCache: ConcurrentHashMap<LocalDate, Int>,
    private val dateMetadataCache: ConcurrentHashMap<LocalDate, DateFileMetadata>,
    private val starredDates: ConcurrentHashMap<LocalDate, String>,
    private val dayLabels: ConcurrentHashMap<LocalDate, String>,
    private val revisitDates: ConcurrentHashMap<LocalDate, LocalDate>,
    private val revisitNotes: ConcurrentHashMap<LocalDate, String>,
    private val settingsRepository: SettingsRepository,
    private val cachedDatesProvider: () -> Set<LocalDate>?,
    private val setCachedDates: (Set<LocalDate>?) -> Unit,
    private val isCachePopulated: () -> Boolean,
    private val setCachePopulated: (Boolean) -> Unit,
    private val isFrontmatterPopulated: () -> Boolean,
    private val setFrontmatterPopulated: (Boolean) -> Unit,
    private val totalCachedEntriesProvider: () -> Int,
    private val runRoomCount: () -> Int,
    private val saveDateMetadataCache: () -> Unit,
    private val markTodoFingerprint: (JournalStorageFingerprint?) -> Unit
) {
    private var fingerprintRefreshJob: Job? = null
    private var warmupJob: Job? = null
    @Volatile private var cacheEpoch: Long = 0L

    private fun <T> runRoomOffMain(block: () -> T): T =
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            runBlocking(Dispatchers.IO) { block() }
        } else {
            block()
        }

    fun saveDayToDb(date: LocalDate, immediate: Boolean = false) {
        val entries = cache[date]
        val entryCount = entryCountCache[date] ?: entries?.size ?: 0
        val wordCount = wordCountCache[date] ?: countWordsIgnoringChecklistMarkers(entries ?: emptyList())
        val metadata = dateMetadataCache[date]
        val isStarred = starredDates.containsKey(date)
        val label = dayLabels[date] ?: starredDates[date] ?: ""
        val revisitOn = revisitDates[date]?.toString()
        val revisitNote = revisitNotes[date] ?: ""

        val entity = JournalDayCacheEntity(
            date = date.toString(),
            journalId = DEFAULT_JOURNAL_ID,
            entries = entries ?: emptyList(),
            isStarred = isStarred,
            label = label,
            revisitOn = revisitOn,
            revisitNote = revisitNote,
            wordCount = wordCount,
            entryCount = entryCount,
            fileModifiedAt = metadata?.modifiedAt ?: 0L,
            fileSize = metadata?.size ?: 0L
        )

        if (immediate) {
            runRoomOffMain { dao.insertOrUpdate(entity) }
        } else {
            scope.launch {
                dao.insertOrUpdate(entity)
            }
        }
    }

    fun deleteDayFromDb(date: LocalDate, immediate: Boolean = false) {
        if (immediate) {
            runRoomOffMain { dao.delete(DEFAULT_JOURNAL_ID, date.toString()) }
        } else {
            scope.launch {
                dao.delete(DEFAULT_JOURNAL_ID, date.toString())
            }
        }
    }

    fun ensureCachePopulated(lock: Any, migrationJob: Job, onProgress: ((Int, Int) -> Unit)? = null) {
        if (!isCachePopulated()) {
            synchronized(lock) {
                if (!isCachePopulated()) {
                    awaitMigrationIfNeeded(migrationJob)
                    if (primeCachesFromDiskLocked()) {
                        scope.launch {
                            refreshHotCacheWindow(onProgress = onProgress)
                        }
                    } else {
                        populateCache(onProgress)
                    }
                }
            }
        }
    }

    fun ensureFrontmatterPopulated(lock: Any, migrationJob: Job) {
        if (!isFrontmatterPopulated()) {
            synchronized(lock) {
                if (!isFrontmatterPopulated()) {
                    awaitMigrationIfNeeded(migrationJob)
                    primeCachesFromDiskLocked()
                    setFrontmatterPopulated(true)
                }
            }
        }
    }

    private fun awaitMigrationIfNeeded(migrationJob: Job) {
        if (!migrationJob.isCompleted && android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            runBlocking {
                migrationJob.join()
            }
        }
    }

    fun primeCachesFromDisk(lock: Any): Boolean =
        synchronized(lock) { primeCachesFromDiskLocked() }

    private fun primeCachesFromDiskLocked(): Boolean {
        var loadedAnything = false
        try {
            val allDays = runRoomOffMain { dao.getAllDaysSync(DEFAULT_JOURNAL_ID) }
            if (allDays.isNotEmpty()) {
                clearMemoryState()

                allDays.forEach { entity ->
                    val date = LocalDate.parse(entity.date)
                    cache[date] = entity.entries
                    entryCountCache[date] = entity.entryCount
                    wordCountCache[date] = entity.wordCount
                    dateMetadataCache[date] = DateFileMetadata(entity.fileSize, entity.fileModifiedAt)
                    if (entity.isStarred) {
                        starredDates[date] = entity.label
                    }
                    if (entity.label.isNotEmpty()) {
                        dayLabels[date] = entity.label
                    }
                    entity.revisitOn?.let {
                        runCatching { LocalDate.parse(it) }.onSuccess { parsed ->
                            revisitDates[date] = parsed
                        }
                    }
                    if (entity.revisitNote.isNotEmpty()) {
                        revisitNotes[date] = entity.revisitNote
                    }
                }
                setCachePopulated(true)
                setFrontmatterPopulated(true)
                setCachedDates(cache.keys.toSet())
                loadedAnything = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prime caches from Room", e)
        }
        return loadedAnything
    }

    fun forceRefresh(lock: Any, onProgress: (Int, Int) -> Unit) {
        invalidateCache(lock)
        populateCache(onProgress)
        setCachePopulated(true)
        persistCurrentJournalFingerprint(
            immediate = true,
            knownDates = cachedDatesProvider() ?: cache.keys.toSet(),
            anchorDate = LocalDate.now()
        )
    }

    private fun populateCache(onProgress: ((Int, Int) -> Unit)? = null) {
        val expectedEpoch = cacheEpoch
        val orderedDates = getAllJournalDatesLightweight().sortedDescending()
        clearMemoryState()
        val total = orderedDates.size.coerceAtLeast(1)
        val entities = mutableListOf<JournalDayCacheEntity>()
        orderedDates.forEachIndexed { index, date ->
            scope.coroutineContext.ensureActive()
            if (expectedEpoch != cacheEpoch) return
            val parsed = journalStorage.readDateContent(date)?.let { parseJournalDateContent(it) }
            val entries = parsed?.entries.orEmpty()
            val metadata = parsed?.let { readDateMetadata(date) }
            synchronized(this) {
                if (expectedEpoch != cacheEpoch) return
                if (parsed != null && (entries.isNotEmpty() || hasFrontmatterData(parsed.frontmatter))) {
                    applyScannedDateToMemoryState(date, entries, parsed.frontmatter, metadata)
                    entities.add(buildJournalDayCacheEntity(date, entries, parsed.frontmatter, metadata))
                }
            }
            onProgress?.invoke(index + 1, total)
        }

        if (expectedEpoch == cacheEpoch && entities.isNotEmpty()) {
            runRoomOffMain { dao.insertAll(entities) }
        }
        setCachePopulated(true)
        setFrontmatterPopulated(true)
        setCachedDates(orderedDates.toSet())
    }

    fun refreshHotCacheWindow(
        monthsBack: Long = 12L,
        onProgress: ((Int, Int) -> Unit)? = null
    ) {
        val expectedEpoch = cacheEpoch
        val allDates = getAllJournalDatesLightweight(forceRefresh = true)
        val cutoff = LocalDate.now().minusMonths(monthsBack)
        val hotDates = allDates.filter { !it.isBefore(cutoff) }.sortedDescending()
        val total = hotDates.size.coerceAtLeast(1)
        val toInsert = mutableListOf<JournalDayCacheEntity>()
        val toDelete = mutableListOf<String>()
        hotDates.forEachIndexed { index, date ->
            scope.coroutineContext.ensureActive()
            if (expectedEpoch != cacheEpoch) return
            val metadata = readDateMetadata(date)
            if (metadata == null) {
                removeScannedDateFromMemoryState(date)
                toDelete.add(date.toString())
                if (toDelete.size >= BATCH_SIZE) {
                    runRoomOffMain { dao.deleteDays(DEFAULT_JOURNAL_ID, toDelete) }
                    toDelete.clear()
                }
                onProgress?.invoke(index + 1, total)
                return@forEachIndexed
            }
            val previousMetadata = dateMetadataCache[date]
            val knowsDateWithoutEntries = previousMetadata == metadata && dateMetadataCache.containsKey(date)
            val needsRead = previousMetadata != metadata || (!cache.containsKey(date) && !knowsDateWithoutEntries)
            if (needsRead) {
                val parsed = journalStorage.readDateContent(date)?.let { parseJournalDateContent(it) }
                val entries = parsed?.entries.orEmpty()
                if (expectedEpoch != cacheEpoch) return
                if (parsed != null && (entries.isNotEmpty() || hasFrontmatterData(parsed.frontmatter))) {
                    applyScannedDateToMemoryState(date, entries, parsed.frontmatter, metadata)
                    toInsert.add(buildJournalDayCacheEntity(date, entries, parsed.frontmatter, metadata))
                    if (toInsert.size >= BATCH_SIZE) {
                        runRoomOffMain { dao.insertAll(toInsert) }
                        toInsert.clear()
                    }
                } else {
                    removeScannedDateFromMemoryState(date)
                    toDelete.add(date.toString())
                    if (toDelete.size >= BATCH_SIZE) {
                        runRoomOffMain { dao.deleteDays(DEFAULT_JOURNAL_ID, toDelete) }
                        toDelete.clear()
                    }
                }
            }
            onProgress?.invoke(index + 1, total)
        }
        if (expectedEpoch == cacheEpoch) {
            if (toInsert.isNotEmpty()) {
                runRoomOffMain { dao.insertAll(toInsert) }
            }
            if (toDelete.isNotEmpty()) {
                runRoomOffMain { dao.deleteDays(DEFAULT_JOURNAL_ID, toDelete) }
            }
        }
        setCachedDates(allDates)
    }

    fun startIncrementalWarmup(
        lock: Any,
        ensureFrontmatterPopulated: () -> Unit,
        latestFirst: Boolean = true,
        onProgress: ((Int, Int) -> Unit)? = null
    ) {
        if (isCachePopulated()) return
        synchronized(lock) {
            if (isCachePopulated() || warmupJob?.isActive == true) return
            ensureFrontmatterPopulated()
            warmupJob = scope.launch {
                val expectedEpoch = cacheEpoch
                try {
                    val orderedDates = if (latestFirst) {
                        getAllJournalDatesLightweight().sortedDescending()
                    } else {
                        getAllJournalDatesLightweight().sorted()
                    }
                    val total = orderedDates.size.coerceAtLeast(1)
                    val toInsert = mutableListOf<JournalDayCacheEntity>()
                    val toDelete = mutableListOf<String>()
                    orderedDates.forEachIndexed { index, date ->
                        ensureActive()
                        if (expectedEpoch != cacheEpoch) return@launch
                        if (!cache.containsKey(date) && !dateMetadataCache.containsKey(date)) {
                            val parsed = journalStorage.readDateContent(date)?.let { parseJournalDateContent(it) }
                            val entries = parsed?.entries.orEmpty()
                            if (expectedEpoch != cacheEpoch) return@launch
                            if (parsed != null && (entries.isNotEmpty() || hasFrontmatterData(parsed.frontmatter))) {
                                val metadata = readDateMetadata(date)
                                applyScannedDateToMemoryState(date, entries, parsed.frontmatter, metadata)
                                toInsert.add(buildJournalDayCacheEntity(date, entries, parsed.frontmatter, metadata))
                                if (toInsert.size >= BATCH_SIZE) {
                                    dao.insertAll(toInsert)
                                    toInsert.clear()
                                }
                            } else {
                                removeScannedDateFromMemoryState(date)
                                toDelete.add(date.toString())
                                if (toDelete.size >= BATCH_SIZE) {
                                    dao.deleteDays(DEFAULT_JOURNAL_ID, toDelete)
                                    toDelete.clear()
                                }
                            }
                        }
                        onProgress?.invoke(index + 1, total)
                    }
                    if (expectedEpoch == cacheEpoch) {
                        if (toInsert.isNotEmpty()) {
                            dao.insertAll(toInsert)
                        }
                        if (toDelete.isNotEmpty()) {
                            dao.deleteDays(DEFAULT_JOURNAL_ID, toDelete)
                        }
                    }
                    setCachePopulated(true)
                    setCachedDates(orderedDates.toSet())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Incremental cache warmup failed", e)
                } finally {
                    if (warmupJob?.isActive != true) {
                        warmupJob = null
                    }
                }
            }
        }
    }

    fun invalidateCache(lock: Any) {
        synchronized(lock) {
            warmupJob?.cancel()
            warmupJob = null
            cancelFingerprintRefresh()
            cacheEpoch += 1
            clearMemoryState()
            setCachePopulated(false)
            setFrontmatterPopulated(false)
            setCachedDates(null)
            journalStorage.invalidateDirectoryCache()

            scope.launch {
                dao.deleteAll(DEFAULT_JOURNAL_ID)
                try {
                    database.openHelper.writableDatabase.execSQL("VACUUM")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to vacuum database after clearing cache", e)
                }
            }
            metadataStore.clear()
        }
    }

    private fun readDateMetadata(date: LocalDate): DateFileMetadata? =
        journalStorage.getDateFileMetadata(date, settingsRepository.storageUri.value)
            ?.let { DateFileMetadata(it.first, it.second) }

    private fun hasFrontmatterData(frontmatter: MarkdownFileManager.Companion.FrontmatterSnapshot): Boolean =
        frontmatter.isStarred ||
            frontmatter.label.isNotEmpty() ||
            frontmatter.revisitOn != null ||
            frontmatter.revisitNote.isNotEmpty()

    private fun buildJournalDayCacheEntity(
        date: LocalDate,
        entries: List<String>,
        frontmatter: MarkdownFileManager.Companion.FrontmatterSnapshot,
        metadata: DateFileMetadata?
    ): JournalDayCacheEntity =
        JournalDayCacheEntity(
            date = date.toString(),
            journalId = DEFAULT_JOURNAL_ID,
            entries = entries,
            isStarred = frontmatter.isStarred,
            label = frontmatter.label,
            revisitOn = frontmatter.revisitOn?.toString(),
            revisitNote = frontmatter.revisitNote,
            wordCount = countWordsIgnoringChecklistMarkers(entries),
            entryCount = entries.size,
            fileModifiedAt = metadata?.modifiedAt ?: 0L,
            fileSize = metadata?.size ?: 0L
        )

    private fun applyScannedDateToMemoryState(
        date: LocalDate,
        entries: List<String>,
        frontmatter: MarkdownFileManager.Companion.FrontmatterSnapshot,
        metadata: DateFileMetadata?
    ) {
        if (entries.isNotEmpty()) {
            cache[date] = entries
            entryCountCache[date] = entries.size
            wordCountCache[date] = countWordsIgnoringChecklistMarkers(entries)
        } else {
            cache.remove(date)
            entryCountCache.remove(date)
            wordCountCache.remove(date)
        }
        metadata?.let { dateMetadataCache[date] = it } ?: dateMetadataCache.remove(date)
        applyFrontmatterToMemoryState(
            date = date,
            frontmatter = frontmatter,
            starredDates = starredDates,
            dayLabels = dayLabels,
            revisitDates = revisitDates,
            revisitNotes = revisitNotes
        )
    }

    private fun removeScannedDateFromMemoryState(date: LocalDate) {
        cache.remove(date)
        entryCountCache.remove(date)
        wordCountCache.remove(date)
        dateMetadataCache.remove(date)
        starredDates.remove(date)
        dayLabels.remove(date)
        revisitDates.remove(date)
        revisitNotes.remove(date)
    }

    private fun clearMemoryState() {
        cache.clear()
        entryCountCache.clear()
        wordCountCache.clear()
        dateMetadataCache.clear()
        starredDates.clear()
        dayLabels.clear()
        revisitDates.clear()
        revisitNotes.clear()
    }

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
        private const val TAG = "JournalCacheCoordinator"
        private const val DATABASE_NAME = "journal_database"
        private const val DEFAULT_JOURNAL_ID = "default"
        private const val BATCH_SIZE = 50
    }
}
