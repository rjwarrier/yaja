package com.mj.yaja.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.mj.yaja.BuildConfig
import com.mj.yaja.R
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.floatSpring
import com.mj.yaja.ui.design.scaledDelay
import com.mj.yaja.ui.design.scaledDuration
import com.mj.yaja.ui.theme.BodoniModaFamily
import com.mj.yaja.ui.theme.LibreBaskervilleFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppNavigationDrawer(
        drawerState: DrawerState,
        scope: CoroutineScope,
        currentRoute: String,
        onNavigateToJournal: () -> Unit,
        onNavigateToCalendar: () -> Unit,
        onNavigateToLookback: () -> Unit,
        onNavigateToStatistics: () -> Unit = {},
        onNavigateToKeywords: () -> Unit,
        onNavigateToShortcodes: () -> Unit,
        onNavigateToTodos: () -> Unit,
        onNavigateToRebuildCache: () -> Unit,
        onBackupData: () -> Unit,
        showBackupReminder: Boolean = false,
        onNavigateToSettings: () -> Unit,
        onNavigateToHelp: () -> Unit,
        onNavigateToShare: () -> Unit,
        showStatistics: Boolean = true,
        showLookbackInNavBar: Boolean = true,
        showKeywordsInNavBar: Boolean = false,
        showTodosInNavBar: Boolean = false,
        showStatisticsInNavBar: Boolean = false,
        syncProgress: Float? = null,
        backgroundWorkLabel: String? = null,
        content: @Composable () -> Unit
) {
        var showLogoEasterEgg by remember { mutableStateOf(false) }
        val uriHandler = LocalUriHandler.current
        var syncStartedAtMillis by remember { mutableStateOf<Long?>(null) }
        LaunchedEffect(syncProgress == null) {
                if (syncProgress == null) {
                        syncStartedAtMillis = null
                } else if (syncStartedAtMillis == null) {
                        syncStartedAtMillis = System.currentTimeMillis()
                }
        }
        Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                        val drawerScrollState = rememberScrollState()
                        ModalDrawerSheet(
                                modifier =
                                        Modifier.widthIn(max = 320.dp)
                                                .fillMaxHeight()
                                                .navigationBarsPadding()
                        ) {
                                BoxWithConstraints(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .fillMaxHeight()
                                                        .padding(bottom = 16.dp)
                                ) {
                                        var topSectionHeightPx by remember { mutableStateOf(0) }
                                        var bottomSectionHeightPx by remember { mutableStateOf(0) }
                                        val density = LocalDensity.current
                                        val spacerHeight =
                                                with(density) {
                                                        (constraints.maxHeight -
                                                                topSectionHeightPx -
                                                                bottomSectionHeightPx)
                                                                .coerceAtLeast(0)
                                                                .toDp()
                                                }

                                        Column(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .verticalScroll(drawerScrollState)
                                        ) {
                                                Column(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .onSizeChanged {
                                                                                topSectionHeightPx = it.height
                                                                        }
                                                ) {
                                                        Spacer(Modifier.height(24.dp))
                                                        DrawerBrandHeader(
                                                                versionName = BuildConfig.VERSION_NAME,
                                                                onLogoClick = { showLogoEasterEgg = true },
                                                                onOpenPlayStore = {
                                                                        uriHandler.openUri(
                                                                                "https://play.google.com/store/apps/details?id=com.mj.yaja"
                                                                        )
                                                                },
                                                                onOpenWebsite = {
                                                                        uriHandler.openUri("https://ranjithj.in/yaja/")
                                                                },
                                                                onShareApp = {
                                                                        scope.launch { drawerState.close() }
                                                                        onNavigateToShare()
                                                                }
                                                        )
                                                        Spacer(Modifier.height(24.dp))
                                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                                        Spacer(Modifier.height(16.dp))
                                                        DrawerPrimarySection(
                                                                currentRoute = currentRoute,
                                                                syncProgress = syncProgress,
                                                                backgroundWorkLabel = backgroundWorkLabel,
                                                                syncStartedAtMillis = syncStartedAtMillis,
                                                                onNavigateToJournal = {
                                                                        scope.launch { drawerState.close() }
                                                                        onNavigateToJournal()
                                                                },
                                                                onNavigateToCalendar = {
                                                                        scope.launch { drawerState.close() }
                                                                        onNavigateToCalendar()
                                                                }
                                                        )
                                                        DrawerPowerFeaturesSection(
                                                                currentRoute = currentRoute,
                                                                showLookbackInNavBar = showLookbackInNavBar,
                                                                showKeywordsInNavBar = showKeywordsInNavBar,
                                                                showTodosInNavBar = showTodosInNavBar,
                                                                showStatistics = showStatistics,
                                                                showStatisticsInNavBar = showStatisticsInNavBar,
                                                                onNavigateToLookback = {
                                                                        scope.launch { drawerState.close() }
                                                                        onNavigateToLookback()
                                                                },
                                                                onNavigateToKeywords = {
                                                                        scope.launch { drawerState.close() }
                                                                        onNavigateToKeywords()
                                                                },
                                                                onNavigateToStatistics = {
                                                                        scope.launch { drawerState.close() }
                                                                        onNavigateToStatistics()
                                                                },
                                                                onNavigateToTodos = {
                                                                        scope.launch { drawerState.close() }
                                                                        onNavigateToTodos()
                                                                }
                                                        )
                                                        DrawerWritingToolsSection(
                                                                currentRoute = currentRoute,
                                                                onNavigateToShortcodes = {
                                                                        scope.launch { drawerState.close() }
                                                                        onNavigateToShortcodes()
                                                                }
                                                        )
                                                        Spacer(Modifier.height(12.dp))
                                                }
                                                Spacer(Modifier.height(spacerHeight))
                                                Column(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .onSizeChanged {
                                                                                bottomSectionHeightPx = it.height
                                                                        }
                                                ) {
                                                        DrawerAdvancedAndAppSection(
                                                                currentRoute = currentRoute,
                                                                showBackupReminder = showBackupReminder,
                                                                syncProgress = syncProgress,
                                                                backgroundWorkLabel = backgroundWorkLabel,
                                                                syncStartedAtMillis = syncStartedAtMillis,
                                                                onNavigateToRebuildCache = {
                                                                        scope.launch { drawerState.close() }
                                                                        onNavigateToRebuildCache()
                                                                },
                                                                onBackupData = {
                                                                        scope.launch { drawerState.close() }
                                                                        onBackupData()
                                                                },
                                                                onNavigateToSettings = {
                                                                        scope.launch { drawerState.close() }
                                                                        onNavigateToSettings()
                                                                },
                                                                onNavigateToHelp = {
                                                                        scope.launch { drawerState.close() }
                                                                        onNavigateToHelp()
                                                                }
                                                        )
                                                }
                                        }
                                }
                        }
                },
                content = content
        )
                if (showLogoEasterEgg) {
                        YajaLogoEasterEgg(onClose = { showLogoEasterEgg = false })
                }
        }
}
