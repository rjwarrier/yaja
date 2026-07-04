package com.mj.yaja.data

import android.content.Context
import android.net.Uri
import android.util.Log
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.util.zip.ZipInputStream
import org.json.JSONObject

/**
 * Imports Day One journal entries from a .zip or .json export file into Yaja's
 * markdown storage. Each Day One entry is mapped to its local date; if a date
 * already has entries in Yaja the imported lines are appended (merge mode).
 */
class DayOneImporter(
    private val context: Context,
    private val fileManager: MarkdownFileManager
) {
    data class ImportResult(
        val newDays: Int,      // dates with no prior Yaja entries
        val mergedDays: Int,   // dates that already had entries (appended)
        val skippedEntries: Int, // blank / image-only entries that were ignored
        val errorDays: Int,     // dates that failed to write
        val cancelled: Boolean = false
    ) {
        val totalEntries: Int get() = newDays + mergedDays
    }

    private val TAG = "DayOneImporter"
    @Volatile private var cancelRequested = false

    fun requestCancel() {
        cancelRequested = true
    }

    // ─── Public entry point ────────────────────────────────────────────────

    fun importFromUri(
        uri: Uri,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): ImportResult {
        val mime = context.contentResolver.getType(uri) ?: ""
        val path = uri.path ?: ""
        val json = when {
            mime.contains("zip", ignoreCase = true) || path.endsWith(".zip", ignoreCase = true) ->
                extractJsonFromZip(uri)
            else ->
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        } ?: throw IllegalArgumentException("Could not read file — ensure it is a Day One .zip or .json export")

        return parseAndImport(json, onProgress)
    }

    // ─── ZIP extraction ────────────────────────────────────────────────────

    private fun extractJsonFromZip(uri: Uri): String? {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    // Day One ZIP has Journal.json at the root level
                    if (!entry.isDirectory && entry.name.endsWith(".json", ignoreCase = true)) {
                        Log.d(TAG, "Reading ZIP entry: ${entry.name}")
                        return zip.bufferedReader(Charsets.UTF_8).readText()
                    }
                    entry = zip.nextEntry
                }
            }
        }
        return null
    }

    // ─── Core parse + import ───────────────────────────────────────────────

    private fun parseAndImport(json: String, onProgress: ((Int, Int) -> Unit)? = null): ImportResult {
        cancelRequested = false
        val root = JSONObject(json)
        val entries = root.getJSONArray("entries")

        // Group by local date: date -> list of (timeHHmm, listOfLines)
        val grouped = mutableMapOf<LocalDate, MutableList<Pair<String, List<String>>>>()
        var skipped = 0

        for (i in 0 until entries.length()) {
            try {
                val obj = entries.getJSONObject(i)
                val creationDate = obj.optString("creationDate").ifBlank { continue }
                val rawText = obj.optString("text", "")

                val lines = cleanAndSplit(rawText)
                if (lines.isEmpty()) { skipped++; continue }

                val instant = Instant.parse(creationDate)
                val local = instant.atZone(ZoneId.systemDefault())
                val date = local.toLocalDate()
                val time = "%02d:%02d".format(local.hour, local.minute)

                grouped.getOrPut(date) { mutableListOf() }.add(time to lines)
            } catch (e: Exception) {
                Log.w(TAG, "Skipping malformed entry at index $i: ${e.message}")
                skipped++
            }
        }

        var newDays = 0
        var mergedDays = 0
        var errorDays = 0
        val totalDates = grouped.size
        var processedDates = 0

        for ((date, items) in grouped.entries.sortedBy { it.key }) {
            if (cancelRequested) {
                return ImportResult(newDays, mergedDays, skipped, errorDays, cancelled = true)
            }
            processedDates++
            onProgress?.invoke(processedDates, totalDates)
            try {
                // Sort Day One entries within the day by time, then flatten to line list
                val newLines = items
                    .sortedBy { it.first }
                    .flatMap { (_, lines) -> lines }

                val existing = fileManager.getEntriesForDateFromDisk(date)
                if (existing.isEmpty()) {
                    fileManager.setEntriesForDate(date, newLines)
                    newDays++
                } else {
                    fileManager.setEntriesForDate(date, existing + newLines)
                    mergedDays++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write entries for $date", e)
                errorDays++
            }
        }

        return ImportResult(
            newDays = newDays,
            mergedDays = mergedDays,
            skippedEntries = skipped,
            errorDays = errorDays
        )
    }

    // ─── Text cleaning (mirrors dayone.py logic) ──────────────────────────

    /**
     * Cleans a Day One entry text and returns non-blank lines ready to be stored
     * as individual Yaja entries.
     */
    private fun cleanAndSplit(raw: String): List<String> {
        var text = raw
        // Remove embedded image links: ![](dayone-moment://XXXX)
        text = text.replace(Regex("""!\[.*?]\(dayone-moment://.*?\)"""), "")
        // Remove HTML paragraph tags
        text = text.replace("<p dir=\"auto\">", "").replace("</p>", "")
        // Remove any remaining HTML-ish tags
        text = text.replace(Regex("<[^>]+>"), "")

        return text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
