package com.mj.yaja.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.yaja.ui.components.AnimatedMenuButton
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.utils.MarkdownUtils
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit,
        onNavigateBack: () -> Unit,
        onNavigateToDate: (LocalDate) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favoritedDates by viewModel.favoritedDates.collectAsStateWithLifecycle()
    val starredLabels by viewModel.starredLabels.collectAsStateWithLifecycle()
    val revisitTargetDates by viewModel.revisitTargetDates.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(TimelineFilter.ALL) }
    var selectedDensity by remember { mutableStateOf(TimelineDensity.COMFORTABLE) }
    var selectedStyle by rememberSaveable { mutableStateOf(TimelineStyle.TRACK) }
    var showAllDates by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableStateOf(ALL_YEARS) }
    var labelQuery by remember { mutableStateOf("") }
    var jumpMenuExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var pendingJumpMonth by remember { mutableStateOf<YearMonth?>(null) }
    var forcedAnchorDate by remember { mutableStateOf<LocalDate?>(null) }
    var previewNode by remember { mutableStateOf<TimelineDateNode?>(null) }
    var previewText by remember { mutableStateOf<String?>(null) }
    val collapsedMonths = remember { mutableStateMapOf<YearMonth, Boolean>() }

    val today = remember { LocalDate.now() }
    val knownDates = remember(uiState.datesWithEntries, starredLabels, revisitTargetDates, today) {
        buildKnownTimelineDates(
            datesWithEntries = uiState.datesWithEntries,
            starredLabels = starredLabels,
            revisitTargetDates = revisitTargetDates,
            today = today
        )
    }
    val latestTimelineDate = remember(knownDates, today) { knownDates.firstOrNull() ?: today }
    val selectedOrToday = remember(uiState.selectedDate, today, latestTimelineDate) {
        when {
            uiState.selectedDate.isAfter(latestTimelineDate) -> latestTimelineDate
            else -> uiState.selectedDate
        }
    }
    val availableYears = remember(knownDates) {
        knownDates.map { it.year }.distinct().sortedDescending()
    }

    LaunchedEffect(availableYears) {
        if (selectedYear != ALL_YEARS && selectedYear.toIntOrNull() !in availableYears) {
            selectedYear = ALL_YEARS
        }
    }

    var metricsByDate by remember {
        mutableStateOf(emptyMap<LocalDate, com.mj.yaja.data.DailyJournalMetrics>())
    }
    LaunchedEffect(uiState.datesWithEntries) {
        metricsByDate = viewModel.getTimelineMetrics(uiState.datesWithEntries.sorted())
    }

    val timelineDates = remember(knownDates, showAllDates, latestTimelineDate) {
        buildTimelineDates(
            knownDates = knownDates,
            showAllDates = showAllDates,
            latestTimelineDate = latestTimelineDate
        )
    }

    val monthSections = remember(
        timelineDates,
        uiState.datesWithEntries,
        metricsByDate,
        favoritedDates,
        starredLabels,
        revisitTargetDates,
        selectedFilter,
        selectedYear,
        labelQuery,
        today
    ) {
        buildTimelineMonthSections(
            timelineDates = timelineDates,
            datesWithEntriesSource = uiState.datesWithEntries,
            metricsByDate = metricsByDate,
            favoritedDates = favoritedDates,
            starredLabels = starredLabels,
            revisitTargetDates = revisitTargetDates,
            selectedFilter = selectedFilter,
            selectedYear = selectedYear,
            labelQuery = labelQuery,
            today = today
        )
    }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val listState = rememberLazyListState()
    val anchorTargetDate = forcedAnchorDate ?: selectedOrToday
    val anchorDate = remember(monthSections, anchorTargetDate) {
        findNearestVisibleDate(monthSections.flatMap { it.nodes }.map { it.date }, anchorTargetDate)
    }

    LaunchedEffect(monthSections, anchorTargetDate) {
        anchorDate?.let { collapsedMonths[YearMonth.from(it)] = false }
    }

    LaunchedEffect(monthSections, collapsedMonths.toMap(), anchorDate, forcedAnchorDate) {
        anchorDate?.let { date ->
            findDateItemIndex(monthSections, collapsedMonths, date)?.let { listState.scrollToItem(it) }
            if (forcedAnchorDate != null) {
                forcedAnchorDate = null
            }
        }
    }

    LaunchedEffect(previewNode?.date) {
        previewText = previewNode?.date?.let { date ->
            viewModel.getTimelinePreview(date)?.let(MarkdownUtils::stripMetadata)
        }
    }

    LaunchedEffect(pendingJumpMonth, monthSections, collapsedMonths.toMap()) {
        pendingJumpMonth?.let { targetMonth ->
            findMonthHeaderIndex(monthSections, targetMonth)?.let { listState.scrollToItem(it) }
            pendingJumpMonth = null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Timeline",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    AnimatedMenuButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        AppScreenReveal(
            visible = true,
            key = listOf(selectedFilter.name, selectedYear, showAllDates, selectedDensity.name, selectedStyle.name, labelQuery),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            TimelineQuickActionsRow(
                onTodayClick = {
                    viewModel.selectDate(today)
                    selectedFilter = TimelineFilter.ALL
                    selectedYear = ALL_YEARS
                    labelQuery = ""
                    showAllDates = true
                    previewNode = null
                    previewText = null
                    collapsedMonths.clear()
                    forcedAnchorDate = today
                },
                onOpenMonthMenu = { jumpMenuExpanded = true },
                onOpenFilters = { showFilterSheet = true }
            )
            TimelineActiveFiltersRow(
                selectedFilter = selectedFilter,
                selectedYear = selectedYear,
                showAllDates = showAllDates,
                selectedDensity = selectedDensity,
                labelQuery = labelQuery
            )
            if (monthSections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Nothing here yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Try a different filter or adjust the label search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        monthSections.forEach { section ->
                            stickyHeader(key = "month_${section.yearMonth}") {
                                TimelineMonthHeader(
                                    title = section.yearMonth.format(monthFormatter).uppercase(Locale.getDefault()),
                                    summary = "${section.entryDayCount} entry days Â· ${section.labeledCount} labeled Â· ${section.favoriteCount} favorites",
                                    isCollapsed = collapsedMonths[section.yearMonth] == true,
                                    onToggle = {
                                        val currentlyCollapsed = collapsedMonths[section.yearMonth] == true
                                        collapsedMonths[section.yearMonth] = !currentlyCollapsed
                                    }
                                )
                            }

                            if (collapsedMonths[section.yearMonth] != true) {
                                items(
                                    items = section.nodes,
                                    key = { node -> "date_${node.date}" }
                                ) { node ->
                                    val nodeIndex = section.nodes.indexOf(node)
                                    AppStaggeredEntrance(
                                        visible = true,
                                        index = nodeIndex.coerceAtMost(6),
                                        strength = AppEntranceStrength.SUBTLE
                                    ) {
                                        TimelineDateRow(
                                            node = node,
                                            isFirst = node == section.nodes.firstOrNull(),
                                            isLast = node == section.nodes.lastOrNull(),
                                            density = selectedDensity,
                                            style = selectedStyle,
                                            onClick = { onNavigateToDate(node.date) },
                                            onLongClick = { previewNode = node }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = jumpMenuExpanded,
                        onDismissRequest = { jumpMenuExpanded = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .heightIn(max = 320.dp)
                    ) {
                        monthSections.forEach { section ->
                            DropdownMenuItem(
                                text = { Text(section.yearMonth.format(monthFormatter)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    collapsedMonths[section.yearMonth] = false
                                    jumpMenuExpanded = false
                                    pendingJumpMonth = section.yearMonth
                                }
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (previewNode != null) {
        ModalBottomSheet(
            onDismissRequest = {
                previewNode = null
                previewText = null
            }
        ) {
            TimelinePreviewSheet(
                node = previewNode!!,
                previewText = previewText,
                onOpenDate = {
                    val date = previewNode!!.date
                    previewNode = null
                    previewText = null
                    onNavigateToDate(date)
                }
            )
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            TimelineFilterSheet(
                labelQuery = labelQuery,
                onLabelQueryChange = { labelQuery = it },
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                years = availableYears,
                selectedYear = selectedYear,
                onYearSelected = { selectedYear = it },
                showAllDates = showAllDates,
                onShowAllDatesChanged = { showAllDates = it },
                selectedDensity = selectedDensity,
                onDensitySelected = { selectedDensity = it },
                selectedStyle = selectedStyle,
                onStyleSelected = { selectedStyle = it }
            )
        }
    }
}
