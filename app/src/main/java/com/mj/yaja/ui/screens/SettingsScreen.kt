package com.mj.yaja.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.CalendarMonth
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
        val navigationRequester = remember { BringIntoViewRequester() }
        val reviewRequester = remember { BringIntoViewRequester() }
        val securityRequester = remember { BringIntoViewRequester() }
        val integrationsRequester = remember { BringIntoViewRequester() }
        val helpRequester = remember { BringIntoViewRequester() }
        val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
        val entryReviewEnabled by viewModel.entryReviewEnabled.collectAsStateWithLifecycle()
        val keywordHighlightingEnabled by viewModel.keywordHighlightingEnabled.collectAsStateWithLifecycle()
        val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
        val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
        val autoLockTimeoutMinutes by viewModel.autoLockTimeoutMinutes.collectAsStateWithLifecycle()
        val hideTextModeEnabled by viewModel.hideTextModeEnabled.collectAsStateWithLifecycle()
        val showStatistics by viewModel.showStatistics.collectAsStateWithLifecycle()
        val showLookbackInNavBar by viewModel.showLookbackInNavBar.collectAsStateWithLifecycle()
        val showKeywordsInNavBar by viewModel.showKeywordsInNavBar.collectAsStateWithLifecycle()
        val showTodosInNavBar by viewModel.showTodosInNavBar.collectAsStateWithLifecycle()
        val showStatisticsInNavBar by viewModel.showStatisticsInNavBar.collectAsStateWithLifecycle()
        val navigationChromeMode by viewModel.navigationChromeMode.collectAsStateWithLifecycle()
        val showBottomPanelLabels by viewModel.showBottomPanelLabels.collectAsStateWithLifecycle()
        val adaptiveBottomNav by viewModel.adaptiveBottomNav.collectAsStateWithLifecycle()
        val calendarDensityPreference by
                viewModel.calendarDensityPreference.collectAsStateWithLifecycle()
        val swipeToNavigateDatesEnabled by viewModel.swipeToNavigateDatesEnabled.collectAsStateWithLifecycle()
        val enableDragAndDrop by viewModel.enableDragAndDrop.collectAsStateWithLifecycle()
        val entryDeleteSelectionEnabled by viewModel.entryDeleteSelectionEnabled.collectAsStateWithLifecycle()
        val fuzzyThreshold by viewModel.fuzzyThreshold.collectAsStateWithLifecycle()
        val allowTaskerAccess by viewModel.allowTaskerAccess.collectAsStateWithLifecycle()
        val allowTaskerEvents by viewModel.allowTaskerEvents.collectAsStateWithLifecycle()
        val includeEntryTextInTaskerEvents by
                viewModel.includeEntryTextInTaskerEvents.collectAsStateWithLifecycle()

        var settingsSearchQuery by rememberSaveable { mutableStateOf("") }
        var settingsSearchHistory by rememberSaveable { mutableStateOf(listOf<String>()) }
        var showSettingsSuggestions by remember { mutableStateOf(false) }

        val settingsSearchTargets =
                remember {
                        listOf(
                                SettingsSearchTarget("Theme", "Appearance", listOf("light", "dark", "amoled", "system"), onSelect = onNavigateToAppearance),
                                SettingsSearchTarget("Colors", "Appearance", listOf("material you", "custom", "palette"), onSelect = onNavigateToAppearance),
                                SettingsSearchTarget("Personal Themes", "Appearance", listOf("personal", "theme slots", "generated accents"), onSelect = onNavigateToAppearance),
                                SettingsSearchTarget("Font", "Appearance", listOf("sans", "serif", "mono"), onSelect = onNavigateToAppearance),
                                SettingsSearchTarget("Font Size", "Appearance", listOf("text size", "scale"), onSelect = onNavigateToAppearance),
                                SettingsSearchTarget("Language", "Language", listOf("español", "português", "français", "translate", "locale"), languageRequester),
                                SettingsSearchTarget("Show Timestamps", "Journal Experience", listOf("time", "timeline"), onSelect = onNavigateToJournalExperience),
                                SettingsSearchTarget("Allow Future Entries", "Journal Experience", listOf("future dates"), onSelect = onNavigateToJournalExperience),
                                SettingsSearchTarget("Carry Forward Todos", "Journal Experience", listOf("unchecked tasks", "yesterday todo"), onSelect = onNavigateToJournalExperience),
                                SettingsSearchTarget("Show Day Header Counts", "Journal Experience", listOf("header stats", "counts"), onSelect = onNavigateToJournalExperience),
                                SettingsSearchTarget("Render Checkboxes as Text", "Journal Experience", listOf("todo checkbox", "text checkbox"), onSelect = onNavigateToJournalExperience),
                                SettingsSearchTarget("Truncate Long Entries", "Journal Experience", listOf("preview", "character limit"), onSelect = onNavigateToJournalExperience),
                                SettingsSearchTarget("First Day of Week", "Journal Experience", listOf("calendar", "sunday", "monday"), onSelect = onNavigateToJournalExperience),
                                SettingsSearchTarget("Date Order", "Journal Experience", listOf("dd/mm", "mm/dd"), onSelect = onNavigateToJournalExperience),
                                SettingsSearchTarget("Entry Style", "Journal Experience", listOf("cards", "flat"), onSelect = onNavigateToJournalExperience),
                                SettingsSearchTarget("Animations", "Journal Experience", listOf("motion", "reduced"), onSelect = onNavigateToJournalExperience),
                                SettingsSearchTarget("Date Keywords", "Journal Experience", listOf("keywords", "today", "tomorrow"), onSelect = onNavigateToJournalExperience),
                                SettingsSearchTarget("Navigation Mode", "Navigation & Gestures", listOf("floating", "panel", "bottom panel"), navigationRequester),
                                SettingsSearchTarget("Lookback", "Navigation & Gestures", listOf("nav bar"), navigationRequester),
                                SettingsSearchTarget("People & Places", "Navigation & Gestures", listOf("keywords", "nav bar"), navigationRequester),
                                SettingsSearchTarget("Todos", "Navigation & Gestures", listOf("nav bar"), navigationRequester),
                                SettingsSearchTarget("Statistics", "Navigation & Gestures", listOf("nav bar"), navigationRequester),
                                SettingsSearchTarget("Swipe to Navigate Dates", "Navigation & Gestures", listOf("gestures", "swipe"), navigationRequester),
                                SettingsSearchTarget("Enable Drag-to-Reorder", "Navigation & Gestures", listOf("drag", "reorder"), navigationRequester),
                                SettingsSearchTarget("Entry Delete", "Navigation & Gestures", listOf("selection", "delete"), navigationRequester),
                                SettingsSearchTarget("Post-write Review", "Review & Insights", listOf("save sheet", "review"), reviewRequester),
                                SettingsSearchTarget("People & Places Highlighting", "Review & Insights", listOf("highlighting", "people places"), reviewRequester),
                                SettingsSearchTarget("Match Sensitivity", "Review & Insights", listOf("keyword matching", "fuzzy"), reviewRequester),
                                SettingsSearchTarget("Privacy & Security", "Privacy & Security", listOf("pin", "biometric", "lock"), securityRequester),
                                SettingsSearchTarget("PIN", "Privacy & Security", listOf("password", "lock"), securityRequester),
                                SettingsSearchTarget("Biometric", "Privacy & Security", listOf("fingerprint", "face unlock"), securityRequester),
                                SettingsSearchTarget("Auto Lock", "Privacy & Security", listOf("timeout"), securityRequester),
                                SettingsSearchTarget("Hide Text Mode", "Privacy & Security", listOf("privacy", "panic blur", "hide text"), securityRequester),
                                SettingsSearchTarget("Privacy Dashboard", "Privacy & Security", listOf("transparency", "data dashboard", "widgets", "tasker"), securityRequester, onSelect = onNavigateToPrivacyDashboard),
                                SettingsSearchTarget("Data & Recovery", "Data & Recovery", listOf("backup", "restore", "storage"), onSelect = onNavigateToDataRecovery),
                                SettingsSearchTarget("Storage Location", "Data & Recovery", listOf("folder", "storage"), onSelect = onNavigateToDataRecovery),
                                SettingsSearchTarget("Backup", "Data & Recovery", listOf("backup now", "backup reminder"), onSelect = onNavigateToDataRecovery),
                                SettingsSearchTarget("Restore Backup", "Data & Recovery", listOf("restore zip"), onSelect = onNavigateToDataRecovery),
                                SettingsSearchTarget("Import", "Data & Recovery", listOf("day one", "journalistic"), onSelect = onNavigateToDataRecovery),
                                SettingsSearchTarget("Rebuild Cache", "Data & Recovery", listOf("refresh cache", "rebuild"), onSelect = onNavigateToDataRecovery),
                                SettingsSearchTarget("Version History", "Data & Recovery", listOf("snapshots", "history"), onSelect = onNavigateToDataRecovery),
                                SettingsSearchTarget("Tasker", "Advanced Integrations", listOf("tasker integration", "automation"), integrationsRequester),
                                SettingsSearchTarget("Shortcodes", "Advanced Integrations", listOf("snippets", "text expansion"), integrationsRequester),
                                SettingsSearchTarget("Help & About", "Help & About", listOf("help", "about", "faq"), helpRequester),
                                SettingsSearchTarget("App Log", "Help & About", listOf("logs", "crash"), helpRequester)
                        )
                }
        val filteredSettingsTargets =
                remember(settingsSearchQuery, settingsSearchTargets) {
                        val query = settingsSearchQuery.trim().lowercase()
                        if (query.isBlank()) {
                                emptyList()
                        } else {
                                settingsSearchTargets.filter { target ->
                                        target.title.lowercase().contains(query) ||
                                                target.section.lowercase().contains(query) ||
                                                target.keywords.any { it.contains(query) }
                                }
                        }
                }

        fun jumpToSettingsTarget(target: SettingsSearchTarget) {
                settingsSearchQuery = target.title
                settingsSearchHistory =
                        listOf(target.title) + settingsSearchHistory.filterNot { it == target.title }.take(4)
                showSettingsSuggestions = false
                target.onSelect?.invoke()
                target.requester?.let { requester -> scope.launch { requester.bringIntoView() } }
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
                                                history = settingsSearchHistory,
                                                filteredTargets = filteredSettingsTargets,
                                                showSuggestions = showSettingsSuggestions,
                                                onHistoryTap = { title ->
                                                        val target =
                                                                settingsSearchTargets.firstOrNull {
                                                                        it.title == title
                                                                }
                                                        if (target != null) {
                                                                jumpToSettingsTarget(target)
                                                        } else {
                                                                settingsSearchQuery = title
                                                                showSettingsSuggestions = false
                                                        }
                                                },
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

                                        ProgressiveSettingsCard(
                                                isPinEnabled = isPinEnabled,
                                                adaptiveBottomNav = adaptiveBottomNav,
                                                calendarDensityPreference = calendarDensityPreference.name,
                                                onOpenPrivacy = onNavigateToPrivacyDashboard,
                                                onOpenNavigation = {
                                                        scope.launch {
                                                                navigationRequester.bringIntoView()
                                                        }
                                                },
                                                onOpenCalendarSettings = onNavigateToJournalExperience
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))

                                        AppearanceEntrySection(onNavigateToAppearance = onNavigateToAppearance)

                                        Column(modifier = Modifier.bringIntoViewRequester(languageRequester)) {
                                                LanguageSection(
                                                        appLanguage = appLanguage,
                                                        onLanguageSelected = { viewModel.setAppLanguage(it) }
                                                )
                                        }

                                        // ── Preferences Section ──
                                        JournalExperienceEntrySection(
                                                onNavigateToJournalExperience =
                                                        onNavigateToJournalExperience
                                        )

                                        Column(modifier = Modifier.bringIntoViewRequester(navigationRequester)) {
                                                SettingsSectionHeader(
                                                        icon = Icons.Rounded.Swipe,
                                                        title = stringResource(R.string.settings_section_navigation_gestures)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))

                                                NavigationSection(
                                                                navigationChromeMode = navigationChromeMode,
                                                                onNavigationChromeModeChange = {
                                                                        viewModel.setNavigationChromeMode(it)
                                                                },
                                                                showBottomPanelLabels = showBottomPanelLabels,
                                                                onShowBottomPanelLabelsChange = {
                                                                        viewModel.setShowBottomPanelLabels(it)
                                                                },
                                                                adaptiveBottomNav = adaptiveBottomNav,
                                                                onAdaptiveBottomNavChange = {
                                                                        viewModel.setAdaptiveBottomNav(it)
                                                                },
                                                                showLookbackInNavBar = showLookbackInNavBar,
                                                                onShowLookbackChange = { viewModel.setShowLookbackInNavBar(it) },
                                                                showKeywordsInNavBar = showKeywordsInNavBar,
                                                                onShowKeywordsChange = { viewModel.setShowKeywordsInNavBar(it) },
                                                                showTodosInNavBar = showTodosInNavBar,
                                                                onShowTodosChange = { viewModel.setShowTodosInNavBar(it) },
                                                                showStatistics = showStatistics,
                                                                showStatisticsInNavBar = showStatisticsInNavBar,
                                                                onShowStatisticsInNavBarChange = {
                                                                        viewModel.setShowStatisticsInNavBar(it)
                                                                }
                                                        )
                                                        Spacer(modifier = Modifier.height(32.dp))

                                                // ── Gestures Section ──
                                                GesturesSection(
                                                        entryDeleteSelectionEnabled = entryDeleteSelectionEnabled,
                                                        onEntryDeleteSelectionEnabledChange = {
                                                                viewModel.setEntryDeleteSelectionEnabled(it)
                                                        },
                                                        swipeToNavigateDatesEnabled = swipeToNavigateDatesEnabled,
                                                        onSwipeToNavigateDatesEnabledChange = {
                                                                viewModel.setSwipeToNavigateDatesEnabled(it)
                                                        },
                                                        enableDragAndDrop = enableDragAndDrop,
                                                        onEnableDragAndDropChange = { viewModel.setEnableDragAndDrop(it) }
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(32.dp))

                                        Column(modifier = Modifier.bringIntoViewRequester(reviewRequester)) {
                                                SettingsSectionHeader(
                                                        icon = Icons.Rounded.Info,
                                                        title = stringResource(R.string.settings_section_review_insights)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))

                                                ReviewAndInsightsSection(
                                                        entryReviewEnabled = entryReviewEnabled,
                                                        onEntryReviewEnabledChange = {
                                                                viewModel.setEntryReviewEnabled(it)
                                                        },
                                                        keywordHighlightingEnabled = keywordHighlightingEnabled,
                                                        onKeywordHighlightingEnabledChange = {
                                                                viewModel.setKeywordHighlightingEnabled(it)
                                                        },
                                                        fuzzyThreshold = fuzzyThreshold,
                                                        onFuzzyThresholdChange = { viewModel.setKeywordFuzzyThreshold(it) }
                                                )
                                        }

                                        Column(modifier = Modifier.bringIntoViewRequester(securityRequester)) {
                                                SecuritySection(
                                                        isPinEnabled = isPinEnabled,
                                                        onEnablePin = onNavigateToPinSetup,
                                                        onDisablePin = onNavigateToPinDisable,
                                                        onChangePin = onNavigateToPinSetup,
                                                        isBiometricEnabled = isBiometricEnabled,
                                                        onEnableBiometric = { viewModel.enableBiometric() },
                                                        onDisableBiometric = { viewModel.disableBiometric() },
                                                        autoLockTimeoutMinutes = autoLockTimeoutMinutes,
                                                        onAutoLockTimeoutChange = { viewModel.setAutoLockTimeout(it) },
                                                        hideTextModeEnabled = hideTextModeEnabled,
                                                        onHideTextModeEnabledChange = {
                                                                viewModel.setHideTextModeEnabled(it)
                                                        },
                                                        onNavigateToPrivacyDashboard = onNavigateToPrivacyDashboard
                                                )
                                        }

                                        DataRecoveryEntrySection(
                                                onNavigateToDataRecovery = onNavigateToDataRecovery
                                        )

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

