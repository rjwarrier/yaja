package com.mj.yaja.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.mj.yaja.data.AppLanguage

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
