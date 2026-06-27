package com.mj.yaja.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mj.yaja.data.DayOneImporter
import com.mj.yaja.data.JournalisticImporter
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.backup.BackupService
import com.mj.yaja.data.keywords.KeywordCsvCodec
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class EntryImportSummary(
    val newDays: Int,
    val mergedDays: Int,
    val skippedEntries: Int,
    val cancelled: Boolean
)

internal data class RestoreProcessingResult(
    val newDays: Int,
    val mergedDays: Int,
    val skippedEntries: Int,
    val shortcodesAdded: Int,
    val shortcodesSkipped: Int,
    val dateKeywordsAdded: Int,
    val dateKeywordsSkipped: Int,
    val keywordsAdded: Int,
    val keywordsSkipped: Int
)

internal fun canStartImport(importState: JournalViewModel.ImportState): Boolean =
    importState !is JournalViewModel.ImportState.Running

internal fun buildImportSuccessState(summary: EntryImportSummary): JournalViewModel.ImportState.Success =
    JournalViewModel.ImportState.Success(
        newDays = summary.newDays,
        mergedDays = summary.mergedDays,
        skippedEntries = summary.skippedEntries
    )

internal fun buildRestoreImportSuccessState(
    processed: RestoreProcessingResult
): JournalViewModel.ImportState.Success =
    JournalViewModel.ImportState.Success(
        newDays = processed.newDays,
        mergedDays = processed.mergedDays,
        skippedEntries = processed.skippedEntries
    )

internal fun buildRestoreSummary(
    processed: RestoreProcessingResult
): JournalViewModel.RestoreSummary =
    JournalViewModel.RestoreSummary(
        newDays = processed.newDays,
        mergedDays = processed.mergedDays,
        skippedJournalEntries = processed.skippedEntries,
        shortcodesAdded = processed.shortcodesAdded,
        shortcodesSkipped = processed.shortcodesSkipped,
        dateKeywordsAdded = processed.dateKeywordsAdded,
        dateKeywordsSkipped = processed.dateKeywordsSkipped,
        peoplePlacesAdded = processed.keywordsAdded,
        peoplePlacesSkipped = processed.keywordsSkipped
    )

internal fun buildRestoreToastMessage(processed: RestoreProcessingResult): String =
    "Restore finished. ${processed.newDays} new days, ${processed.mergedDays} merged, " +
        "${processed.shortcodesAdded} shortcode(s), ${processed.dateKeywordsAdded} date keyword(s), " +
        "${processed.keywordsAdded} People & Places keyword(s) added."

internal fun createBackupShareIntent(uri: Uri): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

internal suspend fun runBackupShareWorkflow(
    createBackupZip: suspend () -> Uri?,
    markBackupCreated: (Long) -> Unit,
    launchShare: (Intent) -> Unit,
    emitToast: suspend (String) -> Unit
) {
    try {
        emitToast("Preparing backup...")
        val uri = createBackupZip()
        if (uri != null) {
            markBackupCreated(System.currentTimeMillis())
            val chooser = Intent.createChooser(createBackupShareIntent(uri), "Share Backup").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            launchShare(chooser)
        } else {
            emitToast("Backup couldn't be created.")
        }
    } catch (e: Exception) {
        emitToast(e.message ?: "Backup couldn't be created.")
    }
}

internal suspend fun runRestoreBackupWorkflow(
    bundle: BackupService.BackupBundle,
    processRestoreBundle: (BackupService.BackupBundle, (Int, Int) -> Unit) -> RestoreProcessingResult,
    publishRunningState: (Int, Int) -> Unit,
    clearLookbackCache: () -> Unit,
    reloadSelectedDate: () -> Unit,
    refreshCalendarDates: () -> Unit,
    refreshStarredLabels: () -> Unit,
    rebuildTodoIndex: () -> Unit,
    startIncrementalWarmup: () -> Unit,
    markBackgroundRefreshComplete: () -> Unit,
    importState: MutableStateFlow<JournalViewModel.ImportState>,
    restoreSummary: MutableStateFlow<JournalViewModel.RestoreSummary?>,
    toastEvents: MutableSharedFlow<String>
) {
    importState.value = JournalViewModel.ImportState.Running(0, 1)
    val restoreResult = processRestoreBundle(bundle, publishRunningState)

    clearLookbackCache()
    reloadSelectedDate()
    refreshCalendarDates()
    refreshStarredLabels()
    rebuildTodoIndex()
    startIncrementalWarmup()
    markBackgroundRefreshComplete()

    importState.value = buildRestoreImportSuccessState(restoreResult)
    restoreSummary.value = buildRestoreSummary(restoreResult)
    toastEvents.emit(buildRestoreToastMessage(restoreResult))
}

