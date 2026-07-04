package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.data.DateKeywordEntry
import com.mj.yaja.data.DateOrderPreference
import com.mj.yaja.data.EntryKind
import com.mj.yaja.data.EntryRevisitMetadata
import com.mj.yaja.data.EntryTemplate
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.parseEntryKind
import com.mj.yaja.data.parseEntryRevisitMetadata
import com.mj.yaja.data.stripEntryKindMetadata
import com.mj.yaja.data.stripEntryRevisitMetadata
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEntryTransientSheets(
    showHelpDialog: Boolean,
    onDismissHelp: () -> Unit,
    showTemplateSheet: Boolean,
    onDismissTemplateSheet: () -> Unit,
    entryTemplates: List<EntryTemplate>,
    recentTemplateIds: List<String>,
    favoriteTemplateIds: Set<String>,
    onToggleFavoriteTemplate: (String) -> Unit,
    onTemplatePreview: (EntryTemplate) -> Unit,
    onTemplateSelected: (EntryTemplate) -> Unit,
    templatePreview: EntryTemplate?,
    onDismissTemplatePreview: () -> Unit,
    onUsePreviewTemplate: (EntryTemplate) -> Unit,
    showTemplateMergeDialog: Boolean,
    pendingTemplate: EntryTemplate?,
    onDismissTemplateMerge: () -> Unit,
    onInsertBelow: () -> Unit,
    onInsertAtCursor: () -> Unit,
    onReplaceDraft: () -> Unit,
    showReviewSheet: Boolean,
    onDismissReviewSheet: () -> Unit,
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
    onSave: () -> Unit,
    showTimePicker: Boolean,
    initialTime: String,
    onDismissTimePicker: () -> Unit,
    onConfirmTimePicker: (String) -> Unit,
    showDiscardDialog: Boolean,
    onDismissDiscardDialog: () -> Unit,
    onConfirmDiscard: () -> Unit
) {
    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDismiss = onDismissDiscardDialog,
            onConfirmDiscard = onConfirmDiscard
        )
    }

    if (showHelpDialog) {
        AddEntryHelpDialog(onDismiss = onDismissHelp)
    }

    if (showTemplateSheet) {
        ModalBottomSheet(onDismissRequest = onDismissTemplateSheet) {
            EntryTemplateSheet(
                templates = entryTemplates,
                recentTemplateIds = recentTemplateIds,
                favoriteTemplateIds = favoriteTemplateIds,
                onToggleFavorite = onToggleFavoriteTemplate,
                onTemplatePreview = onTemplatePreview,
                onTemplateSelected = onTemplateSelected
            )
        }
    }

    if (templatePreview != null) {
        ModalBottomSheet(onDismissRequest = onDismissTemplatePreview) {
            TemplatePreviewSheet(
                template = templatePreview,
                isFavorited = favoriteTemplateIds.contains(templatePreview.id),
                onToggleFavorite = { onToggleFavoriteTemplate(templatePreview.id) },
                onUseTemplate = { onUsePreviewTemplate(templatePreview) }
            )
        }
    }

    if (showTemplateMergeDialog && pendingTemplate != null) {
        TemplateMergeDialog(
            template = pendingTemplate,
            onDismiss = onDismissTemplateMerge,
            onInsertBelow = onInsertBelow,
            onInsertAtCursor = onInsertAtCursor,
            onReplaceDraft = onReplaceDraft
        )
    }

    if (showReviewSheet) {
        ModalBottomSheet(onDismissRequest = onDismissReviewSheet) {
            EntryReviewSheet(
                reviewTodos = reviewTodos,
                reviewMentions = reviewMentions,
                isStarred = isStarred,
                onStarredChange = onStarredChange,
                dayLabel = dayLabel,
                onDayLabelChange = onDayLabelChange,
                revisitDate = revisitDate,
                onRevisitDateChange = onRevisitDateChange,
                revisitNote = revisitNote,
                onRevisitNoteChange = onRevisitNoteChange,
                selectedDate = selectedDate,
                onExtractTodos = onExtractTodos,
                onSave = onSave
            )
        }
    }

    if (showTimePicker) {
        EntryTimePickerDialog(
            initialTime = initialTime,
            onDismiss = onDismissTimePicker,
            onConfirm = onConfirmTimePicker
        )
    }
}

