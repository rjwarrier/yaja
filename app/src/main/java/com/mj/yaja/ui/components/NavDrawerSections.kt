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
import androidx.compose.material.icons.rounded.Home
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mj.yaja.R

@Composable
internal fun DrawerPrimarySection(
        currentRoute: String,
        syncProgress: Float?,
        backgroundWorkLabel: String?,
        syncStartedAtMillis: Long?,
        onNavigateToDashboard: () -> Unit,
        onNavigateToJournal: () -> Unit,
        onNavigateToCalendar: () -> Unit
) {
        NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_home)) },
                selected = currentRoute == "dashboard",
                onClick = onNavigateToDashboard,
                modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_journal)) },
                selected = currentRoute == "home",
                onClick = onNavigateToJournal,
                modifier = Modifier.padding(horizontal = 12.dp)
        )
        if (syncProgress != null) {
                Spacer(Modifier.height(12.dp))
                DrawerSectionHeader(stringResource(R.string.nav_status_header))
                val syncPercent = (syncProgress * 100).toInt()
                val syncEta = estimateNavRemainingTimeText(syncProgress, syncStartedAtMillis)
                val syncLabel =
                        backgroundWorkLabel ?: if (syncProgress >= 0.9f) {
                                stringResource(R.string.nav_finishing_index_rebuild)
                        } else {
                                stringResource(R.string.nav_rebuilding_index)
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
                                        text = if (syncEta != null) {
                                                stringResource(R.string.nav_sync_percent_eta_format, syncPercent, syncEta)
                                        } else {
                                                stringResource(R.string.nav_sync_percent_format, syncPercent)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
        }
        Spacer(Modifier.height(8.dp))
        NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_calendar)) },
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
                        label = { Text(stringResource(R.string.nav_lookback)) },
                        selected = currentRoute == "lookback",
                        onClick = onNavigateToLookback,
                        modifier = Modifier.padding(horizontal = 12.dp)
                )
        }
        if (!showKeywordsInNavBar) {
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.People, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_people_places)) },
                        selected = currentRoute == "keywords",
                        onClick = onNavigateToKeywords,
                        modifier = Modifier.padding(horizontal = 12.dp)
                )
        }
        if (showStatistics && !showStatisticsInNavBar) {
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_statistics)) },
                        selected = currentRoute == "statistics",
                        onClick = onNavigateToStatistics,
                        modifier = Modifier.padding(horizontal = 12.dp)
                )
        }
        if (!showTodosInNavBar) {
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Checklist, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_todos)) },
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
                label = { Text(stringResource(R.string.nav_shortcodes)) },
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
                label = { Text(stringResource(R.string.nav_rebuild_index)) },
                selected = currentRoute == "rebuild_cache",
                onClick = onNavigateToRebuildCache,
                modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.Backup, contentDescription = null) },
                label = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(stringResource(R.string.nav_backup_data))
                                if (showBackupReminder) {
                                        Text(
                                                text = stringResource(R.string.nav_backup_too_old),
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
                        label = backgroundWorkLabel ?: stringResource(R.string.nav_refreshing_index),
                        startedAtMillis = syncStartedAtMillis
                )
        }
        Spacer(Modifier.height(8.dp))
        NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_settings)) },
                selected = currentRoute == "settings",
                onClick = onNavigateToSettings,
                modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(8.dp))
        NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Rounded.Help, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_help)) },
                selected = currentRoute == "help_about_settings",
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
