package com.mj.yaja.data

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class EventItem(
    val date: LocalDate,
    val entryIndex: Int,
    val recordedTime: String? = null,
    val mentionedTime: String? = null,
    val displayText: String,
    val lineHash: String = ""
)
