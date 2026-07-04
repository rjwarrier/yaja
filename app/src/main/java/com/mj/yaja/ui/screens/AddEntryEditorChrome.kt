package com.mj.yaja.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.R
import com.mj.yaja.data.EntryKind
import com.mj.yaja.data.extractMentionedEventTime
import com.mj.yaja.ui.theme.metaTextStyle
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditorDateHeaderCard(
        selectedDate: LocalDate,
        dayLabel: String,
        recordedTime: String?,
        selectedEntryKind: EntryKind = EntryKind.NORMAL,
        entryText: String = "",
        isEditingMode: Boolean = false,
        onRecordedTimeClick: () -> Unit,
        dayFormatter: DateTimeFormatter,
        weekdayFormatter: DateTimeFormatter,
        monthYearFormatter: DateTimeFormatter
) {
        val rawMentionedEventTime =
                remember(selectedEntryKind, entryText) {
                        if (selectedEntryKind != EntryKind.EVENT) null else extractMentionedEventTime(entryText)
                }
        val mentionedEventTime =
                remember(rawMentionedEventTime, recordedTime) {
                        rawMentionedEventTime?.takeIf { time ->
                                time != recordedTime && time != "${recordedTime} Hrs"
                        }
                }
        // Only treat as full-day once editing settles — while actively typing, a missing time
        // is often just not-typed-yet, not a deliberate full-day choice.
        val isFullDayEvent =
                selectedEntryKind == EntryKind.EVENT && rawMentionedEventTime == null && !isEditingMode
        Surface(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 2.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.96f),
                border =
                        BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
                        ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
        ) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                ) {
                        Surface(
                                modifier = Modifier.size(60.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primary
                        ) {
                                Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(
                                                text = selectedDate.format(dayFormatter),
                                                style = MaterialTheme.typography.headlineLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                textAlign = TextAlign.Center
                                        )
                                }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                                Text(
                                        text = buildAnnotatedString {
                                                withStyle(
                                                        SpanStyle(
                                                                color =
                                                                        MaterialTheme.colorScheme.onSurface
                                                                                .copy(alpha = 0.96f),
                                                                fontSize = 20.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                        )
                                                ) {
                                                        append(selectedDate.format(weekdayFormatter))
                                                }
                                                withStyle(
                                                        SpanStyle(
                                                                color =
                                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                                .copy(alpha = 0.82f),
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Normal
                                                        )
                                                ) {
                                                        append("  ${selectedDate.format(monthYearFormatter).uppercase()}")
                                                }
                                        },
                                        lineHeight = 22.sp
                                )
                                FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                        EntryHeaderMetaChip(
                                                text = recordedTime ?: stringResource(R.string.addentry_set_time),
                                                icon = Icons.Rounded.AccessTime,
                                                onClick = onRecordedTimeClick,
                                                containerColor =
                                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                                contentColor =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                iconTint = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(10.dp)
                                        )

                                        if (dayLabel.isNotEmpty()) {
                                                EntryHeaderMetaChip(
                                                        text = dayLabel,
                                                        icon = Icons.AutoMirrored.Rounded.Label,
                                                        containerColor =
                                                                MaterialTheme.colorScheme.secondaryContainer,
                                                        contentColor =
                                                                MaterialTheme.colorScheme.onSecondaryContainer,
                                                        iconTint =
                                                                MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                        }

                                        if (selectedEntryKind == EntryKind.EVENT) {
                                                EntryHeaderMetaChip(
                                                        text = stringResource(R.string.addentry_event_chip),
                                                        icon = Icons.Rounded.Event,
                                                        containerColor =
                                                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                                                        alpha = 0.34f
                                                                ),
                                                        contentColor =
                                                                MaterialTheme.colorScheme.tertiary.copy(
                                                                        alpha = 0.82f
                                                                ),
                                                        iconTint =
                                                                MaterialTheme.colorScheme.tertiary.copy(
                                                                        alpha = 0.82f
                                                                ),
                                                        border =
                                                                BorderStroke(
                                                                        1.dp,
                                                                        MaterialTheme.colorScheme.tertiary.copy(
                                                                                alpha = 0.46f
                                                                        )
                                                                ),
                                                        shape = RoundedCornerShape(50.dp),
                                                        fontWeight = FontWeight.Medium
                                                )

                                                if (isFullDayEvent) {
                                                        EntryHeaderMetaChip(
                                                                text = stringResource(R.string.addentry_full_day_event_label),
                                                                icon = Icons.Rounded.Today,
                                                                containerColor =
                                                                        MaterialTheme.colorScheme.primary,
                                                                contentColor =
                                                                        MaterialTheme.colorScheme.onPrimary,
                                                                iconTint =
                                                                        MaterialTheme.colorScheme.onPrimary,
                                                                fontWeight = FontWeight.Bold,
                                                                shape = RoundedCornerShape(10.dp)
                                                        )
                                                } else {
                                                        mentionedEventTime?.let { eventTime ->
                                                                EntryHeaderMetaChip(
                                                                        text = eventTime,
                                                                        icon = Icons.Rounded.AccessTime,
                                                                        containerColor =
                                                                                MaterialTheme.colorScheme.primary,
                                                                        contentColor =
                                                                                MaterialTheme.colorScheme.onPrimary,
                                                                        iconTint =
                                                                                MaterialTheme.colorScheme.onPrimary,
                                                                        fontWeight = FontWeight.Bold,
                                                                        shape = RoundedCornerShape(10.dp)
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun EntryHeaderMetaChip(
        text: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        containerColor: androidx.compose.ui.graphics.Color,
        contentColor: androidx.compose.ui.graphics.Color,
        iconTint: androidx.compose.ui.graphics.Color = contentColor,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        border: BorderStroke? = null,
        shape: RoundedCornerShape = RoundedCornerShape(14.dp),
        fontWeight: FontWeight = FontWeight.Medium
) {
        Surface(
                modifier =
                        modifier
                                .height(28.dp)
                                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
                shape = shape,
                color = containerColor,
                contentColor = contentColor,
                border = border,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
        ) {
                Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(14.dp)
                        )
                        Text(
                                text = text,
                                style = MaterialTheme.typography.metaTextStyle(),
                                color = contentColor,
                                fontWeight = fontWeight
                        )
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3TimePickerInput(timePickerState: androidx.compose.material3.TimePickerState) {
        var useDialMode by remember { mutableStateOf(true) }

        androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
                if (useDialMode) {
                        TimePicker(state = timePickerState)
                } else {
                        TimeInput(state = timePickerState)
                }
                TextButton(onClick = { useDialMode = !useDialMode }) {
                        Text(
                                stringResource(
                                        if (useDialMode) R.string.addentry_switch_to_keyboard
                                        else R.string.addentry_switch_to_dial
                                )
                        )
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryTimePickerDialog(
        initialTime: String,
        onDismiss: () -> Unit,
        onConfirm: (String) -> Unit
) {
        val parsedTime =
                remember(initialTime) {
                        runCatching { LocalTime.parse(initialTime, DateTimeFormatter.ofPattern("HH:mm")) }
                                .getOrElse { LocalTime.now() }
                }
        val timePickerState =
                rememberTimePickerState(
                        initialHour = parsedTime.hour,
                        initialMinute = parsedTime.minute,
                        is24Hour = true
                )

        AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                        TextButton(
                                onClick = {
                                        onConfirm(
                                                String.format(
                                                        "%02d:%02d",
                                                        timePickerState.hour,
                                                        timePickerState.minute
                                                )
                                        )
                                }
                        ) {
                                Text(stringResource(R.string.action_set))
                        }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                },
                title = { Text(stringResource(R.string.addentry_pick_time_title)) },
                text = {
                        Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                        ) {
                                M3TimePickerInput(timePickerState = timePickerState)
                        }
                }
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventInputDialog(
        onDismiss: () -> Unit,
        onConfirm: (time: String, title: String, description: String, isAllDay: Boolean) -> Unit
) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var isAllDay by remember { mutableStateOf(false) }

        val parsedTime = remember { LocalTime.now() }
        val timePickerState =
                rememberTimePickerState(
                        initialHour = parsedTime.hour,
                        initialMinute = parsedTime.minute,
                        is24Hour = true
                )

        AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                        TextButton(
                                onClick = {
                                        val timeString =
                                                if (isAllDay) {
                                                        ""
                                                } else {
                                                        String.format(
                                                                "%02d:%02d",
                                                                timePickerState.hour,
                                                                timePickerState.minute
                                                        )
                                                }
                                        onConfirm(timeString, title.trim(), description.trim(), isAllDay)
                                },
                                enabled = title.isNotBlank()
                        ) {
                                Text(stringResource(R.string.action_ok))
                        }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                },
                title = { Text(stringResource(R.string.addentry_new_event_title)) },
                text = {
                        androidx.compose.foundation.layout.Column(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = stringResource(R.string.addentry_full_day_event_label),
                                                style = MaterialTheme.typography.bodyLarge
                                        )
                                        Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                                }

                                if (!isAllDay) {
                                        M3TimePickerInput(timePickerState = timePickerState)
                                }

                                OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text(stringResource(R.string.addentry_title_label)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                        value = description,
                                        onValueChange = { description = it },
                                        label = { Text(stringResource(R.string.addentry_event_description_label)) },
                                        minLines = 5,
                                        modifier = Modifier.fillMaxWidth()
                                )
                        }
                }
        )
}
