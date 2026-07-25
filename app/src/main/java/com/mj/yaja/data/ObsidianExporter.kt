package com.mj.yaja.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Mirrors Yaja's markdown journal into an Obsidian-friendly vault folder: one .md file
 * per day (matching Yaja's own year/month layout), Obsidian-style YAML frontmatter, and
 * People & Places keyword mentions rewritten as `[[wikilinks]]`. Read-only over the
 * journal — always exports plain, decrypted, human-readable markdown, reinforcing that
 * a Yaja journal is never locked into this app.
 */
class ObsidianExporter(
    private val context: Context,
    private val fileManager: MarkdownFileManager,
    private val keywordRepository: KeywordRepository,
    private val keywordMatchCache: KeywordMatchCache
) {
    data class ExportResult(
        val daysExported: Int,
        val cancelled: Boolean = false
    )

    private val TAG = "ObsidianExporter"
    private val monthFormatter = DateTimeFormatter.ofPattern("MM")

    @Volatile private var cancelRequested = false

    fun requestCancel() {
        cancelRequested = true
    }

    fun exportToTreeUri(
        treeUri: Uri,
        dates: Collection<LocalDate>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): ExportResult {
        cancelRequested = false
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("Could not open the selected folder")

        val sorted = dates.sorted()
        val total = sorted.size
        var exported = 0

        sorted.forEachIndexed { index, date ->
            if (cancelRequested) {
                return ExportResult(exported, cancelled = true)
            }
            onProgress?.invoke(index + 1, total)
            try {
                val entries = fileManager.getEntriesForDate(date)
                if (entries.isEmpty()) return@forEachIndexed

                val matches = keywordMatchCache.getMatchesForDate(date)
                val body = buildVaultNote(date, entries, matches)

                val dayFile = resolveDayFile(root, date) ?: return@forEachIndexed
                context.contentResolver.openOutputStream(dayFile.uri, "wt")?.use { out ->
                    out.write(body.toByteArray(Charsets.UTF_8))
                }
                exported++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export $date", e)
            }
        }

        return ExportResult(exported)
    }

    private fun resolveDayFile(root: DocumentFile, date: LocalDate): DocumentFile? {
        val year = date.year.toString()
        val month = date.format(monthFormatter)
        val dayFileName = "$date.md"

        val yearDir = root.findFile(year) ?: root.createDirectory(year) ?: return null
        val monthDir = yearDir.findFile(month) ?: yearDir.createDirectory(month) ?: return null
        return monthDir.findFile(dayFileName) ?: monthDir.createFile("text/markdown", dayFileName)
    }

    private fun buildVaultNote(
        date: LocalDate,
        entries: List<String>,
        matches: List<KeywordMatch>
    ): String {
        val tags = matches
            .mapNotNull { keywordRepository.getKeywordById(it.keywordId)?.name }
            .distinct()

        val frontmatter = buildString {
            appendLine("---")
            appendLine("date: $date")
            if (tags.isNotEmpty()) {
                appendLine("tags:")
                tags.forEach { appendLine("  - ${it.replace(" ", "-")}") }
            }
            appendLine("---")
        }

        val linkedEntries = entries.mapIndexed { entryIndex, entry ->
            applyWikilinks(entry, matches.filter { it.entryIndex == entryIndex })
        }

        return frontmatter + "\n" + linkedEntries.joinToString("\n\n")
    }

    /** Rewrites each keyword mention span as an Obsidian `[[wikilink]]`, using the
     *  keyword's canonical name rather than the raw matched alias text. */
    private fun applyWikilinks(entry: String, entryMatches: List<KeywordMatch>): String {
        val valid = entryMatches
            .filter { it.startIndex >= 0 && it.endExclusive in (it.startIndex + 1)..entry.length }
            .sortedByDescending { it.startIndex }
        if (valid.isEmpty()) return entry

        val result = StringBuilder(entry)
        var lastReplacedStart = entry.length
        for (match in valid) {
            if (match.endExclusive > lastReplacedStart) continue // overlapping match, skip
            val name = keywordRepository.getKeywordById(match.keywordId)?.name ?: continue
            result.replace(match.startIndex, match.endExclusive, "[[$name]]")
            lastReplacedStart = match.startIndex
        }
        return result.toString()
    }
}
