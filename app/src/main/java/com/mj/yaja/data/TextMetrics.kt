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
