package com.mj.yaja.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.data.KeywordType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewTodoSummaryCard(
        reviewTodos: List<ReviewTodoSuggestion>,
        onExtractTodos: () -> Unit
) {
        if (reviewTodos.isEmpty()) return

        Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Text(
                                text = stringResource(R.string.addentry_todos_found_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                        )
                        reviewTodos.take(4).forEach { todo ->
                                Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                        Text(
                                                text = todo.checklistLine,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 10.dp
                                                                )
                                        )
                                }
                        }
                        Button(onClick = onExtractTodos) {
                                Text(
                                        pluralStringResource(
                                                R.plurals.addentry_convert_to_checklist,
                                                reviewTodos.size,
                                                reviewTodos.size
                                        )
                                )
                        }
                }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewMentionsCard(reviewMentions: List<ReviewMentionSuggestion>) {
        Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Text(
                                text = stringResource(R.string.addentry_people_places_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                        )
                        if (reviewMentions.isEmpty()) {
                                Text(
                                        text = stringResource(R.string.addentry_no_tracked_mentions),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        } else {
                                FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        reviewMentions.forEach { mention ->
                                                AssistChip(
                                                        onClick = {},
                                                        label = {
                                                                Text(
                                                                        "${mention.name} | ${mention.matches}"
                                                                )
                                                        },
                                                        leadingIcon = {
                                                                Icon(
                                                                        imageVector =
                                                                                if (
                                                                                        mention.type ==
                                                                                                KeywordType
                                                                                                        .PERSON
                                                                                ) {
                                                                                        Icons.Rounded
                                                                                                .Person
                                                                                } else {
                                                                                        Icons.Rounded
                                                                                                .LocationOn
                                                                                },
                                                                        contentDescription = null,
                                                                        modifier = Modifier.size(16.dp)
                                                                )
                                                        }
                                                )
                                        }
                                }
                        }
                }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewDayMemoryCard(
        isStarred: Boolean,
        onStarredChange: (Boolean) -> Unit,
        dayLabel: String,
        onDayLabelChange: (String) -> Unit,
        revisitDate: LocalDate?,
        onRevisitDateChange: (LocalDate?) -> Unit,
        revisitNote: String,
        onRevisitNoteChange: (String) -> Unit,
        selectedDate: LocalDate,
        reviewDateFormatter: DateTimeFormatter,
        onShowCustomDatePicker: () -> Unit
) {
        Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                        Text(
                                text = stringResource(R.string.addentry_day_memory_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                        )
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = stringResource(R.string.addentry_star_day_title),
                                                style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                                text =
                                                        stringResource(R.string.addentry_star_day_subtitle),
                                                style = MaterialTheme.typography.bodySmall,
                                                color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                                Switch(
                                        checked = isStarred,
                                        onCheckedChange = onStarredChange
                                )
                        }
                        HorizontalDivider()
                        OutlinedTextField(
                                value = dayLabel,
                                onValueChange = onDayLabelChange,
                                label = { Text(stringResource(R.string.addentry_day_label_label)) },
                                placeholder = { Text(stringResource(R.string.addentry_day_label_placeholder)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                                text = stringResource(R.string.addentry_follow_up_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                FilterChip(
                                        selected = revisitDate == selectedDate.plusDays(1),
                                        onClick = {
                                                onRevisitDateChange(selectedDate.plusDays(1))
                                        },
                                        label = { Text(stringResource(R.string.settings_meaning_tomorrow)) }
                                )
                                FilterChip(
                                        selected = revisitDate == selectedDate.plusWeeks(1),
                                        onClick = {
                                                onRevisitDateChange(selectedDate.plusWeeks(1))
                                        },
                                        label = { Text(stringResource(R.string.settings_meaning_next_week)) }
                                )
                                FilterChip(
                                        selected = revisitDate == selectedDate.plusMonths(1),
                                        onClick = {
                                                onRevisitDateChange(selectedDate.plusMonths(1))
                                        },
                                        label = { Text(stringResource(R.string.addentry_next_month)) }
                                )
                                FilterChip(
                                        selected = false,
                                        onClick = onShowCustomDatePicker,
                                        label = { Text(stringResource(R.string.addentry_custom)) }
                                )
                        }
                        revisitDate?.let { dueDate ->
                                AssistChip(
                                        onClick = { onRevisitDateChange(null) },
                                        label = {
                                                Text(
                                                        stringResource(
                                                                R.string.addentry_due_date_format,
                                                                dueDate.format(reviewDateFormatter)
                                                        )
                                                )
                                        },
                                        trailingIcon = {
                                                Icon(
                                                        Icons.Rounded.Close,
                                                        contentDescription = stringResource(R.string.addentry_cd_clear_follow_up)
                                                )
                                        }
                                )
                        }
                        OutlinedTextField(
                                value = revisitNote,
                                onValueChange = onRevisitNoteChange,
                                label = { Text(stringResource(R.string.addentry_follow_up_note_label)) },
                                placeholder = { Text(stringResource(R.string.addentry_follow_up_note_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                        )
                }
        }
}
