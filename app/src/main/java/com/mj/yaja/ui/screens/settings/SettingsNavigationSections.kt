package com.mj.yaja.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.R
import com.mj.yaja.data.NavigationChromeMode

@Composable
fun NavigationSection(
    navigationChromeMode: NavigationChromeMode,
    onNavigationChromeModeChange: (NavigationChromeMode) -> Unit,
    showBottomPanelLabels: Boolean,
    onShowBottomPanelLabelsChange: (Boolean) -> Unit,
    adaptiveBottomNav: Boolean,
    onAdaptiveBottomNavChange: (Boolean) -> Unit,
    showLookbackInNavBar: Boolean,
    onShowLookbackChange: (Boolean) -> Unit,
    showKeywordsInNavBar: Boolean,
    onShowKeywordsChange: (Boolean) -> Unit,
    showTodosInNavBar: Boolean,
    onShowTodosChange: (Boolean) -> Unit,
    showStatistics: Boolean,
    showStatisticsInNavBar: Boolean,
    onShowStatisticsInNavBarChange: (Boolean) -> Unit
) {
    SettingsSectionHeader(
        icon = Icons.Rounded.ViewAgenda,
        title = stringResource(R.string.settings_navigation_title)
    )

    Spacer(modifier = Modifier.height(12.dp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = stringResource(R.string.settings_bottom_nav_style_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_navigation_mode_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_navigation_mode_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small
                    )
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    stringResource(R.string.settings_nav_mode_floating) to NavigationChromeMode.FLOATING_BAR,
                    stringResource(R.string.settings_nav_mode_panel) to NavigationChromeMode.EXPRESSIVE_PANEL
                ).forEach { (label, mode) ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (navigationChromeMode == mode) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable { onNavigationChromeModeChange(mode) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (navigationChromeMode == mode) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = stringResource(R.string.settings_adaptive_bottom_nav_title),
            subtitle = stringResource(R.string.settings_adaptive_bottom_nav_subtitle),
            checked = adaptiveBottomNav,
            onCheckedChange = onAdaptiveBottomNavChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        val maxOptionalItems =
            if (navigationChromeMode == NavigationChromeMode.FLOATING_BAR) 2 else 3
        val selectedOptionalItems =
            listOf(
                showLookbackInNavBar,
                showKeywordsInNavBar,
                showTodosInNavBar,
                showStatistics && showStatisticsInNavBar
            ).count { it }
        val navigationScreensSummary =
            if (navigationChromeMode == NavigationChromeMode.FLOATING_BAR) {
                stringResource(R.string.settings_nav_summary_floating, selectedOptionalItems, maxOptionalItems)
            } else {
                stringResource(R.string.settings_nav_summary_panel, selectedOptionalItems, maxOptionalItems)
            }

        Text(
            text = stringResource(R.string.settings_navigation_screens_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
        )

        Text(
            text = navigationScreensSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        PreferencesSwitchRow(
            title = stringResource(R.string.nav_lookback),
            subtitle = stringResource(R.string.settings_nav_lookback_subtitle),
            checked = showLookbackInNavBar,
            enabled = showLookbackInNavBar || selectedOptionalItems < maxOptionalItems,
            onCheckedChange = onShowLookbackChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = stringResource(R.string.settings_review_people_places),
            subtitle = stringResource(R.string.settings_nav_people_places_subtitle),
            checked = showKeywordsInNavBar,
            enabled = showKeywordsInNavBar || selectedOptionalItems < maxOptionalItems,
            onCheckedChange = onShowKeywordsChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = stringResource(R.string.settings_nav_todos_title),
            subtitle = stringResource(R.string.settings_nav_todos_subtitle),
            checked = showTodosInNavBar,
            enabled = showTodosInNavBar || selectedOptionalItems < maxOptionalItems,
            onCheckedChange = onShowTodosChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = stringResource(R.string.settings_nav_statistics_title),
            subtitle = if (showStatistics) {
                stringResource(R.string.settings_nav_statistics_subtitle_on)
            } else {
                stringResource(R.string.settings_nav_statistics_subtitle_off)
            },
            checked = showStatistics && showStatisticsInNavBar,
            enabled = showStatistics && (showStatisticsInNavBar || selectedOptionalItems < maxOptionalItems),
            onCheckedChange = onShowStatisticsInNavBarChange
        )

        if (navigationChromeMode == NavigationChromeMode.EXPRESSIVE_PANEL) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            PreferencesSwitchRow(
                title = stringResource(R.string.settings_show_labels_bottom_panel_title),
                subtitle = stringResource(R.string.settings_show_labels_bottom_panel_subtitle),
                checked = showBottomPanelLabels,
                onCheckedChange = onShowBottomPanelLabelsChange
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun PreferencesSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                }
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun GesturesSection(
    entryDeleteSelectionEnabled: Boolean,
    onEntryDeleteSelectionEnabledChange: (Boolean) -> Unit,
    swipeToNavigateDatesEnabled: Boolean,
    onSwipeToNavigateDatesEnabledChange: (Boolean) -> Unit,
    enableDragAndDrop: Boolean,
    onEnableDragAndDropChange: (Boolean) -> Unit
) {
    SettingsSectionHeader(
        icon = Icons.Rounded.Fingerprint,
        title = stringResource(R.string.settings_gestures_title)
    )

    Spacer(modifier = Modifier.height(12.dp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        PreferencesSwitchRow(
            title = stringResource(R.string.settings_entry_delete_title),
            subtitle = stringResource(R.string.settings_entry_delete_subtitle),
            checked = entryDeleteSelectionEnabled,
            onCheckedChange = onEntryDeleteSelectionEnabledChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = stringResource(R.string.settings_swipe_navigate_title),
            subtitle = stringResource(R.string.settings_swipe_navigate_subtitle),
            checked = swipeToNavigateDatesEnabled,
            onCheckedChange = onSwipeToNavigateDatesEnabledChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = stringResource(R.string.settings_drag_reorder_title),
            subtitle = stringResource(R.string.settings_drag_reorder_subtitle),
            checked = enableDragAndDrop,
            onCheckedChange = onEnableDragAndDropChange
        )
    }

    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun NavigationGesturesEntrySection(onNavigateToNavigationGestures: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToNavigationGestures),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ViewAgenda,
                contentDescription = stringResource(R.string.settings_section_navigation_gestures),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_section_navigation_gestures),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_navigation_gestures_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
}
