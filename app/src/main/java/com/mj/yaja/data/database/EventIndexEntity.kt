package com.mj.yaja.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_index",
    indices = [
        Index(value = ["date"])
    ]
)
data class EventIndexEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // format: YYYY-MM-DD
    val entryIndex: Int,
    val recordedTime: String?,
    val mentionedTime: String?,
    val displayText: String,
    val lineHash: String
)
