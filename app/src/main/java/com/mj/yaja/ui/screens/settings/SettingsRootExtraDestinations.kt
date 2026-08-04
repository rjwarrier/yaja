package com.mj.yaja.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mj.yaja.R

@Composable
fun LanguageEntrySection(onNavigateToLanguage: () -> Unit) {
    SettingsDestinationEntry(
        icon = Icons.Rounded.Language,
        title = stringResource(R.string.settings_language),
        subtitle = stringResource(R.string.settings_language_subtitle),
        onClick = onNavigateToLanguage
    )
}

@Composable
fun ReviewInsightsEntrySection(onNavigateToReviewInsights: () -> Unit) {
    SettingsDestinationEntry(
        icon = Icons.Rounded.Info,
        title = stringResource(R.string.settings_section_review_insights),
        subtitle = stringResource(R.string.addentry_review_entry_subtitle),
        onClick = onNavigateToReviewInsights
    )
}

@Composable
fun AdvancedIntegrationsEntrySection(onNavigateToAdvancedIntegrations: () -> Unit) {
    SettingsDestinationEntry(
        icon = Icons.Rounded.Settings,
        title = stringResource(R.string.settings_advanced_integrations_title),
        subtitle = stringResource(R.string.settings_tasker_integration_subtitle),
        onClick = onNavigateToAdvancedIntegrations
    )
}

@Composable
fun HelpAboutEntrySection(onNavigateToHelpAbout: () -> Unit) {
    SettingsDestinationEntry(
        icon = Icons.AutoMirrored.Rounded.HelpOutline,
        title = stringResource(R.string.nav_help),
        subtitle = stringResource(R.string.settings_help_subtitle),
        onClick = onNavigateToHelpAbout
    )
}
