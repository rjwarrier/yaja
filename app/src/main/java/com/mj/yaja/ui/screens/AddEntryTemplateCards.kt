package com.mj.yaja.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.EntryTemplate

fun templateIcon(template: EntryTemplate): ImageVector =
        when (template.id) {
                "meeting_note" -> Icons.Rounded.Person
                "work_log" -> Icons.Rounded.Check
                "travel_day" -> Icons.Rounded.LocationOn
                "idea_dump" -> Icons.Rounded.Edit
                "health_log" -> Icons.Rounded.Today
                "expense_note" -> Icons.Rounded.Description
                "reflection" -> Icons.Rounded.Check
                "decision_log" -> Icons.Rounded.Check
                "conversation_note" -> Icons.Rounded.Share
                "event_note" -> Icons.Rounded.Person
                "reading_note" -> Icons.Rounded.Description
                "dream_log" -> Icons.Rounded.Today
                "project_update" -> Icons.Rounded.Check
                else -> Icons.Rounded.Edit
        }

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TemplateCard(
        template: EntryTemplate,
        isFavorited: Boolean,
        onToggleFavorite: (String) -> Unit,
        onTemplatePreview: (EntryTemplate) -> Unit,
        onTemplateSelected: (EntryTemplate) -> Unit
) {
        Surface(
                modifier =
                        Modifier.combinedClickable(
                                onClick = { onTemplateSelected(template) },
                                onLongClick = { onTemplatePreview(template) }
                        ),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border =
                        BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                        Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                        imageVector = templateIcon(template),
                                                        contentDescription = null,
                                                        tint =
                                                                MaterialTheme.colorScheme
                                                                        .onPrimaryContainer
                                                )
                                        }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                        Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        text = template.name,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                )
                                                if (template.isPopular) {
                                                        AssistChip(
                                                                onClick = { onTemplateSelected(template) },
                                                                label = { Text("Popular") }
                                                        )
                                                }
                                                if (template.isNew) {
                                                        AssistChip(
                                                                onClick = { onTemplateSelected(template) },
                                                                label = { Text("New") }
                                                        )
                                                }
                                        }
                                        Text(
                                                text = template.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (template.bestFor.isNotBlank()) {
                                                Text(
                                                        text = "Best for: ${template.bestFor}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                                        .copy(alpha = 0.9f)
                                                )
                                        }
                                }
                                IconButton(onClick = { onToggleFavorite(template.id) }) {
                                        Icon(
                                                imageVector =
                                                        if (isFavorited) Icons.Rounded.Star
                                                        else Icons.Rounded.StarOutline,
                                                contentDescription =
                                                        if (isFavorited)
                                                                "Remove template favorite"
                                                        else "Favorite template",
                                                tint =
                                                        if (isFavorited)
                                                                MaterialTheme.colorScheme.primary
                                                        else
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                        )
                                }
                        }
                        FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                templateSections(template).forEach { section ->
                                        AssistChip(
                                                onClick = { onTemplateSelected(template) },
                                                label = { Text(section, maxLines = 1) }
                                        )
                                }
                        }
                        FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                AssistChip(
                                        onClick = { onTemplateSelected(template) },
                                        label = { Text("${templateSections(template).size} sections") }
                                )
                                if (templateHasChecklist(template)) {
                                        AssistChip(
                                                onClick = { onTemplateSelected(template) },
                                                label = { Text("Checklist") }
                                        )
                                }
                                if (templateHasFollowUp(template)) {
                                        AssistChip(
                                                onClick = { onTemplateSelected(template) },
                                                label = { Text("Follow-up") }
                                        )
                                }
                        }
                        Text(
                                text = templatePreviewLine(template),
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                                maxLines = 1
                        )
                }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplatePreviewSheet(
        template: EntryTemplate,
        isFavorited: Boolean,
        onToggleFavorite: () -> Unit,
        onUseTemplate: () -> Unit
) {
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = template.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                )
                                Text(
                                        text = template.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                        IconButton(onClick = onToggleFavorite) {
                                Icon(
                                        imageVector =
                                                if (isFavorited) Icons.Rounded.Star
                                                else Icons.Rounded.StarOutline,
                                        contentDescription = "Toggle favorite template"
                                )
                        }
                }
                FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        AssistChip(onClick = {}, label = { Text(template.category) })
                        AssistChip(
                                onClick = {},
                                label = { Text("${templateSections(template).size} sections") }
                        )
                        if (templateHasChecklist(template)) {
                                AssistChip(onClick = {}, label = { Text("Checklist") })
                        }
                        if (templateHasFollowUp(template)) {
                                AssistChip(onClick = {}, label = { Text("Follow-up") })
                        }
                }
                if (template.bestFor.isNotBlank()) {
                        Text(
                                text = "Best for: ${template.bestFor}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
                Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                        Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                Text(
                                        text = "Template preview",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                        text = template.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                )
                        }
                }
                Button(onClick = onUseTemplate, modifier = Modifier.fillMaxWidth()) {
                        Text("Use Template")
                }
        }
}

fun templateSections(template: EntryTemplate): List<String> =
        template.body.lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("##") }
                .map { it.trimStart('#').trim() }
                .take(3)
                .toList()

fun templatePreviewLine(template: EntryTemplate): String =
        templateSections(template).joinToString(" | ").ifBlank { template.description }

fun templateSearchText(template: EntryTemplate): String =
        buildString {
                append(template.name)
                append(' ')
                append(template.description)
                append(' ')
                append(template.category)
                append(' ')
                append(templateSections(template).joinToString(" "))
        }

fun templateHasChecklist(template: EntryTemplate): Boolean = "[ ]" in template.body

fun templateHasFollowUp(template: EntryTemplate): Boolean =
        template.body.contains("follow-up", ignoreCase = true) ||
                template.body.contains("revisit", ignoreCase = true)

fun templateScore(
        template: EntryTemplate,
        recentTemplateIds: List<String>,
        favoriteTemplateIds: Set<String>
): Int {
        var score = 0
        if (template.id in favoriteTemplateIds) score += 100
        val recentIndex = recentTemplateIds.indexOf(template.id)
        if (recentIndex >= 0) score += 50 - recentIndex
        if (template.isPopular) score += 20
        if (template.isNew) score += 10
        return score
}
