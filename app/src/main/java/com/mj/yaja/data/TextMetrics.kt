package com.mj.yaja.data

private val checklistMarkerRegex = Regex("""\[(?: |x|X)\]""")

fun stripChecklistMarkers(text: String): String =
    checklistMarkerRegex.replace(text, "").replace(Regex(""" {2,}"""), " ")

fun countWordsIgnoringChecklistMarkers(text: String): Int =
    stripChecklistMarkers(text).trim().split(Regex("\\s+")).count { it.isNotBlank() }

fun countCharsIgnoringChecklistMarkers(text: String): Int =
    stripChecklistMarkers(text).length

fun countWordsIgnoringChecklistMarkers(entries: List<String>): Int =
    entries.sumOf(::countWordsIgnoringChecklistMarkers)

fun countCharsIgnoringChecklistMarkers(entries: List<String>): Int =
    entries.sumOf(::countCharsIgnoringChecklistMarkers)
