package com.mj.yaja.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mj.yaja.R
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLogScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    val appLogText by viewModel.appLogText.collectAsStateWithLifecycle()
    val appLogRetentionDays by viewModel.appLogRetentionDays.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val displayLogText = appLogText.toNewestFirstLog(localTimeLabel = stringResource(R.string.app_log_local_time_format))

    LaunchedEffect(Unit) {
        viewModel.loadAppLog()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_app_log_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            shareAppLog(
                                context = context,
                                logText = appLogText,
                                emptyText = context.getString(R.string.app_log_share_empty),
                                subject = context.getString(R.string.app_log_share_subject),
                                chooserTitle = context.getString(R.string.app_log_share_chooser)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.app_log_cd_share)
                        )
                    }
                    IconButton(onClick = { viewModel.clearAppLog() }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = stringResource(R.string.app_log_cd_clear)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.app_log_privacy_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.app_log_privacy_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = pluralStringResource(
                            R.plurals.app_log_retention_days,
                            appLogRetentionDays,
                            appLogRetentionDays
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = appLogRetentionDays.toFloat(),
                        onValueChange = { value ->
                            viewModel.setAppLogRetentionDays(value.toInt().coerceIn(1, 30))
                        },
                        valueRange = 1f..30f,
                        steps = 28
                    )
                    Text(
                        text = stringResource(R.string.app_log_retention_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = displayLogText.ifBlank { stringResource(R.string.app_log_empty) },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = { viewModel.loadAppLog() }) {
                Text(stringResource(R.string.app_log_refresh))
            }
        }
    }
}

private fun shareAppLog(
    context: Context,
    logText: String,
    emptyText: String,
    subject: String,
    chooserTitle: String
) {
    val shareText = logText.ifBlank { emptyText }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

private fun String.toNewestFirstLog(localTimeLabel: String): String {
    if (isBlank()) return ""
    val eventStart = Regex("""(?m)^\d{4}-\d{2}-\d{2}T""")
    val starts = eventStart.findAll(this).map { it.range.first }.toList()
    if (starts.size <= 1) return trim()

    return starts.indices
        .map { index ->
            val start = starts[index]
            val end = starts.getOrNull(index + 1) ?: length
            substring(start, end).trim().withLocalTimestampPrefix(localTimeLabel)
        }
        .asReversed()
        .joinToString("\n\n")
}

private fun String.withLocalTimestampPrefix(localTimeFormat: String): String {
    val separatorIndex = indexOf(" | ")
    if (separatorIndex <= 0) return this
    val rawTimestamp = take(separatorIndex)
    val localTimestamp = runCatching {
        LOCAL_LOG_TIME_FORMAT.format(Instant.parse(rawTimestamp).atZone(ZoneId.systemDefault()))
    }.getOrNull() ?: return this
    return localTimeFormat.format(localTimestamp, substring(separatorIndex + 3))
}

private val LOCAL_LOG_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
