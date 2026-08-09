package com.mj.yaja.ui.app

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.R
import com.mj.yaja.data.NavigationChromeMode
import com.mj.yaja.ui.components.AppNavigationDrawer
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.scaledDuration
import com.mj.yaja.ui.design.tweenSpec
import com.mj.yaja.ui.navigation.Route
import com.mj.yaja.ui.viewmodel.JournalViewModel
import kotlin.math.max
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun JournalScaffold(
    viewModel: JournalViewModel,
    navController: NavHostController,
    drawerState: DrawerState,
    currentRoute: String?,
    startDestination: String,
    showBottomBar: Boolean,
    navigationChromeMode: NavigationChromeMode,
    showBottomPanelLabels: Boolean,
    showLookbackInNavBar: Boolean,
    showKeywordsInNavBar: Boolean,
    showTodosInNavBar: Boolean,
    showStatisticsInNavBar: Boolean,
    bottomShowLookbackInNavBar: Boolean = showLookbackInNavBar,
    bottomShowKeywordsInNavBar: Boolean = showKeywordsInNavBar,
    bottomShowTodosInNavBar: Boolean = showTodosInNavBar,
    bottomShowStatisticsInNavBar: Boolean = showStatisticsInNavBar,
    showStatistics: Boolean,
    topLevelRoutes: Set<String>
) {
    val scope = rememberCoroutineScope()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val backgroundWorkLabel by viewModel.backgroundWorkLabel.collectAsStateWithLifecycle()
    val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsStateWithLifecycle()
    val backupReminderDays by viewModel.backupReminderDays.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val motionPreference = LocalAnimationPreference.current
    val shouldShowBottomChrome =
        showBottomBar &&
            (currentRoute in topLevelRoutes)
    val useExpressivePanel =
        shouldShowBottomChrome &&
            navigationChromeMode == NavigationChromeMode.EXPRESSIVE_PANEL &&
            currentRoute in topLevelRoutes
    val isDrawerOpen =
        drawerState.currentValue != DrawerValue.Closed ||
            drawerState.targetValue != DrawerValue.Closed
    val bottomChromePadding = if (useExpressivePanel) 128.dp else 104.dp
    val syncChromePadding = if (useExpressivePanel) 120.dp else 96.dp

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val backupTooOld =
        remember(lastBackupTimestamp, backupReminderDays) {
            if (backupReminderDays <= 0) {
                false
            } else if (lastBackupTimestamp <= 0L) {
                true
            } else {
                val ageMillis = max(0L, System.currentTimeMillis() - lastBackupTimestamp)
                ageMillis > backupReminderDays * 24L * 60L * 60L * 1000L
            }
        }

    val bottomBar: @Composable () -> Unit = {
        if (shouldShowBottomChrome) {
            if (navigationChromeMode == NavigationChromeMode.EXPRESSIVE_PANEL) {
                ExpressiveBottomNavigationPanel(
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
                    },
                    onNavigateStatistics = {
                        if (currentRoute != Route.Statistics.path) {
                            navController.navigate(Route.Statistics.path) { popUpTo(Route.Home.path) }
                        }
                    },
                    onNavigateTodos = {
                        if (currentRoute != Route.Todos.path) {
                            navController.navigate(Route.Todos.path) { popUpTo(Route.Home.path) }
                        }
                    },
                    onNavigateKeywords = {
                        if (currentRoute != Route.Keywords.path) {
                            navController.navigate(Route.Keywords.path) { popUpTo(Route.Home.path) }
                        }
                    },
                    showLookbackInNavBar = bottomShowLookbackInNavBar,
                    showKeywordsInNavBar = bottomShowKeywordsInNavBar,
                    showTodosInNavBar = bottomShowTodosInNavBar,
                    showStatisticsInNavBar = bottomShowStatisticsInNavBar,
                    showLabels = showBottomPanelLabels
                )
            } else {
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
                    },
                    onNavigateStatistics = {
                        if (currentRoute != Route.Statistics.path) {
                            navController.navigate(Route.Statistics.path) { popUpTo(Route.Home.path) }
                        }
                    },
                    onNavigateTodos = {
                        if (currentRoute != Route.Todos.path) {
                            navController.navigate(Route.Todos.path) { popUpTo(Route.Home.path) }
                        }
                    },
                    onNavigateKeywords = {
                        if (currentRoute != Route.Keywords.path) {
                            navController.navigate(Route.Keywords.path) { popUpTo(Route.Home.path) }
                        }
                    },
                    showLookbackInNavBar = bottomShowLookbackInNavBar,
                    showKeywordsInNavBar = bottomShowKeywordsInNavBar,
                    showTodosInNavBar = bottomShowTodosInNavBar,
                    showStatisticsInNavBar = bottomShowStatisticsInNavBar
                )
            }
        }
    }

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
        onNavigateToKeywords = { navController.navigate(Route.Keywords.path) { popUpTo(Route.Home.path) } },
        onNavigateToShortcodes = { navController.navigate(Route.Shortcodes.path) { popUpTo(Route.Home.path) } },
        onNavigateToTodos = { navController.navigate(Route.Todos.path) { popUpTo(Route.Home.path) } },
        onNavigateToRebuildCache = { navController.navigate(Route.RebuildCache.path) { popUpTo(Route.Home.path) } },
        onBackupData = { viewModel.backupData(context) },
        showBackupReminder = backupTooOld,
        onNavigateToSettings = { navController.navigate(Route.Settings.path) { popUpTo(Route.Home.path) } },
        onNavigateToHelp = {
                navController.navigate(Route.HelpAboutSettings.path) { popUpTo(Route.Home.path) }
        },
        showStatistics = showStatistics,
        showLookbackInNavBar = showLookbackInNavBar,
        showKeywordsInNavBar = showKeywordsInNavBar,
        showTodosInNavBar = showTodosInNavBar,
        showStatisticsInNavBar = showStatistics && showStatisticsInNavBar,
        syncProgress = syncProgress,
        backgroundWorkLabel = backgroundWorkLabel
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            JournalNavHost(
                navController = navController,
                startDestination = startDestination,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                onOpenDrawer = { scope.launch { drawerState.open() } },
                isDrawerOpen = isDrawerOpen
            )

            AnimatedVisibility(
                visible = drawerState.isClosed,
                enter = motionPreference.enterOrNone(
                    fadeIn(animationSpec = motionPreference.floatTween(180)) +
                        slideInVertically(
                            animationSpec = motionPreference.tweenSpec(220),
                            initialOffsetY = { it / 2 }
                        )
                ),
                exit = motionPreference.exitOrNone(
                    fadeOut(animationSpec = motionPreference.floatTween(140)) +
                        slideOutVertically(
                            animationSpec = motionPreference.tweenSpec(180),
                            targetOffsetY = { it / 2 }
                        )
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) { bottomBar() }

            val isImportRunning = importState is JournalViewModel.ImportState.Running
            AnimatedVisibility(
                visible = isImportRunning && drawerState.isClosed,
                enter = motionPreference.enterOrNone(
                    slideInVertically(
                        animationSpec = motionPreference.tweenSpec(220),
                        initialOffsetY = { it }
                    ) + fadeIn(animationSpec = motionPreference.floatTween(180))
                ),
                exit = motionPreference.exitOrNone(
                    slideOutVertically(
                        animationSpec = motionPreference.tweenSpec(180),
                        targetOffsetY = { it }
                    ) + fadeOut(animationSpec = motionPreference.floatTween(140))
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (shouldShowBottomChrome) bottomChromePadding else 40.dp)
            ) {
                val runningState = importState as? JournalViewModel.ImportState.Running
                if (runningState != null) {
                    ImportProgressChip(
                        current = runningState.current,
                        total = runningState.total,
                        onCancel = { viewModel.cancelImport() }
                    )
                }
            }

            AnimatedVisibility(
                visible = syncProgress != null || backgroundWorkLabel != null,
                enter = motionPreference.enterOrNone(
                    slideInVertically(
                        animationSpec = motionPreference.tweenSpec(220),
                        initialOffsetY = { it }
                    ) + fadeIn(animationSpec = motionPreference.floatTween(180))
                ),
                exit = motionPreference.exitOrNone(
                    slideOutVertically(
                        animationSpec = motionPreference.tweenSpec(180),
                        targetOffsetY = { it }
                    ) + fadeOut(animationSpec = motionPreference.floatTween(140))
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (shouldShowBottomChrome) syncChromePadding else 32.dp)
            ) {
                SyncProgressPill(
                    progress = syncProgress,
                    label = backgroundWorkLabel ?: stringResource(R.string.syncing_data)
                )
            }
        }
    }
}

