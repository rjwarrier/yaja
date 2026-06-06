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
    val displayLogText = appLogText.toNewestFirstLog()

    LaunchedEffect(Unit) {
        viewModel.loadAppLog()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("App Log") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { shareAppLog(context, appLogText) }) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share app log"
                        )
                    }
                    IconButton(onClick = { viewModel.clearAppLog() }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear app log"
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
                            text = "Privacy-safe diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This log records major app events, cache/storage operations, widget refreshes, and crash details. It does not include journal entries, todo text, or private note content.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Keep last $appLogRetentionDays ${if (appLogRetentionDays == 1) "day" else "days"}",
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
                        text = "Older log events are pruned automatically during logging and immediately when this setting changes.",
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
                    text = displayLogText.ifBlank { "No app log yet." },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = { viewModel.loadAppLog() }) {
                Text("Refresh Log")
            }
        }
    }
}

private fun shareAppLog(context: Context, logText: String) {
    val shareText = logText.ifBlank { "No Yaja app log entries yet." }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Yaja App Log")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Yaja app log"))
}

private fun String.toNewestFirstLog(): String {
    if (isBlank()) return ""
    val eventStart = Regex("""(?m)^\d{4}-\d{2}-\d{2}T""")
    val starts = eventStart.findAll(this).map { it.range.first }.toList()
    if (starts.size <= 1) return trim()

    return starts.indices
        .map { index ->
            val start = starts[index]
            val end = starts.getOrNull(index + 1) ?: length
            substring(start, end).trim().withLocalTimestampPrefix()
        }
        .asReversed()
        .joinToString("\n\n")
}

private fun String.withLocalTimestampPrefix(): String {
    val separatorIndex = indexOf(" | ")
    if (separatorIndex <= 0) return this
    val rawTimestamp = take(separatorIndex)
    val localTimestamp = runCatching {
        LOCAL_LOG_TIME_FORMAT.format(Instant.parse(rawTimestamp).atZone(ZoneId.systemDefault()))
    }.getOrNull() ?: return this
    return "$localTimestamp | local time\n${substring(separatorIndex + 3)}"
}

private val LOCAL_LOG_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
