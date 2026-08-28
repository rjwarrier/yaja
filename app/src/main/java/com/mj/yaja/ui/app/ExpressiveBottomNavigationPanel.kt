package com.mj.yaja.ui.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.dpSpring
import com.mj.yaja.ui.design.floatSpring
import com.mj.yaja.ui.design.rememberAppEntrance
import com.mj.yaja.ui.design.tweenSpec
import com.mj.yaja.ui.navigation.Route

private data class BottomPanelItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/** Smallest gap left between an indicator and the edge of the slot it sits in. */
private val INDICATOR_SLOT_MARGIN = 4.dp

/**
 * Width of the selection indicator inside a slot [itemWidth] wide.
 *
 * The minimum keeps the indicator comfortably tappable, but it has to give way once the
 * slots themselves are narrower than it: six destinations at a large display size leave
 * roughly 57dp each, and a minimum that ignored the slot would overlap every neighbour.
 */
internal fun resolveIndicatorWidth(itemWidth: Dp, showLabels: Boolean): Dp {
    val inset = if (showLabels) 18.dp else 20.dp
    val comfortable = if (showLabels) 72.dp else 56.dp
    val slotLimit = (itemWidth - INDICATOR_SLOT_MARGIN).coerceAtLeast(0.dp)
    return (itemWidth - inset).coerceAtLeast(comfortable).coerceAtMost(slotLimit)
}

/** Ceiling on how far a large font scale may stretch the bottom panel. */
private const val MAX_BOTTOM_PANEL_LABEL_SCALE = 1.6f

