package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatisticsSectionSettingsSheet(
    viewModel: JournalViewModel,
    sectionOrder: List<StatisticsSection>,
    visibleSections: Set<StatisticsSection>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Choose Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Hide sections you do not want. At least one section must stay visible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${visibleSections.size} of ${sectionOrder.size} visible",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (visibleSections.size < sectionOrder.size) {
                    TextButton(
                        onClick = {
                            viewModel.setVisibleStatisticsSections(
                                sectionOrder.map { it.name }.toSet()
                            )
                        }
                    ) {
                        Text("Show All")
                    }
                }
            }
            sectionOrder.forEach { section ->
                val enabled = section in visibleSections
                val isLastVisible = enabled && visibleSections.size == 1
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = section.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when {
                                    isLastVisible -> "One section must remain visible"
                                    enabled -> "Shown in statistics"
                                    else -> "Hidden from statistics"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLastVisible) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { show ->
                                val updated =
                                    if (show) {
                                        visibleSections + section
                                    } else {
                                        if (isLastVisible) visibleSections else visibleSections - section
                                    }
                                viewModel.setVisibleStatisticsSections(updated.map { it.name }.toSet())
                            },
                            enabled = !isLastVisible || !enabled
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatisticsStartDatePickerDialog(
    customStartDate: LocalDate,
    yesterday: LocalDate,
    yesterdayMillis: Long,
    onDismiss: () -> Unit,
    onDateConfirmed: (LocalDate) -> Unit,
    onNext: () -> Unit
) {
    val startPickerState = rememberDatePickerState(
        initialSelectedDateMillis = customStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = IntRange(2000, yesterday.year),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= yesterdayMillis
            override fun isSelectableYear(year: Int): Boolean = year <= yesterday.year
        }
    )
    val startConfirmEnabled = startPickerState.selectedDateMillis?.let { it <= yesterdayMillis } == true
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = startConfirmEnabled,
                onClick = {
                    startPickerState.selectedDateMillis?.let { millis ->
                        if (millis <= yesterdayMillis) {
                            onDateConfirmed(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            )
                        }
                    }
                    onNext()
                }
            ) { Text("Next") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(
            state = startPickerState,
            showModeToggle = false,
            headline = {
                Text(
                    "Select Start Date",
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatisticsEndDatePickerDialog(
    customStartDate: LocalDate,
    customEndDate: LocalDate,
    today: LocalDate,
    todayMillis: Long,
    onDismiss: () -> Unit,
    onDateConfirmed: (LocalDate) -> Unit,
    onApply: () -> Unit
) {
    val fromMillis = customStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val endPickerState = rememberDatePickerState(
        initialSelectedDateMillis = customEndDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = IntRange(customStartDate.year, today.year),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis in fromMillis..todayMillis
            override fun isSelectableYear(year: Int): Boolean = year in customStartDate.year..today.year
        }
    )
    val endConfirmEnabled = endPickerState.selectedDateMillis?.let { it in fromMillis..todayMillis } == true
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = endConfirmEnabled,
                onClick = {
                    endPickerState.selectedDateMillis?.let { millis ->
                        if (millis in fromMillis..todayMillis) {
                            onDateConfirmed(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            )
                        }
                    }
                    onApply()
                }
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(
            state = endPickerState,
            showModeToggle = false,
            headline = {
                Text(
                    "Select End Date",
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                )
            }
        )
    }
}
