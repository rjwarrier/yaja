package com.mj.yaja.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.ui.viewmodel.JournalViewModel
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.tweenSpec
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

@Composable
fun DataAndStorageSection(
    storageLocationText: String,
    hasCustomStorage: Boolean,
    onResetStorage: () -> Unit,
    onChooseFolder: () -> Unit,
    formattedBackupDate: String,
    backupReminderDays: Int,
    onBackupNow: () -> Unit,
    onBackupReminderDaysChange: (Int) -> Unit,
    onRestoreBackup: () -> Unit,
    onRefreshCache: () -> Unit,
    swipeToSyncEnabled: Boolean,
    onSwipeToSyncEnabledChange: (Boolean) -> Unit,
    largeJournalSafeMode: Boolean,
    onLargeJournalSafeModeChange: (Boolean) -> Unit,
    versionHistoryEnabled: Boolean,
    onVersionHistoryEnabledChange: (Boolean) -> Unit,
    onNavigateToVersionHistory: () -> Unit,
    importState: JournalViewModel.ImportState,
    onLaunchDayOneImport: () -> Unit,
    onLaunchJournalisticImport: () -> Unit,
    onCancelImport: () -> Unit,
    onResetImportState: () -> Unit
) {
    SettingsSectionHeader(
        icon = Icons.Rounded.Storage,
        title = "Data & Recovery"
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Swipe down on home screen to rebuild index",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Switch(
                checked = swipeToSyncEnabled,
                onCheckedChange = onSwipeToSyncEnabledChange
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = "Large Journal Safe Mode",
            subtitle = "When a very large journal is detected, defer nonessential startup stats work so the app opens faster and more safely.",
            checked = largeJournalSafeMode,
            onCheckedChange = onLargeJournalSafeModeChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToVersionHistory)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Version History",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Keep safe copies before entry edits, deletes, labels, and revisit changes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Switch(
                checked = versionHistoryEnabled,
                onCheckedChange = onVersionHistoryEnabledChange
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Storage Location",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
    )

    Spacer(modifier = Modifier.height(6.dp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = storageLocationText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                if (hasCustomStorage) {
                    TextButton(
                        onClick = onResetStorage,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text("Reset to Default") }
                }
                TextButton(
                    onClick = onChooseFolder,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) { Text("Choose Folder") }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Backup",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
    )

    Spacer(modifier = Modifier.height(6.dp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Create one Yaja backup zip containing journal entries, shortcodes, date keywords, and People & Places data. Restore can merge it back safely later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Last Backup: $formattedBackupDate",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Backup reminder age",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (backupReminderDays == 0) {
                            "Disabled"
                        } else {
                            "Highlight backup in sidebar when older than $backupReminderDays days."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = backupReminderDays.toFloat(),
                        onValueChange = { onBackupReminderDaysChange(it.toInt()) },
                        valueRange = 0f..30f,
                        steps = 29,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = if (backupReminderDays == 0) {
                            "0 days (disabled)"
                        } else {
                            "$backupReminderDays days"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onRestoreBackup,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text("Restore") }
                    TextButton(
                        onClick = onBackupNow,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text("Backup Now") }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Database Index Management",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
    )

    Spacer(modifier = Modifier.height(6.dp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Open focused rebuild tools for todos, People & Places, or a full database index rebuild.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onRefreshCache,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rebuild Tools")
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    var showImportInfo by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
    ) {
        Text(
            text = "Import",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(
            onClick = { showImportInfo = true },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "Import information",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showImportInfo) {
        AlertDialog(
            onDismissRequest = { showImportInfo = false },
            title = { Text("Import Information") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Day One Import", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("- Export from Day One without images", style = MaterialTheme.typography.bodySmall)
                        Text("- Only text data will be imported", style = MaterialTheme.typography.bodySmall)
                        Text("- Timestamp and location data (if available) will be preserved", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Performance Warning",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Importing a large number of entries (500+) may take several minutes. You can navigate away and the import will continue in the background.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportInfo = false }) { Text("Got it") }
            }
        )
    }

    Spacer(modifier = Modifier.height(6.dp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            val preference = LocalAnimationPreference.current
            androidx.compose.animation.AnimatedVisibility(
                visible = importState !is JournalViewModel.ImportState.Idle,
                enter = preference.enterOrNone(
                    fadeIn(preference.floatTween(150)) + expandVertically(preference.tweenSpec(200))
                ),
                exit = preference.exitOrNone(
                    fadeOut(preference.floatTween(100)) + shrinkVertically(preference.tweenSpec(180))
                )
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (val s = importState) {
                            is JournalViewModel.ImportState.Running -> {
                                Column(Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 1.5.dp
                                        )
                                        Text("${s.current}/${s.total}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    LinearProgressIndicator(
                                        progress = { s.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp),
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                                TextButton(
                                    onClick = onCancelImport,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) { Text("Cancel", style = MaterialTheme.typography.labelSmall) }
                            }
                            is JournalViewModel.ImportState.Success -> {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "${s.newDays} new days, ${s.mergedDays} merged, ${s.skippedEntries} skipped",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = onResetImportState,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) { Text("Close", style = MaterialTheme.typography.labelSmall) }
                            }
                            is JournalViewModel.ImportState.Error -> {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    s.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = onResetImportState,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) { Text("Close", style = MaterialTheme.typography.labelSmall) }
                            }
                            else -> Unit
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }

            ImportSourceRow(
                title = "Day One",
                subtitle = ".zip or .json export",
                enabled = importState !is JournalViewModel.ImportState.Running,
                onClick = onLaunchDayOneImport
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            ImportSourceRow(
                title = "Journalistic",
                subtitle = ".json export",
                enabled = importState !is JournalViewModel.ImportState.Running,
                onClick = onLaunchJournalisticImport
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
private fun ImportSourceRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Rounded.FileOpen,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
