package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.KeywordType

@Composable
internal fun KeywordsScreenContent(
    contentVisible: Boolean,
    keywords: List<KeywordDefinition>,
    isAnyKeywordIndexing: Boolean,
    onImportCsv: () -> Unit,
    onExportCsv: () -> Unit,
    onDownloadTemplate: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTypeFilter: KeywordType?,
    onSelectedTypeFilterChange: (KeywordType?) -> Unit,
    showOnlyNeedsReindex: Boolean,
    onShowOnlyNeedsReindexChange: (Boolean) -> Unit,
    selectedSort: KeywordListSortOption,
    onSelectedSortChange: (KeywordListSortOption) -> Unit,
    keywordRebuildProgress: Float?,
    keywordEstimatedRemainingMillis: Long?,
    filteredKeywords: List<KeywordDefinition>,
    people: List<KeywordDefinition>,
    places: List<KeywordDefinition>,
    normalizedSearch: String,
    keywordMatchCounts: Map<String, Int>,
    keywordIndexingIds: Set<String>,
    fabBottomPadding: Dp,
    onNavigateToKeyword: (String) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onEdit: (KeywordDefinition) -> Unit,
    onDelete: (KeywordDefinition) -> Unit,
    paddingValues: androidx.compose.foundation.layout.PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        KeywordTransferActionsCard(
            visible = contentVisible,
            onImportCsv = onImportCsv,
            onExportCsv = onExportCsv,
            onDownloadTemplate = onDownloadTemplate
        )

        KeywordFiltersCard(
            visible = contentVisible,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            selectedTypeFilter = selectedTypeFilter,
            onSelectedTypeFilterChange = onSelectedTypeFilterChange,
            showOnlyNeedsReindex = showOnlyNeedsReindex,
            onShowOnlyNeedsReindexChange = onShowOnlyNeedsReindexChange,
            selectedSort = selectedSort,
            onSelectedSortChange = onSelectedSortChange
        )

        if (isAnyKeywordIndexing && keywordRebuildProgress != null) {
            KeywordProgressSection(
                progress = keywordRebuildProgress,
                label = buildKeywordProgressLabel(
                    progress = keywordRebuildProgress,
                    estimatedRemainingMillis = keywordEstimatedRemainingMillis
                )
            )
        }

        if (keywords.isEmpty()) {
            KeywordsEmptyState()
        } else {
            KeywordsListSection(
                filteredKeywords = filteredKeywords,
                people = people,
                places = places,
                selectedTypeFilter = selectedTypeFilter,
                normalizedSearch = normalizedSearch,
                showOnlyNeedsReindex = showOnlyNeedsReindex,
                contentVisible = contentVisible,
                keywordMatchCounts = keywordMatchCounts,
                keywordIndexingIds = keywordIndexingIds,
                fabBottomPadding = fabBottomPadding,
                onNavigateToKeyword = onNavigateToKeyword,
                onToggleEnabled = onToggleEnabled,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}
