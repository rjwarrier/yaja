package com.mj.yaja.data

import java.time.LocalDate

internal data class JournalDateFrontmatterState(
    val isStarred: Boolean = false,
    val dayLabel: String = "",
    val revisitOn: LocalDate? = null,
    val revisitNote: String = ""
)

internal data class ParsedJournalDateContent(
    val entries: List<String>,
    val frontmatter: MarkdownFileManager.Companion.FrontmatterSnapshot
)

internal fun captureJournalDateFrontmatterState(
    date: LocalDate,
    starredDates: Map<LocalDate, String>,
    dayLabels: Map<LocalDate, String>,
    revisitDates: Map<LocalDate, LocalDate>,
    revisitNotes: Map<LocalDate, String>
): JournalDateFrontmatterState =
    JournalDateFrontmatterState(
        isStarred = starredDates.containsKey(date),
        dayLabel = dayLabels[date].orEmpty(),
        revisitOn = revisitDates[date],
        revisitNote = revisitNotes[date].orEmpty()
    )

internal fun buildJournalDateContent(
    date: LocalDate,
    entries: List<String>,
    frontmatter: JournalDateFrontmatterState
): String {
    val sb = StringBuilder()

    if (frontmatter.isStarred || frontmatter.dayLabel.isNotEmpty() || frontmatter.revisitOn != null) {
        sb.append("---\n")
        if (frontmatter.isStarred) sb.append("starred: true\n")
        if (frontmatter.dayLabel.isNotEmpty()) {
            sb.append("label: \"${frontmatter.dayLabel}\"\n")
        }
        frontmatter.revisitOn?.let { sb.append("revisit_on: \"$it\"\n") }
        if (frontmatter.revisitOn != null && frontmatter.revisitNote.isNotEmpty()) {
            sb.append("revisit_note: \"${frontmatter.revisitNote.replace("\"", "\\\"")}\"\n")
        }
        sb.append("---\n")
    }

    sb.append("# $date\n\n")
    entries.forEach { entry ->
        val entryLines = entry.lines()
        entryLines.forEachIndexed { index, line ->
            when (index) {
                0 -> sb.append("- ").append(line).append("\n")
                else -> {
                    if (line.isBlank()) {
                        sb.append("\n")
                    } else {
                        sb.append("  ").append(line).append("\n")
                    }
                }
            }
        }
    }
    return sb.toString()
}

internal fun parseJournalEntriesFromContent(content: String): List<String> =
    MarkdownFileManager.parseEntries(content.split("\n"))

internal fun parseJournalDateContent(content: String): ParsedJournalDateContent {
    val lines = content.lines()
    return ParsedJournalDateContent(
        entries = MarkdownFileManager.parseEntries(lines),
        frontmatter = MarkdownFileManager.parseFrontmatter(lines)
    )
}

internal fun applyFrontmatterToMemoryState(
    date: LocalDate,
    frontmatter: MarkdownFileManager.Companion.FrontmatterSnapshot,
    starredDates: MutableMap<LocalDate, String>,
    dayLabels: MutableMap<LocalDate, String>,
    revisitDates: MutableMap<LocalDate, LocalDate>,
    revisitNotes: MutableMap<LocalDate, String>
) {
    if (frontmatter.isStarred) {
        starredDates[date] = frontmatter.label
    } else {
        starredDates.remove(date)
    }

    if (frontmatter.label.isNotEmpty()) {
        dayLabels[date] = frontmatter.label
    } else {
        dayLabels.remove(date)
    }

    frontmatter.revisitOn?.let { revisitDates[date] = it } ?: revisitDates.remove(date)
    if (frontmatter.revisitOn != null && frontmatter.revisitNote.isNotEmpty()) {
        revisitNotes[date] = frontmatter.revisitNote
    } else {
        revisitNotes.remove(date)
    }
}

internal fun applyParsedJournalDateToMemoryState(
    date: LocalDate,
    parsed: ParsedJournalDateContent,
    cache: MutableMap<LocalDate, List<String>>,
    entryCountCache: MutableMap<LocalDate, Int>,
    wordCountCache: MutableMap<LocalDate, Int>,
    starredDates: MutableMap<LocalDate, String>,
    dayLabels: MutableMap<LocalDate, String>,
    revisitDates: MutableMap<LocalDate, LocalDate>,
    revisitNotes: MutableMap<LocalDate, String>,
    countWords: (List<String>) -> Int,
    updateCachedDatePresence: (LocalDate, Boolean) -> Unit
) {
    cache[date] = parsed.entries
    entryCountCache[date] = parsed.entries.size
    wordCountCache[date] = countWords(parsed.entries)
    updateCachedDatePresence(date, parsed.entries.isNotEmpty())

    applyFrontmatterToMemoryState(
        date = date,
        frontmatter = parsed.frontmatter,
        starredDates = starredDates,
        dayLabels = dayLabels,
        revisitDates = revisitDates,
        revisitNotes = revisitNotes
    )
}

internal fun finalizeAppliedJournalDateState(
    date: LocalDate,
    entries: List<String>,
    saveCacheToDisk: () -> Unit,
    saveEntryCountCacheToDisk: () -> Unit,
    saveWordCountCacheToDisk: () -> Unit,
    saveLabelsCacheToDisk: () -> Unit,
    syncTodoIndexForDate: (LocalDate, List<String>) -> Unit,
    scheduleFingerprintRefresh: () -> Unit,
    scheduleEntryMutationRefresh: (LocalDate, List<String>) -> Unit
) {
    saveCacheToDisk()
    saveEntryCountCacheToDisk()
    saveWordCountCacheToDisk()
    saveLabelsCacheToDisk()
    syncTodoIndexForDate(date, entries)
    scheduleFingerprintRefresh()
    scheduleEntryMutationRefresh(date, entries)
}
