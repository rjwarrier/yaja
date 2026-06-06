package com.mj.yaja.data.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "journal_day_cache",
    primaryKeys = ["journalId", "date"],
    indices = [
        Index(value = ["journalId", "isStarred", "date"]),
        Index(value = ["journalId", "revisitOn", "date"])
    ]
)
data class JournalDayCacheEntity(
    val date: String, // format: YYYY-MM-DD
    val journalId: String = "default",
    val entries: List<String>,
    val isStarred: Boolean,
    val label: String,
    val revisitOn: String?,
    val revisitNote: String,
    val wordCount: Int,
    val entryCount: Int,
    val fileModifiedAt: Long,
    val fileSize: Long
)

