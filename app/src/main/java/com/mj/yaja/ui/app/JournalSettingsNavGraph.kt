package com.mj.yaja.ui.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.core.net.toUri
import com.mj.yaja.ui.navigation.Route
import com.mj.yaja.ui.screens.AdvancedIntegrationsSettingsScreen
import com.mj.yaja.ui.screens.AppLogScreen
import com.mj.yaja.ui.screens.AppearanceSettingsScreen
import com.mj.yaja.ui.screens.DataRecoverySettingsScreen
import com.mj.yaja.ui.screens.DataPrivacyDashboardScreen
import com.mj.yaja.ui.screens.HelpScreen
import com.mj.yaja.ui.screens.HelpAboutSettingsScreen
import com.mj.yaja.ui.screens.ShareAppScreen
import com.mj.yaja.ui.screens.JournalExperienceSettingsScreen
import com.mj.yaja.ui.screens.LanguageSettingsScreen
import com.mj.yaja.ui.screens.NavigationGesturesSettingsScreen
import com.mj.yaja.ui.screens.OnboardingScreen
import com.mj.yaja.ui.screens.PinLockScreen
import com.mj.yaja.ui.screens.PinMode
import com.mj.yaja.ui.screens.PrivacySecuritySettingsScreen
import com.mj.yaja.ui.screens.RebuildCacheScreen
import com.mj.yaja.ui.screens.ReviewInsightsSettingsScreen
import com.mj.yaja.ui.screens.SettingsScreen
import com.mj.yaja.ui.screens.ShortcodesScreen
import com.mj.yaja.ui.screens.TaskerIntegrationScreen
import com.mj.yaja.ui.screens.VersionHistorySettingsScreen
import com.mj.yaja.ui.screens.VersionSnapshotsScreen
import com.mj.yaja.ui.viewmodel.JournalViewModel

