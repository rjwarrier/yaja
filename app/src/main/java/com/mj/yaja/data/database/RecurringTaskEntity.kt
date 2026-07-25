package com.mj.yaja.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "compliance_masters",
    indices = [Index(value = ["journalId"])]
)
data class RecurringTaskEntity(
    @PrimaryKey val id: String,
    val journalId: String,
    val title: String,
    val description: String = "",
    val isActive: Boolean = true,
    val itemType: String = "TASK",
    val scheduleMode: String,
    val frequency: String,
    val dueDayOfMonth: Int?,
    val dueDayOfWeek: Int?,
    val leadDays: Int,
    val anchorDate: String,
    val startMonth: String,
    val startTime: String? = null,
    val endMode: String = "NEVER",
    val endDate: String? = null,
    val endCount: Int? = null,
    val retiredOn: String?,
    val createdAt: Long
)
