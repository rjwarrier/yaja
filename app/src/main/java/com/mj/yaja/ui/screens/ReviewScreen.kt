package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(
    viewModel: JournalViewModel,
    period: ReviewPeriodType,
    onNavigateBack: () -> Unit,
    onNavigateToDate: (LocalDate) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val anchorDate = uiState.selectedDate
    val summary by produceState<ReviewSummaryData?>(initialValue = null, period, anchorDate) {
        value = viewModel.buildReviewSummary(period, anchorDate)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = period.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val data = summary
        if (data == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val rangeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
            val anchorFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
            val dayTitleFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM")
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
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
                                    imageVector = if (period == ReviewPeriodType.WEEKLY) {
                                        Icons.Rounded.CalendarViewWeek
                                    } else {
                                        Icons.Rounded.CalendarMonth
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (period == ReviewPeriodType.WEEKLY) {
                                        "Week containing ${anchorDate.format(anchorFormatter)}"
                                    } else {
                                        "Month containing ${anchorDate.format(anchorFormatter)}"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${data.startDate.format(rangeFormatter)} - ${data.endDate.format(rangeFormatter)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReviewChip("Entries ${data.totalEntries}", true)
                                ReviewChip("Words ${data.totalWords}")
                                ReviewChip("Days ${data.writingDays}")
                            }
                            Text(
                                text = "Tap date chips below to jump back into the journal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider()
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReviewChip(
                                    label = "Streak ${data.longestStreak}",
                                    emphasized = data.longestStreak > 0
                                )
                                ReviewChip(
                                    label = "Gap ${data.longestGap}",
                                    emphasized = false
                                )
                                ReviewChip(
                                    label = "Labels ${data.labelsCreated.size}",
                                    emphasized = data.labelsCreated.isNotEmpty()
                                )
                                ReviewChip(
                                    label = "Favorites ${data.favoriteMoments.size}",
                                    emphasized = data.favoriteMoments.isNotEmpty()
                                )
                            }
                        }
                    }
                }

                if (data.totalEntries == 0) {
                    item {
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Text(
                                text = "No journal entries in this period yet. Labels and highlights still appear below if they exist.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 18.dp),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }

                item {
                    ReviewMetricCard(
                        title = "Writing Rhythm",
                        icon = Icons.Rounded.History,
                        rows = listOf(
                            "Best streak" to "${data.longestStreak} days",
                            "Longest gap" to "${data.longestGap} days"
                        )
                    )
                }

                if (data.mostActiveDays.isNotEmpty()) {
                    item {
                        ReviewDatesCard(
                            title = "Most Active Days",
                            icon = Icons.Rounded.Insights,
                            items = data.mostActiveDays.map { day ->
                                ReviewDateRow(
                                    date = day.date,
                                    title = day.label.ifBlank { day.date.format(dayTitleFormatter) },
                                    subtitle = "${day.entryCount} entries | ${day.wordCount} words"
                                )
                            },
                            onNavigateToDate = onNavigateToDate
                        )
                    }
                }

                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ReviewChipBlock(
                                title = "Top People",
                                icon = Icons.Rounded.Person,
                                items = data.topPeople.map { "${it.name} (${it.count})" },
                                emptyText = "No people mentions"
                            )
                            HorizontalDivider()
                            ReviewChipBlock(
                                title = "Top Places",
                                icon = Icons.Rounded.LocationOn,
                                items = data.topPlaces.map { "${it.name} (${it.count})" },
                                emptyText = "No place mentions"
                            )
                        }
                    }
                }

                if (data.favoriteMoments.isNotEmpty()) {
                    item {
                        ReviewDatesCard(
                            title = "Favorite Moments",
                            icon = Icons.Rounded.Star,
                            items = data.favoriteMoments.map {
                                ReviewDateRow(
                                    date = it.date,
                                    title = it.label.ifBlank { "Starred day" },
                                    subtitle = "Highlighted in this period"
                                )
                            },
                            onNavigateToDate = onNavigateToDate
                        )
                    }
                }

                if (data.labelsCreated.isNotEmpty()) {
                    item {
                        ReviewDatesCard(
                            title = "Labels Created",
                            icon = Icons.AutoMirrored.Rounded.Label,
                            items = data.labelsCreated.map {
                                ReviewDateRow(
                                    date = it.date,
                                    title = it.label,
                                    subtitle = "Labeled day"
                                )
                            },
                            onNavigateToDate = onNavigateToDate
                        )
                    }
                }

                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
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
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Language Split",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (data.languageDistribution.isEmpty()) {
                                Text(
                                    text = "Not enough text in this period",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    data.languageDistribution.entries.forEachIndexed { index, entry ->
                                        ReviewChip(
                                            label = "${entry.key} ${entry.value}",
                                            emphasized = index == 0
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(96.dp)) }
            }
        }
    }
}
