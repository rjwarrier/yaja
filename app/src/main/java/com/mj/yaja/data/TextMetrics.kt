package com.mj.yaja.data

private val checklistMarkerRegex = Regex("""\[(?: |x|X)\]""")
private val multiSpaceRegex = Regex(""" {2,}""")
private val whitespaceRegex = Regex("\\s+")

fun stripChecklistMarkers(text: String): String =
    checklistMarkerRegex.replace(text, "").replace(multiSpaceRegex, " ")

fun countWordsIgnoringChecklistMarkers(text: String): Int =
    stripChecklistMarkers(text).trim().split(whitespaceRegex).count { it.isNotBlank() }

fun countCharsIgnoringChecklistMarkers(text: String): Int =
    stripChecklistMarkers(text).length

fun countWordsIgnoringChecklistMarkers(entries: List<String>): Int =
    entries.sumOf(::countWordsIgnoringChecklistMarkers)

fun countCharsIgnoringChecklistMarkers(entries: List<String>): Int =
    entries.sumOf(::countCharsIgnoringChecklistMarkers)

fun estimateReadingTimeMinutes(wordCount: Int, wordsPerMinute: Int = 225): Int {
    if (wordCount <= 0 || wordsPerMinute <= 0) return 0
    return ((wordCount + wordsPerMinute - 1) / wordsPerMinute).coerceAtLeast(1)
}

fun estimateReadingTimeMinutes(text: String, wordsPerMinute: Int = 225): Int =
    estimateReadingTimeMinutes(
        wordCount = countWordsIgnoringChecklistMarkers(text),
        wordsPerMinute = wordsPerMinute
    )

fun estimateReadingTimeMinutes(entries: List<String>, wordsPerMinute: Int = 225): Int =
    estimateReadingTimeMinutes(
        wordCount = countWordsIgnoringChecklistMarkers(entries),
        wordsPerMinute = wordsPerMinute
    )
