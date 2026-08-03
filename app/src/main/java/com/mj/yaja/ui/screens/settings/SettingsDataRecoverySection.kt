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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.R
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
    onExportObsidianVault: () -> Unit,
    onRefreshCache: () -> Unit,
    swipeToSyncEnabled: Boolean,
    onSwipeToSyncEnabledChange: (Boolean) -> Unit,
    largeJournalSafeMode: Boolean,
    onLargeJournalSafeModeChange: (Boolean) -> Unit,
    showOnboardingNextLaunch: Boolean,
    onShowOnboardingNextLaunchChange: (Boolean) -> Unit,
    versionHistoryEnabled: Boolean,
    onVersionHistoryEnabledChange: (Boolean) -> Unit,
    onNavigateToVersionHistory: () -> Unit,
    importState: JournalViewModel.ImportState,
    onLaunchDayOneImport: () -> Unit,
    onLaunchJournalisticImport: () -> Unit,
    onLaunchMarkdownFolderImport: () -> Unit,
    onCancelImport: () -> Unit,
    onResetImportState: () -> Unit
) {
    SettingsSectionHeader(
        icon = Icons.Rounded.Storage,
        title = stringResource(R.string.settings_data_recovery_title)
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
                    text = stringResource(R.string.settings_swipe_rebuild_index),
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
            title = stringResource(R.string.settings_large_journal_safe_mode_title),
            subtitle = stringResource(R.string.settings_large_journal_safe_mode_subtitle),
            checked = largeJournalSafeMode,
            onCheckedChange = onLargeJournalSafeModeChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = stringResource(R.string.settings_onboarding_next_launch_title),
            subtitle = stringResource(R.string.settings_onboarding_next_launch_subtitle),
            checked = showOnboardingNextLaunch,
            onCheckedChange = onShowOnboardingNextLaunchChange
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
                    text = stringResource(R.string.settings_version_history_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_version_history_subtitle),
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
        text = stringResource(R.string.settings_storage_location_label),
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
                    ) { Text(stringResource(R.string.settings_reset_to_default)) }
                }
                TextButton(
                    onClick = onChooseFolder,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) { Text(stringResource(R.string.settings_choose_folder)) }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.settings_backup),
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
                text = stringResource(R.string.settings_backup_description),
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
                    text = stringResource(R.string.settings_last_backup, formattedBackupDate),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_backup_reminder_age_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (backupReminderDays == 0) {
                            stringResource(R.string.settings_backup_reminder_disabled)
                        } else {
                            stringResource(R.string.settings_backup_reminder_desc, backupReminderDays)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = backupReminderDays.toFloat(),
                        onValueChange = { onBackupReminderDaysChange(it.toInt()) },
                        valueRange = 0f..60f,
                        steps = 59,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = if (backupReminderDays == 0) {
                            stringResource(R.string.settings_backup_reminder_days_disabled)
                        } else {
                            stringResource(R.string.settings_backup_reminder_days, backupReminderDays)
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
                    ) { Text(stringResource(R.string.settings_restore_button)) }
                    TextButton(
                        onClick = onBackupNow,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text(stringResource(R.string.settings_backup_now_button)) }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.settings_obsidian_export_title),
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
                text = stringResource(R.string.settings_obsidian_export_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                TextButton(
                    onClick = onExportObsidianVault,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) { Text(stringResource(R.string.settings_obsidian_export_button)) }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.settings_db_index_management_label),
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
                text = stringResource(R.string.settings_db_index_management_desc),
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
                    Text(stringResource(R.string.settings_rebuild_tools_button))
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
            text = stringResource(R.string.settings_import_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(
            onClick = { showImportInfo = true },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = stringResource(R.string.settings_import_info_cd),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showImportInfo) {
        AlertDialog(
            onDismissRequest = { showImportInfo = false },
            title = { Text(stringResource(R.string.settings_import_info_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_import_day_one_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.settings_import_day_one_bullet1), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.settings_import_day_one_bullet2), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.settings_import_day_one_bullet3), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.settings_import_perf_warning_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        stringResource(R.string.settings_import_perf_warning_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportInfo = false }) { Text(stringResource(R.string.settings_got_it)) }
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
                                ) { Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelSmall) }
                            }
                            is JournalViewModel.ImportState.Success -> {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    stringResource(R.string.settings_import_success, s.newDays, s.mergedDays, s.skippedEntries),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = onResetImportState,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) { Text(stringResource(R.string.action_close), style = MaterialTheme.typography.labelSmall) }
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
                                ) { Text(stringResource(R.string.action_close), style = MaterialTheme.typography.labelSmall) }
                            }
                            else -> Unit
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }

            ImportSourceRow(
                title = stringResource(R.string.settings_import_source_day_one),
                subtitle = stringResource(R.string.settings_import_source_day_one_subtitle),
                enabled = importState !is JournalViewModel.ImportState.Running,
                onClick = onLaunchDayOneImport
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            ImportSourceRow(
                title = stringResource(R.string.settings_import_source_journalistic),
                subtitle = stringResource(R.string.settings_import_source_journalistic_subtitle),
                enabled = importState !is JournalViewModel.ImportState.Running,
                onClick = onLaunchJournalisticImport
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            ImportSourceRow(
                title = stringResource(R.string.settings_import_source_markdown_folder),
                subtitle = stringResource(R.string.settings_import_source_markdown_folder_subtitle),
                enabled = importState !is JournalViewModel.ImportState.Running,
                onClick = onLaunchMarkdownFolderImport
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun DataRecoveryEntrySection(onNavigateToDataRecovery: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToDataRecovery),
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
                imageVector = Icons.Rounded.Storage,
                contentDescription = stringResource(R.string.settings_data_recovery_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_data_recovery_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_data_recovery_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
