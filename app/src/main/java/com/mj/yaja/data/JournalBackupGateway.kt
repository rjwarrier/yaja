package com.mj.yaja.data

import android.net.Uri
import android.util.Log
import com.mj.yaja.data.backup.BackupService
import com.mj.yaja.data.backup.BackupService.BackupBundle
import com.mj.yaja.data.backup.BackupService.BackupJournalDay
import com.mj.yaja.data.storage.JournalStorage
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal class JournalBackupGateway(
    private val backupService: BackupService,
    private val journalStorage: JournalStorage,
    private val storageUriProvider: () -> String?,
    private val cache: ConcurrentHashMap<LocalDate, List<String>>,
    private val dayLabels: ConcurrentHashMap<LocalDate, String>,
    private val starredDates: ConcurrentHashMap<LocalDate, String>,
    private val revisitDates: ConcurrentHashMap<LocalDate, LocalDate>,
    private val revisitNotes: ConcurrentHashMap<LocalDate, String>,
    private val allJournalDatesProvider: (forceRefresh: Boolean) -> Set<LocalDate>,
    private val invalidateCache: () -> Unit
) {
    suspend fun migrateEntries(fromUriString: String?, toUriString: String?) = withContext(Dispatchers.IO) {
        // If they are the same, nothing to do
        if (fromUriString == toUriString) return@withContext

        val datesWithData = journalStorage.listJournalDates(fromUriString)

        // Read from SOURCE and write to DESTINATION
        for (date in datesWithData) {
            ensureActive()
            val sourceContent = journalStorage.readDateContentFromSpecificStorage(date, fromUriString)
                ?: throw IOException("Failed to read source journal file for $date")
            ensureActive()
            val destinationContent = journalStorage.readDateContentFromSpecificStorage(date, toUriString)
            val contentToWrite = if (destinationContent.isNullOrBlank()) {
                sourceContent
            } else {
                mergeJournalDateContentForMigration(
                    date = date,
                    destinationContent = destinationContent,
                    sourceContent = sourceContent
                )
            }
            if (!journalStorage.writeDateContentToSpecificStorage(date, contentToWrite, toUriString)) {
                throw IOException("Failed to migrate journal file for $date")
            }
            ensureActive()
        }

        // Invalidate cache since storage has changed
        invalidateCache()
    }

    fun createBackupZip(
        shortcodes: Map<String, String>,
        dateKeywords: List<DateKeywordEntry> = emptyList(),
        keywords: List<KeywordDefinition> = emptyList(),
        recurringTasks: List<RecurringTaskItem> = emptyList(),
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): BackupService.BackupZipResult? {
        Log.d(TAG, "Direct backup started")
        return backupService.createBackupZipFromJournalFiles(
            shortcodes = shortcodes,
            dateKeywords = dateKeywords,
            keywords = keywords,
            recurringTasks = recurringTasks,
            writeJournalFiles = { zip ->
                journalStorage.copyJournalFilesToZip(
                    zip = zip,
                    uriString = storageUriProvider(),
                    onProgress = onProgress
                )
            }
        ).also { result ->
            Log.d(TAG, "Direct backup finished: size=${result?.sizeBytes}")
        }
    }

    fun readBackupZip(uri: Uri): BackupBundle? = backupService.readBackupZip(uri)

    fun getBackupJournalSnapshot(
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Map<LocalDate, BackupJournalDay> {
        val snapshot = linkedMapOf<LocalDate, BackupJournalDay>()
        val forcedDates = runCatching { allJournalDatesProvider(true) }
            .getOrElse {
                Log.w(TAG, "Backup date scan failed; falling back to cached/frontmatter dates", it)
                emptySet()
            }
        val candidateDates = linkedSetOf<LocalDate>().apply {
            addAll(forcedDates)
            addAll(allJournalDatesProvider(false))
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
            val entries = MarkdownFileManager.parseEntries(lines)
            val frontmatter = MarkdownFileManager.parseFrontmatter(lines)
            snapshot[date] = BackupJournalDay(
                content = content,
                entries = entries,
                isStarred = frontmatter.isStarred,
                label = frontmatter.label,
                revisitOn = frontmatter.revisitOn,
                revisitNote = frontmatter.revisitNote
            )
            onProgress(index + 1, total)
        }
        return snapshot
    }

    private fun mergeJournalDateContentForMigration(
        date: LocalDate,
        destinationContent: String,
        sourceContent: String
    ): String {
        val destination = parseJournalDateContent(destinationContent)
        val source = parseJournalDateContent(sourceContent)
        val mergedEntries = linkedSetOf<String>().apply {
            addAll(destination.entries)
            addAll(source.entries)
        }.toList()
        val destinationFrontmatter = destination.frontmatter
        val sourceFrontmatter = source.frontmatter
        val revisitOn = destinationFrontmatter.revisitOn ?: sourceFrontmatter.revisitOn
        val revisitNote = when {
            destinationFrontmatter.revisitOn != null -> destinationFrontmatter.revisitNote
            sourceFrontmatter.revisitOn != null -> sourceFrontmatter.revisitNote
            else -> destinationFrontmatter.revisitNote.ifBlank { sourceFrontmatter.revisitNote }
        }

        return buildJournalDateContent(
            date = date,
            entries = mergedEntries,
            frontmatter = JournalDateFrontmatterState(
                isStarred = destinationFrontmatter.isStarred || sourceFrontmatter.isStarred,
                dayLabel = destinationFrontmatter.label.ifBlank { sourceFrontmatter.label },
                revisitOn = revisitOn,
                revisitNote = revisitNote
            )
        )
    }

    private companion object {
        private const val TAG = "JournalBackupGateway"
    }
}
