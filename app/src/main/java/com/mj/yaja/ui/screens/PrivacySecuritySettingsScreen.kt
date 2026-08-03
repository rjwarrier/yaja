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
fun PrivacySecuritySettingsScreen(
        viewModel: JournalViewModel,
        onNavigateBack: () -> Unit,
        onNavigateToPinSetup: () -> Unit,
        onNavigateToPinDisable: () -> Unit,
        onNavigateToPrivacyDashboard: () -> Unit
) {
        val uiState by viewModel.privacySecuritySettingsUiState.collectAsStateWithLifecycle()

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                stringResource(
                                                        R.string.settings_privacy_security_title
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                },
                                navigationIcon = {
                                        IconButton(onClick = onNavigateBack) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Rounded.ArrowBack,
                                                        contentDescription =
                                                                stringResource(R.string.action_back)
                                                )
                                        }
                                },
                                colors =
                                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.background
                                        )
                        )
                },
                containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
                AppScreenReveal(visible = true, modifier = Modifier.fillMaxSize()) {
                        Column(
                                modifier =
                                        Modifier
                                                .fillMaxSize()
                                                .padding(paddingValues)
                                                .padding(horizontal = 20.dp)
                                                .verticalScroll(rememberScrollState())
                        ) {
                                SecuritySection(
                                        isPinEnabled = uiState.isPinEnabled,
                                        onEnablePin = onNavigateToPinSetup,
                                        onDisablePin = onNavigateToPinDisable,
                                        onChangePin = onNavigateToPinSetup,
                                        isBiometricEnabled = uiState.isBiometricEnabled,
                                        onEnableBiometric = { viewModel.enableBiometric() },
                                        onDisableBiometric = { viewModel.disableBiometric() },
                                        autoLockTimeoutMinutes = uiState.autoLockTimeoutMinutes,
                                        onAutoLockTimeoutChange = {
                                                viewModel.setAutoLockTimeout(it)
                                        },
                                        hideTextModeEnabled = uiState.hideTextModeEnabled,
                                        onHideTextModeEnabledChange = {
                                                viewModel.setHideTextModeEnabled(it)
                                        },
                                        onNavigateToPrivacyDashboard = onNavigateToPrivacyDashboard
                                )
                        }
                }
        }
}
