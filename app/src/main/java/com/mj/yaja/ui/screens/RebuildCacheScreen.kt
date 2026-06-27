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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
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

    val formattedAge =
        if (cacheAgeMs == null) stringResource(R.string.rebuild_database_age_never)
        else {
            val minutes = cacheAgeMs / 60000L
            when {
                minutes < 1 -> stringResource(R.string.rebuild_database_age_just_now)
                minutes == 1L -> stringResource(R.string.rebuild_database_age_one_minute)
                minutes < 60 -> stringResource(R.string.rebuild_database_age_minutes, minutes)
                else -> {
                    val hours = minutes / 60
                    if (hours == 1L) {
                        stringResource(R.string.rebuild_database_age_one_hour)
                    } else {
                        stringResource(R.string.rebuild_database_age_hours, hours)
                    }
                }
            }
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.rebuild_tools_title), color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.rebuild_tools_back)
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
                text = stringResource(R.string.rebuild_tools_intro),
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
                title = stringResource(R.string.rebuild_todo_index_title),
                subtitle = stringResource(R.string.rebuild_todo_index_subtitle),
                buttonText = stringResource(R.string.rebuild_todo_index_button),
                onClick = { viewModel.refreshTodos(forceRebuild = true) }
            )

            RebuildActionCard(
                icon = Icons.Rounded.Groups,
                title = stringResource(R.string.rebuild_people_places_title),
                subtitle = stringResource(R.string.rebuild_people_places_subtitle),
                buttonText = stringResource(R.string.rebuild_people_places_button),
                onClick = { viewModel.rebuildKeywordIndex(immediate = true) }
            )

            RebuildActionCard(
                icon = Icons.Rounded.Storage,
                title = stringResource(R.string.rebuild_database_title),
                subtitle = stringResource(R.string.rebuild_database_subtitle),
                buttonText = stringResource(R.string.rebuild_database_button),
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
            title = { Text(stringResource(R.string.rebuild_database_confirm_title)) },
            text = {
                Text(
                    stringResource(R.string.rebuild_database_confirm_message)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFullRebuildWarning = false
                        viewModel.refreshCache()
                    }
                ) {
                    Text(stringResource(R.string.rebuild_database_confirm_start))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullRebuildWarning = false }) {
                    Text(stringResource(R.string.action_cancel))
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
                    text = stringResource(R.string.rebuild_database_status_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusRow(label = stringResource(R.string.rebuild_database_storage_engine), value = stringResource(R.string.rebuild_database_storage_engine_value))
                StatusRow(label = stringResource(R.string.rebuild_database_indexed_days), value = stringResource(R.string.rebuild_database_indexed_days_value, cachedDays))
                StatusRow(label = stringResource(R.string.rebuild_database_file_size), value = formattedDbSize)
                StatusRow(label = stringResource(R.string.rebuild_database_last_sync), value = lastUpdated)
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
                    text = stringResource(R.string.rebuild_cache_storage_transparency_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.rebuild_cache_storage_transparency_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ArchitectureDetailRow(
                title = stringResource(R.string.rebuild_cache_source_of_truth_title),
                desc = stringResource(R.string.rebuild_cache_source_of_truth_desc)
            )

            Spacer(modifier = Modifier.height(10.dp))

            ArchitectureDetailRow(
                title = stringResource(R.string.rebuild_cache_read_cache_title),
                desc = stringResource(R.string.rebuild_cache_read_cache_desc)
            )

            Spacer(modifier = Modifier.height(10.dp))

            ArchitectureDetailRow(
                title = stringResource(R.string.rebuild_cache_local_private_title),
                desc = stringResource(R.string.rebuild_cache_local_private_desc)
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
