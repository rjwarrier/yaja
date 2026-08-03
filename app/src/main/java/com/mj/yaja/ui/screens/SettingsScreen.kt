package com.mj.yaja.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.tweenSpec
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import com.mj.yaja.R
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.data.DateKeywordEntry
import com.mj.yaja.data.EntryStyle
import com.mj.yaja.ui.viewmodel.JournalViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import kotlin.math.roundToInt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit,
        onNavigateBack: () -> Unit,
        onNavigateToPinSetup: () -> Unit = {},
        onNavigateToPinDisable: () -> Unit,
        onNavigateToTaskerIntegration: () -> Unit,
        onNavigateToRebuildCache: () -> Unit,
        onNavigateToVersionHistory: () -> Unit,
        onNavigateToAppearance: () -> Unit,
        onNavigateToJournalExperience: () -> Unit,
        onNavigateToNavigationGestures: () -> Unit,
        onNavigateToPrivacySecurity: () -> Unit,
        onNavigateToDataRecovery: () -> Unit,
        onNavigateToHelp: () -> Unit,
        onNavigateToAppLog: () -> Unit,
        onNavigateToShortcodes: () -> Unit,
        onNavigateToPrivacyDashboard: () -> Unit,
        onNavigateToJournal: () -> Unit,
        onNavigateToCalendar: () -> Unit,
        onNavigateToLookback: () -> Unit
) {
        val scope = rememberCoroutineScope()
        val languageRequester = remember { BringIntoViewRequester() }
        val reviewRequester = remember { BringIntoViewRequester() }
        val integrationsRequester = remember { BringIntoViewRequester() }
        val helpRequester = remember { BringIntoViewRequester() }
        val uiState by viewModel.rootSettingsUiState.collectAsStateWithLifecycle()

        var settingsSearchQuery by rememberSaveable { mutableStateOf("") }
        var settingsSearchHistory by rememberSaveable { mutableStateOf(listOf<String>()) }
        var showSettingsSuggestions by remember { mutableStateOf(false) }

        val settingsSearchTargets =
                SettingsSearchRegistry.entries.map { entry ->
                        LocalizedSettingsSearchEntry(
                                id = entry.id,
                                title = stringResource(entry.titleRes),
                                section = stringResource(entry.sectionRes),
                                keywords = entry.keywords,
                                action = entry.action
                        )
                }
        val settingsSearchTargetsById =
                remember(settingsSearchTargets) { settingsSearchTargets.associateBy { it.id.name } }
        val settingsSearchHistoryTargets =
                remember(settingsSearchHistory, settingsSearchTargetsById) {
                        settingsSearchHistory.mapNotNull { settingsSearchTargetsById[it] }
                }
        val filteredSettingsTargets =
                remember(settingsSearchQuery, settingsSearchTargets) {
                        SettingsSearchMatcher.search(settingsSearchQuery, settingsSearchTargets)
                }

        fun runSettingsSearchAction(action: SettingsSearchAction) {
                when (action) {
                        is SettingsSearchAction.OpenDestination -> {
                                when (action.destination) {
                                        SettingsDestinationId.APPEARANCE -> onNavigateToAppearance()
                                        SettingsDestinationId.JOURNAL_EXPERIENCE -> onNavigateToJournalExperience()
                                        SettingsDestinationId.NAVIGATION_GESTURES -> onNavigateToNavigationGestures()
                                        SettingsDestinationId.PRIVACY_SECURITY -> onNavigateToPrivacySecurity()
                                        SettingsDestinationId.PRIVACY_DASHBOARD -> onNavigateToPrivacyDashboard()
                                        SettingsDestinationId.DATA_RECOVERY -> onNavigateToDataRecovery()
                                }
                        }
                        is SettingsSearchAction.ScrollTo -> {
                                val requester =
                                        when (action.anchor) {
                                                SettingsSearchAnchor.LANGUAGE -> languageRequester
                                                SettingsSearchAnchor.REVIEW_INSIGHTS -> reviewRequester
                                                SettingsSearchAnchor.ADVANCED_INTEGRATIONS -> integrationsRequester
                                                SettingsSearchAnchor.HELP_ABOUT -> helpRequester
                                        }
                                scope.launch { requester.bringIntoView() }
                        }
                }
        }

        fun jumpToSettingsTarget(target: LocalizedSettingsSearchEntry) {
                settingsSearchQuery = target.title
                settingsSearchHistory =
                        listOf(target.id.name) +
                                settingsSearchHistory.filterNot { it == target.id.name }.take(4)
                showSettingsSuggestions = false
                runSettingsSearchAction(target.action)
        }
        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                stringResource(R.string.settings_title),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                },
                                navigationIcon = {
                                        com.mj.yaja.ui.components.AnimatedMenuButton(
                                                onClick = onOpenDrawer,
                                                modifier = Modifier.padding(start = 8.dp)
                                        )
                                },
                                colors =
                                        TopAppBarDefaults.topAppBarColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.background,
                                                titleContentColor =
                                                        MaterialTheme.colorScheme.primary,
                                                navigationIconContentColor =
                                                        MaterialTheme.colorScheme.onSurface,
                                                actionIconContentColor =
                                                        MaterialTheme.colorScheme.onSurface
                                        )
                        )
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
        ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        AppScreenReveal(
                                visible = true,
                                modifier = Modifier.fillMaxSize()
                        ) {
                                Column(
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .padding(horizontal = 20.dp)
                                                        .verticalScroll(rememberScrollState())
                                ) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        SettingsSearchCard(
                                                query = settingsSearchQuery,
                                                onQueryChange = {
                                                        settingsSearchQuery = it
                                                        showSettingsSuggestions = true
                                                },
                                                history = settingsSearchHistoryTargets,
                                                filteredTargets = filteredSettingsTargets,
                                                showSuggestions = showSettingsSuggestions,
                                                onHistoryTap = { jumpToSettingsTarget(it) },
                                                onTargetTap = { jumpToSettingsTarget(it) },
                                                onClearQuery = {
                                                        settingsSearchQuery = ""
                                                        showSettingsSuggestions = false
                                                },
                                                onDismissSuggestions = {
                                                        showSettingsSuggestions = false
                                                }
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))

                                        AppearanceEntrySection(onNavigateToAppearance = onNavigateToAppearance)

                                        JournalExperienceEntrySection(
                                                onNavigateToJournalExperience =
                                                        onNavigateToJournalExperience
                                        )

                                        NavigationGesturesEntrySection(
                                                onNavigateToNavigationGestures =
                                                        onNavigateToNavigationGestures
                                        )

                                        PrivacySecurityEntrySection(
                                                onNavigateToPrivacySecurity =
                                                        onNavigateToPrivacySecurity
                                        )

                                        DataRecoveryEntrySection(
                                                onNavigateToDataRecovery = onNavigateToDataRecovery
                                        )

                                        Column(modifier = Modifier.bringIntoViewRequester(languageRequester)) {
                                                LanguageSection(
                                                        appLanguage = uiState.appLanguage,
                                                        onLanguageSelected = { viewModel.setAppLanguage(it) }
                                                )
                                        }

                                        Column(modifier = Modifier.bringIntoViewRequester(reviewRequester)) {
                                                SettingsSectionHeader(
                                                        icon = Icons.Rounded.Info,
                                                        title = stringResource(R.string.settings_section_review_insights)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))

                                                ReviewAndInsightsSection(
                                                        entryReviewEnabled = uiState.entryReviewEnabled,
                                                        onEntryReviewEnabledChange = {
                                                                viewModel.setEntryReviewEnabled(it)
                                                        },
                                                        keywordHighlightingEnabled = uiState.keywordHighlightingEnabled,
                                                        onKeywordHighlightingEnabledChange = {
                                                                viewModel.setKeywordHighlightingEnabled(it)
                                                        },
                                                        fuzzyThreshold = uiState.fuzzyThreshold,
                                                        onFuzzyThresholdChange = { viewModel.setKeywordFuzzyThreshold(it) }
                                                )
                                        }

                                        Column(modifier = Modifier.bringIntoViewRequester(integrationsRequester)) {
                                                TaskerIntegrationSection(
                                                        onNavigateToTaskerIntegration = onNavigateToTaskerIntegration,
                                                        onNavigateToShortcodes = onNavigateToShortcodes
                                                )
                                        }
                                        Column(modifier = Modifier.bringIntoViewRequester(helpRequester)) {
                                                AboutSection(
                                                        onNavigateToHelp = onNavigateToHelp,
                                                        onNavigateToAppLog = onNavigateToAppLog
                                                 )
                                        }

                                        Spacer(modifier = Modifier.height(32.dp))
                                }
                        }
                }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSearchCard(
        query: String,
        onQueryChange: (String) -> Unit,
        history: List<LocalizedSettingsSearchEntry>,
        filteredTargets: List<LocalizedSettingsSearchEntry>,
        showSuggestions: Boolean,
        onHistoryTap: (LocalizedSettingsSearchEntry) -> Unit,
        onTargetTap: (LocalizedSettingsSearchEntry) -> Unit,
        onClearQuery: () -> Unit,
        onDismissSuggestions: () -> Unit
) {
        val showHistory = query.isBlank() && history.isNotEmpty() && showSuggestions
        val visibleTargets = filteredTargets.take(8)
        val showResults = visibleTargets.isNotEmpty() && showSuggestions

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                        leadingIcon = {
                                Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        },
                        trailingIcon = {
                                if (query.isNotBlank()) {
                                        IconButton(onClick = onClearQuery) {
                                                Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = stringResource(R.string.settings_search_clear),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                        }
                                }
                        },
                        placeholder = {
                                Text(
                                        text = stringResource(R.string.settings_search_placeholder),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        },
                        colors =
                                TextFieldDefaults.colors(
                                        focusedContainerColor =
                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                        unfocusedContainerColor =
                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                        disabledContainerColor =
                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        cursorColor = MaterialTheme.colorScheme.primary,
                                        focusedLeadingIconColor =
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                        unfocusedLeadingIconColor =
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                        focusedTrailingIconColor =
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                        unfocusedTrailingIconColor =
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                )
                )

                if (showHistory) {
                        ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                        ) {
                                Column(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                        Text(
                                                text = stringResource(R.string.settings_search_recent),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                        FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                history.forEach { item ->
                                                        Box(
                                                                modifier =
                                                                        Modifier.border(
                                                                                BorderStroke(
                                                                                        1.dp,
                                                                                        MaterialTheme.colorScheme.outlineVariant
                                                                                ),
                                                                                CircleShape
                                                                        )
                                                                                .clickable { onHistoryTap(item) }
                                                                                .padding(
                                                                                        horizontal = 12.dp,
                                                                                        vertical = 8.dp
                                                                                )
                                                        ) {
                                                                Text(
                                                                        text = item.title,
                                                                        style = MaterialTheme.typography.labelLarge,
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                }

                if (showResults) {
                        ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                        ) {
                                Column {
                                        visibleTargets.forEachIndexed { index, target ->
                                                DropdownMenuItem(
                                                        text = {
                                                                Column {
                                                                        Text(target.title)
                                                                        Text(
                                                                                target.section,
                                                                                style = MaterialTheme.typography.labelSmall,
                                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                }
                                                        },
                                                        onClick = { onTargetTap(target) }
                                                )
                                                if (index != visibleTargets.lastIndex) {
                                                        HorizontalDivider(
                                                                color = MaterialTheme.colorScheme.surfaceVariant
                                                        )
                                                }
                                        }
                                }
                        }
                }

                if (!showHistory && !showResults && query.isNotBlank() && showSuggestions) {
                        ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Text(
                                                text = stringResource(R.string.settings_search_no_results),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(onClick = onDismissSuggestions) {
                                                Text(stringResource(R.string.settings_search_close))
                                        }
                                }
                        }
                }
        }
}
