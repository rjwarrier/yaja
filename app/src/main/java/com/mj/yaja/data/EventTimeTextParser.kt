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
