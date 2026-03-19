package com.mj.yaja.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.data.SwipeDirection
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit,
        onNavigateToJournal: () -> Unit,
        onNavigateToCalendar: () -> Unit,
        onNavigateToAddEntry: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToHelp: () -> Unit,
        onNavigateToLookback: () -> Unit,
        onNavigateToShortcodes: () -> Unit
) {
        val uiState by viewModel.uiState.collectAsState()
        val showTimestamps by viewModel.showTimestamps.collectAsState()
        val swipeToDeleteEnabled by viewModel.swipeToDeleteEnabled.collectAsState()
        val swipeDeleteDirection by viewModel.swipeDeleteDirection.collectAsState()
        val favoritedDates by viewModel.favoritedDates.collectAsState()
        val isFavorited = favoritedDates.contains(uiState.selectedDate.toString())
        val lastDeleted by viewModel.lastDeleted.collectAsState()

        // Countdown state for undo bar (5 seconds)
        val undoCountdown = remember { Animatable(1f) }
        LaunchedEffect(lastDeleted) {
                if (lastDeleted != null) {
                        undoCountdown.snapTo(1f)
                        undoCountdown.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
                        )
                        viewModel.clearLastDeleted()
                }
        }

        val context = androidx.compose.ui.platform.LocalContext.current
        LaunchedEffect(Unit) {
                viewModel.toastEvents.collect { message ->
                        android.widget.Toast.makeText(
                                        context,
                                        message,
                                        android.widget.Toast.LENGTH_SHORT
                                )
                                .show()
                }
        }

        val allowFutureEntries by viewModel.allowFutureEntries.collectAsState()
        val swipeToSyncEnabled by viewModel.swipeToSyncEnabled.collectAsState()
        val isPreviewLimitEnabled by viewModel.isPreviewLimitEnabled.collectAsState()
        val previewLimitLength by viewModel.previewLimitLength.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
                HomeScreenContent(
                        selectedDate = uiState.selectedDate,
                        isLoading = uiState.isLoading,
                        entries = uiState.entries,
                        onDeleteEntry = { index -> viewModel.deleteEntry(index) },
                        onOpenDrawer = onOpenDrawer,
                        onNavigateToCalendar = onNavigateToCalendar,
                        onNavigateToAddEntry = onNavigateToAddEntry,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToHelp = onNavigateToHelp,
                        onNavigateToLookback = onNavigateToLookback,
                        onNavigateToShortcodes = onNavigateToShortcodes,
                        onNavigateToJournal = onNavigateToJournal,
                        onPreviousDate = {
                                viewModel.selectDate(uiState.selectedDate.minusDays(1))
                        },
                        onNextDate = { viewModel.selectDate(uiState.selectedDate.plusDays(1)) },
                        onJumpToToday = { viewModel.selectDate(LocalDate.now()) },
                        onStartEditing = { entry, index -> viewModel.startEditing(entry, index) },
                        onClearEditing = { viewModel.clearEditing() },
                        searchQuery = uiState.searchQuery,
                        searchResults = uiState.searchResults,
                        onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
                        onClearSearch = { viewModel.clearSearch() },
                        showTimestamps = showTimestamps,
                        isFavorited = isFavorited,
                        onToggleFavorite = { viewModel.toggleFavorite(uiState.selectedDate) },
                        allowFutureEntries = allowFutureEntries,
                        swipeToDeleteEnabled = swipeToDeleteEnabled,
                        swipeDeleteDirection = swipeDeleteDirection,
                        swipeToSyncEnabled = swipeToSyncEnabled,
                        onRefreshCache = { viewModel.refreshCache() },
                        onResultClicked = { date ->
                                viewModel.clearSearch()
                                viewModel.selectDate(date)
                        },
                        onDismissAnomalyDialog = { viewModel.dismissCacheAnomalyDialog() },
                        onAcceptAnomalyRefresh = { viewModel.acceptCacheAnomalyRefresh() },
                        showCacheAnomalyDialog = uiState.showCacheAnomalyDialog,
                        isPreviewLimitEnabled = isPreviewLimitEnabled,
                        previewLimitLength = previewLimitLength
                )

                // UNDO bar — full-width rectangle at the bottom with countdown + UNDO button
                AnimatedVisibility(
                        visible = lastDeleted != null,
                        enter =
                                slideInVertically(
                                        spring(
                                                dampingRatio = 0.7f,
                                                stiffness = Spring.StiffnessMediumLow
                                        )
                                ) { it } + fadeIn(tween(200)),
                        exit =
                                slideOutVertically(spring(stiffness = Spring.StiffnessMediumLow)) {
                                        it
                                } + fadeOut(tween(150)),
                        modifier =
                                Modifier.align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(bottom = 80.dp)
                ) {
                        Surface(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                tonalElevation = 6.dp,
                                shadowElevation = 8.dp
                        ) {
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(
                                                                horizontal = 16.dp,
                                                                vertical = 10.dp
                                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        // Left: circular countdown timer
                                        Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.size(36.dp)
                                        ) {
                                                CircularProgressIndicator(
                                                        progress = { undoCountdown.value },
                                                        modifier = Modifier.size(36.dp),
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .inverseOnSurface,
                                                        trackColor =
                                                                MaterialTheme.colorScheme
                                                                        .inverseOnSurface.copy(
                                                                        alpha = 0.2f
                                                                ),
                                                        strokeWidth = 3.dp
                                                )
                                                Text(
                                                        text =
                                                                "${kotlin.math.ceil((undoCountdown.value * 5).toDouble()).toInt()}",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .inverseOnSurface
                                                )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Center: deleted label
                                        Text(
                                                text = "Entry deleted",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color =
                                                        MaterialTheme.colorScheme.inverseOnSurface
                                                                .copy(alpha = 0.8f),
                                                modifier = Modifier.weight(1f)
                                        )

                                        // Right: UNDO button
                                        TextButton(
                                                onClick = { viewModel.undoDelete() },
                                                colors =
                                                        ButtonDefaults.textButtonColors(
                                                                contentColor =
                                                                        MaterialTheme.colorScheme
                                                                                .inversePrimary
                                                        )
                                        ) {
                                                Text(
                                                        text = "UNDO",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        letterSpacing = 1.sp
                                                )
                                        }
                                }
                        }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
        selectedDate: LocalDate,
        isLoading: Boolean,
        entries: List<String>,
        onDeleteEntry: (Int) -> Unit,
        onOpenDrawer: () -> Unit,
        onNavigateToCalendar: () -> Unit,
        onNavigateToAddEntry: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToHelp: () -> Unit,
        onNavigateToLookback: () -> Unit,
        onNavigateToShortcodes: () -> Unit,
        onNavigateToJournal: () -> Unit,
        onPreviousDate: () -> Unit,
        onNextDate: () -> Unit,
        onJumpToToday: () -> Unit,
        onStartEditing: (String, Int) -> Unit,
        onClearEditing: () -> Unit,
        searchQuery: String = "",
        searchResults: List<com.mj.yaja.data.SearchResult> = emptyList(),
        onSearchQueryChanged: (String) -> Unit = {},
        onClearSearch: () -> Unit = {},
        showTimestamps: Boolean = true,
        isFavorited: Boolean = false,
        onToggleFavorite: () -> Unit = {},
        allowFutureEntries: Boolean = false,
        swipeToDeleteEnabled: Boolean = true,
        swipeDeleteDirection: SwipeDirection = SwipeDirection.END_TO_START,
        swipeToSyncEnabled: Boolean = true,
        onRefreshCache: () -> Unit = {},
        onResultClicked: (LocalDate) -> Unit = {},
        showCacheAnomalyDialog: Boolean = false,
        onDismissAnomalyDialog: () -> Unit = {},
        onAcceptAnomalyRefresh: () -> Unit = {},
        isPreviewLimitEnabled: Boolean = true,
        previewLimitLength: Int = 200
) {
        val dayFormatter = DateTimeFormatter.ofPattern("dd")
        val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM - yyyy")
        val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE")
        var showFutureDateDialog by remember { mutableStateOf(false) }

        // Morphing Shape logic: Cycle through 4 expressive forms based on day % 4
        val shapeStep = selectedDate.dayOfMonth % 4
        val tlRadius by
                animateDpAsState(
                        targetValue =
                                when (shapeStep) {
                                        1 -> 24.dp
                                        2 -> 8.dp
                                        3 -> 20.dp
                                        else -> 16.dp
                                },
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "TL"
                )
        val trRadius by
                animateDpAsState(
                        targetValue =
                                when (shapeStep) {
                                        1 -> 8.dp
                                        2 -> 24.dp
                                        3 -> 20.dp
                                        else -> 16.dp
                                },
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "TR"
                )
        val brRadius by
                animateDpAsState(
                        targetValue =
                                when (shapeStep) {
                                        1 -> 24.dp
                                        2 -> 8.dp
                                        3 -> 4.dp
                                        else -> 16.dp
                                },
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "BR"
                )
        val blRadius by
                animateDpAsState(
                        targetValue =
                                when (shapeStep) {
                                        1 -> 8.dp
                                        2 -> 24.dp
                                        3 -> 20.dp
                                        else -> 16.dp
                                },
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "BL"
                )

        val expressiveShape =
                RoundedCornerShape(
                        topStart = tlRadius,
                        topEnd = trRadius,
                        bottomEnd = brRadius,
                        bottomStart = blRadius
                )

        val isTodayOrFuture = !selectedDate.isBefore(LocalDate.now())

        Scaffold(
                topBar = {
                        Surface(
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Column(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .windowInsetsPadding(
                                                                WindowInsets.statusBars
                                                        )
                                ) {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 8.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                com.mj.yaja.ui.components.AnimatedMenuButton(
                                                        onClick = onOpenDrawer
                                                )

                                                OutlinedTextField(
                                                        value = searchQuery,
                                                        onValueChange = onSearchQueryChanged,
                                                        placeholder = {
                                                                Text(
                                                                        "Search entries...",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyLarge,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.6f
                                                                                        )
                                                                )
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        leadingIcon = {
                                                                Icon(
                                                                        Icons.Rounded.Search,
                                                                        contentDescription =
                                                                                "Search",
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        24.dp
                                                                                ),
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                        },
                                                        trailingIcon = {
                                                                if (searchQuery.isNotEmpty()) {
                                                                        IconButton(
                                                                                onClick =
                                                                                        onClearSearch
                                                                        ) {
                                                                                Icon(
                                                                                        Icons.Rounded
                                                                                                .Close,
                                                                                        contentDescription =
                                                                                                "Clear Search",
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        20.dp
                                                                                                )
                                                                                )
                                                                        }
                                                                }
                                                        },
                                                        shape = CircleShape,
                                                        colors =
                                                                OutlinedTextFieldDefaults.colors(
                                                                        focusedBorderColor =
                                                                                Color.Transparent,
                                                                        unfocusedBorderColor =
                                                                                Color.Transparent,
                                                                        focusedContainerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceContainerHigh,
                                                                        unfocusedContainerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceContainerHigh
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.7f
                                                                                        )
                                                                ),
                                                        singleLine = true
                                                )

                                                // Animate scale on each toggle: 1 → 1.45 →
                                                // 0.85 →
                                                // 1.15 → 1
                                                val starScale = remember { Animatable(1f) }
                                                LaunchedEffect(isFavorited) {
                                                        starScale.animateTo(
                                                                targetValue = 1f,
                                                                animationSpec =
                                                                        keyframes {
                                                                                durationMillis = 400
                                                                                1.45f at 80
                                                                                0.85f at 180
                                                                                1.15f at 270
                                                                                1f at 400
                                                                        }
                                                        )
                                                }

                                                Surface(
                                                        color =
                                                                if (isFavorited)
                                                                        Color(0xFFFFD700)
                                                                                .copy(alpha = 0.15f)
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceContainerHigh,
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(48.dp),
                                                        onClick = onToggleFavorite
                                                ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                                AnimatedContent(
                                                                        targetState = isFavorited,
                                                                        transitionSpec = {
                                                                                (scaleIn(
                                                                                        spring(
                                                                                                dampingRatio =
                                                                                                        0.5f,
                                                                                                stiffness =
                                                                                                        Spring.StiffnessMedium
                                                                                        )
                                                                                ) +
                                                                                        fadeIn(
                                                                                                tween(
                                                                                                        150
                                                                                                )
                                                                                        )) togetherWith
                                                                                        (scaleOut(
                                                                                                spring(
                                                                                                        stiffness =
                                                                                                                Spring.StiffnessMedium
                                                                                                )
                                                                                        ) +
                                                                                                fadeOut(
                                                                                                        tween(
                                                                                                                100
                                                                                                        )
                                                                                                ))
                                                                        },
                                                                        contentAlignment =
                                                                                Alignment.Center,
                                                                        label = "StarIconMorph"
                                                                ) { starred ->
                                                                        Icon(
                                                                                imageVector =
                                                                                        if (starred)
                                                                                                Icons.Rounded
                                                                                                        .Star
                                                                                        else
                                                                                                Icons.Rounded
                                                                                                        .StarOutline,
                                                                                contentDescription =
                                                                                        "Favorite",
                                                                                tint =
                                                                                        if (starred)
                                                                                                Color(
                                                                                                        0xFFFFD700
                                                                                                )
                                                                                        else
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurfaceVariant,
                                                                                modifier =
                                                                                        Modifier
                                                                                                .graphicsLayer {
                                                                                                        scaleX =
                                                                                                                starScale
                                                                                                                        .value
                                                                                                        scaleY =
                                                                                                                starScale
                                                                                                                        .value
                                                                                                }
                                                                        )
                                                                }
                                                        }
                                                }
                                        }

                                        AnimatedVisibility(
                                                visible = searchQuery.isEmpty(),
                                                enter =
                                                        fadeIn(
                                                                spring(
                                                                        stiffness =
                                                                                Spring.StiffnessMediumLow
                                                                )
                                                        ) +
                                                                slideInVertically(
                                                                        spring(
                                                                                stiffness =
                                                                                        Spring.StiffnessMediumLow
                                                                        )
                                                                ) { -it / 3 },
                                                exit =
                                                        fadeOut(
                                                                spring(
                                                                        stiffness =
                                                                                Spring.StiffnessMediumLow
                                                                )
                                                        ) +
                                                                slideOutVertically(
                                                                        spring(
                                                                                stiffness =
                                                                                        Spring.StiffnessMediumLow
                                                                        )
                                                                ) { -it / 3 }
                                        ) {
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(
                                                                                start = 12.dp,
                                                                                end = 12.dp,
                                                                                bottom = 12.dp,
                                                                                top = 8.dp
                                                                        ),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        val prevInteractionSource = remember {
                                                                MutableInteractionSource()
                                                        }
                                                        val isPrevPressed by
                                                                prevInteractionSource
                                                                        .collectIsPressedAsState()
                                                        val prevScale by
                                                                animateFloatAsState(
                                                                        targetValue =
                                                                                if (isPrevPressed)
                                                                                        0.85f
                                                                                else 1f,
                                                                        animationSpec =
                                                                                spring(
                                                                                        dampingRatio =
                                                                                                0.5f,
                                                                                        stiffness =
                                                                                                Spring.StiffnessMedium
                                                                                ),
                                                                        label = "PrevScale"
                                                                )

                                                        Surface(
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceContainerLow,
                                                                shape = CircleShape,
                                                                modifier =
                                                                        Modifier.size(40.dp)
                                                                                .graphicsLayer {
                                                                                        scaleX =
                                                                                                prevScale
                                                                                        scaleY =
                                                                                                prevScale
                                                                                },
                                                                interactionSource =
                                                                        prevInteractionSource,
                                                                onClick = onPreviousDate
                                                        ) {
                                                                Box(
                                                                        contentAlignment =
                                                                                Alignment.Center
                                                                ) {
                                                                        Icon(
                                                                                Icons.Rounded
                                                                                        .ChevronLeft,
                                                                                contentDescription =
                                                                                        "Previous Date",
                                                                                tint =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant
                                                                        )
                                                                }
                                                        }

                                                        var totalDrag by remember {
                                                                mutableStateOf(0f)
                                                        }
                                                        Row(
                                                                modifier =
                                                                        Modifier.weight(1f)
                                                                                .padding(
                                                                                        horizontal =
                                                                                                4.dp
                                                                                )
                                                                                .draggable(
                                                                                        state =
                                                                                                rememberDraggableState {
                                                                                                        delta
                                                                                                        ->
                                                                                                        totalDrag +=
                                                                                                                delta
                                                                                                },
                                                                                        orientation =
                                                                                                Orientation
                                                                                                        .Horizontal,
                                                                                        onDragStopped = {
                                                                                                if (totalDrag <
                                                                                                                -50
                                                                                                ) {
                                                                                                        onNextDate()
                                                                                                } else if (totalDrag >
                                                                                                                50
                                                                                                ) {
                                                                                                        onPreviousDate()
                                                                                                }
                                                                                                totalDrag =
                                                                                                        0f
                                                                                        }
                                                                                ),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.Center
                                                        ) {
                                                                Box(
                                                                        modifier =
                                                                                Modifier.size(56.dp)
                                                                                        .background(
                                                                                                color =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primaryContainer
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.4f
                                                                                                                ),
                                                                                                shape =
                                                                                                        expressiveShape
                                                                                        ),
                                                                        contentAlignment =
                                                                                Alignment.Center
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        selectedDate
                                                                                                .format(
                                                                                                        dayFormatter
                                                                                                ),
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .headlineLarge,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .ExtraBold,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary
                                                                        )
                                                                }
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        16.dp
                                                                                )
                                                                )
                                                                AnimatedContent(
                                                                        targetState = selectedDate,
                                                                        transitionSpec = {
                                                                                val goingForward =
                                                                                        targetState >
                                                                                                initialState
                                                                                (slideInHorizontally(
                                                                                        tween(280)
                                                                                ) {
                                                                                        if (goingForward
                                                                                        )
                                                                                                it
                                                                                        else -it
                                                                                } +
                                                                                        fadeIn(
                                                                                                tween(
                                                                                                        200
                                                                                                )
                                                                                        )) togetherWith
                                                                                        (slideOutHorizontally(
                                                                                                tween(
                                                                                                        280
                                                                                                )
                                                                                        ) {
                                                                                                if (goingForward
                                                                                                )
                                                                                                        -it
                                                                                                else
                                                                                                        it
                                                                                        } +
                                                                                                fadeOut(
                                                                                                        tween(
                                                                                                                160
                                                                                                        )
                                                                                                ))
                                                                        },
                                                                        contentAlignment =
                                                                                Alignment
                                                                                        .CenterStart,
                                                                        label =
                                                                                "DateHeaderCrossfade"
                                                                ) { date ->
                                                                        Column {
                                                                                Text(
                                                                                        text =
                                                                                                date.format(
                                                                                                        weekdayFormatter
                                                                                                ),
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .labelMedium,
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .primary
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.7f
                                                                                                        ),
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .Bold,
                                                                                        letterSpacing =
                                                                                                0.5.sp
                                                                                )
                                                                                Text(
                                                                                        text =
                                                                                                date.format(
                                                                                                        monthYearFormatter
                                                                                                ),
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .titleMedium,
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurface,
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .Medium
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                        val nextInteractionSource = remember {
                                                                MutableInteractionSource()
                                                        }
                                                        val isNextPressed by
                                                                nextInteractionSource
                                                                        .collectIsPressedAsState()
                                                        val nextScale by
                                                                animateFloatAsState(
                                                                        targetValue =
                                                                                if (isNextPressed)
                                                                                        0.85f
                                                                                else 1f,
                                                                        animationSpec =
                                                                                spring(
                                                                                        dampingRatio =
                                                                                                0.5f,
                                                                                        stiffness =
                                                                                                Spring.StiffnessMedium
                                                                                ),
                                                                        label = "NextScale"
                                                                )

                                                        Surface(
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceContainerLow,
                                                                shape = CircleShape,
                                                                modifier =
                                                                        Modifier.size(40.dp)
                                                                                .graphicsLayer {
                                                                                        scaleX =
                                                                                                nextScale
                                                                                        scaleY =
                                                                                                nextScale
                                                                                },
                                                                interactionSource =
                                                                        nextInteractionSource,
                                                                onClick = onNextDate
                                                        ) {
                                                                Box(
                                                                        contentAlignment =
                                                                                Alignment.Center
                                                                ) {
                                                                        Icon(
                                                                                Icons.Rounded
                                                                                        .ChevronRight,
                                                                                contentDescription =
                                                                                        "Next Date",
                                                                                tint =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                },
                floatingActionButton = {
                        // Track whether the + FAB has been tapped — triggers icon morph
                        var fabPressed by remember { mutableStateOf(false) }

                        // Navigate after the morph animation plays out (~380ms)
                        LaunchedEffect(fabPressed) {
                                if (fabPressed) {
                                        delay(380)
                                        onClearEditing()
                                        onNavigateToAddEntry()
                                        fabPressed = false
                                }
                        }

                        Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                                AnimatedVisibility(
                                        visible =
                                                !selectedDate.isAfter(LocalDate.now()) ||
                                                        allowFutureEntries,
                                        enter =
                                                scaleIn(
                                                        spring(
                                                                dampingRatio = 0.6f,
                                                                stiffness = Spring.StiffnessMedium
                                                        )
                                                ) + fadeIn(),
                                        exit =
                                                scaleOut(
                                                        spring(stiffness = Spring.StiffnessMedium)
                                                ) + fadeOut()
                                ) {
                                        FloatingActionButton(
                                                onClick = {
                                                        if (!fabPressed) {
                                                                if (selectedDate.isAfter(
                                                                                LocalDate.now()
                                                                        )
                                                                ) {
                                                                        showFutureDateDialog = true
                                                                } else {
                                                                        fabPressed = true
                                                                }
                                                        }
                                                },
                                                containerColor =
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                contentColor =
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                                AnimatedContent(
                                                        targetState = fabPressed,
                                                        transitionSpec = {
                                                                (scaleIn(
                                                                        spring(
                                                                                dampingRatio = 0.5f,
                                                                                stiffness =
                                                                                        Spring.StiffnessMedium
                                                                        )
                                                                ) + fadeIn(tween(180))) togetherWith
                                                                        (scaleOut(
                                                                                spring(
                                                                                        stiffness =
                                                                                                Spring.StiffnessMedium
                                                                                )
                                                                        ) + fadeOut(tween(120)))
                                                        },
                                                        contentAlignment = Alignment.Center,
                                                        label = "FabIconMorph"
                                                ) { isPressed ->
                                                        if (isPressed) {
                                                                Icon(
                                                                        Icons.Rounded.Check,
                                                                        contentDescription =
                                                                                "Saving"
                                                                )
                                                        } else {
                                                                Icon(
                                                                        Icons.Rounded.Add,
                                                                        contentDescription =
                                                                                "Add Entry"
                                                                )
                                                        }
                                                }
                                        }
                                }

                                AnimatedVisibility(
                                        visible = selectedDate != LocalDate.now(),
                                        enter =
                                                scaleIn(
                                                        spring(
                                                                dampingRatio = 0.6f,
                                                                stiffness = Spring.StiffnessMedium
                                                        )
                                                ) + fadeIn(),
                                        exit =
                                                scaleOut(
                                                        spring(stiffness = Spring.StiffnessMedium)
                                                ) + fadeOut()
                                ) {
                                        FloatingActionButton(
                                                onClick = onJumpToToday,
                                                containerColor =
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                contentColor =
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                                Icon(
                                                        Icons.Rounded.Today,
                                                        contentDescription = "Jump to Today"
                                                )
                                        }
                                }
                        }
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
        ) { paddingValues ->
                var totalDrag by remember { mutableStateOf(0f) }

                val contentColumn =
                        @Composable
                        {
                                Column(
                                        modifier =
                                                Modifier.fillMaxSize().pointerInput(Unit) {
                                                        detectHorizontalDragGestures(
                                                                onDragStart = { totalDrag = 0f },
                                                                onHorizontalDrag = {
                                                                        change,
                                                                        dragAmount ->
                                                                        change.consume()
                                                                        totalDrag += dragAmount
                                                                },
                                                                onDragEnd = {
                                                                        if (totalDrag < -50) {
                                                                                // Swiped
                                                                                // left ->
                                                                                // move date
                                                                                // forward
                                                                                onNextDate()
                                                                        } else if (totalDrag > 50) {
                                                                                // Swiped
                                                                                // right ->
                                                                                // move date
                                                                                // backward
                                                                                onPreviousDate()
                                                                        }
                                                                        totalDrag = 0f
                                                                }
                                                        )
                                                }
                                ) {
                                        if (searchQuery.isNotEmpty()) {
                                                Text(
                                                        text =
                                                                "${searchResults.size} Matches Found",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier =
                                                                Modifier.padding(
                                                                        horizontal = 24.dp,
                                                                        vertical = 8.dp
                                                                )
                                                )

                                                LazyColumn(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(12.dp),
                                                        contentPadding =
                                                                WindowInsets.navigationBars
                                                                        .asPaddingValues()
                                                ) {
                                                        itemsIndexed(
                                                                items = searchResults,
                                                                key = { _, it ->
                                                                        "${it.date}_${it.entryPreview.hashCode()}"
                                                                }
                                                        ) { index, result ->
                                                                var appeared by remember {
                                                                        mutableStateOf(false)
                                                                }
                                                                LaunchedEffect(Unit) {
                                                                        delay(
                                                                                (index * 40L)
                                                                                        .coerceAtMost(
                                                                                                200L
                                                                                        )
                                                                        )
                                                                        appeared = true
                                                                }
                                                                AnimatedVisibility(
                                                                        visible = appeared,
                                                                        enter =
                                                                                slideInVertically(
                                                                                        tween(
                                                                                                280,
                                                                                                easing =
                                                                                                        FastOutSlowInEasing
                                                                                        )
                                                                                ) { it / 3 } +
                                                                                        fadeIn(
                                                                                                tween(
                                                                                                        280
                                                                                                )
                                                                                        ),
                                                                        modifier =
                                                                                Modifier.animateItem(
                                                                                        fadeInSpec =
                                                                                                spring(
                                                                                                        stiffness =
                                                                                                                Spring.StiffnessLow
                                                                                                ),
                                                                                        fadeOutSpec =
                                                                                                spring(
                                                                                                        stiffness =
                                                                                                                Spring.StiffnessLow
                                                                                                ),
                                                                                        placementSpec =
                                                                                                spring(
                                                                                                        dampingRatio =
                                                                                                                Spring.DampingRatioLowBouncy,
                                                                                                        stiffness =
                                                                                                                Spring.StiffnessMediumLow
                                                                                                )
                                                                                )
                                                                ) {
                                                                        ElevatedCard(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                                                .padding(
                                                                                                        horizontal =
                                                                                                                24.dp
                                                                                                )
                                                                                                .clickable {
                                                                                                        onResultClicked(
                                                                                                                result.date
                                                                                                        )
                                                                                                },
                                                                                colors =
                                                                                        CardDefaults
                                                                                                .elevatedCardColors(
                                                                                                        containerColor =
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .surfaceContainerLow
                                                                                                ),
                                                                                elevation =
                                                                                        CardDefaults
                                                                                                .elevatedCardElevation(
                                                                                                        defaultElevation =
                                                                                                                2.dp
                                                                                                ),
                                                                                shape =
                                                                                        MaterialTheme
                                                                                                .shapes
                                                                                                .medium
                                                                        ) {
                                                                                Column(
                                                                                        modifier =
                                                                                                Modifier.fillMaxWidth()
                                                                                                        .padding(
                                                                                                                16.dp
                                                                                                        )
                                                                                ) {
                                                                                        val resultFormatter =
                                                                                                DateTimeFormatter
                                                                                                        .ofPattern(
                                                                                                                "dd-MMM-yyyy"
                                                                                                        )
                                                                                        Text(
                                                                                                text =
                                                                                                        result.date
                                                                                                                .format(
                                                                                                                        resultFormatter
                                                                                                                ),
                                                                                                style =
                                                                                                        MaterialTheme
                                                                                                                .typography
                                                                                                                .labelMedium,
                                                                                                color =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primary,
                                                                                                modifier =
                                                                                                        Modifier.padding(
                                                                                                                bottom =
                                                                                                                        8.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                text =
                                                                                                        MarkdownUtils
                                                                                                                .parseMarkdown(
                                                                                                                        result.entryPreview
                                                                                                                ),
                                                                                                style =
                                                                                                        MaterialTheme
                                                                                                                .typography
                                                                                                                .bodyMedium,
                                                                                                color =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .onSurfaceVariant,
                                                                                                maxLines =
                                                                                                        2,
                                                                                                overflow =
                                                                                                        TextOverflow
                                                                                                                .Ellipsis
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                        item {
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        80.dp
                                                                                )
                                                                )
                                                        }
                                                }
                                        } else if (isLoading) {
                                                // Ignore empty loading state flash
                                        } else if (entries.isEmpty()) {
                                                var appeared by remember { mutableStateOf(false) }
                                                LaunchedEffect(Unit) { appeared = true }

                                                val infiniteTransition =
                                                        rememberInfiniteTransition(
                                                                label = "EmptyBreathing"
                                                        )
                                                val iconScale by
                                                        infiniteTransition.animateFloat(
                                                                initialValue = 0.95f,
                                                                targetValue = 1.05f,
                                                                animationSpec =
                                                                        infiniteRepeatable(
                                                                                animation =
                                                                                        tween(
                                                                                                1500,
                                                                                                easing =
                                                                                                        FastOutSlowInEasing
                                                                                        ),
                                                                                repeatMode =
                                                                                        RepeatMode
                                                                                                .Reverse
                                                                        ),
                                                                label = "IconScale"
                                                        )

                                                AnimatedVisibility(
                                                        visible = appeared,
                                                        enter =
                                                                fadeIn(tween(500)) +
                                                                        slideInVertically(
                                                                                tween(500)
                                                                        ) { it / 6 },
                                                        modifier = Modifier.fillMaxSize()
                                                ) {
                                                        Box(
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Column(
                                                                        horizontalAlignment =
                                                                                Alignment
                                                                                        .CenterHorizontally,
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        horizontal =
                                                                                                48.dp
                                                                                )
                                                                ) {
                                                                        androidx.compose.material3
                                                                                .Surface(
                                                                                        shape =
                                                                                                androidx.compose
                                                                                                        .foundation
                                                                                                        .shape
                                                                                                        .RoundedCornerShape(
                                                                                                                32.dp
                                                                                                        ),
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .secondaryContainer,
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                                100.dp
                                                                                                        )
                                                                                                        .graphicsLayer {
                                                                                                                scaleX =
                                                                                                                        iconScale
                                                                                                                scaleY =
                                                                                                                        iconScale
                                                                                                        }
                                                                                ) {
                                                                                        Box(
                                                                                                contentAlignment =
                                                                                                        Alignment
                                                                                                                .Center
                                                                                        ) {
                                                                                                Icon(
                                                                                                        imageVector =
                                                                                                                if (selectedDate
                                                                                                                                .isAfter(
                                                                                                                                        LocalDate
                                                                                                                                                .now()
                                                                                                                                )
                                                                                                                )
                                                                                                                        Icons.Rounded
                                                                                                                                .EditCalendar
                                                                                                                else if (selectedDate
                                                                                                                                .isBefore(
                                                                                                                                        LocalDate
                                                                                                                                                .now()
                                                                                                                                )
                                                                                                                )
                                                                                                                        Icons.Rounded
                                                                                                                                .HistoryEdu
                                                                                                                else
                                                                                                                        Icons.Rounded
                                                                                                                                .AutoStories,
                                                                                                        contentDescription =
                                                                                                                "Empty Status Icon",
                                                                                                        modifier =
                                                                                                                Modifier.size(
                                                                                                                        52.dp
                                                                                                                ),
                                                                                                        tint =
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .onSecondaryContainer
                                                                                                )
                                                                                        }
                                                                                }
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                16.dp
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                text =
                                                                                        if (selectedDate
                                                                                                        .isAfter(
                                                                                                                LocalDate
                                                                                                                        .now()
                                                                                                        )
                                                                                        )
                                                                                                "This is the future. Record a reminder!"
                                                                                        else if (selectedDate
                                                                                                        .isBefore(
                                                                                                                LocalDate
                                                                                                                        .now()
                                                                                                        )
                                                                                        )
                                                                                                "You are in the Past. Record a Memory before it fades..."
                                                                                        else
                                                                                                "No entries yet. Start your day.",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyLarge,
                                                                                textAlign =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .text
                                                                                                .style
                                                                                                .TextAlign
                                                                                                .Center,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onBackground
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.5f
                                                                                                )
                                                                        )
                                                                }
                                                        }
                                                }
                                        } else {
                                                LazyColumn(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(12.dp),
                                                        contentPadding =
                                                                WindowInsets.navigationBars
                                                                        .asPaddingValues()
                                                ) {
                                                        itemsIndexed(
                                                                items = entries,
                                                                key = { _, entry -> entry }
                                                        ) { index, entry ->
                                                                var appeared by remember {
                                                                        mutableStateOf(false)
                                                                }
                                                                LaunchedEffect(Unit) {
                                                                        delay(
                                                                                (index * 40L)
                                                                                        .coerceAtMost(
                                                                                                200L
                                                                                        )
                                                                        )
                                                                        appeared = true
                                                                }
                                                                AnimatedVisibility(
                                                                        visible = appeared,
                                                                        enter =
                                                                                slideInVertically(
                                                                                        tween(
                                                                                                280,
                                                                                                easing =
                                                                                                        FastOutSlowInEasing
                                                                                        )
                                                                                ) { it / 3 } +
                                                                                        fadeIn(
                                                                                                tween(
                                                                                                        280
                                                                                                )
                                                                                        ),
                                                                        modifier =
                                                                                Modifier.animateItem(
                                                                                        fadeInSpec =
                                                                                                spring(
                                                                                                        stiffness =
                                                                                                                Spring.StiffnessLow
                                                                                                ),
                                                                                        fadeOutSpec =
                                                                                                spring(
                                                                                                        stiffness =
                                                                                                                Spring.StiffnessLow
                                                                                                ),
                                                                                        placementSpec =
                                                                                                spring(
                                                                                                        dampingRatio =
                                                                                                                Spring.DampingRatioLowBouncy,
                                                                                                        stiffness =
                                                                                                                Spring.StiffnessMediumLow
                                                                                                )
                                                                                )
                                                                ) {
                                                                        JournalEntryItem(
                                                                                entry = entry,
                                                                                showTimestamps =
                                                                                        showTimestamps,
                                                                                swipeToDeleteEnabled =
                                                                                        swipeToDeleteEnabled,
                                                                                swipeDeleteDirection =
                                                                                        swipeDeleteDirection,
                                                                                isPreviewLimitEnabled =
                                                                                        isPreviewLimitEnabled,
                                                                                previewLimitLength =
                                                                                        previewLimitLength,
                                                                                onDelete = {
                                                                                        onDeleteEntry(
                                                                                                index
                                                                                        )
                                                                                },
                                                                                onEdit = {
                                                                                        onStartEditing(
                                                                                                entry,
                                                                                                index
                                                                                        )
                                                                                        onNavigateToAddEntry()
                                                                                }
                                                                        )
                                                                }
                                                        }
                                                        item {
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        80.dp
                                                                                )
                                                                )
                                                        } // FAB padding
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

                if (showFutureDateDialog) {
                        AlertDialog(
                                onDismissRequest = { showFutureDateDialog = false },
                                title = { Text("Future Date Entry") },
                                text = {
                                        Text(
                                                "You are adding an entry for a future date (${selectedDate.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))}). Do you want to continue?"
                                        )
                                },
                                confirmButton = {
                                        TextButton(
                                                onClick = {
                                                        showFutureDateDialog = false
                                                        onClearEditing()
                                                        onNavigateToAddEntry()
                                                }
                                        ) { Text("Yes") }
                                },
                                dismissButton = {
                                        TextButton(onClick = { showFutureDateDialog = false }) {
                                                Text("No")
                                        }
                                }
                        )
                }

                if (showCacheAnomalyDialog) {
                        AlertDialog(
                                onDismissRequest = onDismissAnomalyDialog,
                                title = { Text("Data Cache Issue Detected") },
                                text = {
                                        Text(
                                                "It appears a substantial number of journal entries are missing from the cache. Would you like to perform a full data cache refresh to attempt to restore them?"
                                        )
                                },
                                confirmButton = {
                                        TextButton(onClick = onAcceptAnomalyRefresh) {
                                                Text("Refresh")
                                        }
                                },
                                dismissButton = {
                                        TextButton(onClick = onDismissAnomalyDialog) {
                                                Text("Ignore")
                                        }
                                }
                        )
                }
        }
}
