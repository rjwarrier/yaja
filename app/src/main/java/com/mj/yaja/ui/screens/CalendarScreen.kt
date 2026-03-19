package com.mj.yaja.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

enum class CalendarViewMode {
        DAYS,
        MONTHS,
        YEARS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit,
        onNavigateBack: () -> Unit,
        onDateSelected: (LocalDate) -> Unit,
        onDateSelectedForEntry: (LocalDate) -> Unit,
        onNavigateToJournal: () -> Unit,
        onNavigateToCalendar: () -> Unit,
        onNavigateToLookback: () -> Unit,
        onNavigateToShortcodes: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToHelp: () -> Unit
) {
        val uiState by viewModel.uiState.collectAsState()
        val firstDayOfWeekPref by viewModel.firstDayOfWeek.collectAsState()
        val scope = rememberCoroutineScope()

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        var currentMonth by remember { mutableStateOf(YearMonth.from(uiState.selectedDate)) }
        var currentYearWindowStart by remember {
                mutableStateOf(currentMonth.year - (currentMonth.year % 10))
        }
        var viewMode by remember { mutableStateOf(CalendarViewMode.DAYS) }

        val allowFutureEntries by viewModel.allowFutureEntries.collectAsState()
        val favoritedDates by viewModel.favoritedDates.collectAsState()
        var showFutureDateDialog by remember { mutableStateOf(false) }
        var showEnableFutureDateDialog by remember { mutableStateOf(false) }
        var pendingFutureDate by remember { mutableStateOf<LocalDate?>(null) }
        val context = LocalContext.current

        val prevInteractionSource = remember { MutableInteractionSource() }
        val prevIsPressed by prevInteractionSource.collectIsPressedAsState()
        val prevScale by animateFloatAsState(if (prevIsPressed) 0.88f else 1f, label = "PrevScale")

        val nextInteractionSource = remember { MutableInteractionSource() }
        val nextIsPressed by nextInteractionSource.collectIsPressedAsState()
        val nextScale by animateFloatAsState(if (nextIsPressed) 0.88f else 1f, label = "NextScale")

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                "Calendar",
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
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        Column(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .padding(horizontal = 24.dp)
                                                .verticalScroll(rememberScrollState())
                        ) {
                                Spacer(modifier = Modifier.height(16.dp))

                                // Calendar Area Card
                                AnimatedVisibility(
                                        visible = visible,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 2 },
                                                        animationSpec = tween(350, 100)
                                                ) + fadeIn(animationSpec = tween(350, 100))
                                ) {
                                        androidx.compose.material3.ElevatedCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                        androidx.compose.material3.CardDefaults
                                                                .elevatedCardColors(
                                                                        containerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceContainerLow
                                                                ),
                                                elevation =
                                                        androidx.compose.material3.CardDefaults
                                                                .elevatedCardElevation(0.dp),
                                                shape = MaterialTheme.shapes.medium
                                        ) {
                                                Column(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(16.dp)
                                                ) {
                                                        // Month Header
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement =
                                                                        Arrangement.SpaceBetween,
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                IconButton(
                                                                        modifier =
                                                                                Modifier
                                                                                        .graphicsLayer {
                                                                                                scaleX =
                                                                                                        prevScale
                                                                                                scaleY =
                                                                                                        prevScale
                                                                                        },
                                                                        interactionSource =
                                                                                prevInteractionSource,
                                                                        onClick = {
                                                                                when (viewMode) {
                                                                                        CalendarViewMode
                                                                                                .DAYS ->
                                                                                                currentMonth =
                                                                                                        currentMonth
                                                                                                                .minusMonths(
                                                                                                                        1
                                                                                                                )
                                                                                        CalendarViewMode
                                                                                                .MONTHS ->
                                                                                                currentMonth =
                                                                                                        currentMonth
                                                                                                                .minusYears(
                                                                                                                        1
                                                                                                                )
                                                                                        CalendarViewMode
                                                                                                .YEARS ->
                                                                                                currentYearWindowStart -=
                                                                                                        10
                                                                                }
                                                                        }
                                                                ) {
                                                                        Icon(
                                                                                Icons.Rounded
                                                                                        .ChevronLeft,
                                                                                contentDescription =
                                                                                        "Previous"
                                                                        )
                                                                }

                                                                Text(
                                                                        text =
                                                                                when (viewMode) {
                                                                                        CalendarViewMode
                                                                                                .DAYS ->
                                                                                                "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}"
                                                                                        CalendarViewMode
                                                                                                .MONTHS ->
                                                                                                "${currentMonth.year}"
                                                                                        CalendarViewMode
                                                                                                .YEARS ->
                                                                                                "$currentYearWindowStart - ${currentYearWindowStart + 9}"
                                                                                },
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .titleMedium
                                                                                        .copy(
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .SemiBold
                                                                                        ),
                                                                        modifier =
                                                                                Modifier.clickable {
                                                                                                viewMode =
                                                                                                        when (viewMode
                                                                                                        ) {
                                                                                                                CalendarViewMode
                                                                                                                        .DAYS ->
                                                                                                                        CalendarViewMode
                                                                                                                                .MONTHS
                                                                                                                CalendarViewMode
                                                                                                                        .MONTHS -> {
                                                                                                                        currentYearWindowStart =
                                                                                                                                currentMonth
                                                                                                                                        .year -
                                                                                                                                        (currentMonth
                                                                                                                                                .year %
                                                                                                                                                10)
                                                                                                                        CalendarViewMode
                                                                                                                                .YEARS
                                                                                                                }
                                                                                                                CalendarViewMode
                                                                                                                        .YEARS ->
                                                                                                                        CalendarViewMode
                                                                                                                                .DAYS // Loop
                                                                                                        // back or
                                                                                                        // stay at
                                                                                                        // top
                                                                                                        // level
                                                                                                        }
                                                                                        }
                                                                                        .padding(
                                                                                                horizontal =
                                                                                                        16.dp,
                                                                                                vertical =
                                                                                                        8.dp
                                                                                        ) // larger
                                                                        // hit
                                                                        // area
                                                                        )

                                                                val maxYearMonth =
                                                                        if (allowFutureEntries)
                                                                                YearMonth.now()
                                                                                        .plusYears(
                                                                                                1
                                                                                        )
                                                                        else YearMonth.now()
                                                                IconButton(
                                                                        enabled =
                                                                                when (viewMode) {
                                                                                        CalendarViewMode
                                                                                                .DAYS ->
                                                                                                currentMonth <
                                                                                                        maxYearMonth
                                                                                        CalendarViewMode
                                                                                                .MONTHS ->
                                                                                                currentMonth <
                                                                                                        maxYearMonth
                                                                                        CalendarViewMode
                                                                                                .YEARS ->
                                                                                                currentYearWindowStart +
                                                                                                        9 <
                                                                                                        maxYearMonth
                                                                                                                .year
                                                                                },
                                                                        modifier =
                                                                                Modifier
                                                                                        .graphicsLayer {
                                                                                                scaleX =
                                                                                                        nextScale
                                                                                                scaleY =
                                                                                                        nextScale
                                                                                        },
                                                                        interactionSource =
                                                                                nextInteractionSource,
                                                                        onClick = {
                                                                                when (viewMode) {
                                                                                        CalendarViewMode
                                                                                                .DAYS ->
                                                                                                currentMonth =
                                                                                                        currentMonth
                                                                                                                .plusMonths(
                                                                                                                        1
                                                                                                                )
                                                                                        CalendarViewMode
                                                                                                .MONTHS ->
                                                                                                currentMonth =
                                                                                                        currentMonth
                                                                                                                .plusYears(
                                                                                                                        1
                                                                                                                )
                                                                                        CalendarViewMode
                                                                                                .YEARS ->
                                                                                                currentYearWindowStart +=
                                                                                                        10
                                                                                }
                                                                        }
                                                                ) {
                                                                        Icon(
                                                                                Icons.Rounded
                                                                                        .ChevronRight,
                                                                                contentDescription =
                                                                                        "Next"
                                                                        )
                                                                }
                                                        }

                                                        Spacer(modifier = Modifier.height(24.dp))

                                                        AnimatedVisibility(
                                                                visible =
                                                                        viewMode ==
                                                                                CalendarViewMode
                                                                                        .DAYS
                                                        ) {
                                                                // Days of week header
                                                                val daysOfWeek =
                                                                        if (firstDayOfWeekPref ==
                                                                                        java.time
                                                                                                .DayOfWeek
                                                                                                .SUNDAY
                                                                        ) {
                                                                                listOf(
                                                                                        "S",
                                                                                        "M",
                                                                                        "T",
                                                                                        "W",
                                                                                        "T",
                                                                                        "F",
                                                                                        "S"
                                                                                )
                                                                        } else {
                                                                                listOf(
                                                                                        "M",
                                                                                        "T",
                                                                                        "W",
                                                                                        "T",
                                                                                        "F",
                                                                                        "S",
                                                                                        "S"
                                                                                )
                                                                        }

                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceEvenly
                                                                ) {
                                                                        daysOfWeek.forEach { day ->
                                                                                Text(
                                                                                        text = day,
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .bodyMedium,
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onBackground
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.5f
                                                                                                        ),
                                                                                        modifier =
                                                                                                Modifier.weight(
                                                                                                        1f
                                                                                                ),
                                                                                        textAlign =
                                                                                                androidx.compose
                                                                                                        .ui
                                                                                                        .text
                                                                                                        .style
                                                                                                        .TextAlign
                                                                                                        .Center
                                                                                )
                                                                        }
                                                                }

                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        16.dp
                                                                                )
                                                                )
                                                        }

                                                        AnimatedContent(
                                                                targetState = viewMode,
                                                                transitionSpec = {
                                                                        (slideInHorizontally(
                                                                                        animationSpec =
                                                                                                tween(
                                                                                                        300
                                                                                                )
                                                                                ) { width ->
                                                                                        width / 3
                                                                                } +
                                                                                        fadeIn(
                                                                                                animationSpec =
                                                                                                        tween(
                                                                                                                300
                                                                                                        )
                                                                                        ))
                                                                                .togetherWith(
                                                                                        slideOutHorizontally(
                                                                                                animationSpec =
                                                                                                        tween(
                                                                                                                300
                                                                                                        )
                                                                                        ) { width ->
                                                                                                -width /
                                                                                                        3
                                                                                        } +
                                                                                                fadeOut(
                                                                                                        animationSpec =
                                                                                                                tween(
                                                                                                                        300
                                                                                                                )
                                                                                                )
                                                                                )
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                label = "ViewModeSwitch"
                                                        ) { currentViewMode ->
                                                                if (currentViewMode ==
                                                                                CalendarViewMode
                                                                                        .DAYS
                                                                ) {
                                                                        // Calendar Grid
                                                                        val daysInMonth =
                                                                                currentMonth
                                                                                        .lengthOfMonth()
                                                                        // ISO-8601 day of
                                                                        // week:
                                                                        // Monday = 1
                                                                        // ...
                                                                        // Sunday = 7
                                                                        val firstDayOfMonthVal =
                                                                                currentMonth.atDay(
                                                                                                1
                                                                                        )
                                                                                        .dayOfWeek
                                                                                        .value

                                                                        val prePadding =
                                                                                if (firstDayOfWeekPref ==
                                                                                                java.time
                                                                                                        .DayOfWeek
                                                                                                        .SUNDAY
                                                                                ) {
                                                                                        // If Sunday
                                                                                        // is
                                                                                        // first (0
                                                                                        // index),
                                                                                        // Mon=1,
                                                                                        // Tue=2,
                                                                                        // Wed=3,
                                                                                        // Thu=4,
                                                                                        // Fri=5,
                                                                                        // Sat=6
                                                                                        // ISO
                                                                                        // Sunday is
                                                                                        // 7,
                                                                                        // so (7 %
                                                                                        // 7) = 0.
                                                                                        // ISO
                                                                                        // Monday is
                                                                                        // 1,
                                                                                        // so (1 %
                                                                                        // 7) = 1.
                                                                                        firstDayOfMonthVal %
                                                                                                7
                                                                                } else {
                                                                                        // If Monday
                                                                                        // is
                                                                                        // first (0
                                                                                        // index),
                                                                                        // Mon=0,
                                                                                        // Tue=1,
                                                                                        // Wed=2,
                                                                                        // Thu=3,
                                                                                        // Fri=4,
                                                                                        // Sat=5,
                                                                                        // Sun=6
                                                                                        // ISO
                                                                                        // Monday is
                                                                                        // 1,
                                                                                        // so 1 - 1
                                                                                        // =
                                                                                        // 0. ISO
                                                                                        // Sunday
                                                                                        // is 7, so
                                                                                        // 7
                                                                                        // -
                                                                                        // 1
                                                                                        // =
                                                                                        // 6.
                                                                                        firstDayOfMonthVal -
                                                                                                1
                                                                                }

                                                                        val totalCells =
                                                                                prePadding +
                                                                                        daysInMonth
                                                                        val totalRows =
                                                                                kotlin.math
                                                                                        .ceil(
                                                                                                totalCells /
                                                                                                        7.0
                                                                                        )
                                                                                        .toInt()

                                                                        Column(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                        ) {
                                                                                for (row in
                                                                                        0 until
                                                                                                totalRows) {
                                                                                        Row(
                                                                                                modifier =
                                                                                                        Modifier.fillMaxWidth()
                                                                                        ) {
                                                                                                for (col in
                                                                                                        0 until
                                                                                                                7) {
                                                                                                        val index =
                                                                                                                row *
                                                                                                                        7 +
                                                                                                                        col
                                                                                                        Box(
                                                                                                                modifier =
                                                                                                                        Modifier.weight(
                                                                                                                                1f
                                                                                                                        )
                                                                                                        ) {
                                                                                                                if (index >=
                                                                                                                                prePadding &&
                                                                                                                                index <
                                                                                                                                        prePadding +
                                                                                                                                                daysInMonth
                                                                                                                ) {
                                                                                                                        val day =
                                                                                                                                index -
                                                                                                                                        prePadding +
                                                                                                                                        1
                                                                                                                        val date =
                                                                                                                                currentMonth
                                                                                                                                        .atDay(
                                                                                                                                                day
                                                                                                                                        )
                                                                                                                        val isSelected =
                                                                                                                                date ==
                                                                                                                                        uiState.selectedDate
                                                                                                                        val isToday =
                                                                                                                                date ==
                                                                                                                                        LocalDate
                                                                                                                                                .now()
                                                                                                                        val hasEntries =
                                                                                                                                uiState.datesWithEntries
                                                                                                                                        .contains(
                                                                                                                                                date
                                                                                                                                        )
                                                                                                                        val isFuture =
                                                                                                                                date.isAfter(
                                                                                                                                        LocalDate
                                                                                                                                                .now()
                                                                                                                                )
                                                                                                                        val isFavorited =
                                                                                                                                favoritedDates
                                                                                                                                        .contains(
                                                                                                                                                date.toString()
                                                                                                                                        )

                                                                                                                        CalendarDay(
                                                                                                                                day =
                                                                                                                                        day,
                                                                                                                                isSelected =
                                                                                                                                        isSelected,
                                                                                                                                isToday =
                                                                                                                                        isToday,
                                                                                                                                hasEntries =
                                                                                                                                        hasEntries,
                                                                                                                                isFuture =
                                                                                                                                        isFuture,
                                                                                                                                isDimmed =
                                                                                                                                        isFuture &&
                                                                                                                                                !allowFutureEntries,
                                                                                                                                isFavorited =
                                                                                                                                        isFavorited,
                                                                                                                                onClick = {
                                                                                                                                        if (isFuture
                                                                                                                                        ) {
                                                                                                                                                if (hasEntries
                                                                                                                                                ) {
                                                                                                                                                        onDateSelected(
                                                                                                                                                                date
                                                                                                                                                        )
                                                                                                                                                } else if (allowFutureEntries
                                                                                                                                                ) {
                                                                                                                                                        pendingFutureDate =
                                                                                                                                                                date
                                                                                                                                                        showFutureDateDialog =
                                                                                                                                                                true
                                                                                                                                                } else {
                                                                                                                                                        pendingFutureDate =
                                                                                                                                                                date
                                                                                                                                                        showEnableFutureDateDialog =
                                                                                                                                                                true
                                                                                                                                                }
                                                                                                                                        } else {
                                                                                                                                                onDateSelected(
                                                                                                                                                        date
                                                                                                                                                )
                                                                                                                                        }
                                                                                                                                }
                                                                                                                        )
                                                                                                                } else {
                                                                                                                        Spacer(
                                                                                                                                modifier =
                                                                                                                                        Modifier.aspectRatio(
                                                                                                                                                1f
                                                                                                                                        )
                                                                                                                        )
                                                                                                                }
                                                                                                        }
                                                                                                }
                                                                                        }
                                                                                }
                                                                        }
                                                                } else if (currentViewMode ==
                                                                                CalendarViewMode
                                                                                        .MONTHS
                                                                ) {
                                                                        Column(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                        ) {
                                                                                val months =
                                                                                        java.time
                                                                                                .Month
                                                                                                .values()
                                                                                for (row in
                                                                                        0 until 4) {
                                                                                        Row(
                                                                                                modifier =
                                                                                                        Modifier.fillMaxWidth(),
                                                                                                horizontalArrangement =
                                                                                                        Arrangement
                                                                                                                .spacedBy(
                                                                                                                        8.dp
                                                                                                                )
                                                                                        ) {
                                                                                                for (col in
                                                                                                        0 until
                                                                                                                3) {
                                                                                                        val month =
                                                                                                                months[
                                                                                                                        row *
                                                                                                                                3 +
                                                                                                                                col]
                                                                                                        Box(
                                                                                                                modifier =
                                                                                                                        Modifier.weight(
                                                                                                                                        1f
                                                                                                                                )
                                                                                                                                .aspectRatio(
                                                                                                                                        1.8f
                                                                                                                                )
                                                                                                                                .background(
                                                                                                                                        if (currentMonth
                                                                                                                                                        .month ==
                                                                                                                                                        month &&
                                                                                                                                                        currentMonth
                                                                                                                                                                .year ==
                                                                                                                                                                LocalDate
                                                                                                                                                                        .now()
                                                                                                                                                                        .year
                                                                                                                                        )
                                                                                                                                                MaterialTheme
                                                                                                                                                        .colorScheme
                                                                                                                                                        .primaryContainer
                                                                                                                                        else
                                                                                                                                                MaterialTheme
                                                                                                                                                        .colorScheme
                                                                                                                                                        .surfaceContainerLow,
                                                                                                                                        shape =
                                                                                                                                                CircleShape
                                                                                                                                )
                                                                                                                                .clickable {
                                                                                                                                        currentMonth =
                                                                                                                                                currentMonth
                                                                                                                                                        .withMonth(
                                                                                                                                                                month.value
                                                                                                                                                        )
                                                                                                                                        viewMode =
                                                                                                                                                CalendarViewMode
                                                                                                                                                        .DAYS
                                                                                                                                },
                                                                                                                contentAlignment =
                                                                                                                        Alignment
                                                                                                                                .Center
                                                                                                        ) {
                                                                                                                Text(
                                                                                                                        text =
                                                                                                                                month.getDisplayName(
                                                                                                                                        TextStyle
                                                                                                                                                .SHORT,
                                                                                                                                        Locale.getDefault()
                                                                                                                                ),
                                                                                                                        style =
                                                                                                                                MaterialTheme
                                                                                                                                        .typography
                                                                                                                                        .labelLarge
                                                                                                                                        .copy(
                                                                                                                                                fontWeight =
                                                                                                                                                        FontWeight
                                                                                                                                                                .Bold
                                                                                                                                        ),
                                                                                                                        color =
                                                                                                                                if (currentMonth
                                                                                                                                                .month ==
                                                                                                                                                month &&
                                                                                                                                                currentMonth
                                                                                                                                                        .year ==
                                                                                                                                                        LocalDate
                                                                                                                                                                .now()
                                                                                                                                                                .year
                                                                                                                                )
                                                                                                                                        MaterialTheme
                                                                                                                                                .colorScheme
                                                                                                                                                .onPrimaryContainer
                                                                                                                                else
                                                                                                                                        MaterialTheme
                                                                                                                                                .colorScheme
                                                                                                                                                .onSurfaceVariant
                                                                                                                )
                                                                                                        }
                                                                                                }
                                                                                        }
                                                                                        if (row < 3)
                                                                                                Spacer(
                                                                                                        modifier =
                                                                                                                Modifier.height(
                                                                                                                        8.dp
                                                                                                                )
                                                                                                )
                                                                                }
                                                                        }
                                                                } else if (currentViewMode ==
                                                                                CalendarViewMode
                                                                                        .YEARS
                                                                ) {
                                                                        Column(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                        ) {
                                                                                for (row in
                                                                                        0 until 4) {
                                                                                        Row(
                                                                                                modifier =
                                                                                                        Modifier.fillMaxWidth(),
                                                                                                horizontalArrangement =
                                                                                                        Arrangement
                                                                                                                .spacedBy(
                                                                                                                        8.dp
                                                                                                                )
                                                                                        ) {
                                                                                                for (col in
                                                                                                        0 until
                                                                                                                3) {
                                                                                                        val year =
                                                                                                                currentYearWindowStart +
                                                                                                                        (row *
                                                                                                                                3 +
                                                                                                                                col)
                                                                                                        Box(
                                                                                                                modifier =
                                                                                                                        Modifier.weight(
                                                                                                                                        1f
                                                                                                                                )
                                                                                                                                .aspectRatio(
                                                                                                                                        1.8f
                                                                                                                                )
                                                                                                                                .background(
                                                                                                                                        if (year ==
                                                                                                                                                        LocalDate
                                                                                                                                                                .now()
                                                                                                                                                                .year
                                                                                                                                        )
                                                                                                                                                MaterialTheme
                                                                                                                                                        .colorScheme
                                                                                                                                                        .primaryContainer
                                                                                                                                        else
                                                                                                                                                MaterialTheme
                                                                                                                                                        .colorScheme
                                                                                                                                                        .surfaceContainerLow,
                                                                                                                                        shape =
                                                                                                                                                CircleShape
                                                                                                                                )
                                                                                                                                .clickable {
                                                                                                                                        currentMonth =
                                                                                                                                                currentMonth
                                                                                                                                                        .withYear(
                                                                                                                                                                year
                                                                                                                                                        )
                                                                                                                                        viewMode =
                                                                                                                                                CalendarViewMode
                                                                                                                                                        .MONTHS
                                                                                                                                },
                                                                                                                contentAlignment =
                                                                                                                        Alignment
                                                                                                                                .Center
                                                                                                        ) {
                                                                                                                Text(
                                                                                                                        text =
                                                                                                                                year.toString(),
                                                                                                                        style =
                                                                                                                                MaterialTheme
                                                                                                                                        .typography
                                                                                                                                        .labelLarge
                                                                                                                                        .copy(
                                                                                                                                                fontWeight =
                                                                                                                                                        FontWeight
                                                                                                                                                                .Bold
                                                                                                                                        ),
                                                                                                                        color =
                                                                                                                                if (year ==
                                                                                                                                                LocalDate
                                                                                                                                                        .now()
                                                                                                                                                        .year
                                                                                                                                )
                                                                                                                                        MaterialTheme
                                                                                                                                                .colorScheme
                                                                                                                                                .onPrimaryContainer
                                                                                                                                else
                                                                                                                                        MaterialTheme
                                                                                                                                                .colorScheme
                                                                                                                                                .onSurfaceVariant
                                                                                                                )
                                                                                                        }
                                                                                                }
                                                                                        }
                                                                                        if (row < 3)
                                                                                                Spacer(
                                                                                                        modifier =
                                                                                                                Modifier.height(
                                                                                                                        8.dp
                                                                                                                )
                                                                                                )
                                                                                }
                                                                        }
                                                                }
                                                        } // end AnimatedContent (viewMode)

                                                        // Entry coverage stat � only in
                                                        // DAYS mode
                                                        // and not a
                                                        // fully-future month
                                                        if (viewMode == CalendarViewMode.DAYS) {
                                                                val today = LocalDate.now()
                                                                val thisYearMonth =
                                                                        YearMonth.from(today)
                                                                val isFutureMonth =
                                                                        currentMonth.isAfter(
                                                                                thisYearMonth
                                                                        )
                                                                if (!isFutureMonth) {
                                                                        val denominator =
                                                                                if (currentMonth ==
                                                                                                thisYearMonth
                                                                                ) {
                                                                                        today.dayOfMonth
                                                                                } else {
                                                                                        currentMonth
                                                                                                .lengthOfMonth()
                                                                                }
                                                                        val entriesInMonth =
                                                                                uiState.datesWithEntries
                                                                                        .count {
                                                                                                YearMonth
                                                                                                        .from(
                                                                                                                it
                                                                                                        ) ==
                                                                                                        currentMonth &&
                                                                                                        !it.isAfter(
                                                                                                                today
                                                                                                        )
                                                                                        }
                                                                        val pct =
                                                                                if (denominator > 0)
                                                                                        (entriesInMonth *
                                                                                                100) /
                                                                                                denominator
                                                                                else 0

                                                                        HorizontalDivider(
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                top =
                                                                                                        20.dp,
                                                                                                bottom =
                                                                                                        12.dp
                                                                                        ),
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .outlineVariant
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.4f
                                                                                                )
                                                                        )
                                                                        Row(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth(),
                                                                                horizontalArrangement =
                                                                                        Arrangement
                                                                                                .SpaceBetween,
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically
                                                                        ) {
                                                                                Text(
                                                                                        text =
                                                                                                "Entries this month",
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .labelMedium,
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurfaceVariant
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.7f
                                                                                                        ),
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .Medium
                                                                                )
                                                                                Row(
                                                                                        verticalAlignment =
                                                                                                Alignment
                                                                                                        .CenterVertically,
                                                                                        horizontalArrangement =
                                                                                                Arrangement
                                                                                                        .spacedBy(
                                                                                                                8.dp
                                                                                                        )
                                                                                ) {
                                                                                        AnimatedContent(
                                                                                                targetState =
                                                                                                        "$entriesInMonth/$denominator",
                                                                                                transitionSpec = {
                                                                                                        (slideInVertically {
                                                                                                                        height
                                                                                                                        ->
                                                                                                                        height
                                                                                                                } +
                                                                                                                        fadeIn())
                                                                                                                .togetherWith(
                                                                                                                        slideOutVertically {
                                                                                                                                height
                                                                                                                                ->
                                                                                                                                -height
                                                                                                                        } +
                                                                                                                                fadeOut()
                                                                                                                )
                                                                                                },
                                                                                                label =
                                                                                                        "CountRoll"
                                                                                        ) {
                                                                                                targetText
                                                                                                ->
                                                                                                Text(
                                                                                                        text =
                                                                                                                targetText,
                                                                                                        style =
                                                                                                                MaterialTheme
                                                                                                                        .typography
                                                                                                                        .labelLarge,
                                                                                                        color =
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .onSurface,
                                                                                                        fontWeight =
                                                                                                                FontWeight
                                                                                                                        .Bold
                                                                                                )
                                                                                        }
                                                                                        Surface(
                                                                                                shape =
                                                                                                        MaterialTheme
                                                                                                                .shapes
                                                                                                                .small,
                                                                                                color =
                                                                                                        if (pct ==
                                                                                                                        100
                                                                                                        )
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .tertiaryContainer
                                                                                                        else
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .secondaryContainer
                                                                                        ) {
                                                                                                AnimatedContent(
                                                                                                        targetState =
                                                                                                                "$pct%",
                                                                                                        transitionSpec = {
                                                                                                                (scaleIn(
                                                                                                                                initialScale =
                                                                                                                                        0.8f
                                                                                                                        ) +
                                                                                                                                fadeIn())
                                                                                                                        .togetherWith(
                                                                                                                                scaleOut(
                                                                                                                                        targetScale =
                                                                                                                                                1.2f
                                                                                                                                ) +
                                                                                                                                        fadeOut()
                                                                                                                        )
                                                                                                        },
                                                                                                        label =
                                                                                                                "PctRoll"
                                                                                                ) {
                                                                                                        targetText
                                                                                                        ->
                                                                                                        Text(
                                                                                                                text =
                                                                                                                        targetText,
                                                                                                                modifier =
                                                                                                                        Modifier.padding(
                                                                                                                                horizontal =
                                                                                                                                        8.dp,
                                                                                                                                vertical =
                                                                                                                                        2.dp
                                                                                                                        ),
                                                                                                                style =
                                                                                                                        MaterialTheme
                                                                                                                                .typography
                                                                                                                                .labelMedium,
                                                                                                                fontWeight =
                                                                                                                        FontWeight
                                                                                                                                .Bold,
                                                                                                                color =
                                                                                                                        if (pct ==
                                                                                                                                        100
                                                                                                                        )
                                                                                                                                MaterialTheme
                                                                                                                                        .colorScheme
                                                                                                                                        .onTertiaryContainer
                                                                                                                        else
                                                                                                                                MaterialTheme
                                                                                                                                        .colorScheme
                                                                                                                                        .onSecondaryContainer
                                                                                                        )
                                                                                                }
                                                                                        }
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                } // end AnimatedVisibility (Calendar Card)

                                Spacer(modifier = Modifier.height(32.dp))

                                AnimatedVisibility(
                                        visible = visible,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 2 },
                                                        animationSpec = tween(350, 200)
                                                ) + fadeIn(animationSpec = tween(350, 200))
                                ) { MonthlyStatsGraph(stats = uiState.monthlyStats) }

                                AnimatedVisibility(
                                        visible = visible && uiState.yearlyStats.size > 1,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 2 },
                                                        animationSpec = tween(350, 300)
                                                ) + fadeIn(animationSpec = tween(350, 300))
                                ) {
                                        Column {
                                                Spacer(modifier = Modifier.height(32.dp))
                                                YearlyConsistencyGraph(stats = uiState.yearlyStats)
                                        }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Spacer(modifier = Modifier.height(32.dp))

                                AnimatedVisibility(
                                        visible = visible,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 3 },
                                                        animationSpec = tween(450, 400)
                                                ) + fadeIn(animationSpec = tween(450, 400))
                                ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                                // Dynamic size for the morphing shape based
                                                // on
                                                // memory count
                                                val memoryCount = uiState.datesWithEntries.size
                                                val dynamicSize by
                                                        animateDpAsState(
                                                                targetValue =
                                                                        (80 +
                                                                                        (memoryCount *
                                                                                                        0.5f)
                                                                                                .coerceAtMost(
                                                                                                        40f
                                                                                                ))
                                                                                .dp,
                                                                animationSpec =
                                                                        spring(
                                                                                dampingRatio =
                                                                                        Spring.DampingRatioLowBouncy,
                                                                                stiffness =
                                                                                        Spring.StiffnessLow
                                                                        ),
                                                                label = "morph_size"
                                                        )

                                                Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally
                                                ) {
                                                        Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier =
                                                                        Modifier.size(dynamicSize)
                                                        ) {
                                                                MorphingBackground(
                                                                        modifier =
                                                                                Modifier.fillMaxSize(),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.12f
                                                                                        )
                                                                )
                                                                Text(
                                                                        text = "$memoryCount",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .displayMedium
                                                                                        .copy(
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Black,
                                                                                                fontSize =
                                                                                                        (dynamicSize
                                                                                                                        .value /
                                                                                                                        3)
                                                                                                                .sp // Scale
                                                                                                // text
                                                                                                // with
                                                                                                // container
                                                                                                ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                        }
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Text(
                                                                text = "days of memories",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(alpha = 0.7f),
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }

                                                Spacer(
                                                        modifier =
                                                                Modifier.navigationBarsPadding()
                                                                        .height(96.dp)
                                                 )
                                        } // end Column (memory blob)
                                } // end AnimatedVisibility (memory blob)
                        }
                }
        }

        if (showFutureDateDialog && pendingFutureDate != null) {
                AlertDialog(
                        onDismissRequest = { showFutureDateDialog = false },
                        title = { Text("Future Date Entry") },
                        text = {
                                Text(
                                        "You are adding an entry for a future date (${pendingFutureDate?.format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy"))}). Do you want to continue?"
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                pendingFutureDate?.let {
                                                        onDateSelectedForEntry(it)
                                                }
                                                showFutureDateDialog = false
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
        if (showEnableFutureDateDialog && pendingFutureDate != null) {
                AlertDialog(
                        onDismissRequest = { showEnableFutureDateDialog = false },
                        title = { Text("Enable Future Dates") },
                        text = { Text("Future date entry is disabled. Do you want to enable it?") },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                viewModel.setAllowFutureEntries(true)
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "Future date entry has been enabled",
                                                                android.widget.Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                pendingFutureDate?.let {
                                                        onDateSelectedForEntry(it)
                                                }
                                                showEnableFutureDateDialog = false
                                        }
                                ) { Text("Enable") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showEnableFutureDateDialog = false }) {
                                        Text("Cancel")
                                }
                        }
                )
        }
}

