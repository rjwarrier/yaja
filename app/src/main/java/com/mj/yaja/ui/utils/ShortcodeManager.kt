package com.mj.yaja.ui.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ShortcodeManager {

    fun expand(text: String, customShortcodes: Map<String, String> = emptyMap()): String {
        var result = text

        // 1. Expand Custom (and newly migrated built-in) Shortcodes
        customShortcodes.forEach { (code, value) ->
            if (result.contains(code)) {
                result = result.replace(code, value)
            }
        }

        // 2. Expand {{today:FORMAT}} / {{now:FORMAT}} placeholders anywhere in the text
        result = expandPlaceholders(result)

        // 3. Handle @x (Todo Toggle Logic) - Moved from AddEntryScreen
        if ("@x" in result) {
            result = handleTodoToggle(result)
        }

        return result
    }

    private fun expandPlaceholders(value: String): String {
        var expanded = value
        val now = LocalDate.now()
        val timeNow = LocalTime.now()

        // Regex for {{today:FORMAT}}, {{yesterday:FORMAT}}, {{tomorrow:FORMAT}}, {{now:FORMAT}}
        val placeholderRegex = Regex("\\{\\{(today|yesterday|tomorrow|now):(.+?)\\}\\}")

        return placeholderRegex.replace(expanded) { matchResult ->
            val type = matchResult.groupValues[1]
            val format = matchResult.groupValues[2]

            try {
                val formatter = DateTimeFormatter.ofPattern(format)
                when (type) {
                    "today" -> now.format(formatter)
                    "yesterday" -> now.minusDays(1).format(formatter)
                    "tomorrow" -> now.plusDays(1).format(formatter)
                    "now" -> timeNow.format(formatter)
                    else -> matchResult.value
                }
            } catch (e: Exception) {
                matchResult.value // Return original if format is invalid
            }
        }
    }

    private fun handleTodoToggle(text: String): String {
        val idx = text.indexOf("@x")
        val startMatch = text.lastIndexOf('\n', idx - 1)
        val lineStart = if (startMatch == -1) 0 else startMatch + 1

        val lineEndMatch = text.indexOf('\n', idx)
        val lineEnd = if (lineEndMatch == -1) text.length else lineEndMatch

        val line = text.substring(lineStart, lineEnd)
        val trimmedLine = line.trimStart()

        return if (trimmedLine.startsWith("[ ]")) {
            val bracketIdx = line.indexOf("[ ]")
            val newLine =
                    line.substring(0, bracketIdx) +
                            "[x]" +
                            line.substring(bracketIdx + 3).replace("@x", "")
            text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        } else if (trimmedLine.startsWith("[x]")) {
            val bracketIdx = line.indexOf("[x]")
            val newLine =
                    line.substring(0, bracketIdx) +
                            "[ ]" +
                            line.substring(bracketIdx + 3).replace("@x", "")
            text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        } else {
            text.replace("@x", "[x]")
        }
    }
}
