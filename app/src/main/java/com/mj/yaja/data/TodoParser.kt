package com.mj.yaja.data

import java.security.MessageDigest

data class ParsedTodoLine(
    val prefix: String,
    val displayText: String,
    val isChecked: Boolean,
    val lineHash: String
)

object TodoParser {
    private val todoRegex = Regex("""^(\s*(?:[+*\-]\s+)?)\[( |x|X)\](\s+.*)$""")

    fun parseLine(line: String): ParsedTodoLine? {
        val match = todoRegex.find(line) ?: return null
        val prefix = match.groupValues[1]
        val displayText = match.groupValues[3].trim()
        return ParsedTodoLine(
            prefix = prefix,
            displayText = displayText,
            isChecked = match.groupValues[2].equals("x", ignoreCase = true),
            lineHash = stableHash(prefix, displayText)
        )
    }

    fun toggleLine(line: String, expectedLineHash: String? = null): String? {
        val parsed = parseLine(line) ?: return null
        if (!expectedLineHash.isNullOrBlank() && parsed.lineHash != expectedLineHash) return null
        val nextMarker = if (parsed.isChecked) " " else "x"
        return parsed.prefix + "[$nextMarker] " + parsed.displayText
    }

    fun stableHash(prefix: String, displayText: String): String {
        val normalized = prefix.trim() + "\u0000" + displayText.trim().lowercase()
        val bytes = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}
