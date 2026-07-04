package com.mj.yaja.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.applyEntryRevisitMetadata
import com.mj.yaja.data.EntryTemplate
import com.mj.yaja.data.EntryTemplates
import com.mj.yaja.data.EntryKind
import com.mj.yaja.data.applyEntryKindMetadata
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val entryTimeMarkerRegex = Regex("^<!--time:.*?-->\\n?")
private val recordedTimeDisplayRegex = Regex("^(\\d{2}:\\d{2})")
private val recordedTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun extractRecordedTime(rawTimestamp: String?): String? =
        recordedTimeDisplayRegex.find(rawTimestamp.orEmpty())?.groupValues?.getOrNull(1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
        viewModel: JournalViewModel,
        onNavigateBack: () -> Unit,
        onJumpToToday: () -> Unit
) {
        val context = LocalContext.current
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val customShortcodes by viewModel.customShortcodes.collectAsStateWithLifecycle()
        val recentTemplateIds by viewModel.recentTemplateIds.collectAsStateWithLifecycle()
        val favoriteTemplateIds by viewModel.favoriteTemplateIds.collectAsStateWithLifecycle()
        val currentDayLabel by viewModel.currentDayLabel.collectAsStateWithLifecycle()
        val currentRevisitDate by viewModel.currentRevisitDate.collectAsStateWithLifecycle()
        val currentRevisitNote by viewModel.currentRevisitNote.collectAsStateWithLifecycle()
        val dateOrderPreference by viewModel.dateOrderPreference.collectAsStateWithLifecycle()
        val customDateKeywords by viewModel.customDateKeywords.collectAsStateWithLifecycle()
        val entryReviewEnabled by viewModel.entryReviewEnabled.collectAsStateWithLifecycle()
        val keywordHighlightingEnabled by viewModel.keywordHighlightingEnabled.collectAsStateWithLifecycle()
        val keywords by viewModel.keywords.collectAsStateWithLifecycle()
        val favoritedDates by viewModel.favoritedDates.collectAsStateWithLifecycle()
        val editingEntry = uiState.editingEntry
        val motionPreference = LocalAnimationPreference.current
        val today = remember { LocalDate.now() }

        var isEditingMode by remember { mutableStateOf(editingEntry == null) }
        val entryRevisitMetadata =
                remember(editingEntry) {
                        buildEntryRevisitMetadata(editingEntry)
                }
        val shouldUseLegacyDayRevisitFallback =
                remember(editingEntry, uiState.editingIndex, entryRevisitMetadata, currentRevisitDate) {
                        shouldUseLegacyDayRevisitFallback(
                                editingEntry = editingEntry,
                                editingIndex = uiState.editingIndex,
                                entryRevisitMetadata = entryRevisitMetadata,
                                currentRevisitDate = currentRevisitDate
                        )
                }

        val initialText =
                remember(editingEntry) {
                        buildInitialEntryEditorText(
                                editingEntry = editingEntry,
                                entryTimeMarkerRegex = entryTimeMarkerRegex
                        )
                }
        val initialEntryKind =
                remember(editingEntry) { buildInitialEntryKind(editingEntry) }

        var textFieldValue by
                remember(initialText) {
                        mutableStateOf(
                                buildInitialEntryTextFieldValue(initialText)
                        )
                }
        var selectedEntryKind by
                remember(editingEntry, initialEntryKind) { mutableStateOf(initialEntryKind) }
        var selectedListMode by remember { mutableStateOf(ListInsertMode.NONE) }
        val defaultRecordedTime =
                remember(editingEntry) {
                        buildDefaultRecordedTime(
                                editingEntry = editingEntry,
                                recordedTimeFormatter = recordedTimeFormatter,
                                extractRecordedTime = ::extractRecordedTime
                        )
                }
        var selectedRecordedTime by
                remember(editingEntry, defaultRecordedTime) { mutableStateOf(defaultRecordedTime) }
        var showTimePicker by remember { mutableStateOf(false) }
        var showEventInputDialog by remember { mutableStateOf(false) }
        val hasUnsavedChanges =
                textFieldValue.text != initialText ||
                selectedEntryKind != initialEntryKind ||
                selectedRecordedTime != defaultRecordedTime
        var showDiscardDialog by remember { mutableStateOf(false) }
        var showHelpDialog by remember { mutableStateOf(false) }
        var showReviewSheet by remember { mutableStateOf(false) }
        var showTemplateSheet by remember { mutableStateOf(false) }
        var pendingTemplate by remember { mutableStateOf<EntryTemplate?>(null) }
        var showTemplateMergeDialog by remember { mutableStateOf(false) }
        var templatePreview by remember { mutableStateOf<EntryTemplate?>(null) }
        var activeTemplateId by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }
        var mutationLabel by remember { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()
        val entryTemplates = remember { EntryTemplates.builtIns }
        val onTemplateApplied: (String) -> Unit = { templateId ->
                activeTemplateId = templateId
                viewModel.markTemplateUsed(templateId)
        }
        val isFavorited = remember(uiState.selectedDate, favoritedDates) {
                isDateFavorited(
                        selectedDate = uiState.selectedDate,
                        favoritedDates = favoritedDates
                )
        }
        val reviewMentions =
                remember(textFieldValue.text, keywords) {
                        buildReviewMentionSuggestions(textFieldValue.text, keywords)
                }
        val reviewTodos =
                remember(textFieldValue.text) { buildReviewTodoSuggestions(textFieldValue.text) }
        var reviewShouldStarDay by
                remember(uiState.selectedDate, isFavorited) { mutableStateOf(isFavorited) }
        var reviewDayLabelInput by
                remember(uiState.selectedDate, currentDayLabel) {
                        mutableStateOf(currentDayLabel)
                }
        var reviewRevisitDate by
                remember(uiState.selectedDate, entryRevisitMetadata, currentRevisitDate, shouldUseLegacyDayRevisitFallback) {
                        mutableStateOf(
                                buildInitialReviewRevisitDate(
                                        entryRevisitMetadata = entryRevisitMetadata,
                                        currentRevisitDate = currentRevisitDate,
                                        shouldUseLegacyDayRevisitFallback = shouldUseLegacyDayRevisitFallback
                                )
                        )
                }
        var reviewRevisitNote by
                remember(uiState.selectedDate, entryRevisitMetadata, currentRevisitNote, shouldUseLegacyDayRevisitFallback) {
                        mutableStateOf(
                                buildInitialReviewRevisitNote(
                                        entryRevisitMetadata = entryRevisitMetadata,
                                        currentRevisitNote = currentRevisitNote,
                                        shouldUseLegacyDayRevisitFallback = shouldUseLegacyDayRevisitFallback
                                )
                        )
                }

        fun finishTemplateFlow() {
                finishTemplateSelectionFlow(
                        updatePendingTemplate = { pendingTemplate = it },
                        updateShowTemplateMergeDialog = { showTemplateMergeDialog = it },
                        updateShowTemplateSheet = { showTemplateSheet = it }
                )
        }

        fun applyChosenTemplate(template: EntryTemplate, mode: TemplateInsertMode) {
                applyEditorTemplate(
                        template = template,
                        mode = mode,
                        onTemplateUsed = onTemplateApplied,
                        currentValue = textFieldValue,
                        updateValue = { textFieldValue = it },
                        onFinish = { finishTemplateFlow() }
                )
        }

        fun stageTemplateSelection(template: EntryTemplate) {
                stageEditorTemplateSelection(
                        template = template,
                        currentValue = textFieldValue,
                        onApplyImmediately = { applyChosenTemplate(it, TemplateInsertMode.REPLACE) },
                        onRequireMergeChoice = {
                                pendingTemplate = it
                                showTemplateMergeDialog = true
                        }
                )
        }

        fun handleDeleteEntry() {
                if (isSaving) return
                isSaving = true
                mutationLabel = "Deleting..."
                coroutineScope.launch {
                        try {
                                viewModel.deleteEntryAndWait(uiState.editingIndex)
                                viewModel.clearEditing()
                                onNavigateBack()
                        } catch (_: Exception) {
                                Toast.makeText(
                                        context,
                                        "Delete failed. Entry was not removed.",
                                        Toast.LENGTH_SHORT
                                ).show()
                        } finally {
                                isSaving = false
                                mutationLabel = null
                        }
                }
        }

        fun commitSave() {
                if (textFieldValue.text.isBlank() || isSaving) return
                showReviewSheet = false
                isSaving = true
                mutationLabel = buildSaveMutationLabel(editingEntry)
                coroutineScope.launch {
                        try {
                                val finalLabel = reviewDayLabelInput.trim()
                                val finalEntryText =
                                        applyEntryKindMetadata(
                                                applyEntryRevisitMetadata(
                                                        textFieldValue.text,
                                                        reviewRevisitDate,
                                                        reviewRevisitNote.trim()
                                                ),
                                                selectedEntryKind
                                        )

                                if (editingEntry != null) {
                                        viewModel.updateEntry(
                                                finalEntryText,
                                                selectedRecordedTime
                                        )
                                        viewModel.clearEditing()
                                } else {
                                        viewModel.addEntry(
                                                finalEntryText,
                                                selectedRecordedTime
                                        )
                                }
                                if (reviewShouldStarDay) {
                                        viewModel.setStarredWithLabel(uiState.selectedDate, finalLabel)
                                } else {
                                        if (isFavorited) {
                                                viewModel.unStarDate(uiState.selectedDate)
                                        }
                                        if (finalLabel != currentDayLabel) {
                                                viewModel.setDayLabel(uiState.selectedDate, finalLabel)
                                        }
                                }
                                if (shouldUseLegacyDayRevisitFallback) {
                                        viewModel.setRevisit(
                                                uiState.selectedDate,
                                                null,
                                                ""
                                        )
                                }
                                activeTemplateId?.let { templateId ->
                                        viewModel.incrementTemplateUsage(templateId)
                                        if (reviewRevisitDate != null) {
                                                viewModel.incrementTemplateFollowUp(templateId)
                                        }
                                }
                                onNavigateBack()
                        } catch (_: Exception) {
                                Toast.makeText(
                                        context,
                                        "Save failed. Entry was not written.",
                                        Toast.LENGTH_SHORT
                                ).show()
                        } finally {
                                isSaving = false
                                mutationLabel = null
                        }
                }
        }

        fun handlePrimaryAction() {
                resolvePrimaryEditorAction(
                        isEditingMode = isEditingMode,
                        hasText = textFieldValue.text.isNotBlank(),
                        isSaving = isSaving,
                        entryReviewEnabled = entryReviewEnabled,
                        onEnterEditMode = { isEditingMode = true },
                        onOpenReview = { showReviewSheet = true },
                        onCommitSave = { commitSave() }
                )
        }

        BackHandler(enabled = hasUnsavedChanges && !isSaving) { showDiscardDialog = true }

        val dayFormatter = remember { DateTimeFormatter.ofPattern("dd") }
        val monthYearFormatter = remember { DateTimeFormatter.ofPattern("MMMM, yyyy") }
        val weekdayFormatter = remember { DateTimeFormatter.ofPattern("EEEE") }

        AddEntryTransientSheets(
                showHelpDialog = showHelpDialog,
                onDismissHelp = { showHelpDialog = false },
                showTemplateSheet = showTemplateSheet,
                onDismissTemplateSheet = { showTemplateSheet = false },
                entryTemplates = entryTemplates,
                recentTemplateIds = recentTemplateIds,
                favoriteTemplateIds = favoriteTemplateIds,
                onToggleFavoriteTemplate = { templateId ->
                        viewModel.toggleFavoriteTemplate(templateId)
                },
                onTemplatePreview = { template -> templatePreview = template },
                onTemplateSelected = { template -> stageTemplateSelection(template) },
                templatePreview = templatePreview,
                onDismissTemplatePreview = { templatePreview = null },
                onUsePreviewTemplate = { template ->
                        templatePreview = null
                        stageTemplateSelection(template)
                },
                showTemplateMergeDialog = showTemplateMergeDialog,
                pendingTemplate = pendingTemplate,
                onDismissTemplateMerge = {
                        showTemplateMergeDialog = false
                        pendingTemplate = null
                },
                onInsertBelow = { pendingTemplate?.let { applyChosenTemplate(it, TemplateInsertMode.APPEND) } },
                onInsertAtCursor = { pendingTemplate?.let { applyChosenTemplate(it, TemplateInsertMode.AT_CURSOR) } },
                onReplaceDraft = { pendingTemplate?.let { applyChosenTemplate(it, TemplateInsertMode.REPLACE) } },
                showReviewSheet = showReviewSheet,
                onDismissReviewSheet = { showReviewSheet = false },
                reviewTodos = reviewTodos,
                reviewMentions = reviewMentions,
                isStarred = reviewShouldStarDay,
                onStarredChange = { reviewShouldStarDay = it },
                dayLabel = reviewDayLabelInput,
                onDayLabelChange = { reviewDayLabelInput = it.take(30) },
                revisitDate = reviewRevisitDate,
                onRevisitDateChange = { reviewRevisitDate = it },
                revisitNote = reviewRevisitNote,
                onRevisitNoteChange = { reviewRevisitNote = it.take(80) },
                selectedDate = uiState.selectedDate,
                onExtractTodos = {
                        AddEntryReviewExtractAction(
                                textFieldValue = textFieldValue,
                                onValueChange = { textFieldValue = it }
                        )
                },
                onSave = { commitSave() },
                showTimePicker = showTimePicker,
                initialTime = selectedRecordedTime,
                onDismissTimePicker = { showTimePicker = false },
                onConfirmTimePicker = { pickedTime ->
                        selectedRecordedTime = pickedTime
                        showTimePicker = false
                        if (!isEditingMode) {
                                isEditingMode = true
                        }
                },
                showDiscardDialog = showDiscardDialog,
                onDismissDiscardDialog = { showDiscardDialog = false },
                onConfirmDiscard = {
                        showDiscardDialog = false
                        viewModel.clearEditing()
                        onNavigateBack()
                }
        )

        if (showEventInputDialog) {
                EventInputDialog(
                        onDismiss = { showEventInputDialog = false },
                        onConfirm = { time, title, description, isAllDay ->
                                showEventInputDialog = false
                                selectedEntryKind = EntryKind.EVENT
                                val eventText =
                                        if (isAllDay) {
                                                "$title\n\n$description"
                                        } else {
                                                "$time : $title\n\n$description"
                                        }
                                textFieldValue = insertEditorSnippetAtCursor(textFieldValue, eventText)
                        }
                )
        }

        Scaffold(
                topBar = {
                        val headerMode = buildAddEntryHeaderMode(editingEntry, isEditingMode)
                        AddEntryTopSection(
                                headerMode = headerMode,
                                isEditingMode = isEditingMode,
                                isSaving = isSaving,
                                canDelete = editingEntry != null,
                                showJumpToToday = uiState.selectedDate != today,
                                onBack = {
                                        if (isSaving) {
                                                return@AddEntryTopSection
                                        } else if (hasUnsavedChanges) {
                                                showDiscardDialog = true
                                        } else {
                                                viewModel.clearEditing()
                                                onNavigateBack()
                                        }
                                },
                                onShowHelp = { showHelpDialog = true },
                                onShowTemplates = { showTemplateSheet = true },
                                onDelete = { handleDeleteEntry() },
                                onJumpToToday = {
                                        if (isSaving) return@AddEntryTopSection
                                        viewModel.clearEditing()
                                        onJumpToToday()
                                },
                                shareText = textFieldValue.text,
                                selectedDate = uiState.selectedDate,
                                dayLabel = currentDayLabel,
                                recordedTime = selectedRecordedTime,
                                selectedEntryKind = selectedEntryKind,
                                entryText = textFieldValue.text,
                                onRecordedTimeClick = { showTimePicker = true },
                                dayFormatter = dayFormatter,
                                weekdayFormatter = weekdayFormatter,
                                monthYearFormatter = monthYearFormatter
                        )
                },
                bottomBar = {},
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.ime
        ) { paddingValues ->
                AddEntryBodySection(
                        paddingValues = paddingValues,
                        isEditingMode = isEditingMode,
                        textFieldValue = textFieldValue,
                        onTextFieldValueChange = { textFieldValue = it },
                        selectedDate = uiState.selectedDate,
                        keywords = keywords,
                        keywordHighlightingEnabled = keywordHighlightingEnabled,
                        dateOrderPreference = dateOrderPreference,
                        customDateKeywords = customDateKeywords,
                        onDateClick = { date ->
                                viewModel.selectDate(date)
                                onNavigateBack()
                        },
                        customShortcodes = customShortcodes,
                        motionPreference = motionPreference,
                        isSaving = isSaving,
                        mutationLabel = mutationLabel,
                        hasContentToSave = textFieldValue.text.isNotBlank(),
                        selectedEntryKind = selectedEntryKind,
                        selectedListMode = selectedListMode,
                        onSelectEvent = {
                                showEventInputDialog = true
                                selectedListMode = ListInsertMode.NONE
                        },
                        onInsertTodo = {
                                selectedEntryKind = EntryKind.NORMAL
                                selectedListMode = ListInsertMode.NONE
                                textFieldValue =
                                        insertEditorSnippetAtCursor(
                                                textFieldValue,
                                                "[ ] "
                                        )
                        },
                        onToggleNumberedList = {
                                selectedListMode =
                                        if (selectedListMode == ListInsertMode.NUMBERED) {
                                                ListInsertMode.NONE
                                        } else {
                                                ListInsertMode.NUMBERED
                                        }
                        },
                        onToggleBulletedList = {
                                selectedListMode =
                                        if (selectedListMode == ListInsertMode.BULLETED) {
                                                ListInsertMode.NONE
                                        } else {
                                                ListInsertMode.BULLETED
                                        }
                        },
                        onInsertNumericList = {
                                textFieldValue =
                                        insertEditorSnippetAtCursor(
                                                textFieldValue,
                                                "1. "
                                        )
                        },
                        onInsertAlphabeticList = {
                                textFieldValue =
                                        insertEditorSnippetAtCursor(
                                                textFieldValue,
                                                "a. "
                                        )
                        },
                        onInsertPlusBullet = {
                                textFieldValue =
                                        insertEditorSnippetAtCursor(
                                                textFieldValue,
                                                "+ "
                                        )
                        },
                        onInsertQuoteBullet = {
                                textFieldValue =
                                        insertEditorSnippetAtCursor(
                                                textFieldValue,
                                                "> "
                                        )
                        },
                        onBold = {
                                applyFormatting(
                                        textFieldValue,
                                        "**",
                                        "**"
                                ) { textFieldValue = it }
                        },
                        onItalic = {
                                applyFormatting(
                                        textFieldValue,
                                        "*",
                                        "*"
                                ) { textFieldValue = it }
                        },
                        onPrimaryAction = { handlePrimaryAction() }
                )
        }
}



