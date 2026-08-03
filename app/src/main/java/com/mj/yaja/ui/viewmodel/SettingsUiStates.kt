package com.mj.yaja.ui.viewmodel

import androidx.compose.runtime.Immutable

@Immutable
data class PrivacySecuritySettingsUiState(
    val isPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val autoLockTimeoutMinutes: Int = 5,
    val hideTextModeEnabled: Boolean = false
)
