package com.mj.yaja.data

import android.content.Context
import android.util.Log
import com.mj.yaja.data.storage.JournalStorage
import com.mj.yaja.data.storage.JournalStorageFingerprint
import com.mj.yaja.ui.widget.WidgetRefreshCoordinator
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class JournalIndexCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val journalStorage: JournalStorage,
    private val settingsRepository: SettingsRepository,
    private val dateMetadataCache: ConcurrentHashMap<LocalDate, DateFileMetadata>,
    private val todoIndexRepository: TodoIndexRepository,
    private val eventIndexRepository: EventIndexRepository,
    private val dayLabelProvider: (LocalDate) -> String,
    private val saveDateMetadataCache: () -> Unit
) {
    fun syncTodoIndexForDate(
        date: LocalDate,
        entries: List<String>,
        fingerprint: JournalStorageFingerprint? = null
    ) {
        syncDateFileMetadata(date)
        todoIndexRepository.replaceDate(date, entries, dayLabelProvider(date), fingerprint)
        eventIndexRepository.replaceDate(date, entries, fingerprint)
    }

    fun updateTodoIndexRows(date: LocalDate, entries: List<String>?) {
        if (entries.isNullOrEmpty()) {
            todoIndexRepository.removeDate(date)
            eventIndexRepository.removeDate(date)
        } else {
            todoIndexRepository.replaceDate(date, entries, dayLabelProvider(date))
            eventIndexRepository.replaceDate(date, entries)
        }
        Log.d(
            TODO_PIPELINE_TAG,
            "Todo/event indexes synced: date=$date todoRows=${todoIndexRepository.getEntries().size} eventRows=${eventIndexRepository.getEntries().size}"
        )
        WidgetRefreshCoordinator.requestTodoListUpdate(context)
    }

    fun scheduleTodoIndexRowsUpdate(date: LocalDate, entries: List<String>?) {
        scope.launch {
            updateTodoIndexRows(date, entries)
        }
    }

    fun scheduleTodoIndexSyncForDate(date: LocalDate, entries: List<String>) {
        scope.launch {
            syncTodoIndexForDate(date, entries)
        }
    }

    fun scheduleEntryMutationRefresh(date: LocalDate, entries: List<String>?) {
        scope.launch {
            entries?.let { syncDateFileMetadata(date) }
            WidgetRefreshCoordinator.requestHeatmapUpdate(context, invalidateCache = true)
        }
    }

    fun syncDateFileMetadata(date: LocalDate) {
        journalStorage.getDateFileMetadata(date, settingsRepository.storageUri.value)?.let {
            dateMetadataCache[date] = DateFileMetadata(it.first, it.second)
            saveDateMetadataCache()
        }
    }

    private companion object {
        private const val TODO_PIPELINE_TAG = "YajaTodoPipeline"
    }
}
