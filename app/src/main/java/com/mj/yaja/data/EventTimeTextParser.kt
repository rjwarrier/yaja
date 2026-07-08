package com.mj.yaja.data

private val meridiemTimeRegex =
    Regex("""\b(\d{1,2})([:.](\d{2}))?\s*(AM|PM|am|pm)\b""")
private val twentyFourHourTimeRegex =
    Regex("""\b([01]?\d|2[0-3])[:.]([0-5]\d)(?:\s*(?:Hrs|hrs|HR|hr|Hr))?\b""")

fun extractMentionedEventTime(text: String): String? {
    val meridiemMatch = meridiemTimeRegex.find(text)
    val twentyFourHourMatch = twentyFourHourTimeRegex.find(text)
    val chosen = listOfNotNull(meridiemMatch, twentyFourHourMatch).minByOrNull { it.range.first } ?: return null

    return when (chosen) {
        meridiemMatch -> {
            val hour = chosen.groupValues[1]
            val minutePart = chosen.groupValues[2].replace(':', '.')
            val suffix = chosen.groupValues[4].uppercase()
            if (minutePart.isBlank()) "$hour $suffix" else "$hour$minutePart $suffix"
        }
        else -> {
            val hour = chosen.groupValues[1].padStart(2, '0')
            val minute = chosen.groupValues[2]
            "$hour.$minute Hrs"
        }
    }
}

fun eventTextStartsWithTime(text: String, candidate: String?): Boolean {
    if (candidate.isNullOrBlank()) return false
    val leadingTime = extractLeadingEventTime(text) ?: return false
    return normalizeEventTimeToken(leadingTime) == normalizeEventTimeToken(candidate)
}

fun stripMentionedEventTimeFromText(text: String, candidate: String?): String {
    if (candidate.isNullOrBlank()) return text.trim()
    val normalizedCandidate = normalizeEventTimeToken(candidate)
    return text
        .lineSequence()
        .map { line ->
            val withoutTime = eventTimeTokenRegex.replace(line) { match ->
                if (normalizeEventTimeToken(match.value) == normalizedCandidate) "" else match.value
            }
            withoutTime
                .replace(Regex("""^\s*[-:|,]+\s*"""), "")
                .replace(Regex("""\s*[:|,]\s*$"""), "")
                .replace(Regex("""\s{2,}"""), " ")
                .trim()
        }
        .joinToString("\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

private fun extractLeadingEventTime(text: String): String? {
    val trimmed = text.trimStart()
    val meridiemMatch = meridiemTimeRegex.find(trimmed)?.takeIf { it.range.first == 0 }
    val twentyFourHourMatch = twentyFourHourTimeRegex.find(trimmed)?.takeIf { it.range.first == 0 }
    val chosen = listOfNotNull(meridiemMatch, twentyFourHourMatch).minByOrNull { it.range.first } ?: return null

    return when (chosen) {
        meridiemMatch -> {
            val hour = chosen.groupValues[1]
            val minutePart = chosen.groupValues[2].replace(':', '.')
            val suffix = chosen.groupValues[4].uppercase()
            if (minutePart.isBlank()) "$hour $suffix" else "$hour$minutePart $suffix"
        }
        else -> {
            val hour = chosen.groupValues[1].padStart(2, '0')
            val minute = chosen.groupValues[2]
            "$hour.$minute Hrs"
        }
    }
}

private val eventTimeTokenRegex =
    Regex("""\b\d{1,2}(?:[:.]\d{2})?\s*(?:AM|PM|am|pm)\b|\b(?:[01]?\d|2[0-3])[:.][0-5]\d(?:\s*(?:Hrs|hrs|HR|hr|Hr))?\b""")

private fun normalizeEventTimeToken(value: String): String {
    val trimmed = value.trim()
    meridiemTimeRegex.matchEntire(trimmed)?.let { match ->
        val hour = match.groupValues[1].padStart(2, '0')
        val minute = match.groupValues[3].ifBlank { "00" }
        val suffix = match.groupValues[4].uppercase()
        return "$hour:$minute $suffix"
    }
    twentyFourHourTimeRegex.matchEntire(trimmed)?.let { match ->
        val hour = match.groupValues[1].padStart(2, '0')
        val minute = match.groupValues[2]
        return "$hour:$minute"
    }
    return trimmed.lowercase()
}