@Composable
internal fun AddEntryTopSection(
    headerMode: String,
    isEditingMode: Boolean,
    isSaving: Boolean,
    canDelete: Boolean,
    showJumpToToday: Boolean,
    onBack: () -> Unit,
    onShowHelp: () -> Unit,
    onShowTemplates: () -> Unit,
    onDelete: () -> Unit,
    onJumpToToday: () -> Unit,
    shareText: String,
    selectedDate: LocalDate,
    dayLabel: String,
    recordedTime: String,
    selectedEntryKind: EntryKind,
    entryText: String,
    onRecordedTimeClick: () -> Unit,
    dayFormatter: DateTimeFormatter,
    weekdayFormatter: DateTimeFormatter,
    monthYearFormatter: DateTimeFormatter
) {
    Column {
        AddEntryTopBar(
            headerMode = headerMode,
            isEditingMode = isEditingMode,
            isSaving = isSaving,
            canDelete = canDelete,
            showJumpToToday = showJumpToToday,
            onBack = onBack,
            onShowHelp = onShowHelp,
            onShowTemplates = onShowTemplates,
            onDelete = onDelete,
            onJumpToToday = onJumpToToday,
            shareText = shareText
        )

        EditorDateHeaderCard(
            selectedDate = selectedDate,
            dayLabel = dayLabel,
            recordedTime = recordedTime,
            selectedEntryKind = selectedEntryKind,
            entryText = entryText,
            isEditingMode = isEditingMode,
            onRecordedTimeClick = onRecordedTimeClick,
            dayFormatter = dayFormatter,
            weekdayFormatter = weekdayFormatter,
            monthYearFormatter = monthYearFormatter
        )
    }
}

internal fun AddEntryReviewExtractAction(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    val updatedText = convertReviewTodosToChecklist(textFieldValue.text)
    onValueChange(
        textFieldValue.copy(
            text = updatedText,
            selection = TextRange(updatedText.length)
        )
    )
}

internal fun buildInitialEntryEditorText(
    editingEntry: String?,
    entryTimeMarkerRegex: Regex
): String {
    var text = editingEntry ?: ""
    text = stripEntryRevisitMetadata(text)
    text = stripEntryKindMetadata(text)
    text = text.replace(entryTimeMarkerRegex, "")
    return text
}

internal fun buildInitialEntryKind(
    editingEntry: String?
): EntryKind =
    editingEntry?.let(::parseEntryKind) ?: EntryKind.NORMAL

internal fun buildInitialEntryTextFieldValue(
    initialText: String
): TextFieldValue =
    TextFieldValue(
        initialText,
        selection = TextRange(initialText.length)
    )

internal fun buildAddEntryHeaderMode(
    editingEntry: String?,
    isEditingMode: Boolean
): String =
    when {
        editingEntry == null -> "New Entry"
        isEditingMode -> "Edit Entry"
        else -> "View Entry"
    }

internal fun buildSaveMutationLabel(
    editingEntry: String?
): String =
    if (editingEntry != null) {
        "Updating..."
    } else {
        "Saving..."
    }

internal fun buildDefaultRecordedTime(
    editingEntry: String?,
    recordedTimeFormatter: DateTimeFormatter,
    extractRecordedTime: (String?) -> String?
): String {
    val timeMatch = editingEntry?.let { Regex("<!--time:(.*?)-->").find(it) }
    val timeStr = timeMatch?.groupValues?.get(1)
    return extractRecordedTime(timeStr) ?: LocalTime.now().format(recordedTimeFormatter)
}

internal fun buildEntryRevisitMetadata(
    editingEntry: String?
): EntryRevisitMetadata =
    editingEntry?.let(::parseEntryRevisitMetadata) ?: EntryRevisitMetadata()

internal fun shouldUseLegacyDayRevisitFallback(
    editingEntry: String?,
    editingIndex: Int,
    entryRevisitMetadata: EntryRevisitMetadata,
    currentRevisitDate: LocalDate?
): Boolean =
    editingEntry != null &&
        editingIndex == 0 &&
        entryRevisitMetadata.revisitOn == null &&
        currentRevisitDate != null

internal fun isDateFavorited(
    selectedDate: LocalDate,
    favoritedDates: Set<String>
): Boolean = favoritedDates.contains(selectedDate.toString())

internal fun buildInitialReviewRevisitDate(
    entryRevisitMetadata: EntryRevisitMetadata,
    currentRevisitDate: LocalDate?,
    shouldUseLegacyDayRevisitFallback: Boolean
): LocalDate? =
    entryRevisitMetadata.revisitOn
        ?: if (shouldUseLegacyDayRevisitFallback) currentRevisitDate else null

internal fun buildInitialReviewRevisitNote(
    entryRevisitMetadata: EntryRevisitMetadata,
    currentRevisitNote: String,
    shouldUseLegacyDayRevisitFallback: Boolean
): String =
    if (entryRevisitMetadata.revisitOn != null) {
        entryRevisitMetadata.note
    } else if (shouldUseLegacyDayRevisitFallback) {
        currentRevisitNote
    } else {
        ""
    }

internal fun resolvePrimaryEditorAction(
    isEditingMode: Boolean,
    hasText: Boolean,
    isSaving: Boolean,
    entryReviewEnabled: Boolean,
    onEnterEditMode: () -> Unit,
    onOpenReview: () -> Unit,
    onCommitSave: () -> Unit
) {
    if (!isEditingMode) {
        onEnterEditMode()
    } else if (hasText && !isSaving) {
        if (entryReviewEnabled) {
            onOpenReview()
        } else {
            onCommitSave()
        }
    }
}

