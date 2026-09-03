package com.mj.yaja.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.yaja.R
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.utils.MarkdownUtils
import com.mj.yaja.ui.viewmodel.JournalViewModel
import kotlinx.coroutines.async
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit,
        onNavigateToAddEntry: () -> Unit,
        onOpenToday: () -> Unit,
        onNavigateToTimeline: () -> Unit,
        onNavigateToStatistics: () -> Unit
) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val todos by viewModel.todos.collectAsStateWithLifecycle()
        val events by viewModel.events.collectAsStateWithLifecycle()
        val allTimeStats by viewModel.allTimeStats.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
                // Reuses the same freshness cache Statistics relies on, so repeatedly reopening
                // Home (e.g. via the long-press toggle) doesn't rescan the whole journal each time.
                viewModel.ensureStatisticsLoaded(StatisticsPeriod.ALL_TIME)
        }

        // Deliberately not `remember`ed: this screen can stay composed across a midnight
        // rollover (backgrounded overnight, timezone travel), and a frozen "today" would leave
        // the hero card, week strip, and streak filtering silently wrong until the next full
        // recomposition from an unrelated state change.
        val today = LocalDate.now()
        val now = LocalTime.now()
        val hasTodayEntry = today in uiState.datesWithEntries
        val hasAnyEntries = uiState.datesWithEntries.isNotEmpty()

        var todayPreview by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(hasTodayEntry, today) {
                todayPreview = if (hasTodayEntry) {
                        viewModel.getTimelinePreview(today)?.let { MarkdownUtils.stripMetadata(it).trim() }
                } else {
                        null
                }
        }

        val openTodosToday = remember(todos) { todos.count { it.date == today && !it.isChecked } }
        val eventsToday = remember(events) { events.count { it.date == today } }

        val showBottomBar by viewModel.showBottomBar.collectAsStateWithLifecycle()
        val navigationChromeMode by viewModel.navigationChromeMode.collectAsStateWithLifecycle()
        val showBottomPanelLabels by viewModel.showBottomPanelLabels.collectAsStateWithLifecycle()
        val fabBottomPadding =
                if (showBottomBar) {
                        when (navigationChromeMode) {
                                com.mj.yaja.data.NavigationChromeMode.EXPRESSIVE_PANEL -> {
                                        if (showBottomPanelLabels) 92.dp else 76.dp
                                }
                                com.mj.yaja.data.NavigationChromeMode.FLOATING_BAR -> 0.dp
                        }
                } else {
                        0.dp
                }

        // "Recent" excludes today (it's the hero) and any future-dated entries — those aren't
        // actually recent, they just sort first if allowed to slip into a plain date compare.
        val recentDates = remember(uiState.datesWithEntries, today) {
                uiState.datesWithEntries.filter { it != today && !it.isAfter(today) }
                        .sortedDescending()
                        .take(3)
        }
        // Todo counts are already in memory (`todos`), so they're derived synchronously and kept
        // out of the IO-bound effect below — otherwise checking a box anywhere in the app would
        // retrigger a disk read for every recent entry just to redraw an unrelated number.
        val recentTodoCounts = remember(todos, recentDates) {
                val byDate = todos.groupingBy { it.date }.eachCount()
                recentDates.associateWith { byDate[it] ?: 0 }
        }
        var recentDetails by remember { mutableStateOf<Map<LocalDate, DashboardRecentDetail>>(emptyMap()) }
        LaunchedEffect(recentDates) {
                if (recentDates.isEmpty()) {
                        recentDetails = emptyMap()
                        return@LaunchedEffect
                }
                // The metrics batch and each date's preview are independent disk reads — fire
                // them together instead of awaiting one at a time, which serialized what should
                // be a single round-trip's worth of latency into up to four.
                val metricsDeferred = async { viewModel.getTimelineMetrics(recentDates) }
                val previewDeferredByDate = recentDates.associateWith { date ->
                        async { viewModel.getTimelinePreview(date) }
                }
                val metrics = metricsDeferred.await()
                recentDetails = previewDeferredByDate.mapValues { (date, deferred) ->
                        DashboardRecentDetail(
                                preview = deferred.await()
                                        ?.let { MarkdownUtils.stripMetadata(it).trim() }
                                        .orEmpty(),
                                wordCount = metrics[date]?.wordCount ?: 0
                        )
                }
        }

        Scaffold(
                topBar = {
                        DashboardTopBar(
                                onOpenDrawer = onOpenDrawer,
                                onSearch = onOpenToday
                        )
                },
                floatingActionButton = {
                        androidx.compose.material3.FloatingActionButton(
                                onClick = onOpenToday,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(bottom = fabBottomPadding)
                        ) {
                                Icon(
                                        imageVector = Icons.Rounded.Today,
                                        contentDescription = stringResource(R.string.dashboard_goto_today)
                                )
                        }
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
        ) { paddingValues ->
                AppScreenReveal(visible = true, modifier = Modifier.fillMaxSize()) {
                        Column(
                                modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp)
                        ) {
                                Spacer(modifier = Modifier.height(4.dp))

                                DashboardTodayHeroCard(
                                        today = today,
                                        now = now,
                                        hasTodayEntry = hasTodayEntry,
                                        hasAnyEntries = hasAnyEntries,
                                        todayPreview = todayPreview,
                                        openTodosToday = openTodosToday,
                                        eventsToday = eventsToday,
                                        onWriteOrContinue = onNavigateToAddEntry,
                                        onOpenGlance = onOpenToday
                                )

                                Spacer(modifier = Modifier.height(22.dp))

                                DashboardWeekSection(
                                        today = today,
                                        datesWithEntries = uiState.datesWithEntries,
                                        currentStreak = allTimeStats?.currentStreak ?: 0,
                                        onOpenDate = { date ->
                                                viewModel.selectDate(date, source = "dashboard_week_strip")
                                                onOpenToday()
                                        }
                                )

                                Spacer(modifier = Modifier.height(22.dp))

                                DashboardOverviewRow(
                                        totalDaysWithEntries = allTimeStats?.totalDaysWithEntries ?: 0,
                                        totalEntries = allTimeStats?.totalEntries ?: 0,
                                        currentStreak = allTimeStats?.currentStreak ?: 0,
                                        onClick = onNavigateToStatistics
                                )

                                if (recentDates.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(22.dp))
                                        DashboardRecentSection(
                                                dates = recentDates,
                                                details = recentDetails,
                                                todoCounts = recentTodoCounts,
                                                onAllEntries = onNavigateToTimeline,
                                                onOpenDate = { date ->
                                                        viewModel.selectDate(date, source = "dashboard_recent_entry")
                                                        onOpenToday()
                                                }
                                        )
                                }

                                Spacer(modifier = Modifier.height(fabBottomPadding + 88.dp))
                        }
                }
        }
}

