package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.data.AppFontFamily
import com.mj.yaja.data.AppLanguage
import com.mj.yaja.data.BackgroundTintLevel
import com.mj.yaja.data.CalendarDensityPreference
import com.mj.yaja.data.ColorSource
import com.mj.yaja.data.CustomPalette
import com.mj.yaja.data.DateKeywordEntry
import com.mj.yaja.data.DateOrderPreference
import com.mj.yaja.data.EntryStyle
import com.mj.yaja.data.FabPlacement
import com.mj.yaja.data.FontScalePreference
import com.mj.yaja.data.UiScalePreference
import com.mj.yaja.data.KeywordRepository
import com.mj.yaja.data.NavigationChromeMode
import com.mj.yaja.data.PersonalAccentStyle
import com.mj.yaja.data.PersonalThemeSlot
import com.mj.yaja.data.SettingsRepository
import com.mj.yaja.data.ThemeColorIntensity
import com.mj.yaja.data.ThemePreference
import java.time.DayOfWeek
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

/**
 * Owns Settings feature state composition and direct SettingsRepository actions.
 *
 * JournalViewModel intentionally keeps public property/method names as a compatibility
 * facade while settings screens migrate behind this controller one slice at a time.
 */
internal class SettingsFeatureController(
    private val settingsRepository: SettingsRepository,
    private val keywordRepository: KeywordRepository,
    importState: StateFlow<JournalViewModel.ImportState>,
    restoreSummary: StateFlow<JournalViewModel.RestoreSummary?>,
    scope: CoroutineScope
) {
    private data class NavigationVisibilitySettingsSlice(
        val showStatistics: Boolean,
        val showLookbackInNavBar: Boolean,
        val showKeywordsInNavBar: Boolean,
        val showTodosInNavBar: Boolean,
        val showStatisticsInNavBar: Boolean
    )

    private data class NavigationChromeSettingsSlice(
        val navigationChromeMode: NavigationChromeMode,
        val showBottomPanelLabels: Boolean,
        val adaptiveBottomNav: Boolean
    )

    private data class GestureSettingsSlice(
        val swipeToNavigateDatesEnabled: Boolean,
        val enableDragAndDrop: Boolean,
        val entryDeleteSelectionEnabled: Boolean
    )

    private data class JournalExperienceDisplaySettingsSlice(
        val animationPreference: AnimationPreference,
        val entryStyle: EntryStyle,
        val showTimestamps: Boolean,
        val showDayHeaderStats: Boolean,
        val renderCheckboxesAsText: Boolean
    )

    private data class JournalExperienceDateSettingsSlice(
        val allowFutureEntries: Boolean,
        val firstDayOfWeek: DayOfWeek,
        val dateOrderPreference: DateOrderPreference,
        val calendarDensityPreference: CalendarDensityPreference,
        val customDateKeywords: List<DateKeywordEntry>
    )

    private data class JournalExperiencePreviewSettingsSlice(
        val carryForwardTodosEnabled: Boolean,
        val isPreviewLimitEnabled: Boolean,
        val previewLimitLength: Int,
        val fuzzyThreshold: Float
    )

    private data class DataRecoveryStorageSettingsSlice(
        val storageUri: String?,
        val lastBackupTimestamp: Long,
        val backupReminderDays: Int,
        val showOnboardingNextLaunch: Boolean
    )

    private data class DataRecoverySafetySettingsSlice(
        val swipeToSyncEnabled: Boolean,
        val largeJournalSafeMode: Boolean,
        val versionHistoryEnabled: Boolean
    )

    private data class AppearanceThemeSettingsSlice(
        val themePreference: ThemePreference,
        val colorSource: ColorSource,
        val customPalette: CustomPalette,
        val themeColorIntensity: ThemeColorIntensity,
        val backgroundTintLevel: BackgroundTintLevel
    )

    private data class AppearancePersonalThemeSettingsSlice(
        val personalThemeSlots: List<PersonalThemeSlot>,
        val activePersonalThemeSlotId: Int
    )

    private data class AppearanceFontSettingsSlice(
        val fontScalePreference: FontScalePreference,
        val dataFontScalePreference: FontScalePreference,
        val followUiFontScale: Boolean,
        val appFontFamily: AppFontFamily,
        val monoFontWeight: Int
    )

    private data class AppearanceCustomFontSettingsSlice(
        val customFontPath: String?,
        val customFontName: String?,
        val fabPlacement: FabPlacement,
        val uiScalePreference: UiScalePreference
    )

    val themePreference = settingsRepository.themePreference
    val colorSource = settingsRepository.colorSource
    val customPalette = settingsRepository.customPalette
    val themeColorIntensity = settingsRepository.themeColorIntensity
    val backgroundTintLevel = settingsRepository.backgroundTintLevel
    val personalThemeSlots = settingsRepository.personalThemeSlots
    val activePersonalThemeSlotId = settingsRepository.activePersonalThemeSlotId
    val appFontFamily = settingsRepository.appFontFamily
    val monoFontWeight = settingsRepository.monoFontWeight
    val customFontPath = settingsRepository.customFontPath
    val customFontName = settingsRepository.customFontName
    val entryStyle = settingsRepository.entryStyle
    val storageUri = settingsRepository.storageUri
    val hasCompletedOnboarding = settingsRepository.hasCompletedOnboarding
    val shouldShowOnboarding = settingsRepository.shouldShowOnboarding
    val showOnboardingNextLaunch = settingsRepository.showOnboardingNextLaunch
    val showTimestamps = settingsRepository.showTimestamps
    val showDayHeaderStats = settingsRepository.showDayHeaderStats
    val renderCheckboxesAsText = settingsRepository.renderCheckboxesAsText
    val uiScalePreference = settingsRepository.uiScalePreference
    val fontScalePreference = settingsRepository.fontScalePreference
    val dataFontScalePreference = settingsRepository.dataFontScalePreference
    val followUiFontScale = settingsRepository.followUiFontScale
    val appLanguage = settingsRepository.appLanguage
    val animationPreference = settingsRepository.animationPreference
    val lastBackupTimestamp = settingsRepository.lastBackupTimestamp
    val backupReminderDays = settingsRepository.backupReminderDays
    val appLogRetentionDays = settingsRepository.appLogRetentionDays
    val firstDayOfWeek = settingsRepository.firstDayOfWeek
    val dateOrderPreference = settingsRepository.dateOrderPreference
    val customDateKeywords = settingsRepository.customDateKeywords
    val isPinEnabled = settingsRepository.isPinEnabled
    val isBiometricEnabled = settingsRepository.isBiometricEnabled
    val autoLockTimeoutMinutes = settingsRepository.autoLockTimeoutMinutes
    val hideTextModeEnabled = settingsRepository.hideTextModeEnabled
    val carryForwardTodosEnabled = settingsRepository.carryForwardTodosEnabled
    val allowFutureEntries = settingsRepository.allowFutureEntries
    val allowTaskerAccess = settingsRepository.allowTaskerAccess
    val allowTaskerEvents = settingsRepository.allowTaskerEvents
    val includeEntryTextInTaskerEvents = settingsRepository.includeEntryTextInTaskerEvents
    val swipeToNavigateDatesEnabled = settingsRepository.swipeToNavigateDatesEnabled
    val swipeToSyncEnabled = settingsRepository.swipeToSyncEnabled
    val largeJournalSafeMode = settingsRepository.largeJournalSafeMode
    val versionHistoryEnabled = settingsRepository.versionHistoryEnabled
    val versionHistoryMaxVersions = settingsRepository.versionHistoryMaxVersions
    val versionHistoryRetentionDays = settingsRepository.versionHistoryRetentionDays
    val lastBackgroundFullRefreshAt = settingsRepository.lastBackgroundFullRefreshAt
    val showStatistics = settingsRepository.showStatistics
    val showLookbackInNavBar = settingsRepository.showLookbackInNavBar
    val showKeywordsInNavBar = settingsRepository.showKeywordsInNavBar
    val showTodosInNavBar = settingsRepository.showTodosInNavBar
    val showCompletedTodos = settingsRepository.showCompletedTodos
    val showStatisticsInNavBar = settingsRepository.showStatisticsInNavBar
    val enableDragAndDrop = settingsRepository.enableDragAndDrop
    val entryDeleteSelectionEnabled = settingsRepository.entryDeleteSelectionEnabled
    val hasActiveWidgets = settingsRepository.hasActiveWidgets
    val showBottomBar = settingsRepository.showBottomBar
    val navigationChromeMode = settingsRepository.navigationChromeMode
    val showBottomPanelLabels = settingsRepository.showBottomPanelLabels
    val fabPlacement = settingsRepository.fabPlacement
    val calendarDensityPreference = settingsRepository.calendarDensityPreference
    val adaptiveBottomNav = settingsRepository.adaptiveBottomNav
    val customShortcodes = settingsRepository.customShortcodes
    val recentTemplateIds = settingsRepository.recentTemplateIds
    val favoriteTemplateIds = settingsRepository.favoriteTemplateIds
    val templateUsageCounts = settingsRepository.templateUsageCounts
    val templateFollowUpCounts = settingsRepository.templateFollowUpCounts
    val entryReviewEnabled = settingsRepository.entryReviewEnabled
    val keywordHighlightingEnabled = settingsRepository.keywordHighlightingEnabled
    val isPreviewLimitEnabled = settingsRepository.isPreviewLimitEnabled
    val previewLimitLength = settingsRepository.previewLimitLength
    val statisticsSectionOrder = settingsRepository.statisticsSectionOrder
    val visibleStatisticsSections = settingsRepository.visibleStatisticsSections
    val useMLKitDetection = settingsRepository.useMLKitDetection
    val keywords = keywordRepository.keywords
    val fuzzyThreshold = keywordRepository.fuzzyThreshold

    val privacySecuritySettingsUiState: StateFlow<PrivacySecuritySettingsUiState> =
        combine(
            isPinEnabled,
            isBiometricEnabled,
            autoLockTimeoutMinutes,
            hideTextModeEnabled
        ) { pinEnabled, biometricEnabled, autoLockMinutes, hideTextEnabled ->
            PrivacySecuritySettingsUiState(
                isPinEnabled = pinEnabled,
                isBiometricEnabled = biometricEnabled,
                autoLockTimeoutMinutes = autoLockMinutes,
                hideTextModeEnabled = hideTextEnabled
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PrivacySecuritySettingsUiState(
                isPinEnabled = isPinEnabled.value,
                isBiometricEnabled = isBiometricEnabled.value,
                autoLockTimeoutMinutes = autoLockTimeoutMinutes.value,
                hideTextModeEnabled = hideTextModeEnabled.value
            )
        )

    val dataRecoverySettingsUiState: StateFlow<DataRecoverySettingsUiState> =
        combine(
            combine(
                storageUri,
                lastBackupTimestamp,
                backupReminderDays,
                showOnboardingNextLaunch
            ) { uri, backupTimestamp, reminderDays, showOnboarding ->
                DataRecoveryStorageSettingsSlice(
                    storageUri = uri,
                    lastBackupTimestamp = backupTimestamp,
                    backupReminderDays = reminderDays,
                    showOnboardingNextLaunch = showOnboarding
                )
            },
            combine(
                swipeToSyncEnabled,
                largeJournalSafeMode,
                versionHistoryEnabled
            ) { swipeSync, safeMode, versionHistory ->
                DataRecoverySafetySettingsSlice(
                    swipeToSyncEnabled = swipeSync,
                    largeJournalSafeMode = safeMode,
                    versionHistoryEnabled = versionHistory
                )
            },
            combine(
                importState,
                restoreSummary
            ) { importProgress, restore ->
                importProgress to restore
            }
        ) { storage, safety, progress ->
            DataRecoverySettingsUiState(
                storageUri = storage.storageUri,
                lastBackupTimestamp = storage.lastBackupTimestamp,
                backupReminderDays = storage.backupReminderDays,
                swipeToSyncEnabled = safety.swipeToSyncEnabled,
                largeJournalSafeMode = safety.largeJournalSafeMode,
                showOnboardingNextLaunch = storage.showOnboardingNextLaunch,
                versionHistoryEnabled = safety.versionHistoryEnabled,
                importState = progress.first,
                restoreSummary = progress.second
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DataRecoverySettingsUiState(
                storageUri = storageUri.value,
                lastBackupTimestamp = lastBackupTimestamp.value,
                backupReminderDays = backupReminderDays.value,
                swipeToSyncEnabled = swipeToSyncEnabled.value,
                largeJournalSafeMode = largeJournalSafeMode.value,
                showOnboardingNextLaunch = showOnboardingNextLaunch.value,
                versionHistoryEnabled = versionHistoryEnabled.value,
                importState = importState.value,
                restoreSummary = restoreSummary.value
            )
        )

    val navigationGesturesSettingsUiState: StateFlow<NavigationGesturesSettingsUiState> =
        combine(
            combine(
                showStatistics,
                showLookbackInNavBar,
                showKeywordsInNavBar,
                showTodosInNavBar,
                showStatisticsInNavBar
            ) { statistics, lookback, keywords, todos, statisticsInNav ->
                NavigationVisibilitySettingsSlice(
                    showStatistics = statistics,
                    showLookbackInNavBar = lookback,
                    showKeywordsInNavBar = keywords,
                    showTodosInNavBar = todos,
                    showStatisticsInNavBar = statisticsInNav
                )
            },
            combine(
                navigationChromeMode,
                showBottomPanelLabels,
                adaptiveBottomNav
            ) { chromeMode, bottomPanelLabels, adaptiveNav ->
                NavigationChromeSettingsSlice(
                    navigationChromeMode = chromeMode,
                    showBottomPanelLabels = bottomPanelLabels,
                    adaptiveBottomNav = adaptiveNav
                )
            },
            combine(
                swipeToNavigateDatesEnabled,
                enableDragAndDrop,
                entryDeleteSelectionEnabled
            ) { swipeToNavigate, dragAndDrop, deleteSelection ->
                GestureSettingsSlice(
                    swipeToNavigateDatesEnabled = swipeToNavigate,
                    enableDragAndDrop = dragAndDrop,
                    entryDeleteSelectionEnabled = deleteSelection
                )
            }
        ) { visibility, chrome, gestures ->
            NavigationGesturesSettingsUiState(
                showStatistics = visibility.showStatistics,
                showLookbackInNavBar = visibility.showLookbackInNavBar,
                showKeywordsInNavBar = visibility.showKeywordsInNavBar,
                showTodosInNavBar = visibility.showTodosInNavBar,
                showStatisticsInNavBar = visibility.showStatisticsInNavBar,
                navigationChromeMode = chrome.navigationChromeMode,
                showBottomPanelLabels = chrome.showBottomPanelLabels,
                adaptiveBottomNav = chrome.adaptiveBottomNav,
                swipeToNavigateDatesEnabled = gestures.swipeToNavigateDatesEnabled,
                enableDragAndDrop = gestures.enableDragAndDrop,
                entryDeleteSelectionEnabled = gestures.entryDeleteSelectionEnabled
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NavigationGesturesSettingsUiState(
                showStatistics = showStatistics.value,
                showLookbackInNavBar = showLookbackInNavBar.value,
                showKeywordsInNavBar = showKeywordsInNavBar.value,
                showTodosInNavBar = showTodosInNavBar.value,
                showStatisticsInNavBar = showStatisticsInNavBar.value,
                navigationChromeMode = navigationChromeMode.value,
                showBottomPanelLabels = showBottomPanelLabels.value,
                adaptiveBottomNav = adaptiveBottomNav.value,
                swipeToNavigateDatesEnabled = swipeToNavigateDatesEnabled.value,
                enableDragAndDrop = enableDragAndDrop.value,
                entryDeleteSelectionEnabled = entryDeleteSelectionEnabled.value
            )
        )

    val journalExperienceSettingsUiState: StateFlow<JournalExperienceSettingsUiState> =
        combine(
            combine(
                animationPreference,
                entryStyle,
                showTimestamps,
                showDayHeaderStats,
                renderCheckboxesAsText
            ) { animation, style, timestamps, dayHeaderStats, checkboxesAsText ->
                JournalExperienceDisplaySettingsSlice(
                    animationPreference = animation,
                    entryStyle = style,
                    showTimestamps = timestamps,
                    showDayHeaderStats = dayHeaderStats,
                    renderCheckboxesAsText = checkboxesAsText
                )
            },
            combine(
                allowFutureEntries,
                firstDayOfWeek,
                dateOrderPreference,
                calendarDensityPreference,
                customDateKeywords
            ) { futureEntries, firstDay, dateOrder, calendarDensity, dateKeywords ->
                JournalExperienceDateSettingsSlice(
                    allowFutureEntries = futureEntries,
                    firstDayOfWeek = firstDay,
                    dateOrderPreference = dateOrder,
                    calendarDensityPreference = calendarDensity,
                    customDateKeywords = dateKeywords
                )
            },
            combine(
                carryForwardTodosEnabled,
                isPreviewLimitEnabled,
                previewLimitLength,
                fuzzyThreshold
            ) { carryForward, previewLimitEnabled, previewLength, threshold ->
                JournalExperiencePreviewSettingsSlice(
                    carryForwardTodosEnabled = carryForward,
                    isPreviewLimitEnabled = previewLimitEnabled,
                    previewLimitLength = previewLength,
                    fuzzyThreshold = threshold
                )
            }
        ) { display, date, preview ->
            JournalExperienceSettingsUiState(
                animationPreference = display.animationPreference,
                isPreviewLimitEnabled = preview.isPreviewLimitEnabled,
                previewLimitLength = preview.previewLimitLength,
                showTimestamps = display.showTimestamps,
                allowFutureEntries = date.allowFutureEntries,
                firstDayOfWeek = date.firstDayOfWeek,
                dateOrderPreference = date.dateOrderPreference,
                customDateKeywords = date.customDateKeywords,
                showDayHeaderStats = display.showDayHeaderStats,
                renderCheckboxesAsText = display.renderCheckboxesAsText,
                carryForwardTodosEnabled = preview.carryForwardTodosEnabled,
                calendarDensityPreference = date.calendarDensityPreference,
                fuzzyThreshold = preview.fuzzyThreshold,
                entryStyle = display.entryStyle
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = JournalExperienceSettingsUiState(
                animationPreference = animationPreference.value,
                isPreviewLimitEnabled = isPreviewLimitEnabled.value,
                previewLimitLength = previewLimitLength.value,
                showTimestamps = showTimestamps.value,
                allowFutureEntries = allowFutureEntries.value,
                firstDayOfWeek = firstDayOfWeek.value,
                dateOrderPreference = dateOrderPreference.value,
                customDateKeywords = customDateKeywords.value,
                showDayHeaderStats = showDayHeaderStats.value,
                renderCheckboxesAsText = renderCheckboxesAsText.value,
                carryForwardTodosEnabled = carryForwardTodosEnabled.value,
                calendarDensityPreference = calendarDensityPreference.value,
                fuzzyThreshold = fuzzyThreshold.value,
                entryStyle = entryStyle.value
            )
        )

    val rootSettingsUiState: StateFlow<RootSettingsUiState> =
        combine(
            appLanguage,
            entryReviewEnabled,
            keywordHighlightingEnabled,
            fuzzyThreshold
        ) { language, reviewEnabled, highlightingEnabled, threshold ->
            RootSettingsUiState(
                appLanguage = language,
                entryReviewEnabled = reviewEnabled,
                keywordHighlightingEnabled = highlightingEnabled,
                fuzzyThreshold = threshold
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RootSettingsUiState(
                appLanguage = appLanguage.value,
                entryReviewEnabled = entryReviewEnabled.value,
                keywordHighlightingEnabled = keywordHighlightingEnabled.value,
                fuzzyThreshold = fuzzyThreshold.value
            )
        )

    val appearanceSettingsUiState: StateFlow<AppearanceSettingsUiState> =
        combine(
            combine(
                themePreference,
                colorSource,
                customPalette,
                themeColorIntensity,
                backgroundTintLevel
            ) { theme, source, palette, intensity, tintLevel ->
                AppearanceThemeSettingsSlice(
                    themePreference = theme,
                    colorSource = source,
                    customPalette = palette,
                    themeColorIntensity = intensity,
                    backgroundTintLevel = tintLevel
                )
            },
            combine(
                personalThemeSlots,
                activePersonalThemeSlotId
            ) { slots, activeSlotId ->
                AppearancePersonalThemeSettingsSlice(
                    personalThemeSlots = slots,
                    activePersonalThemeSlotId = activeSlotId
                )
            },
            combine(
                fontScalePreference,
                dataFontScalePreference,
                followUiFontScale,
                appFontFamily,
                monoFontWeight
            ) { uiFontScale, dataFontScale, followUiScale, fontFamily, monoWeight ->
                AppearanceFontSettingsSlice(
                    fontScalePreference = uiFontScale,
                    dataFontScalePreference = dataFontScale,
                    followUiFontScale = followUiScale,
                    appFontFamily = fontFamily,
                    monoFontWeight = monoWeight
                )
            },
            combine(
                customFontPath,
                customFontName,
                fabPlacement,
                uiScalePreference
            ) { fontPath, fontName, placement, uiScale ->
                AppearanceCustomFontSettingsSlice(
                    customFontPath = fontPath,
                    customFontName = fontName,
                    fabPlacement = placement,
                    uiScalePreference = uiScale
                )
            }
        ) { theme, personalTheme, font, customFont ->
            AppearanceSettingsUiState(
                themePreference = theme.themePreference,
                colorSource = theme.colorSource,
                customPalette = theme.customPalette,
                themeColorIntensity = theme.themeColorIntensity,
                backgroundTintLevel = theme.backgroundTintLevel,
                personalThemeSlots = personalTheme.personalThemeSlots,
                activePersonalThemeSlotId = personalTheme.activePersonalThemeSlotId,
                fontScalePreference = font.fontScalePreference,
                dataFontScalePreference = font.dataFontScalePreference,
                followUiFontScale = font.followUiFontScale,
                appFontFamily = font.appFontFamily,
                monoFontWeight = font.monoFontWeight,
                customFontPath = customFont.customFontPath,
                customFontName = customFont.customFontName,
                fabPlacement = customFont.fabPlacement,
                uiScalePreference = customFont.uiScalePreference
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppearanceSettingsUiState(
                themePreference = themePreference.value,
                colorSource = colorSource.value,
                customPalette = customPalette.value,
                themeColorIntensity = themeColorIntensity.value,
                backgroundTintLevel = backgroundTintLevel.value,
                personalThemeSlots = personalThemeSlots.value,
                activePersonalThemeSlotId = activePersonalThemeSlotId.value,
                fontScalePreference = fontScalePreference.value,
                dataFontScalePreference = dataFontScalePreference.value,
                followUiFontScale = followUiFontScale.value,
                appFontFamily = appFontFamily.value,
                monoFontWeight = monoFontWeight.value,
                customFontPath = customFontPath.value,
                customFontName = customFontName.value,
                fabPlacement = fabPlacement.value,
                uiScalePreference = uiScalePreference.value
            )
        )

    fun setPin(plain: String) = settingsRepository.setPin(plain)
    fun clearPin() = settingsRepository.clearPin()
    fun checkPin(plain: String) = settingsRepository.checkPin(plain)
    fun enableBiometric() = settingsRepository.enableBiometric()
    fun disableBiometric() = settingsRepository.disableBiometric()
    fun setAutoLockTimeout(minutes: Int) = settingsRepository.setAutoLockTimeout(minutes)
    fun setAllowFutureEntries(allow: Boolean) = settingsRepository.setAllowFutureEntries(allow)
    fun setHideTextModeEnabled(enabled: Boolean) = settingsRepository.setHideTextModeEnabled(enabled)
    fun setCarryForwardTodosEnabled(enabled: Boolean) = settingsRepository.setCarryForwardTodosEnabled(enabled)
    fun setAllowTaskerAccess(allow: Boolean) = settingsRepository.setAllowTaskerAccess(allow)
    fun setAllowTaskerEvents(allow: Boolean) = settingsRepository.setAllowTaskerEvents(allow)
    fun setIncludeEntryTextInTaskerEvents(include: Boolean) = settingsRepository.setIncludeEntryTextInTaskerEvents(include)
    fun setSwipeToNavigateDatesEnabled(enabled: Boolean) = settingsRepository.setSwipeToNavigateDatesEnabled(enabled)
    fun setSwipeToSyncEnabled(enabled: Boolean) = settingsRepository.setSwipeToSyncEnabled(enabled)
    fun setLargeJournalSafeMode(enabled: Boolean) = settingsRepository.setLargeJournalSafeMode(enabled)
    fun setVersionHistoryEnabled(enabled: Boolean) = settingsRepository.setVersionHistoryEnabled(enabled)
    fun setVersionHistoryMaxVersions(count: Int) = settingsRepository.setVersionHistoryMaxVersions(count)
    fun setVersionHistoryRetentionDays(days: Int) = settingsRepository.setVersionHistoryRetentionDays(days)
    fun setBackupReminderDays(days: Int) = settingsRepository.setBackupReminderDays(days)
    fun setShowBottomBar(show: Boolean) = settingsRepository.setShowBottomBar(show)
    fun setNavigationChromeMode(mode: NavigationChromeMode) = settingsRepository.setNavigationChromeMode(mode)
    fun setShowBottomPanelLabels(show: Boolean) = settingsRepository.setShowBottomPanelLabels(show)
    fun setFabPlacement(placement: FabPlacement) = settingsRepository.setFabPlacement(placement)
    fun setCalendarDensityPreference(preference: CalendarDensityPreference) = settingsRepository.setCalendarDensityPreference(preference)
    fun setAdaptiveBottomNav(enabled: Boolean) = settingsRepository.setAdaptiveBottomNav(enabled)
    fun setPreviewLimitEnabled(enabled: Boolean) = settingsRepository.setPreviewLimitEnabled(enabled)
    fun setPreviewLimitLength(length: Int) = settingsRepository.setPreviewLimitLength(length)
    fun refreshWidgetStatus() = settingsRepository.refreshActiveWidgetsStatus()
    fun setThemePreference(preference: ThemePreference) = settingsRepository.setThemePreference(preference)
    fun setColorSource(source: ColorSource) = settingsRepository.setColorSource(source)
    fun setCustomPalette(palette: CustomPalette) = settingsRepository.setCustomPalette(palette)
    fun setThemeColorIntensity(intensity: ThemeColorIntensity) = settingsRepository.setThemeColorIntensity(intensity)
    fun setBackgroundTintLevel(level: BackgroundTintLevel) = settingsRepository.setBackgroundTintLevel(level)
    fun setActivePersonalThemeSlotId(slotId: Int) = settingsRepository.setActivePersonalThemeSlotId(slotId)
    fun renamePersonalThemeSlot(slotId: Int, name: String) = settingsRepository.renamePersonalThemeSlot(slotId, name)
    fun setPersonalThemeHue(slotId: Int, hue: Float) = settingsRepository.setPersonalThemeHue(slotId, hue)
    fun setPersonalThemeSaturation(slotId: Int, saturation: Float) = settingsRepository.setPersonalThemeSaturation(slotId, saturation)
    fun setPersonalThemeBrightness(slotId: Int, brightness: Float) = settingsRepository.setPersonalThemeBrightness(slotId, brightness)
    fun setPersonalThemeAccentStyle(slotId: Int, style: PersonalAccentStyle) = settingsRepository.setPersonalThemeAccentStyle(slotId, style)
    fun setAppFontFamily(fontFamily: AppFontFamily) = settingsRepository.setAppFontFamily(fontFamily)
    fun setMonoFontWeight(weight: Int) = settingsRepository.setMonoFontWeight(weight)
    fun setEntryStyle(style: EntryStyle) = settingsRepository.setEntryStyle(style)
    fun setUiScalePreference(preference: UiScalePreference) = settingsRepository.setUiScalePreference(preference)
    fun setFontScalePreference(preference: FontScalePreference) = settingsRepository.setFontScalePreference(preference)
    fun setDataFontScalePreference(preference: FontScalePreference) = settingsRepository.setDataFontScalePreference(preference)
    fun setFollowUiFontScale(follow: Boolean) = settingsRepository.setFollowUiFontScale(follow)
    fun setAppLanguage(language: AppLanguage) = settingsRepository.setAppLanguage(language)
    fun setAnimationPreference(preference: AnimationPreference) = settingsRepository.setAnimationPreference(preference)
    fun setFirstDayOfWeek(dayOfWeek: DayOfWeek) = settingsRepository.setFirstDayOfWeek(dayOfWeek)
    fun setDateOrderPreference(pref: DateOrderPreference) = settingsRepository.setDateOrderPreference(pref)
    fun setCustomDateKeywords(entries: List<DateKeywordEntry>) = settingsRepository.setCustomDateKeywords(entries)
    fun setShowTimestamps(show: Boolean) = settingsRepository.setShowTimestamps(show)
    fun setShowDayHeaderStats(show: Boolean) = settingsRepository.setShowDayHeaderStats(show)
    fun setRenderCheckboxesAsText(renderAsText: Boolean) = settingsRepository.setRenderCheckboxesAsText(renderAsText)
    fun setShowStatistics(show: Boolean) = settingsRepository.setShowStatistics(show)
    fun setShowLookbackInNavBar(show: Boolean) = settingsRepository.setShowLookbackInNavBar(show)
    fun setShowKeywordsInNavBar(show: Boolean) = settingsRepository.setShowKeywordsInNavBar(show)
    fun setShowTodosInNavBar(show: Boolean) = settingsRepository.setShowTodosInNavBar(show)
    fun setShowCompletedTodos(show: Boolean) = settingsRepository.setShowCompletedTodos(show)
    fun setShowStatisticsInNavBar(show: Boolean) = settingsRepository.setShowStatisticsInNavBar(show)
    fun setEnableDragAndDrop(enable: Boolean) = settingsRepository.setEnableDragAndDrop(enable)
    fun setEntryDeleteSelectionEnabled(enable: Boolean) = settingsRepository.setEntryDeleteSelectionEnabled(enable)
    fun setEntryReviewEnabled(enabled: Boolean) = settingsRepository.setEntryReviewEnabled(enabled)
    fun setKeywordHighlightingEnabled(enabled: Boolean) = settingsRepository.setKeywordHighlightingEnabled(enabled)
    fun setStatisticsSectionOrder(order: List<String>) = settingsRepository.setStatisticsSectionOrder(order)
    fun setVisibleStatisticsSections(sectionNames: Set<String>) = settingsRepository.setVisibleStatisticsSections(sectionNames)
    fun setUseMLKitDetection(enabled: Boolean) = settingsRepository.setUseMLKitDetection(enabled)
    fun markTemplateUsed(templateId: String) = settingsRepository.markTemplateUsed(templateId)
    fun toggleFavoriteTemplate(templateId: String) = settingsRepository.toggleFavoriteTemplate(templateId)
    fun incrementTemplateUsage(templateId: String) = settingsRepository.incrementTemplateUsage(templateId)
    fun incrementTemplateFollowUp(templateId: String) = settingsRepository.incrementTemplateFollowUp(templateId)
    fun setCustomShortcodes(shortcodes: Map<String, String>) = settingsRepository.setCustomShortcodes(shortcodes)
    fun setKeywordFuzzyThreshold(threshold: Float) = keywordRepository.setFuzzyThreshold(threshold)
}
