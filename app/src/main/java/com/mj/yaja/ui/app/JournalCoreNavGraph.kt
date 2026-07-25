package com.mj.yaja.ui.app

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mj.yaja.ui.navigation.Route
import com.mj.yaja.ui.screens.AddEntryScreen
import com.mj.yaja.ui.screens.CalendarScreen
import com.mj.yaja.ui.screens.HomeScreen
import com.mj.yaja.ui.screens.TimelineScreen
import com.mj.yaja.ui.viewmodel.JournalViewModel
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.ui.design.navFadeTween
import com.mj.yaja.ui.design.navScaleTween
import com.mj.yaja.ui.design.navSlideTween
import java.time.LocalDate

internal fun NavGraphBuilder.addCoreJournalRoutes(
        navController: NavHostController,
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit,
        isDrawerOpen: Boolean,
        motionPreference: AnimationPreference
) {
        composable(
                route = Route.Home.path,
                enterTransition = {
                        if (initialState.destination.route == Route.Calendar.path) {
                                slideInHorizontally(
                                        initialOffsetX = { fullWidth -> -fullWidth },
                                        animationSpec = motionPreference.navSlideTween()
                                ) + fadeIn(motionPreference.navFadeTween(entering = true)) + scaleIn(
                                        initialScale = 0.99f,
                                        animationSpec = motionPreference.navScaleTween()
                                )
                        } else {
                                slideInHorizontally(
                                        initialOffsetX = { fullWidth -> fullWidth },
                                        animationSpec = motionPreference.navSlideTween()
                                ) + fadeIn(motionPreference.navFadeTween(entering = true)) + scaleIn(
                                        initialScale = 0.985f,
                                        animationSpec = motionPreference.navScaleTween()
                                )
                        }
                },
                exitTransition = {
                        if (targetState.destination.route == Route.Calendar.path) {
                                slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> fullWidth },
                                        animationSpec = motionPreference.navSlideTween()
                                ) + fadeOut(motionPreference.navFadeTween(entering = false)) + scaleOut(
                                        targetScale = 0.99f,
                                        animationSpec = motionPreference.navScaleTween()
                                )
                        } else {
                                slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> -fullWidth },
                                        animationSpec = motionPreference.navSlideTween()
                                ) + fadeOut(motionPreference.navFadeTween(entering = false)) + scaleOut(
                                        targetScale = 0.99f,
                                        animationSpec = motionPreference.navScaleTween()
                                )
                        }
                }
        ) {
                HomeScreen(
                        viewModel = viewModel,
                        onOpenDrawer = onOpenDrawer,
                        isDrawerOpen = isDrawerOpen,
                        onNavigateToAddEntry = { navController.navigate(Route.AddEntry.path) },
                        onNavigateToVersionSnapshots = { navController.navigate(Route.VersionSnapshots.path) }
                )
        }
        composable(Route.Calendar.path) {
                CalendarScreen(
                        viewModel = viewModel,
                        onOpenDrawer = onOpenDrawer,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTimeline = { navController.navigate(Route.Timeline.path) },
                        onDateSelected = { date ->
                                viewModel.selectDate(date)
                                navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                }
                        },
                        onDateSelectedForEntry = { date ->
                                viewModel.selectDate(date)
                                navController.navigate(Route.AddEntry.path)
                        }
                )
        }
        composable(Route.Timeline.path) {
                TimelineScreen(
                        viewModel = viewModel,
                        onOpenDrawer = onOpenDrawer,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDate = { date ->
                                viewModel.selectDate(date)
                                navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                }
                        }
                )
        }
        composable(Route.AddEntry.path) {
                AddEntryScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onJumpToToday = {
                                viewModel.selectDate(LocalDate.now())
                                navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                }
                        }
                )
        }
}
