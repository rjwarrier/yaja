package com.mj.yaja.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.EventItem
import com.mj.yaja.data.TodoItem
import com.mj.yaja.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mj.yaja.ui.components.AnimatedMenuButton
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.rememberAppEntrance
import java.time.LocalDate

private val todoCheerMessages = listOf(
    R.string.todos_cheer_all_clear,
    R.string.todos_cheer_done_and_dusted,
    R.string.todos_cheer_nothing_pending,
    R.string.todos_cheer_caught_up,
    R.string.todos_cheer_clean_slate
)

internal enum class TodoFilter {
    ALL,
    OPEN,
    DONE,
    EVENTS
}

internal data class TodoHeroMetrics(
    val mainValue: Int,
    val mainLabel: String,
    val accentTitle: String,
    val statAValue: Int,
    val statALabel: String,
    val statBValue: Int,
    val statBLabel: String,
    val statCValue: Int,
    val statCLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodosTopBar(
    onOpenDrawer: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.settings_nav_todos_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        },
        navigationIcon = {
            AnimatedMenuButton(
                onClick = onOpenDrawer,
                modifier = Modifier.padding(start = 8.dp)
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodosScreenContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    paddingValues: PaddingValues,
    todos: List<TodoItem>,
    events: List<EventItem>,
    syncProgress: Float?,
    groupedTodos: List<Pair<LocalDate, List<TodoItem>>>,
    groupedEvents: List<Pair<LocalDate, List<EventItem>>>,
    renderCheckboxesAsText: Boolean,
    dayLabelForDate: (LocalDate) -> String?,
    onNavigateToDate: (LocalDate) -> Unit,
    onToggleTodo: (TodoItem) -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf(TodoFilter.ALL) }
    var openExpanded by rememberSaveable { mutableStateOf(true) }
    var doneExpanded by rememberSaveable { mutableStateOf(true) }
    var pastEventsExpanded by rememberSaveable { mutableStateOf(false) }
    var futureEventsExpanded by rememberSaveable { mutableStateOf(false) }
    var laterTodosExpanded by rememberSaveable { mutableStateOf(false) }
    
    val today = remember { java.time.LocalDate.now() }
    val farFutureCutoff = remember(today) { today.plusDays(10) }
    
    val openTodos = remember(todos) { todos.filter { !it.isChecked } }
    val doneTodos = remember(todos) { todos.filter { it.isChecked } }
    val openGrouped = remember(groupedTodos) {
        groupedTodos.mapNotNull { (date, items) ->
            val filtered = items.filter { !it.isChecked }
            filtered.takeIf { it.isNotEmpty() }?.let { date to it }
        }
    }
    val doneGrouped = remember(groupedTodos) {
        groupedTodos.mapNotNull { (date, items) ->
            val filtered = items.filter { it.isChecked }
            filtered.takeIf { it.isNotEmpty() }?.let { date to it }
        }
    }
    
    val pastEvents = remember(groupedEvents, today) {
        groupedEvents.filter { it.first.isBefore(today) }
    }
    val upcomingEvents = remember(groupedEvents, today, farFutureCutoff) {
        groupedEvents.filter { !it.first.isBefore(today) && !it.first.isAfter(farFutureCutoff) }
    }
    val futureEvents = remember(groupedEvents, farFutureCutoff) {
        groupedEvents.filter { it.first.isAfter(farFutureCutoff) }
    }
    
    val upcomingOpenTodos = remember(openGrouped, farFutureCutoff) {
        openGrouped.filter { !it.first.isAfter(farFutureCutoff) }
    }
    val futureOpenTodos = remember(openGrouped, farFutureCutoff) {
        openGrouped.filter { it.first.isAfter(farFutureCutoff) }
    }
    val entranceTriggered = rememberAppEntrance(delayMillis = 110)
    val sectionUpcoming = stringResource(R.string.todos_section_upcoming)
    val sectionFutureEvents = stringResource(R.string.todos_section_future_events)
    val sectionPastEvents = stringResource(R.string.todos_section_past_events)
    val sectionOpen = stringResource(R.string.todos_section_open)
    val sectionLater = stringResource(R.string.todos_section_later)
    val todayOpen = todos.count { !it.isChecked && it.date == today }
    val todayDone = todos.count { it.isChecked && it.date == today }
    val todayEvents = events.count { it.date == today }
    val recentEvents = events.count { it.date >= today.minusDays(6) }
    val eventDays = events.map { it.date }.toSet().size
    val upcomingEventsCount = events.count { !it.date.isBefore(today) && !it.date.isAfter(farFutureCutoff) }
    val openTasksCount = todos.count { !it.isChecked }
    val heroMetrics = when (selectedFilter) {
        TodoFilter.ALL -> TodoHeroMetrics(
            mainValue = openTasksCount,
            mainLabel = pluralStringResource(R.plurals.todos_main_open_tasks, openTasksCount, openTasksCount),
            accentTitle = stringResource(R.string.todos_hero_all_activity),
            statAValue = todayOpen,
            statALabel = stringResource(R.string.todos_stat_today_open),
            statBValue = todayDone,
            statBLabel = stringResource(R.string.todos_stat_done_today),
            statCValue = upcomingEventsCount,
            statCLabel = stringResource(R.string.todos_stat_events)
        )
        TodoFilter.OPEN -> TodoHeroMetrics(
            mainValue = openTodos.size,
            mainLabel = pluralStringResource(R.plurals.todos_main_open_tasks, openTodos.size, openTodos.size),
            accentTitle = stringResource(R.string.todos_hero_open_focus),
            statAValue = todayOpen,
            statALabel = stringResource(R.string.settings_meaning_today),
            statBValue = doneTodos.size,
            statBLabel = stringResource(R.string.action_done),
            statCValue = upcomingEventsCount,
            statCLabel = stringResource(R.string.todos_stat_events)
        )
        TodoFilter.DONE -> TodoHeroMetrics(
            mainValue = doneTodos.size,
            mainLabel = pluralStringResource(R.plurals.todos_main_done_tasks, doneTodos.size, doneTodos.size),
            accentTitle = stringResource(R.string.todos_hero_completed),
            statAValue = todayDone,
            statALabel = stringResource(R.string.settings_meaning_today),
            statBValue = openTodos.size,
            statBLabel = stringResource(R.string.todos_stat_still_open),
            statCValue = upcomingEventsCount,
            statCLabel = stringResource(R.string.todos_stat_events)
        )
        TodoFilter.EVENTS -> TodoHeroMetrics(
            mainValue = upcomingEventsCount,
            mainLabel = pluralStringResource(R.plurals.todos_main_upcoming_events, upcomingEventsCount, upcomingEventsCount),
            accentTitle = stringResource(R.string.todos_stat_events),
            statAValue = todayEvents,
            statALabel = stringResource(R.string.settings_meaning_today),
            statBValue = recentEvents,
            statBLabel = stringResource(R.string.todos_stat_past_7d),
            statCValue = eventDays,
            statCLabel = stringResource(R.string.todos_stat_days)
        )
    }
    LaunchedEffect(selectedFilter, openTodos.size) {
        if (selectedFilter == TodoFilter.OPEN && openTodos.isEmpty()) {
            selectedFilter = TodoFilter.ALL
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val filteredVisibleTodos = when (selectedFilter) {
                TodoFilter.ALL -> todos
                TodoFilter.OPEN -> openTodos
                TodoFilter.DONE -> doneTodos
                TodoFilter.EVENTS -> emptyList()
            }
            val emptyMessage = when (selectedFilter) {
                TodoFilter.DONE -> stringResource(R.string.todos_empty_done)
                TodoFilter.EVENTS -> stringResource(R.string.todos_empty_events)
                else -> stringResource(todoCheerMessages.random())
            }
            when {
                selectedFilter != TodoFilter.EVENTS && filteredVisibleTodos.isEmpty() && syncProgress != null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                selectedFilter == TodoFilter.EVENTS && events.isEmpty() -> {
                    TodosEmptyState(message = emptyMessage)
                }
                selectedFilter != TodoFilter.EVENTS && filteredVisibleTodos.isEmpty() -> {
                    TodosEmptyState(message = emptyMessage)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
                    ) {
                        item("hero") {
                            AppStaggeredEntrance(
                                visible = entranceTriggered,
                                index = 0,
                                strength = AppEntranceStrength.HERO
                            ) {
                                TodoHeroCard(
                                    selectedFilter = selectedFilter,
                                    metrics = heroMetrics
                                )
                            }
                        }
                        item("filters") {
                            AppStaggeredEntrance(
                                visible = entranceTriggered,
                                index = 1,
                                strength = AppEntranceStrength.SECTION
                            ) {
                                TodoFilterChips(
                                    selectedFilter = selectedFilter,
                                    allCount = todos.size,
                                    openCount = openTodos.size,
                                    doneCount = doneTodos.size,
                                    eventCount = events.size,
                                    onFilterSelected = { selectedFilter = it }
                                )
                            }
                        }
                        if (selectedFilter == TodoFilter.EVENTS) {
                            val eventSections = listOf(
                                Triple(sectionUpcoming, upcomingEvents, true to {}),
                                Triple(sectionFutureEvents, futureEvents, futureEventsExpanded to { futureEventsExpanded = !futureEventsExpanded }),
                                Triple(sectionPastEvents, pastEvents, pastEventsExpanded to { pastEventsExpanded = !pastEventsExpanded })
                            )
                            eventSections.forEach { (title, grouped, expansion) ->
                                val (expanded, onToggle) = expansion
                                if (grouped.isNotEmpty()) {
                                    item("${title}_header") {
                                        AppStaggeredEntrance(
                                            visible = entranceTriggered,
                                            index = 2,
                                            strength = AppEntranceStrength.SECTION
                                        ) {
                                            TodoSectionHeader(
                                                title = title,
                                                count = grouped.sumOf { it.second.size },
                                                expanded = expanded,
                                                onToggle = onToggle
                                            )
                                        }
                                    }
                                    if (expanded) {
                                        grouped.forEach { (date, itemsForDate) ->
                                            item(key = "event-header-$title-$date") {
                                                val groupIndex = grouped.indexOfFirst { it.first == date }
                                                AppStaggeredEntrance(
                                                    visible = entranceTriggered,
                                                    index = (groupIndex + 3).coerceAtMost(6),
                                                    strength = AppEntranceStrength.SECTION
                                                ) {
                                                    TodoDateHeader(
                                                        date = date,
                                                        label = dayLabelForDate(date).orEmpty(),
                                                        onClick = { onNavigateToDate(date) }
                                                    )
                                                }
                                            }
                                            itemsIndexed(
                                                items = itemsForDate,
                                                key = { _, item ->
                                                    "event-$title-${item.date}-${item.entryIndex}-${item.lineHash}"
                                                }
                                            ) { index, item ->
                                                AppStaggeredEntrance(
                                                    visible = entranceTriggered,
                                                    index = (index + 4).coerceAtMost(7),
                                                    strength = AppEntranceStrength.SUBTLE
                                                ) {
                                                    EventItemCard(
                                                        item = item,
                                                        modifier = Modifier.animateItem(
                                                            fadeInSpec = androidx.compose.animation.core.spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                            ),
                                                            fadeOutSpec = androidx.compose.animation.core.spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                            ),
                                                            placementSpec = androidx.compose.animation.core.spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (selectedFilter != TodoFilter.DONE && selectedFilter != TodoFilter.EVENTS && openTodos.isNotEmpty()) {
                            val openSections = listOf(
                                Triple(sectionOpen, upcomingOpenTodos, openExpanded to { openExpanded = !openExpanded }),
                                Triple(sectionLater, futureOpenTodos, laterTodosExpanded to { laterTodosExpanded = !laterTodosExpanded })
                            )
                            openSections.forEach { (title, grouped, expansion) ->
                                val (expanded, onToggle) = expansion
                                if (grouped.isNotEmpty()) {
                                    item("${title}_section_header") {
                                        AppStaggeredEntrance(
                                            visible = entranceTriggered,
                                            index = 2,
                                            strength = AppEntranceStrength.SECTION
                                        ) {
                                            TodoSectionHeader(
                                                title = title,
                                                count = grouped.sumOf { it.second.size },
                                                expanded = expanded,
                                                onToggle = onToggle
                                            )
                                        }
                                    }
                                    if (expanded) {
                                        grouped.forEach { (date, itemsForDate) ->
                                            item(key = "open-header-$title-$date") {
                                                val groupIndex = grouped.indexOfFirst { it.first == date }
                                                AppStaggeredEntrance(
                                                    visible = entranceTriggered,
                                                    index = (groupIndex + 3).coerceAtMost(6),
                                                    strength = AppEntranceStrength.SECTION
                                                ) {
                                                    TodoDateHeader(
                                                        date = date,
                                                        label = dayLabelForDate(date).orEmpty(),
                                                        onClick = { onNavigateToDate(date) }
                                                    )
                                                }
                                            }
                                            val keyedItems = itemsForDate.withDuplicateHashCounters()
                                            itemsIndexed(
                                                items = keyedItems,
                                                key = { _, keyedItem ->
                                                    "open-$title-${keyedItem.item.date}-${keyedItem.item.lineHash}-${keyedItem.duplicateIndex}"
                                                }
                                            ) { index, keyedItem ->
                                                AppStaggeredEntrance(
                                                    visible = entranceTriggered,
                                                    index = (index + 4).coerceAtMost(7),
                                                    strength = AppEntranceStrength.SUBTLE
                                                ) {
                                                    TodoItemCard(
                                                        item = keyedItem.item,
                                                        renderCheckboxesAsText = renderCheckboxesAsText,
                                                        onToggle = { onToggleTodo(keyedItem.item) },
                                                        modifier = Modifier.animateItem(
                                                            fadeInSpec = androidx.compose.animation.core.spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                            ),
                                                            fadeOutSpec = androidx.compose.animation.core.spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                            ),
                                                            placementSpec = androidx.compose.animation.core.spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (selectedFilter != TodoFilter.OPEN && selectedFilter != TodoFilter.EVENTS) {
                            item("done_section_header") {
                                AppStaggeredEntrance(
                                    visible = entranceTriggered,
                                    index = 2,
                                    strength = AppEntranceStrength.SECTION
                                ) {
                                    TodoSectionHeader(
                                        title = stringResource(R.string.action_done).uppercase(),
                                        count = doneTodos.size,
                                        expanded = doneExpanded,
                                        onToggle = { doneExpanded = !doneExpanded }
                                    )
                                }
                            }
                            if (doneExpanded) {
                                doneGrouped.forEach { (date, itemsForDate) ->
                                    item(key = "done-header-$date") {
                                        val groupIndex = doneGrouped.indexOfFirst { it.first == date }
                                        AppStaggeredEntrance(
                                            visible = entranceTriggered,
                                            index = (groupIndex + 3).coerceAtMost(6),
                                            strength = AppEntranceStrength.SECTION
                                        ) {
                                            TodoDateHeader(
                                                date = date,
                                                label = dayLabelForDate(date).orEmpty(),
                                                onClick = { onNavigateToDate(date) }
                                            )
                                        }
                                    }
                                    val keyedItems = itemsForDate.withDuplicateHashCounters()
                                    itemsIndexed(
                                        items = keyedItems,
                                        key = { _, keyedItem ->
                                            "done-${keyedItem.item.date}-${keyedItem.item.lineHash}-${keyedItem.duplicateIndex}"
                                        }
                                    ) { index, keyedItem ->
                                        AppStaggeredEntrance(
                                            visible = entranceTriggered,
                                            index = (index + 4).coerceAtMost(7),
                                            strength = AppEntranceStrength.SUBTLE
                                        ) {
                                            TodoItemCard(
                                                item = keyedItem.item,
                                                renderCheckboxesAsText = renderCheckboxesAsText,
                                                onToggle = { onToggleTodo(keyedItem.item) },
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = androidx.compose.animation.core.spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    ),
                                                    fadeOutSpec = androidx.compose.animation.core.spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    ),
                                                    placementSpec = androidx.compose.animation.core.spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal data class KeyedTodoItem(
    val item: TodoItem,
    val duplicateIndex: Int
)

internal fun List<TodoItem>.withDuplicateHashCounters(): List<KeyedTodoItem> {
    val counters = mutableMapOf<String, Int>()
    return map { item ->
        val key = "${item.date}-${item.lineHash}"
        val duplicateIndex = counters.getOrDefault(key, 0)
        counters[key] = duplicateIndex + 1
        KeyedTodoItem(item, duplicateIndex)
    }
}
