package com.mj.yaja.data

import java.security.MessageDigest

data class ParsedTodoLine(
    val prefix: String,
    val displayText: String,
    val isChecked: Boolean,
    val lineHash: String,
    val complianceId: String? = null
)

object TodoParser {
    private val todoRegex = Regex("""^(\s*(?:[+*\-]\s+)?)\[( |x|X)\](\s+.*)$""")
    private val complianceCommentRegex = Regex("""\s*<!--compliance:([a-zA-Z0-9-]+)-->\s*$""")

    fun parseLine(line: String): ParsedTodoLine? {
        val match = todoRegex.find(line) ?: return null
        val prefix = match.groupValues[1]
        var rawText = match.groupValues[3].trim()

        var complianceId: String? = null
        val commentMatch = complianceCommentRegex.find(rawText)
        if (commentMatch != null) {
            complianceId = commentMatch.groupValues[1]
            rawText = rawText.replace(complianceCommentRegex, "").trim()
        }

        return ParsedTodoLine(
            prefix = prefix,
            displayText = rawText,
            isChecked = match.groupValues[2].equals("x", ignoreCase = true),
            lineHash = stableHash(prefix, rawText),
            complianceId = complianceId
        )
    }

    fun toggleLine(line: String, expectedLineHash: String? = null): String? {
        val parsed = parseLine(line) ?: return null
        if (!expectedLineHash.isNullOrBlank() && parsed.lineHash != expectedLineHash) return null
        val nextMarker = if (parsed.isChecked) " " else "x"
        val suffix = if (parsed.complianceId != null) " <!--compliance:${parsed.complianceId}-->" else ""
        return parsed.prefix + "[$nextMarker] " + parsed.displayText + suffix
    }

    fun stableHash(prefix: String, displayText: String): String {
        val normalized = prefix.trim() + "\u0000" + displayText.trim().lowercase()
        val bytes = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}
