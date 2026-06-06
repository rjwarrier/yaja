package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.unit.sp
import com.mj.yaja.ui.viewmodel.JournalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskerIntegrationScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    val allowTaskerAccess by viewModel.allowTaskerAccess.collectAsState()
    val allowTaskerEvents by viewModel.allowTaskerEvents.collectAsState()
    val includeEntryTextInTaskerEvents by viewModel.includeEntryTextInTaskerEvents.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Tasker Integration",
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
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TaskerToggleRow(
                        title = "Allow Tasker Add Entry",
                        subtitle = "Lets Tasker use Yaja Quick Capture actions like Add Entry and Add Todo.",
                        checked = allowTaskerAccess,
                        onCheckedChange = { viewModel.setAllowTaskerAccess(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    TaskerToggleRow(
                        title = "Allow Tasker Events",
                        subtitle = "Lets Yaja send Entry Saved, Entry Modified, and Entry Deleted events to Tasker.",
                        checked = allowTaskerEvents,
                        onCheckedChange = { viewModel.setAllowTaskerEvents(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    TaskerToggleRow(
                        title = "Include Entry Text in Events",
                        subtitle = "Off by default for privacy. When off, Tasker receives event type, date, label, and time, but not journal text.",
                        checked = includeEntryTextInTaskerEvents,
                        enabled = allowTaskerEvents,
                        onCheckedChange = { viewModel.setIncludeEntryTextInTaskerEvents(it) }
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RowTitle("What this does")
                    BodyText(
                        "Tasker can quickly add journal entries or todos into Yaja, append lines into a grouped entry such as Missed Calls, and Yaja can tell Tasker when an entry is saved, edited, or deleted."
                    )

                    RowTitle("How to use")
                    BodyText(
                        "Turn on the toggle you need here first. Then open Tasker and add either a Yaja Quick Capture action or a Yaja Journal Events event."
                    )

                    RowTitle("Notes")
                    BodyText(
                        "Entry text is not shared with Tasker unless you enable Include Entry Text in Events. If Tasker does not show the latest options or variables, reopen the Tasker configuration screen after updating Yaja."
                    )

                    RowTitle("Example")
                    BodyText(
                        "Action example: Tasker action > Yaja Quick Capture > Append, with Match / header set to Missed Calls and Line to append set to a Tasker variable. Event example: Tasker event > Yaja Journal Events > Entry Modified, then use variables like %yaja_event_type, %yaja_date, and optionally %yaja_entry_text."
                    )

                    RowTitle("Use cases")
                    BodyText(
                        "1. Use a Yaja journal event in Tasker to trigger a FolderSync sync action, so your Yaja folder gets pushed to any cloud provider after entries change."
                    )
                    BodyText(
                        "2. Use Pushbullet with Tasker to send text into Yaja Quick Capture, so the pushed text gets saved as a new entry for the current day."
                    )
                    BodyText(
                        "3. Use a missed-call Tasker profile to send an Append action into Yaja, so all missed-call details land under one Missed Calls entry instead of creating separate entries."
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TaskerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun RowTitle(text: String) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 22.sp
    )
}