@Composable
fun ExpressiveBottomNavigationPanel(
    currentRoute: String?,
    onNavigateHome: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateLookback: () -> Unit,
    onNavigateKeywords: () -> Unit,
    onNavigateTodos: () -> Unit,
    onNavigateStatistics: () -> Unit,
    showLookbackInNavBar: Boolean,
    showKeywordsInNavBar: Boolean,
    showTodosInNavBar: Boolean,
    showStatisticsInNavBar: Boolean,
    showLabels: Boolean
) {
    val visible = rememberAppEntrance(delayMillis = 120)
    val motionPreference = LocalAnimationPreference.current
    val density = LocalDensity.current
    val selectedRoute = if (currentRoute == Route.RecurringTasks.path) {
        Route.Todos.path
    } else {
        currentRoute
    }
    val journalLabel = stringResource(R.string.nav_journal)
    val calendarLabel = stringResource(R.string.nav_calendar)
    val lookbackLabel = stringResource(R.string.nav_lookback)
    val peopleLabel = stringResource(R.string.nav_people_places)
    val todosLabel = stringResource(R.string.nav_todos)
    val statsLabel = stringResource(R.string.nav_statistics)

    val items = remember(
        showLookbackInNavBar,
        showKeywordsInNavBar,
        showTodosInNavBar,
        showStatisticsInNavBar,
        journalLabel,
        calendarLabel,
        lookbackLabel,
        peopleLabel,
        todosLabel,
        statsLabel
    ) {
        buildList {
            add(BottomPanelItem(Route.Home.path, journalLabel, Icons.AutoMirrored.Rounded.MenuBook))
            add(BottomPanelItem(Route.Calendar.path, calendarLabel, Icons.Rounded.CalendarMonth))
            if (showLookbackInNavBar) {
                add(BottomPanelItem(Route.Lookback.path, lookbackLabel, Icons.Rounded.History))
            }
            if (showKeywordsInNavBar) {
                add(BottomPanelItem(Route.Keywords.path, peopleLabel, Icons.Rounded.People))
            }
            if (showTodosInNavBar) {
                add(BottomPanelItem(Route.Todos.path, todosLabel, Icons.Rounded.Checklist))
            }
            if (showStatisticsInNavBar) {
                add(
                    BottomPanelItem(
                        Route.Statistics.path,
                        statsLabel,
                        Icons.AutoMirrored.Rounded.TrendingUp
                    )
                )
            }
        }
    }

    var indicatorRoute by remember(items) {
        mutableStateOf(selectedRoute ?: items.firstOrNull()?.route.orEmpty())
    }
    LaunchedEffect(selectedRoute, items) {
        val visibleRoutes = items.map { it.route }.toSet()
        indicatorRoute = when {
            selectedRoute != null && selectedRoute in visibleRoutes -> selectedRoute
            indicatorRoute in visibleRoutes -> indicatorRoute
            else -> items.firstOrNull()?.route.orEmpty()
        }
    }

    val selectedIndex = remember(indicatorRoute, items) {
        items.indexOfFirst { it.route == indicatorRoute }.coerceAtLeast(0)
    }

    AppStaggeredEntrance(
        visible = visible,
        index = 0,
        strength = AppEntranceStrength.HERO
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 4.dp,
            shadowElevation = 0.dp
        ) {
            // The bar grows with the text it holds. Every height below is derived from
            // this one factor so the indicator stays centred: a heightIn on the outer
            // box alone would let it grow while the fixed inner heights clipped anyway.
            val labelScale = density.fontScale.coerceIn(1f, MAX_BOTTOM_PANEL_LABEL_SCALE)
            val containerHeight = (if (showLabels) 92.dp else 76.dp) * labelScale
            val indicatorHeight = (if (showLabels) 62.dp else 48.dp) * labelScale
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(containerHeight)
            ) {
                val itemCount = items.size.coerceAtLeast(1)
                val itemWidth = maxWidth / itemCount
                val targetIndicatorWidth = resolveIndicatorWidth(itemWidth, showLabels)
                val animatedIndicatorWidth by androidx.compose.animation.core.animateDpAsState(
                    targetValue = targetIndicatorWidth,
                    animationSpec = motionPreference.dpSpring(
                        dampingRatio = 0.65f,
                        stiffness = 500f
                    ),
                    label = "bottom_panel_indicator_width"
                )
                val targetIndicatorOffset =
                    (itemWidth * selectedIndex) + ((itemWidth - animatedIndicatorWidth) / 2)
                val animatedIndicatorOffset by androidx.compose.animation.core.animateDpAsState(
                    targetValue = targetIndicatorOffset,
                    animationSpec = motionPreference.dpSpring(
                        dampingRatio = if (motionPreference == AnimationPreference.REDUCED) 0.9f else 0.65f,
                        stiffness = if (motionPreference == AnimationPreference.REDUCED) 520f else 500f
                    ),
                    label = "bottom_panel_indicator_offset"
                )
                val indicatorScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (indicatorRoute != selectedRoute) 1.04f else 1f,
                    animationSpec = motionPreference.floatSpring(
                        dampingRatio = if (motionPreference == AnimationPreference.REDUCED) 0.92f else 0.60f,
                        stiffness = if (motionPreference == AnimationPreference.REDUCED) 520f else 420f
                    ),
                    label = "bottom_panel_indicator_scale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(containerHeight)
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    )

                    Box(
                        modifier = Modifier
                            .offset(
                                x = animatedIndicatorOffset,
                                y = (containerHeight - indicatorHeight) / 2
                            )
                            .width(animatedIndicatorWidth)
                            .height(indicatorHeight)
                            .graphicsLayer {
                                scaleX = indicatorScale
                                scaleY = indicatorScale
                            }
                            .clip(RoundedCornerShape(if (showLabels) 28.dp else 22.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(containerHeight),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        items.forEach { item ->
                            val selected = indicatorRoute == item.route
                            val iconTapMotion = rememberBottomNavIconTapMotion(item.route)
                            val iconColor by animateColorAsState(
                                targetValue = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                animationSpec = motionPreference.tweenSpec(160),
                                label = "bottom_panel_icon_color_${item.route}"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (selected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                animationSpec = motionPreference.tweenSpec(160),
                                label = "bottom_panel_text_color_${item.route}"
                            )
                            val scale by animateFloatAsState(
                                targetValue = if (selected) 1.15f else 1f,
                                animationSpec = motionPreference.floatSpring(
                                    dampingRatio = if (motionPreference == AnimationPreference.REDUCED) 0.92f else 0.60f,
                                    stiffness = if (motionPreference == AnimationPreference.REDUCED) 520f else 420f
                                ),
                                label = "bottom_panel_icon_scale_${item.route}"
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(containerHeight)
                                    .clickable(
                                            interactionSource = remember(item.route) { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                        iconTapMotion.play(motionPreference)
                                        indicatorRoute = item.route
                                        when (item.route) {
                                            Route.Home.path -> onNavigateHome()
                                            Route.Calendar.path -> onNavigateCalendar()
                                            Route.Lookback.path -> onNavigateLookback()
                                            Route.Keywords.path -> onNavigateKeywords()
                                            Route.Todos.path -> onNavigateTodos()
                                            Route.Statistics.path -> onNavigateStatistics()
                                        }
                                    }
                                    .padding(
                                        horizontal = if (showLabels) 8.dp else 12.dp,
                                        vertical = if (showLabels) 12.dp else 14.dp
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height((if (showLabels) 30.dp else 24.dp) * labelScale)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = iconColor,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer {
                                                scaleX = scale * iconTapMotion.scale.value
                                                scaleY = scale * iconTapMotion.scale.value
                                                rotationZ = iconTapMotion.rotation.value
                                                translationY = with(density) { iconTapMotion.liftDp.value.dp.toPx() }
                                            }
                                    )
                                }

                                if (showLabels) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = textColor,
                                        modifier = Modifier.padding(top = 8.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
