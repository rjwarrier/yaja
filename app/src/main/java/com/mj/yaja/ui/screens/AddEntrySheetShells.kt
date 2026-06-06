package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.EntryTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EntryReviewSheet(
        reviewTodos: List<ReviewTodoSuggestion>,
        reviewMentions: List<ReviewMentionSuggestion>,
        isStarred: Boolean,
        onStarredChange: (Boolean) -> Unit,
        dayLabel: String,
        onDayLabelChange: (String) -> Unit,
        revisitDate: LocalDate?,
        onRevisitDateChange: (LocalDate?) -> Unit,
        revisitNote: String,
        onRevisitNoteChange: (String) -> Unit,
        selectedDate: LocalDate,
        onExtractTodos: () -> Unit,
        onSave: () -> Unit
) {
        var showRevisitDatePicker by remember { mutableStateOf(false) }
        val reviewDateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
        val revisitPickerState =
                androidx.compose.material3.rememberDatePickerState(
                        initialSelectedDateMillis =
                                (revisitDate ?: selectedDate.plusDays(1))
                                        .atStartOfDay(java.time.ZoneOffset.UTC)
                                        .toInstant()
                                        .toEpochMilli(),
                        yearRange = IntRange(selectedDate.year, selectedDate.year + 10),
                        selectableDates =
                                object : androidx.compose.material3.SelectableDates {
                                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                                val picked =
                                                        java.time.Instant.ofEpochMilli(
                                                                        utcTimeMillis
                                                                )
                                                                .atZone(java.time.ZoneOffset.UTC)
                                                                .toLocalDate()
                                                return !picked.isBefore(selectedDate.plusDays(1))
                                        }
                                }
                )

        if (showRevisitDatePicker) {
                DatePickerDialog(
                        onDismissRequest = { showRevisitDatePicker = false },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                revisitPickerState.selectedDateMillis?.let { millis ->
                                                        val picked =
                                                                java.time.Instant.ofEpochMilli(millis)
                                                                        .atZone(java.time.ZoneOffset.UTC)
                                                                        .toLocalDate()
                                                        onRevisitDateChange(picked)
                                                }
                                                showRevisitDatePicker = false
                                        }
                                ) { Text("Set") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showRevisitDatePicker = false }) {
                                        Text("Cancel")
                                }
                        }
                ) {
                        DatePicker(state = revisitPickerState, showModeToggle = false)
                }
        }

        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
                Text(
                        text = "Review Entry",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        text =
                                "Quick pass before save. Pull todos out, check tracked mentions, and mark day if it matters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        AssistChip(
                                onClick = {},
                                label = {
                                        Text(
                                                "${reviewTodos.size} todo${if (reviewTodos.size == 1) "" else "s"}"
                                        )
                                }
                        )
                        AssistChip(
                                onClick = {},
                                label = {
                                        Text(
                                                "${reviewMentions.size} mention${if (reviewMentions.size == 1) "" else "s"}"
                                        )
                                }
                        )
                        AssistChip(
                                onClick = {},
                                label = { Text(selectedDate.format(reviewDateFormatter)) }
                        )
                }

                ReviewTodoSummaryCard(
                        reviewTodos = reviewTodos,
                        onExtractTodos = onExtractTodos
                )

                ReviewMentionsCard(reviewMentions = reviewMentions)

                ReviewDayMemoryCard(
                        isStarred = isStarred,
                        onStarredChange = onStarredChange,
                        dayLabel = dayLabel,
                        onDayLabelChange = onDayLabelChange,
                        revisitDate = revisitDate,
                        onRevisitDateChange = onRevisitDateChange,
                        revisitNote = revisitNote,
                        onRevisitNoteChange = onRevisitNoteChange,
                        selectedDate = selectedDate,
                        reviewDateFormatter = reviewDateFormatter,
                        onShowCustomDatePicker = { showRevisitDatePicker = true }
                )

                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                        Text("Save Entry")
                }
                Spacer(modifier = Modifier.height(12.dp))
        }
}

enum class TemplateInsertMode {
        REPLACE,
        APPEND,
        AT_CURSOR
}

