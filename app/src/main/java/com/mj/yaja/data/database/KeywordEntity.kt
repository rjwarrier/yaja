package com.mj.yaja.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "keywords")
data class KeywordEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // Maps to KeywordType.name (PERSON or PLACE)
    val relation: String,
    val aliases: List<String>, // Uses TypeConverters for String List
    val isEnabled: Boolean,
    val createdAt: Long
)
