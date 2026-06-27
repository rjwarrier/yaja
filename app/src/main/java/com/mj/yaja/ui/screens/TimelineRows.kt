package com.mj.yaja.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.LocalIndication
import com.mj.yaja.R
import com.mj.yaja.ui.design.expressivePressMotion

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TimelineMonthHeader(
    title: String,
    summary: String,
    isCollapsed: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onToggle, onLongClick = onToggle)
                .padding(top = 16.dp, bottom = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = if (isCollapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                summary
                    .split("Â·", "Ã‚Â·", ignoreCase = false)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { item ->
                        AssistChip(
                            onClick = {},
                            label = { Text(item) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineDateRow(
    node: TimelineDateNode,
    isFirst: Boolean,
    isLast: Boolean,
    density: TimelineDensity,
    style: TimelineStyle,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    if (style == TimelineStyle.CARD) {
        TimelineDateCard(
            node = node,
            density = density,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        TimelineDateClassicRow(
            node = node,
            isFirst = isFirst,
            isLast = isLast,
            density = density,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineDateClassicRow(
    node: TimelineDateNode,
    isFirst: Boolean,
    isLast: Boolean,
    density: TimelineDensity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val accent = when {
        node.isToday -> MaterialTheme.colorScheme.primary
        node.isFavorite -> MaterialTheme.colorScheme.tertiary
        !node.hasEntries && !node.label.isNullOrBlank() -> MaterialTheme.colorScheme.secondary
        !node.hasEntries -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.secondary
    }
    val dayNumberColor = when {
        node.isToday || node.isFavorite -> accent
        node.hasEntries -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
    }
    val weekdayColor = if (node.hasEntries) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    }
    val isLabelOnly = !node.hasEntries && !node.label.isNullOrBlank()
    val dayTextStyle = if (density == TimelineDensity.COMFORTABLE) {
        MaterialTheme.typography.headlineSmall
    } else {
        MaterialTheme.typography.titleLarge
    }
    val dayOfWeekStyle = if (density == TimelineDensity.COMFORTABLE) {
        MaterialTheme.typography.titleSmall
    } else {
        MaterialTheme.typography.labelLarge
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .expressivePressMotion(interactionSource, pressedScale = 0.98f)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = density.verticalPadding.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimelineTrack(
            accent = accent,
            isFirst = isFirst,
            isLast = isLast,
            isToday = node.isToday,
            isFavorite = node.isFavorite,
            hasEntries = node.hasEntries,
            trackHeight = density.trackHeight.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = node.date.dayOfMonth.toString().padStart(2, '0'),
                style = dayTextStyle,
                color = dayNumberColor,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = node.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = dayOfWeekStyle,
                color = weekdayColor,
                fontWeight = FontWeight.Medium
            )
            if (node.isToday) {
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            stringResource(R.string.timeline_chip_today),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            if (node.entryCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            pluralStringResource(
                                R.plurals.timeline_chip_entries,
                                node.entryCount,
                                node.entryCount
                            )
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            if (node.hasFollowUp) {
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = onClick,
                    label = { Text(stringResource(R.string.timeline_chip_follow_up)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
            if (isLabelOnly) {
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = onClick,
                    label = { Text(stringResource(R.string.timeline_chip_label_only)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            node.label?.let { label ->
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (node.hasEntries) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (node.hasEntries) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (node.isFavorite) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = stringResource(R.string.timeline_cd_favorite),
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineDateCard(
    node: TimelineDateNode,
    density: TimelineDensity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val accent = when {
        node.isToday -> MaterialTheme.colorScheme.primary
        node.isFavorite -> MaterialTheme.colorScheme.tertiary
        !node.hasEntries && !node.label.isNullOrBlank() -> MaterialTheme.colorScheme.secondary
        !node.hasEntries -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.secondary
    }
    val dayNumberColor = when {
        node.isToday || node.isFavorite -> accent
        node.hasEntries -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
    }
    val weekdayColor = if (node.hasEntries) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    }
    val isLabelOnly = !node.hasEntries && !node.label.isNullOrBlank()
    val dayTextStyle = if (density == TimelineDensity.COMFORTABLE) {
        MaterialTheme.typography.headlineSmall
    } else {
        MaterialTheme.typography.titleLarge
    }
    val dayOfWeekStyle = if (density == TimelineDensity.COMFORTABLE) {
        MaterialTheme.typography.titleSmall
    } else {
        MaterialTheme.typography.labelLarge
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .expressivePressMotion(interactionSource, pressedScale = 0.98f)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = (density.verticalPadding + 2).dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = node.date.dayOfMonth.toString().padStart(2, '0'),
                    style = dayTextStyle,
                    color = dayNumberColor,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = node.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = dayOfWeekStyle,
                    color = weekdayColor,
                    fontWeight = FontWeight.Medium
                )

                if (node.isToday) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = onClick,
                        label = {
                            Text(
                                stringResource(R.string.timeline_chip_today),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                if (node.entryCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = onClick,
                        label = {
                            Text(
                                pluralStringResource(
                                    R.plurals.timeline_chip_entries,
                                    node.entryCount,
                                    node.entryCount
                                )
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                if (node.hasFollowUp) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = onClick,
                        label = { Text(stringResource(R.string.timeline_chip_follow_up)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
                if (isLabelOnly) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = onClick,
                        label = { Text(stringResource(R.string.timeline_chip_label_only)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                node.label?.let { label ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (node.hasEntries) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (node.hasEntries) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (node.isFavorite) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = stringResource(R.string.timeline_cd_favorite),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}

@Composable
fun TimelineTrack(
    accent: Color,
    isFirst: Boolean,
    isLast: Boolean,
    isToday: Boolean,
    isFavorite: Boolean,
    hasEntries: Boolean,
    trackHeight: Dp
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val centerFillColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .width(24.dp)
            .height(trackHeight),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            if (!isFirst) {
                drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(centerX, 0f),
                    end = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            if (!isLast) {
                drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    end = androidx.compose.ui.geometry.Offset(centerX, size.height),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(
                color = accent.copy(alpha = when {
                    !hasEntries -> 0.28f
                    isToday || isFavorite -> 0.95f
                    else -> 0.4f
                }),
                radius = when {
                    isToday -> 7.dp.toPx()
                    isFavorite -> 6.dp.toPx()
                    else -> 4.5.dp.toPx()
                },
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
            drawCircle(
                color = centerFillColor,
                radius = when {
                    isToday -> 3.dp.toPx()
                    isFavorite -> 2.5.dp.toPx()
                    !hasEntries -> 2.dp.toPx()
                    else -> 0f
                },
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }
    }
}
