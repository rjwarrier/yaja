package com.mj.yaja.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.mj.yaja.R
import com.mj.yaja.data.countCharsIgnoringChecklistMarkers
import com.mj.yaja.data.countWordsIgnoringChecklistMarkers
import com.mj.yaja.data.estimateReadingTimeMinutes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.KeywordMatch
import com.mj.yaja.data.NavigationChromeMode
import com.mj.yaja.data.DueRevisitItem
import com.mj.yaja.data.EntryStyle
import com.mj.yaja.ui.components.AnimatedMenuButton
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.utils.MarkdownUtils
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

internal data class HomeEntryListItem(
        val id: Long,
        val text: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit,
        isDrawerOpen: Boolean,
        onNavigateToAddEntry: () -> Unit,
        onNavigateToVersionSnapshots: () -> Unit,
) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val showTimestamps by viewModel.showTimestamps.collectAsStateWithLifecycle()
        val showDayHeaderStats by viewModel.showDayHeaderStats.collectAsStateWithLifecycle()
        val hideTextModeEnabled by viewModel.hideTextModeEnabled.collectAsStateWithLifecycle()
        val renderCheckboxesAsText by viewModel.renderCheckboxesAsText.collectAsStateWithLifecycle()
        val swipeToNavigateDatesEnabled by
                viewModel.swipeToNavigateDatesEnabled.collectAsStateWithLifecycle()
        val starredDates by viewModel.starredDates.collectAsStateWithLifecycle()
        val starredLabels by viewModel.starredLabels.collectAsStateWithLifecycle()
        val isFavorited by remember(uiState.selectedDate, starredDates) {
                derivedStateOf { starredDates.contains(uiState.selectedDate) }
        }
        val lastDeleted by viewModel.lastDeleted.collectAsStateWithLifecycle()
        val enableDragAndDrop by viewModel.enableDragAndDrop.collectAsStateWithLifecycle()
        val entryDeleteSelectionEnabled by
                viewModel.entryDeleteSelectionEnabled.collectAsStateWithLifecycle()
        val currentDayLabel by viewModel.currentDayLabel.collectAsStateWithLifecycle()
        val dueRevisits by viewModel.dueRevisits.collectAsStateWithLifecycle()
        val showBottomBar by viewModel.showBottomBar.collectAsStateWithLifecycle()
        val navigationChromeMode by viewModel.navigationChromeMode.collectAsStateWithLifecycle()
        val showBottomPanelLabels by viewModel.showBottomPanelLabels.collectAsStateWithLifecycle()
        val entryStyle by viewModel.entryStyle.collectAsStateWithLifecycle()
        val versionSnapshots by viewModel.versionHistorySnapshots.collectAsStateWithLifecycle()
        // Day label dialog state (for all days)
        var showDayLabelDialog by remember { mutableStateOf(false) }
        var dayLabelInput by remember { mutableStateOf("") }

        // Star label dialog state (for starring days without an existing label)
        var showLabelDialog by remember { mutableStateOf(false) }
        var labelInput by remember { mutableStateOf("") }
        var selectedEntryItemIds by remember(uiState.selectedDate) { mutableStateOf<Set<Long>>(emptySet()) }

        // Countdown state for undo bar (5 seconds)
        val undoCountdown = remember { Animatable(1f) }
        LaunchedEffect(lastDeleted) {
                if (lastDeleted != null) {
                        undoCountdown.snapTo(1f)
                        undoCountdown.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
                        )
                        viewModel.finalizeDeleteCountdown()
                }
        }

        val allowFutureEntries by viewModel.allowFutureEntries.collectAsStateWithLifecycle()
        val swipeToSyncEnabled by viewModel.swipeToSyncEnabled.collectAsStateWithLifecycle()
        val isPreviewLimitEnabled by viewModel.isPreviewLimitEnabled.collectAsStateWithLifecycle()
        val previewLimitLength by viewModel.previewLimitLength.collectAsStateWithLifecycle()
        val dateOrderPreference by viewModel.dateOrderPreference.collectAsStateWithLifecycle()
        val monthFirst by remember(dateOrderPreference) {
                derivedStateOf {
                        com.mj.yaja.ui.utils.DateLinkUtils.resolveMonthFirst(dateOrderPreference)
                }
        }
        val customDateKeywords by viewModel.customDateKeywords.collectAsStateWithLifecycle()
        val keywords by viewModel.keywords.collectAsStateWithLifecycle()
        val keywordHighlightingEnabled by
                viewModel.keywordHighlightingEnabled.collectAsStateWithLifecycle()

        LaunchedEffect(uiState.selectedDate) {
                viewModel.loadVersionHistorySnapshots(uiState.selectedDate)
        }

        LaunchedEffect(Unit) {
                viewModel.refreshSelectedDateEntries(
                        showLoading = false,
                        reason = "home_screen_visible"
                )
        }

        Box(modifier = Modifier.fillMaxSize()) {
                HomeScreenContent(
                        selectedDate = uiState.selectedDate,
                        isLoading = uiState.isLoading,
                        entries = uiState.entries,
                        activeKeywordFilter = uiState.activeKeywordFilter,
                        keywordFilteredEntries = uiState.keywordFilteredEntries,
                        onDeleteEntry = { index -> viewModel.deleteEntry(index) },
                        selectedEntryItemIds = selectedEntryItemIds,
                        onClearEntrySelection = { selectedEntryItemIds = emptySet() },
                        onToggleEntrySelection = { itemId ->
                                selectedEntryItemIds =
                                        if (itemId in selectedEntryItemIds) {
                                                selectedEntryItemIds - itemId
                                        } else {
                                                selectedEntryItemIds + itemId
                                        }
                        },
                        onStartEntrySelection = { itemId ->
                                selectedEntryItemIds = selectedEntryItemIds + itemId
                        },
                        onDeleteSelectedEntries = { indices ->
                                selectedEntryItemIds = emptySet()
                                viewModel.deleteEntries(indices)
                        },
                        onReorderEntries = { reorderedEntries -> viewModel.reorderEntries(reorderedEntries) },
                        onClearKeywordFilter = { viewModel.clearKeywordFilter() },
                        onOpenDrawer = onOpenDrawer,
                        onNavigateToAddEntry = onNavigateToAddEntry,
                        onPreviousDate = {
                                viewModel.selectDate(
                                        uiState.selectedDate.minusDays(1),
                                        source = "home_previous_date"
                                )
                        },
                        onNextDate = {
                                viewModel.selectDate(
                                        uiState.selectedDate.plusDays(1),
                                        source = "home_next_date"
                                )
                        },
                        onSelectDate = { viewModel.selectDate(it, source = "home_date_select") },
                        onJumpToToday = {
                                viewModel.selectDate(
                                        LocalDate.now(),
                                        source = "home_jump_to_today"
                                )
                        },
                        onStartEditing = { entry, index -> viewModel.startEditing(entry, index) },
                        onClearEditing = { viewModel.clearEditing() },
                        searchQuery = uiState.searchQuery,
                        searchResults = uiState.searchResults,
                        onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
                        onClearSearch = { viewModel.clearSearch() },
                        showTimestamps = showTimestamps,
                        showDayHeaderStats = showDayHeaderStats,
                        hideTextModeEnabled = hideTextModeEnabled,
                        onHideTextModeEnabledChange = { viewModel.setHideTextModeEnabled(it) },
                        renderCheckboxesAsText = renderCheckboxesAsText,
                        isFavorited = isFavorited,
                        allowFutureEntries = allowFutureEntries,
                        swipeToNavigateDatesEnabled = swipeToNavigateDatesEnabled,
                        swipeToSyncEnabled = swipeToSyncEnabled,
                        onRefreshCache = { viewModel.refreshCache() },
                        onResultClicked = { date ->
                                viewModel.clearSearch()
                                viewModel.selectDate(date, source = "home_revisit_jump")
                        },
                        onDismissAnomalyDialog = { viewModel.dismissCacheAnomalyDialog() },
                        onAcceptAnomalyRefresh = { viewModel.acceptCacheAnomalyRefresh() },
                        showCacheAnomalyDialog = uiState.showCacheAnomalyDialog,
                        isPreviewLimitEnabled = isPreviewLimitEnabled,
                        previewLimitLength = previewLimitLength,
                        enableDragAndDrop = enableDragAndDrop,
                        dayLabel = currentDayLabel,
                        starredLabels = starredLabels,
                        dueRevisits = dueRevisits,
                        entryStyle = entryStyle,
                        onOpenDueRevisit = { sourceDate ->
                                viewModel.selectDate(sourceDate, source = "home_due_revisit")
                        },
                        onOpenVersionSnapshots = onNavigateToVersionSnapshots,
                        showVersionSnapshotsButton = versionSnapshots.isNotEmpty(),
                        versionSnapshotsCount = versionSnapshots.size,
                        onEditDayLabel = {
                            dayLabelInput = currentDayLabel  // pre-fill with existing label
                            showDayLabelDialog = true
                        },
                        onToggleStar = {
                                if (!isFavorited) {
                                        // Starring — skip the label dialog if the day already has a label
                                        if (currentDayLabel.isNotEmpty()) {
                                                // Use existing day label as the star label too
                                                viewModel.setStarredWithLabel(uiState.selectedDate, currentDayLabel)
                                        } else {
                                                labelInput = ""
                                                showLabelDialog = true
                                        }
                                } else {
                                        // Unstarring - just toggle off
                                        viewModel.unStarDate(uiState.selectedDate)
                                }
                        },
                        onDateLinkClick = { date ->
                                viewModel.selectDate(date, source = "home_date_link")
                        },
                        keywords = if (keywordHighlightingEnabled) keywords else emptyList(),
                        monthFirst = monthFirst,
                        customDateKeywords = customDateKeywords,
                        isDrawerOpen = isDrawerOpen,
                        entryDeleteSelectionEnabled = entryDeleteSelectionEnabled,
                        fabBottomPadding =
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
                )

                HomeScreenOverlays(
                        showDayLabelDialog = showDayLabelDialog,
                        currentDayLabel = currentDayLabel,
                        dayLabelInput = dayLabelInput,
                        onDayLabelInputChange = { dayLabelInput = it },
                        onDismissDayLabelDialog = { showDayLabelDialog = false },
                        onSaveDayLabel = {
                                viewModel.setDayLabel(uiState.selectedDate, dayLabelInput)
                                showDayLabelDialog = false
                        },
                        onRemoveDayLabel = {
                                viewModel.setDayLabel(uiState.selectedDate, "")
                                showDayLabelDialog = false
                        },
                        showLabelDialog = showLabelDialog,
                        labelInput = labelInput,
                        onLabelInputChange = { labelInput = it },
                        onDismissLabelDialog = { showLabelDialog = false },
                        onConfirmLabelDialog = {
                                viewModel.setStarredWithLabel(uiState.selectedDate, labelInput)
                                showLabelDialog = false
                        },
                        showUndoDeleteBar = lastDeleted != null,
                        deletedEntryCount = lastDeleted?.entries?.size ?: 0,
                        undoCountdownValue = undoCountdown.value,
                        onUndoDelete = { viewModel.undoDelete() }
                )
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
        selectedDate: LocalDate,
        isLoading: Boolean,
        entries: List<String>,
        activeKeywordFilter: KeywordDefinition? = null,
        keywordFilteredEntries: List<Pair<LocalDate, List<KeywordMatch>>>? = null,
        onDeleteEntry: (Int) -> Unit,
        selectedEntryItemIds: Set<Long> = emptySet(),
        onClearEntrySelection: () -> Unit = {},
        onToggleEntrySelection: (Long) -> Unit = {},
        onStartEntrySelection: (Long) -> Unit = {},
        onDeleteSelectedEntries: (Set<Int>) -> Unit = {},
        onReorderEntries: (List<String>) -> Unit = {},
        onClearKeywordFilter: () -> Unit = {},
        onOpenDrawer: () -> Unit,
        onNavigateToAddEntry: () -> Unit,
        onPreviousDate: () -> Unit,
        onNextDate: () -> Unit,
        onSelectDate: (LocalDate) -> Unit = {},
        swipeToNavigateDatesEnabled: Boolean = false,
        onJumpToToday: () -> Unit,
        dueRevisits: List<DueRevisitItem> = emptyList(),
        entryStyle: EntryStyle = EntryStyle.CARDS,
        onOpenDueRevisit: (LocalDate) -> Unit = {},
        onOpenVersionSnapshots: () -> Unit = {},
        showVersionSnapshotsButton: Boolean = false,
        versionSnapshotsCount: Int = 0,
        onStartEditing: (String, Int) -> Unit,
        onClearEditing: () -> Unit,
        searchQuery: String = "",
        searchResults: List<com.mj.yaja.data.SearchResult> = emptyList(),
        onSearchQueryChanged: (String) -> Unit = {},
        onClearSearch: () -> Unit = {},
        showTimestamps: Boolean = true,
        showDayHeaderStats: Boolean = true,
        hideTextModeEnabled: Boolean = false,
        onHideTextModeEnabledChange: (Boolean) -> Unit = {},
        renderCheckboxesAsText: Boolean = false,
        isFavorited: Boolean = false,
        onToggleFavorite: () -> Unit = {},
        onToggleStar: () -> Unit = {},
        allowFutureEntries: Boolean = false,
        swipeToSyncEnabled: Boolean = true,
        onRefreshCache: () -> Unit = {},
        onResultClicked: (LocalDate) -> Unit = {},
        showCacheAnomalyDialog: Boolean = false,
        onDismissAnomalyDialog: () -> Unit = {},
        onAcceptAnomalyRefresh: () -> Unit = {},
        isPreviewLimitEnabled: Boolean = true,
        previewLimitLength: Int = 200,
        enableDragAndDrop: Boolean = true,
        entryDeleteSelectionEnabled: Boolean = true,
        dayLabel: String = "",
        starredLabels: Map<LocalDate, String> = emptyMap(),
        onEditDayLabel: () -> Unit = {},
        onDateLinkClick: (LocalDate) -> Unit = {},
        keywords: List<com.mj.yaja.data.KeywordDefinition> = emptyList(),
        monthFirst: Boolean = com.mj.yaja.ui.utils.DateLinkUtils.isMonthFirst(),
        customDateKeywords: List<com.mj.yaja.data.DateKeywordEntry> = emptyList(),
        isDrawerOpen: Boolean = false,
        fabBottomPadding: Dp = 0.dp
) {
        val dayFormatter = remember { DateTimeFormatter.ofPattern("dd") }
        val monthYearFormatter = remember { DateTimeFormatter.ofPattern("MMMM, yyyy") }
        val weekdayFormatter = remember { DateTimeFormatter.ofPattern("EEEE") }
        val cleanedEntries = remember(entries) {
                entries.map(MarkdownUtils::stripMetadata)
        }
        val totalWords = remember(cleanedEntries) {
                countWordsIgnoringChecklistMarkers(cleanedEntries)
        }
        val totalChars = remember(cleanedEntries) {
                countCharsIgnoringChecklistMarkers(cleanedEntries)
        }
        val readingTimeMinutes = remember(totalWords) { estimateReadingTimeMinutes(totalWords) }
        var showFutureDateDialog by remember { mutableStateOf(false) }
        var nextEntryItemId by rememberSaveable { mutableLongStateOf(0L) }
        var reorderedEntryItems by remember { mutableStateOf(entries.map { HomeEntryListItem(nextEntryItemId++, it) }) }
        val isKeywordFilterActive = keywordFilteredEntries != null

        // Update reorderedEntries when entries change (add/edit/delete)
        LaunchedEffect(selectedDate, entries) {
                val existingByText = reorderedEntryItems.groupBy { it.text }.mapValues { (_, value) ->
                        ArrayDeque(value)
                }
                reorderedEntryItems = entries.map { entry ->
                        existingByText[entry]?.removeFirstOrNull()
                                ?: HomeEntryListItem(nextEntryItemId++, entry)
                }
        }

        // Reorderable list state
        val listState = rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(
                lazyListState = listState,
                onMove = { from, to ->
                        reorderedEntryItems = reorderedEntryItems.toMutableList().apply {
                                add(to.index, removeAt(from.index))
                        }
                        onReorderEntries(reorderedEntryItems.map { it.text })
                }
        )

        val isTodayOrFuture = !selectedDate.isBefore(LocalDate.now())

        Scaffold(
                topBar = {
                        HomeTopBar(
                                selectedDate = selectedDate,
                                searchQuery = searchQuery,
                                onSearchQueryChanged = onSearchQueryChanged,
                                onClearSearch = onClearSearch,
                                onOpenDrawer = onOpenDrawer,
                                isFavorited = isFavorited,
                                onToggleStar = onToggleStar,
                                hideTextModeEnabled = hideTextModeEnabled,
                                onHideTextModeEnabledChange = onHideTextModeEnabledChange,
                                entryCount = entries.size,
                                totalWords = totalWords,
                                totalChars = totalChars,
                                showDayHeaderStats = showDayHeaderStats,
                                onPreviousDate = onPreviousDate,
                                onNextDate = onNextDate,
                                onSelectDate = onSelectDate,
                                swipeToNavigateDatesEnabled = swipeToNavigateDatesEnabled,
                                onEditDayLabel = onEditDayLabel,
                                dayLabel = dayLabel,
                                starredLabels = starredLabels,
                                onOpenVersionSnapshots = onOpenVersionSnapshots,
                                showVersionSnapshotsButton = showVersionSnapshotsButton,
                                versionSnapshotsCount = versionSnapshotsCount
                        )
                },
                floatingActionButton = {
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                                .padding(bottom = fabBottomPadding)
                        ) {
                                DeleteSelectedEntriesPill(
                                        selectedEntryCount = selectedEntryItemIds.size,
                                        onDeleteSelectedEntries = {
                                                val selectedIndices =
                                                        reorderedEntryItems.mapIndexedNotNull { index, item ->
                                                                index.takeIf { item.id in selectedEntryItemIds }
                                                        }.toSet()
                                                if (selectedIndices.isNotEmpty()) {
                                                        onClearEntrySelection()
                                                        onDeleteSelectedEntries(selectedIndices)
                                                }
                                        },
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                )
                                HomeFabCluster(
                                        selectedDate = selectedDate,
                                        drawerOpen = isDrawerOpen,
                                        allowFutureEntries = allowFutureEntries,
                                        onJumpToToday = onJumpToToday,
                                        onConfirmAddEntry = {
                                                onClearEditing()
                                                onNavigateToAddEntry()
                                        },
                                        onFutureDateAttempt = { showFutureDateDialog = true },
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                )
                        }
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
        ) { paddingValues ->
                val contentColumn: @Composable () -> Unit = {
                        AppScreenReveal(
                                visible = true,
                                key = Triple(selectedDate, searchQuery, activeKeywordFilter?.name),
                                modifier =
                                        Modifier.fillMaxSize().then(
                                                if (swipeToNavigateDatesEnabled) {
                                                        val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
                                                        Modifier.pointerInput(
                                                                selectedDate,
                                                                swipeToNavigateDatesEnabled
                                                        ) {
                                                                var totalDrag = 0f
                                                                detectHorizontalDragGestures(
                                                                        onDragStart = { totalDrag = 0f },
                                                                        onHorizontalDrag = { change, dragAmount ->
                                                                                change.consume()
                                                                                totalDrag += dragAmount
                                                                        },
                                                                        onDragEnd = {
                                                                                if (totalDrag < -50f) {
                                                                                        if (isRtl) onPreviousDate() else onNextDate()
                                                                                } else if (totalDrag > 50f) {
                                                                                        if (isRtl) onNextDate() else onPreviousDate()
                                                                                }
                                                                                totalDrag = 0f
                                                                        },
                                                                        onDragCancel = { totalDrag = 0f }
                                                                )
                                                        }
                                                } else {
                                                        Modifier
                                                }
                                        )
                        ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                        if (searchQuery.isNotEmpty()) {
                                                SearchResultsContent(
                                                        searchResults = searchResults,
                                                        onResultClicked = onResultClicked,
                                                        hideTextModeEnabled = hideTextModeEnabled
                                                )
                                        } else if (isLoading) {
                                                // Ignore empty loading state flash
                                        } else if (isKeywordFilterActive) {
                                                KeywordFilterResultsContent(
                                                        keywordFilteredEntries = keywordFilteredEntries.orEmpty(),
                                                        onDateLinkClick = onDateLinkClick
                                                )
                                        } else if (entries.isEmpty()) {
                                                HomeEmptyStateSection(
                                                        selectedDate = selectedDate,
                                                        dayLabel = dayLabel,
                                                        dueRevisits = dueRevisits,
                                                        allowFutureEntries = allowFutureEntries,
                                                        bottomPadding =
                                                                WindowInsets.navigationBars
                                                                        .asPaddingValues()
                                                                        .calculateBottomPadding(),
                                                        onOpenDueRevisit = onOpenDueRevisit,
                                                        onFutureDateBlocked = {
                                                                showFutureDateDialog = true
                                                        },
                                                        onAddEntry = {
                                                                onClearEditing()
                                                                onNavigateToAddEntry()
                                                        }
                                                )
                                        } else {
                                                HomeLoadedEntriesSection(
                                                        listState = listState,
                                                        reorderState = reorderState,
                                                        reorderedEntryItems = reorderedEntryItems,
                                                        contentPadding =
                                                                WindowInsets.navigationBars
                                                                        .asPaddingValues(),
                                                        dueRevisits = dueRevisits,
                                                        onOpenDueRevisit = onOpenDueRevisit,
                                                        enableDragAndDrop = enableDragAndDrop,
                                                        entryDeleteSelectionEnabled =
                                                                entryDeleteSelectionEnabled,
                                                        showTimestamps = showTimestamps,
                                                        hideTextModeEnabled = hideTextModeEnabled,
                                                        renderCheckboxesAsText = renderCheckboxesAsText,
                                                        isPreviewLimitEnabled =
                                                                isPreviewLimitEnabled,
                                                        previewLimitLength =
                                                                previewLimitLength,
                                                        onDeleteEntry = onDeleteEntry,
                                                        onStartEditingEntry = onStartEditing,
                                                        selectedEntryItemIds =
                                                                selectedEntryItemIds,
                                                        onClearEntrySelection =
                                                                onClearEntrySelection,
                                                        onToggleEntrySelection =
                                                                onToggleEntrySelection,
                                                        onStartEntrySelection =
                                                                onStartEntrySelection,
                                                        onNavigateToAddEntry =
                                                                onNavigateToAddEntry,
                                                        selectedDate = selectedDate,
                                                        onDateLinkClick = onDateLinkClick,
                                                        keywords = keywords,
                                                        monthFirst = monthFirst,
                                                        customDateKeywords =
                                                                customDateKeywords,
                                                        entryStyle = entryStyle
                                                )
                                        }
                                }
                        }
                }

        if (swipeToSyncEnabled) {
                        PullToRefreshBox(
                                isRefreshing = isLoading,
                                onRefresh = onRefreshCache,
                                modifier = Modifier.fillMaxSize().padding(paddingValues)
                        ) { contentColumn() }
                } else {
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                                contentColumn()
                        }
                }

                HomeScreenDialogs(
                        showFutureDateDialog = showFutureDateDialog,
                        selectedDate = selectedDate,
                        onDismissFutureDateDialog = { showFutureDateDialog = false },
                        onConfirmFutureDateDialog = {
                                showFutureDateDialog = false
                                onClearEditing()
                                onNavigateToAddEntry()
                        },
                        showCacheAnomalyDialog = showCacheAnomalyDialog,
                        onDismissCacheAnomalyDialog = onDismissAnomalyDialog,
                        onAcceptCacheAnomalyRefresh = onAcceptAnomalyRefresh
                )
        }
}

