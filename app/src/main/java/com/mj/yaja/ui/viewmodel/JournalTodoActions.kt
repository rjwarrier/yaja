package com.mj.yaja.ui.viewmodel

import android.util.Log
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.EventIndexRepository
import com.mj.yaja.data.TodoIndexRepository
import com.mj.yaja.data.TodoItem
import com.mj.yaja.data.RecurringTaskRepository
import com.mj.yaja.ui.widget.WidgetRefreshCoordinator

internal fun sortedTodoItems(
    todoIndexRepository: TodoIndexRepository
): List<TodoItem> =
    todoIndexRepository.getTodoItems().sortedWith(
        compareByDescending<TodoItem> { it.date }
            .thenBy { it.entryIndex }
            .thenBy { it.lineIndexInEntry }
    )

internal suspend fun refreshTodosWorkflow(
    fileManager: MarkdownFileManager,
    todoIndexRepository: TodoIndexRepository,
    eventIndexRepository: EventIndexRepository,
    forceRebuild: Boolean,
    emitBackgroundToast: suspend (String) -> Unit,
    publishCurrentTodos: () -> Unit
) {
    publishCurrentTodos()

    runCatching {
        RecurringTaskRepository.getInstance(fileManager.getContext()).generateTodos(fileManager)
    }.onFailure {
        Log.e("YajaTodoPipeline", "Error generating recurring todos", it)
    }

    val dates = fileManager.getAllJournalDatesLightweight(forceRefresh = true)
    val fingerprint = fileManager.computeCurrentJournalFingerprint(knownDates = dates)
    val shouldRebuildTodos = forceRebuild || !todoIndexRepository.isCurrent(fingerprint)
    val shouldRebuildEvents = forceRebuild || !eventIndexRepository.isCurrent(fingerprint)
    if (shouldRebuildTodos || shouldRebuildEvents) {
        emitBackgroundToast("Updating todos and events...")
        val entriesSnapshot = fileManager.getEntriesSnapshotForRebuild()
        val entryLoader: (java.time.LocalDate) -> List<String> = { date ->
            entriesSnapshot[date] ?: fileManager.readEntriesForDateDirect(date)
        }
        if (shouldRebuildTodos) {
            todoIndexRepository.rebuildStreaming(
                dates = dates,
                entryLoader = entryLoader,
                dayLabelLoader = { date -> fileManager.getDayLabel(date) },
                fingerprint = fingerprint
            )
        }
        if (shouldRebuildEvents) {
            eventIndexRepository.rebuildStreaming(
                dates = dates,
                entryLoader = entryLoader,
                fingerprint = fingerprint
            )
        }
        emitBackgroundToast("Todos and events updated.")
        publishCurrentTodos()
        WidgetRefreshCoordinator.requestTodoListUpdate(fileManager.getContext())
    }
}

internal suspend fun rebuildTodoIndexWorkflow(
    fileManager: MarkdownFileManager,
    todoIndexRepository: TodoIndexRepository,
    eventIndexRepository: EventIndexRepository,
    emitBackgroundToast: suspend (String) -> Unit,
    publishCurrentTodos: () -> Unit
) {
    runCatching {
        RecurringTaskRepository.getInstance(fileManager.getContext()).generateTodos(fileManager)
    }.onFailure {
        Log.e("YajaTodoPipeline", "Error generating recurring todos", it)
    }

    val dates = fileManager.getAllJournalDatesLightweight(forceRefresh = true)
    val fingerprint = fileManager.computeCurrentJournalFingerprint(knownDates = dates)
    emitBackgroundToast("Updating todos and events...")
    val entriesSnapshot = fileManager.getEntriesSnapshotForRebuild()
    val entryLoader: (java.time.LocalDate) -> List<String> = { date ->
        entriesSnapshot[date] ?: fileManager.readEntriesForDateDirect(date)
    }
    todoIndexRepository.rebuildStreaming(
        dates = dates,
        entryLoader = entryLoader,
        dayLabelLoader = { date -> fileManager.getDayLabel(date) },
        fingerprint = fingerprint
    )
    eventIndexRepository.rebuildStreaming(
        dates = dates,
        entryLoader = entryLoader,
        fingerprint = fingerprint
    )
    emitBackgroundToast("Todos and events updated.")
    publishCurrentTodos()
    WidgetRefreshCoordinator.requestTodoListUpdate(fileManager.getContext())
}

internal suspend fun toggleTodoWorkflow(
    fileManager: MarkdownFileManager,
    item: TodoItem,
    onToggleFailed: suspend () -> Unit,
    onSelectedDateChanged: suspend (TodoItem) -> Unit,
    publishCurrentTodos: () -> Unit
) {
    if (
        !fileManager.toggleTodoLine(
            item.date,
            item.entryIndex,
            item.lineIndexInEntry,
            item.lineHash,
            item.displayText
        )
    ) {
        onToggleFailed()
        return
    }

    onSelectedDateChanged(item)
    publishCurrentTodos()
}
