package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.yaja.R
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.viewmodel.JournalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmallSettingsScreenScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        AppScreenReveal(visible = true, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        }
    }
}

@Composable
fun LanguageSettingsScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.rootSettingsUiState.collectAsStateWithLifecycle()

    SmallSettingsScreenScaffold(
        title = stringResource(R.string.settings_language),
        onNavigateBack = onNavigateBack
    ) {
        LanguageSection(
            appLanguage = uiState.appLanguage,
            onLanguageSelected = { viewModel.setAppLanguage(it) }
        )
    }
}

@Composable
fun ReviewInsightsSettingsScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.rootSettingsUiState.collectAsStateWithLifecycle()

    SmallSettingsScreenScaffold(
        title = stringResource(R.string.settings_section_review_insights),
        onNavigateBack = onNavigateBack
    ) {
        ReviewAndInsightsSection(
            entryReviewEnabled = uiState.entryReviewEnabled,
            onEntryReviewEnabledChange = { viewModel.setEntryReviewEnabled(it) },
            keywordHighlightingEnabled = uiState.keywordHighlightingEnabled,
            onKeywordHighlightingEnabledChange = {
                viewModel.setKeywordHighlightingEnabled(it)
            },
            fuzzyThreshold = uiState.fuzzyThreshold,
            onFuzzyThresholdChange = { viewModel.setKeywordFuzzyThreshold(it) }
        )
    }
}

@Composable
fun AdvancedIntegrationsSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTaskerIntegration: () -> Unit,
    onNavigateToShortcodes: () -> Unit
) {
    SmallSettingsScreenScaffold(
        title = stringResource(R.string.settings_advanced_integrations_title),
        onNavigateBack = onNavigateBack
    ) {
        TaskerIntegrationSection(
            onNavigateToTaskerIntegration = onNavigateToTaskerIntegration,
            onNavigateToShortcodes = onNavigateToShortcodes,
            showHeader = false
        )
    }
}

@Composable
fun HelpAboutSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToAppLog: () -> Unit
) {
    SmallSettingsScreenScaffold(
        title = stringResource(R.string.nav_help),
        onNavigateBack = onNavigateBack
    ) {
        AboutSection(
            onNavigateToHelp = onNavigateToHelp,
            onNavigateToAppLog = onNavigateToAppLog,
            showHeader = false
        )
    }
}
