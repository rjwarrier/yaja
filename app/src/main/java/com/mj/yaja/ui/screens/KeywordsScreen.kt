package com.mj.yaja.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.KeywordType
import com.mj.yaja.data.NavigationChromeMode
import com.mj.yaja.data.keywords.KeywordCsvCodec
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.rememberAppEntrance
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlinx.coroutines.launch

internal enum class KeywordListSortOption(val label: String) {
    ALPHABETICAL("A-Z"),
    MOST_MENTIONS("Most Mentioned"),
    RECENTLY_ADDED("Recently Added")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeywordsScreen(
    viewModel: JournalViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToKeyword: (String) -> Unit
) {
    val keywords by viewModel.keywords.collectAsStateWithLifecycle()
    val keywordIndexingIds by viewModel.keywordIndexingIds.collectAsStateWithLifecycle()
    val keywordMatchCounts by viewModel.keywordMatchCounts.collectAsStateWithLifecycle()
    val keywordLastIndexedAt by viewModel.keywordLastIndexedAt.collectAsStateWithLifecycle()
    val keywordRebuildProgress by viewModel.keywordRebuildProgress.collectAsStateWithLifecycle()
    val keywordEstimatedRemainingMillis by viewModel.keywordEstimatedRemainingMillis.collectAsStateWithLifecycle()
    val showBottomBar by viewModel.showBottomBar.collectAsStateWithLifecycle()
    val navigationChromeMode by viewModel.navigationChromeMode.collectAsStateWithLifecycle()
    val showBottomPanelLabels by viewModel.showBottomPanelLabels.collectAsStateWithLifecycle()
    val isAnyKeywordIndexing = keywordIndexingIds.isNotEmpty()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fabEntranceTriggered = rememberAppEntrance()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingKeyword by remember { mutableStateOf<KeywordDefinition?>(null) }
    var pendingDeleteKeyword by remember { mutableStateOf<KeywordDefinition?>(null) }
    var contentVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<KeywordType?>(null) }
    var selectedSort by remember { mutableStateOf(KeywordListSortOption.ALPHABETICAL) }
    var showOnlyNeedsReindex by remember { mutableStateOf(false) }
    val fabBottomPadding =
        if (showBottomBar) {
            when (navigationChromeMode) {
                NavigationChromeMode.EXPRESSIVE_PANEL -> {
                    if (showBottomPanelLabels) 92.dp else 76.dp
                }
                NavigationChromeMode.FLOATING_BAR -> {
                    0.dp
                }
            }
        } else {
            0.dp
        }

    val normalizedSearch = remember(searchQuery) { searchQuery.trim().lowercase() }
    val lastIndexedAtSnapshot = keywordLastIndexedAt
    val filteredKeywords = remember(
        keywords,
        keywordMatchCounts,
        keywordIndexingIds,
        lastIndexedAtSnapshot,
        normalizedSearch,
        selectedTypeFilter,
        selectedSort,
        showOnlyNeedsReindex
    ) {
        keywords
            .asSequence()
            .filter { keyword ->
                selectedTypeFilter == null || keyword.type == selectedTypeFilter
            }
            .filter { keyword ->
                if (!showOnlyNeedsReindex) {
                    true
                } else {
                    keyword.id in keywordIndexingIds ||
                        lastIndexedAtSnapshot == null ||
                        keyword.createdAt > lastIndexedAtSnapshot
                }
            }
            .filter { keyword ->
                if (normalizedSearch.isBlank()) {
                    true
                } else {
                    buildList {
                        add(keyword.name)
                        add(keyword.relation)
                        addAll(keyword.aliases)
                    }.any { candidate ->
                        candidate.trim().lowercase().contains(normalizedSearch)
                    }
                }
            }
            .sortedWith(
                when (selectedSort) {
                    KeywordListSortOption.ALPHABETICAL ->
                        compareBy<KeywordDefinition> { it.name.lowercase() }
                    KeywordListSortOption.MOST_MENTIONS ->
                        compareByDescending<KeywordDefinition> { keywordMatchCounts[it.id] ?: 0 }
                            .thenBy { it.name.lowercase() }
                    KeywordListSortOption.RECENTLY_ADDED ->
                        compareByDescending<KeywordDefinition> { it.createdAt }
                            .thenBy { it.name.lowercase() }
                }
            )
            .toList()
    }