@Composable
private fun SyncProgressPill(progress: Float?, label: String) {
    var startedAtMillis by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(progress == null) {
        if (progress == null) {
            startedAtMillis = null
        } else if (startedAtMillis == null) {
            startedAtMillis = System.currentTimeMillis()
        }
    }

    val progressText = progress?.let { currentProgress ->
        val percentage = (currentProgress * 100).toInt()
        val etaText = estimateRemainingTimeText(
            progress = currentProgress,
            startedAtMillis = startedAtMillis
        )
        if (etaText != null) "$percentage% - $etaText" else "$percentage%"
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .heightIn(min = 64.dp)
            .widthIn(min = 240.dp, max = 380.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                if (progressText != null) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (progress != null) {
                SquigglyLinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

private fun estimateRemainingTimeText(progress: Float, startedAtMillis: Long?): String? {
    val startedAt = startedAtMillis ?: return null
    val safeProgress = progress.coerceIn(0f, 1f)
    if (safeProgress < 0.01f || safeProgress >= 0.995f) return null

    val elapsedMillis = System.currentTimeMillis() - startedAt
    if (elapsedMillis < 500L) return null

    val estimatedTotalMillis = (elapsedMillis / safeProgress).toLong()
    val remainingMillis = (estimatedTotalMillis - elapsedMillis).coerceAtLeast(0L)
    if (remainingMillis < 1_000L) return "almost done"

    val remainingSeconds = (remainingMillis + 999L) / 1_000L
    return when {
        remainingSeconds < 60L -> "~${remainingSeconds}s left"
        remainingSeconds < 3_600L -> "~${(remainingSeconds + 30L) / 60L}m left"
        else -> "~${(remainingSeconds + 1_800L) / 3_600L}h left"
    }
}

@Composable
private fun ImportProgressChip(
    current: Int,
    total: Int,
    onCancel: () -> Unit
) {
    val progress = if (total > 0) current.toFloat() / total else 0f
    val percentage = (progress * 100).toInt()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .heightIn(min = 44.dp)
            .widthIn(min = 200.dp, max = 300.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            Text(
                text = stringResource(R.string.settings_import_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.settings_import_cancel_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SquigglyLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    trackColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val motionPreference = LocalAnimationPreference.current
    val phase = if (motionPreference == AnimationPreference.OFF) {
        0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "squiggly")
        val animatedPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = when (motionPreference) {
                        AnimationPreference.FULL -> 1500
                        AnimationPreference.REDUCED -> motionPreference.scaledDuration(3200)
                        AnimationPreference.OFF -> 0
                    },
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        animatedPhase
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val progressWidth = width * progress

        drawLine(
            color = trackColor,
            start = androidx.compose.ui.geometry.Offset(0f, height / 2),
            end = androidx.compose.ui.geometry.Offset(width, height / 2),
            strokeWidth = height,
            cap = StrokeCap.Round
        )

        if (progress > 0f) {
            val path = Path()
            val points = 100
            val waveLength = 40.dp.toPx()
            val amplitude = 3.dp.toPx()

            for (i in 0..points) {
                val x = (i.toFloat() / points.toFloat()) * progressWidth
                val relativeX = x / waveLength
                val y = (height / 2) + (amplitude * sin((relativeX * 2 * PI + phase).toDouble())).toFloat()

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
