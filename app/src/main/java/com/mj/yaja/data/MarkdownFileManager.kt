package com.mj.yaja.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    companion object {
        private const val TAG = "MarkdownFileManager"
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

        /** Parses a list of raw file lines into individual journal entry strings. */
        internal fun parseEntries(lines: List<String>): List<String> {
            val entries = mutableListOf<String>()
            var currentEntry = StringBuilder()
            for (line in lines) {
                if (line.startsWith("# ")) continue // Skip date headings
                if (line.isBlank() && currentEntry.isEmpty()) continue

                if (line.startsWith("- ")) {
                    if (currentEntry.isNotEmpty()) {
                        entries.add(currentEntry.toString().trimEnd())
                    }
                    currentEntry = StringBuilder()
                    currentEntry.append(line.removePrefix("- "))
                } else if (currentEntry.isNotEmpty()) {
                    currentEntry.append("\n").append(line)
                }
            }
            if (currentEntry.isNotEmpty()) {
                entries.add(currentEntry.toString().trimEnd())
            }
            return entries
        }
    }

    // ── In-memory cache ──────────────────────────────────────────────────
    private val cache = ConcurrentHashMap<LocalDate, List<String>>()
    @Volatile private var cachePopulated = false
    private val cacheScope = externalScope ?: CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val diskCacheFile: File by lazy { File(context.filesDir, "journal_cache_v1.json") }

    private fun ensureCachePopulated(onProgress: ((Int, Int) -> Unit)? = null) {
        if (!cachePopulated) {
            synchronized(this) {
                if (!cachePopulated) {
                    if (loadCacheFromDisk()) {
                        cachePopulated = true
                        // Background refresh to pick up any changes while app was closed
                        cacheScope.launch {
                            populateCache(onProgress)
                            saveCacheToDisk()
                        }
                    } else {
                        populateCache(onProgress)
                        cachePopulated = true
                        saveCacheToDisk()
                    }
                }
            }
        }
    }

    private fun saveCacheToDisk() {
        cacheScope.launch {
            try {
                val json = JSONObject()
                cache.forEach { (date, entries) ->
                    val array = JSONArray()
                    entries.forEach { array.put(it) }
                    json.put(date.toString(), array)
                }
                diskCacheFile.writeText(json.toString(), Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save cache to disk", e)
            }
        }
    }

    private fun loadCacheFromDisk(): Boolean {
        if (!diskCacheFile.exists()) return false
        return try {
            val json = JSONObject(diskCacheFile.readText(Charsets.UTF_8))
            val newCache = mutableMapOf<LocalDate, List<String>>()
            json.keys().forEach { key ->
                val date = LocalDate.parse(key)
                val array = json.getJSONArray(key)
                val entries = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    entries.add(array.getString(i))
                }
                newCache[date] = entries
            }
            cache.clear()
            cache.putAll(newCache)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cache from disk", e)
            false
        }
    }

    private fun populateCache(onProgress: ((Int, Int) -> Unit)? = null) {
        val newCache = mutableMapOf<LocalDate, List<String>>()
        val uriString = settingsRepository.storageUri.value
        if (uriString != null) {
            try {
                val rootUri = Uri.parse(uriString)
                val rootDir = DocumentFile.fromTreeUri(context, rootUri) ?: return

                // Pass 1: Collect all .md files to get total count
                val allFiles = mutableListOf<DocumentFile>()
                rootDir.listFiles()?.forEach { yearDir ->
                    if (yearDir.isDirectory && yearDir.name?.toIntOrNull() != null) {
                        yearDir.listFiles()?.forEach { monthDir ->
                            if (monthDir.isDirectory && monthDir.name?.toIntOrNull() != null) {
                                monthDir.listFiles()?.forEach { dayFile ->
                                    if (dayFile.name?.endsWith(".md") == true) {
                                        allFiles.add(dayFile)
                                    }
                                }
                            }
                        }
                    }
                }

                val total = allFiles.size
                // Pass 2: Process files with progress reporting
                allFiles.forEachIndexed { index, dayFile ->
                    val fileName = dayFile.name ?: return@forEachIndexed
                    val dateString = fileName.removeSuffix(".md")
                    try {
                        val date = LocalDate.parse(dateString)
                        val entries =
                                context.contentResolver.openInputStream(dayFile.uri)?.use { stream
                                    ->
                                    parseEntries(stream.bufferedReader(Charsets.UTF_8).readLines())
                                }
                                        ?: emptyList()
                        if (entries.isNotEmpty()) {
                            newCache[date] = entries
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse file: ${dayFile.name}", e)
                    }
                    onProgress?.invoke(index + 1, total)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to populate cache from external storage", e)
            }
        } else {
            val files =
                    defaultJournalsDir.listFiles()?.filter { it.name.endsWith(".md") }
                            ?: emptyList()
            val total = files.size
            files.forEachIndexed { index, file ->
                val dateString = file.name.removeSuffix(".md")
                try {
                    val date = LocalDate.parse(dateString)
                    val entries = parseEntries(file.readLines(Charsets.UTF_8))
                    if (entries.isNotEmpty()) {
                        newCache[date] = entries
                    }
                } catch (_: Exception) {}
                onProgress?.invoke(index + 1, total)
            }
        }

        synchronized(this) {
            cache.clear()
            cache.putAll(newCache)
        }
    }

    fun invalidateCache() {
        synchronized(this) {
            cache.clear()
            cachePopulated = false
            if (diskCacheFile.exists()) diskCacheFile.delete()
        }
    }

    fun forceRefresh(onProgress: (Int, Int) -> Unit) {
        invalidateCache()
        ensureCachePopulated(onProgress)
    }

    private val defaultJournalsDir: File by lazy {
        File(context.filesDir, "journals").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun getFileForDate(date: LocalDate): File? {
        val uriString = settingsRepository.storageUri.value
        if (uriString == null) {
            return File(defaultJournalsDir, "$date.md")
        }
        return null // Indicates using DocumentFile
    }

    private fun getDocumentFileForDate(
            date: LocalDate,
            createIfNotExists: Boolean = false
    ): DocumentFile? {
        val uriString = settingsRepository.storageUri.value ?: return null
        val rootUri = Uri.parse(uriString)
        val rootDir = DocumentFile.fromTreeUri(context, rootUri) ?: return null

        val year = date.year.toString()
        val month = date.format(MONTH_FORMATTER)
        val dayFileName = "$date.md"

        var yearDir = rootDir.findFile(year)
        if (yearDir == null && createIfNotExists) yearDir = rootDir.createDirectory(year)
        if (yearDir == null) return null

        var monthDir = yearDir.findFile(month)
        if (monthDir == null && createIfNotExists) monthDir = yearDir.createDirectory(month)
        if (monthDir == null) return null

        var dayFile = monthDir.findFile(dayFileName)
        if (dayFile == null && createIfNotExists) {
            dayFile = monthDir.createFile("text/markdown", dayFileName)
        }
        return dayFile
    }

    fun hasEntriesForDate(date: LocalDate): Boolean {
        ensureCachePopulated()
        return cache[date]?.isNotEmpty() == true
    }

    fun getTotalEntryCount(): Int {
        ensureCachePopulated()
        return cache.values.sumOf { it.size }
    }

    private fun parseEntries(lines: List<String>): List<String> =
            Companion.parseEntries(lines)

    fun getEntriesForDate(date: LocalDate): List<String> {
        ensureCachePopulated()
        return cache[date] ?: emptyList()
    }

    fun addEntryForDate(date: LocalDate, entry: String) {
        if (entry.isBlank()) return
        val uriString = settingsRepository.storageUri.value
        if (uriString != null) {
            val docFile = getDocumentFileForDate(date, createIfNotExists = true) ?: return
            try {
                val exists = docFile.exists() && docFile.length() > 0
                val openMode = if (exists) "wa" else "w"
                context.contentResolver.openOutputStream(docFile.uri, openMode)?.use { outputStream
                    ->
                    outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        if (!exists) {
                            writer.write("# $date\n\n")
                        }
                        writer.write("- $entry\n")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception caught", e)
            }
        } else {
            val file = getFileForDate(date) ?: return
            try {
                if (!file.exists()) {
                    file.createNewFile()
                    file.appendText("# $date\n\n", Charsets.UTF_8)
                }
                file.appendText("- $entry\n", Charsets.UTF_8)
            } catch (e: IOException) {
                Log.e(TAG, "Exception caught", e)
            }
        }
        // Sync cache
        synchronized(this) {
            ensureCachePopulated()
            val newEntries = (cache[date] ?: emptyList()) + entry
            cache[date] = newEntries
        }
        saveCacheToDisk()

        // Update persistent count to avoid anomaly dialog on next cold start
        settingsRepository.setLastKnownEntryCount(getTotalEntryCount())
    }

    /**
     * Insert an entry at a specific position (0-based index) in the day's entries. Used by undo to
     * restore a deleted entry to its original position.
     */
    fun insertEntryAtPosition(date: LocalDate, entry: String, index: Int) {
        if (entry.isBlank()) return
        ensureCachePopulated()
        val existingEntries = cache[date] ?: emptyList()
        val mutableEntries = existingEntries.toMutableList()
        val clampedIndex = index.coerceIn(0, mutableEntries.size)
        mutableEntries.add(clampedIndex, entry)

        // Write to disk
        val uriString = settingsRepository.storageUri.value
        if (uriString != null) {
            val docFile = getDocumentFileForDate(date, createIfNotExists = true) ?: return
            try {
                context.contentResolver.openOutputStream(docFile.uri, "wt")?.use { outputStream ->
                    outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.write("# $date\n\n")
                        mutableEntries.forEach { e -> writer.write("- $e\n") }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception caught", e)
            }
        } else {
            val file = getFileForDate(date) ?: return
            try {
                file.writeText("# $date\n\n", Charsets.UTF_8)
                mutableEntries.forEach { e -> file.appendText("- $e\n", Charsets.UTF_8) }
            } catch (e: IOException) {
                Log.e(TAG, "Exception caught", e)
            }
        }
        // Sync cache
        cache[date] = mutableEntries
        saveCacheToDisk()
    }

    fun deleteEntryForDate(date: LocalDate, indexToDelete: Int) {
        ensureCachePopulated()
        val currentEntries = cache[date] ?: return
        if (indexToDelete !in currentEntries.indices) return

        val newEntries = currentEntries.toMutableList()
        newEntries.removeAt(indexToDelete)

        // Write to disk
        val uriString = settingsRepository.storageUri.value
        if (uriString != null) {
            val docFile = getDocumentFileForDate(date) ?: return
            if (!docFile.exists()) return
            try {
                if (newEntries.isEmpty()) {
                    docFile.delete()
                } else {
                    context.contentResolver.openOutputStream(docFile.uri, "wt")?.use { outputStream
                        ->
                        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.write("# $date\n\n")
                            newEntries.forEach { entry -> writer.write("- $entry\n") }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception caught", e)
            }
        } else {
            val file = getFileForDate(date) ?: return
            if (!file.exists()) return
            try {
                if (newEntries.isEmpty()) {
                    file.delete()
                } else {
                    file.writeText("# $date\n\n", Charsets.UTF_8)
                    newEntries.forEach { entry -> file.appendText("- $entry\n", Charsets.UTF_8) }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Exception caught", e)
            }
        }
        // Sync cache
        if (newEntries.isEmpty()) cache.remove(date) else cache[date] = newEntries
        saveCacheToDisk()
    }

    fun updateEntryForDate(date: LocalDate, indexToUpdate: Int, newEntry: String) {
        ensureCachePopulated()
        val currentEntries = cache[date] ?: return
        if (indexToUpdate !in currentEntries.indices) return

        val updatedEntries = currentEntries.toMutableList()
        updatedEntries[indexToUpdate] = newEntry

        // Write to disk
        val uriString = settingsRepository.storageUri.value
        if (uriString != null) {
            val docFile = getDocumentFileForDate(date) ?: return
            if (!docFile.exists()) return
            try {
                context.contentResolver.openOutputStream(docFile.uri, "wt")?.use { outputStream ->
                    outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.write("# $date\n\n")
                        updatedEntries.forEach { entry -> writer.write("- $entry\n") }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception caught", e)
            }
        } else {
            val file = getFileForDate(date) ?: return
            if (!file.exists()) return
            try {
                file.writeText("# $date\n\n", Charsets.UTF_8)
                updatedEntries.forEach { entry -> file.appendText("- $entry\n", Charsets.UTF_8) }
            } catch (e: IOException) {
                Log.e(TAG, "Exception caught", e)
            }
        }
        // Sync cache
        cache[date] = updatedEntries
        saveCacheToDisk()
    }

    fun setEntriesForDate(date: LocalDate, newEntries: List<String>) {
        ensureCachePopulated()
        if (newEntries.isEmpty()) return

        // Write to disk
        val uriString = settingsRepository.storageUri.value
        if (uriString != null) {
            val docFile = getDocumentFileForDate(date) ?: return
            if (!docFile.exists()) return
            try {
                context.contentResolver.openOutputStream(docFile.uri, "wt")?.use { outputStream ->
                    outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.write("# $date\n\n")
                        newEntries.forEach { entry -> writer.write("- $entry\n") }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception caught", e)
            }
        } else {
            val file = getFileForDate(date) ?: return
            if (!file.exists()) return
            try {
                file.writeText("# $date\n\n", Charsets.UTF_8)
                newEntries.forEach { entry -> file.appendText("- $entry\n", Charsets.UTF_8) }
            } catch (e: IOException) {
                Log.e(TAG, "Exception caught", e)
            }
        }
        // Sync cache
        cache[date] = newEntries
        saveCacheToDisk()
    }

    fun getAllJournalDatesWithData(): Set<LocalDate> {
        ensureCachePopulated()
        return cache.keys.toSet()
    }

    fun searchEntries(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        ensureCachePopulated()
        val results = mutableListOf<SearchResult>()
        val timestampRegex = Regex("<!--.*?-->")
        // Split into words for AND-logic: all words must appear in the entry
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }

        cache.keys.sortedDescending().forEach { date ->
            cache[date]?.forEach { entry ->
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
        }
        return results
    }

    suspend fun migrateEntries(fromUriString: String?, toUriString: String?) = withContext(Dispatchers.IO) {
        // If they are the same, nothing to do
        if (fromUriString == toUriString) return@withContext

        val datesWithData = mutableSetOf<LocalDate>()

        // 1. Gather all dates from the SOURCE
        if (fromUriString != null) {
            try {
                val rootUri = Uri.parse(fromUriString)
                val rootDir = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext
                rootDir.listFiles()?.forEach { yearDir ->
                    if (yearDir.isDirectory && yearDir.name?.toIntOrNull() != null) {
                        yearDir.listFiles()?.forEach { monthDir ->
                            if (monthDir.isDirectory && monthDir.name?.toIntOrNull() != null) {
                                monthDir.listFiles()?.forEach { dayFile ->
                                    if (dayFile.name?.endsWith(".md") == true) {
                                        val dateString = dayFile.name!!.removeSuffix(".md")
                                        try {
                                            datesWithData.add(LocalDate.parse(dateString))
                                        } catch (e: Exception) {}
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception caught", e)
            }
        } else {
            defaultJournalsDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".md")) {
                    val dateString = file.name.removeSuffix(".md")
                    try {
                        datesWithData.add(LocalDate.parse(dateString))
                    } catch (e: Exception) {}
                }
            }
        }

        // 2. Read from SOURCE and write to DESTINATION
        for (date in datesWithData) {
            val entries = getEntriesForDateFromSpecificStorage(date, fromUriString)
            if (entries.isNotEmpty()) {
                addEntriesToSpecificStorage(date, entries, toUriString)
            }
        }

        // 3. Invalidate cache since storage has changed
        invalidateCache()
    }

    private fun getEntriesForDateFromSpecificStorage(
            date: LocalDate,
            uriString: String?
    ): List<String> {
        if (uriString != null) {
            val rootUri = Uri.parse(uriString)
            val rootDir = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
            val year = date.year.toString()
            val month = date.format(DateTimeFormatter.ofPattern("MM"))
            val docFile =
                    rootDir.findFile(year)?.findFile(month)?.findFile("$date.md")
                            ?: return emptyList()

            if (!docFile.exists()) return emptyList()
            return try {
                context.contentResolver.openInputStream(docFile.uri)?.use { inputStream ->
                    val lines = inputStream.bufferedReader(Charsets.UTF_8).readLines()
                    parseEntries(lines)
                }
                        ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Exception caught", e)
                emptyList()
            }
        } else {
            val file = File(defaultJournalsDir, "$date.md")
            if (!file.exists()) return emptyList()
            return try {
                val lines = file.readLines(Charsets.UTF_8)
                parseEntries(lines)
            } catch (e: IOException) {
                Log.e(TAG, "Exception caught", e)
                emptyList()
            }
        }
    }

    private fun addEntriesToSpecificStorage(
            date: LocalDate,
            entries: List<String>,
            uriString: String?
    ) {
        if (uriString != null) {
            val rootUri = Uri.parse(uriString)
            val rootDir = DocumentFile.fromTreeUri(context, rootUri) ?: return

            val year = date.year.toString()
            val month = date.format(MONTH_FORMATTER)
            val dayFileName = "$date.md"

            var yearDir = rootDir.findFile(year) ?: rootDir.createDirectory(year) ?: return
            var monthDir = yearDir.findFile(month) ?: yearDir.createDirectory(month) ?: return
            var docFile =
                    monthDir.findFile(dayFileName)
                            ?: monthDir.createFile("text/markdown", dayFileName) ?: return

            try {
                // For migration, we usually append or overwrite.
                // Let's read what's there to avoid duplicates if partially migrated.
                val existingEntries = getEntriesForDateFromSpecificStorage(date, uriString)
                val entriesToAdd = entries.filter { !existingEntries.contains(it) }

                if (entriesToAdd.isEmpty()) return

                val exists = docFile.exists() && docFile.length() > 0
                val openMode = if (exists) "wa" else "w"
                context.contentResolver.openOutputStream(docFile.uri, openMode)?.use { outputStream
                    ->
                    outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        if (!exists) {
                            writer.write("# $date\n\n")
                        }
                        entriesToAdd.forEach { entry -> writer.write("- $entry\n") }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception caught", e)
            }
        } else {
            val file = File(defaultJournalsDir, "$date.md")
            try {
                val existingEntries =
                        if (file.exists()) {
                            parseEntries(file.readLines())
                        } else emptyList()

                val entriesToAdd = entries.filter { !existingEntries.contains(it) }
                if (entriesToAdd.isEmpty()) return

                if (!file.exists()) {
                    file.createNewFile()
                    file.appendText("# $date\n\n", Charsets.UTF_8)
                }
                entriesToAdd.forEach { entry -> file.appendText("- $entry\n", Charsets.UTF_8) }
            } catch (e: IOException) {
                Log.e(TAG, "Exception caught", e)
            }
        }
    }

    fun createBackupZip(shortcodes: Map<String, String>): Uri? {
        ensureCachePopulated()
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val dateStr =
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                        .format(java.time.LocalDateTime.now())
        val zipFile = File(backupDir, "journal_backup_$dateStr.zip")

        try {
            java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                // 1. Add shortcodes.csv to root
                if (shortcodes.isNotEmpty()) {
                    zos.putNextEntry(java.util.zip.ZipEntry("shortcodes.csv"))
                    shortcodes.forEach { (code, value) ->
                        zos.write("$code,$value\n".toByteArray(Charsets.UTF_8))
                    }
                    zos.closeEntry()
                }

                // 2. Add journal entries
                cache.keys.sorted().forEach { date ->
                    val entries = cache[date] ?: return@forEach
                    if (entries.isNotEmpty()) {
                        val entryName =
                                "${date.year}/${date.format(DateTimeFormatter.ofPattern("MM"))}/$date.md"
                        zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                        zos.write("# $date\n\n".toByteArray(Charsets.UTF_8))
                        entries.forEach { entry ->
                            zos.write("- $entry\n".toByteArray(Charsets.UTF_8))
                        }
                        zos.closeEntry()
                    }
                }
            }
            return androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create backup zip", e)
            return null
        }
    }
}
