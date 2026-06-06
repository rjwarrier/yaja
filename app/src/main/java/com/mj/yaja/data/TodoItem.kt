package com.mj.yaja.data

import java.time.LocalDate

data class TodoItem(
    val date: LocalDate,
    val entryIndex: Int,
    val lineIndexInEntry: Int,
    val displayText: String,
    val isChecked: Boolean,
    val lineHash: String = ""
)
