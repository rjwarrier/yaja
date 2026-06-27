package com.mj.yaja.ui.app

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.navFadeTween
import com.mj.yaja.ui.design.navScaleTween
import com.mj.yaja.ui.design.navSlideTween
import com.mj.yaja.ui.navigation.Route
import com.mj.yaja.ui.viewmodel.JournalViewModel

@Composable
fun JournalNavHost(
    navController: NavHostController,
    startDestination: String,
    viewModel: JournalViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit,
    isDrawerOpen: Boolean,
    onStartUpdateFlow: (com.google.android.play.core.appupdate.AppUpdateInfo) -> Unit
) {
    val motionPreference = LocalAnimationPreference.current
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = motionPreference.navSlideTween()
            ) + fadeIn(motionPreference.navFadeTween(entering = true)) + scaleIn(
                initialScale = 0.985f,
                animationSpec = motionPreference.navScaleTween()
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = motionPreference.navSlideTween()
            ) + fadeOut(motionPreference.navFadeTween(entering = false)) + scaleOut(
                targetScale = 0.99f,
                animationSpec = motionPreference.navScaleTween()
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = motionPreference.navSlideTween()
            ) + fadeIn(motionPreference.navFadeTween(entering = true)) + scaleIn(
                initialScale = 0.99f,
                animationSpec = motionPreference.navScaleTween()
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = motionPreference.navSlideTween()
            ) + fadeOut(motionPreference.navFadeTween(entering = false)) + scaleOut(
                targetScale = 0.985f,
                animationSpec = motionPreference.navScaleTween()
            )
        }
    ) {
        addSecurityAndSettingsRoutes(
            navController = navController,
            viewModel = viewModel,
            onOpenDrawer = onOpenDrawer
        )
        addCoreJournalRoutes(
            navController = navController,
            viewModel = viewModel,
            onOpenDrawer = onOpenDrawer,
            isDrawerOpen = isDrawerOpen,
            motionPreference = motionPreference,
            onStartUpdateFlow = onStartUpdateFlow
        )
        addInsightRoutes(
            navController = navController,
            viewModel = viewModel,
            onOpenDrawer = onOpenDrawer
        )
    }
}
