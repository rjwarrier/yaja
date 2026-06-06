package com.mj.yaja.ui.viewmodel

import android.content.Context
import com.mj.yaja.TaskerIntegration
import com.mj.yaja.data.stripEntryRevisitMetadata
import java.time.LocalDate

private val entryTimeCommentRegex = Regex("^<!--time:(.*?)-->\\n?")
private val recordedTimeRegex = Regex("^<!--time:(\\d{2}:\\d{2})")

internal fun emitTaskerEntrySavedEvent(
    context: Context,
    date: LocalDate,
    entries: List<String>,
    sourceEntry: String,
    dayLabel: String,
    customTime: String?,
    isEdit: Boolean,
    entryIndexHint: Int = -1
) {
    val savedEntry = when {
        isEdit && entryIndexHint >= 0 -> entries.getOrNull(entryIndexHint)
        else -> entries.lastOrNull()
    } ?: sourceEntry

    val visibleEntry = stripEntryRevisitMetadata(savedEntry)
        .replace(entryTimeCommentRegex, "")
        .trim()
    val recordedTime = customTime ?: recordedTimeRegex.find(savedEntry)
        ?.groupValues
        ?.getOrNull(1)

    TaskerIntegration.sendEntrySavedEvent(
        context = context,
        date = date,
        entryText = visibleEntry,
        dayLabel = dayLabel,
        recordedTime = recordedTime,
        isEdit = isEdit
    )
}

internal fun emitTaskerEntryDeletedEvent(
    context: Context,
    date: LocalDate,
    sourceEntry: String,
    dayLabel: String
) {
    val visibleEntry = stripEntryRevisitMetadata(sourceEntry)
        .replace(entryTimeCommentRegex, "")
        .trim()
    val recordedTime = recordedTimeRegex.find(sourceEntry)
        ?.groupValues
        ?.getOrNull(1)

    TaskerIntegration.sendEntryDeletedEvent(
        context = context,
        date = date,
        entryText = visibleEntry,
        dayLabel = dayLabel,
        recordedTime = recordedTime
    )
}
