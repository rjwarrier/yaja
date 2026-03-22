package com.mj.yaja

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.*
// import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.SettingsRepository
import com.mj.yaja.data.ThemePreference
import com.mj.yaja.ui.components.AppNavigationDrawer
import com.mj.yaja.ui.screens.AddEntryScreen
import com.mj.yaja.ui.screens.CalendarScreen
import com.mj.yaja.ui.screens.HelpScreen
import com.mj.yaja.ui.screens.HomeScreen
import com.mj.yaja.ui.screens.LookbackScreen
import com.mj.yaja.ui.screens.PinLockScreen
import com.mj.yaja.ui.screens.PinMode
import com.mj.yaja.ui.screens.SettingsScreen
import com.mj.yaja.ui.screens.ShortcodesScreen
import com.mj.yaja.ui.screens.StatisticsScreen
import com.mj.yaja.ui.theme.JournalTheme
import com.mj.yaja.ui.viewmodel.JournalViewModel
import com.mj.yaja.ui.navigation.Route
import com.mj.yaja.ui.widget.QuickCaptureWidgetProvider
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    // Use singletons so QuickCaptureActivity and TaskerReceiver share the same cache
    private val settingsRepository by lazy { SettingsRepository.getInstance(applicationContext) }
    private val fileManager by lazy { MarkdownFileManager.getInstance(applicationContext, settingsRepository) }

    // Proper Jetpack ViewModel delegate — survives config changes
    private val viewModel: JournalViewModel by viewModels {
        JournalViewModel.Factory(fileManager, settingsRepository)
    }

    private val widgetStatusReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == QuickCaptureWidgetProvider.ACTION_WIDGET_STATUS_CHANGED) {
                        viewModel.refreshWidgetStatus()
                    }
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // SplashScreen.installSplashScreen(this)
            super.onCreate(savedInstanceState)
            setContent {
                val crashLog = remember { checkCrashLog() }
                JournalApp(viewModel, crashLog) 
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Critical crash in onCreate", e)
            super.onCreate(savedInstanceState)
            throw e 
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppResume()
        val filter = IntentFilter(QuickCaptureWidgetProvider.ACTION_WIDGET_STATUS_CHANGED)
        // Use ContextCompat.registerReceiver for API compatibility (RECEIVER_NOT_EXPORTED requires API 33+)
        ContextCompat.registerReceiver(this, widgetStatusReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun checkCrashLog(): String? {
        return try {
            val file = File(cacheDir, "crash_log.txt")
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    // Helper for context access if needed
    fun getAppActivityContext(): Context = applicationContext
}

@Composable
fun JournalApp(viewModel: JournalViewModel, initialCrashLog: String? = null) {
    var showCrashDialog by remember { mutableStateOf(initialCrashLog != null) }
    
    if (showCrashDialog && initialCrashLog != null) {
        AlertDialog(
            onDismissRequest = { showCrashDialog = false },
            title = { Text("Previous Session Crash Log") },
            text = { 
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    item { Text(initialCrashLog, style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showCrashDialog = false
                    viewModel.clearCrashLog()
                }) {
                    Text("Clear & Close")
                }
            }
        )
    }

    val navController = rememberNavController()

    val themePreference by viewModel.themePreference.collectAsState()
    val appFontFamily by viewModel.appFontFamily.collectAsState()
    val fontScalePreference by viewModel.fontScalePreference.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme =
            when (themePreference) {
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
            appFontFamily = appFontFamily
    ) {
        val isPinEnabled by viewModel.isPinEnabled.collectAsState()
        val showBottomBar by viewModel.showBottomBar.collectAsState()
        val startDestination = if (isPinEnabled) Route.PinLock.path else Route.Home.path

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val uiState by viewModel.uiState.collectAsState()
        val topLevelRoutes = Route.topLevel

        val bottomBar =
                @Composable
                {
                    if (showBottomBar && currentRoute in topLevelRoutes) {
                        AnimatedFloatingBottomBar(
                                currentRoute = currentRoute,
                                onNavigateHome = {
                                    if (currentRoute != Route.Home.path) {
                                        navController.navigate(Route.Home.path) {
                                            popUpTo(Route.Home.path) { inclusive = true }
                                        }
                                    }
                                },
                                onNavigateCalendar = {
                                    if (currentRoute != Route.Calendar.path) {
                                        viewModel.refreshCalendarDates()
                                        navController.navigate(Route.Calendar.path) { popUpTo(Route.Home.path) }
                                    }
                                },
                                onNavigateLookback = {
                                    if (currentRoute != Route.Lookback.path) {
                                        navController.navigate(Route.Lookback.path) { popUpTo(Route.Home.path) }
                                    }
                                }
                        )
                    }
                }

        val showStatistics by viewModel.showStatistics.collectAsState()

        AppNavigationDrawer(
                drawerState = drawerState,
                scope = scope,
                currentRoute = currentRoute ?: Route.Home.path,
                onNavigateToJournal = {
                    navController.navigate(Route.Home.path) { popUpTo(Route.Home.path) { inclusive = true } }
                },
                onNavigateToCalendar = {
                    viewModel.refreshCalendarDates()
                    navController.navigate(Route.Calendar.path) { popUpTo(Route.Home.path) }
                },
                onNavigateToLookback = { navController.navigate(Route.Lookback.path) { popUpTo(Route.Home.path) } },
                onNavigateToStatistics = { navController.navigate(Route.Statistics.path) { popUpTo(Route.Home.path) } },
                onNavigateToShortcodes = {
                    navController.navigate(Route.Shortcodes.path) { popUpTo(Route.Home.path) }
                },
                onNavigateToSettings = { navController.navigate(Route.Settings.path) { popUpTo(Route.Home.path) } },
                onNavigateToHelp = { navController.navigate(Route.Help.path) { popUpTo(Route.Home.path) } },
                datesWithEntries = uiState.datesWithEntries,
                onSurpriseMe = { randomDate ->
                    viewModel.selectDate(randomDate)
                    navController.navigate(Route.Home.path) { popUpTo(Route.Home.path) { inclusive = true } }
                },
                showStatistics = showStatistics
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val syncProgress by viewModel.syncProgress.collectAsState()

                NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = {
                            slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec =
                                            tween(500, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                            ) +
                                    fadeIn(tween(400)) +
                                    scaleIn(
                                            initialScale = 0.92f,
                                            animationSpec =
                                                    tween(
                                                            500,
                                                            easing =
                                                                    CubicBezierEasing(
                                                                            0.2f,
                                                                            0f,
                                                                            0f,
                                                                            1f
                                                                    )
                                                    )
                                    )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec =
                                            tween(500, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                            ) +
                                    fadeOut(tween(350)) +
                                    scaleOut(
                                            targetScale = 0.95f,
                                            animationSpec =
                                                    tween(
                                                            500,
                                                            easing =
                                                                    CubicBezierEasing(
                                                                            0.2f,
                                                                            0f,
                                                                            0f,
                                                                            1f
                                                                    )
                                                    )
                                    )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec =
                                            tween(500, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                            ) +
                                    fadeIn(tween(400)) +
                                    scaleIn(
                                            initialScale = 0.95f,
                                            animationSpec =
                                                    tween(
                                                            500,
                                                            easing =
                                                                    CubicBezierEasing(
                                                                            0.2f,
                                                                            0f,
                                                                            0f,
                                                                            1f
                                                                    )
                                                    )
                                    )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth },
                                    animationSpec =
                                            tween(500, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                            ) +
                                    fadeOut(tween(350)) +
                                    scaleOut(
                                            targetScale = 0.92f,
                                            animationSpec =
                                                    tween(
                                                            500,
                                                            easing =
                                                                    CubicBezierEasing(
                                                                            0.2f,
                                                                            0f,
                                                                            0f,
                                                                            1f
                                                                    )
                                                    )
                                    )
                        }
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
                                onCancel = { navController.popBackStack() }
                        )
                    }
                    composable(Route.PinDisable.path) {
                        val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
                        PinLockScreen(
                                mode = PinMode.DISABLE,
                                checkPin = { viewModel.checkPin(it) },
                                onEnterCorrect = {
                                    viewModel.clearPin()
                                    navController.popBackStack()
                                },
                                onSetupComplete = {},
                                onCancel = { navController.popBackStack() },
                                isBiometricAvailable = isBiometricEnabled
                        )
                    }
                    composable(
                            Route.Home.path,
                            exitTransition = {
                                // When navigating to a peer top-level screen, fade+shrink instead
                                // of sliding left (which drags the FAB off-screen)
                                if (targetState.destination.route in setOf(Route.Calendar.path, Route.Lookback.path)
                                ) {
                                    fadeOut(tween(350)) +
                                            scaleOut(
                                                    targetScale = 0.95f,
                                                    animationSpec =
                                                            tween(
                                                                    500,
                                                                    easing =
                                                                            CubicBezierEasing(
                                                                                    0.2f,
                                                                                    0f,
                                                                                    0f,
                                                                                    1f
                                                                            )
                                                            )
                                            )
                                } else {
                                    slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> -fullWidth },
                                            animationSpec =
                                                    tween(
                                                            500,
                                                            easing =
                                                                    CubicBezierEasing(
                                                                            0.2f,
                                                                            0f,
                                                                            0f,
                                                                            1f
                                                                    )
                                                    )
                                    ) +
                                            fadeOut(tween(350)) +
                                            scaleOut(
                                                    targetScale = 0.95f,
                                                    animationSpec =
                                                            tween(
                                                                    500,
                                                                    easing =
                                                                            CubicBezierEasing(
                                                                                    0.2f,
                                                                                    0f,
                                                                                    0f,
                                                                                    1f
                                                                            )
                                                            )
                                            )
                                }
                            }
                    ) {
                        HomeScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNavigateToJournal = {
                                    navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                    }
                                },
                                onNavigateToCalendar = {
                                    viewModel.refreshCalendarDates()
                                    navController.navigate(Route.Calendar.path)
                                },
                                onNavigateToAddEntry = { navController.navigate(Route.AddEntry.path) },
                                onNavigateToSettings = { navController.navigate(Route.Settings.path) },
                                onNavigateToHelp = { navController.navigate(Route.Help.path) },
                                onNavigateToLookback = { navController.navigate(Route.Lookback.path) },
                                onNavigateToShortcodes = { navController.navigate(Route.Shortcodes.path) }
                        )
                    }
                    composable(Route.Lookback.path) {
                        LookbackScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNavigateToDate = { date ->
                                    viewModel.selectDate(date)
                                    navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                    }
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
                                onNavigateToLookback = {
                                    navController.navigate(Route.Lookback.path) {
                                        popUpTo(Route.Home.path) { inclusive = false }
                                    }
                                },
                                onNavigateToShortcodes = { navController.navigate(Route.Shortcodes.path) },
                                onNavigateToSettings = { navController.navigate(Route.Settings.path) },
                                onNavigateToHelp = { navController.navigate(Route.Help.path) }
                        )
                    }
                    composable(Route.Statistics.path) {
                        StatisticsScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToJournal = {
                                    navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                    }
                                },
                                onNavigateToCalendar = {
                                    viewModel.refreshCalendarDates()
                                    navController.navigate(Route.Calendar.path)
                                },
                                onNavigateToLookback = { navController.navigate(Route.Lookback.path) },
                                onNavigateToShortcodes = { navController.navigate(Route.Shortcodes.path) },
                                onNavigateToSettings = { navController.navigate(Route.Settings.path) },
                                onNavigateToHelp = { navController.navigate(Route.Help.path) }
                        )
                    }
                    composable(Route.Calendar.path) {
                        CalendarScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNavigateBack = { navController.popBackStack() },
                                onDateSelected = { date ->
                                    viewModel.selectDate(date)
                                    navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                    }
                                },
                                onDateSelectedForEntry = { date ->
                                    viewModel.selectDate(date)
                                    navController.navigate(Route.AddEntry.path)
                                },
                                onNavigateToJournal = {
                                    navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                    }
                                },
                                onNavigateToCalendar = {
                                    viewModel.refreshCalendarDates()
                                    navController.navigate(Route.Calendar.path) { popUpTo(Route.Home.path) }
                                },
                                onNavigateToLookback = {
                                    navController.navigate(Route.Lookback.path) { popUpTo(Route.Home.path) }
                                },
                                onNavigateToShortcodes = { navController.navigate(Route.Shortcodes.path) },
                                onNavigateToSettings = { navController.navigate(Route.Settings.path) },
                                onNavigateToHelp = { navController.navigate(Route.Help.path) }
                        )
                    }
                    composable(Route.AddEntry.path) {
                        AddEntryScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Route.Settings.path) {
                        SettingsScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPinSetup = { navController.navigate(Route.PinSetup.path) },
                                onNavigateToPinDisable = { navController.navigate(Route.PinDisable.path) },
                                onNavigateToHelp = { navController.navigate(Route.Help.path) },
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
                    composable(Route.Shortcodes.path) {
                        ShortcodesScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToJournal = {
                                    navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                    }
                                },
                                onNavigateToCalendar = {
                                    viewModel.refreshCalendarDates()
                                    navController.navigate(Route.Calendar.path)
                                },
                                onNavigateToLookback = { navController.navigate(Route.Lookback.path) },
                                onNavigateToShortcodes = {
                                    navController.navigate(Route.Shortcodes.path) {
                                        popUpTo(Route.Home.path) { inclusive = false }
                                    }
                                },
                                onNavigateToSettings = { navController.navigate(Route.Settings.path) },
                                onNavigateToHelp = { navController.navigate(Route.Help.path) }
                        )
                    }
                    composable(Route.Help.path) {
                        HelpScreen(
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToJournal = {
                                    navController.navigate(Route.Home.path) {
                                        popUpTo(Route.Home.path) { inclusive = true }
                                    }
                                },
                                onNavigateToCalendar = {
                                    viewModel.refreshCalendarDates()
                                    navController.navigate(Route.Calendar.path)
                                },
                                onNavigateToLookback = { navController.navigate(Route.Lookback.path) },
                                onNavigateToShortcodes = { navController.navigate(Route.Shortcodes.path) },
                                onNavigateToSettings = { navController.navigate(Route.Settings.path) },
                                onNavigateToHelp = {
                                    navController.navigate(Route.Help.path) {
                                        popUpTo(Route.Home.path) { inclusive = false }
                                    }
                                }
                        )
                    }
                }

                // Global floating bottom bar (stays static during transitions)
                // Animated visibility based on drawer state to hide it when drawer is open
                AnimatedVisibility(
                        visible = drawerState.isClosed,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                        modifier = Modifier.align(Alignment.BottomCenter)
                ) { bottomBar() }

                // Global Sync Progress Pill
                AnimatedVisibility(
                        visible = syncProgress != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier =
                                Modifier.align(Alignment.BottomCenter)
                                        .padding(
                                                bottom =
                                                        if (showBottomBar &&
                                                                        currentRoute in
                                                                                topLevelRoutes
                                                        )
                                                                96.dp
                                                        else 32.dp
                                        )
                ) { syncProgress?.let { progress -> SyncProgressPill(progress = progress) } }
            }
        }
    }
}

