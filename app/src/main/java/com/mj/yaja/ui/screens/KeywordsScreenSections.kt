package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.KeywordType
import com.mj.yaja.ui.design.AppStaggeredEntrance



@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KeywordTransferActionsCard(
        visible: Boolean,
        onImportCsv: () -> Unit,
        onExportCsv: () -> Unit,
        onDownloadTemplate: () -> Unit
) {
        AppStaggeredEntrance(visible = visible, index = 1) {
                Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.large
                ) {
                        FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                FilledTonalIconButton(onClick = onImportCsv) {
                                        Icon(
                                                Icons.Rounded.FileOpen,
                                                contentDescription = stringResource(R.string.keywords_cd_import_csv),
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                }
                                FilledTonalIconButton(onClick = onExportCsv) {
                                        Icon(
                                                Icons.Rounded.SaveAlt,
                                                contentDescription = stringResource(R.string.keywords_cd_export_csv),
                                                tint = MaterialTheme.colorScheme.tertiary
                                        )
                                }
                                FilledTonalIconButton(onClick = onDownloadTemplate) {
                                        Icon(
                                                Icons.Rounded.Description,
                                                contentDescription = stringResource(R.string.keywords_cd_download_template),
                                                tint = MaterialTheme.colorScheme.secondary
                                        )
                                }
                        }
                }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KeywordFiltersCard(
        visible: Boolean,
        searchQuery: String,
        onSearchQueryChange: (String) -> Unit,
        selectedTypeFilter: KeywordType?,
        onSelectedTypeFilterChange: (KeywordType?) -> Unit,
        showOnlyNeedsReindex: Boolean,
        onShowOnlyNeedsReindexChange: (Boolean) -> Unit,
        selectedSort: KeywordListSortOption,
        onSelectedSortChange: (KeywordListSortOption) -> Unit
) {
        AppStaggeredEntrance(visible = visible, index = 2) {
                Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.large
                ) {
                        Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                                        leadingIcon = {
                                                Icon(
                                                        imageVector = Icons.Rounded.Search,
                                                        contentDescription = null
                                                )
                                        },
                                        placeholder = {
                                                Text(stringResource(R.string.keywords_search_placeholder))
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                )

                                FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        FilterChip(
                                                selected = selectedTypeFilter == null,
                                                onClick = { onSelectedTypeFilterChange(null) },
                                                label = { Text(stringResource(R.string.keywords_connections_filter_all)) }
                                        )
                                        FilterChip(
                                                selected = selectedTypeFilter == KeywordType.PERSON,
                                                onClick = { onSelectedTypeFilterChange(KeywordType.PERSON) },
                                                label = { Text(stringResource(R.string.keywords_connections_filter_people)) }
                                        )
                                        FilterChip(
                                                selected = selectedTypeFilter == KeywordType.PLACE,
                                                onClick = { onSelectedTypeFilterChange(KeywordType.PLACE) },
                                                label = { Text(stringResource(R.string.keywords_connections_filter_places)) }
                                        )
                                        FilterChip(
                                                selected = showOnlyNeedsReindex,
                                                onClick = { onShowOnlyNeedsReindexChange(!showOnlyNeedsReindex) },
                                                label = { Text(stringResource(R.string.keywords_filter_needs_reindex)) }
                                        )
                                }

                                FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        KeywordListSortOption.entries.forEach { option ->
                                                FilterChip(
                                                        selected = selectedSort == option,
                                                        onClick = { onSelectedSortChange(option) },
                                                        leadingIcon = {
                                                                if (selectedSort == option) {
                                                                        Icon(
                                                                                imageVector = Icons.Rounded.SortByAlpha,
                                                                                contentDescription = null,
                                                                                modifier = Modifier.size(16.dp)
                                                                        )
                                                                }
                                                        },
                                                        label = { Text(stringResource(option.labelRes)) }
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
internal fun KeywordProgressSection(
        progress: Float,
        label: String
) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
                LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                text = stringResource(R.string.keywords_refreshing_counters),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
        }
}

@Composable
internal fun KeywordsEmptyState() {
        Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(88.dp)
                ) {
                        Box(contentAlignment = Alignment.Center) {
                                Icon(
                                        imageVector = Icons.Rounded.People,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(42.dp)
                                )
                        }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                        stringResource(R.string.keywords_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        stringResource(R.string.keywords_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }
}

@Composable
internal fun KeywordsListSection(
        filteredKeywords: List<KeywordDefinition>,
        people: List<KeywordDefinition>,
        places: List<KeywordDefinition>,
        selectedTypeFilter: KeywordType?,
        normalizedSearch: String,
        showOnlyNeedsReindex: Boolean,
        contentVisible: Boolean,
        keywordMatchCounts: Map<String, Int>,
        keywordIndexingIds: Set<String>,
        fabBottomPadding: androidx.compose.ui.unit.Dp,
        onNavigateToKeyword: (String) -> Unit,
        onToggleEnabled: (String, Boolean) -> Unit,
        onEdit: (KeywordDefinition) -> Unit,
        onDelete: (KeywordDefinition) -> Unit
) {
        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = fabBottomPadding + 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                if (filteredKeywords.isEmpty()) {
                        item {
                                Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.large
                                ) {
                                        Column(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                                Text(
                                                        text = stringResource(R.string.keywords_no_results_title),
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                        text = stringResource(R.string.keywords_no_results_subtitle),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                        }
                                }
                        }
                }

                if (selectedTypeFilter != KeywordType.PLACE && filteredKeywords.isNotEmpty()) {
                        item {
                                KeywordSectionHeader(
                                        icon = Icons.Rounded.Person,
                                        title = stringResource(R.string.keywords_people_title),
                                        count = people.size,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                        }
                        if (people.isEmpty()) {
                                item {
                                        Text(
                                                text = if (normalizedSearch.isBlank() && !showOnlyNeedsReindex) {
                                                        stringResource(R.string.keywords_people_empty)
                                                } else {
                                                        stringResource(R.string.keywords_people_filtered_empty)
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                        )
                                }
                        } else {
                                itemsIndexed(people, key = { _, it -> it.id }) { index, keyword ->
                                        AppStaggeredEntrance(
                                                visible = contentVisible,
                                                index = index.coerceAtMost(5) + 1
                                        ) {
                                                KeywordCard(
                                                        keyword = keyword,
                                                        matchCount = keywordMatchCounts[keyword.id] ?: 0,
                                                        isIndexing = keyword.id in keywordIndexingIds,
                                                        onNavigateToDetail = { onNavigateToKeyword(keyword.id) },
                                                        onToggleEnabled = { onToggleEnabled(keyword.id, it) },
                                                        onEdit = { onEdit(keyword) },
                                                        onDelete = { onDelete(keyword) }
                                                )
                                        }
                                }
                        }
                }

                if (selectedTypeFilter == null && filteredKeywords.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                if (selectedTypeFilter != KeywordType.PERSON && filteredKeywords.isNotEmpty()) {
                        item {
                                KeywordSectionHeader(
                                        icon = Icons.Rounded.LocationOn,
                                        title = stringResource(R.string.keywords_places_title),
                                        count = places.size,
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        onContainerColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                        }
                        if (places.isEmpty()) {
                                item {
                                        Text(
                                                text = if (normalizedSearch.isBlank() && !showOnlyNeedsReindex) {
                                                        stringResource(R.string.keywords_places_empty)
                                                } else {
                                                        stringResource(R.string.keywords_places_filtered_empty)
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                        )
                                }
                        } else {
                                itemsIndexed(places, key = { _, it -> it.id }) { index, keyword ->
                                        AppStaggeredEntrance(
                                                visible = contentVisible,
                                                index = index.coerceAtMost(5) + 1
                                        ) {
                                                KeywordCard(
                                                        keyword = keyword,
                                                        matchCount = keywordMatchCounts[keyword.id] ?: 0,
                                                        isIndexing = keyword.id in keywordIndexingIds,
                                                        onNavigateToDetail = { onNavigateToKeyword(keyword.id) },
                                                        onToggleEnabled = { onToggleEnabled(keyword.id, it) },
                                                        onEdit = { onEdit(keyword) },
                                                        onDelete = { onDelete(keyword) }
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
internal fun DeleteKeywordDialog(
        keyword: KeywordDefinition,
        onDismiss: () -> Unit,
        onConfirmDelete: () -> Unit
) {
        AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                        Text(if (keyword.type == KeywordType.PERSON) stringResource(R.string.keywords_delete_person_title) else stringResource(R.string.keywords_delete_place_title))
                },
                text = {
                        Text(
                                stringResource(R.string.keywords_delete_message, keyword.name)
                        )
                },
                confirmButton = {
                        TextButton(onClick = onConfirmDelete) {
                                Text(stringResource(R.string.keywords_action_delete))
                        }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.action_cancel))
                        }
                }
        )
}
