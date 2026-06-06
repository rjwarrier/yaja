package com.mj.yaja.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private val ComparisonCardMinHeight = 228.dp
private val ComparisonCardNameSlotHeight = 34.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatisticsPeriodSelector(
    selectedPeriod: StatisticsPeriod,
    onPeriodChange: (StatisticsPeriod) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Period",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    StatisticsPeriod.ALL_TIME to "All Time",
                    StatisticsPeriod.CURRENT_YEAR to "Current Year",
                    StatisticsPeriod.PREVIOUS_YEAR to "Prev Year",
                    StatisticsPeriod.CURRENT_MONTH to "This Month",
                    StatisticsPeriod.PREVIOUS_MONTH to "Prev Month",
                    StatisticsPeriod.CUSTOM to "Custom"
                ).forEach { (period, label) ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onPeriodChange(period) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }
    }
}

@Composable
fun CompareModeToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = null,
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Compare Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Month, year, and keyword deltas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun StatisticsComparisonSection(
    comparison: StatisticsComparisonData
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ComparisonMetricCard(
                title = "This Month",
                currentLabel = comparison.monthWindow.currentLabel,
                previousLabel = comparison.monthWindow.previousLabel,
                currentValue = comparison.monthWindow.currentEntries,
                previousValue = comparison.monthWindow.previousEntries,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            ComparisonMetricCard(
                title = "This Year",
                currentLabel = comparison.yearWindow.currentLabel,
                previousLabel = comparison.yearWindow.previousLabel,
                currentValue = comparison.yearWindow.currentEntries,
                previousValue = comparison.yearWindow.previousEntries,
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ComparisonMetricCard(
                title = "Writing Days Delta",
                currentLabel = comparison.monthWindow.currentLabel,
                previousLabel = comparison.monthWindow.previousLabel,
                currentValue = comparison.monthWindow.currentWritingDays,
                previousValue = comparison.monthWindow.previousWritingDays,
                accentColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            KeywordDeltaCard(
                title = "Top Person Delta",
                emptyLabel = "No people mentions yet",
                delta = comparison.topPersonDelta,
                icon = Icons.Rounded.Person,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        KeywordDeltaCard(
            title = "Top Place Delta",
            emptyLabel = "No place mentions yet",
            delta = comparison.topPlaceDelta,
            icon = Icons.Rounded.LocationOn,
            accentColor = MaterialTheme.colorScheme.secondary
        )

        ComparisonWindowDetailCard(
            comparison = comparison,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateInsightsCard(
    insights: TemplateInsightsData
) {
    val totalTrackedEntries = (insights.templateEntryCount + insights.blankEntryCount).coerceAtLeast(1)
    val templateShare = (insights.templateEntryCount * 100) / totalTrackedEntries
    val blankShare = (insights.blankEntryCount * 100) / totalTrackedEntries
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Structured Writing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TemplateInsightChip(
                    label = "Template-led ${insights.templateEntryCount} · ${templateShare}%",
                    emphasized = true
                )
                TemplateInsightChip(
                    label = "Blank ${insights.blankEntryCount} · ${blankShare}%"
                )
            }

            if (insights.topTemplates.isNotEmpty()) {
                TemplateInsightBlock(
                    title = "Most Used Templates",
                    items = insights.topTemplates.map { "${it.name} ${it.count}" },
                    icon = Icons.AutoMirrored.Rounded.Notes
                )
            }

            if (insights.categoryMix.isNotEmpty()) {
                TemplateInsightBlock(
                    title = "Template Mix",
                    items = insights.categoryMix.map { "${it.category} ${it.count}" },
                    icon = Icons.Rounded.DateRange
                )
            }

            if (insights.followUpLeaders.isNotEmpty()) {
                TemplateInsightBlock(
                    title = "Follow-up Leaders",
                    items = insights.followUpLeaders.map { "${it.name} ${it.count}" },
                    icon = Icons.Rounded.CheckCircle
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateInsightBlock(
    title: String,
    items: List<String>,
    icon: ImageVector
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEachIndexed { index, item ->
                TemplateInsightChip(
                    label = item,
                    emphasized = index == 0
                )
            }
        }
    }
}

@Composable
private fun TemplateInsightChip(
    label: String,
    emphasized: Boolean = false
) {
    AssistChip(
        onClick = {},
        enabled = false,
        border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (emphasized) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            disabledContainerColor = if (emphasized) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            labelColor = if (emphasized) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            disabledLabelColor = if (emphasized) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    )
}

@Composable
private fun ComparisonMetricCard(
    title: String,
    currentLabel: String,
    previousLabel: String,
    currentValue: Int,
    previousValue: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val delta = currentValue - previousValue
    ElevatedCard(
        modifier = modifier.heightIn(min = ComparisonCardMinHeight),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(ComparisonCardNameSlotHeight))
            Text(
                text = formatSignedDelta(delta),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            CompactComparisonChips(
                primary = "$currentLabel $currentValue",
                secondary = "$previousLabel $previousValue"
            )
        }
    }
}

@Composable
private fun KeywordDeltaCard(
    title: String,
    emptyLabel: String,
    delta: KeywordDeltaData?,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.heightIn(min = ComparisonCardMinHeight),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (delta == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComparisonCardNameSlotHeight),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = emptyLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComparisonCardNameSlotHeight),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = delta.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = formatSignedDelta(delta.delta),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                CompactComparisonChips(
                    primary = "This month ${delta.currentMentions}",
                    secondary = "Last month ${delta.previousMentions}"
                )
            }
        }
    }
}

@Composable
private fun ComparisonWindowDetailCard(
    comparison: StatisticsComparisonData,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Compare Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            ComparisonWindowRow("Month", comparison.monthWindow)
            HorizontalDivider()
            ComparisonWindowRow("Year", comparison.yearWindow)
        }
    }
}

