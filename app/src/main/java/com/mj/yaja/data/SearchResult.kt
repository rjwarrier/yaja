package com.mj.yaja.data

import java.time.LocalDate

@androidx.compose.runtime.Immutable
data class SearchResult(
    val date: LocalDate,
    val entryPreview: String,
    val isLabelMatch: Boolean = false
)
