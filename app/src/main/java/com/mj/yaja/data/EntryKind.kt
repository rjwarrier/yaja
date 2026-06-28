package com.mj.yaja.data

enum class EntryKind {
    NORMAL,
    EVENT
}

private val entryKindRegex = Regex("""<!--type:([a-z_]+)-->""", RegexOption.IGNORE_CASE)
private val leadingNewlinesRegex = Regex("""^\n+""")
private val leadingTimeMarkerRegex = Regex("^<!--time:[^>]+-->\\n?")

fun parseEntryKind(entry: String): EntryKind {
    val rawKind = entryKindRegex.find(entry)?.groupValues?.getOrNull(1)?.lowercase()
    return when (rawKind) {
        "event" -> EntryKind.EVENT
        else -> EntryKind.NORMAL
    }
}

fun stripEntryKindMetadata(entry: String): String =
    entry.replace(entryKindRegex, "").replace(leadingNewlinesRegex, "")

fun applyEntryKindMetadata(entry: String, kind: EntryKind): String {
    val withoutKind = stripEntryKindMetadata(entry).trimStart('\n')
    if (kind == EntryKind.NORMAL) return withoutKind

    val timeMatch = leadingTimeMarkerRegex.find(withoutKind)
    return if (timeMatch != null) {
        val timePrefix = timeMatch.value.trimEnd()
        val remainder = withoutKind.removePrefix(timeMatch.value).trimStart('\n')
        "$timePrefix\n<!--type:event-->\n$remainder".trimEnd()
    } else {
        "<!--type:event-->\n$withoutKind".trimEnd()
    }
}
