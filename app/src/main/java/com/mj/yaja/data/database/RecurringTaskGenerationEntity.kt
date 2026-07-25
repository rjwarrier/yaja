package com.mj.yaja.data.database

import androidx.room.Entity

@Entity(
    tableName = "compliance_generations",
    primaryKeys = ["itemId", "targetDate"]
)
data class RecurringTaskGenerationEntity(
    val itemId: String,
    val targetDate: String, // format: YYYY-MM-DD
    val generatedAt: Long = System.currentTimeMillis()
)
