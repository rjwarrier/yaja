package com.mj.yaja.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.mj.yaja.data.AppLanguage
import com.mj.yaja.data.NavigationChromeMode

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