@Composable
internal fun DueRevisitCard(
        items: List<DueRevisitItem>,
        onOpenDate: (LocalDate) -> Unit
) {
        val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
        val visibleItems = remember(items) { items.take(2) }
        val remainingCount = (items.size - visibleItems.size).coerceAtLeast(0)
        ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors =
                        CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                ) {
                                        Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                        imageVector = Icons.Rounded.Today,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                        }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text =
                                                        pluralStringResource(
                                                                R.plurals.home_followup_due_today,
                                                                items.size,
                                                        ),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                text = stringResource(R.string.home_followup_subtitle),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                                AssistChip(
                                        onClick = {},
                                        label = { Text(items.size.toString()) },
                                        leadingIcon = {
                                                Icon(
                                                        imageVector = Icons.Rounded.HistoryEdu,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                )
                                        }
                                )
                        }
                        visibleItems.forEach { item ->
                                Surface(
                                        onClick = { onOpenDate(item.sourceDate) },
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = MaterialTheme.shapes.medium
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                                Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                        Text(
                                                                text =
                                                                        item.label.ifBlank {
                                                                                stringResource(R.string.home_from_date_format, item.sourceDate.format(dateFormatter))
                                                                        },
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Medium,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (item.note.isNotBlank()) {
                                                                Text(
                                                                        text = item.note,
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        maxLines = 2,
                                                                        overflow = TextOverflow.Ellipsis
                                                                )
                                                        }
                                                        AssistChip(
                                                                onClick = { onOpenDate(item.sourceDate) },
                                                                label = {
                                                                        Text(
                                                                                stringResource(R.string.home_from_date_format, item.sourceDate.format(dateFormatter))
                                                                        )
                                                                },
                                                                leadingIcon = {
                                                                        Icon(
                                                                                imageVector = Icons.Rounded.EditCalendar,
                                                                                contentDescription = null,
                                                                                modifier = Modifier.size(16.dp)
                                                                        )
                                                                }
                                                        )
                                                }
                                                Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                        }
                                }
                        }
                        if (remainingCount > 0) {
                                Text(
                                        text = stringResource(R.string.home_more_due_today_format, remainingCount),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
        }
}

