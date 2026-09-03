package com.mj.yaja.ui.app

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.dpSpring
import com.mj.yaja.ui.design.floatSpring
import com.mj.yaja.ui.navigation.Route

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedFloatingBottomBar(
    currentRoute: String?,
    onNavigateHome: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateLookback: () -> Unit,
    onNavigateKeywords: () -> Unit,
    onNavigateStatistics: () -> Unit = {},
    onNavigateTodos: () -> Unit = {},
    onLongPressHome: () -> Unit = {},
    showLookbackInNavBar: Boolean = true,
    showKeywordsInNavBar: Boolean = false,
    showTodosInNavBar: Boolean = false,
    showStatisticsInNavBar: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    val selectedRoute = if (currentRoute == Route.RecurringTasks.path) {
        Route.Todos.path
    } else {
        currentRoute
    }
    val motionPreference = LocalAnimationPreference.current
    val density = LocalDensity.current
    val journalLabel = stringResource(R.string.nav_journal)
    val calendarLabel = stringResource(R.string.nav_calendar)
    val lookbackLabel = stringResource(R.string.nav_lookback)
    val peoplePlacesLabel = stringResource(R.string.nav_people_places)
    val todosLabel = stringResource(R.string.nav_todos)
    val statisticsLabel = stringResource(R.string.nav_statistics)
    val items = remember(
        showLookbackInNavBar,
        showKeywordsInNavBar,
        showTodosInNavBar,
        showStatisticsInNavBar,
        journalLabel,
        calendarLabel,
        lookbackLabel,
        peoplePlacesLabel,
        todosLabel,
        statisticsLabel
    ) {
        buildList {
            add(Triple(Route.Home.path, journalLabel, Icons.AutoMirrored.Rounded.MenuBook))
            add(Triple(Route.Calendar.path, calendarLabel, Icons.Rounded.CalendarMonth))
            if (showLookbackInNavBar) add(Triple(Route.Lookback.path, lookbackLabel, Icons.Rounded.History))
            if (showKeywordsInNavBar) add(Triple(Route.Keywords.path, peoplePlacesLabel, Icons.Rounded.People))
            if (showTodosInNavBar) add(Triple(Route.Todos.path, todosLabel, Icons.Rounded.Checklist))
            if (showStatisticsInNavBar) add(Triple(Route.Statistics.path, statisticsLabel, Icons.AutoMirrored.Rounded.TrendingUp))
        }
    }

    val selectedIndex = items.indexOfFirst { it.first == selectedRoute }.coerceAtLeast(0)
    val navigationFabSize = 64.dp
    val itemWidth = navigationFabSize
    // Set a uniform 2.dp gap between the highlight circle shape and the outer pill.
    // By setting horizontalPadding = 0.dp, the first and last items center exactly at the cap centers (32.dp),
    // making the highlight circle concentric with the pill's rounded caps and avoiding any clipping.
    val indicatorSize = 60.dp
    val indicatorInset = (navigationFabSize - indicatorSize) / 2 // 2.dp
    val iconSize = 24.dp
    val horizontalPadding = 0.dp

    val indicatorOffset by animateDpAsState(
        targetValue = horizontalPadding + (itemWidth * selectedIndex) + (itemWidth - indicatorSize) / 2,
        animationSpec = motionPreference.dpSpring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = 0.65f
        ),
        label = "indicatorOffset"
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.height(navigationFabSize).wrapContentWidth()) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset, y = indicatorInset)
                        .size(indicatorSize)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape
                        )
                )

                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .fillMaxHeight()
                        .padding(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, (route, label, icon) ->
                        val isSelected = index == selectedIndex
                        val iconTapMotion = rememberBottomNavIconTapMotion(route)
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = motionPreference.floatSpring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "bottom_bar_icon_scale_$route"
                        )
                        Box(
                            modifier = Modifier
                                .width(itemWidth)
                                .fillMaxHeight()
                                .combinedClickable(
                                        interactionSource = remember(route) { MutableInteractionSource() },
                                        indication = null,
                                        onLongClick = if (route == Route.Home.path) {
                                            {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onLongPressHome()
                                            }
                                        } else {
                                            null
                                        }
                                    ) {
                                    iconTapMotion.play(motionPreference)
                                    when (route) {
                                        Route.Home.path -> onNavigateHome()
                                        Route.Calendar.path -> onNavigateCalendar()
                                        Route.Lookback.path -> onNavigateLookback()
                                        Route.Keywords.path -> onNavigateKeywords()
                                        Route.Todos.path -> onNavigateTodos()
                                        Route.Statistics.path -> onNavigateStatistics()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier
                                    .size(iconSize)
                                    .graphicsLayer {
                                        scaleX = scale * iconTapMotion.scale.value
                                        scaleY = scale * iconTapMotion.scale.value
                                        rotationZ = iconTapMotion.rotation.value
                                        translationY = with(density) { iconTapMotion.liftDp.value.dp.toPx() }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
