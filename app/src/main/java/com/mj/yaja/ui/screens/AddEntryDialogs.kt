package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.data.EntryTemplate

@Composable
fun DiscardChangesDialog(
        onDismiss: () -> Unit,
        onConfirmDiscard: () -> Unit
) {
        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.addentry_discard_changes_title)) },
                text = {
                        Text(
                                stringResource(R.string.addentry_discard_changes_message)
                        )
                },
                confirmButton = {
                        TextButton(onClick = onConfirmDiscard) { Text(stringResource(R.string.addentry_go_back)) }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.addentry_stay)) }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
}

@Composable
fun AddEntryHelpDialog(
        onDismiss: () -> Unit
) {
        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.addentry_shortcuts_title)) },
                text = {
                        val boldSuffix = stringResource(R.string.addentry_help_bold_suffix)
                        val italicSuffix = stringResource(R.string.addentry_help_italic_suffix)
                        val todaySuffix = stringResource(R.string.addentry_help_today_suffix)
                        val nowSuffix = stringResource(R.string.addentry_help_now_suffix)
                        val weekSuffix = stringResource(R.string.addentry_help_week_suffix)
                        val daySuffix = stringResource(R.string.addentry_help_day_suffix)
                        val todoSuffix = stringResource(R.string.addentry_help_todo_suffix)
                        val toggleSuffix = stringResource(R.string.addentry_help_toggle_suffix)
                        Column {
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("**Bold**")
                                                }
                                                append(boldSuffix)
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("*Italic*")
                                                }
                                                append(italicSuffix)
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text("-------------------------------")
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("@today")
                                                }
                                                append(todaySuffix)
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("@now")
                                                }
                                                append(nowSuffix)
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("@week")
                                                }
                                                append(weekSuffix)
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("@day")
                                                }
                                                append(daySuffix)
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("@t ")
                                                }
                                                append(todoSuffix)
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("@x")
                                                }
                                                append(toggleSuffix)
                                        }
                                )
                        }
                },
                confirmButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.addentry_okey)) }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
}

@Composable
fun TemplateMergeDialog(
        template: EntryTemplate,
        onDismiss: () -> Unit,
        onInsertBelow: () -> Unit,
        onInsertAtCursor: () -> Unit,
        onReplaceDraft: () -> Unit
) {
        AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                        Text(
                                text = template.name,
                                fontWeight = FontWeight.SemiBold
                        )
                },
                text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                        stringResource(R.string.addentry_template_merge_message)
                                )
                                Text(
                                        text = stringResource(R.string.addentry_template_merge_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                },
                confirmButton = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = onInsertBelow) { Text(stringResource(R.string.addentry_insert_below)) }
                                        TextButton(onClick = onInsertAtCursor) { Text(stringResource(R.string.addentry_at_cursor)) }
                                }
                                Button(onClick = onReplaceDraft) { Text(stringResource(R.string.addentry_replace_draft)) }
                        }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                }
        )
}
