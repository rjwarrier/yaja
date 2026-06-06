package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TimelinePreviewSheet(
    node: TimelineDateNode,
    previewText: String?,
    onOpenDate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = node.date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.getDefault())),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        node.label?.let {
            AssistChip(onClick = onOpenDate, label = { Text(it) })
        }
        Text(
            text = when {
                !node.hasEntries -> "This day is labeled, but there is no journal entry yet."
                previewText.isNullOrBlank() -> "Loading preview..."
                else -> previewText
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (node.entryCount > 1) {
            Text(
                text = "+ ${node.entryCount - 1} more ${if (node.entryCount == 2) "entry" else "entries"} on this day",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        FilledTonalButton(onClick = onOpenDate) {
            Text("Open This Day")
        }
    }
}
