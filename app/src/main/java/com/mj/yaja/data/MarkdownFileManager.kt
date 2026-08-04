package com.mj.yaja.data

import android.content.Context
import com.mj.yaja.data.database.JournalDatabase
import com.mj.yaja.data.database.JsonToRoomMigrator
import android.net.Uri
import android.util.Log
import com.mj.yaja.data.backup.BackupService
import com.mj.yaja.data.backup.BackupService.BackupBundle
import com.mj.yaja.data.backup.BackupService.BackupJournalDay
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.storage.JournalCacheMetadataStore
import com.mj.yaja.data.storage.JournalStorageFingerprint
import com.mj.yaja.data.storage.JournalStorage
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal data class DateFileMetadata(val size: Long, val modifiedAt: Long)

class MarkdownFileManager(
        private val context: Context,
        private val settingsRepository: SettingsRepository,
        externalScope: CoroutineScope? = null
) {
    fun getContext(): Context = context
    private val todoIndexRepository = TodoIndexRepository.getInstance(context)
    private val eventIndexRepository = EventIndexRepository.getInstance(context)
    data class EntryMutationResult(val entries: List<String>, val success: Boolean)
    data class VersionHistorySnapshotInfo(
        val id: String,
        val createdAt: Long,
        val content: String
    )
    companion object {
        private const val TAG = "MarkdownFileManager"
        private const val TODO_PIPELINE_TAG = "YajaTodoPipeline"
        private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM")
        @Volatile private var instance: MarkdownFileManager? = null

        /**
         * Returns the process-wide singleton.
         * All callers (MainActivity, QuickCaptureActivity, TaskerReceiver) share the same
         * in-memory cache, so a widget save is immediately visible in the main app.
         */
        fun getInstance(context: Context, settingsRepository: SettingsRepository): MarkdownFileManager =
                instance
                        ?: synchronized(this) {
                            instance
                                    ?: MarkdownFileManager(
                                                    context.applicationContext,
                                                    settingsRepository
                                            )
                                            .also { instance = it }
                        }

        /** Parses a list of raw file lines into individual journal entry strings. Skips YAML frontmatter and date headings. */
        internal fun parseEntries(lines: List<String>): List<String> {
            val entries = mutableListOf<String>()
            var currentEntry = StringBuilder()
            var inFrontmatter = false
            var lineIndex = 0

            for (line in lines) {
                // Skip YAML frontmatter (--- delimited)
                if (line.trim() == "---") {
                    if (lineIndex == 0) {
                        inFrontmatter = true
                    } else if (inFrontmatter) {
                        inFrontmatter = false
                    }
                    lineIndex++
                    continue
                }
                if (inFrontmatter) {
                    lineIndex++
                    continue
                }

                if (line.startsWith("# ")) continue // Skip date headings
                if (line.isBlank() && currentEntry.isEmpty()) continue

                if (line.startsWith("- ")) {
                    if (currentEntry.isNotEmpty()) {
                        entries.add(currentEntry.toString().trimEnd())
                    }
                    currentEntry = StringBuilder()
                    currentEntry.append(line.removePrefix("- "))
                } else if (line.startsWith("  ") && currentEntry.isNotEmpty()) {
                    currentEntry.append("\n").append(line.removePrefix("  "))
                } else if (currentEntry.isNotEmpty()) {
                    currentEntry.append("\n").append(line)
                }
                lineIndex++
            }
            if (currentEntry.isNotEmpty()) {
                entries.add(currentEntry.toString().trimEnd())
            }
            return entries
        }

        internal data class FrontmatterSnapshot(
            val isStarred: Boolean = false,
            val label: String = "",
            val revisitOn: LocalDate? = null,
            val revisitNote: String = ""
        )

        /** Extract frontmatter from file lines. */
        internal fun parseFrontmatter(lines: List<String>): FrontmatterSnapshot {
            if (lines.isEmpty()) return FrontmatterSnapshot()
            if (lines[0].trim() != "---") return FrontmatterSnapshot()

            var isStarred = false
            var label = ""
            var revisitOn: LocalDate? = null
            var revisitNote = ""
            var i = 1
            while (i < lines.size && lines[i].trim() != "---") {
                val line = lines[i].trim()
                if (line.startsWith("starred:")) {
                    isStarred = line.substringAfter("starred:")
                        .trim()
                        .removeSurrounding("\"")
                        .equals("true", ignoreCase = true)
                }
                if (line.startsWith("label:")) {
                    label = line.substringAfter("label:").trim().removeSurrounding("\"")
                }
                if (line.startsWith("revisit_on:")) {
                    runCatching {
                        LocalDate.parse(line.substringAfter("revisit_on:").trim().removeSurrounding("\""))
                    }.onSuccess { revisitOn = it }
                }
                if (line.startsWith("revisit_note:")) {
                    revisitNote = line.substringAfter("revisit_note:").trim().removeSurrounding("\"")
                }
                i++
            }
            return FrontmatterSnapshot(
                isStarred = isStarred,
                label = label,
                revisitOn = revisitOn,
                revisitNote = revisitNote
            )
        }
    }

    // ── In-memory cache ──────────────────────────────────────────────────
    private val cache = ConcurrentHashMap<LocalDate, List<String>>()
    private val mutationLocks = ConcurrentHashMap<LocalDate, Any>()
    private val entryCountCache = ConcurrentHashMap<LocalDate, Int>()
    private val wordCountCache = ConcurrentHashMap<LocalDate, Int>()
    private val dateMetadataCache = ConcurrentHashMap<LocalDate, DateFileMetadata>()
    private val starredDates = ConcurrentHashMap<LocalDate, String>()  // Track starred dates with labels (empty string if not starred)
    private val dayLabels = ConcurrentHashMap<LocalDate, String>()     // Day labels for ALL days (label: frontmatter field)
    private val revisitDates = ConcurrentHashMap<LocalDate, LocalDate>()
    private val revisitNotes = ConcurrentHashMap<LocalDate, String>()
    @Volatile private var cachePopulated = false
    @Volatile private var frontmatterPopulated = false
    @Volatile private var lightweightDatesCache: Set<LocalDate>? = null
    private val cacheScope = externalScope ?: CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val database = JournalDatabase.getDatabase(context)
    private val dao = database.journalCacheDao()
    private val journalSearchService =
        JournalSearchService(
            ensureSearchMetadataReady = { ensureFrontmatterPopulated() },
            isCachePopulated = { cachePopulated },
            cachedEntriesForDate = { date -> cache[date] },
            rebuildSnapshotProvider = { getEntriesSnapshotForRebuild().ifEmpty { null } },
            allJournalDatesProvider = { getAllJournalDatesLightweight() },
            labelForDate = { date -> dayLabels[date] },
            entriesForDateProvider = { date -> getEntriesForDate(date) }
        )
    private val journalQueryService =
        JournalQueryService(
            cache = cache,
            entryCountCache = entryCountCache,
            wordCountCache = wordCountCache,
            dao = dao,
            isCachePopulated = { cachePopulated },
            allJournalDatesProvider = { forceRefresh -> getAllJournalDatesLightweight(forceRefresh) },
            entriesForDateProvider = { date -> getEntriesForDate(date) },
            loadEntryCountCache = { loadEntryCountCacheFromDisk() },
            loadWordCountCache = { loadWordCountCacheFromDisk() },
            saveEntryCountCache = { saveEntryCountCacheToDisk() },
            saveWordCountCache = { saveWordCountCacheToDisk() }
        )
    private val journalBackupGateway by lazy {
        JournalBackupGateway(
            backupService = backupService,
            journalStorage = journalStorage,
            storageUriProvider = { settingsRepository.storageUri.value },
            cache = cache,
            dayLabels = dayLabels,
            starredDates = starredDates,
            revisitDates = revisitDates,
            revisitNotes = revisitNotes,
            allJournalDatesProvider = { forceRefresh -> getAllJournalDatesLightweight(forceRefresh) },
            invalidateCache = { invalidateCache() }
        )
    }
    private val journalMetadataRepository by lazy {
        JournalMetadataRepository(
            context = context,
            journalStorage = journalStorage,
            cache = cache,
            entryCountCache = entryCountCache,
            wordCountCache = wordCountCache,
            starredDates = starredDates,
            dayLabels = dayLabels,
            revisitDates = revisitDates,
            revisitNotes = revisitNotes,
            isCachePopulated = { cachePopulated },
            ensureFrontmatterPopulated = { ensureFrontmatterPopulated() },
            setFrontmatterPopulated = { populated -> frontmatterPopulated = populated },
            entriesForDateProvider = { date -> getEntriesForDate(date) },
            entriesForMutationProvider = { date -> getEntriesForDateForMutation(date) },
            contentBuilder = { date, entries -> buildFileContent(date, entries) },
            snapshotBeforeMutation = { date, reason -> snapshotDateBeforeMutation(date, reason) },
            updateCachedDatePresence = { date, hasEntries -> updateCachedDatePresence(date, hasEntries) },
            saveDayToDb = { date -> saveDayToDb(date) },
            deleteDayFromDb = { date -> deleteDayFromDb(date) },
            scheduleFingerprintRefresh = { scheduleFingerprintRefresh() },
            scheduleTodoIndexSyncForDate = { date, entries -> scheduleTodoIndexSyncForDate(date, entries) },
            removeTodoDate = { date ->
                todoIndexRepository.removeDate(date)
                eventIndexRepository.removeDate(date)
            },
            rebuildSnapshotProvider = { getEntriesSnapshotForRebuild() },
            entriesSnapshotForDatesProvider = { dates -> getEntriesSnapshotForDates(dates) },
            allJournalDatesProvider = { getAllJournalDatesLightweight() }
        )
    }
    private val journalMutationService by lazy {
        JournalMutationService(
            journalStorage = journalStorage,
            cache = cache,
            entryCountCache = entryCountCache,
            wordCountCache = wordCountCache,
            starredDates = starredDates,
            revisitDates = revisitDates,
            revisitNotes = revisitNotes,
            entriesForMutationProvider = { date -> getEntriesForDateForMutation(date) },
            directEntriesProvider = { date -> readEntriesForDateDirect(date) },
            contentBuilder = { date, entries -> buildFileContent(date, entries) },
            snapshotBeforeMutation = { date, reason -> snapshotDateBeforeMutation(date, reason) },
            updateCachedDatePresence = { date, hasEntries -> updateCachedDatePresence(date, hasEntries) },
            saveDayToDb = { date -> saveDayToDb(date) },
            deleteDayFromDb = { date -> deleteDayFromDb(date) },
            scheduleFingerprintRefresh = { scheduleFingerprintRefresh() },
            updatePersistedEntryCount = { delta -> updatePersistedEntryCount(delta) },
            updateTodoIndexRows = { date, entries -> updateTodoIndexRows(date, entries) },
            scheduleTodoIndexRowsUpdate = { date, entries -> scheduleTodoIndexRowsUpdate(date, entries) },
            scheduleEntryMutationRefresh = { date, entries -> scheduleEntryMutationRefresh(date, entries) },
            removeDateMetadata = { date -> dateMetadataCache.remove(date) },
            versionHistoryManager = versionHistoryManager,
            snapshotDateBeforeMutation = { date, reason -> snapshotDateBeforeMutation(date, reason) },
            currentDateContentProvider = { date -> journalStorage.readDateContent(date) },
            applyRawDateContentToState = { date, content -> applyRawDateContentToState(date, content) }
        )
    }

    val migrationJob: Job = cacheScope.launch {
        JsonToRoomMigrator.migrateIfNeeded(context, database)
        com.mj.yaja.data.database.KeywordMigrator.migrateIfNeeded(context, database)
        com.mj.yaja.data.database.TodoIndexMigrator.migrateIfNeeded(context, database)
        com.mj.yaja.data.database.EventIndexMigrator.migrateIfNeeded(context, database)
        com.mj.yaja.data.database.KeywordMatchMigrator.migrateIfNeeded(context, database)
    }

    private val metadataCacheFile: File by lazy { File(context.filesDir, "journal_cache_meta_v1.json") }
    private val metadataStore by lazy {
        JournalCacheMetadataStore(
            metadataFile = metadataCacheFile,
            scope = cacheScope,
            logError = { message, error -> Log.e(TAG, message, error) }
        )
    }
    private val backupService by lazy { BackupService(context, TAG) }
    private val journalCacheCoordinator by lazy {
        JournalCacheCoordinator(
            context = context,
            scope = cacheScope,
            journalStorage = journalStorage,
            metadataStore = metadataStore,
            dao = dao,
            database = database,
            cache = cache,
            entryCountCache = entryCountCache,
            wordCountCache = wordCountCache,
            dateMetadataCache = dateMetadataCache,
            starredDates = starredDates,
            dayLabels = dayLabels,
            revisitDates = revisitDates,
            revisitNotes = revisitNotes,
            settingsRepository = settingsRepository,
            cachedDatesProvider = { lightweightDatesCache },
            setCachedDates = { dates -> lightweightDatesCache = dates },
            isCachePopulated = { cachePopulated },
            setCachePopulated = { populated -> cachePopulated = populated },
            isFrontmatterPopulated = { frontmatterPopulated },
            setFrontmatterPopulated = { populated -> frontmatterPopulated = populated },
            totalCachedEntriesProvider = { cache.values.sumOf { it.size } },
            runRoomCount = { runRoomOffMain { dao.getCount("default") } },
            markTodoFingerprint = { fingerprint -> todoIndexRepository.markFingerprint(fingerprint) }
        )
    }
    private val journalIndexCoordinator by lazy {
        JournalIndexCoordinator(
            context = context,
            scope = cacheScope,
            journalStorage = journalStorage,
            settingsRepository = settingsRepository,
            dateMetadataCache = dateMetadataCache,
            todoIndexRepository = todoIndexRepository,
            eventIndexRepository = eventIndexRepository,
            dayLabelProvider = { date -> getDayLabel(date) },
            saveDateMetadataCache = { saveDateMetadataCacheToDisk() }
        )
    }

    private fun <T> runRoomOffMain(block: () -> T): T =
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            runBlocking(Dispatchers.IO) { block() }
        } else {
            block()
        }

    private fun saveDayToDb(date: LocalDate, immediate: Boolean = false) =
        journalCacheCoordinator.saveDayToDb(date, immediate)

    private fun deleteDayFromDb(date: LocalDate, immediate: Boolean = false) =
        journalCacheCoordinator.deleteDayFromDb(date, immediate)

    private fun saveLabelsCacheToDisk(immediate: Boolean = false) {}
    private fun loadLabelsCacheFromDisk(): Boolean = false
    private fun loadEntryCountCacheFromDisk(): Boolean = false
    private fun saveEntryCountCacheToDisk(immediate: Boolean = false) {}
    private fun loadWordCountCacheFromDisk(): Boolean = false
    private fun saveWordCountCacheToDisk(immediate: Boolean = false) {}
    private fun loadDateMetadataCacheFromDisk(): Boolean = false
    private fun saveDateMetadataCacheToDisk(immediate: Boolean = false) {}
    private fun persistDateIntCache(targetFile: File, source: Map<LocalDate, Int>, label: String) {}

    private fun ensureCachePopulated(onProgress: ((Int, Int) -> Unit)? = null) =
        journalCacheCoordinator.ensureCachePopulated(this, migrationJob, onProgress)

    private fun ensureFrontmatterPopulated() =
        journalCacheCoordinator.ensureFrontmatterPopulated(this, migrationJob)

    private fun saveCacheToDisk(immediate: Boolean = false) {}
    private fun loadCacheFromDisk(): Boolean = false

    fun primeCachesFromDisk(): Boolean =
        journalCacheCoordinator.primeCachesFromDisk(this)

    fun getCachedJournalDates(): Set<LocalDate> =
        journalCacheCoordinator.getCachedJournalDates()

    fun getJournalCacheAgeMillis(): Long? =
        journalCacheCoordinator.getJournalCacheAgeMillis()

    fun getDatabaseSize(): Long =
        journalCacheCoordinator.getDatabaseSize()

    fun getCachedDaysCount(): Int =
        journalCacheCoordinator.getCachedDaysCount()

    fun computeCurrentJournalFingerprint(
        knownDates: Collection<LocalDate> = getCachedJournalDates(),
        anchorDate: LocalDate = LocalDate.now()
    ): JournalStorageFingerprint? =
        journalCacheCoordinator.computeCurrentJournalFingerprint(knownDates, anchorDate)

    fun persistCurrentJournalFingerprint(
        immediate: Boolean = false,
        knownDates: Collection<LocalDate> = getCachedJournalDates(),
        anchorDate: LocalDate = LocalDate.now()
    ): JournalStorageFingerprint? =
        journalCacheCoordinator.persistCurrentJournalFingerprint(immediate, knownDates, anchorDate)

    fun storeJournalFingerprint(
        fingerprint: JournalStorageFingerprint?,
        immediate: Boolean = false
    ) = journalCacheCoordinator.storeJournalFingerprint(fingerprint, immediate)

    private fun scheduleFingerprintRefresh(delayMillis: Long = 600L) {
        journalCacheCoordinator.scheduleFingerprintRefresh(delayMillis)
    }

    fun getCachedEntriesForDate(date: LocalDate): List<String>? =
        journalCacheCoordinator.getCachedEntriesForDate(date)

    fun getCachedDayLabel(date: LocalDate): String =
        journalCacheCoordinator.getCachedDayLabel(date)
    fun invalidateCache() =
        journalCacheCoordinator.invalidateCache(this)

    fun forceRefresh(onProgress: (Int, Int) -> Unit) {
        journalCacheCoordinator.forceRefresh(this, onProgress)
    }

    fun getAllJournalDatesLightweight(forceRefresh: Boolean = false): Set<LocalDate> {
        if (!forceRefresh) {
            lightweightDatesCache?.let { return it }
        }
        synchronized(this) {
            return journalCacheCoordinator.getAllJournalDatesLightweight(forceRefresh)
        }
    }

    fun startIncrementalWarmup(
        latestFirst: Boolean = true,
        onProgress: ((Int, Int) -> Unit)? = null
    ) = journalCacheCoordinator.startIncrementalWarmup(
        lock = this,
        ensureFrontmatterPopulated = { ensureFrontmatterPopulated() },
        latestFirst = latestFirst,
        onProgress = onProgress
    )

    private val defaultJournalsDir: File by lazy {
        File(context.filesDir, "journals").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    private val journalStorage by lazy {
        JournalStorage(
            context = context,
            rootUriStringProvider = { settingsRepository.storageUri.value },
            defaultJournalsDir = defaultJournalsDir,
            monthFormatter = MONTH_FORMATTER,
            parseEntriesFromContent = ::parseEntriesFromContent,
            logError = { message, error -> Log.e(TAG, message, error) }
        )
    }
    private val versionHistoryManager by lazy {
        VersionHistoryManager(
            context = context,
            settingsRepository = settingsRepository,
            scope = cacheScope
        )
    }

    fun hasEntriesForDate(date: LocalDate): Boolean {
        return getEntriesForDate(date).isNotEmpty()
    }

    fun getTotalEntryCount(): Int {
        return if (cachePopulated) {
            cache.values.sumOf { it.size }
        } else {
            getAllJournalDatesLightweight().sumOf { date ->
                journalStorage.readEntriesForDate(date).size
            }
        }
    }

    private fun parseEntries(lines: List<String>): List<String> =
            Companion.parseEntries(lines)

    /** Generate file content with optional frontmatter for starred/labelled dates. */
    private fun buildFileContent(date: LocalDate, entries: List<String>): String {
        return buildJournalDateContent(
            date = date,
            entries = entries,
            frontmatter = captureJournalDateFrontmatterState(
                date = date,
                starredDates = starredDates,
                dayLabels = dayLabels,
                revisitDates = revisitDates,
                revisitNotes = revisitNotes
            )
        )
    }

    fun getEntriesForDate(date: LocalDate): List<String> =
        journalCacheCoordinator.getEntriesForDate(date)

    /**
     * Load all journal entries from Room DB in a single batch query.
     * Returns a map of date → entries for every day that has a Room DB record.
     * Used as the fast path for full-corpus operations like the keyword index rebuild:
     * one SQLite table scan instead of opening one markdown file per day via SAF.
     */
    fun getEntriesSnapshotForRebuild(): Map<LocalDate, List<String>> =
        journalCacheCoordinator.getEntriesSnapshotForRebuild()

    /**
     * Read entries directly from disk, bypassing the in-memory cache entirely.
     * Used as the fallback for dates not present in Room DB.
     */
    fun readEntriesForDateDirect(date: LocalDate): List<String> =
        journalCacheCoordinator.readEntriesForDateDirect(date)

    /**
     * Despite the name, this reads through the in-memory cache. For a guaranteed disk read
     * (e.g. before a mutation), use [readEntriesForDateDirect].
     */
    @Deprecated("Obsolete: Use getEntriesForDate instead to utilize memory cache.", ReplaceWith("getEntriesForDate(date)"))
    fun getEntriesForDateFromDisk(date: LocalDate): List<String> {
        return getEntriesForDate(date)
    }

    /**
     * Checks if the disk file has been modified since it was cached.
     * If so, updates the cache with the new contents.
     */
    fun revalidateDateCache(date: LocalDate, forceDiskRead: Boolean = false): Boolean =
        journalCacheCoordinator.revalidateDateCache(date, forceDiskRead)

    private fun snapshotDateBeforeMutation(date: LocalDate, reason: String): Boolean {
        return versionHistoryManager.snapshotBeforeMutation(
            date = date,
            reason = reason,
            currentContent = journalStorage.readDateContent(date)
        )
    }

    fun getVersionHistorySnapshots(date: LocalDate): List<VersionHistorySnapshotInfo> =
        journalMutationService.getVersionHistorySnapshots(date)

    fun restoreVersionHistorySnapshot(date: LocalDate, snapshotId: String): Boolean =
        withDateMutationLock(date) { journalMutationService.restoreVersionHistorySnapshot(date, snapshotId) }

    private fun <T> withDateMutationLock(date: LocalDate, block: () -> T): T {
        val lock = mutationLocks.getOrPut(date) { Any() }
        return synchronized(lock) { block() }
    }

    private fun parseEntriesFromContent(content: String): List<String> =
        parseJournalEntriesFromContent(content)

    private fun applyRawDateContentToState(date: LocalDate, content: String) {
        val parsed = parseJournalDateContent(content)
        val entries = parsed.entries
        applyParsedJournalDateToMemoryState(
            date = date,
            parsed = parsed,
            cache = cache,
            entryCountCache = entryCountCache,
            wordCountCache = wordCountCache,
            starredDates = starredDates,
            dayLabels = dayLabels,
            revisitDates = revisitDates,
            revisitNotes = revisitNotes,
            countWords = ::countWords,
            updateCachedDatePresence = ::updateCachedDatePresence
        )

        finalizeAppliedJournalDateState(
            date = date,
            entries = entries,
            saveCacheToDisk = { saveDayToDb(date) },
            saveEntryCountCacheToDisk = {},
            saveWordCountCacheToDisk = {},
            saveLabelsCacheToDisk = { saveDayToDb(date) },
            syncTodoIndexForDate = ::syncTodoIndexForDate,
            scheduleFingerprintRefresh = ::scheduleFingerprintRefresh,
            scheduleEntryMutationRefresh = ::scheduleEntryMutationRefresh
        )
    }

    // Mutations must start from disk, not the in-memory cache, so a stale cache cannot overwrite
    // existing entries for the day.
    private fun getEntriesForDateForMutation(date: LocalDate): List<String> {
        return loadEntriesForMutation(
            date = date,
            cachedEntries = cache[date],
            diskEntries = readEntriesForDateDirect(date),
            logCacheMismatch = { message -> Log.w(TODO_PIPELINE_TAG, message) }
        )
    }

    fun addEntryForDate(date: LocalDate, entry: String): List<String> {
        return tryAddEntryForDate(date, entry).entries
    }

    fun tryAddEntryForDate(date: LocalDate, entry: String): EntryMutationResult =
        withDateMutationLock(date) { journalMutationService.tryAddEntryForDate(date, entry) }

    /**
     * Insert an entry at a specific position (0-based index) in the day's entries. Used by undo to
     * restore a deleted entry to its original position.
     */
    fun insertEntryAtPosition(date: LocalDate, entry: String, index: Int): List<String> {
        return tryInsertEntryAtPosition(date, entry, index).entries
    }

    fun tryInsertEntryAtPosition(date: LocalDate, entry: String, index: Int): EntryMutationResult =
        withDateMutationLock(date) { journalMutationService.tryInsertEntryAtPosition(date, entry, index) }

    fun deleteEntryForDate(date: LocalDate, indexToDelete: Int): List<String> {
        return tryDeleteEntryForDate(date, indexToDelete).entries
    }

    fun tryDeleteEntryForDate(date: LocalDate, indexToDelete: Int): EntryMutationResult =
        withDateMutationLock(date) { journalMutationService.tryDeleteEntryForDate(date, indexToDelete) }

    fun updateEntryForDate(date: LocalDate, indexToUpdate: Int, newEntry: String): List<String> {
        return tryUpdateEntryForDate(date, indexToUpdate, newEntry).entries
    }

    fun tryUpdateEntryForDate(date: LocalDate, indexToUpdate: Int, newEntry: String): EntryMutationResult =
        withDateMutationLock(date) { journalMutationService.tryUpdateEntryForDate(date, indexToUpdate, newEntry) }

    fun setEntriesForDate(
        date: LocalDate,
        newEntries: List<String>,
        preserveMissingDiskEntries: Boolean = true
    ): List<String> {
        return trySetEntriesForDate(date, newEntries, preserveMissingDiskEntries).entries
    }

    fun trySetEntriesForDate(
        date: LocalDate,
        newEntries: List<String>,
        preserveMissingDiskEntries: Boolean = true
    ): EntryMutationResult =
        withDateMutationLock(date) {
            journalMutationService.trySetEntriesForDate(date, newEntries, preserveMissingDiskEntries)
        }

    fun toggleTodoLine(
        date: LocalDate,
        entryIndex: Int,
        lineIndex: Int,
        expectedLineHash: String? = null,
        expectedDisplayText: String? = null
    ): Boolean =
        withDateMutationLock(date) {
            journalMutationService.toggleTodoLine(
                date = date,
                entryIndex = entryIndex,
                lineIndex = lineIndex,
                expectedLineHash = expectedLineHash,
                expectedDisplayText = expectedDisplayText
            )
        }

    private fun syncTodoIndexForDate(
        date: LocalDate,
        entries: List<String>,
        fingerprint: JournalStorageFingerprint? = null
    ) = journalIndexCoordinator.syncTodoIndexForDate(date, entries, fingerprint)

    private fun updateTodoIndexRows(date: LocalDate, entries: List<String>?) =
        journalIndexCoordinator.updateTodoIndexRows(date, entries)

    private fun scheduleTodoIndexRowsUpdate(date: LocalDate, entries: List<String>?) =
        journalIndexCoordinator.scheduleTodoIndexRowsUpdate(date, entries)

    private fun scheduleTodoIndexSyncForDate(date: LocalDate, entries: List<String>) =
        journalIndexCoordinator.scheduleTodoIndexSyncForDate(date, entries)

    private fun scheduleEntryMutationRefresh(date: LocalDate, entries: List<String>?) =
        journalIndexCoordinator.scheduleEntryMutationRefresh(date, entries)

    fun getAllJournalDatesWithData(): Set<LocalDate> =
        journalQueryService.getAllJournalDatesWithData()

    fun getCacheSnapshot(): Map<LocalDate, List<String>> =
        journalQueryService.getCacheSnapshot()

    /** Snapshot only the requested dates, without forcing a full-journal snapshot. */
    fun getEntriesSnapshotForDates(dates: Iterable<LocalDate>): Map<LocalDate, List<String>> =
        journalQueryService.getEntriesSnapshotForDates(dates)

    fun getDailyMetricsSnapshotForDates(dates: Iterable<LocalDate>): Map<LocalDate, DailyJournalMetrics> =
        journalQueryService.getDailyMetricsSnapshotForDates(dates)

    fun getWordCountSnapshotSince(startDate: LocalDate): Map<LocalDate, Int> =
        journalQueryService.getWordCountSnapshotSince(startDate)

    fun searchEntries(query: String): List<SearchResult> =
        journalSearchService.searchEntries(query)

    suspend fun migrateEntries(fromUriString: String?, toUriString: String?) =
        journalBackupGateway.migrateEntries(fromUriString, toUriString)

    fun createBackupZip(
        shortcodes: Map<String, String>,
        dateKeywords: List<DateKeywordEntry> = emptyList(),
        keywords: List<KeywordDefinition> = emptyList(),
        recurringTasks: List<RecurringTaskItem> = emptyList(),
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): BackupService.BackupZipResult? =
        journalBackupGateway.createBackupZip(
            shortcodes = shortcodes,
            dateKeywords = dateKeywords,
            keywords = keywords,
            recurringTasks = recurringTasks,
            onProgress = onProgress
        )

    fun readBackupZip(uri: Uri): BackupBundle? =
        journalBackupGateway.readBackupZip(uri)

    // ── Starred dates (persisted in markdown files) ─────────────────────

    /** Check if a date is starred by reading the markdown file. */
    fun isDateStarred(date: LocalDate): Boolean =
        journalMetadataRepository.isDateStarred(date)

    /** Get the label for a starred date. Returns empty string if not starred or no label. */
    fun getStarredLabel(date: LocalDate): String =
        journalMetadataRepository.getStarredLabel(date)

    /** Get the day label for any date. Returns empty string if no label. */
    fun getDayLabel(date: LocalDate): String =
        journalMetadataRepository.getDayLabel(date)

    fun getRevisitDate(date: LocalDate): LocalDate? =
        journalMetadataRepository.getRevisitDate(date)

    fun getRevisitNote(date: LocalDate): String =
        journalMetadataRepository.getRevisitNote(date)

    fun getEntryRevisit(date: LocalDate, entryIndex: Int): EntryRevisitMetadata =
        journalMetadataRepository.getEntryRevisit(date, entryIndex)

    fun setRevisit(date: LocalDate, revisitOn: LocalDate?, note: String = ""): Boolean =
        withDateMutationLock(date) { journalMetadataRepository.setRevisit(date, revisitOn, note) }

    /** Set or clear the day label for any date. Writes label: to frontmatter without changing starred status. */
    fun setDayLabel(date: LocalDate, label: String): Boolean =
        withDateMutationLock(date) { journalMetadataRepository.setDayLabel(date, label) }

    /** Toggle starred status for a date. */
    fun toggleStarred(date: LocalDate, label: String = ""): Boolean {
        val isCurrentlyStarred = isDateStarred(date)
        return setStarred(date, !isCurrentlyStarred, label)
    }

    /** Set starred status for a date with optional label. Updates the markdown file with frontmatter. */
    fun setStarred(date: LocalDate, starred: Boolean, label: String = ""): Boolean =
        withDateMutationLock(date) { journalMetadataRepository.setStarred(date, starred, label) }

    /** Get all starred dates. */
    fun getStarredDates(): List<LocalDate> =
        journalMetadataRepository.getStarredDates()

    /** Return a snapshot of all day labels (date → label).
     *  Uses dayLabels as the authoritative source so edits made via the day label dialog
     *  are always reflected, regardless of how the label was originally set. */
    fun refreshFrontmatterSnapshot() =
        journalMetadataRepository.refreshFrontmatterSnapshot(this)

    fun getAllStarredLabels(): Map<LocalDate, String> =
        journalMetadataRepository.getAllStarredLabels()

    data class RevisitOverview(
        val markers: List<RevisitMarker>,
        val targetDates: Set<LocalDate>,
        val dueItems: List<DueRevisitItem>
    )

    /**
     * Produces all revisit state in one corpus scan. Prefer this over calling
     * [getAllRevisitMarkers] and [getDueRevisitItems] separately, which scans twice.
     */
    fun getRevisitOverview(targetDate: LocalDate): RevisitOverview =
        journalMetadataRepository.getRevisitOverview(targetDate)

    fun getAllRevisitMarkers(): List<RevisitMarker> =
        journalMetadataRepository.getAllRevisitMarkers()

    fun getRevisitTargetDates(): Set<LocalDate> =
        journalMetadataRepository.getRevisitTargetDates()

    fun getDueRevisitItems(targetDate: LocalDate): List<DueRevisitItem> =
        journalMetadataRepository.getDueRevisitItems(targetDate)

    fun getBackupJournalSnapshot(
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Map<LocalDate, BackupJournalDay> =
        journalBackupGateway.getBackupJournalSnapshot(onProgress)

    private fun updateCachedDatePresence(date: LocalDate, hasEntries: Boolean) {
        synchronized(this) {
            val base = lightweightDatesCache
            // Membership unchanged — skip the O(n) set copy this would otherwise do per mutation.
            if (base != null && base.contains(date) == hasEntries) return
            val next = base?.toMutableSet() ?: mutableSetOf()
            if (hasEntries) next.add(date) else next.remove(date)
            lightweightDatesCache = next
        }
    }

    private fun updatePersistedEntryCount(delta: Int) {
        val current = settingsRepository.lastKnownEntryCount.value
        val nextCount = when {
            // O(1) delta path for every mutation; the full O(all days) sum only runs to
            // recover when no count has been persisted yet.
            current >= 0 -> (current + delta).coerceAtLeast(0)
            cachePopulated -> cache.values.sumOf { it.size }
            else -> return
        }
        settingsRepository.setLastKnownEntryCount(nextCount)
    }

    private fun countWords(entries: List<String>): Int =
        countWordsIgnoringChecklistMarkers(entries)
}
