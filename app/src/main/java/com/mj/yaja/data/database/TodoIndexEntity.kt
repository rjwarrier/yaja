package com.mj.yaja.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_index",
    indices = [
        Index(value = ["date"]),
        Index(value = ["lineHash"])
    ]
)
data class TodoIndexEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // format: YYYY-MM-DD
    val entryIndex: Int,
    val lineIndexInEntry: Int,
    val displayText: String,
    val isChecked: Boolean,
    val dayLabel: String,
    val lineHash: String,
    val complianceId: String? = null
)
