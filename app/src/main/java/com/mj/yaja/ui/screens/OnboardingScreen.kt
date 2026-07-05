package com.mj.yaja.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.yaja.R
import com.mj.yaja.data.ThemePreference
import com.mj.yaja.ui.viewmodel.JournalViewModel

private const val ONBOARDING_STEP_WELCOME = 0
private const val ONBOARDING_STEP_STORAGE = 1
private const val ONBOARDING_STEP_CUSTOMIZE = 2
private const val ONBOARDING_STEP_IMPORT = 3
private const val ONBOARDING_STEP_COUNT = 4

@Composable
fun OnboardingScreen(
    viewModel: JournalViewModel,
    isRerun: Boolean,
    onNavigateToPinSetup: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val storageUri by viewModel.storageUri.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val backupReminderDays by viewModel.backupReminderDays.collectAsStateWithLifecycle()
    val versionHistoryEnabled by viewModel.versionHistoryEnabled.collectAsStateWithLifecycle()
    val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    var currentStep by remember { mutableIntStateOf(ONBOARDING_STEP_WELCOME) }

    fun finishOnboarding() {
        viewModel.completeOnboarding()
        onComplete()
    }

    val currentStorageText =
        if (storageUri == null) {
            stringResource(R.string.onboarding_storage_internal)
        } else {
            stringResource(
                R.string.onboarding_storage_custom,
                storageUri?.toUri()?.path?.substringAfterLast(":") ?: ""
            )
        }

    val folderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                val takeFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (_: SecurityException) {
                }
                viewModel.setStorageUri(uri.toString())
            }
        }

    val dayOneLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.importDayOneFile(uri, context)
            }
        }
    val journalisticLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.importJournalisticFile(uri, context)
            }
        }

    Scaffold { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.onboarding_progress, currentStep + 1, ONBOARDING_STEP_COUNT),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = ::finishOnboarding) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (currentStep) {
                ONBOARDING_STEP_WELCOME -> {
                    OnboardingStepCard(
                        title = stringResource(R.string.onboarding_title),
                        body = stringResource(R.string.onboarding_body)
                    ) {
                        OnboardingInfoRow(
                            icon = Icons.Rounded.Lock,
                            text = stringResource(R.string.onboarding_intro_private)
                        )
                        OnboardingInfoRow(
                            icon = Icons.Rounded.Storage,
                            text = stringResource(R.string.onboarding_intro_files)
                        )
                        OnboardingInfoRow(
                            icon = Icons.Rounded.ImportExport,
                            text = stringResource(R.string.onboarding_intro_flexible)
                        )
                    }
                }

                ONBOARDING_STEP_STORAGE -> {
                    OnboardingStepCard(
                        title = stringResource(R.string.onboarding_storage_title),
                        body = stringResource(R.string.onboarding_storage_body)
                    ) {
                        OnboardingInfoRow(
                            icon = Icons.Rounded.Storage,
                            text = currentStorageText
                        )
                        OnboardingInfoRow(
                            icon = Icons.Rounded.Lock,
                            text = stringResource(R.string.onboarding_storage_note)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = { viewModel.setStorageUri(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text =
                                    if (storageUri == null) {
                                        stringResource(R.string.onboarding_continue_internal)
                                    } else {
                                        stringResource(R.string.onboarding_switch_internal)
                                    }
                            )
                        }

                        OutlinedButton(
                            onClick = { folderLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null
                            )
                            Text(
                                text = stringResource(R.string.onboarding_choose_folder),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                ONBOARDING_STEP_CUSTOMIZE -> {
                    OnboardingStepCard(
                        title = stringResource(R.string.onboarding_customize_title),
                        body = stringResource(R.string.onboarding_customize_body)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_theme_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeChoiceButton(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.settings_theme_system),
                                selected = themePreference == ThemePreference.SYSTEM,
                                onClick = { viewModel.setThemePreference(ThemePreference.SYSTEM) }
                            )
                            ThemeChoiceButton(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.settings_theme_light),
                                selected = themePreference == ThemePreference.LIGHT,
                                onClick = { viewModel.setThemePreference(ThemePreference.LIGHT) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeChoiceButton(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.settings_theme_dark),
                                selected = themePreference == ThemePreference.DARK,
                                onClick = { viewModel.setThemePreference(ThemePreference.DARK) }
                            )
                            ThemeChoiceButton(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.settings_theme_amoled),
                                selected = themePreference == ThemePreference.AMOLED,
                                onClick = { viewModel.setThemePreference(ThemePreference.AMOLED) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SettingSwitchRow(
                            icon = Icons.Rounded.Security,
                            title = stringResource(R.string.onboarding_version_history_title),
                            subtitle = stringResource(R.string.onboarding_version_history_body),
                            checked = versionHistoryEnabled,
                            onCheckedChange = { viewModel.setVersionHistoryEnabled(it) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.onboarding_backup_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.onboarding_backup_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BackupChoiceButton(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.onboarding_backup_off),
                                selected = backupReminderDays == 0,
                                onClick = { viewModel.setBackupReminderDays(0) }
                            )
                            BackupChoiceButton(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.onboarding_backup_30),
                                selected = backupReminderDays == 30,
                                onClick = { viewModel.setBackupReminderDays(30) }
                            )
                            BackupChoiceButton(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.onboarding_backup_60),
                                selected = backupReminderDays == 60,
                                onClick = { viewModel.setBackupReminderDays(60) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = onNavigateToPinSetup,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null
                            )
                            Text(
                                text =
                                    if (isPinEnabled) {
                                        stringResource(R.string.onboarding_pin_enabled)
                                    } else {
                                        stringResource(R.string.onboarding_pin_setup)
                                    },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                ONBOARDING_STEP_IMPORT -> {
                    OnboardingStepCard(
                        title = stringResource(R.string.onboarding_import_title),
                        body = stringResource(R.string.onboarding_import_body)
                    ) {
                        OutlinedButton(
                            onClick = {
                                dayOneLauncher.launch(arrayOf("application/zip", "application/json", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.settings_import_source_day_one))
                        }

                        OutlinedButton(
                            onClick = {
                                journalisticLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.settings_import_source_journalistic))
                        }

                        val importStatus =
                            when (val state = importState) {
                                is JournalViewModel.ImportState.Running ->
                                    context.getString(
                                        R.string.onboarding_import_running,
                                        state.current,
                                        state.total
                                    )
                                is JournalViewModel.ImportState.Success ->
                                    context.getString(
                                        R.string.onboarding_import_success,
                                        state.newDays,
                                        state.mergedDays
                                    )
                                is JournalViewModel.ImportState.Error ->
                                    state.message
                                JournalViewModel.ImportState.Idle -> null
                            }

                        if (importStatus != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = importStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.onboarding_import_later),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > ONBOARDING_STEP_WELCOME) {
                    OutlinedButton(
                        onClick = { currentStep -= 1 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                }

                Button(
                    onClick = {
                        if (currentStep == ONBOARDING_STEP_IMPORT) {
                            finishOnboarding()
                        } else {
                            currentStep += 1
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text =
                            if (currentStep == ONBOARDING_STEP_IMPORT) {
                                stringResource(R.string.onboarding_finish)
                            } else {
                                stringResource(R.string.onboarding_next)
                            }
                    )
                }
            }

            if (isRerun) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.onboarding_rerun_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepCard(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                content()
            }
        )
    }
}

@Composable
private fun OnboardingInfoRow(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeChoiceButton(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ChoiceCard(
        modifier = modifier,
        title = title,
        selected = selected,
        onClick = onClick
    )
}

@Composable
private fun BackupChoiceButton(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ChoiceCard(
        modifier = modifier,
        title = label,
        selected = selected,
        onClick = onClick
    )
}

@Composable
private fun ChoiceCard(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
