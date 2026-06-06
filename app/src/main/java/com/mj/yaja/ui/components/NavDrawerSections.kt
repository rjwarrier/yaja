package com.mj.yaja.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.IntegrationInstructions
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun DrawerPrimarySection(
        currentRoute: String,
        syncProgress: Float?,
        backgroundWorkLabel: String?,
        syncStartedAtMillis: Long?,
        onNavigateToJournal: () -> Unit,
        onNavigateToCalendar: () -> Unit
) {
        NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null) },
                label = { Text("Journal") },
                selected = currentRoute == "home",
                onClick = onNavigateToJournal,
                modifier = Modifier.padding(horizontal = 12.dp)
        )
        if (syncProgress != null) {
                Spacer(Modifier.height(12.dp))
                DrawerSectionHeader("Status")
                val syncPercent = (syncProgress * 100).toInt()
                val syncEta = estimateNavRemainingTimeText(syncProgress, syncStartedAtMillis)
                val syncLabel =
                        backgroundWorkLabel ?: if (syncProgress >= 0.9f) {
                                "Finishing index rebuild..."
                        } else {
                                "Rebuilding database index"
                        }
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.Surface(
                        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium
                ) {
                        Column(
                                modifier =
                                        Modifier.padding(
                                                horizontal = 14.dp,
                                                vertical = 10.dp
                                        )
                        ) {
                                Text(
                                        text = syncLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(6.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                        progress = { syncProgress.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                        text = if (syncEta != null) "$syncPercent% - $syncEta" else "$syncPercent%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
        }
        Spacer(Modifier.height(8.dp))
        NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                label = { Text("Calendar") },
                selected = currentRoute == "calendar",
                onClick = onNavigateToCalendar,
                modifier = Modifier.padding(horizontal = 12.dp)
        )
}

@Composable
internal fun DrawerPowerFeaturesSection(
        currentRoute: String,
        showLookbackInNavBar: Boolean,
        showKeywordsInNavBar: Boolean,
        showTodosInNavBar: Boolean,
        showStatistics: Boolean,
        showStatisticsInNavBar: Boolean,
        onNavigateToLookback: () -> Unit,
        onNavigateToKeywords: () -> Unit,
        onNavigateToStatistics: () -> Unit,
        onNavigateToTodos: () -> Unit
) {
        Spacer(Modifier.height(12.dp))
        if (!showLookbackInNavBar) {
                NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                        label = { Text("Lookback") },
                        selected = currentRoute == "lookback",
                        onClick = onNavigateToLookback,
                        modifier = Modifier.padding(horizontal = 12.dp)
                )
        }
        if (!showKeywordsInNavBar) {
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.People, contentDescription = null) },
                        label = { Text("People & Places") },
                        selected = currentRoute == "keywords",
                        onClick = onNavigateToKeywords,
                        modifier = Modifier.padding(horizontal = 12.dp)
                )
        }
        if (showStatistics && !showStatisticsInNavBar) {
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null) },
                        label = { Text("Statistics") },
                        selected = currentRoute == "statistics",
                        onClick = onNavigateToStatistics,
                        modifier = Modifier.padding(horizontal = 12.dp)
                )
        }
        if (!showTodosInNavBar) {
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Checklist, contentDescription = null) },
                        label = { Text("Todos") },
                        selected = currentRoute == "todos",
                        onClick = onNavigateToTodos,
                        modifier = Modifier.padding(horizontal = 12.dp)
                )
        }
}

@Composable
internal fun DrawerWritingToolsSection(
        currentRoute: String,
        onNavigateToShortcodes: () -> Unit
) {
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))
        NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.IntegrationInstructions, contentDescription = null) },
                label = { Text("Shortcodes") },
                selected = currentRoute == "shortcodes",
                onClick = onNavigateToShortcodes,
                modifier = Modifier.padding(horizontal = 12.dp)
        )
}

@Composable
internal fun DrawerAdvancedAndAppSection(
        currentRoute: String,
        showBackupReminder: Boolean,
        syncProgress: Float?,
        backgroundWorkLabel: String?,
        syncStartedAtMillis: Long?,
        onNavigateToRebuildCache: () -> Unit,
        onBackupData: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToHelp: () -> Unit
) {
        HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.Cached, contentDescription = null) },
                label = { Text("Rebuild Database Index") },
                selected = currentRoute == "rebuild_cache",
                onClick = onNavigateToRebuildCache,
                modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.Backup, contentDescription = null) },
                label = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Backup Data")
                                if (showBackupReminder) {
                                        Text(
                                                text = "backup is too old...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                        )
                                }
                        }
                },
                selected = false,
                onClick = onBackupData,
                colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor =
                                if (showBackupReminder) {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f)
                                } else {
                                        Color.Transparent
                                },
                        unselectedIconColor =
                                if (showBackupReminder) {
                                        MaterialTheme.colorScheme.error
                                } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        unselectedTextColor =
                                if (showBackupReminder) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                }
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
        )
        if (syncProgress != null || backgroundWorkLabel != null) {
                DrawerCacheStatus(
                        progress = syncProgress,
                        label = backgroundWorkLabel ?: "Refreshing index",
                        startedAtMillis = syncStartedAtMillis
                )
        }
        Spacer(Modifier.height(8.dp))
        NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                label = { Text("Settings") },
                selected = currentRoute == "settings",
                onClick = onNavigateToSettings,
                modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(8.dp))
        NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Rounded.Help, contentDescription = null) },
                label = { Text("Help & About") },
                selected = currentRoute == "help",
                onClick = onNavigateToHelp,
                modifier = Modifier.padding(horizontal = 12.dp)
        )
}

@Composable
internal fun DrawerSectionHeader(
        title: String,
        modifier: Modifier = Modifier
) {
        Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.padding(horizontal = 28.dp)
        )
}