internal fun NavGraphBuilder.addSecurityAndSettingsRoutes(
        navController: NavHostController,
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit
) {
        composable(Route.Onboarding.path) {
                val hasCompletedOnboarding by
                        viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
                OnboardingScreen(
                        viewModel = viewModel,
                        isRerun = hasCompletedOnboarding,
                        onNavigateToPinSetup = { navController.navigate(Route.PinSetup.path) },
                        onComplete = {
                                navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Onboarding.path) { inclusive = true }
                                }
                        }
                )
        }
        composable(Route.PinLock.path) {
                val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
                val shouldShowOnboarding by
                        viewModel.shouldShowOnboarding.collectAsStateWithLifecycle()
                PinLockScreen(
                        mode = PinMode.ENTER,
                        checkPin = { viewModel.checkPin(it) },
                        onEnterCorrect = {
                                val destination =
                                        if (shouldShowOnboarding) {
                                                Route.Onboarding.path
                                        } else {
                                                Route.Home.path
                                        }
                                navController.navigate(destination) {
                                        popUpTo(Route.PinLock.path) { inclusive = true }
                                }
                        },
                        onSetupComplete = {},
                        isBiometricAvailable = isBiometricEnabled
                )
        }
        composable(Route.PinSetup.path) {
                PinLockScreen(
                        mode = PinMode.SETUP,
                        onSetupComplete = { pin ->
                                viewModel.setPin(pin)
                                navController.popBackStack()
                        },
                        onEnterCorrect = {},
                        checkPin = { true }
                )
        }
        composable(Route.PinDisable.path) {
                PinLockScreen(
                        mode = PinMode.DISABLE,
                        onSetupComplete = {},
                        onEnterCorrect = {
                                viewModel.clearPin()
                                navController.popBackStack()
                        },
                        checkPin = { viewModel.checkPin(it) }
                )
        }
        composable(Route.Settings.path) {
                SettingsScreen(
                        onOpenDrawer = onOpenDrawer,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPinSetup = { navController.navigate(Route.PinSetup.path) },
                        onNavigateToPinDisable = { navController.navigate(Route.PinDisable.path) },
                        onNavigateToTaskerIntegration = {
                                navController.navigate(Route.TaskerIntegration.path)
                        },
                        onNavigateToRebuildCache = {
                                navController.navigate(Route.RebuildCache.path)
                        },
                        onNavigateToVersionHistory = {
                                navController.navigate(Route.VersionHistory.path)
                        },
                        onNavigateToAppearance = { navController.navigate(Route.Appearance.path) },
                        onNavigateToJournalExperience = {
                                navController.navigate(Route.JournalExperience.path)
                        },
                        onNavigateToNavigationGestures = {
                                navController.navigate(Route.NavigationGestures.path)
                        },
                        onNavigateToPrivacySecurity = {
                                navController.navigate(Route.PrivacySecurity.path)
                        },
                        onNavigateToDataRecovery = {
                                navController.navigate(Route.DataRecovery.path)
                        },
                        onNavigateToLanguage = {
                                navController.navigate(Route.LanguageSettings.path)
                        },
                        onNavigateToReviewInsights = {
                                navController.navigate(Route.ReviewInsightsSettings.path)
                        },
                        onNavigateToAdvancedIntegrations = {
                                navController.navigate(Route.AdvancedIntegrationsSettings.path)
                        },
                        onNavigateToHelpAbout = {
                                navController.navigate(Route.HelpAboutSettings.path)
                        },
                        onNavigateToHelp = { navController.navigate(Route.Help.path) },
                        onNavigateToAppLog = { navController.navigate(Route.AppLog.path) },
                        onNavigateToShortcodes = { navController.navigate(Route.Shortcodes.path) },
                        onNavigateToPrivacyDashboard = {
                                navController.navigate(Route.DataPrivacyDashboard.path)
                        },
                        onNavigateToJournal = {
                                navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                }
                        },
                        onNavigateToCalendar = {
                                viewModel.refreshCalendarDates()
                                navController.navigate(Route.Calendar.path)
                        },
                        onNavigateToLookback = { navController.navigate(Route.Lookback.path) }
                )
        }
        composable(Route.Appearance.path) {
                AppearanceSettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.JournalExperience.path) {
                JournalExperienceSettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.NavigationGestures.path) {
                NavigationGesturesSettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.PrivacySecurity.path) {
                PrivacySecuritySettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPinSetup = { navController.navigate(Route.PinSetup.path) },
                        onNavigateToPinDisable = { navController.navigate(Route.PinDisable.path) },
                        onNavigateToPrivacyDashboard = {
                                navController.navigate(Route.DataPrivacyDashboard.path)
                        }
                )
        }
        composable(Route.DataRecovery.path) {
                DataRecoverySettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToRebuildCache = {
                                navController.navigate(Route.RebuildCache.path)
                        },
                        onNavigateToVersionHistory = {
                                navController.navigate(Route.VersionHistory.path)
                        }
                )
        }
        composable(Route.LanguageSettings.path) {
                LanguageSettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.ReviewInsightsSettings.path) {
                ReviewInsightsSettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.AdvancedIntegrationsSettings.path) {
                AdvancedIntegrationsSettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTaskerIntegration = {
                                navController.navigate(Route.TaskerIntegration.path)
                        },
                        onNavigateToShortcodes = {
                                navController.navigate(Route.Shortcodes.path)
                        }
                )
        }
        composable(Route.HelpAboutSettings.path) {
                HelpAboutSettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToHelp = { navController.navigate(Route.Help.path) },
                        onNavigateToAppLog = { navController.navigate(Route.AppLog.path) },
                        onNavigateToShare = { navController.navigate(Route.ShareApp.path) }
                )
        }
        composable(Route.ShareApp.path) {
                ShareAppScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Route.DataPrivacyDashboard.path) {
                val storageUri by viewModel.storageUri.collectAsStateWithLifecycle()
                val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsStateWithLifecycle()
                val backupReminderDays by viewModel.backupReminderDays.collectAsStateWithLifecycle()
                val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
                val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
                val autoLockTimeoutMinutes by
                        viewModel.autoLockTimeoutMinutes.collectAsStateWithLifecycle()
                val hideTextModeEnabled by
                        viewModel.hideTextModeEnabled.collectAsStateWithLifecycle()
                val allowTaskerAccess by viewModel.allowTaskerAccess.collectAsStateWithLifecycle()
                val allowTaskerEvents by viewModel.allowTaskerEvents.collectAsStateWithLifecycle()
                val includeEntryTextInTaskerEvents by
                        viewModel.includeEntryTextInTaskerEvents.collectAsStateWithLifecycle()

                val formattedBackupDate =
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

                DataPrivacyDashboardScreen(
                        isPinEnabled = isPinEnabled,
                        isBiometricEnabled = isBiometricEnabled,
                        autoLockTimeoutMinutes = autoLockTimeoutMinutes,
                        hideTextModeEnabled = hideTextModeEnabled,
                        storageLocationText =
                                if (storageUri == null) {
                                        "App Internal Storage (Default)"
                                } else {
                                        "Custom Folder:\n" +
                                                (storageUri?.toUri()?.path
                                                        ?.substringAfterLast(":")
                                                        ?: "")
                                },
                        formattedBackupDate = formattedBackupDate,
                        backupReminderDays = backupReminderDays,
                        allowTaskerAccess = allowTaskerAccess,
                        allowTaskerEvents = allowTaskerEvents,
                        includeEntryTextInTaskerEvents = includeEntryTextInTaskerEvents,
                        onEnableHideTextMode = { viewModel.setHideTextModeEnabled(true) },
                        onDisableTaskerTextSharing = {
                                viewModel.setIncludeEntryTextInTaskerEvents(false)
                        },
                        onNavigateToTaskerIntegration = {
                                navController.navigate(Route.TaskerIntegration.path)
                        },
                        onNavigateToPinSetup = { navController.navigate(Route.PinSetup.path) },
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.RebuildCache.path) {
                RebuildCacheScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.VersionHistory.path) {
                VersionHistorySettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.VersionSnapshots.path) {
                VersionSnapshotsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.Shortcodes.path) {
                ShortcodesScreen(
                        viewModel = viewModel,
                        onOpenDrawer = onOpenDrawer,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.Help.path) {
                HelpScreen(
                        onOpenDrawer = onOpenDrawer,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.AppLog.path) {
                AppLogScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
        composable(Route.TaskerIntegration.path) {
                TaskerIntegrationScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                )
        }
}
