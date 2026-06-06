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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.EntryTemplate

@Composable
fun DiscardChangesDialog(
        onDismiss: () -> Unit,
        onConfirmDiscard: () -> Unit
) {
        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Discard Changes") },
                text = {
                        Text(
                                "You have unsaved changes. Go back and discard them, or stay to keep editing."
                        )
                },
                confirmButton = {
                        TextButton(onClick = onConfirmDiscard) { Text("Go Back") }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text("Stay") }
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
                title = { Text("Shortcuts") },
                text = {
                        Column {
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("**Bold**")
                                                }
                                                append(
                                                        " to make text bold (or select text and tap Bold Button)"
                                                )
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("*Italic*")
                                                }
                                                append(
                                                        " to make text italic (or select text and tap Italic Button)"
                                                )
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
                                                append(": Expands to current date (e.g., 25-Dec-25)")
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("@now")
                                                }
                                                append(": Expands to current time (e.g., 14:30)")
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("@week")
                                                }
                                                append(": Expands to current week (e.g., Week 52)")
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("@day")
                                                }
                                                append(": Expands to current weekday (e.g., Monday)")
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("@t ")
                                                }
                                                append(": Expands to a todo checkbox (e.g., [ ] )")
                                        }
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                Text(
                                        buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("@x")
                                                }
                                                append(
                                                        ": When entered anywhere in the line containing a todo checkbox, it will toggle it."
                                                )
                                        }
                                )
                        }
                },
                confirmButton = {
                        TextButton(onClick = onDismiss) { Text("Okey!") }
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
                                        "This draft already has text. Choose how the template should be placed."
                                )
                                Text(
                                        text = "Insert Below keeps the current draft. At Cursor places the template where your caret is now.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                },
                confirmButton = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = onInsertBelow) { Text("Insert Below") }
                                        TextButton(onClick = onInsertAtCursor) { Text("At Cursor") }
                                }
                                Button(onClick = onReplaceDraft) { Text("Replace Draft") }
                        }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                }
        )
}
