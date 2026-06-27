package com.mj.yaja.data

import java.time.LocalDate

enum class TodoSourceType {
    JOURNAL,
    COMPLIANCE
}

data class TodoItem(
    val date: LocalDate,
    val entryIndex: Int,
    val lineIndexInEntry: Int,
    val displayText: String,
    val isChecked: Boolean,
    val lineHash: String = "",
    val sourceType: TodoSourceType = TodoSourceType.JOURNAL,
    val sourceId: String? = null,
    val sourcePeriod: String? = null
)