@Composable
fun SyncProgressPill(progress: Float) {
    Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier =
                    Modifier.padding(horizontal = 24.dp)
                            .height(64.dp)
                            .widthIn(min = 220.dp, max = 340.dp)
    ) {
        Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                        text = stringResource(R.string.syncing_data),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                )
                Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SquigglyLinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun SquigglyLinearProgressIndicator(
        progress: Float,
        modifier: Modifier = Modifier,
        color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
        trackColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "squiggly")
    val phase by
            infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 2f * PI.toFloat(),
                    animationSpec =
                            infiniteRepeatable(
                                    animation = tween(1500, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                            ),
                    label = "phase"
            )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val progressWidth = width * progress

        // Draw Track
        drawLine(
                color = trackColor,
                start = androidx.compose.ui.geometry.Offset(0f, height / 2),
                end = androidx.compose.ui.geometry.Offset(width, height / 2),
                strokeWidth = height,
                cap = StrokeCap.Round
        )

        // Draw Wavy Progress
        if (progress > 0f) {
            val path = Path()
            val points = 100
            val waveLength = 40.dp.toPx()
            val amplitude = 3.dp.toPx()

            for (i in 0..points) {
                val x = (i.toFloat() / points.toFloat()) * progressWidth
                val relativeX = x / waveLength
                val y =
                        (height / 2) +
                                (amplitude * sin((relativeX * 2 * PI + phase).toDouble()))
                                        .toFloat()

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = height, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun AnimatedFloatingBottomBar(
        currentRoute: String?,
        onNavigateHome: () -> Unit,
        onNavigateCalendar: () -> Unit,
        onNavigateLookback: () -> Unit
) {
    val items =
            listOf(
                    Triple(Route.Home.path, "Journal", Icons.Rounded.Book),
                    Triple(Route.Calendar.path, "Calendar", Icons.Rounded.CalendarMonth),
                    Triple(Route.Lookback.path, "Lookback", Icons.Rounded.History)
            )

    val selectedIndex = items.indexOfFirst { it.first == currentRoute }.coerceAtLeast(0)

    // Pill shape indicator dimensions
    val itemWidth = 56.dp
    val indicatorWidth = 48.dp
    val indicatorHeight = 48.dp
    val horizontalPadding = 1.dp

    val indicatorOffset by
            androidx.compose.animation.core.animateDpAsState(
                    targetValue =
                            horizontalPadding +
                                    (itemWidth * selectedIndex) +
                                    (itemWidth - indicatorWidth) / 2,
                    animationSpec =
                            androidx.compose.animation.core.spring(
                                    stiffness =
                                            androidx.compose.animation.core.Spring
                                                    .StiffnessMediumLow,
                                    dampingRatio =
                                            androidx.compose.animation.core.Spring
                                                    .DampingRatioNoBouncy
                            ),
                    label = "indicatorOffset"
            )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Surface(
                modifier =
                        Modifier.padding(bottom = 16.dp)
                                .windowInsetsPadding(WindowInsets.navigationBars),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.height(56.dp).wrapContentWidth()) {
                // The sliding animated indicator
                Box(
                        modifier =
                                Modifier.offset(x = indicatorOffset, y = 4.dp)
                                        .size(width = indicatorWidth, height = indicatorHeight)
                                        .background(
                                                MaterialTheme.colorScheme.secondaryContainer,
                                                androidx.compose.foundation.shape.CircleShape
                                        )
                )

                Row(
                        modifier =
                                Modifier.wrapContentWidth()
                                        .fillMaxHeight()
                                        .padding(horizontal = horizontalPadding),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, (route, label, icon) ->
                        val isSelected = index == selectedIndex
                        Box(
                                modifier =
                                        Modifier.width(itemWidth)
                                                .fillMaxHeight()
                                                .clickable(
                                                        interactionSource =
                                                                androidx.compose.runtime.remember {
                                                                    MutableInteractionSource()
                                                                },
                                                        indication = null,
                                                        onClick = {
                                                            when (route) {
                                                                Route.Home.path -> onNavigateHome()
                                                                Route.Calendar.path -> onNavigateCalendar()
                                                                Route.Lookback.path -> onNavigateLookback()
                                                            }
                                                        }
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint =
                                            if (isSelected)
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
