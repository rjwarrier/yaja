package com.mj.yaja.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.data.AppLanguage
import com.mj.yaja.data.AppFontFamily
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
import com.mj.yaja.data.NavigationChromeMode
import com.mj.yaja.data.PersonalThemeSlot
import com.mj.yaja.data.ThemeColorIntensity
import com.mj.yaja.data.ThemePreference
import java.time.DayOfWeek

@Immutable
data class RootSettingsUiState(
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val entryReviewEnabled: Boolean = true,
    val keywordHighlightingEnabled: Boolean = true,
    val fuzzyThreshold: Float = 0.90f
)

@Immutable
data class PrivacySecuritySettingsUiState(
    val isPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val autoLockTimeoutMinutes: Int = 5,
    val hideTextModeEnabled: Boolean = false
)

@Immutable
data class NavigationGesturesSettingsUiState(
    val showStatistics: Boolean = true,
    val showLookbackInNavBar: Boolean = true,
    val showKeywordsInNavBar: Boolean = true,
    val showTodosInNavBar: Boolean = true,
    val showStatisticsInNavBar: Boolean = true,
    val navigationChromeMode: NavigationChromeMode = NavigationChromeMode.FLOATING_BAR,
    val showBottomPanelLabels: Boolean = true,
    val adaptiveBottomNav: Boolean = true,
    val swipeToNavigateDatesEnabled: Boolean = true,
    val enableDragAndDrop: Boolean = false,
    val entryDeleteSelectionEnabled: Boolean = false
)

@Immutable
data class JournalExperienceSettingsUiState(
    val animationPreference: AnimationPreference = AnimationPreference.FULL,
    val isPreviewLimitEnabled: Boolean = false,
    val previewLimitLength: Int = 280,
    val showTimestamps: Boolean = true,
    val allowFutureEntries: Boolean = true,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val dateOrderPreference: DateOrderPreference = DateOrderPreference.AUTO,
    val customDateKeywords: List<DateKeywordEntry> = emptyList(),
    val showDayHeaderStats: Boolean = true,
    val renderCheckboxesAsText: Boolean = false,
    val carryForwardTodosEnabled: Boolean = false,
    val calendarDensityPreference: CalendarDensityPreference = CalendarDensityPreference.COMFORTABLE,
    val fuzzyThreshold: Float = 0.90f,
    val entryStyle: EntryStyle = EntryStyle.CARDS
)

@Immutable
data class DataRecoverySettingsUiState(
    val storageUri: String? = null,
    val lastBackupTimestamp: Long = 0L,
    val backupReminderDays: Int = 7,
    val swipeToSyncEnabled: Boolean = false,
    val largeJournalSafeMode: Boolean = false,
    val showOnboardingNextLaunch: Boolean = false,
    val versionHistoryEnabled: Boolean = false,
    val importState: JournalViewModel.ImportState = JournalViewModel.ImportState.Idle,
    val restoreSummary: JournalViewModel.RestoreSummary? = null,
    val storageMigrationInProgress: Boolean = false
)

@Immutable
data class AppearanceSettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val colorSource: ColorSource = ColorSource.MATERIAL_YOU,
    val customPalette: CustomPalette = CustomPalette.YAJA,
    val themeColorIntensity: ThemeColorIntensity = ThemeColorIntensity.NORMAL,
    val backgroundTintLevel: BackgroundTintLevel = BackgroundTintLevel.CLEAN,
    val personalThemeSlots: List<PersonalThemeSlot> = emptyList(),
    val activePersonalThemeSlotId: Int = 0,
    val uiScalePreference: UiScalePreference = UiScalePreference.NORMAL,
    val fontScalePreference: FontScalePreference = FontScalePreference.NORMAL,
    val dataFontScalePreference: FontScalePreference = FontScalePreference.NORMAL,
    val followUiFontScale: Boolean = true,
    val appFontFamily: AppFontFamily = AppFontFamily.SANS_SERIF,
    val monoFontWeight: Int = 400,
    val customFontPath: String? = null,
    val customFontName: String? = null,
    val fabPlacement: FabPlacement = FabPlacement.RIGHT
)