internal fun finishTemplateSelectionFlow(
    updatePendingTemplate: (EntryTemplate?) -> Unit,
    updateShowTemplateMergeDialog: (Boolean) -> Unit,
    updateShowTemplateSheet: (Boolean) -> Unit
) {
    updatePendingTemplate(null)
    updateShowTemplateMergeDialog(false)
    updateShowTemplateSheet(false)
}

internal fun applyEditorTemplate(
    template: EntryTemplate,
    mode: TemplateInsertMode,
    onTemplateUsed: (String) -> Unit,
    currentValue: TextFieldValue,
    updateValue: (TextFieldValue) -> Unit,
    onFinish: () -> Unit
) {
    applyTemplate(
        template = template,
        mode = mode,
        onTemplateUsed = onTemplateUsed,
        currentValue = currentValue,
        updateValue = updateValue,
        onFinish = onFinish
    )
}

internal fun stageEditorTemplateSelection(
    template: EntryTemplate,
    currentValue: TextFieldValue,
    onApplyImmediately: (EntryTemplate) -> Unit,
    onRequireMergeChoice: (EntryTemplate) -> Unit
) {
    if (currentValue.text.isBlank()) {
        onApplyImmediately(template)
    } else {
        onRequireMergeChoice(template)
    }
}

@Composable
internal fun AddEntryBodySection(
    paddingValues: PaddingValues,
    isEditingMode: Boolean,
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    selectedDate: LocalDate,
    keywords: List<KeywordDefinition>,
    keywordHighlightingEnabled: Boolean,
    dateOrderPreference: DateOrderPreference,
    customDateKeywords: List<DateKeywordEntry>,
    onDateClick: (LocalDate) -> Unit,
    customShortcodes: Map<String, String>,
    motionPreference: AnimationPreference,
    isSaving: Boolean,
    mutationLabel: String?,
    hasContentToSave: Boolean,
    selectedEntryKind: EntryKind,
    selectedListMode: ListInsertMode,
    onSelectEvent: () -> Unit,
    onInsertTodo: () -> Unit,
    onToggleNumberedList: () -> Unit,
    onToggleBulletedList: () -> Unit,
    onInsertNumericList: () -> Unit,
    onInsertAlphabeticList: () -> Unit,
    onInsertPlusBullet: () -> Unit,
    onInsertQuoteBullet: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onPrimaryAction: () -> Unit
) {
    val isFormattingBarVisible = isEditingMode && !textFieldValue.selection.collapsed

    Box(
        modifier =
            Modifier.fillMaxSize()
                .padding(paddingValues)
                .then(
                    if (isFormattingBarVisible) Modifier
                    else Modifier.navigationBarsPadding()
                )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier.fillMaxSize().padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                if (!isEditingMode) {
                    EntryViewBody(
                        text = textFieldValue.text,
                        entryDate = selectedDate,
                        keywords = keywords,
                        keywordHighlightingEnabled = keywordHighlightingEnabled,
                        dateOrderPreference = dateOrderPreference,
                        customDateKeywords = customDateKeywords,
                        onDateClick = onDateClick,
                        modifier =
                            Modifier.fillMaxWidth()
                                .weight(1f)
                    )
                } else {
                    EntryQuickInsertChips(
                        selectedEntryKind = selectedEntryKind,
                        selectedListMode = selectedListMode,
                        onSelectEvent = onSelectEvent,
                        onInsertTodo = onInsertTodo,
                        onToggleNumberedList = onToggleNumberedList,
                        onToggleBulletedList = onToggleBulletedList,
                        onInsertNumericList = onInsertNumericList,
                        onInsertAlphabeticList = onInsertAlphabeticList,
                        onInsertPlusBullet = onInsertPlusBullet,
                        onInsertQuoteBullet = onInsertQuoteBullet,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    EntryEditorField(
                        value = textFieldValue,
                        customShortcodes = customShortcodes,
                        onValueChange = onTextFieldValueChange,
                        autoFocus = isEditingMode,
                        modifier =
                            Modifier.fillMaxWidth()
                                .weight(1f)
                    )
                }

                EntryCountFooter(
                    text = textFieldValue.text,
                    motionPreference = motionPreference,
                    modifier =
                        Modifier.padding(bottom = 16.dp)
                            .align(Alignment.CenterHorizontally)
                )
            }

            AddEntryFloatingChrome(
                isFormattingBarVisible = isFormattingBarVisible,
                motionPreference = motionPreference,
                onBold = onBold,
                onItalic = onItalic,
                isSaving = isSaving,
                mutationLabel = mutationLabel,
                isEditingMode = isEditingMode,
                hasContentToSave = hasContentToSave,
                onPrimaryAction = onPrimaryAction
            )
        }
    }
}
