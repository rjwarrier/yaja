package com.mj.yaja.ui.screens

import android.util.Log
import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.expressivePressMotion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.widget.HeatmapWidgetProvider
import com.mj.yaja.ui.widget.QuickCaptureWidgetProvider
import com.mj.yaja.ui.widget.QuickTodoWidgetProvider
import com.mj.yaja.ui.widget.TodoListWidgetProvider

@Composable
fun AboutSection(
    onNavigateToHelp: () -> Unit,
    onNavigateToAppLog: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    SettingsSectionHeader(icon = Icons.Rounded.Info, title = stringResource(R.string.nav_help))
    Spacer(modifier = Modifier.height(12.dp))
    val interactionHelp = remember { MutableInteractionSource() }
    ElevatedCard(
        onClick = onNavigateToHelp,
        modifier = Modifier
            .fillMaxWidth()
            .expressivePressMotion(interactionHelp, pressedScale = 0.96f),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        interactionSource = interactionHelp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                contentDescription = stringResource(R.string.nav_help),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.nav_help),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_help_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))

    val interactionLog = remember { MutableInteractionSource() }
    ElevatedCard(
        onClick = onNavigateToAppLog,
        modifier = Modifier
            .fillMaxWidth()
            .expressivePressMotion(interactionLog, pressedScale = 0.96f),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        interactionSource = interactionLog
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Article,
                contentDescription = stringResource(R.string.settings_app_log_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_app_log_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_app_log_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))

    val interactionWeb = remember { MutableInteractionSource() }
    ElevatedCard(
        onClick = { uriHandler.openUri("https://ranjithj.in/yaja/") },
        modifier = Modifier
            .fillMaxWidth()
            .expressivePressMotion(interactionWeb, pressedScale = 0.96f),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        interactionSource = interactionWeb
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = stringResource(R.string.settings_website_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_website_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.settings_website_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun TaskerIntegrationSection(
    onNavigateToTaskerIntegration: () -> Unit,
    onNavigateToShortcodes: () -> Unit
) {
    SettingsSectionHeader(icon = Icons.Rounded.Settings, title = stringResource(R.string.settings_advanced_integrations_title))
    Spacer(modifier = Modifier.height(12.dp))
    val interactionTasker = remember { MutableInteractionSource() }
    ElevatedCard(
        onClick = onNavigateToTaskerIntegration,
        modifier = Modifier
            .fillMaxWidth()
            .expressivePressMotion(interactionTasker, pressedScale = 0.96f),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        interactionSource = interactionTasker
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings_tasker_integration_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_tasker_integration_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_tasker_integration_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))

    val interactionShortcodes = remember { MutableInteractionSource() }
    ElevatedCard(
        onClick = onNavigateToShortcodes,
        modifier = Modifier
            .fillMaxWidth()
            .expressivePressMotion(interactionShortcodes, pressedScale = 0.96f),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        interactionSource = interactionShortcodes
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Article,
                contentDescription = stringResource(R.string.nav_shortcodes),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.nav_shortcodes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_shortcodes_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun SecuritySection(
    isPinEnabled: Boolean,
    onEnablePin: () -> Unit,
    onDisablePin: () -> Unit,
    onChangePin: () -> Unit,
    isBiometricEnabled: Boolean,
    onEnableBiometric: () -> Unit,
    onDisableBiometric: () -> Unit,
    autoLockTimeoutMinutes: Int,
    onAutoLockTimeoutChange: (Int) -> Unit,
    hideTextModeEnabled: Boolean,
    onHideTextModeEnabledChange: (Boolean) -> Unit,
    onNavigateToPrivacyDashboard: () -> Unit
) {
    SettingsSectionHeader(icon = Icons.Rounded.Lock, title = stringResource(R.string.settings_privacy_security_title))

    Spacer(modifier = Modifier.height(12.dp))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_pin_lock_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPinEnabled) {
                            stringResource(R.string.settings_app_locked)
                        } else {
                            stringResource(R.string.settings_app_unlocked)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isPinEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) onEnablePin() else onDisablePin()
                    }
                )
            }

            if (isPinEnabled) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                TextButton(
                    onClick = onChangePin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) { Text(stringResource(R.string.settings_change_pin)) }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.settings_biometric_unlock_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBiometricEnabled) {
                                    stringResource(R.string.settings_biometric_enabled_desc)
                                } else {
                                    stringResource(R.string.settings_biometric_disabled_desc)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { enabled ->
                            try {
                                if (enabled) onEnableBiometric() else onDisableBiometric()
                            } catch (e: Exception) {
                                Log.e("SettingsScreen", "Failed to toggle biometric", e)
                            }
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_autolock_timeout_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = pluralStringResource(
                                    R.plurals.settings_autolock_minutes,
                                    autoLockTimeoutMinutes,
                                    autoLockTimeoutMinutes
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = autoLockTimeoutMinutes.toFloat(),
                        onValueChange = { onAutoLockTimeoutChange(it.toInt()) },
                        valueRange = 1f..30f,
                        steps = 28,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.settings_hide_text_mode_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_hide_text_mode_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = hideTextModeEnabled,
                    onCheckedChange = onHideTextModeEnabledChange
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            TextButton(
                onClick = onNavigateToPrivacyDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) { Text(stringResource(R.string.settings_privacy_dashboard_title)) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_pin_lock_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DataPrivacyDashboardScreen(
    isPinEnabled: Boolean,
    isBiometricEnabled: Boolean,
    autoLockTimeoutMinutes: Int,
    hideTextModeEnabled: Boolean,
    storageLocationText: String,
    formattedBackupDate: String,
    backupReminderDays: Int,
    allowTaskerAccess: Boolean,
    allowTaskerEvents: Boolean,
    includeEntryTextInTaskerEvents: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }
    val quickCaptureCount = remember(appWidgetManager, context) {
        appWidgetManager.getAppWidgetIds(
            ComponentName(context, QuickCaptureWidgetProvider::class.java)
        ).size
    }
    val quickTodoCount = remember(appWidgetManager, context) {
        appWidgetManager.getAppWidgetIds(
            ComponentName(context, QuickTodoWidgetProvider::class.java)
        ).size
    }
    val heatmapCount = remember(appWidgetManager, context) {
        appWidgetManager.getAppWidgetIds(
            ComponentName(context, HeatmapWidgetProvider::class.java)
        ).size
    }
    val todoListCount = remember(appWidgetManager, context) {
        appWidgetManager.getAppWidgetIds(
            ComponentName(context, TodoListWidgetProvider::class.java)
        ).size
    }
    val totalWidgets = quickCaptureCount + quickTodoCount + heatmapCount + todoListCount

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_privacy_dashboard_title)) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsSectionHeader(
                icon = Icons.Rounded.Shield,
                title = stringResource(R.string.settings_privacy_dashboard_overview)
            )

            PrivacyStatusCard(
                title = stringResource(R.string.settings_privacy_storage_title),
                summary = storageLocationText
            )
            PrivacyStatusCard(
                title = stringResource(R.string.settings_privacy_backup_title),
                summary = stringResource(
                    R.string.settings_privacy_backup_summary,
                    formattedBackupDate,
                    backupReminderDays
                )
            )
            PrivacyStatusCard(
                title = stringResource(R.string.settings_privacy_lock_title),
                summary = stringResource(
                    R.string.settings_privacy_lock_summary,
                    if (isPinEnabled) stringResource(R.string.settings_privacy_state_on) else stringResource(R.string.settings_privacy_state_off),
                    if (isBiometricEnabled) stringResource(R.string.settings_privacy_state_on) else stringResource(R.string.settings_privacy_state_off),
                    autoLockTimeoutMinutes
                )
            )
            PrivacyStatusCard(
                title = stringResource(R.string.settings_privacy_hide_mode_title),
                summary = if (hideTextModeEnabled) {
                    stringResource(R.string.settings_privacy_hide_mode_on)
                } else {
                    stringResource(R.string.settings_privacy_hide_mode_off)
                }
            )
            PrivacyStatusCard(
                title = stringResource(R.string.settings_privacy_tasker_title),
                summary = stringResource(
                    R.string.settings_privacy_tasker_summary,
                    if (allowTaskerAccess) stringResource(R.string.settings_privacy_state_on) else stringResource(R.string.settings_privacy_state_off),
                    if (allowTaskerEvents) stringResource(R.string.settings_privacy_state_on) else stringResource(R.string.settings_privacy_state_off),
                    if (includeEntryTextInTaskerEvents) stringResource(R.string.settings_privacy_state_yes) else stringResource(R.string.settings_privacy_state_no)
                )
            )
            PrivacyStatusCard(
                title = stringResource(R.string.settings_privacy_share_title),
                summary = stringResource(R.string.settings_privacy_share_summary)
            )
            PrivacyStatusCard(
                title = stringResource(R.string.settings_privacy_widgets_title),
                summary = stringResource(
                    R.string.settings_privacy_widgets_summary,
                    totalWidgets,
                    quickCaptureCount,
                    quickTodoCount,
                    heatmapCount,
                    todoListCount
                )
            )
        }
    }
}

@Composable
private fun PrivacyStatusCard(
    title: String,
    summary: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
