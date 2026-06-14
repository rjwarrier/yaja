package com.mj.yaja.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mj.yaja.ui.navigation.Route
import com.mj.yaja.ui.screens.AppLogScreen
import com.mj.yaja.ui.screens.AppearanceSettingsScreen
import com.mj.yaja.ui.screens.HelpScreen
import com.mj.yaja.ui.screens.PinLockScreen
import com.mj.yaja.ui.screens.PinMode
import com.mj.yaja.ui.screens.RebuildCacheScreen
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
        composable(Route.PinLock.path) {
                val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
                PinLockScreen(
                        mode = PinMode.ENTER,
                        checkPin = { viewModel.checkPin(it) },
                        onEnterCorrect = {
                                navController.navigate(Route.Home.path) {
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
                        viewModel = viewModel,
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
                        onNavigateToHelp = { navController.navigate(Route.Help.path) },
                        onNavigateToAppLog = { navController.navigate(Route.AppLog.path) },
                        onNavigateToShortcodes = { navController.navigate(Route.Shortcodes.path) },
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
