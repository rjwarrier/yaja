package com.mj.yaja.data

import android.content.Context
import android.net.Uri
import android.util.Log
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import org.json.JSONObject

/**
 * Imports Journalistic app JSON exports into Yaja's markdown storage.
 *
 * Journalistic JSON structure:
 *   data.entries[]  – bullet-style entries (text + position + created_dts)
 *   data.notes[]    – longer-form notes   (title + text + created_dts)
 *   data.gems[]     – thoughts/facts      (type + text + notes + created_dts)
 *
 * All three are merged per date, sorted by position/time, then flattened
 * to individual Yaja entry lines. Existing dates are appended (never overwritten).
 */
class JournalisticImporter(
    private val context: Context,
    private val fileManager: MarkdownFileManager
) {
    data class ImportResult(
        val newDays: Int,
        val mergedDays: Int,
        val skippedEntries: Int,
        val errorDays: Int,
        val cancelled: Boolean = false
    ) {
        val totalDays: Int get() = newDays + mergedDays
    }

    private data class RawItem(
        val date: LocalDate,
        val time: String?,          // HH:mm or null
        val position: Int,
        val lines: List<String>     // already-cleaned, non-blank lines
    )

    private val TAG = "JournalisticImporter"
    @Volatile private var cancelRequested = false

    fun requestCancel() {
        cancelRequested = true
    }

    // ─── Public entry point ────────────────────────────────────────────────

    fun importFromUri(
        uri: Uri,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): ImportResult {
        val json = context.contentResolver.openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)?.readText()
            ?: throw IllegalArgumentException("Could not read file")
        return parseAndImport(json, onProgress)
    }

    // ─── Core parse + import ───────────────────────────────────────────────

    private fun parseAndImport(json: String, onProgress: ((Int, Int) -> Unit)? = null): ImportResult {
        cancelRequested = false
        val root = JSONObject(json)
        val data = root.optJSONObject("data")
            ?: throw IllegalArgumentException("Not a Journalistic export — missing 'data' key")

        val items = mutableListOf<RawItem>()
        var skipped = 0

        // 1. Bullets
        data.optJSONArray("entries")?.let { entries ->
            for (i in 0 until entries.length()) {
                val entry = entries.optJSONObject(i) ?: continue
                val dateStr = entry.optString("date").ifBlank { continue }
                val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: continue
                val bullets = entry.optJSONArray("bullets") ?: continue

                for (j in 0 until bullets.length()) {
                    val bullet = bullets.optJSONObject(j) ?: continue
                    val text = cleanText(bullet.optString("text", ""))
                    if (text.isBlank()) { skipped++; continue }

                    items.add(RawItem(
                        date = date,
                        time = parseTime(bullet.optString("created_dts", "")),
                        position = bullet.optInt("position", 0),
                        lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
                    ))
                }
            }
        }

        // 2. Notes (title + body)
        data.optJSONArray("notes")?.let { notes ->
            for (i in 0 until notes.length()) {
                val note = notes.optJSONObject(i) ?: continue
                val dateStr = note.optString("date").ifBlank { continue }
                val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: continue

                val title = cleanText(note.optString("title", ""))
                val body  = cleanText(note.optString("text", ""))
                if (body.isBlank()) { skipped++; continue }

                val fullText = if (title.isNotBlank()) "**$title**\n$body" else body

                items.add(RawItem(
                    date = date,
                    time = parseTime(note.optString("created_dts", "")),
                    position = 0,
                    lines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }
                ))
            }
        }

        // 3. Gems (type-tagged thoughts/facts)
        data.optJSONArray("gems")?.let { gems ->
            for (i in 0 until gems.length()) {
                val gem = gems.optJSONObject(i) ?: continue
                val dateStr = gem.optString("date").ifBlank { continue }
                val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: continue

                val type  = gem.optString("type", "").trim()
                val text  = cleanText(gem.optString("text", ""))
                val notes = cleanText(gem.optString("notes", ""))
                if (text.isBlank()) { skipped++; continue }

                val prefix = if (type.isNotBlank()) "[$type] " else ""
                val fullText = if (notes.isNotBlank()) "$prefix$text\n  - $notes"
                              else "$prefix$text"

                items.add(RawItem(
                    date = date,
                    time = parseTime(gem.optString("created_dts", "")),
                    position = 0,
                    lines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }
                ))
            }
        }

        // Group by date and write
        val grouped = items.groupBy { it.date }
        var newDays = 0; var mergedDays = 0; var errorDays = 0
        val totalDates = grouped.size
        var processedDates = 0

        for ((date, dayItems) in grouped.entries.sortedBy { it.key }) {
            if (cancelRequested) {
                return ImportResult(newDays, mergedDays, skipped, errorDays, cancelled = true)
            }
            processedDates++
            onProgress?.invoke(processedDates, totalDates)
            try {
                // Sort: position first, then time (mirrors Python script)
                val newLines = dayItems
                    .sortedWith(compareBy({ it.position }, { it.time ?: "" }))
                    .flatMap { it.lines }

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

        return ImportResult(newDays, mergedDays, skipped, errorDays)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private fun cleanText(text: String): String =
        text.replace("<p dir=\"auto\">", "")
            .replace("</p>", "")
            .replace(Regex("<[^>]+>"), "")
            .trim()

    /** Parses ISO-8601 UTC timestamp → "HH:mm", or null on failure. */
    private fun parseTime(iso: String): String? {
        if (iso.isBlank()) return null
        return runCatching {
            val normalised = if (iso.endsWith("Z")) iso else "${iso}Z"
            val local = Instant.parse(normalised).atZone(ZoneId.systemDefault())
            "%02d:%02d".format(local.hour, local.minute)
        }.getOrNull()
    }
}
