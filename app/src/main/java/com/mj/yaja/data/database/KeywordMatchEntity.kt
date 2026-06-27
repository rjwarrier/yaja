package com.mj.yaja.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "keyword_matches",
    indices = [
        Index(value = ["keywordId"]),
        Index(value = ["date"])
    ]
)
data class KeywordMatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keywordId: String,
    val date: String, // format: YYYY-MM-DD
    val entryIndex: Int,
    val matchedText: String,
    val confidence: Float,
    val matchType: String, // KeywordMatchType name
    val snippet: String,
    val startIndex: Int,
    val endExclusive: Int
)
