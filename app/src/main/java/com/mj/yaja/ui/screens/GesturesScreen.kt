package com.mj.yaja.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.SwipeDirection
import com.mj.yaja.ui.viewmodel.JournalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesturesScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    val swipeToDeleteEnabled by viewModel.swipeToDeleteEnabled.collectAsState()
    val swipeDeleteDirection by viewModel.swipeDeleteDirection.collectAsState()
    val enableDragAndDrop by viewModel.enableDragAndDrop.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Gestures",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // ── Gesture Section ──
                SettingsSectionHeader(
                    icon = Icons.Rounded.Fingerprint,
                    title = "Gestures"
                )

                Spacer(modifier = Modifier.height(12.dp))

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Swipe to Delete",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Enable delete gesture on entries",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = swipeToDeleteEnabled,
                                onCheckedChange = {
                                    viewModel.setSwipeToDeleteEnabled(it)
                                }
                            )
                        }

                        // Swipe direction sub-option
                        AnimatedVisibility(visible = swipeToDeleteEnabled) {
                            Column {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = "Swipe Direction",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Which side triggers delete",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.shapes.small
                                            )
                                            .padding(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(
                                            SwipeDirection.END_TO_START to "← Left",
                                            SwipeDirection.START_TO_END to "Right →"
                                        ).forEach { (dir, label) ->
                                            val isSelected = swipeDeleteDirection == dir
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (isSelected)
                                                            MaterialTheme.colorScheme.primaryContainer
                                                        else androidx.compose.ui.graphics.Color.Transparent,
                                                        shape = MaterialTheme.shapes.small
                                                    )
                                                    .clickable {
                                                        viewModel.setSwipeDeleteDirection(dir)
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(label, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Drag and drop
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Drag-to-Reorder",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Long-press entries to rearrange them",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = enableDragAndDrop,
                                onCheckedChange = {
                                    viewModel.setEnableDragAndDrop(it)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ElevatedCard(
    modifier: Modifier = Modifier,
    colors: androidx.compose.material3.CardColors = androidx.compose.material3.CardDefaults.elevatedCardColors(),
    elevation: androidx.compose.material3.CardElevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(),
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.ElevatedCard(
        modifier = modifier,
        colors = colors,
        elevation = elevation,
        content = content
    )
}
