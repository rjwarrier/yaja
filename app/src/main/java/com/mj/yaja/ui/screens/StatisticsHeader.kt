package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Note
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun StatisticsPeriodSummaryCard(
    selectedPeriod: StatisticsPeriod,
    customStartDate: LocalDate,
    customEndDate: LocalDate,
    displayedSectionsCount: Int,
    totalSectionsCount: Int,
    statisticsSettling: Boolean,
    statisticsProgress: Float?
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 2.dp, end = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = selectedPeriod.title(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedPeriod == StatisticsPeriod.CUSTOM) {
                    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            "${customStartDate.format(fmt)} - ${customEndDate.format(fmt)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                if (displayedSectionsCount < totalSectionsCount) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = "$displayedSectionsCount of $totalSectionsCount sections visible",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            if (statisticsSettling) {
                Text(
                    text = "Loading statistics in stages...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { statisticsProgress ?: 0f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun StatisticsOverviewRows(
    stats: AllTimeStatsData,
    highlightedDays: Int,
    entranceTriggered: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        AppStaggeredEntrance(
            visible = entranceTriggered,
            index = 4,
            strength = AppEntranceStrength.SECTION
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticOverviewCard(
                    icon = Icons.AutoMirrored.Rounded.Note,
                    title = "Total Entries",
                    value = stats.totalEntries.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatisticOverviewCard(
                    icon = Icons.Rounded.Edit,
                    title = "Total Words",
                    value = stats.totalWords.toString(),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        AppStaggeredEntrance(
            visible = entranceTriggered,
            index = 5,
            strength = AppEntranceStrength.SECTION
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticOverviewCard(
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    title = "Avg. Words/Entry",
                    value = String.format("%.1f", stats.averageWordsPerEntry),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                StatisticOverviewCard(
                    icon = Icons.Rounded.LocalFireDepartment,
                    title = "Current Streak",
                    value = "${stats.currentStreak} days",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        AppStaggeredEntrance(
            visible = entranceTriggered,
            index = 6,
            strength = AppEntranceStrength.SECTION
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticOverviewCard(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    title = "Pages Written",
                    value = "~ ${(stats.totalWords / 250).coerceAtLeast(0)}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatisticOverviewCard(
                    icon = Icons.Rounded.Star,
                    title = "Days Highlighted",
                    value = highlightedDays.toString(),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun StatisticsPeriod.title(): String =
    when (this) {
        StatisticsPeriod.ALL_TIME -> "All-Time Statistics"
        StatisticsPeriod.CURRENT_YEAR -> "Current Year Statistics"
        StatisticsPeriod.PREVIOUS_YEAR -> "Previous Year Statistics"
        StatisticsPeriod.CURRENT_MONTH -> "Current Month Statistics"
        StatisticsPeriod.PREVIOUS_MONTH -> "Previous Month Statistics"
        StatisticsPeriod.CUSTOM -> "Custom Period Statistics"
    }
