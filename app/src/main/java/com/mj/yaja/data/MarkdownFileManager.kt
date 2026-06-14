package com.mj.yaja.data

import android.content.Context
import com.mj.yaja.data.database.JournalDatabase
import com.mj.yaja.data.database.JournalDayCacheEntity
import com.mj.yaja.data.database.JsonToRoomMigrator
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.mj.yaja.data.backup.BackupService
import com.mj.yaja.data.backup.BackupService.BackupBundle
import com.mj.yaja.data.backup.BackupService.BackupJournalDay
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.keywords.KeywordCsvCodec
import com.mj.yaja.data.storage.JournalCacheMetadataStore
import com.mj.yaja.data.storage.JournalCacheFileOps
import com.mj.yaja.data.storage.JournalCacheStore
import com.mj.yaja.data.storage.JournalLabelsStore
import com.mj.yaja.data.storage.JournalRevisitCache
import com.mj.yaja.data.storage.JournalStorageFingerprint
import com.mj.yaja.data.storage.JournalStorage
import com.mj.yaja.ui.widget.WidgetRefreshCoordinator
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

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
    private data class DateFileMetadata(val size: Long, val modifiedAt: Long)

    companion object {
        private const val TAG = "MarkdownFileManager"
        private const val TODO_PIPELINE_TAG = "YajaTodoPipeline"
        private const val SLOW_MUTATION_STAGE_MS = 500L
        private const val MAX_SEARCH_RESULTS = 200
        private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM")
        private val REVISIT_MARKER_ORDER =
            compareByDescending<RevisitMarker> { it.sourceDate }
                .thenByDescending { it.entryIndex ?: -1 }
        private val DUE_REVISIT_ORDER =
            compareByDescending<DueRevisitItem> { it.sourceDate }
                .thenByDescending { it.entryIndex ?: -1 }

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
    @Volatile private var warmupJob: Job? = null
    @Volatile private var cacheEpoch: Long = 0L
    private val database = JournalDatabase.getDatabase(context)
    private val dao = database.journalCacheDao()

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
    @Volatile private var fingerprintRefreshJob: Job? = null

    private fun saveDayToDb(date: LocalDate, immediate: Boolean = false) {
        val entries = cache[date]
        val entryCount = entryCountCache[date] ?: entries?.size ?: 0
        val wordCount = wordCountCache[date] ?: countWords(entries ?: emptyList())
        val metadata = dateMetadataCache[date]
        val isStarred = starredDates.containsKey(date)
        val label = dayLabels[date] ?: starredDates[date] ?: ""
        val revisitOn = revisitDates[date]?.toString()
        val revisitNote = revisitNotes[date] ?: ""

        val entity = JournalDayCacheEntity(
            date = date.toString(),
            journalId = "default",
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
            dao.insertOrUpdate(entity)
        } else {
            cacheScope.launch {
                dao.insertOrUpdate(entity)
            }
        }
    }

    private fun deleteDayFromDb(date: LocalDate, immediate: Boolean = false) {
        if (immediate) {
            dao.delete("default", date.toString())
        } else {
            cacheScope.launch {
                dao.delete("default", date.toString())
            }
        }
    }

    private fun saveLabelsCacheToDisk(immediate: Boolean = false) {}
    private fun loadLabelsCacheFromDisk(): Boolean = false
    private fun loadEntryCountCacheFromDisk(): Boolean = false
    private fun saveEntryCountCacheToDisk(immediate: Boolean = false) {}
    private fun loadWordCountCacheFromDisk(): Boolean = false
    private fun saveWordCountCacheToDisk(immediate: Boolean = false) {}
    private fun loadDateMetadataCacheFromDisk(): Boolean = false
    private fun saveDateMetadataCacheToDisk(immediate: Boolean = false) {}
    private fun persistDateIntCache(targetFile: File, source: Map<LocalDate, Int>, label: String) {}

    private fun ensureCachePopulated(onProgress: ((Int, Int) -> Unit)? = null) {
        if (!cachePopulated) {
            synchronized(this) {
                if (!cachePopulated) {
                    if (!migrationJob.isCompleted) {
                        kotlinx.coroutines.runBlocking {
                            migrationJob.join()
                        }
                    }
                    if (primeCachesFromDisk()) {
                        cacheScope.launch {
                            refreshHotCacheWindow(onProgress = onProgress)
                        }
                    } else {
                        populateCache(onProgress)
                    }
                }
            }
        }
    }

    private fun ensureFrontmatterPopulated() {
        if (!frontmatterPopulated) {
            synchronized(this) {
                if (!frontmatterPopulated) {
                    if (!migrationJob.isCompleted) {
                        kotlinx.coroutines.runBlocking {
                            migrationJob.join()
                        }
                    }
                    primeCachesFromDisk()
                    frontmatterPopulated = true
                }
            }
        }
    }

    private fun saveCacheToDisk(immediate: Boolean = false) {}
    private fun loadCacheFromDisk(): Boolean = false

    fun primeCachesFromDisk(): Boolean {
        synchronized(this) {
            var loadedAnything = false
            try {
                val allDays = dao.getAllDaysSync("default")
                if (allDays.isNotEmpty()) {
                    cache.clear()
                    entryCountCache.clear()
                    wordCountCache.clear()
                    dateMetadataCache.clear()
                    starredDates.clear()
                    dayLabels.clear()
                    revisitDates.clear()
                    revisitNotes.clear()

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
                    cachePopulated = true
                    frontmatterPopulated = true
                    lightweightDatesCache = cache.keys.toSet()
                    loadedAnything = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prime caches from Room", e)
            }
            return loadedAnything
        }
    }

    fun getCachedJournalDates(): Set<LocalDate> =
        lightweightDatesCache ?: cache.keys.toSet()

    fun getJournalCacheAgeMillis(): Long? {
        val dbFile = context.getDatabasePath("journal_database")
        if (!dbFile.exists()) return null
        return (System.currentTimeMillis() - dbFile.lastModified()).coerceAtLeast(0L)
    }

    fun getDatabaseSize(): Long {
        val dbFile = context.getDatabasePath("journal_database")
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

    fun getCachedDaysCount(): Int {
        try {
            return dao.getCount("default")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cached days count", e)
            return 0
        }
    }

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

    private fun scheduleFingerprintRefresh(delayMillis: Long = 600L) {
        fingerprintRefreshJob?.cancel()
        fingerprintRefreshJob = cacheScope.launch {
            delay(delayMillis)
            val fingerprint = persistCurrentJournalFingerprint(
                immediate = false,
                knownDates = getCachedJournalDates(),
                anchorDate = LocalDate.now()
            )
            todoIndexRepository.markFingerprint(fingerprint)
        }
    }

    fun getCachedEntriesForDate(date: LocalDate): List<String>? =
        cache[date]?.toList()

    fun getCachedDayLabel(date: LocalDate): String =
        dayLabels[date].orEmpty()
    private fun populateCache(onProgress: ((Int, Int) -> Unit)? = null) {
        val expectedEpoch = cacheEpoch
        val orderedDates = getAllJournalDatesLightweight().sortedDescending()
        synchronized(this) {
            cache.clear()
            entryCountCache.clear()
            wordCountCache.clear()
            dateMetadataCache.clear()
            starredDates.clear()
            dayLabels.clear()
            revisitDates.clear()
            revisitNotes.clear()
        }
        val total = orderedDates.size.coerceAtLeast(1)
        val entities = mutableListOf<JournalDayCacheEntity>()
        orderedDates.forEachIndexed { index, date ->
            cacheScope.coroutineContext.ensureActive()
            if (expectedEpoch != cacheEpoch) return
            // Single read per file: entries and frontmatter come from the same content,
            // instead of opening the file once for entries and again for frontmatter.
            val parsed = journalStorage.readDateContent(date)?.let { parseJournalDateContent(it) }
            val entries = parsed?.entries.orEmpty()
            synchronized(this) {
                if (expectedEpoch != cacheEpoch) return
                if (parsed != null && entries.isNotEmpty()) {
                    cache[date] = entries
                    entryCountCache[date] = entries.size
                    wordCountCache[date] = countWords(entries)
                    val metadata = journalStorage.getDateFileMetadata(date, settingsRepository.storageUri.value)
                    metadata?.let { dateMetadataCache[date] = DateFileMetadata(it.first, it.second) }

                    val snapshot = parsed.frontmatter
                    if (snapshot.isStarred) starredDates[date] = snapshot.label
                    if (snapshot.label.isNotEmpty()) dayLabels[date] = snapshot.label
                    snapshot.revisitOn?.let { revisitDates[date] = it }
                    if (snapshot.revisitNote.isNotEmpty()) revisitNotes[date] = snapshot.revisitNote

                    entities.add(
                        JournalDayCacheEntity(
                            date = date.toString(),
                            journalId = "default",
                            entries = entries,
                            isStarred = snapshot.isStarred,
                            label = snapshot.label,
                            revisitOn = snapshot.revisitOn?.toString(),
                            revisitNote = snapshot.revisitNote,
                            wordCount = wordCountCache[date] ?: 0,
                            entryCount = entries.size,
                            fileModifiedAt = metadata?.second ?: 0L,
                            fileSize = metadata?.first ?: 0L
                        )
                    )
                }
            }
            onProgress?.invoke(index + 1, total)
        }

        if (expectedEpoch == cacheEpoch && entities.isNotEmpty()) {
            dao.insertAll(entities)
        }
        cachePopulated = true
        frontmatterPopulated = true
        lightweightDatesCache = orderedDates.toSet()
    }

    private fun refreshHotCacheWindow(
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
            cacheScope.coroutineContext.ensureActive()
            if (expectedEpoch != cacheEpoch) return
            val rawMetadata = journalStorage.getDateFileMetadata(date, settingsRepository.storageUri.value)
            val metadata = rawMetadata?.let { DateFileMetadata(it.first, it.second) }
            if (metadata == null) {
                cache.remove(date)
                entryCountCache.remove(date)
                wordCountCache.remove(date)
                dateMetadataCache.remove(date)
                starredDates.remove(date)
                dayLabels.remove(date)
                revisitDates.remove(date)
                revisitNotes.remove(date)
                toDelete.add(date.toString())
                if (toDelete.size >= 50) {
                    dao.deleteDays("default", toDelete)
                    toDelete.clear()
                }
                onProgress?.invoke(index + 1, total)
                return@forEachIndexed
            }
            val previousMetadata = dateMetadataCache[date]
            val needsRead = previousMetadata != metadata || !cache.containsKey(date)
            if (needsRead) {
                val parsed = journalStorage.readDateContent(date)?.let { parseJournalDateContent(it) }
                val entries = parsed?.entries.orEmpty()
                if (expectedEpoch != cacheEpoch) return
                if (parsed != null && entries.isNotEmpty()) {
                    cache[date] = entries
                    entryCountCache[date] = entries.size
                    wordCountCache[date] = countWords(entries)
                    dateMetadataCache[date] = metadata

                    val snapshot = parsed.frontmatter
                    if (snapshot.isStarred) starredDates[date] = snapshot.label else starredDates.remove(date)
                    if (snapshot.label.isNotEmpty()) dayLabels[date] = snapshot.label else dayLabels.remove(date)
                    snapshot.revisitOn?.let { revisitDates[date] = it } ?: revisitDates.remove(date)
                    if (snapshot.revisitNote.isNotEmpty()) revisitNotes[date] = snapshot.revisitNote else revisitNotes.remove(date)

                    toInsert.add(
                        JournalDayCacheEntity(
                            date = date.toString(),
                            journalId = "default",
                            entries = entries,
                            isStarred = snapshot.isStarred,
                            label = snapshot.label,
                            revisitOn = snapshot.revisitOn?.toString(),
                            revisitNote = snapshot.revisitNote,
                            wordCount = wordCountCache[date] ?: 0,
                            entryCount = entries.size,
                            fileModifiedAt = metadata.modifiedAt,
                            fileSize = metadata.size
                        )
                    )
                    if (toInsert.size >= 50) {
                        dao.insertAll(toInsert)
                        toInsert.clear()
                    }
                } else {
                    cache.remove(date)
                    entryCountCache.remove(date)
                    wordCountCache.remove(date)
                    dateMetadataCache.remove(date)
                    starredDates.remove(date)
                    dayLabels.remove(date)
                    revisitDates.remove(date)
                    revisitNotes.remove(date)
                    toDelete.add(date.toString())
                    if (toDelete.size >= 50) {
                        dao.deleteDays("default", toDelete)
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
                dao.deleteDays("default", toDelete)
            }
        }
        lightweightDatesCache = allDates
    }

    fun invalidateCache() {
        synchronized(this) {
            warmupJob?.cancel()
            warmupJob = null
            fingerprintRefreshJob?.cancel()
            fingerprintRefreshJob = null
            cacheEpoch += 1
            cache.clear()
            entryCountCache.clear()
            wordCountCache.clear()
            starredDates.clear()
            dayLabels.clear()
            revisitDates.clear()
            revisitNotes.clear()
            cachePopulated = false
            frontmatterPopulated = false
            lightweightDatesCache = null
            dateMetadataCache.clear()
            journalStorage.invalidateDirectoryCache()
            
            // Delete all entries from Room database
            cacheScope.launch {
                dao.deleteAll("default")
                try {
                    database.openHelper.writableDatabase.execSQL("VACUUM")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to vacuum database after clearing cache", e)
                }
            }
            metadataStore.clear()
        }
    }

    fun forceRefresh(onProgress: (Int, Int) -> Unit) {
        invalidateCache()
        populateCache(onProgress)
        cachePopulated = true
        persistCurrentJournalFingerprint(
            immediate = true,
            knownDates = lightweightDatesCache ?: cache.keys.toSet(),
            anchorDate = LocalDate.now()
        )
    }

    fun getAllJournalDatesLightweight(forceRefresh: Boolean = false): Set<LocalDate> {
        if (!forceRefresh) {
            lightweightDatesCache?.let { return it }
        }
        synchronized(this) {
            if (!forceRefresh) {
                lightweightDatesCache?.let { return it }
            }
            return journalStorage
                .listJournalDates(settingsRepository.storageUri.value)
                .also { lightweightDatesCache = it }
        }
    }

    fun startIncrementalWarmup(
        latestFirst: Boolean = true,
        onProgress: ((Int, Int) -> Unit)? = null
    ) {
        if (cachePopulated) return
        synchronized(this) {
            if (cachePopulated || warmupJob?.isActive == true) return
            ensureFrontmatterPopulated()
            warmupJob = cacheScope.launch {
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
                        if (!cache.containsKey(date)) {
                            val parsed = journalStorage.readDateContent(date)?.let { parseJournalDateContent(it) }
                            val entries = parsed?.entries.orEmpty()
                            if (expectedEpoch != cacheEpoch) return@launch
                            if (parsed != null && entries.isNotEmpty()) {
                                cache[date] = entries
                                entryCountCache[date] = entries.size
                                wordCountCache[date] = countWords(entries)

                                val rawMetadata = journalStorage.getDateFileMetadata(date, settingsRepository.storageUri.value)
                                val metadata = rawMetadata?.let { DateFileMetadata(it.first, it.second) }
                                metadata?.let { dateMetadataCache[date] = it }

                                val snapshot = parsed.frontmatter
                                if (snapshot.isStarred) starredDates[date] = snapshot.label
                                if (snapshot.label.isNotEmpty()) dayLabels[date] = snapshot.label
                                snapshot.revisitOn?.let { revisitDates[date] = it }
                                if (snapshot.revisitNote.isNotEmpty()) revisitNotes[date] = snapshot.revisitNote

                                toInsert.add(
                                    JournalDayCacheEntity(
                                        date = date.toString(),
                                        journalId = "default",
                                        entries = entries,
                                        isStarred = snapshot.isStarred,
                                        label = snapshot.label,
                                        revisitOn = snapshot.revisitOn?.toString(),
                                        revisitNote = snapshot.revisitNote,
                                        wordCount = wordCountCache[date] ?: 0,
                                        entryCount = entries.size,
                                        fileModifiedAt = metadata?.modifiedAt ?: 0L,
                                        fileSize = metadata?.size ?: 0L
                                    )
                                )
                                if (toInsert.size >= 50) {
                                    dao.insertAll(toInsert)
                                    toInsert.clear()
                                }
                            } else {
                                entryCountCache.remove(date)
                                wordCountCache.remove(date)
                                toDelete.add(date.toString())
                                if (toDelete.size >= 50) {
                                    dao.deleteDays("default", toDelete)
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
                            dao.deleteDays("default", toDelete)
                        }
                    }
                    cachePopulated = true
                    lightweightDatesCache = orderedDates.toSet()
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

    fun getEntriesForDate(date: LocalDate): List<String> {
        cache[date]?.let { return it }
        val knownDates = lightweightDatesCache
        if (knownDates != null && !knownDates.contains(date)) {
            return emptyList()
        }
        // If the full cache is populated but this date has no in-memory entry, it means
        // the Room DB record for this date is missing or stale while the file still exists
        // on disk (lightweight cache confirmed it). Fall through to a disk read rather than
        // returning empty so callers like the keyword rebuild get real content.
        val entries = journalStorage.readEntriesForDate(date)
        if (entries.isNotEmpty()) {
            cache[date] = entries
            entryCountCache[date] = entries.size
            wordCountCache[date] = countWords(entries)
            updateCachedDatePresence(date, hasEntries = true)
            saveEntryCountCacheToDisk()
            saveWordCountCacheToDisk()
        }
        return entries
    }

    /**
     * Load all journal entries from Room DB in a single batch query.
     * Returns a map of date → entries for every day that has a Room DB record.
     * Used as the fast path for full-corpus operations like the keyword index rebuild:
     * one SQLite table scan instead of opening one markdown file per day via SAF.
     */
    fun getEntriesSnapshotForRebuild(): Map<LocalDate, List<String>> {
        return try {
            dao.getAllDaysSync("default")
                .mapNotNull { entity ->
                    runCatching { LocalDate.parse(entity.date) }.getOrNull()
                        ?.let { date -> date to entity.entries }
                }
                .toMap()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load entries snapshot from Room for rebuild", e)
            emptyMap()
        }
    }

    /**
     * Read entries directly from disk, bypassing the in-memory cache entirely.
     * Used as the fallback for dates not present in Room DB.
     */
    fun readEntriesForDateDirect(date: LocalDate): List<String> =
        journalStorage.readEntriesForDate(date)

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
    fun revalidateDateCache(date: LocalDate): Boolean {
        val rawMetadata = journalStorage.getDateFileMetadata(date, settingsRepository.storageUri.value)
        val metadata = rawMetadata?.let { DateFileMetadata(it.first, it.second) }
        val previousMetadata = dateMetadataCache[date]
        
        if (metadata == null) {
            if (cache.remove(date) != null) {
                 entryCountCache.remove(date)
                 wordCountCache.remove(date)
                 dateMetadataCache.remove(date)
                 return true
            }
            return false
        }
        
        if (previousMetadata != metadata || !cache.containsKey(date)) {
            val entries = journalStorage.readEntriesForDate(date)
            if (entries.isNotEmpty()) {
                cache[date] = entries
                entryCountCache[date] = entries.size
                wordCountCache[date] = countWords(entries)
                dateMetadataCache[date] = metadata
            } else {
                cache.remove(date)
                entryCountCache.remove(date)
                wordCountCache.remove(date)
                dateMetadataCache.remove(date)
            }
            return true
        }
        return false
    }

    private fun snapshotDateBeforeMutation(date: LocalDate, reason: String): Boolean {
        return versionHistoryManager.snapshotBeforeMutation(
            date = date,
            reason = reason,
            currentContent = journalStorage.readDateContent(date)
        )
    }

    fun getVersionHistorySnapshots(date: LocalDate): List<VersionHistorySnapshotInfo> {
        return versionHistoryManager.listSnapshots(date).mapNotNull { snapshot ->
            versionHistoryManager.restoreSnapshot(snapshot)?.let { content ->
                VersionHistorySnapshotInfo(
                    id = snapshot.file.name,
                    createdAt = snapshot.createdAt,
                    content = content
                )
            }
        }
    }

    fun restoreVersionHistorySnapshot(date: LocalDate, snapshotId: String): Boolean = withDateMutationLock(date) {
        val snapshot = versionHistoryManager.listSnapshots(date).firstOrNull { it.file.name == snapshotId }
            ?: return@withDateMutationLock false
        val restoredContent = versionHistoryManager.restoreSnapshot(snapshot)
            ?: return@withDateMutationLock false
        val currentContent = journalStorage.readDateContent(date)
        if (currentContent == restoredContent) {
            return@withDateMutationLock true
        }
        if (!snapshotDateBeforeMutation(date, "restore_version")) {
            return@withDateMutationLock false
        }
        if (!journalStorage.writeDateContent(date, restoredContent, createIfNotExists = true)) {
            return@withDateMutationLock false
        }
        applyRawDateContentToState(date, restoredContent)
        true
    }

    private fun <T> withDateMutationLock(date: LocalDate, block: () -> T): T {
        val lock = mutationLocks.getOrPut(date) { Any() }
        return synchronized(lock) { block() }
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

    fun tryAddEntryForDate(date: LocalDate, entry: String): EntryMutationResult = withDateMutationLock(date) {
        if (entry.isBlank()) return@withDateMutationLock EntryMutationResult(getEntriesForDateForMutation(date), false)
        val existingEntries = traceMutationStage("addEntry", date, "read") {
            getEntriesForDateForMutation(date)
        }
        val newEntries = existingEntries + entry
        val content = buildFileContent(date, newEntries)
        val snapshotCreated = traceMutationStage("addEntry", date, "snapshot") {
            snapshotDateBeforeMutation(date, "add_entry")
        }
        if (!snapshotCreated) {
            return@withDateMutationLock EntryMutationResult(existingEntries, false)
        }
        val written = traceMutationStage("addEntry", date, "write") {
            journalStorage.writeDateContent(date, content, createIfNotExists = true)
        }
        if (!written) {
            return@withDateMutationLock EntryMutationResult(existingEntries, false)
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
                updateCachedDatePresence = { targetDate, hasEntries ->
                    updateCachedDatePresence(targetDate, hasEntries)
                },
                saveCacheToDisk = { saveDayToDb(date) },
                saveEntryCountCacheToDisk = {},
                saveWordCountCacheToDisk = {},
                scheduleFingerprintRefresh = ::scheduleFingerprintRefresh,
                updatePersistedEntryCount = { delta -> updatePersistedEntryCount(delta) },
                entryCountDelta = 1,
                updateTodoIndexRows = ::scheduleTodoIndexRowsUpdate,
                scheduleEntryMutationRefresh = ::scheduleEntryMutationRefresh
            )
        }
        EntryMutationResult(newEntries, true)
    }

    /**
     * Insert an entry at a specific position (0-based index) in the day's entries. Used by undo to
     * restore a deleted entry to its original position.
     */
    fun insertEntryAtPosition(date: LocalDate, entry: String, index: Int): List<String> {
        return tryInsertEntryAtPosition(date, entry, index).entries
    }

    fun tryInsertEntryAtPosition(date: LocalDate, entry: String, index: Int): EntryMutationResult = withDateMutationLock(date) {
        if (entry.isBlank()) return@withDateMutationLock EntryMutationResult(getEntriesForDateForMutation(date), false)
        val existingEntries = getEntriesForDateForMutation(date)
        val mutableEntries = existingEntries.toMutableList()
        val clampedIndex = index.coerceIn(0, mutableEntries.size)
        mutableEntries.add(clampedIndex, entry)

        // Write to disk with frontmatter preservation
        val content = buildFileContent(date, mutableEntries)
        if (!snapshotDateBeforeMutation(date, "insert_entry")) {
            return@withDateMutationLock EntryMutationResult(existingEntries, false)
        }
        if (!journalStorage.writeDateContent(date, content, createIfNotExists = true)) {
            return@withDateMutationLock EntryMutationResult(existingEntries, false)
        }
        Log.d(TODO_PIPELINE_TAG, "File written: insertEntry date=$date entries=${mutableEntries.size}")
        applyNonEmptyMutationSuccessState(
            date = date,
            entries = mutableEntries,
            cache = cache,
            entryCountCache = entryCountCache,
            wordCountCache = wordCountCache,
            countWords = ::countWords,
            updateCachedDatePresence = { targetDate, hasEntries ->
                updateCachedDatePresence(targetDate, hasEntries)
            },
            saveCacheToDisk = { saveDayToDb(date) },
            saveEntryCountCacheToDisk = {},
            saveWordCountCacheToDisk = {},
            scheduleFingerprintRefresh = ::scheduleFingerprintRefresh,
            updatePersistedEntryCount = { delta -> updatePersistedEntryCount(delta) },
            entryCountDelta = 1,
            updateTodoIndexRows = ::scheduleTodoIndexRowsUpdate,
            scheduleEntryMutationRefresh = ::scheduleEntryMutationRefresh
        )
        EntryMutationResult(mutableEntries, true)
    }

    fun deleteEntryForDate(date: LocalDate, indexToDelete: Int): List<String> {
        return tryDeleteEntryForDate(date, indexToDelete).entries
    }

    fun tryDeleteEntryForDate(date: LocalDate, indexToDelete: Int): EntryMutationResult = withDateMutationLock(date) {
        val currentEntries = traceMutationStage("deleteEntry", date, "read") {
            getEntriesForDateForMutation(date)
        }
        if (currentEntries.isEmpty()) return@withDateMutationLock EntryMutationResult(currentEntries, false)
        if (indexToDelete !in currentEntries.indices) return@withDateMutationLock EntryMutationResult(currentEntries, false)

        val newEntries = currentEntries.toMutableList()
        newEntries.removeAt(indexToDelete)

        // Write to disk with frontmatter preservation
        val snapshotCreated = traceMutationStage("deleteEntry", date, "snapshot") {
            snapshotDateBeforeMutation(date, "delete_entry")
        }
        if (!snapshotCreated) {
            return@withDateMutationLock EntryMutationResult(currentEntries, false)
        }
        if (newEntries.isEmpty()) {
            val deleted = traceMutationStage("deleteEntry", date, "deleteFile") {
                journalStorage.deleteDateFile(date)
            }
            if (!deleted) return@withDateMutationLock EntryMutationResult(currentEntries, false)
            Log.d(TODO_PIPELINE_TAG, "File deleted: deleteEntry date=$date")
            starredDates.remove(date)  // Remove starred status when deleting all entries
            revisitDates.remove(date)
            revisitNotes.remove(date)
        } else {
            val content = buildFileContent(date, newEntries)
            val written = traceMutationStage("deleteEntry", date, "write") {
                journalStorage.writeDateContent(date, content, createIfNotExists = false)
            }
            if (!written) {
                return@withDateMutationLock EntryMutationResult(currentEntries, false)
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
                updateCachedDatePresence = { targetDate, hasEntries ->
                    updateCachedDatePresence(targetDate, hasEntries)
                },
                saveCacheToDisk = { if (newEntries.isEmpty()) deleteDayFromDb(date) else saveDayToDb(date) },
                saveEntryCountCacheToDisk = {},
                saveWordCountCacheToDisk = {},
                scheduleFingerprintRefresh = ::scheduleFingerprintRefresh,
                updatePersistedEntryCount = { delta -> updatePersistedEntryCount(delta) },
                dateMetadataRemoved = {
                    dateMetadataCache.remove(date)
                    deleteDayFromDb(date)
                },
                updateTodoIndexRows = ::scheduleTodoIndexRowsUpdate,
                scheduleEntryMutationRefresh = ::scheduleEntryMutationRefresh
            )
        }
        EntryMutationResult(newEntries, true)
    }

    fun updateEntryForDate(date: LocalDate, indexToUpdate: Int, newEntry: String): List<String> {
        return tryUpdateEntryForDate(date, indexToUpdate, newEntry).entries
    }

    fun tryUpdateEntryForDate(date: LocalDate, indexToUpdate: Int, newEntry: String): EntryMutationResult = withDateMutationLock(date) {
        val currentEntries = traceMutationStage("updateEntry", date, "read") {
            getEntriesForDateForMutation(date)
        }
        if (currentEntries.isEmpty()) return@withDateMutationLock EntryMutationResult(currentEntries, false)
        if (indexToUpdate !in currentEntries.indices) return@withDateMutationLock EntryMutationResult(currentEntries, false)

        val updatedEntries = currentEntries.toMutableList()
        updatedEntries[indexToUpdate] = newEntry

        // Write to disk with frontmatter preservation
        val content = buildFileContent(date, updatedEntries)
        val snapshotCreated = traceMutationStage("updateEntry", date, "snapshot") {
            snapshotDateBeforeMutation(date, "update_entry")
        }
        if (!snapshotCreated) {
            return@withDateMutationLock EntryMutationResult(currentEntries, false)
        }
        val written = traceMutationStage("updateEntry", date, "write") {
            journalStorage.writeDateContent(date, content, createIfNotExists = false)
        }
        if (!written) {
            return@withDateMutationLock EntryMutationResult(currentEntries, false)
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
                updateCachedDatePresence = { targetDate, hasEntries ->
                    updateCachedDatePresence(targetDate, hasEntries)
                },
                saveCacheToDisk = { saveDayToDb(date) },
                saveEntryCountCacheToDisk = {},
                saveWordCountCacheToDisk = {},
                scheduleFingerprintRefresh = ::scheduleFingerprintRefresh,
                updatePersistedEntryCount = { delta -> updatePersistedEntryCount(delta) },
                entryCountDelta = 0,
                updateTodoIndexRows = ::scheduleTodoIndexRowsUpdate,
                scheduleEntryMutationRefresh = ::scheduleEntryMutationRefresh
            )
        }
        EntryMutationResult(updatedEntries, true)
    }

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
    ): EntryMutationResult = withDateMutationLock(date) {
        if (newEntries.isEmpty()) {
            val diskEntries = getEntriesForDateForMutation(date)
            return@withDateMutationLock EntryMutationResult(diskEntries, false)
        }
        val diskEntries = getEntriesForDateForMutation(date)
        val entriesToWrite =
            if (!preserveMissingDiskEntries || diskEntries.isEmpty()) {
                newEntries
            } else {
                mergeEntriesPreservingMissingCounts(newEntries, diskEntries)
            }

        // Write to disk with frontmatter preservation
        val content = buildFileContent(date, entriesToWrite)
        if (!snapshotDateBeforeMutation(date, "set_entries")) {
            return@withDateMutationLock EntryMutationResult(diskEntries, false)
        }
        if (!journalStorage.writeDateContent(date, content, createIfNotExists = true)) {
            return@withDateMutationLock EntryMutationResult(diskEntries, false)
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
            updateCachedDatePresence = { targetDate, hasEntries ->
                updateCachedDatePresence(targetDate, hasEntries)
            },
            saveCacheToDisk = { saveDayToDb(date) },
            saveEntryCountCacheToDisk = {},
            saveWordCountCacheToDisk = {},
            scheduleFingerprintRefresh = ::scheduleFingerprintRefresh,
            updatePersistedEntryCount = { delta -> updatePersistedEntryCount(delta) },
            entryCountDelta = entriesToWrite.size - previousCount,
            updateTodoIndexRows = ::scheduleTodoIndexRowsUpdate,
            scheduleEntryMutationRefresh = ::scheduleEntryMutationRefresh
        )
        EntryMutationResult(entriesToWrite, true)
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

    fun toggleTodoLine(
        date: LocalDate,
        entryIndex: Int,
        lineIndex: Int,
        expectedLineHash: String? = null,
        expectedDisplayText: String? = null
    ): Boolean = withDateMutationLock(date) {
        val entries = readEntriesForDateDirect(date).toMutableList()
        val target = findTodoTarget(entries, entryIndex, lineIndex, expectedLineHash, expectedDisplayText)
        if (target == null) {
            updateTodoIndexRows(date, entries)
            scheduleEntryMutationRefresh(date, entries)
            Log.w(
                TODO_PIPELINE_TAG,
                "Todo toggle target missing: date=$date entry=$entryIndex line=$lineIndex hash=$expectedLineHash"
            )
            return@withDateMutationLock false
        }
        val (targetEntryIndex, targetLineIndex, line) = target
        val lines = entries[targetEntryIndex].lines().toMutableList()
        val toggledLine = TodoParser.toggleLine(line) ?: return@withDateMutationLock false
        lines[targetLineIndex] = toggledLine
        entries[targetEntryIndex] = lines.joinToString("\n")
        trySetEntriesForDate(date, entries, preserveMissingDiskEntries = false).success
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

    private fun syncTodoIndexForDate(
        date: LocalDate,
        entries: List<String>,
        fingerprint: JournalStorageFingerprint? = null
    ) {
        journalStorage.getDateFileMetadata(date, settingsRepository.storageUri.value)?.let {
            dateMetadataCache[date] = DateFileMetadata(it.first, it.second)
            saveDateMetadataCacheToDisk()
        }
        todoIndexRepository.replaceDate(date, entries, getDayLabel(date), fingerprint)
        eventIndexRepository.replaceDate(date, entries, fingerprint)
    }

    private fun updateTodoIndexRows(date: LocalDate, entries: List<String>?) {
        if (entries.isNullOrEmpty()) {
            todoIndexRepository.removeDate(date)
            eventIndexRepository.removeDate(date)
        } else {
            todoIndexRepository.replaceDate(date, entries, getDayLabel(date))
            eventIndexRepository.replaceDate(date, entries)
        }
        Log.d(
            TODO_PIPELINE_TAG,
            "Todo/event indexes synced: date=$date todoRows=${todoIndexRepository.getEntries().size} eventRows=${eventIndexRepository.getEntries().size}"
        )
        WidgetRefreshCoordinator.requestTodoListUpdate(context)
    }

    private fun scheduleTodoIndexRowsUpdate(date: LocalDate, entries: List<String>?) {
        cacheScope.launch {
            updateTodoIndexRows(date, entries)
        }
    }

    private fun scheduleTodoIndexSyncForDate(date: LocalDate, entries: List<String>) {
        cacheScope.launch {
            syncTodoIndexForDate(date, entries)
        }
    }

    private fun scheduleEntryMutationRefresh(date: LocalDate, entries: List<String>?) {
        cacheScope.launch {
            entries?.let { syncDateFileMetadata(date) }
            WidgetRefreshCoordinator.requestHeatmapUpdate(context, invalidateCache = true)
        }
    }

    private fun syncDateFileMetadata(date: LocalDate) {
        journalStorage.getDateFileMetadata(date, settingsRepository.storageUri.value)?.let {
            dateMetadataCache[date] = DateFileMetadata(it.first, it.second)
            saveDateMetadataCacheToDisk()
        }
    }

    fun getAllJournalDatesWithData(): Set<LocalDate> {
        return if (cachePopulated) cache.keys.toSet() else getAllJournalDatesLightweight()
    }

    fun getCacheSnapshot(): Map<LocalDate, List<String>> {
        if (cachePopulated) {
            return synchronized(this) { cache.mapValues { (_, entries) -> entries.toList() } }
        }
        try {
            val allDays = dao.getAllDaysSync("default")
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
        getAllJournalDatesLightweight().sortedDescending().forEach { date ->
            val entries = getEntriesForDate(date)
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
            val entries = getEntriesForDate(date)
            if (entries.isNotEmpty()) {
                snapshot[date] = entries
            }
        }
        return snapshot
    }

    fun getDailyMetricsSnapshotForDates(dates: Iterable<LocalDate>): Map<LocalDate, DailyJournalMetrics> {
        if (entryCountCache.isEmpty()) {
            loadEntryCountCacheFromDisk()
        }
        if (wordCountCache.isEmpty()) {
            loadWordCountCacheFromDisk()
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
                val entries = getEntriesForDate(date)
                if (entries.isNotEmpty()) {
                    val metrics = DailyJournalMetrics(
                        entryCount = entries.size,
                        wordCount = countWords(entries)
                    )
                    entryCountCache[date] = metrics.entryCount
                    wordCountCache[date] = metrics.wordCount
                    didMutateCache = true
                    snapshot[date] = metrics
                }
            }
        }
        if (didMutateCache) {
            saveEntryCountCacheToDisk()
            saveWordCountCacheToDisk()
        }
        return snapshot
    }

    fun getWordCountSnapshotSince(startDate: LocalDate): Map<LocalDate, Int> {
        if (wordCountCache.isEmpty()) {
            loadWordCountCacheFromDisk()
        }
        val snapshot = linkedMapOf<LocalDate, Int>()
        var didMutateCache = false
        getAllJournalDatesLightweight(forceRefresh = true)
            .asSequence()
            .filter { !it.isBefore(startDate) }
            .sorted()
            .forEach { date ->
                val cachedCount = wordCountCache[date]
                if (cachedCount != null) {
                    snapshot[date] = cachedCount
                } else {
                    val entries = getEntriesForDateFromDisk(date)
                    if (entries.isNotEmpty()) {
                        val count = countWords(entries)
                        entryCountCache[date] = entries.size
                        wordCountCache[date] = count
                        didMutateCache = true
                        snapshot[date] = count
                    }
                }
            }
        if (didMutateCache) {
            saveEntryCountCacheToDisk()
            saveWordCountCacheToDisk()
        }
        return snapshot
    }

    fun searchEntries(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        ensureFrontmatterPopulated()
        val results = mutableListOf<SearchResult>()
        val timestampRegex = Regex("<!--.*?-->")
        // Split into words for AND-logic: all words must appear in the entry or label
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }

        for (date in getAllJournalDatesLightweight().sortedDescending()) {
            // Check day label — surfaces dates whose label matches even if no entry does
            val label = dayLabels[date] ?: ""
            if (label.isNotEmpty()) {
                val labelLower = label.lowercase()
                if (words.all { word -> labelLower.contains(word) }) {
                    results.add(SearchResult(date, "Label: $label"))
                }
            }

            // Check each entry's content
            getEntriesForDate(date).forEach { entry ->
                val cleanEntry = entry.replace(timestampRegex, "").trim()
                val lower = cleanEntry.lowercase()
                if (words.all { word -> lower.contains(word) }) {
                    val snippet =
                            cleanEntry.replace("\n", " ").take(100).let {
                                if (cleanEntry.length > 100) "$it..." else it
                            }
                    results.add(SearchResult(date, snippet))
                }
            }

            // Newest dates are scanned first, so capping keeps the most relevant matches.
            if (results.size >= MAX_SEARCH_RESULTS) break
        }
        return results
    }

    suspend fun migrateEntries(fromUriString: String?, toUriString: String?) = withContext(Dispatchers.IO) {
        // If they are the same, nothing to do
        if (fromUriString == toUriString) return@withContext

        val datesWithData = journalStorage.listJournalDates(fromUriString)

        // 2. Read from SOURCE and write to DESTINATION
        for (date in datesWithData) {
            val entries = journalStorage.getEntriesForDateFromSpecificStorage(date, fromUriString)
            if (entries.isNotEmpty()) {
                if (!journalStorage.addEntriesToSpecificStorage(date, entries, toUriString)) {
                    throw IOException("Failed to migrate entries for $date")
                }
            }
        }

        // 3. Invalidate cache since storage has changed
        invalidateCache()
    }

    fun createBackupZip(
        shortcodes: Map<String, String>,
        dateKeywords: List<DateKeywordEntry> = emptyList(),
        keywords: List<KeywordDefinition> = emptyList(),
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): BackupService.BackupZipResult? {
        Log.d(TAG, "Direct backup started")
        return backupService.createBackupZipFromJournalFiles(
            shortcodes = shortcodes,
            dateKeywords = dateKeywords,
            keywords = keywords,
            writeJournalFiles = { zip ->
                journalStorage.copyJournalFilesToZip(
                    zip = zip,
                    uriString = settingsRepository.storageUri.value,
                    onProgress = onProgress
                )
            }
        ).also { result ->
            Log.d(TAG, "Direct backup finished: size=${result?.sizeBytes}")
        }
    }

    fun readBackupZip(uri: Uri): BackupBundle? = backupService.readBackupZip(uri)

    // ── Starred dates (persisted in markdown files) ─────────────────────

    /** Check if a date is starred by reading the markdown file. */
    fun isDateStarred(date: LocalDate): Boolean {
        ensureFrontmatterPopulated()
        return starredDates.containsKey(date)
    }

    /** Get the label for a starred date. Returns empty string if not starred or no label. */
    fun getStarredLabel(date: LocalDate): String {
        ensureFrontmatterPopulated()
        return starredDates[date] ?: ""
    }

    /** Get the day label for any date. Returns empty string if no label. */
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
        val entry = getEntriesForDate(date).getOrNull(entryIndex).orEmpty()
        return parseEntryRevisitMetadata(entry)
    }

    fun setRevisit(date: LocalDate, revisitOn: LocalDate?, note: String = ""): Boolean = withDateMutationLock(date) {
        ensureFrontmatterPopulated()
        val entries = getEntriesForDateForMutation(date)
        if (entries.isEmpty()) return@withDateMutationLock false
        if (!snapshotDateBeforeMutation(date, "set_revisit")) return@withDateMutationLock false

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

        val content = buildFileContent(date, entries)
        if (journalStorage.writeDateContent(date, content, createIfNotExists = false)) {
            finalizeFrontmatterWriteSuccess(
                date = date,
                entries = entries,
                saveLabelsCacheToDisk = { saveDayToDb(date) },
                scheduleFingerprintRefresh = ::scheduleFingerprintRefresh
            )
            true
        } else {
            finalizeFrontmatterWriteFailure(
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
    }

    /** Set or clear the day label for any date. Writes label: to frontmatter without changing starred status. */
    fun setDayLabel(date: LocalDate, label: String): Boolean = withDateMutationLock(date) {
        ensureFrontmatterPopulated()
        val entries = getEntriesForDateForMutation(date)
        val trimmed = label.take(30)
        val previousLabel = captureOptionalMapValue(date, dayLabels)
        val currentContent = journalStorage.readDateContent(date)
        if (!snapshotDateBeforeMutation(date, "set_day_label")) return@withDateMutationLock false

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
                    updateCachedDatePresence = { targetDate, hasEntries ->
                        updateCachedDatePresence(targetDate, hasEntries)
                    },
                    saveCacheToDisk = { deleteDayFromDb(date) },
                    saveEntryCountCacheToDisk = {},
                    saveWordCountCacheToDisk = {},
                    saveLabelsCacheToDisk = { deleteDayFromDb(date) },
                    removeTodoDate = { targetDate ->
                        todoIndexRepository.removeDate(targetDate)
                        eventIndexRepository.removeDate(targetDate)
                    },
                    scheduleFingerprintRefresh = ::scheduleFingerprintRefresh
                )
                true
            } else {
                finalizeFrontmatterWriteFailure(
                    saveLabelsCacheToDisk = { saveDayToDb(date) }
                ) {
                    restoreCapturedOptionalValue(date, previousLabel, dayLabels)
                }
            }
        } else {
            val content = buildFileContent(date, entries)
            val shouldCreateFile = currentContent.isNullOrBlank()
            if (journalStorage.writeDateContent(date, content, createIfNotExists = shouldCreateFile)) {
                finalizeFrontmatterWriteSuccess(
                    date = date,
                    entries = entries,
                    saveLabelsCacheToDisk = { saveDayToDb(date) },
                    scheduleFingerprintRefresh = ::scheduleFingerprintRefresh,
                    syncTodoIndexForDate = ::scheduleTodoIndexSyncForDate
                )
                if (entries.isEmpty()) {
                    finalizeLabelOnlyDayWriteSuccess(
                        date = date,
                        context = context,
                        cache = cache,
                        entryCountCache = entryCountCache,
                        wordCountCache = wordCountCache,
                        updateCachedDatePresence = { targetDate, hasEntries ->
                            updateCachedDatePresence(targetDate, hasEntries)
                        },
                        saveCacheToDisk = { saveDayToDb(date) },
                        saveEntryCountCacheToDisk = {},
                        saveWordCountCacheToDisk = {}
                    )
                }
                true
            } else {
                finalizeFrontmatterWriteFailure(
                    saveLabelsCacheToDisk = { saveDayToDb(date) }
                ) {
                    restoreCapturedOptionalValue(date, previousLabel, dayLabels)
                }
            }
        }
    }

    /** Toggle starred status for a date. */
    fun toggleStarred(date: LocalDate, label: String = ""): Boolean {
        val isCurrentlyStarred = isDateStarred(date)
        return setStarred(date, !isCurrentlyStarred, label)
    }

    /** Set starred status for a date with optional label. Updates the markdown file with frontmatter. */
    fun setStarred(date: LocalDate, starred: Boolean, label: String = ""): Boolean = withDateMutationLock(date) {
        ensureFrontmatterPopulated()
        val entries = getEntriesForDateForMutation(date)
        if (entries.isEmpty()) return@withDateMutationLock false
        if (!snapshotDateBeforeMutation(date, "set_starred")) return@withDateMutationLock false

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

        // Update the markdown file via buildFileContent to stay consistent
        val content = buildFileContent(date, entries)
        if (journalStorage.writeDateContent(date, content, createIfNotExists = false)) {
            finalizeFrontmatterWriteSuccess(
                date = date,
                entries = entries,
                saveLabelsCacheToDisk = { saveDayToDb(date) },
                scheduleFingerprintRefresh = ::scheduleFingerprintRefresh,
                syncTodoIndexForDate = ::scheduleTodoIndexSyncForDate
            )
            true
        } else {
            finalizeFrontmatterWriteFailure(
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
    }

    /** Get all starred dates. */
    fun getStarredDates(): List<LocalDate> {
        ensureFrontmatterPopulated()
        return starredDates.keys.sorted()
    }

    /** Return a snapshot of all day labels (date → label).
     *  Uses dayLabels as the authoritative source so edits made via the day label dialog
     *  are always reflected, regardless of how the label was originally set. */
    fun refreshFrontmatterSnapshot() {
        synchronized(this) {
            populateFrontmatterData()
            frontmatterPopulated = true
        }
    }

    fun getAllStarredLabels(): Map<LocalDate, String> {
        ensureFrontmatterPopulated()
        return dayLabels.toMap()
    }

    data class RevisitOverview(
        val markers: List<RevisitMarker>,
        val targetDates: Set<LocalDate>,
        val dueItems: List<DueRevisitItem>
    )

    /**
     * Produces all revisit state in one corpus scan. Prefer this over calling
     * [getAllRevisitMarkers] and [getDueRevisitItems] separately, which scans twice.
     */
    fun getRevisitOverview(targetDate: LocalDate): RevisitOverview {
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

        return RevisitOverview(
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

    /**
     * Walks every entry carrying entry-level revisit metadata. Sources entries from the
     * in-memory cache (or the Room snapshot before warmup completes) so the scan never
     * falls back to one SAF file read per day.
     */
    private fun forEachEntryRevisit(action: (LocalDate, Int, LocalDate, String) -> Unit) {
        val snapshot: Map<LocalDate, List<String>> =
            if (cachePopulated) {
                cache
            } else {
                getEntriesSnapshotForRebuild().ifEmpty {
                    getEntriesSnapshotForDates(getAllJournalDatesLightweight())
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

    fun getBackupJournalSnapshot(
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Map<LocalDate, BackupJournalDay> {
        val snapshot = linkedMapOf<LocalDate, BackupJournalDay>()
        val forcedDates = runCatching { getAllJournalDatesLightweight(forceRefresh = true) }
            .getOrElse {
                Log.w(TAG, "Backup date scan failed; falling back to cached/frontmatter dates", it)
                emptySet()
            }
        val candidateDates = linkedSetOf<LocalDate>().apply {
            addAll(forcedDates)
            addAll(getAllJournalDatesLightweight())
            addAll(cache.keys)
            addAll(dayLabels.keys)
            addAll(starredDates.keys)
            addAll(revisitDates.keys)
            addAll(revisitNotes.keys)
        }
        val sortedDates = candidateDates.sorted()
        val total = sortedDates.size.coerceAtLeast(1)
        onProgress(0, total)
        sortedDates.forEachIndexed { index, date ->
            val content = journalStorage.readDateContent(date)?.takeIf { it.isNotBlank() }
            if (content == null) {
                onProgress(index + 1, total)
                return@forEachIndexed
            }
            val lines = content.lines()
            val entries = parseEntries(lines)
            val frontmatter = parseFrontmatter(lines)
            snapshot[date] = BackupJournalDay(
                content = content,
                entries = entries,
                isStarred = frontmatter.isStarred,
                label = frontmatter.label
            )
            onProgress(index + 1, total)
        }
        return snapshot
    }

    /** Reload starred status and day labels for all dates from disk (called after populate cache). */
    private fun populateFrontmatterData() {
        starredDates.clear()
        dayLabels.clear()
        revisitDates.clear()
        revisitNotes.clear()
        val uriString = settingsRepository.storageUri.value
        if (uriString != null) {
            val rootUri = Uri.parse(uriString)
            val rootDir = DocumentFile.fromTreeUri(context, rootUri) ?: return

            rootDir.listFiles()?.forEach { yearDir ->
                if (yearDir.isDirectory && yearDir.name?.toIntOrNull() != null) {
                    yearDir.listFiles()?.forEach { monthDir ->
                        if (monthDir.isDirectory && monthDir.name?.toIntOrNull() != null) {
                            monthDir.listFiles()?.forEach { dayFile ->
                                if (dayFile.name?.endsWith(".md") == true) {
                                    val fileName = dayFile.name ?: return@forEach
                                    val dateString = fileName.removeSuffix(".md")
                                    try {
                                        val date = LocalDate.parse(dateString)
                                        val lines = context.contentResolver.openInputStream(dayFile.uri)
                                            ?.bufferedReader(Charsets.UTF_8)?.readLines() ?: return@forEach
                                        val snapshot = Companion.parseFrontmatter(lines)
                                        if (snapshot.isStarred) starredDates[date] = snapshot.label
                                        if (snapshot.label.isNotEmpty()) dayLabels[date] = snapshot.label
                                        snapshot.revisitOn?.let { revisitDates[date] = it }
                                        if (snapshot.revisitNote.isNotEmpty()) revisitNotes[date] = snapshot.revisitNote
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to check frontmatter for $fileName", e)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            defaultJournalsDir.listFiles()?.filter { it.name.endsWith(".md") }?.forEach { file ->
                val dateString = file.name.removeSuffix(".md")
                try {
                    val date = LocalDate.parse(dateString)
                    val lines = file.readLines(Charsets.UTF_8)
                    val snapshot = Companion.parseFrontmatter(lines)
                    if (snapshot.isStarred) starredDates[date] = snapshot.label
                    if (snapshot.label.isNotEmpty()) dayLabels[date] = snapshot.label
                    snapshot.revisitOn?.let { revisitDates[date] = it }
                    if (snapshot.revisitNote.isNotEmpty()) revisitNotes[date] = snapshot.revisitNote
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check frontmatter for ${file.name}", e)
                }
            }
        }
        // Persist the freshly scanned labels so next startup loads instantly
        saveLabelsCacheToDisk(immediate = true)
        frontmatterPopulated = true
    }

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