internal fun launchManagedEntryImport(
    scope: CoroutineScope,
    importState: MutableStateFlow<JournalViewModel.ImportState>,
    errorMessage: String = "Unknown error",
    onSetup: () -> Unit = {},
    onTeardown: () -> Unit = {},
    onSuccess: (EntryImportSummary) -> Unit = {},
    importBlock: suspend (progress: (Int, Int) -> Unit) -> EntryImportSummary
): Job =
    scope.launch {
        importState.value = JournalViewModel.ImportState.Running()
        try {
            onSetup()
            val result = importBlock { current, total ->
                importState.value = JournalViewModel.ImportState.Running(current, total)
            }

            if (result.cancelled) {
                importState.value = JournalViewModel.ImportState.Idle
            } else {
                onSuccess(result)
                importState.value = buildImportSuccessState(result)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            importState.value = JournalViewModel.ImportState.Error(e.message ?: errorMessage)
        } finally {
            onTeardown()
            if (importState.value is JournalViewModel.ImportState.Running) {
                importState.value = JournalViewModel.ImportState.Idle
            }
        }
    }

internal fun launchDayOneEntryImport(
    scope: CoroutineScope,
    importState: MutableStateFlow<JournalViewModel.ImportState>,
    context: Context,
    uri: Uri,
    fileManager: com.mj.yaja.data.MarkdownFileManager,
    onImporterChanged: (DayOneImporter?) -> Unit,
    onImportSuccess: (EntryImportSummary) -> Unit
): Job {
    var importer: DayOneImporter? = null
    return launchManagedEntryImport(
        scope = scope,
        importState = importState,
        onSetup = {
            importer = DayOneImporter(context, fileManager)
            onImporterChanged(importer)
        },
        onTeardown = {
            onImporterChanged(null)
            importer = null
        },
        onSuccess = onImportSuccess,
        importBlock = { progress ->
            importer!!.importFromUri(uri, progress).toEntryImportSummary()
        }
    )
}

internal fun launchJournalisticEntryImport(
    scope: CoroutineScope,
    importState: MutableStateFlow<JournalViewModel.ImportState>,
    context: Context,
    uri: Uri,
    fileManager: com.mj.yaja.data.MarkdownFileManager,
    onImporterChanged: (JournalisticImporter?) -> Unit,
    onImportSuccess: (EntryImportSummary) -> Unit
): Job {
    var importer: JournalisticImporter? = null
    return launchManagedEntryImport(
        scope = scope,
        importState = importState,
        onSetup = {
            importer = JournalisticImporter(context, fileManager)
            onImporterChanged(importer)
        },
        onTeardown = {
            onImporterChanged(null)
            importer = null
        },
        onSuccess = onImportSuccess,
        importBlock = { progress ->
            importer!!.importFromUri(uri, progress).toEntryImportSummary()
        }
    )
}

internal fun launchKeywordBackupImport(
    scope: CoroutineScope,
    importState: MutableStateFlow<JournalViewModel.ImportState>,
    context: Context,
    uri: Uri,
    onImportKeywords: suspend (List<KeywordDefinition>) -> Unit
): Job =
    scope.launch(Dispatchers.IO) {
        importState.value = JournalViewModel.ImportState.Running(0, 1)
        try {
            val importedKeywords = parseKeywordsFromBackupZip(context, uri)

            if (importedKeywords.isEmpty()) {
                importState.value = JournalViewModel.ImportState.Error("No keywords.csv found in backup ZIP")
            } else {
                withContext(Dispatchers.Main) {
                    onImportKeywords(importedKeywords)
                }
                importState.value = buildImportSuccessState(
                    EntryImportSummary(
                        newDays = importedKeywords.size,
                        mergedDays = 0,
                        skippedEntries = 0,
                        cancelled = false
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            importState.value = JournalViewModel.ImportState.Error(
                e.message ?: "Failed to import backup ZIP"
            )
        } finally {
            if (importState.value is JournalViewModel.ImportState.Running) {
                importState.value = JournalViewModel.ImportState.Idle
            }
        }
    }

internal suspend fun runStorageMigrationWorkflow(
    oldUri: String?,
    newUri: String?,
    setLoading: (Boolean) -> Unit,
    migrateEntries: suspend (String?, String?) -> Unit,
    clearLookbackCache: () -> Unit,
    reloadSelectedDate: () -> Unit,
    refreshCalendarDates: () -> Unit,
    refreshStarredLabels: () -> Unit,
    rebuildTodoIndex: () -> Unit,
    loadAllJournalDates: suspend () -> Collection<LocalDate>,
    startIncrementalWarmup: () -> Unit,
    runDeferredStartupWork: (dateCount: Int) -> Unit,
    persistJournalFingerprint: suspend () -> Unit,
    markBackgroundRefreshComplete: (Long) -> Unit,
    emitToast: suspend (String) -> Unit
) {
    setLoading(true)
    try {
        migrateEntries(oldUri, newUri)

        clearLookbackCache()
        reloadSelectedDate()
        refreshCalendarDates()
        refreshStarredLabels()
        rebuildTodoIndex()

        val dates = loadAllJournalDates()
        startIncrementalWarmup()
        runDeferredStartupWork(dates.size)
        persistJournalFingerprint()
        markBackgroundRefreshComplete(System.currentTimeMillis())
        emitToast("Storage location updated safely.")
    } finally {
        setLoading(false)
    }
}

internal fun runEntryImportSuccessRefresh(
    clearLookbackCache: () -> Unit,
    forceFileRefresh: () -> Unit,
    markBackgroundRefreshComplete: (Long) -> Unit
) {
    clearLookbackCache()
    forceFileRefresh()
    markBackgroundRefreshComplete(System.currentTimeMillis())
}

internal fun cancelActiveImport(
    importJob: Job?,
    dayOneImporter: DayOneImporter?,
    journalisticImporter: JournalisticImporter?
) {
    importJob?.cancel()
    dayOneImporter?.requestCancel()
    journalisticImporter?.requestCancel()
}

internal fun DayOneImporter.ImportResult.toEntryImportSummary(): EntryImportSummary =
    EntryImportSummary(
        newDays = newDays,
        mergedDays = mergedDays,
        skippedEntries = skippedEntries,
        cancelled = cancelled
    )

internal fun JournalisticImporter.ImportResult.toEntryImportSummary(): EntryImportSummary =
    EntryImportSummary(
        newDays = newDays,
        mergedDays = mergedDays,
        skippedEntries = skippedEntries,
        cancelled = cancelled
    )

internal fun parseKeywordsFromBackupZip(
    context: Context,
    uri: Uri
): List<KeywordDefinition> {
    val importedKeywords = mutableListOf<KeywordDefinition>()
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.equals("keywords.csv", ignoreCase = true)) {
                    BufferedReader(InputStreamReader(zip, Charsets.UTF_8)).use { reader ->
                        reader.lineSequence()
                            .dropWhile { it.isBlank() }
                            .forEachIndexed { index, line ->
                                if (index == 0 && KeywordCsvCodec.isHeader(line)) return@forEachIndexed
                                KeywordCsvCodec.parseLine(line)?.let(importedKeywords::add)
                            }
                    }
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    return importedKeywords
}

internal fun processRestoreBundle(
    bundle: BackupService.BackupBundle,
    getEntriesForDate: (LocalDate) -> List<String>,
    setEntriesForDate: (LocalDate, List<String>) -> Unit,
    getDayLabel: (LocalDate) -> String,
    setDayLabel: (LocalDate, String) -> Unit,
    isDateStarred: (LocalDate) -> Boolean,
    setStarred: (LocalDate, Boolean, String) -> Unit,
    mergeShortcodes: (Map<String, String>) -> Int,
    mergeDateKeywords: (List<com.mj.yaja.data.DateKeywordEntry>) -> Int,
    importKeywordsIgnoringDuplicates: (List<KeywordDefinition>) -> Int,
    publishProgress: (current: Int, total: Int) -> Unit
): RestoreProcessingResult {
    val totalSteps =
        bundle.journalDays.size +
            1 +
            1 +
            1
    var completedSteps = 0

    var newDays = 0
    var mergedDays = 0
    var skippedEntries = 0

    bundle.journalDays.toSortedMap().forEach { (date, day) ->
        val existingEntries = getEntriesForDate(date)
        val mergedEntries = linkedSetOf<String>().apply { addAll(existingEntries) }
        var dayAddedCount = 0
        day.entries.forEach { entry ->
            if (mergedEntries.add(entry)) {
                dayAddedCount += 1
            } else {
                skippedEntries += 1
            }
        }
        if (dayAddedCount > 0) {
            setEntriesForDate(date, mergedEntries.toList())
            if (existingEntries.isEmpty()) {
                newDays += 1
            } else {
                mergedDays += 1
            }
        }

        val currentLabel = getDayLabel(date)
        val labelToApply = currentLabel.ifBlank { day.label }
        if (currentLabel.isBlank() && day.label.isNotBlank()) {
            setDayLabel(date, day.label)
        }
        if (day.isStarred && !isDateStarred(date)) {
            setStarred(date, true, labelToApply)
        }

        completedSteps += 1
        publishProgress(completedSteps, totalSteps.coerceAtLeast(1))
    }

    val shortcodesAdded = mergeShortcodes(bundle.shortcodes)
    val shortcodesSkipped = (bundle.shortcodes.size - shortcodesAdded).coerceAtLeast(0)
    completedSteps += 1
    publishProgress(completedSteps, totalSteps.coerceAtLeast(1))

    val dateKeywordsAdded = mergeDateKeywords(bundle.dateKeywords)
    val dateKeywordsSkipped = (bundle.dateKeywords.size - dateKeywordsAdded).coerceAtLeast(0)
    completedSteps += 1
    publishProgress(completedSteps, totalSteps.coerceAtLeast(1))

    val keywordsAdded = importKeywordsIgnoringDuplicates(bundle.keywords)
    val keywordsSkipped = (bundle.keywords.size - keywordsAdded).coerceAtLeast(0)
    completedSteps += 1
    publishProgress(completedSteps, totalSteps.coerceAtLeast(1))

    return RestoreProcessingResult(
        newDays = newDays,
        mergedDays = mergedDays,
        skippedEntries = skippedEntries,
        shortcodesAdded = shortcodesAdded,
        shortcodesSkipped = shortcodesSkipped,
        dateKeywordsAdded = dateKeywordsAdded,
        dateKeywordsSkipped = dateKeywordsSkipped,
        keywordsAdded = keywordsAdded,
        keywordsSkipped = keywordsSkipped
    )
}

internal fun mergeImportedShortcodes(
    current: Map<String, String>,
    incoming: Map<String, String>,
    persist: (Map<String, String>) -> Unit
) {
    val merged = current.toMutableMap()
    incoming.forEach { (code, value) ->
        if (!merged.containsKey(code)) {
            merged[code] = value
        }
    }
    persist(merged)
}

internal fun mergeImportedShortcodesCountingAdded(
    current: Map<String, String>,
    incoming: Map<String, String>,
    persist: (Map<String, String>) -> Unit
): Int {
    val merged = current.toMutableMap()
    var added = 0
    incoming.forEach { (code, value) ->
        if (!merged.containsKey(code)) {
            merged[code] = value
            added += 1
        }
    }
    persist(merged)
    return added
}

internal fun mergeImportedDateKeywordsCountingAdded(
    current: List<com.mj.yaja.data.DateKeywordEntry>,
    incoming: List<com.mj.yaja.data.DateKeywordEntry>,
    persist: (List<com.mj.yaja.data.DateKeywordEntry>) -> Unit
): Int {
    if (incoming.isEmpty()) return 0
    val seenKeywords = current.map { it.keyword.trim().lowercase() }.toMutableSet()
    val merged = current.toMutableList()
    var added = 0
    incoming.forEach { entry ->
        val normalizedKeyword = entry.keyword.trim().lowercase()
        val normalizedMeaning = entry.meaning.trim()
        if (normalizedKeyword.isNotBlank() && normalizedMeaning.isNotBlank() && seenKeywords.add(normalizedKeyword)) {
            merged += com.mj.yaja.data.DateKeywordEntry(entry.keyword.trim(), normalizedMeaning)
            added += 1
        }
    }
    persist(merged)
    return added
}

internal fun mergeImportedKeywordsIgnoringDuplicates(
    existing: List<KeywordDefinition>,
    incoming: List<KeywordDefinition>,
    importKeywords: (List<KeywordDefinition>) -> Unit
): Int {
    if (incoming.isEmpty()) return 0
    val existingKeys = existing.map {
        "${it.name.trim().lowercase()}|${it.type.name}"
    }.toMutableSet()
    val filtered = incoming.filter { keyword ->
        existingKeys.add("${keyword.name.trim().lowercase()}|${keyword.type.name}")
    }
    if (filtered.isNotEmpty()) {
        importKeywords(filtered)
    }
    return filtered.size
}
