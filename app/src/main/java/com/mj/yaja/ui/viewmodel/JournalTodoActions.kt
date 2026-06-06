package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.EventIndexRepository
import com.mj.yaja.data.TodoIndexRepository
import com.mj.yaja.data.TodoItem
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

    val dates = fileManager.getAllJournalDatesLightweight(forceRefresh = true)
    val fingerprint = fileManager.computeCurrentJournalFingerprint(knownDates = dates)
    val shouldRebuildTodos = forceRebuild || !todoIndexRepository.isCurrent(fingerprint)
    val shouldRebuildEvents = forceRebuild || !eventIndexRepository.isCurrent(fingerprint)
    if (shouldRebuildTodos || shouldRebuildEvents) {
        emitBackgroundToast("Updating todos and events...")
        val entriesByDate = dates.associateWith { date -> fileManager.getEntriesForDateFromDisk(date) }
        if (shouldRebuildTodos) {
            todoIndexRepository.rebuild(
                dates = dates,
                entriesForDate = { date -> entriesByDate[date].orEmpty() },
                dayLabelForDate = { date -> fileManager.getDayLabel(date) },
                fingerprint = fingerprint
            )
        }
        if (shouldRebuildEvents) {
            eventIndexRepository.rebuild(
                dates = dates,
                entriesForDate = { date -> entriesByDate[date].orEmpty() },
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
    val dates = fileManager.getAllJournalDatesLightweight(forceRefresh = true)
    val fingerprint = fileManager.computeCurrentJournalFingerprint(knownDates = dates)
    emitBackgroundToast("Updating todos and events...")
    val entriesByDate = dates.associateWith { date -> fileManager.getEntriesForDateFromDisk(date) }
    todoIndexRepository.rebuild(
        dates = dates,
        entriesForDate = { date -> entriesByDate[date].orEmpty() },
        dayLabelForDate = { date -> fileManager.getDayLabel(date) },
        fingerprint = fingerprint
    )
    eventIndexRepository.rebuild(
        dates = dates,
        entriesForDate = { date -> entriesByDate[date].orEmpty() },
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
