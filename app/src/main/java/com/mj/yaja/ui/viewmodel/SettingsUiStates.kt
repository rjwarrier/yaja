package com.mj.yaja.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.data.AppLanguage
import com.mj.yaja.data.CalendarDensityPreference
import com.mj.yaja.data.DateKeywordEntry
import com.mj.yaja.data.DateOrderPreference
import com.mj.yaja.data.EntryStyle
import com.mj.yaja.data.NavigationChromeMode
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
    val restoreSummary: JournalViewModel.RestoreSummary? = null
)
