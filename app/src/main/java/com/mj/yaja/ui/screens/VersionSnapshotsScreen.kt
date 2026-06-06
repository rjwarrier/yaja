package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionSnapshotsScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snapshots by viewModel.versionHistorySnapshots.collectAsState()
    val restoreInProgress by viewModel.versionHistoryRestoreInProgress.collectAsState()
    val selectedDate = uiState.selectedDate
    var previewSnapshot by remember { mutableStateOf<JournalViewModel.VersionHistorySnapshotUi?>(null) }

    LaunchedEffect(selectedDate) {
        viewModel.loadVersionHistorySnapshots(selectedDate)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Day Versions", color = MaterialTheme.colorScheme.primary) },
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
                text = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Open an earlier saved version for this day, inspect it, and restore it when needed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VersionSnapshotsCard(
                selectedDateLabel = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                snapshots = snapshots,
                onPreview = { previewSnapshot = it }
            )
        }
    }

    previewSnapshot?.let { snapshot ->
        SnapshotPreviewDialog(
            snapshot = snapshot,
            restoreInProgress = restoreInProgress,
            onDismiss = { previewSnapshot = null },
            onRestore = {
                viewModel.restoreVersionHistorySnapshot(snapshot.id, selectedDate)
                previewSnapshot = null
            }
        )
    }
}
