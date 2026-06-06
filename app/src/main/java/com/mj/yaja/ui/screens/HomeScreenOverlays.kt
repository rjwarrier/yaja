package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.DueRevisitItem
import java.time.LocalDate

@Composable
fun BoxScope.HomeScreenOverlays(
        showDayLabelDialog: Boolean,
        currentDayLabel: String,
        dayLabelInput: String,
        onDayLabelInputChange: (String) -> Unit,
        onDismissDayLabelDialog: () -> Unit,
        onSaveDayLabel: () -> Unit,
        onRemoveDayLabel: () -> Unit,
        showLabelDialog: Boolean,
        labelInput: String,
        onLabelInputChange: (String) -> Unit,
        onDismissLabelDialog: () -> Unit,
        onConfirmLabelDialog: () -> Unit,
        showUndoDeleteBar: Boolean,
        deletedEntryCount: Int,
        undoCountdownValue: Float,
        onUndoDelete: () -> Unit
) {
        if (showDayLabelDialog) {
                DayLabelDialog(
                        currentDayLabel = currentDayLabel,
                        dayLabelInput = dayLabelInput,
                        onDayLabelInputChange = onDayLabelInputChange,
                        onDismiss = onDismissDayLabelDialog,
                        onSave = onSaveDayLabel,
                        onRemove = onRemoveDayLabel
                )
        }

        if (showLabelDialog) {
                StarLabelDialog(
                        labelInput = labelInput,
                        onLabelInputChange = onLabelInputChange,
                        onDismiss = onDismissLabelDialog,
                        onConfirm = onConfirmLabelDialog
                )
        }

        UndoDeleteBar(
                visible = showUndoDeleteBar,
                deletedEntryCount = deletedEntryCount,
                undoCountdownValue = undoCountdownValue,
                onUndo = onUndoDelete,
                modifier =
                        Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(bottom = 80.dp)
        )
}

@Composable
fun HomeEmptyStateSection(
        selectedDate: LocalDate,
        dayLabel: String,
        dueRevisits: List<DueRevisitItem>,
        allowFutureEntries: Boolean,
        bottomPadding: Dp,
        onOpenDueRevisit: (LocalDate) -> Unit,
        onFutureDateBlocked: () -> Unit,
        onAddEntry: () -> Unit
) {
        Column(
                modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding)
        ) {
                if (dueRevisits.isNotEmpty()) {
                        DueRevisitCard(items = dueRevisits, onOpenDate = onOpenDueRevisit)
                        Spacer(modifier = Modifier.height(12.dp))
                }
                Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                ) {
                        EmptyEntriesState(
                                selectedDate = selectedDate,
                                dayLabel = dayLabel,
                                onIconClick = {
                                        if (
                                                selectedDate.isAfter(LocalDate.now()) &&
                                                        !allowFutureEntries
                                        ) {
                                                onFutureDateBlocked()
                                        } else {
                                                onAddEntry()
                                        }
                                }
                        )
                }
                Spacer(modifier = Modifier.height(80.dp))
        }
}

@Composable
fun HomeScreenDialogs(
        showFutureDateDialog: Boolean,
        selectedDate: LocalDate,
        onDismissFutureDateDialog: () -> Unit,
        onConfirmFutureDateDialog: () -> Unit,
        showCacheAnomalyDialog: Boolean,
        onDismissCacheAnomalyDialog: () -> Unit,
        onAcceptCacheAnomalyRefresh: () -> Unit
) {
        if (showFutureDateDialog) {
                FutureDateDialog(
                        selectedDate = selectedDate,
                        onDismiss = onDismissFutureDateDialog,
                        onConfirm = onConfirmFutureDateDialog
                )
        }

        if (showCacheAnomalyDialog) {
                CacheAnomalyDialog(
                        onDismiss = onDismissCacheAnomalyDialog,
                        onRefresh = onAcceptCacheAnomalyRefresh
                )
        }
}
