package com.mj.yaja.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.BuildConfig
import com.mj.yaja.ui.theme.BodoniModaFamily
import java.time.LocalDate
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
        onNavigateToShortcodes: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToHelp: () -> Unit,
        datesWithEntries: Set<LocalDate> = emptySet(),
        onSurpriseMe: (LocalDate) -> Unit = {},
        showStatistics: Boolean = true,
        showLookbackInNavBar: Boolean = true,
        showStatisticsInNavBar: Boolean = false,
        content: @Composable () -> Unit
) {
        ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                        ModalDrawerSheet(modifier = Modifier.width(320.dp)) {
                                Spacer(Modifier.height(24.dp))
                                Column(modifier = Modifier.padding(horizontal = 28.dp)) {
                                        Text(
                                                "yaja",
                                                style =
                                                        MaterialTheme.typography.headlineMedium
                                                                .copy(
                                                                        fontFamily =
                                                                                BodoniModaFamily,
                                                                        fontWeight =
                                                                                FontWeight.ExtraBold
                                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                                "yet another journaling app",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                "v${BuildConfig.VERSION_NAME}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier =
                                                        Modifier.padding(top = 6.dp)
                                                                .background(
                                                                        MaterialTheme.colorScheme
                                                                                .primary.copy(
                                                                                alpha = 0.1f
                                                                        ),
                                                                        CircleShape
                                                                )
                                                                .padding(
                                                                        horizontal = 10.dp,
                                                                        vertical = 3.dp
                                                                )
                                        )
                                }
                                Spacer(Modifier.height(24.dp))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                Spacer(Modifier.height(16.dp))

                                NavigationDrawerItem(
                                        icon = {
                                                Icon(Icons.Rounded.Book, contentDescription = null)
                                        },
                                        label = { Text("Journal") },
                                        selected = currentRoute == "home",
                                        onClick = {
                                                scope.launch { drawerState.close() }
                                                onNavigateToJournal()
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                NavigationDrawerItem(
                                        icon = {
                                                Icon(
                                                        Icons.Rounded.CalendarMonth,
                                                        contentDescription = null
                                                )
                                        },
                                        label = { Text("Calendar") },
                                        selected = currentRoute == "calendar",
                                        onClick = {
                                                scope.launch { drawerState.close() }
                                                onNavigateToCalendar()
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                if (!showLookbackInNavBar) {
                                        Spacer(Modifier.height(8.dp))
                                        NavigationDrawerItem(
                                                icon = {
                                                        Icon(
                                                                Icons.Rounded.History,
                                                                contentDescription = null
                                                        )
                                                },
                                                label = { Text("Lookback") },
                                                selected = currentRoute == "lookback",
                                                onClick = {
                                                        scope.launch { drawerState.close() }
                                                        onNavigateToLookback()
                                                },
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                }
                                if (showStatistics && !showStatisticsInNavBar) {
                                        Spacer(Modifier.height(8.dp))
                                        NavigationDrawerItem(
                                                icon = {
                                                        Icon(
                                                                Icons.AutoMirrored.Rounded.TrendingUp,
                                                                contentDescription = null
                                                        )
                                                },
                                                label = { Text("Statistics") },
                                                selected = currentRoute == "statistics",
                                                onClick = {
                                                        scope.launch { drawerState.close() }
                                                        onNavigateToStatistics()
                                                },
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                }
                                NavigationDrawerItem(
                                        icon = {
                                                Icon(
                                                        Icons.Rounded.IntegrationInstructions,
                                                        contentDescription = null
                                                )
                                        },
                                        label = { Text("Shortcodes") },
                                        selected = currentRoute == "shortcodes",
                                        onClick = {
                                                scope.launch { drawerState.close() }
                                                onNavigateToShortcodes()
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                )


                                 // Surprise Me -- only visible after 50 past entries
                                 val pastEntryCount = datesWithEntries.count {
                                         it.isBefore(LocalDate.now())
                                 }
                                 if (pastEntryCount >= 50) {
                                         Spacer(Modifier.height(8.dp))
                                         NavigationDrawerItem(
                                                 icon = {
                                                         Icon(
                                                                 Icons.Rounded.AutoAwesome,
                                                                 contentDescription = null
                                                         )
                                                 },
                                                 label = { Text("Surprise Me") },
                                                 selected = false,
                                                 onClick = {
                                                         val candidates =
                                                                 datesWithEntries.filter {
                                                                         it.isBefore(LocalDate.now())
                                                                 }
                                                         if (candidates.isNotEmpty()) {
                                                                 scope.launch {
                                                                         drawerState.close()
                                                                 }
                                                                 onSurpriseMe(candidates.random())
                                                         }
                                                 },
                                                 modifier = Modifier.padding(horizontal = 12.dp)
                                         )
                                 }

                                Spacer(Modifier.weight(1f))

                                HorizontalDivider(
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 16.dp,
                                                        vertical = 8.dp
                                                )
                                )

                                NavigationDrawerItem(
                                        icon = {
                                                Icon(
                                                        Icons.Rounded.Settings,
                                                        contentDescription = null
                                                )
                                        },
                                        label = { Text("Settings") },
                                        selected = currentRoute == "settings",
                                        onClick = {
                                                scope.launch { drawerState.close() }
                                                onNavigateToSettings()
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                NavigationDrawerItem(
                                        icon = {
                                                Icon(
                                                        Icons.AutoMirrored.Rounded.Help,
                                                        contentDescription = null
                                                )
                                        },
                                        label = { Text("Help & About") },
                                        selected = currentRoute == "help",
                                        onClick = {
                                                scope.launch { drawerState.close() }
                                                onNavigateToHelp()
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                        }
                }
        ) { content() }
}

@Composable
fun AnimatedMenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
        val menuInteractionSource = remember { MutableInteractionSource() }
        val isMenuPressed by menuInteractionSource.collectIsPressedAsState()
        val menuScale by
                animateFloatAsState(
                        targetValue = if (isMenuPressed) 0.85f else 1f,
                        animationSpec =
                                spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                        label = "MenuScale"
                )

        Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape,
                modifier =
                        modifier.size(48.dp).graphicsLayer {
                                scaleX = menuScale
                                scaleY = menuScale
                        },
                interactionSource = menuInteractionSource,
                onClick = onClick
        ) {
                Box(contentAlignment = Alignment.Center) {
                        Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
        }
}