private data class DashboardRecentDetail(
        val preview: String,
        val wordCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(onOpenDrawer: () -> Unit, onSearch: () -> Unit) {
        Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
        ) {
                Column(
                        modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                        Row(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .height(58.dp)
                                        .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(44.dp)) {
                                        Icon(
                                                imageVector = Icons.Rounded.Menu,
                                                contentDescription = stringResource(R.string.nav_cd_menu),
                                                tint = MaterialTheme.colorScheme.onSurface
                                        )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                        text = stringResource(R.string.dashboard_title),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = 19.sp,
                                                letterSpacing = (-0.19).sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onSearch, modifier = Modifier.size(44.dp)) {
                                        Icon(
                                                imageVector = Icons.Rounded.Search,
                                                contentDescription = stringResource(R.string.home_cd_search),
                                                tint = MaterialTheme.colorScheme.onSurface
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun DashboardTodayHeroCard(
        today: LocalDate,
        now: LocalTime,
        hasTodayEntry: Boolean,
        hasAnyEntries: Boolean,
        todayPreview: String?,
        openTodosToday: Int,
        eventsToday: Int,
        onWriteOrContinue: () -> Unit,
        onOpenGlance: () -> Unit
) {
        val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE '·' d MMMM", Locale.getDefault()) }
        val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
        // Derived from `now` (the same instant already shown in the meta row) rather than a
        // separately cached clock read, so it can't drift out of sync with the displayed time
        // or freeze on the hour it first composed at.
        val greeting = when (now.hour) {
                in 5..11 -> R.string.dashboard_greeting_morning
                in 12..16 -> R.string.dashboard_greeting_afternoon
                in 17..21 -> R.string.dashboard_greeting_evening
                else -> R.string.dashboard_greeting_night
        }
        val statusText = when {
                !hasTodayEntry -> stringResource(R.string.dashboard_status_empty)
                todayPreview.isNullOrBlank() -> stringResource(R.string.dashboard_status_empty)
                else -> todayPreview.take(60)
        }

        Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
        ) {
                Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                        ) {
                                Text(
                                        text = today.format(dateFormatter).uppercase(Locale.getDefault()),
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.7.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                        text = now.format(timeFormatter),
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                                text = stringResource(greeting),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = 25.sp,
                                        letterSpacing = (-0.25).sp,
                                        lineHeight = 27.5.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(7.dp))

                        Text(
                                text = buildAnnotatedString {
                                        if (hasTodayEntry && !todayPreview.isNullOrBlank()) {
                                                append("“$statusText")
                                                if (todayPreview.length > 60) {
                                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                                                append("▌")
                                                        }
                                                } else {
                                                        append("”")
                                                }
                                        } else {
                                                append(statusText)
                                        }
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.85.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                                onClick = onWriteOrContinue,
                                color = MaterialTheme.colorScheme.primary,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Icon(
                                                imageVector = if (hasTodayEntry) Icons.Rounded.EditNote else Icons.Rounded.Edit,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                                text = stringResource(
                                                        when {
                                                                !hasAnyEntries -> R.string.dashboard_write_first_entry
                                                                hasTodayEntry -> R.string.dashboard_continue_today
                                                                else -> R.string.dashboard_write_today
                                                        }
                                                ),
                                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(
                                        modifier = Modifier
                                                .weight(1f)
                                                .clickable(onClick = onOpenGlance),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        DashboardGlanceGroup(
                                                icon = Icons.Rounded.Checklist,
                                                text = stringResource(R.string.dashboard_glance_todos_format, openTodosToday),
                                                active = openTodosToday > 0
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        DashboardGlanceGroup(
                                                icon = Icons.Rounded.Event,
                                                text = stringResource(R.string.dashboard_glance_events_format, eventsToday),
                                                active = eventsToday > 0
                                        )
                                }
                                Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                )
                        }
                }
        }
}

@Composable
private fun DashboardGlanceGroup(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        text: String,
        active: Boolean
) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(if (active) 1f else 0.55f)
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }
}

@Composable
private fun DashboardWeekSection(
        today: LocalDate,
        datesWithEntries: Set<LocalDate>,
        currentStreak: Int,
        onOpenDate: (LocalDate) -> Unit
) {
        val days = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
        val writtenCount = remember(days, datesWithEntries) { days.count { it in datesWithEntries } }

        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
        ) {
                Text(
                        text = stringResource(R.string.dashboard_section_this_week),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.7.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = buildAnnotatedString {
                                append(
                                        stringResource(
                                                R.string.dashboard_week_summary_format,
                                                writtenCount
                                        )
                                )
                                append(" · ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                        append(
                                                stringResource(
                                                        R.string.dashboard_streak_summary_format,
                                                        currentStreak
                                                )
                                        )
                                }
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
                days.forEach { date ->
                        val isWritten = date in datesWithEntries
                        val isToday = date == today
                        Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                Text(
                                        text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.6.sp),
                                        color = if (isToday) {
                                                MaterialTheme.colorScheme.primary
                                        } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                )
                                Spacer(modifier = Modifier.height(7.dp))
                                Box(
                                        modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        if (isWritten) {
                                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                                        } else {
                                                                androidx.compose.ui.graphics.Color.Transparent
                                                        }
                                                )
                                                .then(
                                                        when {
                                                                isToday -> Modifier.border(
                                                                        width = 2.dp,
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        shape = CircleShape
                                                                )
                                                                !isWritten -> Modifier.border(
                                                                        width = 1.dp,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f),
                                                                        shape = CircleShape
                                                                )
                                                                else -> Modifier
                                                        }
                                                )
                                                .clickable(onClick = { onOpenDate(date) }),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(
                                                text = date.dayOfMonth.toString(),
                                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                                fontWeight = if (isWritten) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                        isToday -> MaterialTheme.colorScheme.primary
                                                        isWritten -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                }
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun DashboardOverviewRow(
        totalDaysWithEntries: Int,
        totalEntries: Int,
        currentStreak: Int,
        onClick: () -> Unit
) {
        val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                DashboardStatTile(
                        modifier = Modifier.weight(1f),
                        value = numberFormat.format(totalDaysWithEntries),
                        label = stringResource(R.string.dashboard_stat_days),
                        onClick = onClick
                )
                DashboardStatTile(
                        modifier = Modifier.weight(1f),
                        value = numberFormat.format(totalEntries),
                        label = stringResource(R.string.dashboard_stat_entries),
                        onClick = onClick
                )
                DashboardStatTile(
                        modifier = Modifier.weight(1f),
                        value = numberFormat.format(currentStreak),
                        label = stringResource(R.string.dashboard_stat_streak),
                        onClick = onClick
                )
        }
}

@Composable
private fun DashboardStatTile(
        modifier: Modifier = Modifier,
        value: String,
        label: String,
        onClick: () -> Unit
) {
        Surface(
                onClick = onClick,
                modifier = modifier,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 13.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Text(
                                text = value,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.5.sp, letterSpacing = (-0.37).sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = label.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 1.14.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }
        }
}

@Composable
private fun DashboardRecentSection(
        dates: List<LocalDate>,
        details: Map<LocalDate, DashboardRecentDetail>,
        todoCounts: Map<LocalDate, Int>,
        onAllEntries: () -> Unit,
        onOpenDate: (LocalDate) -> Unit
) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Text(
                        text = stringResource(R.string.dashboard_recent_entries_title),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.7.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onAllEntries)
                ) {
                        Text(
                                text = stringResource(R.string.dashboard_all_entries),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                        )
                }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
                dates.forEachIndexed { index, date ->
                        val detail = details[date]
                        DashboardRecentRow(
                                date = date,
                                detail = detail,
                                todoCount = todoCounts[date] ?: 0,
                                onClick = { onOpenDate(date) }
                        )
                        if (index != dates.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
                                )
                        }
                }
        }
}

@Composable
private fun DashboardRecentRow(
        date: LocalDate,
        detail: DashboardRecentDetail?,
        todoCount: Int,
        onClick: () -> Unit
) {
        val weekdayFormatter = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClick)
                        .padding(horizontal = 4.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Column(
                        modifier = Modifier.width(30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Text(
                                text = date.format(weekdayFormatter).uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 0.95.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 20.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                }
                Spacer(modifier = Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = detail?.preview.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 18.75.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                        text = stringResource(
                                                R.string.dashboard_words_count_format,
                                                detail?.wordCount ?: 0
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (todoCount > 0) {
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                                imageVector = Icons.Rounded.Checklist,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                                text = todoCount.toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                        }
                }
        }
}
