package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mj.yaja.R
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun CalendarQuickActions(
        onNavigateToTimeline: () -> Unit,
        onJumpToDate: () -> Unit
) {
        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        CalendarActionCard(
                                title = stringResource(R.string.calendar_quick_action_timeline_title),
                                subtitle = stringResource(R.string.calendar_quick_action_timeline_subtitle),
                                icon = Icons.Rounded.Timeline,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                emphasized = false,
                                onClick = onNavigateToTimeline
                        )
                        CalendarActionCard(
                                title = stringResource(R.string.calendar_quick_action_jump_title),
                                subtitle = stringResource(R.string.calendar_quick_action_jump_subtitle),
                                icon = Icons.Rounded.EditCalendar,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                emphasized = true,
                                onClick = onJumpToDate
                        )
                }
        }
}

@Composable
fun CalendarJumpToDateDialog(
        jumpDateValue: androidx.compose.ui.text.input.TextFieldValue,
        jumpDateError: Boolean,
        onValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
        onDismiss: () -> Unit,
        onInvalidDate: () -> Unit,
        onDatePicked: (LocalDate) -> Unit,
        onCurrentMonthChange: (YearMonth) -> Unit,
        onViewModeChange: (CalendarViewMode) -> Unit
) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
                delay(100)
                try {
                        focusRequester.requestFocus()
                } catch (_: Exception) {}
        }
        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.calendar_quick_action_jump_title)) },
                text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                        value = jumpDateValue,
                                        onValueChange = onValueChange,
                                        modifier =
                                                        Modifier.fillMaxWidth()
                                                                .focusRequester(focusRequester),
                                        placeholder = { Text(stringResource(R.string.calendar_jump_placeholder)) },
                                        isError = jumpDateError,
                                        singleLine = true,
                                        keyboardOptions =
                                                KeyboardOptions(
                                                        keyboardType =
                                                                androidx.compose.ui.text.input
                                                                        .KeyboardType.Number,
                                                        imeAction =
                                                                androidx.compose.ui.text.input
                                                                        .ImeAction.Go
                                                ),
                                        keyboardActions =
                                                KeyboardActions(
                                                        onGo = {
                                                                val parsed =
                                                                        parseJumpDate(
                                                                                jumpDateValue.text
                                                                        )
                                                                if (parsed != null) {
                                                                        onCurrentMonthChange(
                                                                                YearMonth.from(
                                                                                        parsed
                                                                                )
                                                                        )
                                                                        onViewModeChange(
                                                                                CalendarViewMode
                                                                                        .DAYS
                                                                        )
                                                                        onDatePicked(parsed)
                                                                        onDismiss()
                                                                } else {
                                                                        onInvalidDate()
                                                                }
                                                        }
                                                )
                                )
                                if (jumpDateError) {
                                        Text(
                                                stringResource(R.string.calendar_jump_invalid_date),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                        )
                                }
                        }
                },
                confirmButton = {
                        Button(
                                onClick = {
                                        val parsed = parseJumpDate(jumpDateValue.text)
                                        if (parsed != null) {
                                                onCurrentMonthChange(YearMonth.from(parsed))
                                                onViewModeChange(CalendarViewMode.DAYS)
                                                onDatePicked(parsed)
                                                onDismiss()
                                        } else {
                                                onInvalidDate()
                                        }
                                },
                                enabled = jumpDateValue.text.filter { it.isDigit() }.length >= 6
                        ) { Text(stringResource(R.string.calendar_action_go)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
                shape = MaterialTheme.shapes.medium
        )
}

@Composable
fun CalendarFutureDateDialogs(
        showFutureDateDialog: Boolean,
        showEnableFutureDateDialog: Boolean,
        pendingFutureDate: LocalDate?,
        onDismissFutureDate: () -> Unit,
        onDismissEnableFutureDate: () -> Unit,
        onConfirmFutureDate: (LocalDate) -> Unit,
        onEnableFutureDate: (LocalDate) -> Unit
) {
        if (showFutureDateDialog && pendingFutureDate != null) {
                AlertDialog(
                        onDismissRequest = onDismissFutureDate,
                        title = { Text(stringResource(R.string.home_future_date_title)) },
                        text = {
                                Text(
                                        stringResource(
                                                R.string.home_future_date_message_format,
                                                pendingFutureDate.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
                                        )
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                onConfirmFutureDate(pendingFutureDate)
                                                onDismissFutureDate()
                                        }
                                ) { Text(stringResource(R.string.action_yes)) }
                        },
                        dismissButton = {
                                TextButton(onClick = onDismissFutureDate) { Text(stringResource(R.string.action_no)) }
                        }
                )
        }

        if (showEnableFutureDateDialog && pendingFutureDate != null) {
                AlertDialog(
                        onDismissRequest = onDismissEnableFutureDate,
                        title = { Text(stringResource(R.string.calendar_enable_future_dates_title)) },
                        text = {
                                Text(
                                        stringResource(R.string.calendar_enable_future_dates_message)
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                onEnableFutureDate(pendingFutureDate)
                                                onDismissEnableFutureDate()
                                        }
                                ) { Text(stringResource(R.string.calendar_action_enable)) }
                        },
                        dismissButton = {
                                TextButton(onClick = onDismissEnableFutureDate) {
                                        Text(stringResource(R.string.action_cancel))
                                }
                        }
                )
        }
}

@Composable
fun CalendarActionCard(
        title: String,
        subtitle: String,
        icon: ImageVector,
        modifier: Modifier = Modifier,
        emphasized: Boolean = false,
        onClick: () -> Unit
) {
        ElevatedCard(
                onClick = onClick,
                modifier = modifier,
                colors =
                        CardDefaults.elevatedCardColors(
                                containerColor =
                                        if (emphasized) {
                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                        } else {
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                        }
                        ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
        ) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Surface(
                                color =
                                        if (emphasized) {
                                                MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                                MaterialTheme.colorScheme.primaryContainer
                                        },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                modifier = Modifier.size(40.dp)
                        ) {
                                androidx.compose.foundation.layout.Box(
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                        )
                                }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                                Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color =
                                                if (emphasized) {
                                                        MaterialTheme.colorScheme.onSecondaryContainer
                                                } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                }
                                )
                                Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                                if (emphasized) {
                                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
                                                } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                )
                        }
                        Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint =
                                        if (emphasized) {
                                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
                                        } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                        )
                }
        }
}
