package com.mj.yaja.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.data.DateKeywordEntry

val DateKeywordMeanings = listOf(
    "yesterday", "today", "tomorrow",
    "day before yesterday", "day after tomorrow",
    "last week", "next week",
    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
)

@Composable
fun dateKeywordMeaningLabel(meaning: String): String {
    val resId = when (meaning) {
        "yesterday" -> R.string.settings_meaning_yesterday
        "today" -> R.string.settings_meaning_today
        "tomorrow" -> R.string.settings_meaning_tomorrow
        "day before yesterday" -> R.string.settings_meaning_day_before_yesterday
        "day after tomorrow" -> R.string.settings_meaning_day_after_tomorrow
        "last week" -> R.string.settings_meaning_last_week
        "next week" -> R.string.settings_meaning_next_week
        "monday" -> R.string.settings_meaning_monday
        "tuesday" -> R.string.settings_meaning_tuesday
        "wednesday" -> R.string.settings_meaning_wednesday
        "thursday" -> R.string.settings_meaning_thursday
        "friday" -> R.string.settings_meaning_friday
        "saturday" -> R.string.settings_meaning_saturday
        "sunday" -> R.string.settings_meaning_sunday
        else -> return meaning.replaceFirstChar { it.uppercase() }
    }
    return stringResource(resId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDateKeywordDialog(
    onDismiss: () -> Unit,
    onAdd: (keyword: String, meaning: String) -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var selectedMeaning by remember { mutableStateOf(DateKeywordMeanings.first()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val isValid = keyword.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_add_date_keyword_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(R.string.settings_keyword_label)) },
                    placeholder = { Text(stringResource(R.string.settings_keyword_placeholder_example)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = {
                        dropdownExpanded = !dropdownExpanded
                    }
                ) {
                    OutlinedTextField(
                        value = dateKeywordMeaningLabel(selectedMeaning),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_means_label)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = dropdownExpanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        DateKeywordMeanings.forEach { meaning ->
                            DropdownMenuItem(
                                text = { Text(dateKeywordMeaningLabel(meaning)) },
                                onClick = {
                                    selectedMeaning = meaning
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(keyword.trim(), selectedMeaning) },
                enabled = isValid
            ) { Text(stringResource(R.string.settings_add_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun ViewKeywordsDialog(
    keywords: List<DateKeywordEntry>,
    onDismiss: () -> Unit,
    onRemove: (index: Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_added_keywords_title, keywords.size)) },
        text = {
            if (keywords.isEmpty()) {
                Text(stringResource(R.string.settings_no_keywords_yet), style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    keywords.forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = entry.keyword,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(
                                        R.string.settings_keyword_meaning_format,
                                        dateKeywordMeaningLabel(entry.meaning)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onRemove(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.action_remove),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        }
    )
}
