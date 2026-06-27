package com.mj.yaja.data

import java.time.LocalDate

@androidx.compose.runtime.Immutable
data class RevisitMarker(
    val sourceDate: LocalDate,
    val revisitOn: LocalDate,
    val entryIndex: Int? = null,
    val note: String = ""
)

@androidx.compose.runtime.Immutable
data class DueRevisitItem(
    val sourceDate: LocalDate,
    val revisitOn: LocalDate,
    val entryIndex: Int? = null,
    val label: String,
    val note: String = ""
)
