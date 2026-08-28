package com.mj.yaja.ui.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mj.yaja.data.ThemePreference
import com.mj.yaja.data.NavigationChromeMode
import com.mj.yaja.ui.navigation.Route
import com.mj.yaja.ui.design.ProvideAnimationPreference
import com.mj.yaja.ui.theme.JournalTheme
import com.mj.yaja.ui.viewmodel.ExternalOpenRequest
import com.mj.yaja.ui.viewmodel.JournalViewModel

@Composable
fun JournalApp(
    viewModel: JournalViewModel,
    initialCrashLog: String? = null
) {
    var showCrashDialog by remember { mutableStateOf(initialCrashLog != null) }

    if (showCrashDialog && initialCrashLog != null) {
        CrashLogDialog(
            crashLog = initialCrashLog,
            onDismiss = { showCrashDialog = false },
            onClear = { viewModel.clearCrashLog() }
        )
    }

    val navController = rememberNavController()

    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val colorSource by viewModel.colorSource.collectAsStateWithLifecycle()
    val customPalette by viewModel.customPalette.collectAsStateWithLifecycle()
    val themeColorIntensity by viewModel.themeColorIntensity.collectAsStateWithLifecycle()
    val backgroundTintLevel by viewModel.backgroundTintLevel.collectAsStateWithLifecycle()
    val personalThemeSlots by viewModel.personalThemeSlots.collectAsStateWithLifecycle()
    val activePersonalThemeSlotId by viewModel.activePersonalThemeSlotId.collectAsStateWithLifecycle()
    val appFontFamily by viewModel.appFontFamily.collectAsStateWithLifecycle()
    val monoFontWeight by viewModel.monoFontWeight.collectAsStateWithLifecycle()
    val customFontPath by viewModel.customFontPath.collectAsStateWithLifecycle()
    val uiScalePreference by viewModel.uiScalePreference.collectAsStateWithLifecycle()
    val fontScalePreference by viewModel.fontScalePreference.collectAsStateWithLifecycle()
    val dataFontScalePreference by viewModel.dataFontScalePreference.collectAsStateWithLifecycle()
    val followUiFontScale by viewModel.followUiFontScale.collectAsStateWithLifecycle()
    val animationPreference by viewModel.animationPreference.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemDark
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.AMOLED -> true
    }
    val useAmoledTheme = themePreference == ThemePreference.AMOLED

    JournalTheme(
        darkTheme = useDarkTheme,
        amoledTheme = useAmoledTheme,
        fontScale = fontScalePreference.scale,
        dataFontScale = if (followUiFontScale) fontScalePreference.scale else dataFontScalePreference.scale,
        uiScale = uiScalePreference.scale,
        appFontFamily = appFontFamily,
        monoFontWeight = monoFontWeight,
        customFontPath = customFontPath,
        colorSource = colorSource,
        customPalette = customPalette,
        colorIntensity = themeColorIntensity,
        backgroundTintLevel = backgroundTintLevel,
        personalThemeSlot =
            personalThemeSlots.firstOrNull { it.slotId == activePersonalThemeSlotId }
    ) {
        ProvideAnimationPreference(animationPreference) {
            val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
            val showBottomBar by viewModel.showBottomBar.collectAsStateWithLifecycle()
            val navigationChromeMode by viewModel.navigationChromeMode.collectAsStateWithLifecycle()
            val showBottomPanelLabels by viewModel.showBottomPanelLabels.collectAsStateWithLifecycle()
            val showLookbackInNavBar by viewModel.showLookbackInNavBar.collectAsStateWithLifecycle()
            val showKeywordsInNavBar by viewModel.showKeywordsInNavBar.collectAsStateWithLifecycle()
            val showTodosInNavBar by viewModel.showTodosInNavBar.collectAsStateWithLifecycle()
            val showStatisticsInNavBar by viewModel.showStatisticsInNavBar.collectAsStateWithLifecycle()
            val showStatistics by viewModel.showStatistics.collectAsStateWithLifecycle()
            val adaptiveBottomNav by viewModel.adaptiveBottomNav.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val todos by viewModel.todos.collectAsStateWithLifecycle()
            val events by viewModel.events.collectAsStateWithLifecycle()
            val keywords by viewModel.keywords.collectAsStateWithLifecycle()
            val shouldShowOnboarding by viewModel.shouldShowOnboarding.collectAsStateWithLifecycle()
            val showOnboardingNextLaunch by
                viewModel.showOnboardingNextLaunch.collectAsStateWithLifecycle()
            val shouldConsumeOnboardingLaunchRequest = remember { showOnboardingNextLaunch }
            val useExpressivePanel =
                showBottomBar && navigationChromeMode == NavigationChromeMode.EXPRESSIVE_PANEL
            val startDestination =
                when {
                    isPinEnabled -> Route.PinLock.path
                    shouldShowOnboarding -> Route.Onboarding.path
                    else -> Route.Home.path
                }

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            LaunchedEffect(Unit) {
                viewModel.externalOpenRequests.collect { request ->
                    when (request) {
                        is ExternalOpenRequest.Date -> {
                            navController.navigate(Route.Home.path) {
                                launchSingleTop = true
                                popUpTo(Route.Home.path) { inclusive = true }
                            }
                        }
                        is ExternalOpenRequest.Entry -> {
                            navController.navigate(Route.AddEntry.path) {
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }

            LaunchedEffect(shouldConsumeOnboardingLaunchRequest) {
                if (shouldConsumeOnboardingLaunchRequest) {
                    viewModel.consumeOnboardingLaunchRequest()
                }
            }

            val shouldLock by viewModel.shouldLock.collectAsStateWithLifecycle()
            LaunchedEffect(shouldLock, currentRoute) {
                if (shouldLock && currentRoute != Route.PinLock.path) {
                    navController.navigate(Route.PinLock.path) {
                        popUpTo(Route.Home.path)
                    }
                    viewModel.onLockHandled()
                }
            }

            val topLevelRoutes = remember(
                useExpressivePanel,
                adaptiveBottomNav,
                showLookbackInNavBar,
                showKeywordsInNavBar,
                showTodosInNavBar,
                showStatistics,
                showStatisticsInNavBar,
                currentRoute,
                uiState.datesWithEntries.size,
                todos.size,
                events.size,
                keywords.size
            ) {
                val bottomShowLookback =
                    showLookbackInNavBar &&
                        (!adaptiveBottomNav ||
                            uiState.datesWithEntries.size > 1 ||
                            currentRoute == Route.Lookback.path)
                val bottomShowKeywords =
                    showKeywordsInNavBar &&
                        (!adaptiveBottomNav ||
                            keywords.isNotEmpty() ||
                            currentRoute == Route.Keywords.path)
                val bottomShowTodos =
                    showTodosInNavBar &&
                        (!adaptiveBottomNav ||
                            todos.isNotEmpty() ||
                            events.isNotEmpty() ||
                            currentRoute == Route.Todos.path ||
                            currentRoute == Route.RecurringTasks.path)
                val bottomShowStatistics =
                    showStatistics &&
                        showStatisticsInNavBar &&
                        (!adaptiveBottomNav ||
                            uiState.datesWithEntries.size >= 3 ||
                            currentRoute == Route.Statistics.path)
                buildSet {
                    add(Route.Home.path)
                    add(Route.Calendar.path)
                    if (bottomShowLookback) add(Route.Lookback.path)
                    if (bottomShowKeywords) add(Route.Keywords.path)
                    if (bottomShowTodos) {
                        add(Route.Todos.path)
                        add(Route.RecurringTasks.path)
                    }
                    if (bottomShowStatistics) add(Route.Statistics.path)
                }
            }
            val bottomShowLookbackInNavBar =
                showLookbackInNavBar &&
                    (!adaptiveBottomNav ||
                        uiState.datesWithEntries.size > 1 ||
                        currentRoute == Route.Lookback.path)
            val bottomShowKeywordsInNavBar =
                showKeywordsInNavBar &&
                    (!adaptiveBottomNav ||
                        keywords.isNotEmpty() ||
                        currentRoute == Route.Keywords.path)
            val bottomShowTodosInNavBar =
                showTodosInNavBar &&
                    (!adaptiveBottomNav ||
                        todos.isNotEmpty() ||
                        events.isNotEmpty() ||
                        currentRoute == Route.Todos.path ||
                        currentRoute == Route.RecurringTasks.path)
            val bottomShowStatisticsInNavBar =
                showStatistics &&
                    showStatisticsInNavBar &&
                    (!adaptiveBottomNav ||
                        uiState.datesWithEntries.size >= 3 ||
                        currentRoute == Route.Statistics.path)

            JournalScaffold(
                viewModel = viewModel,
                navController = navController,
                drawerState = drawerState,
                currentRoute = currentRoute,
                startDestination = startDestination,
                showBottomBar = showBottomBar,
                navigationChromeMode = navigationChromeMode,
                showBottomPanelLabels = showBottomPanelLabels,
                showLookbackInNavBar = showLookbackInNavBar,
                showKeywordsInNavBar = showKeywordsInNavBar,
                showTodosInNavBar = showTodosInNavBar,
                showStatisticsInNavBar = showStatistics && showStatisticsInNavBar,
                bottomShowLookbackInNavBar = bottomShowLookbackInNavBar,
                bottomShowKeywordsInNavBar = bottomShowKeywordsInNavBar,
                bottomShowTodosInNavBar = bottomShowTodosInNavBar,
                bottomShowStatisticsInNavBar = bottomShowStatisticsInNavBar,
                showStatistics = showStatistics,
                topLevelRoutes = topLevelRoutes
            )
        }
    }
}