    val people = remember(filteredKeywords) {
        filteredKeywords.filter { it.type == KeywordType.PERSON }
    }
    val places = remember(filteredKeywords) {
        filteredKeywords.filter { it.type == KeywordType.PLACE }
    }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let {
                scope.launch {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            OutputStreamWriter(outputStream).use { writer ->
                                writer.write(KeywordCsvCodec.encode(keywords))
                            }
                        }
                        Toast.makeText(context, "Keywords exported", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("KeywordsScreen", "Failed to export keywords", e)
                        Toast.makeText(context, "Couldn't export keywords", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    val templateLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let {
                scope.launch {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            OutputStreamWriter(outputStream).use { writer ->
                                writer.write(KeywordCsvCodec.template())
                            }
                        }
                        Toast.makeText(context, "Template saved", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("KeywordsScreen", "Failed to save keyword template", e)
                        Toast.makeText(context, "Couldn't save template", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                scope.launch {
                    try {
                        val imported = mutableListOf<KeywordDefinition>()
                        context.contentResolver.openInputStream(it)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                reader.lineSequence()
                                    .dropWhile { line -> line.isBlank() }
                                    .forEachIndexed { index, rawLine ->
                                        if (index == 0 && KeywordCsvCodec.isHeader(rawLine)) return@forEachIndexed
                                        KeywordCsvCodec.parseLine(rawLine)?.let(imported::add)
                                    }
                            }
                        }

                        if (imported.isNotEmpty()) {
                            viewModel.importKeywords(imported)
                            Toast.makeText(
                                context,
                                "Imported ${imported.size} keyword(s)",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "No valid keywords found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("KeywordsScreen", "Failed to import keywords", e)
                        Toast.makeText(context, "Couldn't import keywords", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    Scaffold(
        topBar = {
            KeywordsTopBar(onOpenDrawer = onOpenDrawer)
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = fabBottomPadding)
            ) {
                KeywordsFab(
                    visible = fabEntranceTriggered,
                    bottomPadding = 0.dp,
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    ) { paddingValues ->
        AppScreenReveal(
            visible = true,
            key = selectedTypeFilter?.name ?: "all",
            modifier = Modifier.fillMaxSize()
        ) {
            KeywordsScreenContent(
                contentVisible = contentVisible,
                keywords = keywords,
                isAnyKeywordIndexing = isAnyKeywordIndexing,
                onImportCsv = {
                    importLauncher.launch(
                        arrayOf("text/comma-separated-values", "text/csv", "application/csv")
                    )
                },
                onExportCsv = { exportLauncher.launch("keywords.csv") },
                onDownloadTemplate = { templateLauncher.launch("keyword-template.csv") },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedTypeFilter = selectedTypeFilter,
                onSelectedTypeFilterChange = { selectedTypeFilter = it },
                showOnlyNeedsReindex = showOnlyNeedsReindex,
                onShowOnlyNeedsReindexChange = { showOnlyNeedsReindex = it },
                selectedSort = selectedSort,
                onSelectedSortChange = { selectedSort = it },
                keywordRebuildProgress = keywordRebuildProgress,
                keywordEstimatedRemainingMillis = keywordEstimatedRemainingMillis,
                filteredKeywords = filteredKeywords,
                people = people,
                places = places,
                normalizedSearch = normalizedSearch,
                keywordMatchCounts = keywordMatchCounts,
                keywordIndexingIds = keywordIndexingIds,
                fabBottomPadding = fabBottomPadding,
                onNavigateToKeyword = onNavigateToKeyword,
                onToggleEnabled = { id, enabled -> viewModel.setKeywordEnabled(id, enabled) },
                onEdit = { editingKeyword = it },
                onDelete = { pendingDeleteKeyword = it },
                paddingValues = paddingValues
            )
        }
    }

    KeywordScreenDialogs(
        showCreateDialog = showCreateDialog,
        editingKeyword = editingKeyword,
        pendingDeleteKeyword = pendingDeleteKeyword,
        onDismissCreate = { showCreateDialog = false },
        onCreateKeyword = { name, type, relation, aliases, enabled ->
            viewModel.addKeyword(name, type, relation, aliases, enabled)
            showCreateDialog = false
        },
        onDismissEdit = { editingKeyword = null },
        onSaveEditedKeyword = { keyword, name, type, relation, aliases, enabled ->
            viewModel.updateKeyword(
                keyword.copy(
                    name = name,
                    type = type,
                    relation = relation,
                    aliases = aliases,
                    isEnabled = enabled
                )
            )
            editingKeyword = null
        },
        onDismissDelete = { pendingDeleteKeyword = null },
        onConfirmDelete = { keyword ->
            viewModel.deleteKeyword(keyword.id)
            pendingDeleteKeyword = null
        }
    )
}

internal fun buildKeywordProgressLabel(
    progress: Float?,
    estimatedRemainingMillis: Long?
): String {
    val pct = (((progress ?: 0f) * 100).toInt()).coerceIn(0, 100)
    val eta = estimatedRemainingMillis?.let(::formatEta)
    return if (eta != null && pct in 1..99) {
        "$pct% • ~$eta left"
    } else {
        "$pct%"
    }
}

private fun formatEta(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}
