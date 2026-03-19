package com.mj.yaja.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.BuildConfig
import com.mj.yaja.R
import com.mj.yaja.ui.theme.BodoniModaFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
        onOpenDrawer: () -> Unit,
        onNavigateBack: () -> Unit,
        onNavigateToJournal: () -> Unit,
        onNavigateToCalendar: () -> Unit,
        onNavigateToLookback: () -> Unit,
        onNavigateToShortcodes: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToHelp: () -> Unit
) {
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { visible = true }

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                "Help & About",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                },
                                navigationIcon = {
                                        com.mj.yaja.ui.components.AnimatedMenuButton(
                                                onClick = onOpenDrawer,
                                                modifier = Modifier.padding(start = 8.dp)
                                        )
                                },
                                colors =
                                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.background,
                                                titleContentColor =
                                                        MaterialTheme.colorScheme.primary,
                                                navigationIconContentColor =
                                                        MaterialTheme.colorScheme.onSurface,
                                                actionIconContentColor =
                                                        MaterialTheme.colorScheme.onSurface
                                        )
                        )
                },
                containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
                LazyColumn(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(paddingValues)
                                        .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
                ) {
                        // ── Header ──
                        item {
                                AnimatedVisibility(
                                        visible = visible,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 2 },
                                                        animationSpec = tween(350, 50)
                                                ) + fadeIn(animationSpec = tween(350, 50))
                                ) {
                                        Column {
                                                Text(
                                                        text = "Welcome to yaja",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .headlineMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                                Text(
                                                        text =
                                                                "Yaja (yet another journaling app) is a journaling app that stays out of your way. No streaks, no reminders, no features fighting for your attention. Just a blank page when you need one. Write down a passing thought or what you had for lunch; it doesn't have to be profound. Your words can just exist.",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                }
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        // ── Help Cards — staggered ──
                        item {
                                AnimatedVisibility(
                                        visible = visible,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 2 },
                                                        animationSpec = tween(350, 100)
                                                ) + fadeIn(animationSpec = tween(350, 100))
                                ) {
                                        HelpCard(
                                                icon = Icons.Rounded.Edit,
                                                title = "Writing Entries",
                                                content =
                                                        "• Tap the floating '+' button or tap anywhere on the Home screen to add an entry for the selected date.\n" +
                                                                "• You can format your text! Use **text** for bold, or *text* for italics.\n" +
                                                                "• Edit existing entries by tapping on them."
                                        )
                                }
                        }

                        item {
                                AnimatedVisibility(
                                        visible = visible,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 2 },
                                                        animationSpec = tween(350, 150)
                                                ) + fadeIn(animationSpec = tween(350, 150))
                                ) {
                                        HelpCard(
                                                icon = Icons.Rounded.Swipe,
                                                title = "Navigation Gestures",
                                                content =
                                                        "• Swipe Left on the home screen to go to the Next date.\n" +
                                                                "• Swipe Right on the home screen to go to the Previous date.\n" +
                                                                "• Swipe Down on the home screen to force a cache Sync.\n" +
                                                                "• Swipe an entry left or right to Delete it (Undo is available for a few seconds)."
                                        )
                                }
                        }

                        item {
                                AnimatedVisibility(
                                        visible = visible,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 2 },
                                                        animationSpec = tween(350, 200)
                                                ) + fadeIn(animationSpec = tween(350, 200))
                                ) {
                                        HelpCard(
                                                icon = Icons.Rounded.Bolt,
                                                title = "Smart Shortcodes",
                                                content =
                                                        "• Create your own shortcuts in Settings > Shortcodes to quickly insert frequently used text.\n" +
                                                                "• Type '@x' anywhere in a line to toggle it as a Todo item ( [ ] ↔ [x] ).\n" +
                                                                "• Use dynamic placeholders like {{today:dd-MMM}} or {{now:HH:mm}} for live timestamps.\n" +
                                                                "• Export/Import Shortcodes in simple CSV Format! Your data backup includes Shortcodes too!"
                                        )
                                }
                        }

                        item {
                                AnimatedVisibility(
                                        visible = visible,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 2 },
                                                        animationSpec = tween(350, 250)
                                                ) + fadeIn(animationSpec = tween(350, 250))
                                ) {
                                        HelpCard(
                                                icon = Icons.Rounded.FolderZip,
                                                title = "Data & Storage",
                                                content =
                                                        "• Your journal entries are saved as standard Markdown (.md) files organized by Year and Month.\n" +
                                                                "• You can change the Storage Folder in Settings if you want to save them on an SD card or shared directory.\n" +
                                                                "• Create Backup Zips from Settings to safeguard your memories."
                                        )
                                }
                        }

                        item {
                                AnimatedVisibility(
                                        visible = visible,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 2 },
                                                        animationSpec = tween(350, 300)
                                                ) + fadeIn(animationSpec = tween(350, 300))
                                ) {
                                        HelpCard(
                                                icon = Icons.Rounded.Security,
                                                title = "Privacy & Security",
                                                content =
                                                        "• Journal uses a secure, hashed PIN lock to keep your entries private.\n" +
                                                                "• Your data never leaves your device unless you manually back it up or sync it to a cloud provider folder."
                                        )
                                }
                        }

                        item {
                                AnimatedVisibility(
                                        visible = visible,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 2 },
                                                        animationSpec = tween(350, 355)
                                                ) + fadeIn(animationSpec = tween(350, 355))
                                ) {
                                        HelpCard(
                                                icon = Icons.Rounded.AutoAwesome,
                                                title = "Lookback & Highlights",
                                                content =
                                                        "• Tap the 'Star' icon on any date to Favorite it.\n" +
                                                                "• Head to the 'Lookback' tab to see what you wrote on this exact day in previous years and to browse your Favorited Highlights."
                                        )
                                }
                        }

                        item {
                                AnimatedVisibility(
                                        visible = visible,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 2 },
                                                        animationSpec = tween(350, 400)
                                                ) + fadeIn(animationSpec = tween(350, 400))
                                ) {
                                        HelpCard(
                                                icon = Icons.Rounded.Widgets,
                                                title = "Quick Capture Widget",
                                                content =
                                                        "• Add the 'yaja' widget to your home screen for instant entry capture.\n" +
                                                                "• Long-press the widget to resize it into different shapes and positions.\n" +
                                                                "• Customize the corner radius and label visibility in Settings > Widget Preferences."
                                        )
                                }
                        }

                        item { Spacer(modifier = Modifier.height(24.dp)) }

                        // ── Branding footer — scale + fade pop-in ──
                        item {
                                AnimatedVisibility(
                                        visible = visible,
                                        enter =
                                                slideInVertically(
                                                        initialOffsetY = { it / 3 },
                                                        animationSpec = tween(450, 450)
                                                ) + fadeIn(animationSpec = tween(450, 450))
                                ) {
                                        Column(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(bottom = 24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                                Surface(
                                                        shape = CircleShape,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer,
                                                        modifier = Modifier.size(64.dp)
                                                ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                        painter =
                                                                                painterResource(
                                                                                        id =
                                                                                                R.drawable
                                                                                                        .ic_launcher_foreground
                                                                                ),
                                                                        contentDescription = null,
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        40.dp
                                                                                ),
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onPrimaryContainer
                                                                )
                                                        }
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                        text = "yaja",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .headlineSmall.copy(
                                                                        fontFamily =
                                                                                BodoniModaFamily,
                                                                        fontWeight =
                                                                                FontWeight.ExtraBold
                                                                ),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                        text = "yet another journaling app",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                                Text(
                                                        text = "v ${BuildConfig.VERSION_NAME}",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .labelMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier =
                                                                Modifier.padding(top = 8.dp)
                                                                        .background(
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.1f
                                                                                        ),
                                                                                CircleShape
                                                                        )
                                                                        .padding(
                                                                                horizontal = 12.dp,
                                                                                vertical = 4.dp
                                                                        )
                                                )
                                                Spacer(modifier = Modifier.height(24.dp))
                                                Text(
                                                        text = "from the labs of RJ",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant.copy(
                                                                        alpha = 0.5f
                                                                ),
                                                        letterSpacing = 1.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                        text = "🇮🇳",
                                                        style = MaterialTheme.typography.labelMedium
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
fun HelpCard(icon: ImageVector, title: String, content: String) {
        Card(
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                modifier = Modifier.fillMaxWidth()
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                                Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                ) {
                                        Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                        imageVector = icon,
                                                        contentDescription = title,
                                                        tint =
                                                                MaterialTheme.colorScheme
                                                                        .onPrimaryContainer,
                                                        modifier = Modifier.size(24.dp)
                                                )
                                        }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                )
                        }
                        Text(
                                text = content,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
        }
}
