package com.mj.yaja.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.yaja.data.EntryKind
import com.mj.yaja.data.EventItem
import com.mj.yaja.data.NavigationChromeMode
import com.mj.yaja.ui.viewmodel.JournalViewModel
import com.mj.yaja.ui.design.AppScreenReveal
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import com.mj.yaja.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodosScreen(
    viewModel: JournalViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToDate: (LocalDate) -> Unit,
    onNavigateToComplianceMaster: () -> Unit
) {
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val isTodoRefreshing by viewModel.todoRefreshInProgress.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val renderCheckboxesAsText by viewModel.renderCheckboxesAsText.collectAsStateWithLifecycle()
    val showBottomBar by viewModel.showBottomBar.collectAsStateWithLifecycle()
    val navigationChromeMode by viewModel.navigationChromeMode.collectAsStateWithLifecycle()
    val showBottomPanelLabels by viewModel.showBottomPanelLabels.collectAsStateWithLifecycle()
    var showAddTodoDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.ensureTodosLoaded()
    }
    // Recomputed only when the lists change — not on every sync-progress recomposition.
    val grouped = remember(todos) { todos.groupBy { it.date }.toList().sortedByDescending { it.first } }
    val groupedEvents = remember(events) { events.groupBy { it.date }.toList().sortedByDescending { it.first } }

    val fabBottomPadding = remember(showBottomBar, navigationChromeMode, showBottomPanelLabels) {
        if (showBottomBar) {
            when (navigationChromeMode) {
                NavigationChromeMode.EXPRESSIVE_PANEL -> {
                    if (showBottomPanelLabels) 92.dp else 76.dp
                }
                NavigationChromeMode.FLOATING_BAR -> {
                    0.dp
                }
            }
        } else {
            0.dp
        }
    }

    Scaffold(
        topBar = {
            TodosTopBar(
                onOpenDrawer = onOpenDrawer,
                onOpenComplianceMaster = onNavigateToComplianceMaster
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = fabBottomPadding)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TodoAddRecurringFab(
                        onClick = onNavigateToComplianceMaster
                    )
                    TodoAddFab(
                        onClick = { showAddTodoDialog = true }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        AppScreenReveal(
            visible = true,
            modifier = Modifier.fillMaxSize()
        ) {
            TodosScreenContent(
                isRefreshing = isTodoRefreshing,
                onRefresh = { viewModel.refreshTodos(forceRebuild = true) },
                paddingValues = paddingValues,
                todos = todos,
                events = events,
                syncProgress = syncProgress,
                groupedTodos = grouped,
                groupedEvents = groupedEvents,
                renderCheckboxesAsText = renderCheckboxesAsText,
                dayLabelForDate = viewModel::getDayLabel,
                onNavigateToDate = onNavigateToDate,
                onToggleTodo = viewModel::toggleTodo
            )
        }

        if (showAddTodoDialog) {
            QuickTodoDialog(
                onDismissRequest = { showAddTodoDialog = false },
                onSave = { text, date, kind ->
                    viewModel.addQuickEntryForDate(
                        date = date,
                        text = text,
                        kind = if (kind == QuickAddKind.EVENT) EntryKind.EVENT else EntryKind.NORMAL
                    )
                    showAddTodoDialog = false
                },
                initialDate = uiState.selectedDate,
                allowDateSelection = true,
                title = stringResource(R.string.todos_quick_add_title)
            )
        }
    }
}
