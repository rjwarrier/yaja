package com.mj.yaja.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
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
import com.mj.yaja.data.AppFontFamily
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.data.DateKeywordEntry
import com.mj.yaja.data.EntryStyle
import com.mj.yaja.data.FontScalePreference
import com.mj.yaja.data.ThemePreference
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
        onNavigateToHelp: () -> Unit,
        onNavigateToAppLog: () -> Unit,
        onNavigateToShortcodes: () -> Unit,
        onNavigateToJournal: () -> Unit,
        onNavigateToCalendar: () -> Unit,
        onNavigateToLookback: () -> Unit
) {
        val scope = rememberCoroutineScope()
        val appearanceRequester = remember { BringIntoViewRequester() }
        val journalRequester = remember { BringIntoViewRequester() }
        val navigationRequester = remember { BringIntoViewRequester() }
        val reviewRequester = remember { BringIntoViewRequester() }
        val securityRequester = remember { BringIntoViewRequester() }
        val dataRequester = remember { BringIntoViewRequester() }
        val integrationsRequester = remember { BringIntoViewRequester() }
        val helpRequester = remember { BringIntoViewRequester() }
        val themePreference by viewModel.themePreference.collectAsState()
        val colorSource by viewModel.colorSource.collectAsState()
        val customPalette by viewModel.customPalette.collectAsState()
        val themeColorIntensity by viewModel.themeColorIntensity.collectAsState()
        val backgroundTintLevel by viewModel.backgroundTintLevel.collectAsState()
        val personalThemeSlots by viewModel.personalThemeSlots.collectAsState()
        val activePersonalThemeSlotId by viewModel.activePersonalThemeSlotId.collectAsState()
        val fontScalePreference by viewModel.fontScalePreference.collectAsState()
        val animationPreference by viewModel.animationPreference.collectAsState()
        val isPreviewLimitEnabled by viewModel.isPreviewLimitEnabled.collectAsState()
        val previewLimitLength by viewModel.previewLimitLength.collectAsState()
        val storageUriString by viewModel.storageUri.collectAsState()
        val showTimestamps by viewModel.showTimestamps.collectAsState()
        val allowFutureEntries by viewModel.allowFutureEntries.collectAsState()
        val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsState()
        val backupReminderDays by viewModel.backupReminderDays.collectAsState()
        val firstDayOfWeek by viewModel.firstDayOfWeek.collectAsState()
        val dateOrderPreference by viewModel.dateOrderPreference.collectAsState()
        val customDateKeywords by viewModel.customDateKeywords.collectAsState()
        val entryReviewEnabled by viewModel.entryReviewEnabled.collectAsState()
        val keywordHighlightingEnabled by viewModel.keywordHighlightingEnabled.collectAsState()
        val showDayHeaderStats by viewModel.showDayHeaderStats.collectAsState()
        val renderCheckboxesAsText by viewModel.renderCheckboxesAsText.collectAsState()
        val isPinEnabled by viewModel.isPinEnabled.collectAsState()
        val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
        val autoLockTimeoutMinutes by viewModel.autoLockTimeoutMinutes.collectAsState()
        val showStatistics by viewModel.showStatistics.collectAsState()
        val showLookbackInNavBar by viewModel.showLookbackInNavBar.collectAsState()
        val showKeywordsInNavBar by viewModel.showKeywordsInNavBar.collectAsState()
        val showTodosInNavBar by viewModel.showTodosInNavBar.collectAsState()
        val showStatisticsInNavBar by viewModel.showStatisticsInNavBar.collectAsState()
        val navigationChromeMode by viewModel.navigationChromeMode.collectAsState()
        val showBottomPanelLabels by viewModel.showBottomPanelLabels.collectAsState()
        val swipeToNavigateDatesEnabled by viewModel.swipeToNavigateDatesEnabled.collectAsState()
        val enableDragAndDrop by viewModel.enableDragAndDrop.collectAsState()
        val entryDeleteSelectionEnabled by viewModel.entryDeleteSelectionEnabled.collectAsState()
        val fuzzyThreshold by viewModel.fuzzyThreshold.collectAsState()
        val appFontFamily by viewModel.appFontFamily.collectAsState()
        val entryStyle by viewModel.entryStyle.collectAsState()
        val swipeToSyncEnabled by viewModel.swipeToSyncEnabled.collectAsState()
        val largeJournalSafeMode by viewModel.largeJournalSafeMode.collectAsState()
        val versionHistoryEnabled by viewModel.versionHistoryEnabled.collectAsState()
        val importState by viewModel.importState.collectAsState()
        val restoreSummary by viewModel.restoreSummary.collectAsState()
        val context = LocalContext.current
        val formattedBackupDate =
                remember(lastBackupTimestamp) {
                        if (lastBackupTimestamp == 0L) {
                                "Never"
                        } else {
                                val instant = java.time.Instant.ofEpochMilli(lastBackupTimestamp)
                                val dateTime =
                                        java.time.LocalDateTime.ofInstant(
                                                instant,
                                                java.time.ZoneId.systemDefault()
                                        )
                                val formatter =
                                        java.time.format.DateTimeFormatter.ofPattern(
                                                "dd-MMM-yy HH:mm 'hrs'"
                                        )
                                dateTime.format(formatter)
                        }
                }

        var pendingUriString by remember { mutableStateOf<String?>(null) }
        var showDialog by remember { mutableStateOf(false) }
        var settingsSearchQuery by rememberSaveable { mutableStateOf("") }
        var settingsSearchHistory by rememberSaveable { mutableStateOf(listOf<String>()) }
        var showSettingsSuggestions by remember { mutableStateOf(false) }

        val settingsSearchTargets =
                remember {
                        listOf(
                                SettingsSearchTarget("Theme", "Appearance", listOf("light", "dark", "amoled", "system"), appearanceRequester),
                                SettingsSearchTarget("Colors", "Appearance", listOf("material you", "custom", "palette"), appearanceRequester),
                                SettingsSearchTarget("Personal Themes", "Appearance", listOf("personal", "theme slots", "generated accents"), appearanceRequester),
                                SettingsSearchTarget("Font", "Appearance", listOf("sans", "serif", "mono"), appearanceRequester),
                                SettingsSearchTarget("Font Size", "Appearance", listOf("text size", "scale"), appearanceRequester),
                                SettingsSearchTarget("Show Timestamps", "Journal Experience", listOf("time", "timeline"), journalRequester),
                                SettingsSearchTarget("Allow Future Entries", "Journal Experience", listOf("future dates"), journalRequester),
                                SettingsSearchTarget("Show Day Header Counts", "Journal Experience", listOf("header stats", "counts"), journalRequester),
                                SettingsSearchTarget("Render Checkboxes as Text", "Journal Experience", listOf("todo checkbox", "text checkbox"), journalRequester),
                                SettingsSearchTarget("Truncate Long Entries", "Journal Experience", listOf("preview", "character limit"), journalRequester),
                                SettingsSearchTarget("First Day of Week", "Journal Experience", listOf("calendar", "sunday", "monday"), journalRequester),
                                SettingsSearchTarget("Date Order", "Journal Experience", listOf("dd/mm", "mm/dd"), journalRequester),
                                SettingsSearchTarget("Entry Style", "Journal Experience", listOf("cards", "flat"), journalRequester),
                                SettingsSearchTarget("Animations", "Journal Experience", listOf("motion", "reduced"), journalRequester),
                                SettingsSearchTarget("Date Keywords", "Journal Experience", listOf("keywords", "today", "tomorrow"), journalRequester),
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
                                SettingsSearchTarget("Data & Recovery", "Data & Recovery", listOf("backup", "restore", "storage"), dataRequester),
                                SettingsSearchTarget("Storage Location", "Data & Recovery", listOf("folder", "storage"), dataRequester),
                                SettingsSearchTarget("Backup", "Data & Recovery", listOf("backup now", "backup reminder"), dataRequester),
                                SettingsSearchTarget("Restore Backup", "Data & Recovery", listOf("restore zip"), dataRequester),
                                SettingsSearchTarget("Import", "Data & Recovery", listOf("day one", "journalistic"), dataRequester),
                                SettingsSearchTarget("Rebuild Cache", "Data & Recovery", listOf("refresh cache", "rebuild"), dataRequester),
                                SettingsSearchTarget("Version History", "Data & Recovery", listOf("snapshots", "history"), dataRequester),
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
                scope.launch { target.requester.bringIntoView() }
        }

        val confirmLocationChange = { uriString: String? ->
                pendingUriString = uriString
                showDialog = true
        }

        val launcher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri
                        ->
                        if (uri != null) {
                                val contentResolver = context.contentResolver
                                val takeFlags: Int =
                                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                android.content.Intent
                                                        .FLAG_GRANT_WRITE_URI_PERMISSION
                                contentResolver.takePersistableUriPermission(uri, takeFlags)
                                val newUriString = uri.toString()
                                if (newUriString != storageUriString) {
                                        confirmLocationChange(newUriString)
                                }
                        }
                }

        val dayOneLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
        ) { uri -> if (uri != null) viewModel.importDayOneFile(uri, context) }
        val journalisticLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
        ) { uri -> if (uri != null) viewModel.importJournalisticFile(uri, context) }
        val backupRestoreLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
        ) { uri -> if (uri != null) viewModel.restoreBackupZip(uri, context) }

        if (showDialog) {
                val destName =
                        if (pendingUriString == null) "App Internal Storage"
                        else
                                pendingUriString?.toUri()?.path?.substringAfterLast(":")
                                        ?: "the new folder"
                val currentName =
                        if (storageUriString == null) "App Internal Storage"
                        else
                                storageUriString?.toUri()?.path?.substringAfterLast(":")
                                        ?: "the current folder"

                AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Change Storage Location") },
                        text = {
                                Text(
                                        "All existing entries will be moved from $currentName to $destName. Do you want to continue?"
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                viewModel.setStorageUri(pendingUriString)
                                                showDialog = false
                                        }
                                ) { Text("Yes") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showDialog = false }) { Text("No") }
                        }
                )
        }

        restoreSummary?.let { summary ->
                AlertDialog(
                        onDismissRequest = { viewModel.dismissRestoreSummary() },
                        title = { Text("Restore Summary") },
                        text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                                "Journal",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                "Added to new days: ${summary.newDays}\nMerged into existing days: ${summary.mergedDays}\nDuplicate journal entries skipped: ${summary.skippedJournalEntries}",
                                                style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                                "Shortcodes",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                "Added: ${summary.shortcodesAdded}\nSkipped duplicates: ${summary.shortcodesSkipped}",
                                                style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                                "Date Keywords",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                "Added: ${summary.dateKeywordsAdded}\nSkipped duplicates: ${summary.dateKeywordsSkipped}",
                                                style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                                "People & Places",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                "Added: ${summary.peoplePlacesAdded}\nSkipped duplicates: ${summary.peoplePlacesSkipped}",
                                                style = MaterialTheme.typography.bodySmall
                                        )
                                }
                        },
                        confirmButton = {
                                TextButton(onClick = { viewModel.dismissRestoreSummary() }) {
                                        Text("Done")
                                }
                        }
                )
        }

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                "Settings",
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

                                        Column(modifier = Modifier.bringIntoViewRequester(appearanceRequester)) {
                                                AppearanceSection(
                                                        themePreference = themePreference,
                                                        onThemeSelected = { viewModel.setThemePreference(it) },
                                                        colorSource = colorSource,
                                                        onColorSourceSelected = { viewModel.setColorSource(it) },
                                                        customPalette = customPalette,
                                                        onCustomPaletteSelected = { viewModel.setCustomPalette(it) },
                                                        themeColorIntensity = themeColorIntensity,
                                                        onThemeColorIntensitySelected = {
                                                                viewModel.setThemeColorIntensity(it)
                                                        },
                                                        backgroundTintLevel = backgroundTintLevel,
                                                        onBackgroundTintLevelSelected = {
                                                                viewModel.setBackgroundTintLevel(it)
                                                        },
                                                        personalThemeSlots = personalThemeSlots,
                                                        activePersonalThemeSlotId = activePersonalThemeSlotId,
                                                        onActivePersonalThemeSlotSelected = {
                                                                viewModel.setActivePersonalThemeSlotId(it)
                                                        },
                                                        onRenamePersonalThemeSlot = { slotId, name ->
                                                                viewModel.renamePersonalThemeSlot(slotId, name)
                                                        },
                                                        onPersonalThemeHueChange = { slotId, hue ->
                                                                viewModel.setPersonalThemeHue(slotId, hue)
                                                        },
                                                        onPersonalThemeSaturationChange = { slotId, saturation ->
                                                                viewModel.setPersonalThemeSaturation(slotId, saturation)
                                                        },
                                                        onPersonalThemeBrightnessChange = { slotId, brightness ->
                                                                viewModel.setPersonalThemeBrightness(slotId, brightness)
                                                        },
                                                        onPersonalThemeAccentStyleSelected = { slotId, style ->
                                                                viewModel.setPersonalThemeAccentStyle(slotId, style)
                                                        },
                                                        appFontFamily = appFontFamily,
                                                        onFontFamilySelected = { viewModel.setAppFontFamily(it) },
                                                        fontScalePreference = fontScalePreference,
                                                        onFontScaleSelected = { viewModel.setFontScalePreference(it) }
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(32.dp))

                                        // ── Preferences Section ──
                                        Column(modifier = Modifier.bringIntoViewRequester(journalRequester)) {
                                                SettingsSectionHeader(
                                                        icon = Icons.Rounded.Settings,
                                                        title = "Journal Experience"
                                                )

                                                Spacer(modifier = Modifier.height(12.dp))

                                                JournalExperienceSection(
                                                        renderCheckboxesAsText = renderCheckboxesAsText,
                                                        onRenderCheckboxesAsTextChange = {
                                                                viewModel.setRenderCheckboxesAsText(it)
                                                        },
                                                        showDayHeaderStats = showDayHeaderStats,
                                                        onShowDayHeaderStatsChange = {
                                                                viewModel.setShowDayHeaderStats(it)
                                                        },
                                                        entryStyle = entryStyle,
                                                        onEntryStyleSelected = {
                                                                viewModel.setEntryStyle(it)
                                                        },
                                                        dateOrderPreference = dateOrderPreference,
                                                        onDateOrderChange = { viewModel.setDateOrderPreference(it) },
                                                        animationPreference = animationPreference,
                                                        onAnimationPreferenceChange = {
                                                                viewModel.setAnimationPreference(it)
                                                        },
                                                        customDateKeywords = customDateKeywords,
                                                        onSetCustomDateKeywords = { viewModel.setCustomDateKeywords(it) },
                                                        fuzzyThreshold = fuzzyThreshold,
                                                        onFuzzyThresholdChange = { viewModel.setKeywordFuzzyThreshold(it) },
                                                        showTimestamps = showTimestamps,
                                                        onShowTimestampsChange = { viewModel.setShowTimestamps(it) },
                                                        allowFutureEntries = allowFutureEntries,
                                                        onAllowFutureEntriesChange = { viewModel.setAllowFutureEntries(it) },
                                                        isPreviewLimitEnabled = isPreviewLimitEnabled,
                                                        onIsPreviewLimitEnabledChange = { viewModel.setPreviewLimitEnabled(it) },
                                                        previewLimitLength = previewLimitLength,
                                                        onPreviewLimitLengthChange = { viewModel.setPreviewLimitLength(it) },
                                                        firstDayOfWeek = firstDayOfWeek,
                                                        onFirstDayOfWeekChange = { viewModel.setFirstDayOfWeek(it) }
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(32.dp))

                                        Column(modifier = Modifier.bringIntoViewRequester(navigationRequester)) {
                                                SettingsSectionHeader(
                                                        icon = Icons.Rounded.Swipe,
                                                        title = "Navigation & Gestures"
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
                                                        title = "Review & Insights"
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
                                                        onAutoLockTimeoutChange = { viewModel.setAutoLockTimeout(it) }
                                                )
                                        }

                                        Column(modifier = Modifier.bringIntoViewRequester(dataRequester)) {
                                                DataAndStorageSection(
                                                        storageLocationText = if (storageUriString == null) {
                                                                "App Internal Storage (Default)"
                                                        } else {
                                                                "Custom Folder:\n" + (storageUriString?.toUri()?.path?.substringAfterLast(":") ?: "")
                                                        },
                                                        hasCustomStorage = storageUriString != null,
                                                        onResetStorage = { confirmLocationChange(null) },
                                                        onChooseFolder = { launcher.launch(null) },
                                                        formattedBackupDate = formattedBackupDate,
                                                        backupReminderDays = backupReminderDays,
                                                        onBackupNow = { viewModel.backupData(context) },
                                                        onBackupReminderDaysChange = { viewModel.setBackupReminderDays(it) },
                                                        onRestoreBackup = {
                                                                backupRestoreLauncher.launch(arrayOf("application/zip", "*/*"))
                                                        },
                                                        onRefreshCache = onNavigateToRebuildCache,
                                                        swipeToSyncEnabled = swipeToSyncEnabled,
                                                        onSwipeToSyncEnabledChange = { viewModel.setSwipeToSyncEnabled(it) },
                                                        largeJournalSafeMode = largeJournalSafeMode,
                                                        onLargeJournalSafeModeChange = { viewModel.setLargeJournalSafeMode(it) },
                                                        versionHistoryEnabled = versionHistoryEnabled,
                                                        onVersionHistoryEnabledChange = {
                                                                viewModel.setVersionHistoryEnabled(it)
                                                        },
                                                        onNavigateToVersionHistory = onNavigateToVersionHistory,
                                                        importState = importState,
                                                        onLaunchDayOneImport = {
                                                                dayOneLauncher.launch(arrayOf("application/zip", "application/json", "*/*"))
                                                        },
                                                        onLaunchJournalisticImport = {
                                                                journalisticLauncher.launch(arrayOf("application/json", "*/*"))
                                                        },
                                                        onCancelImport = { viewModel.cancelImport() },
                                                        onResetImportState = { viewModel.resetImportState() }
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

@OptIn(ExperimentalFoundationApi::class)
private data class SettingsSearchTarget(
        val title: String,
        val section: String,
        val keywords: List<String>,
        val requester: BringIntoViewRequester
)

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
                                                        contentDescription = "Clear search",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                        }
                                }
                        },
                        placeholder = {
                                Text(
                                        text = "Search settings",
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
                                                text = "Recent searches",
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
                                                text = "No matching settings",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(onClick = onDismissSuggestions) {
                                                Text("Close")
                                        }
                                }
                        }
                }
        }
}
