package com.mj.yaja.ui.screens

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.EntryStyle
import com.mj.yaja.data.EntryKind
import com.mj.yaja.data.SwipeDirection
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.extractMentionedEventTime
import com.mj.yaja.data.parseEntryKind
import com.mj.yaja.data.parseEntryRevisitMetadata
import com.mj.yaja.ui.components.CheckboxMarkdownText
import com.mj.yaja.ui.theme.contentTextStyle
import com.mj.yaja.ui.theme.metaSmallTextStyle
import com.mj.yaja.ui.theme.metaTextStyle
import com.mj.yaja.ui.utils.MarkdownUtils
import com.mj.yaja.ui.design.expressivePressMotion
import java.time.LocalDate
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryItem(
        entry: String,
        showTimestamps: Boolean,
        renderCheckboxesAsText: Boolean,
        swipeToDeleteEnabled: Boolean,
        swipeDeleteDirection: SwipeDirection = SwipeDirection.END_TO_START,
        onDelete: () -> Unit,
        onEdit: () -> Unit,
        isPreviewLimitEnabled: Boolean = true,
        previewLimitLength: Int = 200,
        isSelected: Boolean = false,
        selectionMode: Boolean = false,
        selectionLongPressEnabled: Boolean = true,
        onToggleSelected: () -> Unit = {},
        onLongSelect: () -> Unit = {},
        modifier: Modifier = Modifier,
        entryDate: LocalDate? = null,
        onDateLinkClick: ((LocalDate) -> Unit)? = null,
        keywords: List<KeywordDefinition> = emptyList(),
        monthFirst: Boolean = com.mj.yaja.ui.utils.DateLinkUtils.isMonthFirst(),
        customKeywords: List<com.mj.yaja.data.DateKeywordEntry> = emptyList(),
        entryStyle: EntryStyle = EntryStyle.CARDS
) {
        val view = LocalView.current
        var isExpanded by remember { mutableStateOf(false) }
        var isDeleteSwipeArmed by remember { mutableStateOf(false) }

        val dismissState =
                rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                                val matchesDirection =
                                        when (swipeDeleteDirection) {
                                                SwipeDirection.END_TO_START ->
                                                        dismissValue ==
                                                                SwipeToDismissBoxValue.EndToStart
                                                SwipeDirection.START_TO_END ->
                                                        dismissValue ==
                                                                SwipeToDismissBoxValue.StartToEnd
                                        }
                                if (swipeToDeleteEnabled && isDeleteSwipeArmed && matchesDirection) {
                                        if (Build.VERSION.SDK_INT >=
                                                        Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                                        ) {
                                                view.performHapticFeedback(
                                                        HapticFeedbackConstants.SEGMENT_TICK
                                                )
                                        } else {
                                                view.performHapticFeedback(
                                                        HapticFeedbackConstants.KEYBOARD_TAP
                                                )
                                        }
                                        onDelete()
                                        isDeleteSwipeArmed = false
                                        return@rememberSwipeToDismissBoxState true
                                }
                                isDeleteSwipeArmed = false
                                false
                        },
                        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
                )

        LaunchedEffect(isDeleteSwipeArmed) {
                if (isDeleteSwipeArmed) {
                        delay(2500)
                        if (dismissState.currentValue == SwipeToDismissBoxValue.Settled) {
                                isDeleteSwipeArmed = false
                        }
                }
        }

        SwipeToDismissBox(
                state = dismissState,
                modifier = modifier,
                enableDismissFromStartToEnd =
                        swipeToDeleteEnabled &&
                                swipeDeleteDirection == SwipeDirection.START_TO_END,
                enableDismissFromEndToStart =
                        swipeToDeleteEnabled &&
                                swipeDeleteDirection == SwipeDirection.END_TO_START,
                backgroundContent = {
                        if (!swipeToDeleteEnabled || !isDeleteSwipeArmed) return@SwipeToDismissBox

                        val offset =
                                try {
                                        dismissState.requireOffset()
                                } catch (e: Exception) {
                                        0f
                                }
                        val fraction = (offset.absoluteValue / 300f).coerceIn(0f, 1f)

                        val alignment =
                                when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                        else -> Alignment.Center
                                }

                        val color =
                                if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled
                                ) {
                                        MaterialTheme.colorScheme.errorContainer
                                } else {
                                        Color.Transparent
                                }

                        Box(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .background(color)
                                                .padding(horizontal = 24.dp),
                                contentAlignment = alignment
                        ) {
                                if (fraction > 0.05f) {
                                        Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier =
                                                        Modifier.graphicsLayer {
                                                                val s = 0.6f + 0.6f * fraction
                                                                scaleX = s
                                                                scaleY = s
                                                                rotationZ =
                                                                        if (dismissState
                                                                                        .dismissDirection ==
                                                                                        SwipeToDismissBoxValue
                                                                                                .EndToStart
                                                                        ) {
                                                                                -15f *
                                                                                        (1f -
                                                                                                fraction)
                                                                        } else {
                                                                                15f *
                                                                                        (1f -
                                                                                                fraction)
                                                                        }
                                                                alpha = fraction.coerceIn(0f, 1f)
                                                        }
                                        )
                                }
                        }
                },
                content = {
                        val offset =
                                try {
                                        dismissState.requireOffset()
                                } catch (e: Exception) {
                                        0f
                                }
                        val wobbleModifier =
                                if (!swipeToDeleteEnabled && offset != 0f) {
                                        Modifier.graphicsLayer {
                                                // Counteract 80% of the swipe for a "wobble" feel
                                                // in both directions
                                                translationX = -offset * 0.8f
                                        }
                                } else Modifier

                        val timeRegex =
                                Regex(
                                        "^<!--time:(\\d{2}:\\d{2})(?:, added on (.*?))?-->\\n?"
                                )
                        val match = timeRegex.find(entry)
                        val entryKind = parseEntryKind(entry)
                        val isEventEntry = entryKind == EntryKind.EVENT
                        val revisitMetadata = parseEntryRevisitMetadata(entry)
                        val cleanEntry =
                                if (match != null)
                                        MarkdownUtils.stripMetadata(entry.replaceFirst(match.value, ""))
                                else MarkdownUtils.stripMetadata(entry)
                        val mentionedEventTime =
                                if (isEventEntry) extractMentionedEventTime(cleanEntry) else null

                        val isTruncated = isPreviewLimitEnabled && cleanEntry.length > previewLimitLength

                        val interactionSource = remember { MutableInteractionSource() }
                        val commonModifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .then(wobbleModifier)
                                        .expressivePressMotion(interactionSource, pressedScale = 0.98f)
                                        .combinedClickable(
                                                interactionSource = interactionSource,
                                                indication = androidx.compose.foundation.LocalIndication.current,
                                                onClick = {
                                                        isDeleteSwipeArmed = false
                                                        if (selectionMode) {
                                                                onToggleSelected()
                                                        } else if (isTruncated && !isExpanded) {
                                                                isExpanded = true
                                                        } else {
                                                                onEdit()
                                                        }
                                                },
                                                onDoubleClick = {
                                                        if (selectionMode) onToggleSelected() else onEdit()
                                                },
                                                onLongClick = {
                                                        if (!selectionLongPressEnabled) return@combinedClickable
                                                        onLongSelect()
                                                        if (Build.VERSION.SDK_INT >=
                                                                        Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                                                        ) {
                                                                view.performHapticFeedback(
                                                                        HapticFeedbackConstants.GESTURE_START
                                                                )
                                                        } else {
                                                                view.performHapticFeedback(
                                                                        HapticFeedbackConstants.LONG_PRESS
                                                                )
                                                        }
                                                }
                                        )

                        val renderTimestamp: @Composable () -> Unit = {
                                if (showTimestamps && match != null) {
                                        val time = match.groupValues[1]
                                        val addedOn =
                                                match.groupValues[2].takeIf {
                                                        it.isNotEmpty()
                                                }

                                        Surface(
                                                shape = CircleShape,
                                                color =
                                                        MaterialTheme.colorScheme
                                                                .surfaceContainerHigh
                                                                .copy(alpha = 0.72f),
                                                tonalElevation = 0.dp,
                                                shadowElevation = 0.dp,
                                                modifier =
                                                        Modifier.padding(
                                                                bottom = if (isEventEntry) 0.dp else 8.dp,
                                                                top = if (isEventEntry) 12.dp else 0.dp
                                                        )
                                        ) {
                                                Row(
                                                        modifier =
                                                                Modifier.padding(
                                                                        horizontal = 10.dp,
                                                                        vertical = 5.dp
                                                                ),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text = time,
                                                                style =
                                                                        MaterialTheme
                                                                                .typography
                                                                                .metaTextStyle(),
                                                                color =
                                                                        MaterialTheme
                                                                                .colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(
                                                                                        alpha =
                                                                                                0.92f
                                                                                )
                                                        )
                                                        if (addedOn != null) {
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        8.dp
                                                                                )
                                                                )
                                                                Text(
                                                                        text =
                                                                                "added on $addedOn",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .metaSmallTextStyle(),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.72f
                                                                                        )
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }

                        val entryContent: @Composable () -> Unit = {
                                Column {
                                                if (!isEventEntry) {
                                                        renderTimestamp()
                                                }

                                                if (isEventEntry) {
                                                        Row(
                                                                modifier = Modifier.padding(bottom = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                                mentionedEventTime
                                                                        ?.takeIf {
                                                                                val recordedTime =
                                                                                        match?.groupValues?.getOrNull(1)
                                                                                it != recordedTime &&
                                                                                        it !=
                                                                                                "${recordedTime} Hrs"
                                                                        }
                                                                        ?.let { mentionedTime ->
                                                                                Surface(
                                                                                        shape =
                                                                                                RoundedCornerShape(
                                                                                                        12.dp
                                                                                                ),
                                                                                        color =
                                                                                                MaterialTheme.colorScheme.primary,
                                                                                        tonalElevation = 0.dp,
                                                                                        shadowElevation = 0.dp
                                                                                ) {
                                                                                        Row(
                                                                                                modifier =
                                                                                                        Modifier.padding(
                                                                                                                horizontal =
                                                                                                                        12.dp,
                                                                                                                vertical =
                                                                                                                        6.dp
                                                                                                        ),
                                                                                                verticalAlignment =
                                                                                                        Alignment.CenterVertically
                                                                                        ) {
                                                                                                Text(
                                                                                                        text =
                                                                                                                mentionedTime,
                                                                                                        style =
                                                                                                                MaterialTheme.typography.metaSmallTextStyle(),
                                                                                                        color =
                                                                                                                MaterialTheme.colorScheme.onPrimary,
                                                                                                        fontWeight =
                                                                                                                FontWeight.Bold
                                                                                                )
                                                                                        }
                                                                                }
                                                                                Spacer(
                                                                                        modifier =
                                                                                                Modifier.width(
                                                                                                        8.dp
                                                                                                )
                                                                                )
                                                                        }

                                                                Surface(
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        50
                                                                                                .dp
                                                                                ),
                                                                        color =
                                                                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                                                                        alpha = 0.34f
                                                                                ),
                                                                        border =
                                                                                androidx.compose.foundation.BorderStroke(
                                                                                        1.dp,
                                                                                        MaterialTheme.colorScheme.tertiary.copy(
                                                                                                alpha = 0.46f
                                                                                        )
                                                                                ),
                                                                        tonalElevation = 0.dp,
                                                                        shadowElevation = 0.dp
                                                                ) {
                                                                        Row(
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                horizontal =
                                                                                                        9.dp,
                                                                                                vertical =
                                                                                                        4.dp
                                                                                        ),
                                                                                verticalAlignment =
                                                                                        Alignment.CenterVertically
                                                                        ) {
                                                                                Text(
                                                                                        text = "Event",
                                                                                        style =
                                                                                                MaterialTheme.typography.metaSmallTextStyle(),
                                                                                        color =
                                                                                                MaterialTheme.colorScheme.tertiary.copy(
                                                                                                        alpha = 0.82f
                                                                                                ),
                                                                                        fontWeight =
                                                                                                FontWeight.Medium
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }

                                                val truncatedEntry =
                                                        if (isTruncated && !isExpanded) {
                                                                cleanEntry.take(previewLimitLength) + "..."
                                                        } else cleanEntry

                                                CheckboxMarkdownText(
                                                        text = truncatedEntry,
                                                        renderCheckboxesAsText = renderCheckboxesAsText,
                                                        style = MaterialTheme.typography.contentTextStyle(),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        entryDate = entryDate,
                                                        onDateLinkClick = onDateLinkClick,
                                                        keywords = keywords,
                                                        monthFirst = monthFirst,
                                                        customKeywords = customKeywords
                                                )

                                                if (isEventEntry) {
                                                        renderTimestamp()
                                                }
                                }
                        }

                        val flatBarBaseColor =
                                if (isEventEntry) {
                                        MaterialTheme.colorScheme.tertiary
                                } else if (revisitMetadata.revisitOn != null) {
                                        MaterialTheme.colorScheme.secondary
                                } else {
                                        MaterialTheme.colorScheme.primary
                                }
                        val flatBarBlend = when (entry.hashCode().absoluteValue % 4) {
                                0 -> 0.82f
                                1 -> 0.7f
                                2 -> 0.9f
                                else -> 0.76f
                        }
                        val flatBarColor =
                                lerp(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        flatBarBaseColor,
                                        flatBarBlend
                                )

                        if (entryStyle == EntryStyle.CARDS) {
                                ElevatedCard(
                                        modifier = commonModifier,
                                        colors =
                                                CardDefaults.elevatedCardColors(
                                                containerColor =
                                                                if (isSelected) {
                                                                        MaterialTheme.colorScheme
                                                                                .secondaryContainer
                                                                } else if (isEventEntry) {
                                                                        lerp(
                                                                                MaterialTheme.colorScheme.surfaceContainerLow,
                                                                                MaterialTheme.colorScheme.tertiaryContainer,
                                                                                0.12f
                                                                        )
                                                                } else {
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceContainerLow
                                                                }
                                                ),
                                        elevation =
                                                CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                                        shape = MaterialTheme.shapes.large
                                ) {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        start = 22.dp,
                                                                        end = 18.dp,
                                                                        top = 18.dp,
                                                                        bottom = 18.dp
                                                                )
                                                                .height(IntrinsicSize.Min),
                                                verticalAlignment = Alignment.Top
                                        ) {
                                                Box(
                                                        modifier =
                                                                Modifier.width(22.dp)
                                                                        .fillMaxHeight(),
                                                        contentAlignment = Alignment.TopCenter
                                                ) {
                                                        Box(
                                                                modifier =
                                                                        Modifier.width(1.5.dp)
                                                                                .fillMaxHeight()
                                                                                .background(
                                                                                        flatBarBaseColor.copy(
                                                                                                alpha = 0.4f
                                                                                        ),
                                                                                        shape = CircleShape
                                                                                )
                                                        )
                                                        Surface(
                                                                color = flatBarBaseColor,
                                                                shape = CircleShape,
                                                                modifier = Modifier.padding(top = 5.dp)
                                                        ) {
                                                                Box(
                                                                        modifier = Modifier.size(9.dp),
                                                                        contentAlignment = Alignment.Center
                                                                ) {
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                3.dp
                                                                                        )
                                                                                                .background(
                                                                                                        MaterialTheme.colorScheme.onPrimary,
                                                                                                        shape =
                                                                                                                CircleShape
                                                                                                )
                                                                        )
                                                                }
                                                        }
                                                }
                                                Spacer(modifier = Modifier.width(18.dp))
                                                Box(modifier = Modifier.weight(1f)) {
                                                        entryContent()
                                                }
                                        }
                                }
                        } else {
                                Surface(
                                        modifier = commonModifier,
                                        color = MaterialTheme.colorScheme.surface,
                                        border =
                                                if (isSelected) {
                                                        androidx.compose.foundation.BorderStroke(
                                                                1.dp,
                                                                MaterialTheme.colorScheme.primary
                                                        )
                                                } else {
                                                        null
                                                },
                                        tonalElevation = 0.dp,
                                        shadowElevation = 0.dp,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
                                ) {
                                        Row(
                                                modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                                horizontal = 16.dp,
                                                                vertical = 14.dp
                                                        )
                                                        .height(IntrinsicSize.Min),
                                                verticalAlignment = Alignment.Top
                                        ) {
                                                Box(
                                                        modifier =
                                                                Modifier.width(3.dp)
                                                                        .fillMaxHeight()
                                                                        .background(flatBarColor)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Box(
                                                        modifier = Modifier.weight(1f)
                                                ) {
                                                        entryContent()
                                                }
                                        }
                                }
                        }
                }
        )
}
