package com.mj.yaja.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.ui.utils.MarkdownUtils
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookbackScreen(
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit,
        onNavigateToDate: (LocalDate) -> Unit,
        onNavigateToJournal: () -> Unit,
        onNavigateToCalendar: () -> Unit,
        onNavigateToLookback: () -> Unit,
        onNavigateToShortcodes: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToHelp: () -> Unit
) {
        val uiState by viewModel.uiState.collectAsState()
        val flashbacks = uiState.lookbackEntries
        val isPreviewLimitEnabled by viewModel.isPreviewLimitEnabled.collectAsState()
        val previewLimitLength by viewModel.previewLimitLength.collectAsState()
        val monthlyStats by viewModel.monthlyStats.collectAsState()

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                "Lookback",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                },
                                navigationIcon = {
                                        com.mj.yaja.ui.components.AnimatedMenuButton(
                                                onClick = onOpenDrawer,
                                                modifier = Modifier.padding(start = 8.dp)
                                        )
                                },
                                colors =
                                        TopAppBarDefaults.topAppBarColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.background,
                                                titleContentColor =
                                                        MaterialTheme.colorScheme.primary,
                                                navigationIconContentColor =
                                                        MaterialTheme.colorScheme.onSurface,
                                                actionIconContentColor =
                                                        MaterialTheme.colorScheme.onSurface
                                        )
                        )
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
        ) { paddingValues ->
                LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        // Monthly Recap Card
                        item {
                                monthlyStats?.let { stats ->
                                        MonthlyRecapCard(stats = stats)
                                }
                        }

                        if (flashbacks.isNotEmpty()) {
                                item {
                                        SectionHeader(
                                                title = "On this day in past years",
                                                icon = Icons.Rounded.History
                                        )
                                }

                                flashbacks.entries.forEachIndexed { index, entryMap ->
                                        val yearsAgo = entryMap.key
                                        val entries = entryMap.value
                                        item(key = "flashback_$yearsAgo") {
                                                var visible by remember { mutableStateOf(false) }
                                                LaunchedEffect(Unit) { visible = true }

                                                AnimatedVisibility(
                                                        visible = visible,
                                                        enter =
                                                                fadeIn(
                                                                        tween(
                                                                                400,
                                                                                delayMillis =
                                                                                        index * 100
                                                                        )
                                                                ) +
                                                                        slideInVertically(
                                                                                tween(
                                                                                        400,
                                                                                        delayMillis =
                                                                                                index *
                                                                                                        100
                                                                                )
                                                                        ) { it / 2 }
                                                ) {
                                                        FlashbackCard(
                                                                yearsAgo = yearsAgo,
                                                                date =
                                                                        uiState.selectedDate
                                                                                .minusYears(
                                                                                        yearsAgo.toLong()
                                                                                ),
                                                                entries = entries,
                                                                isPreviewLimitEnabled =
                                                                        isPreviewLimitEnabled,
                                                                previewLimitLength =
                                                                        previewLimitLength,
                                                                onClick = {
                                                                        onNavigateToDate(
                                                                                uiState.selectedDate
                                                                                        .minusYears(
                                                                                                yearsAgo.toLong()
                                                                                        )
                                                                        )
                                                                }
                                                        )
                                                }
                                        }
                                }
                        }

                        if (uiState.favoritedHighlights.isNotEmpty()) {
                                item {
                                        SectionHeader(
                                                title = "Your Highlights",
                                                icon = Icons.Rounded.Star
                                        )
                                }

                                itemsIndexed(
                                        uiState.favoritedHighlights,
                                        key = { _, date -> "highlight_$date" }
                                ) { index, date ->
                                        var visible by remember { mutableStateOf(false) }
                                        LaunchedEffect(Unit) { visible = true }

                                        AnimatedVisibility(
                                                visible = visible,
                                                enter =
                                                        fadeIn(
                                                                tween(400, delayMillis = index * 80)
                                                        ) +
                                                                slideInVertically(
                                                                        tween(
                                                                                400,
                                                                                delayMillis =
                                                                                        index * 80
                                                                        )
                                                                ) { it / 2 }
                                        ) {
                                                HighlightCard(
                                                        date = date,
                                                        onClick = { onNavigateToDate(date) }
                                                )
                                        }
                                }
                        }

                        if (flashbacks.isEmpty() && uiState.favoritedHighlights.isEmpty()) {
                                item {
                                        Box(
                                                modifier = Modifier.fillParentMaxSize(),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                val infiniteTransition =
                                                        rememberInfiniteTransition(
                                                                label = "EmptyState"
                                                        )
                                                val scale by
                                                        infiniteTransition.animateFloat(
                                                                initialValue = 1f,
                                                                targetValue = 1.05f,
                                                                animationSpec =
                                                                        infiniteRepeatable(
                                                                                animation =
                                                                                        tween(
                                                                                                2000,
                                                                                                easing =
                                                                                                        FastOutSlowInEasing
                                                                                        ),
                                                                                repeatMode =
                                                                                        RepeatMode
                                                                                                .Reverse
                                                                        ),
                                                                label = "IconBreathing"
                                                        )

                                                Column(
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Rounded.History,
                                                                contentDescription = null,
                                                                modifier =
                                                                        Modifier.size(64.dp)
                                                                                .scale(scale),
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(alpha = 0.2f)
                                                        )
                                                        Spacer(Modifier.height(16.dp))
                                                        Text(
                                                                "No lookbacks or highlights yet.",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleMedium.copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(alpha = 0.6f)
                                                        )
                                                        Text(
                                                                "Favorite important days to see them here!",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(alpha = 0.4f),
                                                                textAlign =
                                                                        androidx.compose.ui.text
                                                                                .style.TextAlign
                                                                                .Center
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        val scale by
                animateFloatAsState(
                        targetValue = if (visible) 1f else 0.5f,
                        animationSpec =
                                spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                ),
                        label = "HeaderIconScale"
                )

        val alpha by
                animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = tween(600),
                        label = "HeaderAlpha"
                )

        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp).alpha(alpha)
        ) {
                Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp).scale(scale)
                ) {
                        Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                        ) {
                                Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                )
                        }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                )
        }
}

