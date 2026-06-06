package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.EntryTemplate

@Composable
fun TemplateStartBlankCard(
        onTemplateSelected: (EntryTemplate) -> Unit
) {
        Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Text(
                                text = "Start Blank",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                        )
                        Text(
                                text = "Skip templates and keep writing from an empty draft.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                                onClick = {
                                        onTemplateSelected(
                                                EntryTemplate(
                                                        id = "blank_entry",
                                                        name = "Blank Entry",
                                                        description = "Plain journal entry",
                                                        category = "General",
                                                        body = ""
                                                )
                                        )
                                }
                        ) { Text("Start Blank") }
                }
        }
}

@Composable
fun TemplateQuickPicksSection(
        quickPicks: List<EntryTemplate>,
        favoriteTemplateIds: Set<String>,
        onToggleFavorite: (String) -> Unit,
        onTemplatePreview: (EntryTemplate) -> Unit,
        onTemplateSelected: (EntryTemplate) -> Unit
) {
        if (quickPicks.isEmpty()) return

        Text(
                        text = "Quick Picks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                )
        quickPicks.forEach { template ->
                TemplateCard(
                        template = template,
                        isFavorited = favoriteTemplateIds.contains(template.id),
                        onToggleFavorite = onToggleFavorite,
                        onTemplatePreview = onTemplatePreview,
                        onTemplateSelected = onTemplateSelected
                )
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateCategoryChips(
        categories: List<String>,
        selectedCategory: String,
        onCategorySelected: (String) -> Unit
) {
        FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                categories.forEach { category ->
                        FilterChip(
                                selected = selectedCategory == category,
                                onClick = { onCategorySelected(category) },
                                label = { Text(category) }
                        )
                }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateSortChips(
        sortMode: String,
        onSortModeSelected: (String) -> Unit
) {
        FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                listOf("Recommended", "Recent", "A-Z").forEach { option ->
                        FilterChip(
                                selected = sortMode == option,
                                onClick = { onSortModeSelected(option) },
                                label = { Text(option) }
                        )
                }
        }
}

@Composable
fun TemplateEmptyResultsCard() {
        Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                        Text(
                                text = "No templates found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                        )
                        Text(
                                text = "Try a different search or category.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
        }
}

@Composable
fun TemplateGroupedResultsSection(
        groupedTemplates: Map<String, List<EntryTemplate>>,
        sortMode: String,
        recentTemplateIds: List<String>,
        favoriteTemplateIds: Set<String>,
        onToggleFavorite: (String) -> Unit,
        onTemplatePreview: (EntryTemplate) -> Unit,
        onTemplateSelected: (EntryTemplate) -> Unit
) {
        groupedTemplates.forEach { (category, categoryTemplatesRaw) ->
                val categoryTemplates =
                        when (sortMode) {
                                "A-Z" -> categoryTemplatesRaw.sortedBy { it.name }
                                "Recent" ->
                                        categoryTemplatesRaw.sortedBy {
                                                recentTemplateIds.indexOf(it.id)
                                                        .takeIf { idx -> idx >= 0 }
                                                        ?: Int.MAX_VALUE
                                        }
                                else ->
                                        categoryTemplatesRaw.sortedByDescending {
                                                templateScore(
                                                        template = it,
                                                        recentTemplateIds = recentTemplateIds,
                                                        favoriteTemplateIds = favoriteTemplateIds
                                                )
                                        }
                        }
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                text = category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                        )
                        Text(
                                text = "${categoryTemplates.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
                categoryTemplates.forEach { template ->
                        TemplateCard(
                                template = template,
                                isFavorited = favoriteTemplateIds.contains(template.id),
                                onToggleFavorite = onToggleFavorite,
                                onTemplatePreview = onTemplatePreview,
                                onTemplateSelected = onTemplateSelected
                        )
                }
                HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
        }
}
