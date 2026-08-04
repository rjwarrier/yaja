package com.mj.yaja.data

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@androidx.compose.runtime.Immutable
data class EntryRevisitMetadata(
    val revisitOn: LocalDate? = null,
    val note: String = ""
)

private val revisitCommentRegex =
    Regex("""^<!--revisit:(\d{4}-\d{2}-\d{2})(?:\|([^>]*))?-->\n?""", RegexOption.MULTILINE)
private val revisitTimeMetadataRegex = Regex("^<!--time:[^>]+-->\\n?")

fun parseEntryRevisitMetadata(entry: String): EntryRevisitMetadata {
    val match = revisitCommentRegex.find(entry) ?: return EntryRevisitMetadata()
    val revisitOn =
        runCatching { LocalDate.parse(match.groupValues[1]) }.getOrNull() ?: return EntryRevisitMetadata()
    val encodedNote = match.groupValues.getOrNull(2).orEmpty()
    val note =
        if (encodedNote.isBlank()) {
            ""
        } else {
            runCatching { URLDecoder.decode(encodedNote, StandardCharsets.UTF_8.name()) }.getOrDefault("")
        }
    return EntryRevisitMetadata(
        revisitOn = revisitOn,
        note = note
    )
}

fun stripEntryRevisitMetadata(entry: String): String =
    entry.replace(revisitCommentRegex, "").trimStart('\n')

fun applyEntryRevisitMetadata(
    entry: String,
    revisitOn: LocalDate?,
    note: String = ""
): String {
    val withoutRevisit = stripEntryRevisitMetadata(entry)
    if (revisitOn == null) return withoutRevisit

    val trimmedNote = note.trim().take(80)
    val encodedNote =
        if (trimmedNote.isBlank()) {
            ""
        } else {
            URLEncoder.encode(trimmedNote, StandardCharsets.UTF_8.name())
        }
    val revisitComment =
        if (encodedNote.isBlank()) {
            "<!--revisit:$revisitOn-->"
        } else {
            "<!--revisit:$revisitOn|$encodedNote-->"
        }

    val timeMatch = revisitTimeMetadataRegex.find(withoutRevisit)
    return if (timeMatch != null) {
        val timePrefix = timeMatch.value.trimEnd('\n')
        val body = withoutRevisit.removePrefix(timeMatch.value).trimStart('\n')
        buildString {
            append(timePrefix)
            append('\n')
            append(revisitComment)
            if (body.isNotBlank()) {
                append('\n')
                append(body)
            }
        }
    } else {
        buildString {
            append(revisitComment)
            if (withoutRevisit.isNotBlank()) {
                append('\n')
                append(withoutRevisit)
            }
        }
    }
}
