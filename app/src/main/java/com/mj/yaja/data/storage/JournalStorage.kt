package com.mj.yaja.data.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class JournalStorage(
    private val context: Context,
    private val rootUriStringProvider: () -> String?,
    private val defaultJournalsDir: File,
    private val monthFormatter: DateTimeFormatter,
    private val parseEntriesFromContent: (String) -> List<String>,
    private val logError: (String, Exception) -> Unit
) {
    private data class SafDirectoryCache(
        val storageKey: String,
        val rootDir: DocumentFile,
        val lock: Any = Any(),
        val yearDirs: MutableMap<String, DocumentFile?> = mutableMapOf(),
        val monthDirsByYear: MutableMap<String, MutableMap<String, DocumentFile?>> = mutableMapOf(),
        // Positive day-file lookups only. DocumentFile.findFile lists the whole month
        // directory per call, so caching hits avoids O(month size) IPC per read. Misses
        // are always re-verified with a real findFile so external changes are still seen.
        val dayFilesByMonth: MutableMap<String, MutableMap<String, DocumentFile>> = mutableMapOf()
    )

    private val safDirectoryCacheLock = Any()
    @Volatile private var safDirectoryCache: SafDirectoryCache? = null

    fun getStorageKey(uriString: String?): String = uriString ?: "local"

    fun readEntriesForDate(date: LocalDate): List<String> {
        return try {
            parseEntriesFromContent(readDateContent(date) ?: return emptyList())
        } catch (e: Exception) {
            logError("Error reading entries for date $date from disk", e)
            emptyList()
        }
    }

    fun readDateContent(date: LocalDate): String? {
        val uriString = rootUriStringProvider()
        return if (uriString != null) {
            val docFile = getDocumentFileForDate(date, createIfNotExists = false) ?: return null
            if (!docFile.exists()) return null
            try {
                context.contentResolver.openInputStream(docFile.uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
            } catch (e: Exception) {
                logError("Error reading raw date content for $date", e)
                null
            }
        } else {
            val file = getFileForDate(date)
            if (!file.exists()) return null
            try {
                file.readText(Charsets.UTF_8)
            } catch (e: IOException) {
                logError("Error reading raw date content for $date", e)
                null
            }
        }
    }

    fun writeDateContent(date: LocalDate, content: String, createIfNotExists: Boolean): Boolean {
        val uriString = rootUriStringProvider()
        return if (uriString != null) {
            val docFile = getDocumentFileForDate(date, createIfNotExists = createIfNotExists) ?: return false
            if (!createIfNotExists && !docFile.exists()) return false
            try {
                val outputStream = context.contentResolver.openOutputStream(docFile.uri, "wt")
                    ?: return false
                outputStream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                true
            } catch (e: Exception) {
                logError("Exception caught", e)
                false
            }
        } else {
            val file = getFileForDate(date)
            if (!createIfNotExists && !file.exists()) return false
            writeLocalDateContentAtomically(file, content)
        }
    }

    private fun writeLocalDateContentAtomically(file: File, content: String): Boolean {
        val parent = file.parentFile ?: return false
        parent.mkdirs()
        val tempFile = File(parent, ".${file.name}.${System.nanoTime()}.tmp")
        return try {
            FileOutputStream(tempFile).use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            try {
                Files.move(
                    tempFile.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    tempFile.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            true
        } catch (e: Exception) {
            tempFile.delete()
            logError("Failed to atomically write date file ${file.name}", e)
            false
        }
    }

    fun deleteDateFile(date: LocalDate): Boolean {
        val uriString = rootUriStringProvider()
        return if (uriString != null) {
            val docFile = getDocumentFileForDate(date, createIfNotExists = false) ?: return false
            if (!docFile.exists()) {
                evictCachedDayFile(date)
                return false
            }
            try {
                docFile.delete().also { deleted ->
                    if (deleted) evictCachedDayFile(date)
                }
            } catch (e: Exception) {
                logError("Exception caught", e)
                false
            }
        } else {
            val file = getFileForDate(date)
            if (!file.exists()) return false
            try {
                file.delete()
            } catch (e: Exception) {
                logError("Exception caught", e)
                false
            }
        }
    }

    fun listJournalDates(uriString: String?): Set<LocalDate> {
        val datesWithData = mutableSetOf<LocalDate>()
        if (uriString != null) {
            try {
                val rootUri = Uri.parse(uriString)
                val rootDir = DocumentFile.fromTreeUri(context, rootUri) ?: return emptySet()
                rootDir.listFiles()?.forEach { yearDir ->
                    if (yearDir.isDirectory && yearDir.name?.toIntOrNull() != null) {
                        yearDir.listFiles()?.forEach { monthDir ->
                            if (monthDir.isDirectory && monthDir.name?.toIntOrNull() != null) {
                                monthDir.listFiles()?.forEach { dayFile ->
                                    if (dayFile.name?.endsWith(".md") == true) {
                                        val dateString = (dayFile.name ?: return@forEach).removeSuffix(".md")
                                        try {
                                            datesWithData.add(LocalDate.parse(dateString))
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logError("Exception caught", e)
            }
        } else {
            defaultJournalsDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".md")) {
                    val dateString = file.name.removeSuffix(".md")
                    try {
                        datesWithData.add(LocalDate.parse(dateString))
                    } catch (_: Exception) {
                    }
                }
            }
        }
        return datesWithData
    }

    fun copyJournalFilesToZip(
        zip: java.util.zip.ZipOutputStream,
        uriString: String?,
        onProgress: (Int, Int) -> Unit
    ): Int {
        return if (uriString != null) {
            copySafJournalFilesToZip(zip, uriString, onProgress)
        } else {
            copyLocalJournalFilesToZip(zip, onProgress)
        }
    }

    private fun copySafJournalFilesToZip(
        zip: java.util.zip.ZipOutputStream,
        uriString: String,
        onProgress: (Int, Int) -> Unit
    ): Int {
        val files = mutableListOf<Pair<LocalDate, DocumentFile>>()
        try {
            val rootUri = Uri.parse(uriString)
            val rootDir = DocumentFile.fromTreeUri(context, rootUri) ?: return 0
            rootDir.listFiles()?.forEach { yearDir ->
                if (yearDir.isDirectory && yearDir.name?.toIntOrNull() != null) {
                    yearDir.listFiles()?.forEach { monthDir ->
                        if (monthDir.isDirectory && monthDir.name?.toIntOrNull() != null) {
                            monthDir.listFiles()?.forEach { dayFile ->
                                val fileName = dayFile.name ?: return@forEach
                                if (dayFile.isFile && fileName.endsWith(".md")) {
                                    runCatching {
                                        LocalDate.parse(fileName.removeSuffix(".md"))
                                    }.getOrNull()?.let { date ->
                                        files.add(date to dayFile)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logError("Exception caught", e)
            return 0
        }

        val sortedFiles = files.sortedBy { it.first }
        val total = sortedFiles.size.coerceAtLeast(1)
        onProgress(0, total)
        var copied = 0
        sortedFiles.forEachIndexed { index, (date, dayFile) ->
            try {
                val entryName = "journal/${date.year}/${date.format(monthFormatter)}/$date.md"
                zip.putNextEntry(java.util.zip.ZipEntry(entryName))
                context.contentResolver.openInputStream(dayFile.uri)?.use { input ->
                    input.copyTo(zip)
                    copied++
                }
                zip.closeEntry()
                onProgress(index + 1, total)
            } catch (e: Exception) {
                logError("Error reading raw date content for $date", e)
                runCatching { zip.closeEntry() }
                onProgress(index + 1, total)
            }
        }
        return copied
    }

    private fun copyLocalJournalFilesToZip(
        zip: java.util.zip.ZipOutputStream,
        onProgress: (Int, Int) -> Unit
    ): Int {
        val files = defaultJournalsDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".md") }
            ?.mapNotNull { file ->
                runCatching {
                    LocalDate.parse(file.name.removeSuffix(".md")) to file
                }.getOrNull()
            }
            ?.sortedBy { it.first }
            .orEmpty()
        val total = files.size.coerceAtLeast(1)
        onProgress(0, total)
        var copied = 0
        files.forEachIndexed { index, (date, file) ->
            try {
                val entryName = "journal/${date.year}/${date.format(monthFormatter)}/$date.md"
                zip.putNextEntry(java.util.zip.ZipEntry(entryName))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
                copied++
                onProgress(index + 1, total)
            } catch (e: IOException) {
                logError("Error reading raw date content for $date", e)
                runCatching { zip.closeEntry() }
                onProgress(index + 1, total)
            }
        }
        return copied
    }

    fun computeSampledStorageFingerprint(
        uriString: String?,
        knownDates: Collection<LocalDate>,
        anchorDate: LocalDate = LocalDate.now(),
        windowDaysBefore: Long = 31L,
        windowDaysAfter: Long = 31L,
        fallbackRecentSampleCount: Int = 6
    ): JournalStorageFingerprint? {
        return try {
            if (knownDates.isEmpty()) {
                return JournalStorageFingerprint(
                    storageKey = getStorageKey(uriString),
                    fileCount = 0,
                    newestModifiedAt = 0L,
                    oldestModifiedAt = 0L,
                    metadataChecksum = FNV_OFFSET_BASIS,
                    computedAt = System.currentTimeMillis()
                )
            }

            val sortedDesc = knownDates.sortedDescending()
            val windowStart = anchorDate.minusDays(windowDaysBefore)
            val windowEnd = anchorDate.plusDays(windowDaysAfter)
            val focusedDates = sortedDesc.filter { !it.isBefore(windowStart) && !it.isAfter(windowEnd) }
            val sampleDates = LinkedHashSet<LocalDate>().apply {
                add(anchorDate)
                addAll(focusedDates)
                addAll(sortedDesc.take(fallbackRecentSampleCount))
            }

            val metadataResolver = createDateMetadataResolver(uriString)
            val records = sampleDates.map { date ->
                val metadata = metadataResolver(date)
                val size = metadata?.first ?: -1L
                val modifiedAt = metadata?.second ?: -1L
                Triple(date.toString(), size, modifiedAt)
            }.sortedBy { it.first }

            var newest = 0L
            var oldest = 0L
            var checksum = FNV_OFFSET_BASIS
            records.forEachIndexed { index, (path, size, modifiedAt) ->
                if (index == 0) {
                    newest = modifiedAt
                    oldest = modifiedAt
                } else {
                    newest = maxOf(newest, modifiedAt)
                    oldest = minOf(oldest, modifiedAt)
                }
                checksum = updateChecksum(checksum, path)
                checksum = updateChecksum(checksum, size.toString())
                checksum = updateChecksum(checksum, modifiedAt.toString())
            }
            checksum = updateChecksum(checksum, "count:${knownDates.size}")
            checksum = updateChecksum(checksum, "sample:${records.size}")

            JournalStorageFingerprint(
                storageKey = getStorageKey(uriString),
                fileCount = knownDates.size,
                newestModifiedAt = newest,
                oldestModifiedAt = oldest,
                metadataChecksum = checksum,
                computedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            logError("Failed to compute sampled journal storage fingerprint", e)
            null
        }
    }

    fun getEntriesForDateFromSpecificStorage(date: LocalDate, uriString: String?): List<String> {
        if (uriString != null) {
            val dayFile = getDocumentFileForDate(date, uriString, createIfNotExists = false) ?: return emptyList()
            return try {
                val content = context.contentResolver.openInputStream(dayFile.uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: return emptyList()
                parseEntriesFromContent(content)
            } catch (e: Exception) {
                logError("Exception caught", e)
                emptyList()
            }
        }

        val file = File(defaultJournalsDir, "$date.md")
        return if (file.exists()) {
            try {
                parseEntriesFromContent(file.readText(Charsets.UTF_8))
            } catch (e: IOException) {
                logError("Exception caught", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun addEntriesToSpecificStorage(date: LocalDate, entries: List<String>, uriString: String?): Boolean {
        if (uriString != null) {
            val docFile = getDocumentFileForDate(date, uriString, createIfNotExists = true) ?: return false

            try {
                val existingEntries = getEntriesForDateFromSpecificStorage(date, uriString)
                val entriesToAdd = entries.filter { !existingEntries.contains(it) }
                if (entriesToAdd.isEmpty()) return true

                val existingContent = readDateContentFromSpecificStorage(date, uriString)
                val content = appendEntriesToRawContent(date, existingContent, entriesToAdd)
                val outputStream = context.contentResolver.openOutputStream(docFile.uri, "wt")
                    ?: return false
                outputStream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                return true
            } catch (e: Exception) {
                logError("Exception caught", e)
                return false
            }
        }

        val file = File(defaultJournalsDir, "$date.md")
        return try {
            val existingEntries = if (file.exists()) {
                parseEntriesFromContent(file.readText(Charsets.UTF_8))
            } else {
                emptyList()
            }
            val entriesToAdd = entries.filter { !existingEntries.contains(it) }
            if (entriesToAdd.isEmpty()) return true

            val existingContent = if (file.exists()) file.readText(Charsets.UTF_8) else null
            val content = appendEntriesToRawContent(date, existingContent, entriesToAdd)
            writeLocalDateContentAtomically(file, content)
        } catch (e: IOException) {
            logError("Exception caught", e)
            false
        }
    }

    private fun readDateContentFromSpecificStorage(date: LocalDate, uriString: String?): String? {
        if (uriString != null) {
            val dayFile = getDocumentFileForDate(date, uriString, createIfNotExists = false) ?: return null
            return try {
                context.contentResolver.openInputStream(dayFile.uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
            } catch (e: Exception) {
                logError("Exception caught", e)
                null
            }
        }

        val file = File(defaultJournalsDir, "$date.md")
        return if (file.exists()) {
            try {
                file.readText(Charsets.UTF_8)
            } catch (e: IOException) {
                logError("Exception caught", e)
                null
            }
        } else {
            null
        }
    }

    private fun appendEntriesToRawContent(
        date: LocalDate,
        existingContent: String?,
        entriesToAdd: List<String>
    ): String {
        val base = existingContent?.takeIf { it.isNotBlank() }?.trimEnd()
            ?: "# $date"
        return buildString {
            append(base)
            append("\n\n")
            entriesToAdd.forEach { entry ->
                appendFormattedEntry(entry)
            }
        }
    }

    private fun StringBuilder.appendFormattedEntry(entry: String) {
        entry.lines().forEachIndexed { index, line ->
            when (index) {
                0 -> append("- ").append(line).append("\n")
                else -> {
                    if (line.isBlank()) append("\n") else append("  ").append(line).append("\n")
                }
            }
        }
    }

    fun hasDateFile(date: LocalDate, uriString: String?): Boolean {
        val metadataResolver = createDateMetadataResolver(uriString)
        return metadataResolver(date) != null
    }

    fun getDateFileMetadata(date: LocalDate, uriString: String?): Pair<Long, Long>? {
        if (uriString != null) {
            return try {
                val dayFile = getDocumentFileForDate(date, uriString, createIfNotExists = false)
                    ?: return null
                if (!dayFile.exists()) {
                    evictCachedDayFile(date)
                    return null
                }
                dayFile.length().coerceAtLeast(0L) to dayFile.lastModified().coerceAtLeast(0L)
            } catch (e: Exception) {
                logError("Failed to read file metadata for $date", e)
                null
            }
        }
        return try {
            val file = getFileForDate(date)
            if (!file.exists()) null
            else file.length().coerceAtLeast(0L) to file.lastModified().coerceAtLeast(0L)
        } catch (e: Exception) {
            logError("Failed to read file metadata for $date", e)
            null
        }
    }

    fun getDateWindowPresence(
        anchorDate: LocalDate,
        daysBefore: Long,
        daysAfter: Long,
        uriString: String?
    ): Map<LocalDate, Boolean> {
        val metadataResolver = createDateMetadataResolver(uriString)
        val result = linkedMapOf<LocalDate, Boolean>()
        var cursor = anchorDate.minusDays(daysBefore)
        val end = anchorDate.plusDays(daysAfter)
        while (!cursor.isAfter(end)) {
            result[cursor] = metadataResolver(cursor) != null
            cursor = cursor.plusDays(1)
        }
        return result
    }

    private fun getFileForDate(date: LocalDate): File = File(defaultJournalsDir, "$date.md")

    private fun createDateMetadataResolver(uriString: String?): (LocalDate) -> Pair<Long, Long>? {
        return if (uriString != null) {
            val rootUri = Uri.parse(uriString)
            val rootDir = DocumentFile.fromTreeUri(context, rootUri)
            if (rootDir == null) {
                { null }
            } else {
                run {
                    val yearDirs = rootDir.listFiles()
                        ?.filter { it.isDirectory }
                        ?.associateBy { it.name.orEmpty() }
                        .orEmpty()
                    val monthDirsByYear = mutableMapOf<String, Map<String, DocumentFile>>()
                    val resolver: (LocalDate) -> Pair<Long, Long>? = { date ->
                        try {
                            val yearKey = date.year.toString()
                            val monthKey = date.format(monthFormatter)
                            val yearDir = yearDirs[yearKey]
                            if (yearDir == null) {
                                null
                            } else {
                                val monthDirs = monthDirsByYear.getOrPut(yearKey) {
                                    yearDir.listFiles()
                                        ?.filter { it.isDirectory }
                                        ?.associateBy { it.name.orEmpty() }
                                        .orEmpty()
                                }
                                val monthDir = monthDirs[monthKey]
                                if (monthDir == null) {
                                    null
                                } else {
                                    val dayFile = monthDir.findFile("$date.md")
                                    if (dayFile == null) null
                                    else dayFile.length().coerceAtLeast(0L) to dayFile.lastModified().coerceAtLeast(0L)
                                }
                            }
                        } catch (e: Exception) {
                            logError("Failed to read file metadata for $date", e)
                            null
                        }
                    }
                    resolver
                }
            }
        } else {
            { date ->
                try {
                    val file = getFileForDate(date)
                    if (!file.exists()) null
                    else file.length().coerceAtLeast(0L) to file.lastModified().coerceAtLeast(0L)
                } catch (e: Exception) {
                    logError("Failed to read file metadata for $date", e)
                    null
                }
            }
        }
    }

    private fun updateChecksum(seed: Long, value: String): Long {
        var hash = seed
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            hash = hash xor (byte.toLong() and 0xff)
            hash *= FNV_PRIME
        }
        return hash
    }

    private fun getDocumentFileForDate(date: LocalDate, createIfNotExists: Boolean): DocumentFile? {
        val uriString = rootUriStringProvider() ?: return null
        return getDocumentFileForDate(date, uriString, createIfNotExists)
    }

    private fun getDocumentFileForDate(
        date: LocalDate,
        uriString: String,
        createIfNotExists: Boolean
    ): DocumentFile? {
        val directoryCache = getOrCreateSafDirectoryCache(uriString) ?: return null
        val year = date.year.toString()
        val month = date.format(monthFormatter)
        val monthKey = "$year/$month"
        val dayFileName = "$date.md"

        synchronized(directoryCache.lock) {
            directoryCache.dayFilesByMonth[monthKey]?.get(dayFileName)?.let { return it }
        }

        val yearDir = resolveYearDir(directoryCache, year, createIfNotExists) ?: return null
        val monthDir = resolveMonthDir(directoryCache, year, yearDir, month, createIfNotExists) ?: return null

        var dayFile = monthDir.findFile(dayFileName)
        if (dayFile == null && createIfNotExists) {
            dayFile = monthDir.createFile("text/markdown", dayFileName)
        }
        if (dayFile != null) {
            synchronized(directoryCache.lock) {
                directoryCache.dayFilesByMonth.getOrPut(monthKey) { mutableMapOf() }[dayFileName] = dayFile
            }
        }
        return dayFile
    }

    private fun evictCachedDayFile(date: LocalDate) {
        val directoryCache = safDirectoryCache ?: return
        val monthKey = "${date.year}/${date.format(monthFormatter)}"
        synchronized(directoryCache.lock) {
            directoryCache.dayFilesByMonth[monthKey]?.remove("$date.md")
        }
    }

    /** Drop all cached SAF directory/file handles (e.g. after a storage location change). */
    fun invalidateDirectoryCache() {
        synchronized(safDirectoryCacheLock) {
            safDirectoryCache = null
        }
    }

    private fun getOrCreateSafDirectoryCache(uriString: String): SafDirectoryCache? {
        val storageKey = getStorageKey(uriString)
        synchronized(safDirectoryCacheLock) {
            val existing = safDirectoryCache
            if (existing != null && existing.storageKey == storageKey) {
                return existing
            }

            val rootUri = Uri.parse(uriString)
            val rootDir = DocumentFile.fromTreeUri(context, rootUri) ?: return null
            return SafDirectoryCache(
                storageKey = storageKey,
                rootDir = rootDir
            ).also { safDirectoryCache = it }
        }
    }

    private fun resolveYearDir(
        cache: SafDirectoryCache,
        year: String,
        createIfMissing: Boolean
    ): DocumentFile? {
        synchronized(cache.lock) {
            if (cache.yearDirs.containsKey(year)) {
                val cached = cache.yearDirs[year]
                if (cached != null || !createIfMissing) return cached
            }
            val resolved = cache.rootDir.findFile(year)
                ?: if (createIfMissing) cache.rootDir.createDirectory(year) else null
            cache.yearDirs[year] = resolved
            return resolved
        }
    }

    private fun resolveMonthDir(
        cache: SafDirectoryCache,
        year: String,
        yearDir: DocumentFile,
        month: String,
        createIfMissing: Boolean
    ): DocumentFile? {
        synchronized(cache.lock) {
            val monthDirs = cache.monthDirsByYear.getOrPut(year) { mutableMapOf() }
            if (monthDirs.containsKey(month)) {
                val cached = monthDirs[month]
                if (cached != null || !createIfMissing) return cached
            }
            val resolved = yearDir.findFile(month)
                ?: if (createIfMissing) yearDir.createDirectory(month) else null
            monthDirs[month] = resolved
            return resolved
        }
    }

    companion object {
        private const val FNV_OFFSET_BASIS = -3750763034362895579L
        private const val FNV_PRIME = 1099511628211L
    }
}
