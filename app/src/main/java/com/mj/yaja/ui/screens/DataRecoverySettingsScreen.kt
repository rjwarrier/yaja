package com.mj.yaja.ui.screens

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.yaja.R
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataRecoverySettingsScreen(
        viewModel: JournalViewModel,
        onNavigateBack: () -> Unit,
        onNavigateToRebuildCache: () -> Unit,
        onNavigateToVersionHistory: () -> Unit
) {
        val context = LocalContext.current
        val storageUriString by viewModel.storageUri.collectAsStateWithLifecycle()
        val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsStateWithLifecycle()
        val backupReminderDays by viewModel.backupReminderDays.collectAsStateWithLifecycle()
        val swipeToSyncEnabled by viewModel.swipeToSyncEnabled.collectAsStateWithLifecycle()
        val largeJournalSafeMode by viewModel.largeJournalSafeMode.collectAsStateWithLifecycle()
        val showOnboardingNextLaunch by viewModel.showOnboardingNextLaunch.collectAsStateWithLifecycle()
        val versionHistoryEnabled by viewModel.versionHistoryEnabled.collectAsStateWithLifecycle()
        val importState by viewModel.importState.collectAsStateWithLifecycle()
        val restoreSummary by viewModel.restoreSummary.collectAsStateWithLifecycle()

        val formattedBackupDate =
                remember(lastBackupTimestamp) {
                        if (lastBackupTimestamp == 0L) {
                                "Never"
                        } else {
                                val instant = Instant.ofEpochMilli(lastBackupTimestamp)
                                val dateTime =
                                        LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                                val formatter =
                                        DateTimeFormatter.ofPattern("dd-MMM-yy HH:mm 'hrs'")
                                dateTime.format(formatter)
                        }
                }

        var pendingUriString by remember { mutableStateOf<String?>(null) }
        var showDialog by remember { mutableStateOf(false) }
        val confirmLocationChange = { uriString: String? ->
                pendingUriString = uriString
                showDialog = true
        }

        val storageLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                        if (uri != null) {
                                val takeFlags =
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                try {
                                        context.contentResolver.takePersistableUriPermission(
                                                uri,
                                                takeFlags
                                        )
                                } catch (e: SecurityException) {
                                        Log.e(
                                                "DataRecoverySettings",
                                                "Failed to take persistable URI permission",
                                                e
                                        )
                                }
                                val newUriString = uri.toString()
                                if (newUriString != storageUriString) {
                                        confirmLocationChange(newUriString)
                                }
                        }
                }

        val dayOneLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                        if (uri != null) viewModel.importDayOneFile(uri, context)
                }
        val journalisticLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                        if (uri != null) viewModel.importJournalisticFile(uri, context)
                }
        val markdownFolderImportLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                        if (uri != null) {
                                try {
                                        context.contentResolver.takePersistableUriPermission(
                                                uri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                } catch (e: SecurityException) {
                                        Log.e(
                                                "DataRecoverySettings",
                                                "Failed to take persistable URI permission for markdown folder import",
                                                e
                                        )
                                }
                                viewModel.importMarkdownFolder(uri, context)
                        }
                }
        val obsidianExportLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                        if (uri != null) {
                                try {
                                        context.contentResolver.takePersistableUriPermission(
                                                uri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        )
                                } catch (e: SecurityException) {
                                        Log.e(
                                                "DataRecoverySettings",
                                                "Failed to take persistable URI permission for Obsidian export",
                                                e
                                        )
                                }
                                viewModel.exportObsidianVault(uri, context)
                        }
                }
        val backupRestoreLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                        if (uri != null) viewModel.restoreBackupZip(uri, context)
                }

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                stringResource(R.string.settings_data_recovery_title),
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                },
                                navigationIcon = {
                                        IconButton(onClick = onNavigateBack) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Rounded.ArrowBack,
                                                        contentDescription =
                                                                stringResource(R.string.action_back)
                                                )
                                        }
                                },
                                colors =
                                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.background
                                        )
                        )
                },
                containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
                AppScreenReveal(visible = true, modifier = Modifier.fillMaxSize()) {
                        Column(
                                modifier =
                                        Modifier
                                                .fillMaxSize()
                                                .padding(paddingValues)
                                                .padding(horizontal = 20.dp)
                                                .verticalScroll(rememberScrollState())
                        ) {
                                DataAndStorageSection(
                                        storageLocationText =
                                                if (storageUriString == null) {
                                                        "App Internal Storage (Default)"
                                                } else {
                                                        "Custom Folder:\n" +
                                                                (
                                                                        storageUriString
                                                                                ?.toUri()
                                                                                ?.path
                                                                                ?.substringAfterLast(":")
                                                                                ?: ""
                                                                )
                                                },
                                        hasCustomStorage = storageUriString != null,
                                        onResetStorage = { confirmLocationChange(null) },
                                        onChooseFolder = { storageLauncher.launch(null) },
                                        formattedBackupDate = formattedBackupDate,
                                        backupReminderDays = backupReminderDays,
                                        onBackupNow = { viewModel.backupData(context) },
                                        onBackupReminderDaysChange = {
                                                viewModel.setBackupReminderDays(it)
                                        },
                                        onRestoreBackup = {
                                                backupRestoreLauncher.launch(
                                                        arrayOf("application/zip", "*/*")
                                                )
                                        },
                                        onExportObsidianVault = {
                                                obsidianExportLauncher.launch(null)
                                        },
                                        onRefreshCache = onNavigateToRebuildCache,
                                        swipeToSyncEnabled = swipeToSyncEnabled,
                                        onSwipeToSyncEnabledChange = {
                                                viewModel.setSwipeToSyncEnabled(it)
                                        },
                                        largeJournalSafeMode = largeJournalSafeMode,
                                        onLargeJournalSafeModeChange = {
                                                viewModel.setLargeJournalSafeMode(it)
                                        },
                                        showOnboardingNextLaunch = showOnboardingNextLaunch,
                                        onShowOnboardingNextLaunchChange = {
                                                viewModel.setShowOnboardingNextLaunch(it)
                                        },
                                        versionHistoryEnabled = versionHistoryEnabled,
                                        onVersionHistoryEnabledChange = {
                                                viewModel.setVersionHistoryEnabled(it)
                                        },
                                        onNavigateToVersionHistory = onNavigateToVersionHistory,
                                        importState = importState,
                                        onLaunchDayOneImport = {
                                                dayOneLauncher.launch(
                                                        arrayOf(
                                                                "application/zip",
                                                                "application/json",
                                                                "*/*"
                                                        )
                                                )
                                        },
                                        onLaunchJournalisticImport = {
                                                journalisticLauncher.launch(
                                                        arrayOf("application/json", "*/*")
                                                )
                                        },
                                        onLaunchMarkdownFolderImport = {
                                                markdownFolderImportLauncher.launch(null)
                                        },
                                        onCancelImport = { viewModel.cancelImport() },
                                        onResetImportState = { viewModel.resetImportState() }
                                )
                        }
                }
        }

        if (showDialog) {
                val destName =
                        if (pendingUriString == null) {
                                "App Internal Storage"
                        } else {
                                pendingUriString?.toUri()?.path?.substringAfterLast(":")
                                        ?: "the new folder"
                        }
                val currentName =
                        if (storageUriString == null) {
                                "App Internal Storage"
                        } else {
                                storageUriString?.toUri()?.path?.substringAfterLast(":")
                                        ?: "the current folder"
                        }

                AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = {
                                Text(stringResource(R.string.settings_change_storage_location_title))
                        },
                        text = {
                                Text(
                                        stringResource(
                                                R.string.settings_change_storage_location_message,
                                                currentName,
                                                destName
                                        )
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                viewModel.setStorageUri(pendingUriString)
                                                showDialog = false
                                        }
                                ) {
                                        Text(stringResource(R.string.settings_yes))
                                }
                        },
                        dismissButton = {
                                TextButton(onClick = { showDialog = false }) {
                                        Text(stringResource(R.string.settings_no))
                                }
                        }
                )
        }

        restoreSummary?.let { summary ->
                AlertDialog(
                        onDismissRequest = { viewModel.dismissRestoreSummary() },
                        title = { Text(stringResource(R.string.settings_restore_summary_title)) },
                        text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                                stringResource(
                                                        R.string.settings_restore_section_journal
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                stringResource(
                                                        R.string.settings_restore_journal_summary,
                                                        summary.newDays,
                                                        summary.mergedDays,
                                                        summary.skippedJournalEntries
                                                ),
                                                style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                                stringResource(
                                                        R.string.settings_restore_section_shortcodes
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                stringResource(
                                                        R.string.settings_restore_shortcodes_summary,
                                                        summary.shortcodesAdded,
                                                        summary.shortcodesSkipped
                                                ),
                                                style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                                stringResource(
                                                        R.string.settings_restore_section_date_keywords
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                stringResource(
                                                        R.string.settings_restore_date_keywords_summary,
                                                        summary.dateKeywordsAdded,
                                                        summary.dateKeywordsSkipped
                                                ),
                                                style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                                stringResource(
                                                        R.string.settings_restore_section_people_places
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                stringResource(
                                                        R.string.settings_restore_people_places_summary,
                                                        summary.peoplePlacesAdded,
                                                        summary.peoplePlacesSkipped
                                                ),
                                                style = MaterialTheme.typography.bodySmall
                                        )
                                }
                        },
                        confirmButton = {
                                TextButton(onClick = { viewModel.dismissRestoreSummary() }) {
                                        Text(stringResource(R.string.action_done))
                                }
                        }
                )
        }
}
