package com.mj.yaja.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.data.NavigationChromeMode
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.expressiveFabMotion
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.ui.design.floatSpring
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.scaledDuration
import com.mj.yaja.ui.design.rememberAppEntrance
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
        onNavigateToReview: (ReviewPeriodType) -> Unit,
        onSurpriseMeNavigate: (LocalDate) -> Unit
) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val flashbacks = uiState.lookbackEntries
        val isPreviewLimitEnabled by viewModel.isPreviewLimitEnabled.collectAsStateWithLifecycle()
        val previewLimitLength by viewModel.previewLimitLength.collectAsStateWithLifecycle()
        val starredLabels by viewModel.starredLabels.collectAsStateWithLifecycle()
        val dateOrderPreference by viewModel.dateOrderPreference.collectAsStateWithLifecycle()
        val monthFirst = com.mj.yaja.ui.utils.DateLinkUtils.resolveMonthFirst(dateOrderPreference)
        val customDateKeywords by viewModel.customDateKeywords.collectAsStateWithLifecycle()
        val showBottomBar by viewModel.showBottomBar.collectAsStateWithLifecycle()
        val navigationChromeMode by viewModel.navigationChromeMode.collectAsStateWithLifecycle()
        val showBottomPanelLabels by viewModel.showBottomPanelLabels.collectAsStateWithLifecycle()
        val fabPlacement by viewModel.fabPlacement.collectAsStateWithLifecycle()
        val entranceTriggered = rememberAppEntrance()
        val motionPreference = LocalAnimationPreference.current
        val listState = rememberLazyListState()
        val showSurpriseFab by remember {
                derivedStateOf {
                        listState.firstVisibleItemIndex == 0 || !listState.isScrollInProgress
                }
        }
        val surpriseFabInteraction = remember { MutableInteractionSource() }
        val fabBottomPadding =
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

        LaunchedEffect(uiState.selectedDate) {
                viewModel.ensureLookbackLoaded(force = true)
        }

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
                floatingActionButton = {
                        Box(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = fabBottomPadding)
                        ) {
                                LookbackSurpriseFab(
                                        visible = showSurpriseFab,
                                        motionPreference = motionPreference,
                                        interactionSource = surpriseFabInteraction,
                                        onClick = {
                                                viewModel.surpriseMe()?.let { onSurpriseMeNavigate(it) }
                                        },
                                        modifier = Modifier.align(fabPlacement.fabAlignment())
                                )
                        }
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
        ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        AppScreenReveal(
                                visible = true,
                                key = uiState.selectedDate,
                                modifier = Modifier.fillMaxSize()
                        ) {
                                LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                        start = 16.dp,
                                        top = 16.dp,
                                        end = 16.dp,
                                        bottom = 120.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                                item {
                                        LookbackFlashbacksSection(
                                                flashbacks = flashbacks,
                                                selectedDate = uiState.selectedDate,
                                                entranceTriggered = entranceTriggered,
                                                isPreviewLimitEnabled = isPreviewLimitEnabled,
                                                previewLimitLength = previewLimitLength,
                                                onNavigateToDate = onNavigateToDate,
                                                monthFirst = monthFirst,
                                                customKeywords = customDateKeywords
                                        )
                                }

                                item {
                                        LookbackHighlightsSection(
                                                favoritedHighlights = uiState.favoritedHighlights,
                                                starredLabels = starredLabels,
                                                entranceTriggered = entranceTriggered,
                                                onNavigateToDate = onNavigateToDate
                                        )
                                }

                                item {
                                        LookbackReviewSection(
                                                onNavigateToReview = onNavigateToReview
                                        )
                                }

                                if (flashbacks.isEmpty() && uiState.favoritedHighlights.isEmpty()) {
                                        item {
                                                LookbackEmptyState()
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
                                LocalAnimationPreference.current.floatSpring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                ),
                        label = "HeaderIconScale"
                )

        val alpha by
                animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = LocalAnimationPreference.current.floatTween(600),
                        label = "HeaderAlpha"
                )

        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 18.dp, bottom = 14.dp).alpha(alpha)
        ) {
                Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.size(40.dp).scale(scale)
                ) {
                        Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                        ) {
                                Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                )
                        }
                }
                Spacer(Modifier.width(14.dp))
                Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
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
        onClick: () -> Unit,
        onDateLinkClick: ((LocalDate) -> Unit)? = null,
        monthFirst: Boolean = com.mj.yaja.ui.utils.DateLinkUtils.isMonthFirst(),
        customKeywords: List<com.mj.yaja.data.DateKeywordEntry> = emptyList()
) {
        ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                colors =
                        CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
                Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(16.dp)
                                ) {
                                        Text(
                                                text =
                                                        "$yearsAgo ${if (yearsAgo == 1) "YEAR" else "YEARS"} AGO",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                letterSpacing = 0.8.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                }
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
                                                val previewText =
                                                        if (isPreviewLimitEnabled &&
                                                                        cleanEntry.length >
                                                                                previewLimitLength
                                                        ) {
                                                                cleanEntry.take(previewLimitLength) + "...."
                                                        } else {
                                                                cleanEntry
                                                        }
                                                val linkColor = MaterialTheme.colorScheme.primary
                                                Text(
                                                        text = if (onDateLinkClick != null) {
                                                                MarkdownUtils.parseMarkdownWithDateLinks(
                                                                        text = previewText,
                                                                        entryDate = date,
                                                                        linkColor = linkColor,
                                                                        monthFirst = monthFirst,
                                                                        customKeywords = customKeywords,
                                                                        onDateClick = onDateLinkClick
                                                                )
                                                        } else {
                                                                MarkdownUtils.parseMarkdown(previewText)
                                                        },
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
fun HighlightCard(date: LocalDate, onClick: () -> Unit, label: String = "") {
        val animationPreference = LocalAnimationPreference.current
        val alpha = if (animationPreference == AnimationPreference.OFF) {
                1f
        } else {
                val infiniteTransition = rememberInfiniteTransition(label = "StarTwinkle")
                val startAlpha = if (animationPreference == AnimationPreference.REDUCED) 0.85f else 0.6f
                val duration = if (animationPreference == AnimationPreference.REDUCED) 2000 else 1000
                val alphaState by
                        infiniteTransition.animateFloat(
                                initialValue = startAlpha,
                                targetValue = 1f,
                                animationSpec =
                                        infiniteRepeatable(
                                                animation = tween(duration, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse
                                        ),
                                label = "TwinkleAlpha"
                        )
                alphaState
        }

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
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.size(42.dp)
                        ) {
                                Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                ) {
                                        Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = alpha),
                                                modifier = Modifier.size(24.dp)
                                        )
                                }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text =
                                                date.format(
                                                        DateTimeFormatter.ofPattern("EEEE, dd MMMM")
                                                ),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                if (label.isNotEmpty()) {
                                        Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1
                                        )
                                }
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
