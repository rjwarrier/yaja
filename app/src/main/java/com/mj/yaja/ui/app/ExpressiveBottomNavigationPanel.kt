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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.dpSpring
import com.mj.yaja.ui.design.floatSpring
import com.mj.yaja.ui.design.rememberAppEntrance
import com.mj.yaja.ui.navigation.Route

private data class BottomPanelItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

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

    val items = remember(
        showLookbackInNavBar,
        showKeywordsInNavBar,
        showTodosInNavBar,
        showStatisticsInNavBar
    ) {
        buildList {
            add(BottomPanelItem(Route.Home.path, "Journal", Icons.AutoMirrored.Rounded.MenuBook))
            add(BottomPanelItem(Route.Calendar.path, "Calendar", Icons.Rounded.CalendarMonth))
            if (showLookbackInNavBar) {
                add(BottomPanelItem(Route.Lookback.path, "Lookback", Icons.Rounded.History))
            }
            if (showKeywordsInNavBar) {
                add(BottomPanelItem(Route.Keywords.path, "People", Icons.Rounded.People))
            }
            if (showTodosInNavBar) {
                add(BottomPanelItem(Route.Todos.path, "Todos", Icons.Rounded.Checklist))
            }
            if (showStatisticsInNavBar) {
                add(
                    BottomPanelItem(
                        Route.Statistics.path,
                        "Stats",
                        Icons.AutoMirrored.Rounded.TrendingUp
                    )
                )
            }
        }
    }

    var indicatorRoute by remember(items) {
        mutableStateOf(currentRoute ?: items.firstOrNull()?.route.orEmpty())
    }
    LaunchedEffect(currentRoute, items) {
        val visibleRoutes = items.map { it.route }.toSet()
        indicatorRoute = when {
            currentRoute != null && currentRoute in visibleRoutes -> currentRoute
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(if (showLabels) 92.dp else 76.dp)
            ) {
                val itemCount = items.size.coerceAtLeast(1)
                val itemWidth = maxWidth / itemCount
                val containerHeight = if (showLabels) 92.dp else 76.dp
                val indicatorHeight = if (showLabels) 62.dp else 48.dp
                val targetIndicatorWidth =
                    (itemWidth - if (showLabels) 18.dp else 20.dp)
                        .coerceAtLeast(if (showLabels) 72.dp else 56.dp)
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
                    targetValue = if (indicatorRoute != currentRoute) 1.04f else 1f,
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
                            val iconColor by animateColorAsState(
                                targetValue = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                label = "bottom_panel_icon_color_${item.route}"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (selected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
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
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
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
                                        .height(if (showLabels) 30.dp else 24.dp)
                                        .heightIn(min = if (showLabels) 30.dp else 24.dp)
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
                                                scaleX = scale
                                                scaleY = scale
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
