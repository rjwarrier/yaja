package com.mj.yaja.ui.screens

import android.util.Log
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

@Composable
fun PrivacySecurityEntrySection(onNavigateToPrivacySecurity: () -> Unit) {
    SettingsSectionHeader(
        icon = Icons.Rounded.Lock,
        title = stringResource(R.string.settings_privacy_security_title)
    )
    Spacer(modifier = Modifier.height(12.dp))
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToPrivacySecurity),
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
                imageVector = Icons.Rounded.Lock,
                contentDescription = stringResource(R.string.settings_privacy_security_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_privacy_security_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_privacy_security_subtitle),
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
    onEnableHideTextMode: () -> Unit,
    onDisableTaskerTextSharing: () -> Unit,
    onNavigateToTaskerIntegration: () -> Unit,
    onNavigateToPinSetup: () -> Unit,
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
    val riskScore =
        (if (isPinEnabled) 0 else 2) +
            (if (hideTextModeEnabled) 0 else 1) +
            (if (includeEntryTextInTaskerEvents) 2 else 0) +
            (if (allowTaskerAccess || allowTaskerEvents) 1 else 0) +
            (if (totalWidgets > 0) 1 else 0)
    val riskLabel =
        when {
            riskScore >= 5 -> stringResource(R.string.settings_privacy_risk_high)
            riskScore >= 3 -> stringResource(R.string.settings_privacy_risk_medium)
            else -> stringResource(R.string.settings_privacy_risk_low)
        }
    val riskSummary =
        when {
            riskScore >= 5 -> stringResource(R.string.settings_privacy_risk_high_summary)
            riskScore >= 3 -> stringResource(R.string.settings_privacy_risk_medium_summary)
            else -> stringResource(R.string.settings_privacy_risk_low_summary)
        }
    val recommendations = buildList {
        if (!isPinEnabled) add(stringResource(R.string.settings_privacy_recommend_pin))
        if (!hideTextModeEnabled) add(stringResource(R.string.settings_privacy_recommend_hide))
        if (includeEntryTextInTaskerEvents) {
            add(stringResource(R.string.settings_privacy_recommend_tasker_text))
        }
        if (totalWidgets > 0) {
            add(stringResource(R.string.settings_privacy_recommend_widgets))
        }
    }

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

            PrivacyHeroCard(
                riskLabel = riskLabel,
                riskSummary = riskSummary
            )

            DashboardSectionTitle(stringResource(R.string.settings_privacy_surfaces_title))
            PrivacySurfaceRow(
                icon = Icons.Rounded.Lock,
                title = stringResource(R.string.settings_privacy_surface_local_title),
                summary = stringResource(R.string.settings_privacy_surface_local_summary),
                stateLabel = stringResource(R.string.settings_privacy_state_default)
            )
            PrivacySurfaceRow(
                icon = Icons.Rounded.Share,
                title = stringResource(R.string.settings_privacy_share_title),
                summary = stringResource(R.string.settings_privacy_share_summary),
                stateLabel = stringResource(R.string.settings_privacy_state_manual)
            )
            PrivacySurfaceRow(
                icon = Icons.Rounded.Settings,
                title = stringResource(R.string.settings_privacy_tasker_title),
                summary = stringResource(
                    R.string.settings_privacy_tasker_summary,
                    if (allowTaskerAccess) stringResource(R.string.settings_privacy_state_on) else stringResource(R.string.settings_privacy_state_off),
                    if (allowTaskerEvents) stringResource(R.string.settings_privacy_state_on) else stringResource(R.string.settings_privacy_state_off),
                    if (includeEntryTextInTaskerEvents) stringResource(R.string.settings_privacy_state_yes) else stringResource(R.string.settings_privacy_state_no)
                ),
                stateLabel =
                    if (allowTaskerAccess || allowTaskerEvents) {
                        stringResource(R.string.settings_privacy_state_active)
                    } else {
                        stringResource(R.string.settings_privacy_state_off)
                    }
            )
            PrivacySurfaceRow(
                icon = Icons.Rounded.GridView,
                title = stringResource(R.string.settings_privacy_widgets_title),
                summary = stringResource(
                    R.string.settings_privacy_widgets_summary,
                    totalWidgets,
                    quickCaptureCount,
                    quickTodoCount,
                    heatmapCount,
                    todoListCount
                ),
                stateLabel =
                    if (totalWidgets > 0) {
                        stringResource(R.string.settings_privacy_state_visible)
                    } else {
                        stringResource(R.string.settings_privacy_state_none)
                    }
            )

            DashboardSectionTitle(stringResource(R.string.settings_privacy_storage_backup_title))
            PrivacyStatusCard(
                icon = Icons.Rounded.Storage,
                title = stringResource(R.string.settings_privacy_storage_title),
                summary = storageLocationText
            )
            PrivacyStatusCard(
                icon = Icons.Rounded.Archive,
                title = stringResource(R.string.settings_privacy_backup_title),
                summary = stringResource(
                    R.string.settings_privacy_backup_summary,
                    formattedBackupDate,
                    backupReminderDays
                )
            )

            DashboardSectionTitle(stringResource(R.string.settings_privacy_controls_title))
            PrivacyStatusCard(
                icon = Icons.Rounded.Security,
                title = stringResource(R.string.settings_privacy_lock_title),
                summary = stringResource(
                    R.string.settings_privacy_lock_summary,
                    if (isPinEnabled) stringResource(R.string.settings_privacy_state_on) else stringResource(R.string.settings_privacy_state_off),
                    if (isBiometricEnabled) stringResource(R.string.settings_privacy_state_on) else stringResource(R.string.settings_privacy_state_off),
                    autoLockTimeoutMinutes
                )
            )
            PrivacyStatusCard(
                icon = Icons.Rounded.VisibilityOff,
                title = stringResource(R.string.settings_privacy_hide_mode_title),
                summary = if (hideTextModeEnabled) {
                    stringResource(R.string.settings_privacy_hide_mode_on)
                } else {
                    stringResource(R.string.settings_privacy_hide_mode_off)
                }
            )

            if (recommendations.isNotEmpty()) {
                DashboardSectionTitle(stringResource(R.string.settings_privacy_recommendations_title))
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recommendations.forEach { recommendation ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = recommendation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            DashboardSectionTitle(stringResource(R.string.settings_privacy_quick_actions_title))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isPinEnabled) {
                    QuickActionChip(
                        icon = Icons.Rounded.Lock,
                        label = stringResource(R.string.settings_privacy_action_set_pin),
                        onClick = onNavigateToPinSetup
                    )
                }
                if (!hideTextModeEnabled) {
                    QuickActionChip(
                        icon = Icons.Rounded.VisibilityOff,
                        label = stringResource(R.string.settings_privacy_action_enable_hide),
                        onClick = onEnableHideTextMode
                    )
                }
                if (includeEntryTextInTaskerEvents) {
                    QuickActionChip(
                        icon = Icons.Rounded.Settings,
                        label = stringResource(R.string.settings_privacy_action_disable_tasker_text),
                        onClick = onDisableTaskerTextSharing
                    )
                }
                QuickActionChip(
                    icon = Icons.Rounded.NorthEast,
                    label = stringResource(R.string.settings_privacy_action_open_tasker),
                    onClick = onNavigateToTaskerIntegration
                )
            }
        }
    }
}

@Composable
private fun PrivacyStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
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
    }
}

@Composable
private fun PrivacyHeroCard(
    riskLabel: String,
    riskSummary: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_privacy_exposure_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                )
                Text(
                    text = riskSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text(riskLabel) }
            )
        }
    }
}

@Composable
private fun DashboardSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 2.dp, top = 8.dp)
    )
}

@Composable
private fun PrivacySurfaceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String,
    stateLabel: String
) {
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
            AssistChip(
                onClick = {},
                label = { Text(stateLabel) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    )
}
