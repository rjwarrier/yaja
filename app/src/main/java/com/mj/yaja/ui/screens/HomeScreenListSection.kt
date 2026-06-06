package com.mj.yaja.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.itemSpring
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.scaledDelay
import com.mj.yaja.ui.design.scaledDuration
import com.mj.yaja.data.AnimationPreference
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.DateKeywordEntry
import com.mj.yaja.data.DueRevisitItem
import com.mj.yaja.data.EntryStyle
import com.mj.yaja.data.KeywordDefinition
import java.time.LocalDate
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

@Composable
internal fun HomeLoadedEntriesSection(
        listState: LazyListState,
        reorderState: ReorderableLazyListState,
        reorderedEntryItems: List<HomeEntryListItem>,
        contentPadding: PaddingValues,
        dueRevisits: List<DueRevisitItem>,
        onOpenDueRevisit: (LocalDate) -> Unit,
        enableDragAndDrop: Boolean,
        entryDeleteSelectionEnabled: Boolean,
        showTimestamps: Boolean,
        renderCheckboxesAsText: Boolean,
        isPreviewLimitEnabled: Boolean,
        previewLimitLength: Int,
        onDeleteEntry: (Int) -> Unit,
        onStartEditingEntry: (String, Int) -> Unit,
        selectedEntryItemIds: Set<Long>,
        onClearEntrySelection: () -> Unit,
        onToggleEntrySelection: (Long) -> Unit,
        onStartEntrySelection: (Long) -> Unit,
        onNavigateToAddEntry: () -> Unit,
        selectedDate: LocalDate,
        onDateLinkClick: (LocalDate) -> Unit,
        keywords: List<KeywordDefinition>,
        monthFirst: Boolean,
        customDateKeywords: List<DateKeywordEntry>,
        entryStyle: EntryStyle
) {
        val clearSelectionInteraction = remember { MutableInteractionSource() }
        LazyColumn(
                modifier =
                        Modifier.fillMaxSize()
                                .clickable(
                                        enabled = selectedEntryItemIds.isNotEmpty(),
                                        interactionSource = clearSelectionInteraction,
                                        indication = null,
                                        onClick = onClearEntrySelection
                                ),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = contentPadding
        ) {
                if (dueRevisits.isNotEmpty()) {
                        item {
                                DueRevisitCard(
                                        items = dueRevisits,
                                        onOpenDate = onOpenDueRevisit
                                )
                        }
                }
                itemsIndexed(
                        items = reorderedEntryItems,
                        key = { _, item -> item.id }
                ) { index, item ->
                        ReorderableItem(
                                state = reorderState,
                                key = item.id
                        ) { isDragging ->
                                val scale =
                                        if (isDragging && enableDragAndDrop) {
                                                0.95f
                                        } else {
                                                1f
                                        }
                                val preference = LocalAnimationPreference.current
                                var appeared by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                        if (preference == AnimationPreference.OFF) {
                                                appeared = true
                                        } else {
                                                val scaledDelay = preference.scaledDelay((index * 40).coerceAtMost(200))
                                                delay(scaledDelay.toLong())
                                                appeared = true
                                        }
                                }
                                val baseModifier =
                                        Modifier.animateItem(
                                                fadeInSpec = preference.itemSpring(
                                                        stiffness = Spring.StiffnessLow
                                                ),
                                                fadeOutSpec = preference.itemSpring(
                                                        stiffness = Spring.StiffnessLow
                                                ),
                                                placementSpec = preference.itemSpring(
                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                )
                                        ).graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                        }
                                val itemModifier = baseModifier
                                AnimatedVisibility(
                                        visible = appeared,
                                        enter = preference.enterOrNone(
                                                slideInVertically(
                                                        tween(
                                                                preference.scaledDuration(220),
                                                                easing = FastOutSlowInEasing
                                                        )
                                                ) { it / 3 } + fadeIn(tween(preference.scaledDuration(220)))
                                        ),
                                        modifier = itemModifier
                                ) {
                                        JournalEntryItem(
                                                entry = item.text,
                                                showTimestamps = showTimestamps,
                                                renderCheckboxesAsText = renderCheckboxesAsText,
                                                swipeToDeleteEnabled = false,
                                                isPreviewLimitEnabled = isPreviewLimitEnabled,
                                                previewLimitLength = previewLimitLength,
                                                onDelete = { onDeleteEntry(index) },
                                                onEdit = {
                                                        if (selectedEntryItemIds.isNotEmpty()) {
                                                                onToggleEntrySelection(item.id)
                                                        } else {
                                                                onStartEditingEntry(item.text, index)
                                                                onNavigateToAddEntry()
                                                        }
                                                },
                                                isSelected = item.id in selectedEntryItemIds,
                                                selectionMode = selectedEntryItemIds.isNotEmpty(),
                                                selectionLongPressEnabled =
                                                        entryDeleteSelectionEnabled,
                                                onToggleSelected = { onToggleEntrySelection(item.id) },
                                                onLongSelect = {
                                                        if (entryDeleteSelectionEnabled) {
                                                                onStartEntrySelection(item.id)
                                                        }
                                                },
                                                entryDate = selectedDate,
                                                onDateLinkClick = onDateLinkClick,
                                                keywords = keywords,
                                                monthFirst = monthFirst,
                                                customKeywords = customDateKeywords,
                                                entryStyle = entryStyle
                                        )
                                }
                        }
                }
                item {
                        Spacer(modifier = Modifier.height(80.dp))
                }
        }
}