@Composable
fun FlashbackCard(
        yearsAgo: Int,
        date: LocalDate,
        entries: List<String>,
        isPreviewLimitEnabled: Boolean,
        previewLimitLength: Int,
        onClick: () -> Unit
) {
        val infiniteTransition = rememberInfiniteTransition(label = "BadgePulse")
        val pulseScale by
                infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.06f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(1500, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "PulseEffect"
                )

        ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                colors =
                        CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
                Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text =
                                                "$yearsAgo ${if (yearsAgo == 1) "YEAR" else "YEARS"} AGO",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.scale(pulseScale)
                                )
                                Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(24.dp)
                                )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                                text = date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM")),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                text = date.year.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.7f
                                        )
                        )
                        Spacer(Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                entries.take(2).forEach { entry ->
                                        val cleanEntry = MarkdownUtils.stripMetadata(entry)
                                        Row(verticalAlignment = Alignment.Top) {
                                                Box(
                                                        modifier =
                                                                Modifier.padding(top = 8.dp)
                                                                        .size(6.dp)
                                                                        .background(
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.5f
                                                                                        ),
                                                                                CircleShape
                                                                        )
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                        text =
                                                                MarkdownUtils.parseMarkdown(
                                                                        if (isPreviewLimitEnabled &&
                                                                                        cleanEntry
                                                                                                .length >
                                                                                                previewLimitLength
                                                                        ) {
                                                                                cleanEntry.take(
                                                                                        previewLimitLength
                                                                                ) + "...."
                                                                        } else {
                                                                                cleanEntry
                                                                        }
                                                                ),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        maxLines = 2,
                                                        overflow =
                                                                androidx.compose.ui.text.style
                                                                        .TextOverflow.Ellipsis,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                }
                        }
                        if (entries.size > 2) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                        text = "+ ${entries.size - 2} more memories",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color =
                                                MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.8f
                                                ),
                                        modifier = Modifier.padding(start = 18.dp)
                                )
                        }
                }
        }
}

@Composable
fun HighlightCard(date: LocalDate, onClick: () -> Unit) {
        val infiniteTransition = rememberInfiniteTransition(label = "StarTwinkle")
        val alpha by
                infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "TwinkleAlpha"
                )

        ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                colors =
                        CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
                Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Surface(
                                color = Color(0xFFFFD700).copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                        ) {
                                Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                ) {
                                        Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD700).copy(alpha = alpha),
                                                modifier = Modifier.size(24.dp)
                                        )
                                }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                                Text(
                                        text =
                                                date.format(
                                                        DateTimeFormatter.ofPattern("EEEE, dd MMMM")
                                                ),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                        text = date.year.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.5f
                                                )
                                )
                        }
                }
        }
}

data class MonthlyStatsData(
        val entriesCount: Int,
        val wordCount: Int,
        val mostActiveDay: String?,
        val longestStreak: Int
)

@Composable
fun MonthlyRecapCard(stats: MonthlyStatsData) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        val scale by animateFloatAsState(
                targetValue = if (visible) 1f else 0.8f,
                animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                ),
                label = "RecapCardScale"
        )

        val alpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(400),
                label = "RecapCardAlpha"
        )

        ElevatedCard(
                modifier = Modifier.fillMaxWidth().scale(scale).alpha(alpha),
                colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
                Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                                Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(40.dp)
                                ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Icon(
                                                        imageVector = Icons.Rounded.TrendingUp,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                )
                                        }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                        "This Month's Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                        }

                        // Stats Grid
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Entries Count
                                        Box(
                                                modifier = Modifier.weight(1f)
                                                        .background(
                                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                                MaterialTheme.shapes.medium
                                                        )
                                                        .padding(12.dp)
                                        ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                                stats.entriesCount.toString(),
                                                                style = MaterialTheme.typography.headlineSmall,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                                "Entries",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                }
                                        }

                                        // Word Count
                                        Box(
                                                modifier = Modifier.weight(1f)
                                                        .background(
                                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                                MaterialTheme.shapes.medium
                                                        )
                                                        .padding(12.dp)
                                        ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                                stats.wordCount.toString(),
                                                                style = MaterialTheme.typography.headlineSmall,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                                "Words",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                }
                                        }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Most Active Day
                                        Box(
                                                modifier = Modifier.weight(1f)
                                                        .background(
                                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                                MaterialTheme.shapes.medium
                                                        )
                                                        .padding(12.dp)
                                        ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                                stats.mostActiveDay?.take(3) ?: "—",
                                                                style = MaterialTheme.typography.headlineSmall,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                                "Best Day",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                }
                                        }

                                        // Longest Streak
                                        Box(
                                                modifier = Modifier.weight(1f)
                                                        .background(
                                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                                MaterialTheme.shapes.medium
                                                        )
                                                        .padding(12.dp)
                                        ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                        "🔥",
                                                                        style = MaterialTheme.typography.labelLarge
                                                                )
                                                                Spacer(Modifier.width(4.dp))
                                                                Text(
                                                                        stats.longestStreak.toString(),
                                                                        style = MaterialTheme.typography.headlineSmall,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = MaterialTheme.colorScheme.primary
                                                                )
                                                        }
                                                        Text(
                                                                "Streak",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}
