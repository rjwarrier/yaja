package com.mj.yaja

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.mj.yaja.data.EntryKind
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.SettingsRepository
import com.mj.yaja.data.applyEntryKindMetadata
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object TaskerIntegration {
    const val TASKER_PACKAGE = "net.dinglisch.android.taskerm"
    const val ACTION_ADD_ENTRY = "com.mj.yaja.ACTION_ADD_ENTRY"
    const val ACTION_ENTRY_SAVED = "com.mj.yaja.EVENT_ENTRY_SAVED"
    const val ACTION_ENTRY_EDITED = "com.mj.yaja.EVENT_ENTRY_EDITED"
    const val ACTION_ENTRY_DELETED = "com.mj.yaja.EVENT_ENTRY_DELETED"
    const val ACTION_INTERNAL_ENTRY_ADDED = "com.mj.yaja.ACTION_INTERNAL_ENTRY_ADDED"
    const val ACTION_EDIT_SETTING = "com.twofortyfouram.locale.intent.action.EDIT_SETTING"
    const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
    const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
    const val EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"

    const val EXTRA_DATE = "date"
    const val EXTRA_ENTRY_TEXT = "entry_text"
    const val EXTRA_DAY_LABEL = "day_label"
    const val EXTRA_RECORDED_TIME = "recorded_time"
    const val EXTRA_IS_EDIT = "is_edit"
    const val BUNDLE_ACTION_KIND = "com.mj.yaja.tasker.ACTION_KIND"
    const val BUNDLE_ENTRY_TEXT = "com.mj.yaja.tasker.ENTRY_TEXT"
    const val BUNDLE_MATCH_HEADER = "com.mj.yaja.tasker.MATCH_HEADER"
    const val EVENT_TYPE_SAVED = "saved"
    const val EVENT_TYPE_EDITED = "edited"
    const val EVENT_TYPE_DELETED = "deleted"
    private const val TASKER_PLUGIN_SAVE_TIMEOUT_MS = 15_000L

    enum class PluginActionKind {
        ADD_ENTRY,
        ADD_EVENT,
        ADD_TODO,
        APPEND
    }

    fun parsePluginActionKind(raw: String?): PluginActionKind =
        PluginActionKind.entries.firstOrNull { it.name == raw } ?: PluginActionKind.ADD_ENTRY

    fun buildPluginBundle(
        actionKind: PluginActionKind,
        entryText: String,
        matchHeader: String = ""
    ): Bundle = Bundle().apply {
        putString(BUNDLE_ACTION_KIND, actionKind.name)
        putString(BUNDLE_ENTRY_TEXT, entryText)
        putString(BUNDLE_MATCH_HEADER, matchHeader)
    }

    fun buildPluginBlurb(
        actionKind: PluginActionKind,
        entryText: String,
        matchHeader: String = ""
    ): String {
        val prefix = when (actionKind) {
            PluginActionKind.ADD_ENTRY -> "Add entry"
            PluginActionKind.ADD_EVENT -> "Add event"
            PluginActionKind.ADD_TODO -> "Add todo"
            PluginActionKind.APPEND -> "Append"
        }
        val compact = entryText.trim().replace(Regex("\\s+"), " ")
        val header = matchHeader.trim().replace(Regex("\\s+"), " ")
        return when (actionKind) {
            PluginActionKind.APPEND ->
                if (header.isBlank()) prefix else "$prefix: ${header.take(40)}"
            else ->
                if (compact.isBlank()) prefix else "$prefix: ${compact.take(40)}"
        }
    }

    fun executePluginAction(
        context: Context,
        settingsRepository: SettingsRepository,
        actionKind: PluginActionKind,
        entryText: String,
        matchHeader: String = ""
    ) {
        if (!settingsRepository.allowTaskerAccess.value) {
            throw IOException("Tasker access is disabled in Yaja settings")
        }
        val fileManager = MarkdownFileManager.getInstance(context.applicationContext, settingsRepository)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val timeString = LocalTime.now().format(formatter)
        val normalizedText = when (actionKind) {
            PluginActionKind.ADD_ENTRY -> entryText.trim()
            PluginActionKind.ADD_EVENT -> entryText.trim()
            PluginActionKind.ADD_TODO -> {
                val trimmed = entryText.trim()
                if (trimmed.startsWith("[ ]")) trimmed else "[ ] $trimmed"
            }
            PluginActionKind.APPEND -> entryText.trim()
        }
        val normalizedHeader = matchHeader.trim()
        if (normalizedText.isBlank()) {
            throw IOException("Tasker action text is empty")
        }
        if (actionKind == PluginActionKind.APPEND && normalizedHeader.isBlank()) {
            throw IOException("Tasker append header is empty")
        }
        val entryDate = LocalDate.now()
        val executor = Executors.newSingleThreadExecutor()
        try {
            val future = executor.submit<Boolean> {
                when (actionKind) {
                    PluginActionKind.APPEND -> {
                        val existingEntries = fileManager.getEntriesForDate(entryDate)
                        val existingIndex = existingEntries.indexOfFirst { entry ->
                            entryHeader(entry).equals(normalizedHeader, ignoreCase = true)
                        }
                        if (existingIndex >= 0) {
                            val updatedEntry =
                                appendLineToEntry(existingEntries[existingIndex], normalizedText)
                            fileManager.tryUpdateEntryForDate(entryDate, existingIndex, updatedEntry).success
                        } else {
                            val finalEntry = "<!--time:$timeString-->\n$normalizedHeader\n$normalizedText"
                            fileManager.tryAddEntryForDate(entryDate, finalEntry).success
                        }
                    }
                    else -> {
                        val baseEntry = "<!--time:$timeString-->\n$normalizedText"
                        val finalEntry = if (actionKind == PluginActionKind.ADD_EVENT) {
                            applyEntryKindMetadata(baseEntry, EntryKind.EVENT)
                        } else {
                            baseEntry
                        }
                        fileManager.tryAddEntryForDate(entryDate, finalEntry).success
                    }
                }
            }
            val saved = future.get(TASKER_PLUGIN_SAVE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (!saved) throw IOException("Yaja Tasker action failed to save entry")
        } catch (e: TimeoutException) {
            throw IOException("Yaja Tasker action timed out while saving", e)
        } finally {
            executor.shutdownNow()
        }
        context.applicationContext.sendBroadcast(
            Intent(ACTION_INTERNAL_ENTRY_ADDED).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_DATE, entryDate.toString())
            }
        )
    }

    private fun entryHeader(entry: String): String =
        entry
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() && !it.startsWith("<!--time:") }
            .orEmpty()

    private fun appendLineToEntry(entry: String, lineToAppend: String): String {
        val trimmedEntry = entry.trimEnd()
        return if (trimmedEntry.isBlank()) {
            lineToAppend
        } else {
            "$trimmedEntry\n$lineToAppend"
        }
    }

    fun sendEntrySavedEvent(
        context: Context,
        date: LocalDate,
        entryText: String,
        dayLabel: String,
        recordedTime: String?,
        isEdit: Boolean
    ) {
        val appContext = context.applicationContext
        val settingsRepository = SettingsRepository.getInstance(appContext)
        if (!settingsRepository.allowTaskerEvents.value) return
        val sharedEntryText =
            if (settingsRepository.includeEntryTextInTaskerEvents.value) entryText else ""

        val action = if (isEdit) ACTION_ENTRY_EDITED else ACTION_ENTRY_SAVED
        val intent = Intent(action).apply {
            `package` = TASKER_PACKAGE
            putExtra(EXTRA_DATE, date.toString())
            putExtra(EXTRA_ENTRY_TEXT, sharedEntryText)
            putExtra(EXTRA_DAY_LABEL, dayLabel)
            putExtra(EXTRA_RECORDED_TIME, recordedTime)
            putExtra(EXTRA_IS_EDIT, isEdit)
        }
        appContext.sendBroadcast(intent)
        appContext.triggerYajaTaskerEvent(
            YajaTaskerEventUpdate(
                eventType = if (isEdit) EVENT_TYPE_EDITED else EVENT_TYPE_SAVED,
                date = date.toString(),
                entryText = sharedEntryText,
                dayLabel = dayLabel,
                recordedTime = recordedTime
            )
        )
    }

    fun sendEntryDeletedEvent(
        context: Context,
        date: LocalDate,
        entryText: String,
        dayLabel: String,
        recordedTime: String?
    ) {
        val appContext = context.applicationContext
        val settingsRepository = SettingsRepository.getInstance(appContext)
        if (!settingsRepository.allowTaskerEvents.value) return
        val sharedEntryText =
            if (settingsRepository.includeEntryTextInTaskerEvents.value) entryText else ""

        val intent = Intent(ACTION_ENTRY_DELETED).apply {
            `package` = TASKER_PACKAGE
            putExtra(EXTRA_DATE, date.toString())
            putExtra(EXTRA_ENTRY_TEXT, sharedEntryText)
            putExtra(EXTRA_DAY_LABEL, dayLabel)
            putExtra(EXTRA_RECORDED_TIME, recordedTime)
            putExtra(EXTRA_IS_EDIT, false)
        }
        appContext.sendBroadcast(intent)
        appContext.triggerYajaTaskerEvent(
            YajaTaskerEventUpdate(
                eventType = EVENT_TYPE_DELETED,
                date = date.toString(),
                entryText = sharedEntryText,
                dayLabel = dayLabel,
                recordedTime = recordedTime
            )
        )
    }
}
