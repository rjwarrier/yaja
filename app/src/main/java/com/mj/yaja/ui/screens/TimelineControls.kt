package com.mj.yaja.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R

@Composable
fun TimelineQuickActionsRow(
    onTodayClick: () -> Unit,
    onOpenMonthMenu: () -> Unit,
    onOpenFilters: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onTodayClick,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Today,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Today")
            }
            FilledTonalButton(
                onClick = onOpenMonthMenu,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Month")
            }
            FilledTonalButton(
                onClick = onOpenFilters,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Filters")
            }
        }
    }
}

@Composable
fun TimelineActiveFiltersRow(
    selectedFilter: TimelineFilter,
    selectedYear: String,
    showAllDates: Boolean,
    selectedDensity: TimelineDensity,
    labelQuery: String
) {
    val activeChips = buildList {
        if (selectedFilter != TimelineFilter.ALL) add(selectedFilter.label)
        if (selectedYear != ALL_YEARS) add(selectedYear)
        if (showAllDates) add("All dates")
        if (selectedDensity != TimelineDensity.COMFORTABLE) add(selectedDensity.label)
        if (labelQuery.isNotBlank()) add("Label: ${labelQuery.trim()}")
    }
    if (activeChips.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        activeChips.forEach { chip ->
            AssistChip(
                onClick = {},
                label = { Text(chip) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun TimelineLabelSearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null
            )
        },
        placeholder = { Text("Search day labels") }
    )
}

@Composable
fun TimelineFilterSheet(
    labelQuery: String,
    onLabelQueryChange: (String) -> Unit,
    selectedFilter: TimelineFilter,
    onFilterSelected: (TimelineFilter) -> Unit,
    years: List<Int>,
    selectedYear: String,
    onYearSelected: (String) -> Unit,
    showAllDates: Boolean,
    onShowAllDatesChanged: (Boolean) -> Unit,
    selectedDensity: TimelineDensity,
    onDensitySelected: (TimelineDensity) -> Unit,
    selectedStyle: TimelineStyle,
    onStyleSelected: (TimelineStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Timeline Filters & Layout",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        TimelineLabelSearchField(
            value = labelQuery,
            onValueChange = onLabelQueryChange
        )
        TimelineFilterRow(
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected
        )
        TimelineYearFilterRow(
            years = years,
            selectedYear = selectedYear,
            onYearSelected = onYearSelected
        )
        TimelineControlsRow(
            checked = showAllDates,
            onCheckedChange = onShowAllDatesChanged,
            selectedDensity = selectedDensity,
            onDensitySelected = onDensitySelected,
            selectedStyle = selectedStyle,
            onStyleSelected = onStyleSelected
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun TimelineFilterRow(
    selectedFilter: TimelineFilter,
    onFilterSelected: (TimelineFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TimelineFilter.values().forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter.label,
                        fontWeight = if (filter == selectedFilter) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

@Composable
fun TimelineYearFilterRow(
    years: List<Int>,
    selectedYear: String,
    onYearSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilterChip(
            selected = selectedYear == ALL_YEARS,
            onClick = { onYearSelected(ALL_YEARS) },
            label = {
                Text(
                    text = ALL_YEARS,
                    fontWeight = if (selectedYear == ALL_YEARS) FontWeight.SemiBold else FontWeight.Medium
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        )
        years.forEach { year ->
            val yearLabel = year.toString()
            FilterChip(
                selected = selectedYear == yearLabel,
                onClick = { onYearSelected(yearLabel) },
                label = {
                    Text(
                        text = yearLabel,
                        fontWeight = if (selectedYear == yearLabel) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
        }
    }
}

@Composable
fun TimelineControlsRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    selectedDensity: TimelineDensity,
    onDensitySelected: (TimelineDensity) -> Unit,
    selectedStyle: TimelineStyle,
    onStyleSelected: (TimelineStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.timeline_show_all_dates),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.timeline_show_all_dates_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimelineDensity.values().forEach { density ->
                    FilterChip(
                        selected = selectedDensity == density,
                        onClick = { onDensitySelected(density) },
                        label = {
                            Text(
                                density.label,
                                fontWeight = if (selectedDensity == density) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimelineStyle.values().forEach { style ->
                    FilterChip(
                        selected = selectedStyle == style,
                        onClick = { onStyleSelected(style) },
                        label = {
                            Text(
                                style.label,
                                fontWeight = if (selectedStyle == style) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }
    }
}