@OptIn(ExperimentalFoundationApi::class)
private data class SettingsSearchTarget(
        val title: String,
        val section: String,
        val keywords: List<String>,
        val requester: BringIntoViewRequester? = null,
        val onSelect: (() -> Unit)? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressiveSettingsCard(
        isPinEnabled: Boolean,
        adaptiveBottomNav: Boolean,
        calendarDensityPreference: String,
        onOpenPrivacy: () -> Unit,
        onOpenNavigation: () -> Unit,
        onOpenCalendarSettings: () -> Unit
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
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Text(
                                text = stringResource(R.string.settings_progressive_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                text = stringResource(R.string.settings_progressive_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                TextButton(onClick = onOpenPrivacy) {
                                        Icon(
                                                imageVector = Icons.Rounded.Lock,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                                if (isPinEnabled) {
                                                        stringResource(R.string.settings_progressive_privacy_review)
                                                } else {
                                                        stringResource(R.string.settings_progressive_privacy_pin)
                                                }
                                        )
                                }
                                TextButton(onClick = onOpenNavigation) {
                                        Icon(
                                                imageVector = Icons.Rounded.Swipe,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                                if (adaptiveBottomNav) {
                                                        stringResource(R.string.settings_progressive_nav_active)
                                                } else {
                                                        stringResource(R.string.settings_progressive_nav_try)
                                                }
                                        )
                                }
                                TextButton(onClick = onOpenCalendarSettings) {
                                        Icon(
                                                imageVector = Icons.Rounded.CalendarMonth,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                                stringResource(
                                                        R.string.settings_progressive_calendar_density,
                                                        calendarDensityPreference.lowercase().replaceFirstChar {
                                                                if (it.isLowerCase()) it.titlecase() else it.toString()
                                                        }
                                                )
                                        )
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
        history: List<String>,
        filteredTargets: List<SettingsSearchTarget>,
        showSuggestions: Boolean,
        onHistoryTap: (String) -> Unit,
        onTargetTap: (SettingsSearchTarget) -> Unit,
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
                                                                        text = item,
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
