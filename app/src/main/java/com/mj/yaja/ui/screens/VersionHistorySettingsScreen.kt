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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val SNAPSHOT_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).withZone(ZoneId.systemDefault())
private val SNAPSHOT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()).withZone(ZoneId.systemDefault())

private val snapshotTimeMarkerRegex = Regex("^<!--time:(\\d{2}:\\d{2})(?:, added on (.*?))?-->\\n?")
private val snapshotMetadataLineRegex = Regex("^<!--[^\\n]*-->\\n?")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistorySettingsScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    val enabled by viewModel.versionHistoryEnabled.collectAsStateWithLifecycle()
    val maxVersions by viewModel.versionHistoryMaxVersions.collectAsStateWithLifecycle()
    val retentionDays by viewModel.versionHistoryRetentionDays.collectAsStateWithLifecycle()
    val selectedDate by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshots by viewModel.versionHistorySnapshots.collectAsStateWithLifecycle()
    val restoreInProgress by viewModel.versionHistoryRestoreInProgress.collectAsStateWithLifecycle()
    var previewSnapshot by remember { mutableStateOf<JournalViewModel.VersionHistorySnapshotUi?>(null) }

    LaunchedEffect(selectedDate.selectedDate) {
        viewModel.loadVersionHistorySnapshots(selectedDate.selectedDate)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.version_history_title),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
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
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.version_history_keep_copies_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.version_history_keep_copies_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { viewModel.setVersionHistoryEnabled(it) }
                    )
                }
            }

            HistorySliderCard(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.version_history_versions_kept_title),
                subtitle = pluralStringResource(
                    R.plurals.version_history_versions_kept_subtitle,
                    maxVersions,
                    maxVersions
                ),
                value = maxVersions.toFloat(),
                valueRange = 1f..10f,
                steps = 8,
                valueLabel = "$maxVersions",
                onValueChange = { viewModel.setVersionHistoryMaxVersions(it.roundToInt()) }
            )

            HistorySliderCard(
                icon = Icons.Rounded.Storage,
                title = stringResource(R.string.version_history_retention_title),
                subtitle = pluralStringResource(
                    R.plurals.version_history_retention_subtitle,
                    retentionDays,
                    retentionDays
                ),
                value = retentionDays.toFloat(),
                valueRange = 2f..30f,
                steps = 27,
                valueLabel = "$retentionDays days",
                onValueChange = { viewModel.setVersionHistoryRetentionDays(it.roundToInt()) }
            )

            Text(
                text = stringResource(R.string.version_history_notes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            VersionSnapshotsCard(
                selectedDateLabel = selectedDate.selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                snapshots = snapshots,
                onPreview = { previewSnapshot = it }
            )

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    previewSnapshot?.let { snapshot ->
        SnapshotPreviewDialog(
            snapshot = snapshot,
            restoreInProgress = restoreInProgress,
            onDismiss = { previewSnapshot = null },
            onRestore = {
                viewModel.restoreVersionHistorySnapshot(
                    snapshotId = snapshot.id,
                    date = selectedDate.selectedDate
                )
                previewSnapshot = null
            }
        )
    }
}

@Composable
private fun HistorySliderCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}

@Composable
fun VersionSnapshotsCard(
    selectedDateLabel: String,
    snapshots: List<JournalViewModel.VersionHistorySnapshotUi>,
    onPreview: (JournalViewModel.VersionHistorySnapshotUi) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.version_history_snapshots_for, selectedDateLabel),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.version_history_snapshots_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (snapshots.isEmpty()) {
                Text(
                    text = stringResource(R.string.version_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                snapshots.forEach { snapshot ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = SNAPSHOT_TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(snapshot.createdAt)),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = snapshot.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(onClick = { onPreview(snapshot) }) {
                                Text(stringResource(R.string.action_view))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SnapshotPreviewDialog(
    snapshot: JournalViewModel.VersionHistorySnapshotUi,
    restoreInProgress: Boolean,
    onDismiss: () -> Unit,
    onRestore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Rounded.History, contentDescription = null)
        },
        title = {
            Text(SNAPSHOT_DATE_FORMATTER.format(Instant.ofEpochMilli(snapshot.createdAt)))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = snapshot.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatSnapshotPreviewContent(snapshot.content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRestore,
                enabled = !restoreInProgress
            ) {
                Text(stringResource(R.string.action_restore))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !restoreInProgress
            ) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

private fun formatSnapshotPreviewContent(content: String): String {
    val visibleLines = mutableListOf<String>()
    var inFrontmatter = false
    var headingSkipped = false

    content.lines().forEachIndexed { index, rawLine ->
        val line = rawLine.trimEnd()
        when {
            index == 0 && line.trim() == "---" -> inFrontmatter = true
            inFrontmatter && line.trim() == "---" -> inFrontmatter = false
            inFrontmatter -> Unit
            !headingSkipped && line.startsWith("# ") -> headingSkipped = true
            else -> visibleLines += line
        }
    }

    val blocks = mutableListOf<String>()
    val current = mutableListOf<String>()
    visibleLines.forEach { line ->
        when {
            line.startsWith("- ") -> {
                if (current.isNotEmpty()) {
                    blocks += current.joinToString("\n")
                    current.clear()
                }
                current += line.removePrefix("- ")
            }
            line.startsWith("  ") && current.isNotEmpty() -> current += line.removePrefix("  ")
            line.isBlank() && current.isNotEmpty() -> current += ""
            line.isNotBlank() && current.isEmpty() -> current += line
        }
    }
    if (current.isNotEmpty()) {
        blocks += current.joinToString("\n")
    }

    val formatted = blocks.ifEmpty { listOf(visibleLines.joinToString("\n")) }.map { block ->
        block
            .lineSequence()
            .filterNot { line ->
                val trimmed = line.trim()
                trimmed.startsWith("<!--time:") || snapshotMetadataLineRegex.matches("${trimmed}\n")
            }
            .joinToString("\n")
            .trim()
    }

    return formatted.filter { it.isNotBlank() }.joinToString("\n\n").trim()
}
