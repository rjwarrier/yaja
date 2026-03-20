package com.mj.yaja.ui.screens

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.SwipeDirection
import com.mj.yaja.ui.utils.MarkdownUtils
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryItem(
        entry: String,
        showTimestamps: Boolean,
        swipeToDeleteEnabled: Boolean,
        swipeDeleteDirection: SwipeDirection = SwipeDirection.END_TO_START,
        onDelete: () -> Unit,
        onEdit: () -> Unit,
        isPreviewLimitEnabled: Boolean = true,
        previewLimitLength: Int = 200,
        modifier: Modifier = Modifier
) {
        val view = LocalView.current
        var isExpanded by remember { mutableStateOf(false) }

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
                                if (swipeToDeleteEnabled && matchesDirection) {
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
                                        return@rememberSwipeToDismissBoxState true
                                }
                                false
                        },
                        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
                )

        SwipeToDismissBox(
                state = dismissState,
                modifier = modifier,
                enableDismissFromStartToEnd =
                        !swipeToDeleteEnabled ||
                                swipeDeleteDirection == SwipeDirection.START_TO_END,
                enableDismissFromEndToStart =
                        !swipeToDeleteEnabled ||
                                swipeDeleteDirection == SwipeDirection.END_TO_START,
                backgroundContent = {
                        if (!swipeToDeleteEnabled) return@SwipeToDismissBox

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
                        val cleanEntry =
                                if (match != null)
                                        entry.replaceFirst(match.value, "")
                                else entry

                        val isTruncated = isPreviewLimitEnabled && cleanEntry.length > previewLimitLength

                        ElevatedCard(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(horizontal = 24.dp)
                                                .then(wobbleModifier)
                                                .combinedClickable(
                                                        onClick = {
                                                                if (isTruncated && !isExpanded) {
                                                                        isExpanded = true
                                                                } else {
                                                                        onEdit()
                                                                }
                                                        },
                                                        onDoubleClick = {
                                                                onEdit()
                                                        }
                                                ),
                                colors =
                                        CardDefaults.elevatedCardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme
                                                                .surfaceContainerLow
                                        ),
                                elevation =
                                        CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                                shape = MaterialTheme.shapes.medium
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.Top
                                ) {
                                        Box(
                                                modifier =
                                                        Modifier.padding(top = 8.dp)
                                                                .size(8.dp)
                                                                .background(
                                                                        MaterialTheme.colorScheme
                                                                                .primary,
                                                                        shape = CircleShape
                                                                )
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column {
                                                if (showTimestamps && match != null) {
                                                        val time = match.groupValues[1]
                                                        val addedOn =
                                                                match.groupValues[2].takeIf {
                                                                        it.isNotEmpty()
                                                                }
                                                        val displayTime =
                                                                if (addedOn != null)
                                                                        "$time (added on $addedOn)"
                                                                else time

                                                        Text(
                                                                text = displayTime,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelMedium.copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primary.copy(
                                                                                alpha = 0.7f
                                                                        ),
                                                                modifier =
                                                                        Modifier.padding(
                                                                                bottom = 2.dp
                                                                        )
                                                        )
                                                }

                                                val truncatedEntry =
                                                        if (isTruncated && !isExpanded) {
                                                                cleanEntry.take(previewLimitLength) + "..."
                                                        } else cleanEntry

                                                Text(
                                                        text =
                                                                MarkdownUtils.parseMarkdown(
                                                                        truncatedEntry
                                                                ),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                )
                                        }
                                }
                        }
                }
        )
}
