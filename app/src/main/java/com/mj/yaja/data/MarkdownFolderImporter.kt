package com.mj.yaja.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.time.LocalDate

/**
 * Imports a folder of plain .md files (Obsidian vault, Jekyll posts, any daily-notes
 * export) into Yaja's markdown storage. Each file is mapped to a date resolved from
 * its filename or a `date:` YAML frontmatter field; if that date already has entries
 * in Yaja the imported file is appended (merge mode).
 */
class MarkdownFolderImporter(
    private val context: Context,
    private val fileManager: MarkdownFileManager
) {
    data class ImportResult(
        val newDays: Int,       // dates with no prior Yaja entries
        val mergedDays: Int,    // dates that already had entries (appended)
        val skippedEntries: Int, // files with no resolvable date or blank content
        val errorDays: Int,     // dates that failed to write
        val cancelled: Boolean = false
    )

    private val TAG = "MarkdownFolderImporter"
    @Volatile private var cancelRequested = false

    fun requestCancel() {
        cancelRequested = true
    }

    fun importFromTreeUri(
        treeUri: Uri,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): ImportResult {
        cancelRequested = false
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalArgumentException("Could not open the selected folder")

        val files = mutableListOf<DocumentFile>()
        collectMarkdownFiles(root, files)

        var newDays = 0
        var mergedDays = 0
        var skipped = 0
        var errorDays = 0
        val total = files.size

        files.forEachIndexed { index, file ->
            if (cancelRequested) {
                return ImportResult(newDays, mergedDays, skipped, errorDays, cancelled = true)
            }
            onProgress?.invoke(index + 1, total)
            try {
                val raw = context.contentResolver.openInputStream(file.uri)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (raw == null) {
                    skipped++
                    return@forEachIndexed
                }

                val date = extractDate(file.name.orEmpty(), raw)
                if (date == null) {
                    skipped++
                    return@forEachIndexed
                }

                val body = stripFrontmatter(raw).trim()
                if (body.isBlank()) {
                    skipped++
                    return@forEachIndexed
                }

                val existing = fileManager.getEntriesForDateFromDisk(date)
                if (existing.isEmpty()) {
                    fileManager.setEntriesForDate(date, listOf(body))
                    newDays++
                } else {
                    fileManager.setEntriesForDate(date, existing + body)
                    mergedDays++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import ${file.name}", e)
                errorDays++
            }
        }

        return ImportResult(newDays, mergedDays, skipped, errorDays)
    }

    private fun collectMarkdownFiles(dir: DocumentFile, out: MutableList<DocumentFile>) {
        dir.listFiles().forEach { child ->
            if (child.isDirectory) {
                collectMarkdownFiles(child, out)
            } else if (child.name?.endsWith(".md", ignoreCase = true) == true) {
                out += child
            }
        }
    }

    // ─── Date resolution ───────────────────────────────────────────────────
    // Tries, in order: exact ISO filename ("2024-03-15.md"), an ISO date embedded
    // anywhere in the filename ("2024-03-15-title.md"), then a `date:` YAML
    // frontmatter field. Files that match none of these are skipped.

    private val isoDateInName = Regex("""(\d{4}-\d{2}-\d{2})""")
    private val yamlDateField = Regex("""^date:\s*"?'?(\d{4}-\d{2}-\d{2})""", RegexOption.MULTILINE)

    private fun extractDate(fileName: String, content: String): LocalDate? {
        val nameWithoutExt = fileName.removeSuffix(".md").removeSuffix(".MD").removeSuffix(".Md")

        runCatching { LocalDate.parse(nameWithoutExt) }.getOrNull()?.let { return it }

        isoDateInName.find(nameWithoutExt)?.groupValues?.get(1)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { return it }

        frontmatterBlock(content)?.let { frontmatter ->
            yamlDateField.find(frontmatter)?.groupValues?.get(1)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.let { return it }
        }

        return null
    }

    private fun frontmatterBlock(content: String): String? {
        if (!content.startsWith("---")) return null
        val end = content.indexOf("\n---", 3)
        return if (end < 0) null else content.substring(0, end)
    }

    private fun stripFrontmatter(content: String): String {
        if (!content.startsWith("---")) return content
        val end = content.indexOf("\n---", 3)
        if (end < 0) return content
        val afterMarker = content.indexOf('\n', end + 1)
        return if (afterMarker >= 0) content.substring(afterMarker + 1) else content
    }
}
