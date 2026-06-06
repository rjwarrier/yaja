package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.ui.viewmodel.JournalViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RebuildCacheScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    var showFullRebuildWarning by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fileManager = viewModel.fileManager
    
    val cachedDays = remember(uiState.isLoading) { fileManager.getCachedDaysCount() }
    val dbSize = remember(uiState.isLoading) { fileManager.getDatabaseSize() }
    val cacheAgeMs = remember(uiState.isLoading) { fileManager.getJournalCacheAgeMillis() }

    val formattedDbSize = remember(dbSize) {
        val kb = dbSize / 1024.0
        if (kb < 1024.0) {
            String.format(java.util.Locale.US, "%.1f KB", kb)
        } else {
            String.format(java.util.Locale.US, "%.2f MB", kb / 1024.0)
        }
    }

    val formattedAge = remember(cacheAgeMs) {
        if (cacheAgeMs == null) "Never"
        else {
            val minutes = cacheAgeMs / 60000L
            when {
                minutes < 1 -> "Just now"
                minutes == 1L -> "1 minute ago"
                minutes < 60 -> "$minutes minutes ago"
                else -> {
                    val hours = minutes / 60
                    if (hours == 1L) "1 hour ago" else "$hours hours ago"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rebuild Tools", color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Use focused rebuilds first. Rebuilding the database is only for storage changes, database corruption, or index mismatch.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DatabaseCacheStatusCard(
                cachedDays = cachedDays,
                formattedDbSize = formattedDbSize,
                lastUpdated = formattedAge
            )

            DataArchitectureInfoCard()

            RebuildActionCard(
                icon = Icons.Rounded.Checklist,
                title = "Rebuild Todo Index",
                subtitle = "Refresh todos from journal files and update Todo screen/widget. Best when todos look stale.",
                buttonText = "Rebuild Todos",
                onClick = { viewModel.refreshTodos(forceRebuild = true) }
            )

            RebuildActionCard(
                icon = Icons.Rounded.Groups,
                title = "Rebuild People & Places",
                subtitle = "Refresh keyword aliases, co-mentions, trends, and relationship data.",
                buttonText = "Rebuild People & Places",
                onClick = { viewModel.rebuildKeywordIndex(immediate = true) }
            )

            RebuildActionCard(
                icon = Icons.Rounded.Storage,
                title = "Full Database Rebuild",
                subtitle = "Recompile database cache from markdown files. Re-indexes entries, counts, todos, People & Places, and widgets from scratch.",
                buttonText = "Rebuild Database",
                danger = true,
                onClick = { showFullRebuildWarning = true }
            )

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    if (showFullRebuildWarning) {
        AlertDialog(
            onDismissRequest = { showFullRebuildWarning = false },
            icon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
            title = { Text("Rebuild database index") },
            text = {
                Text(
                    "This will clear and re-index the entire Room SQLite database from your Markdown files. For large journals, this may take a few minutes."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFullRebuildWarning = false
                        viewModel.refreshCache()
                    }
                ) {
                    Text("Start Rebuild")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullRebuildWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DatabaseCacheStatusCard(
    cachedDays: Int,
    formattedDbSize: String,
    lastUpdated: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Database Index Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusRow(label = "Storage Engine", value = "Room SQLite Database")
                StatusRow(label = "Indexed Days", value = "$cachedDays days indexed")
                StatusRow(label = "Database File Size", value = formattedDbSize)
                StatusRow(label = "Last Index Sync", value = lastUpdated)
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RebuildActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onClick) {
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
private fun DataArchitectureInfoCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "On-Device Storage Transparency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Yaja uses a secure local storage model. Your personal journal data remains local and never leaves your device at any time:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ArchitectureDetailRow(
                title = "Source of Truth (Markdown Files)",
                desc = "All entries, checklist items (todos), timeline events, stars, and tags are permanently saved in plaintext Markdown (.md) files in your selected local journal folder. Your data is 100% owned by you and resides entirely on your device."
            )

            Spacer(modifier = Modifier.height(10.dp))

            ArchitectureDetailRow(
                title = "Read Cache & Index (SQLite Database)",
                desc = "To enable instant screen loading, calendar highlights, keywords, and statistics without scanning the filesystem on every scroll, Yaja parses your Markdown files and indexes them in a local Room SQLite database on your device."
            )

            Spacer(modifier = Modifier.height(10.dp))

            ArchitectureDetailRow(
                title = "100% Local & Private",
                desc = "Yaja does not have any cloud databases, remote backup servers, or telemetry tracking your text. All journal entries and parsed data remain strictly on your physical device and never leave it at any time."
            )
        }
    }
}

@Composable
private fun ArchitectureDetailRow(title: String, desc: String) {
    Column {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
        )
    }
}
