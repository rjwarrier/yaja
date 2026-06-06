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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        onRecordedTimeClick: () -> Unit,
        dayFormatter: DateTimeFormatter,
        weekdayFormatter: DateTimeFormatter,
        monthYearFormatter: DateTimeFormatter
) {
        val mentionedEventTime =
                remember(selectedEntryKind, entryText, recordedTime) {
                        if (selectedEntryKind != EntryKind.EVENT) {
                                null
                        } else {
                                extractMentionedEventTime(entryText)
                                        ?.takeIf { time ->
                                                time != recordedTime && time != "${recordedTime} Hrs"
                                        }
                        }
                }
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
                                                text = recordedTime ?: "Set time",
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
                                                        text = "Event",
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
        var selectedHour by remember { mutableStateOf(parsedTime.hour) }
        var selectedMinute by remember { mutableStateOf(parsedTime.minute) }

        AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                        TextButton(
                                onClick = {
                                        onConfirm(
                                                String.format(
                                                        "%02d:%02d",
                                                        selectedHour,
                                                        selectedMinute
                                                )
                                        )
                                }
                        ) {
                                Text("Set")
                        }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                },
                title = { Text("Pick time") },
                text = {
                        Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                        ) {
                                CustomTimeInput(
                                        hour = selectedHour,
                                        minute = selectedMinute,
                                        onTimeChange = { h, m ->
                                                selectedHour = h
                                                selectedMinute = m
                                        }
                                )
                        }
                }
        )
}

@Composable
fun CustomTimeInput(
        hour: Int,
        minute: Int,
        onTimeChange: (Int, Int) -> Unit
) {
        var hourText by remember { mutableStateOf(String.format("%02d", hour)) }
        var minuteText by remember { mutableStateOf(String.format("%02d", minute)) }

        androidx.compose.runtime.LaunchedEffect(hour) {
                val parsed = hourText.toIntOrNull()
                if (parsed != hour) {
                        hourText = String.format("%02d", hour)
                }
        }

        androidx.compose.runtime.LaunchedEffect(minute) {
                val parsed = minuteText.toIntOrNull()
                if (parsed != minute) {
                        minuteText = String.format("%02d", minute)
                }
        }

        androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                                value = hourText,
                                onValueChange = { newValue ->
                                        val filtered = newValue.filter { it.isDigit() }.take(2)
                                        val h = filtered.toIntOrNull()
                                        if (h != null) {
                                                val clamped = h.coerceIn(0, 23)
                                                hourText = if (clamped != h) clamped.toString() else filtered
                                                onTimeChange(clamped, minute)
                                        } else {
                                                hourText = ""
                                                onTimeChange(0, minute)
                                        }
                                },
                                modifier = Modifier.width(80.dp),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                singleLine = true
                        )
                        Text("Hour", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                }

                Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 24.dp))

                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                                value = minuteText,
                                onValueChange = { newValue ->
                                        val filtered = newValue.filter { it.isDigit() }.take(2)
                                        val m = filtered.toIntOrNull()
                                        if (m != null) {
                                                val clamped = m.coerceIn(0, 59)
                                                minuteText = if (clamped != m) clamped.toString() else filtered
                                                onTimeChange(hour, clamped)
                                        } else {
                                                minuteText = ""
                                                onTimeChange(hour, 0)
                                        }
                                },
                                modifier = Modifier.width(80.dp),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                singleLine = true
                        )
                        Text("Minute", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventInputDialog(
        onDismiss: () -> Unit,
        onConfirm: (time: String, title: String, description: String) -> Unit
) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        
        val parsedTime = LocalTime.now()
        var selectedHour by remember { mutableStateOf(parsedTime.hour) }
        var selectedMinute by remember { mutableStateOf(parsedTime.minute) }

        AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                        TextButton(
                                onClick = {
                                        val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
                                        onConfirm(timeString, title.trim(), description.trim())
                                },
                                enabled = title.isNotBlank()
                        ) {
                                Text("OK")
                        }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                },
                title = { Text("New Event") },
                text = {
                        androidx.compose.foundation.layout.Column(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                CustomTimeInput(
                                        hour = selectedHour,
                                        minute = selectedMinute,
                                        onTimeChange = { h, m ->
                                                selectedHour = h
                                                selectedMinute = m
                                        }
                                )
                                
                                OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text("Title") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                )
                                
                                OutlinedTextField(
                                        value = description,
                                        onValueChange = { description = it },
                                        label = { Text("Event Description") },
                                        minLines = 5,
                                        modifier = Modifier.fillMaxWidth()
                                )
                        }
                }
        )
}