@Composable
private fun ComparisonWindowRow(
    label: String,
    window: StatsComparisonWindow
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        CompactComparisonChips(
            primary = "${window.currentLabel} ${window.currentEntries}e ${window.currentWords}w ${window.currentWritingDays}d",
            secondary = "${window.previousLabel} ${window.previousEntries}e ${window.previousWords}w ${window.previousWritingDays}d"
        )
    }
}

private fun formatSignedDelta(delta: Int): String =
    when {
        delta > 0 -> "+$delta"
        delta < 0 -> delta.toString()
        else -> "0"
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactComparisonChips(
    primary: String,
    secondary: String
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(primary, secondary).forEachIndexed { index, label ->
            AssistChip(
                onClick = {},
                enabled = false,
                border = null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (index == 0) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    disabledContainerColor = if (index == 0) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    labelColor = if (index == 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    disabledLabelColor = if (index == 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ),
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            )
        }
    }
}

@Composable
fun StatisticOverviewCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = color.copy(alpha = 0.14f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WritingInsightsCard(stats: AllTimeStatsData) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightRow(
                icon = Icons.Rounded.EmojiEvents,
                tint = MaterialTheme.colorScheme.primary,
                label = "Longest Streak",
                value = "${stats.longestStreakAllTime} days"
            )
            HorizontalDivider()
            InsightRow(
                icon = Icons.Rounded.DateRange,
                tint = MaterialTheme.colorScheme.secondary,
                label = "Most Active Day",
                value = stats.mostActiveDay ?: "-"
            )
            HorizontalDivider()
            InsightRow(
                icon = Icons.Rounded.CheckCircle,
                tint = MaterialTheme.colorScheme.tertiary,
                label = "Writing Consistency",
                value = "${String.format("%.0f", stats.writingConsistencyScore)}%"
            )
            HorizontalDivider()
            InsightRow(
                icon = Icons.Rounded.DateRange,
                tint = MaterialTheme.colorScheme.error,
                label = "Days With Entries",
                value = stats.totalDaysWithEntries.toString()
            )
            HorizontalDivider()
            InsightRow(
                icon = Icons.Rounded.CalendarMonth,
                tint = MaterialTheme.colorScheme.tertiary,
                label = "Best Month",
                value = if (stats.bestMonthLabel != null) {
                    "${stats.bestMonthLabel} · ${stats.bestMonthCount} entries"
                } else {
                    "-"
                }
            )
            HorizontalDivider()
            InsightRow(
                icon = Icons.Rounded.CalendarViewWeek,
                tint = MaterialTheme.colorScheme.primary,
                label = "Avg. Writing Days / Week",
                value = String.format("%.1f days", stats.averageDaysPerWeek)
            )
        }
    }
}

