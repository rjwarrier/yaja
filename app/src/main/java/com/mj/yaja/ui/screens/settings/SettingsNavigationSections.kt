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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.data.NavigationChromeMode

@Composable
fun NavigationSection(
    navigationChromeMode: NavigationChromeMode,
    onNavigationChromeModeChange: (NavigationChromeMode) -> Unit,
    showBottomPanelLabels: Boolean,
    onShowBottomPanelLabelsChange: (Boolean) -> Unit,
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
        title = "Navigation"
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
            text = "Bottom Navigation Style",
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
                    text = "Navigation Mode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose between the floating nav bar and the full bottom panel.",
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
                    "Floating" to NavigationChromeMode.FLOATING_BAR,
                    "Panel" to NavigationChromeMode.EXPRESSIVE_PANEL
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

        val maxOptionalItems =
            if (navigationChromeMode == NavigationChromeMode.FLOATING_BAR) 2 else 3
        val selectedOptionalItems =
            listOf(
                showLookbackInNavBar,
                showKeywordsInNavBar,
                showTodosInNavBar,
                showStatistics && showStatisticsInNavBar
            ).count { it }
        val selectionSummary =
            if (navigationChromeMode == NavigationChromeMode.FLOATING_BAR) {
                "Journal and Calendar always stay in the floating bar. Pick up to 2 more screens."
            } else {
                "Journal and Calendar always stay in the bottom panel. Pick up to 3 more screens."
            }

        Text(
            text = "Navigation Screens",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
        )

        Text(
            text = "$selectionSummary Currently selected: $selectedOptionalItems of $maxOptionalItems.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        PreferencesSwitchRow(
            title = "Lookback",
            subtitle = "Show Lookback in the active navigation bar or panel.",
            checked = showLookbackInNavBar,
            enabled = showLookbackInNavBar || selectedOptionalItems < maxOptionalItems,
            onCheckedChange = onShowLookbackChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = "People & Places",
            subtitle = "Show People & Places in the active navigation bar or panel.",
            checked = showKeywordsInNavBar,
            enabled = showKeywordsInNavBar || selectedOptionalItems < maxOptionalItems,
            onCheckedChange = onShowKeywordsChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = "Todos",
            subtitle = "Show Todos in the active navigation bar or panel.",
            checked = showTodosInNavBar,
            enabled = showTodosInNavBar || selectedOptionalItems < maxOptionalItems,
            onCheckedChange = onShowTodosChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = "Statistics",
            subtitle = if (showStatistics) {
                "Show Statistics in the active navigation bar or panel."
            } else {
                "Turn Statistics on first to place it in navigation."
            },
            checked = showStatistics && showStatisticsInNavBar,
            enabled = showStatistics && (showStatisticsInNavBar || selectedOptionalItems < maxOptionalItems),
            onCheckedChange = onShowStatisticsInNavBarChange
        )

        if (navigationChromeMode == NavigationChromeMode.EXPRESSIVE_PANEL) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            PreferencesSwitchRow(
                title = "Show Labels In Bottom Panel",
                subtitle = "Display text labels under icons in the bottom panel.",
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
        title = "Gestures"
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
            title = "Entry Delete",
            subtitle = "Long-press an entry to select it. Tap more entries to select multiple, then use the centered Delete button. Tap empty space to clear selection.",
            checked = entryDeleteSelectionEnabled,
            onCheckedChange = onEntryDeleteSelectionEnabledChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = "Swipe to Navigate Dates",
            subtitle = "Swipe right-to-left to move forward one date, and left-to-right to move backward.",
            checked = swipeToNavigateDatesEnabled,
            onCheckedChange = onSwipeToNavigateDatesEnabledChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = "Enable Drag-to-Reorder",
            subtitle = "Long-press entries to rearrange them",
            checked = enableDragAndDrop,
            onCheckedChange = onEnableDragAndDropChange
        )
    }

    Spacer(modifier = Modifier.height(32.dp))
}
