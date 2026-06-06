package com.mj.yaja.ui.screens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.KeywordCoOccurrence
import com.mj.yaja.data.KeywordMatch
import com.mj.yaja.data.KeywordType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.max

private enum class KeywordTimelineRange(val label: String, val months: Int?) {
    SIX_MONTHS("6M", 6),
    TWELVE_MONTHS("12M", 12),
    TWENTY_FOUR_MONTHS("24M", 24),
    ALL("All", null)
}

private enum class ConnectionFilter(val label: String) {
    ALL("All"),
    PEOPLE("People"),
    PLACES("Places"),
    RECENT("Recent"),
    STRONGEST("Strongest")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KeywordHeroCard(
    name: String,
    type: KeywordType,
    relation: String,
    totalMentions: Int,
    uniqueDays: Int,
    isIndexing: Boolean
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (type == KeywordType.PERSON) Icons.Rounded.Person else Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    if (relation.isBlank()) {
                                        type.name.lowercase().replaceFirstChar { it.uppercase() }
                                    } else {
                                        "${type.name.lowercase().replaceFirstChar { it.uppercase() }} • $relation"
                                    }
                                )
                            }
                        )
                        AssistChip(
                            onClick = {},
                            leadingIcon = {
                                if (!isIndexing) {
                                    Icon(
                                        imageVector = Icons.Rounded.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            label = { Text(if (isIndexing) "Indexing" else "Indexed") }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMetricPill(
                    label = "Mentions",
                    value = totalMentions.toString(),
                    modifier = Modifier.weight(1f)
                )
                HeroMetricPill(
                    label = "Days",
                    value = uniqueDays.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KeywordTimelineCard(
    mentionsByMonth: List<Pair<YearMonth, Int>>,
    totalMentions: Int,
    uniqueDays: Int
) {
    var selectedRange by remember(mentionsByMonth) { mutableStateOf(KeywordTimelineRange.TWELVE_MONTHS) }
    val visiblePoints = remember(mentionsByMonth, selectedRange) {
        selectedRange.months?.let { mentionsByMonth.takeLast(it) } ?: mentionsByMonth
    }
    val peakMonth = visiblePoints.maxByOrNull { it.second }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Mentions Over Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (peakMonth != null) {
                            "Peak ${peakMonth.first.format(keywordTimelineMonthFormatter)} • ${peakMonth.second} mention${if (peakMonth.second == 1) "" else "s"}"
                        } else {
                            "No monthly pattern yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoGraph,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "$totalMentions total",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeywordTimelineRange.values().forEach { range ->
                    AssistChip(
                        onClick = { selectedRange = range },
                        label = { Text(range.label) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedRange == range) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            labelColor = if (selectedRange == range) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                }
            }

            if (visiblePoints.isEmpty()) {
                Text(
                    text = "Mentions will chart here after indexing finds entries.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                KeywordTimelineChart(
                    points = visiblePoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    visiblePoints.forEachIndexed { index, (month, _) ->
                        val previousMonth = visiblePoints.getOrNull(index - 1)?.first
                        val yearLabel = when {
                            previousMonth == null -> month.format(keywordTimelineYearFormatter)
                            previousMonth.year != month.year -> month.format(keywordTimelineYearFormatter)
                            else -> " "
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = month.format(keywordTimelineShortFormatter),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = yearLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Days with mentions",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uniqueDays.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun KeywordTimelineChart(
    points: List<Pair<YearMonth, Int>>,
    modifier: Modifier = Modifier
) {
    val maxCount = max(points.maxOfOrNull { it.second } ?: 0, 1)
    val primary = MaterialTheme.colorScheme.primary
    val fillBrush = Brush.verticalGradient(
        colors = listOf(
            primary.copy(alpha = 0.24f),
            primary.copy(alpha = 0.04f)
        )
    )
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        val left = 20.dp.toPx()
        val top = 16.dp.toPx()
        val bottom = size.height - 18.dp.toPx()
        val right = size.width - 10.dp.toPx()
        val chartWidth = (right - left).coerceAtLeast(1f)
        val chartHeight = (bottom - top).coerceAtLeast(1f)
        val stepX = if (points.size <= 1) 0f else chartWidth / (points.size - 1)

        repeat(4) { index ->
            val y = top + (chartHeight * index / 3f)
            drawLine(
                color = primary.copy(alpha = 0.08f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        drawLine(
            color = primary.copy(alpha = 0.12f),
            start = Offset(left, bottom),
            end = Offset(right, bottom),
            strokeWidth = 1.dp.toPx()
        )

        val plotPoints = points.mapIndexed { index, (_, count) ->
            val normalized = count / maxCount.toFloat()
            val x = left + (stepX * index)
            val y = bottom - (normalized * chartHeight)
            Offset(x, y)
        }

        if (plotPoints.size == 1) {
            drawCircle(
                color = primary,
                radius = 6.dp.toPx(),
                center = plotPoints.first()
            )
            return@Canvas
        }

        val linePath = Path().apply {
            moveTo(plotPoints.first().x, plotPoints.first().y)
            for (index in 1 until plotPoints.size) {
                val current = plotPoints[index]
                lineTo(current.x, current.y)
            }
        }

        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(plotPoints.last().x, bottom)
            lineTo(plotPoints.first().x, bottom)
            close()
        }

        drawPath(path = areaPath, brush = fillBrush)
        drawPath(
            path = linePath,
            color = primary,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        plotPoints.forEach { point ->
            drawLine(
                color = primary.copy(alpha = 0.18f),
                start = Offset(point.x, bottom),
                end = Offset(point.x, bottom + 6.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = point
            )
            drawCircle(
                color = primary,
                radius = 3.5.dp.toPx(),
                center = point
            )
        }
    }
}

@Composable
internal fun StatTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun LastSeenCard(
    lastSeen: LocalDate?,
    firstSeen: LocalDate?,
    modifier: Modifier = Modifier
) {
    val daysAgo = lastSeen?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).toInt() }
    val status = when {
        lastSeen == null -> "No matches yet"
        daysAgo == 0 -> "Mentioned today"
        daysAgo == 1 -> "Mentioned yesterday"
        daysAgo != null -> "Last mention $daysAgo days ago"
        else -> "No matches yet"
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Recency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMetricPill(
                    label = "First Seen",
                    value = firstSeen?.format(keywordDetailDateFormatter) ?: "—",
                    modifier = Modifier.weight(1f)
                )
                HeroMetricPill(
                    label = "Last Seen",
                    value = lastSeen?.format(keywordDetailDateFormatter) ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConnectionsCard(
    coOccurring: List<KeywordCoOccurrence>,
    onOpenKeyword: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Co-mentioned With",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                coOccurring.take(8).forEach { coOccurrence ->
                    AssistChip(
                        onClick = { onOpenKeyword(coOccurrence.keywordId) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (coOccurrence.type == KeywordType.PERSON) {
                                    Icons.Rounded.Person
                                } else {
                                    Icons.Rounded.LocationOn
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("${coOccurrence.name} • ${coOccurrence.daysTogether}") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RankedConnectionsCard(
    coOccurring: List<KeywordCoOccurrence>,
    onOpenKeyword: (String) -> Unit
) {
    var expanded by remember(coOccurring) { mutableStateOf(false) }
    var selectedFilter by remember(coOccurring) { mutableStateOf(ConnectionFilter.ALL) }
    val visibleConnections = remember(coOccurring, selectedFilter) {
        when (selectedFilter) {
            ConnectionFilter.ALL -> coOccurring
            ConnectionFilter.PEOPLE -> coOccurring.filter { it.type == KeywordType.PERSON }
            ConnectionFilter.PLACES -> coOccurring.filter { it.type == KeywordType.PLACE }
            ConnectionFilter.RECENT -> coOccurring.sortedWith(
                compareByDescending<KeywordCoOccurrence> { it.lastSeenTogether }
                    .thenByDescending { it.daysTogether }
                    .thenByDescending { it.score }
            )
            ConnectionFilter.STRONGEST -> coOccurring.sortedWith(
                compareByDescending<KeywordCoOccurrence> { it.score }
                    .thenByDescending { it.daysTogether }
                    .thenByDescending { it.sharedEntries }
            )
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Connections",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ranked by repeated overlap and recency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${coOccurring.size} linked",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { expanded = !expanded },
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (expanded) "Hide connections" else "Show connections",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (expanded) {
                                "${visibleConnections.size} items in this view"
                            } else {
                                "Tap to expand ${visibleConnections.size} ranked links"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConnectionFilter.values().forEach { filter ->
                        AssistChip(
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedFilter == filter) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                labelColor = if (selectedFilter == filter) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        )
                    }
                }

                if (visibleConnections.isEmpty()) {
                    Text(
                        text = "No connections for this filter yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    visibleConnections.take(6).forEachIndexed { index, relationship ->
                        RankedConnectionRow(
                            relationship = relationship,
                            onClick = { onOpenKeyword(relationship.keywordId) }
                        )
                        if (index != minOf(visibleConnections.lastIndex, 5)) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankedConnectionRow(
    relationship: KeywordCoOccurrence,
    onClick: () -> Unit
) {
    val lastSeenLabel = relationship.lastSeenTogether?.format(keywordDetailDateFormatter) ?: "Unknown"
    val metaLine = buildString {
        append("${relationship.daysTogether} day")
        if (relationship.daysTogether != 1) append("s")
        append(" together")
        if (relationship.sharedEntries > 0) {
            append(" | ${relationship.sharedEntries} shared entr")
            append(if (relationship.sharedEntries == 1) "y" else "ies")
        }
        append(" | last $lastSeenLabel")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = if (relationship.type == KeywordType.PERSON) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (relationship.type == KeywordType.PERSON) {
                        Icons.Rounded.Person
                    } else {
                        Icons.Rounded.LocationOn
                    },
                    contentDescription = null,
                    tint = if (relationship.type == KeywordType.PERSON) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = relationship.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (relationship.relation.isNotBlank()) {
                        Text(
                            text = relationship.relation,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Score ${relationship.score.toInt()}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onClick,
                    label = { Text("${relationship.daysTogether} days") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
                if (relationship.sharedEntries > 0) {
                    AssistChip(
                        onClick = onClick,
                        label = { Text("${relationship.sharedEntries} shared") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            }
        }
    }
}

@Composable
internal fun MatchCard(
    match: KeywordMatch,
    onNavigateToDate: (LocalDate) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Text(
                        text = LocalDate.parse(match.date).format(keywordDetailDateFormatter),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "${(match.confidence * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = match.snippet.ifBlank { match.matchedText },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.matchType.name.lowercase().replace('_', ' '),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = { onNavigateToDate(LocalDate.parse(match.date)) }) {
                    Text("Open Day")
                }
            }
        }
    }
}