@Composable
fun WritingDistributionCard(stats: AllTimeStatsData) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val dist = stats.entriesByLength
            val total = dist.light + dist.moderate + dist.heavy + dist.intense
            fun pct(n: Int) = if (total == 0) 0f else n.toFloat() / total * 100f

            Text(
                text = "Based on total words written per day",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            StatisticProgressRow("Light (< 50 words)", dist.light, pct(dist.light), MaterialTheme.colorScheme.primary)
            StatisticProgressRow("Moderate (50-200 words)", dist.moderate, pct(dist.moderate), MaterialTheme.colorScheme.secondary)
            StatisticProgressRow("Heavy (200-500 words)", dist.heavy, pct(dist.heavy), MaterialTheme.colorScheme.tertiary)
            StatisticProgressRow("Intense (500+ words)", dist.intense, pct(dist.intense), MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun LanguagesCard(
    distribution: Map<String, Int>,
    useMLKitDetection: Boolean,
    onToggleMLKit: (Boolean) -> Unit,
    isSettling: Boolean = false
) {
    val sorted = distribution.entries
        .sortedByDescending { it.value }
        .take(10)

    val totalEntries = distribution.values.sum().coerceAtLeast(1)
    val distinctLangs = distribution.size

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (distinctLangs == 0) {
                    "Not enough text to detect languages"
                } else {
                    "$distinctLangs ${if (distinctLangs == 1) "language" else "languages"} detected"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isSettling) {
                Text(
                    text = "Language detection is still settling.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!useMLKitDetection) {
                Text(
                    text = "Latin scripts are shown as English",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Use more accurate detection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = useMLKitDetection,
                    onCheckedChange = onToggleMLKit
                )
            }

            if (sorted.isEmpty()) {
                Text(
                    "Write more entries to see language stats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val barColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary
                )
                sorted.forEachIndexed { index, (name, count) ->
                    val pct = count.toFloat() / totalEntries * 100f
                    val color = barColors[index.coerceAtMost(barColors.lastIndex)]
                    StatisticProgressRow(
                        label = name,
                        count = count,
                        percentage = pct,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
fun WritingTimeCard(dist: TimeDistribution) {
    val total = (dist.morning + dist.afternoon + dist.evening + dist.night).coerceAtLeast(1)
    fun pct(n: Int) = n.toFloat() / total * 100f

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Based on entry time",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            StatisticProgressRow("Morning (5am-noon)", dist.morning, pct(dist.morning), MaterialTheme.colorScheme.primary)
            StatisticProgressRow("Afternoon (noon-5pm)", dist.afternoon, pct(dist.afternoon), MaterialTheme.colorScheme.secondary)
            StatisticProgressRow("Evening (5pm-9pm)", dist.evening, pct(dist.evening), MaterialTheme.colorScheme.tertiary)
            StatisticProgressRow("Night (9pm-5am)", dist.night, pct(dist.night), MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun MonthlyActivityChart(trend: List<Pair<String, Int>>) {
    if (trend.isEmpty()) return
    val maxCount = trend.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val barAreaHeight = 80.dp

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barAreaHeight + 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                trend.forEach { (monthKey, count) ->
                    val fraction = count.toFloat() / maxCount
                    val (year, month) = try {
                        val parts = monthKey.split("-")
                        Pair(parts[0].toInt(), parts[1].toInt())
                    } catch (_: Exception) {
                        Pair(2024, 1)
                    }
                    val maxDaysInMonth = try {
                        java.time.YearMonth.of(year, month).lengthOfMonth()
                    } catch (_: Exception) {
                        31
                    }
                    val isCompleteMonth = count >= maxDaysInMonth
                    val monthAbbrev = try {
                        val m = java.time.Month.of(month)
                        m.name[0] + m.name.substring(1, 3).lowercase()
                    } catch (_: Exception) {
                        "?"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(barAreaHeight)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val barHeight = barAreaHeight * fraction.coerceAtLeast(
                                if (count > 0) 0.04f else 0f
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(barHeight)
                                    .background(
                                        when {
                                            count == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                            isCompleteMonth -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.secondary
                                        },
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                        }
                        Text(
                            monthAbbrev,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EntryHeatmap(
    datesWithEntries: Set<LocalDate> = emptySet(),
    entryLengthMap: Map<LocalDate, Int> = emptyMap(),
    isSettling: Boolean = false
) {
    val today = LocalDate.now()
    val startDate = today.minusWeeks(51)

    val weeks = mutableListOf<List<LocalDate>>()
    var currentDate = startDate
    while (currentDate <= today) {
        val weekDates = mutableListOf<LocalDate>()
        for (i in 0..6) {
            if (currentDate <= today) {
                weekDates.add(currentDate)
                currentDate = currentDate.plusDays(1)
            }
        }
        if (weekDates.isNotEmpty()) {
            weeks.add(weekDates)
        }
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            if (isSettling) {
                Text(
                    text = "Heatmap is still filling in.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        week.forEach { date ->
                            val wordCount = entryLengthMap[date] ?: 0
                            val hasEntry = datesWithEntries.contains(date)
                            val cellColor = when {
                                !hasEntry -> MaterialTheme.colorScheme.surfaceVariant
                                wordCount < 50 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                wordCount < 200 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                wordCount < 500 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                else -> MaterialTheme.colorScheme.error
                            }

                            Surface(
                                modifier = Modifier.size(12.dp),
                                shape = RoundedCornerShape(2.dp),
                                color = cellColor
                            ) {}
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HeatmapLegendSwatch(MaterialTheme.colorScheme.surfaceVariant)
                HeatmapLegendSwatch(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                HeatmapLegendSwatch(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                HeatmapLegendSwatch(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                HeatmapLegendSwatch(MaterialTheme.colorScheme.error)
                Text(
                    "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsightRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatisticProgressRow(
    label: String,
    count: Int,
    percentage: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "$count (${String.format("%.0f", percentage)}%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(6.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage / 100f)
                    .background(color, RoundedCornerShape(6.dp))
            )
        }
    }
}

@Composable
private fun HeatmapLegendSwatch(color: Color) {
    Surface(
        modifier = Modifier.size(12.dp),
        shape = RoundedCornerShape(2.dp),
        color = color
    ) {}
}