fun applyTemplate(
        template: EntryTemplate,
        mode: TemplateInsertMode,
        onTemplateUsed: (String) -> Unit,
        currentValue: TextFieldValue,
        updateValue: (TextFieldValue) -> Unit,
        onFinish: () -> Unit
) {
        val templateBody = template.body
        val updatedText: String
        val cursorPosition: Int

        when (mode) {
                TemplateInsertMode.REPLACE -> {
                        updatedText = templateBody
                        cursorPosition = firstTemplateCursorPosition(updatedText)
                }
                TemplateInsertMode.APPEND -> {
                        val prefix =
                                if (currentValue.text.isBlank()) ""
                                else currentValue.text.trimEnd() + "\n\n"
                        updatedText = prefix + templateBody
                        cursorPosition = prefix.length + firstTemplateCursorPosition(templateBody)
                }
                TemplateInsertMode.AT_CURSOR -> {
                        val cursor =
                                currentValue.selection.end.coerceIn(0, currentValue.text.length)
                        val before = currentValue.text.substring(0, cursor)
                        val after = currentValue.text.substring(cursor)
                        val needsLeadingBreak = before.isNotEmpty() && !before.endsWith("\n")
                        val needsTrailingBreak = after.isNotEmpty() && !after.startsWith("\n")
                        val insertion =
                                buildString {
                                        if (needsLeadingBreak) append("\n\n")
                                        append(templateBody)
                                        if (needsTrailingBreak) append("\n\n")
                                }
                        updatedText = before + insertion + after
                        val insertionStart = before.length + if (needsLeadingBreak) 2 else 0
                        cursorPosition =
                                insertionStart + firstTemplateCursorPosition(templateBody)
                }
        }

        updateValue(
                currentValue.copy(
                        text = updatedText,
                        selection = androidx.compose.ui.text.TextRange(cursorPosition)
                )
        )
        onTemplateUsed(template.id)
        onFinish()
}

fun firstTemplateCursorPosition(text: String): Int {
        val match = Regex("(?m)^\\+\\s+Add ").find(text)
        if (match != null) {
                        return match.range.first + 2
        }
        val firstBlank = text.indexOf("\n\n").takeIf { it >= 0 } ?: text.length
        return firstBlank
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryTemplateSheet(
        templates: List<EntryTemplate>,
        recentTemplateIds: List<String>,
        favoriteTemplateIds: Set<String>,
        onToggleFavorite: (String) -> Unit,
        onTemplatePreview: (EntryTemplate) -> Unit,
        onTemplateSelected: (EntryTemplate) -> Unit
) {
        var searchQuery by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("All") }
        var sortMode by remember { mutableStateOf("Recommended") }
        val categories =
                remember(templates) {
                        listOf("All") + templates.map { it.category }.distinct().sorted()
                }
        val filteredTemplates =
                remember(templates, searchQuery, selectedCategory, sortMode, recentTemplateIds, favoriteTemplateIds) {
                        val query = searchQuery.trim()
                        templates.filter { template ->
                                        (selectedCategory == "All" || template.category == selectedCategory) &&
                                                (query.isBlank() ||
                                                        templateSearchText(template).contains(
                                                                query,
                                                                ignoreCase = true
                                                        ))
                                }
                                .let { current ->
                                        when (sortMode) {
                                                "A-Z" -> current.sortedBy { it.name }
                                                "Recent" ->
                                                        current.sortedBy {
                                                                recentTemplateIds.indexOf(it.id)
                                                                        .takeIf { idx -> idx >= 0 }
                                                                        ?: Int.MAX_VALUE
                                                        }
                                                else ->
                                                        current.sortedByDescending {
                                                                templateScore(
                                                                        template = it,
                                                                        recentTemplateIds = recentTemplateIds,
                                                                        favoriteTemplateIds = favoriteTemplateIds
                                                                )
                                                        }
                                        }
                                }
                }
        val quickPicks =
                remember(templates, favoriteTemplateIds, recentTemplateIds) {
                        templates.sortedByDescending {
                                        templateScore(
                                                template = it,
                                                recentTemplateIds = recentTemplateIds,
                                                favoriteTemplateIds = favoriteTemplateIds
                                        )
                                }
                                .take(4)
                }
        val groupedTemplates =
                remember(filteredTemplates, selectedCategory) {
                        if (selectedCategory == "All") {
                                filteredTemplates.groupBy { it.category }
                        } else {
                                linkedMapOf(selectedCategory to filteredTemplates)
                        }
                }

        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                Text(
                        text = "Choose a Template",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        text = "${filteredTemplates.size} of ${templates.size} templates ready",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search templates") },
                        placeholder = { Text("Meeting, health, reflection...") },
                        leadingIcon = {
                                Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = null
                                )
                        },
                        trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                                Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Clear search"
                                                )
                                        }
                                }
                        }
                )
                TemplateStartBlankCard(onTemplateSelected = onTemplateSelected)
                TemplateQuickPicksSection(
                        quickPicks = quickPicks,
                        favoriteTemplateIds = favoriteTemplateIds,
                        onToggleFavorite = onToggleFavorite,
                        onTemplatePreview = onTemplatePreview,
                        onTemplateSelected = onTemplateSelected
                )
                TemplateCategoryChips(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                )
                TemplateSortChips(
                        sortMode = sortMode,
                        onSortModeSelected = { sortMode = it }
                )
                if (filteredTemplates.isEmpty()) {
                        TemplateEmptyResultsCard()
                } else {
                        TemplateGroupedResultsSection(
                                groupedTemplates = groupedTemplates,
                                sortMode = sortMode,
                                recentTemplateIds = recentTemplateIds,
                                favoriteTemplateIds = favoriteTemplateIds,
                                onToggleFavorite = onToggleFavorite,
                                onTemplatePreview = onTemplatePreview,
                                onTemplateSelected = onTemplateSelected
                        )
                }
                Spacer(modifier = Modifier.height(12.dp))
        }
}
