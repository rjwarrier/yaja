package com.mj.yaja.data

import java.time.LocalDate

data class RevisitMarker(
    val sourceDate: LocalDate,
    val revisitOn: LocalDate,
    val entryIndex: Int? = null,
    val note: String = ""
)

data class DueRevisitItem(
    val sourceDate: LocalDate,
    val revisitOn: LocalDate,
    val entryIndex: Int? = null,
    val label: String,
    val note: String = ""
)
