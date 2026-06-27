package com.mj.yaja.ui.screens

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.theme.JournalTheme
import com.mj.yaja.ui.design.ProvideAnimationPreference
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.floatSpring
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.ui.utils.MarkdownVisualTransformation
import com.mj.yaja.ui.utils.ShortcodeManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

enum class QuickAddKind {
        TODO,
        EVENT
}

class QuickCaptureActivity : ComponentActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                enableEdgeToEdge()

                setContent {
                        val settingsRepository = remember { com.mj.yaja.data.SettingsRepository.getInstance(applicationContext) }
                        val animationPreference by settingsRepository.animationPreference.collectAsState(initial = AnimationPreference.FULL)
                        ProvideAnimationPreference(animationPreference) {
                                JournalTheme {
                                        QuickCaptureDialog(
                                                onDismissRequest = { finish() },
                                                onSave = { text -> saveEntry(text) }
                                        )
                                }
                        }
                }
        }
        private fun saveEntry(text: String) {
                if (text.isBlank()) {
                        finish()
                        return
                }
                lifecycleScope.launch {
                        try {
                                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                                val timeString = LocalTime.now().format(timeFormatter)
                                val finalEntry = "<!--time:$timeString-->\n${text.trim()}"
                                withTimeout(QUICK_CAPTURE_SAVE_TIMEOUT_MS) {
                                        withContext(Dispatchers.IO) {
                                                val settingsRepository =
                                                        com.mj.yaja.data.SettingsRepository.getInstance(applicationContext)
                                                val fileManager =
                                                        com.mj.yaja.data.MarkdownFileManager.getInstance(
                                                                applicationContext,
                                                                settingsRepository
                                                        )
                                                val result = fileManager.tryAddEntryForDate(LocalDate.now(), finalEntry)
                                                if (!result.success) error("Quick capture save failed")
                                        }
                                }
                        } catch (t: Throwable) {
                                Log.e(TAG, "Quick capture save failed or timed out", t)
                                Toast.makeText(
                                        this@QuickCaptureActivity,
                                        getString(R.string.quick_capture_save_delayed),
                                        Toast.LENGTH_SHORT
                                ).show()
                        } finally {
                                finish()
                        }
                }
        }

        private companion object {
                private const val TAG = "QuickCaptureActivity"
                private const val QUICK_CAPTURE_SAVE_TIMEOUT_MS = 15_000L
        }
}

@Composable
fun QuickCaptureDialog(onDismissRequest: () -> Unit, onSave: (String) -> Unit) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val settingsRepository = remember { com.mj.yaja.data.SettingsRepository.getInstance(context) }
        val customShortcodes by
                settingsRepository.customShortcodes.collectAsState(initial = emptyMap())

        var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
        val focusRequester = remember { FocusRequester() }
        var showSuccess by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
                delay(100)
                try {
                        focusRequester.requestFocus()
                } catch (e: Exception) {
                        // Ignore focus errors
                }
        }

        if (showSuccess) {
                AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {},
                        title = {
                                Text(
                                        stringResource(R.string.quick_capture_title),
                                        style = MaterialTheme.typography.headlineSmall
                                )
                        },
                        text = {
                                Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        var isAnimating by remember { mutableStateOf(false) }
                                        LaunchedEffect(Unit) { isAnimating = true }
                                        val scale by
                                                animateFloatAsState(
                                                        targetValue = if (isAnimating) 1f else 0f,
                                                        animationSpec =
                                                                LocalAnimationPreference.current.floatSpring(
                                                                        dampingRatio =
                                                                                Spring.DampingRatioMediumBouncy,
                                                                        stiffness =
                                                                                Spring.StiffnessLow
                                                                ),
                                                        label = "SuccessScale"
                                                )
                                        Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = stringResource(R.string.cd_saved),
                                                modifier = Modifier.size(64.dp).scale(scale),
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.large
                )

                LaunchedEffect(Unit) {
                        delay(800)
                        onSave(textFieldValue.text)
                }
        } else {
                AlertDialog(
                        onDismissRequest = onDismissRequest,
                        confirmButton = {
                                Button(
                                        onClick = { showSuccess = true },
                                        enabled = textFieldValue.text.isNotBlank()
                                ) { Text(stringResource(R.string.action_save)) }
                        },
                        dismissButton = {
                                TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.action_cancel)) }
                        },
                        title = { Text(stringResource(R.string.quick_capture_title)) },
                        text = {
                                Column {
                                        OutlinedTextField(
                                                value = textFieldValue,
                                                onValueChange = { newValue ->
                                                        textFieldValue =
                                                                handleEditorValueChange(
                                                                        textFieldValue,
                                                                        newValue,
                                                                        customShortcodes
                                                                )
                                                },
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .focusRequester(focusRequester),
                                                placeholder = { Text(stringResource(R.string.quick_capture_hint)) },
                                                visualTransformation = MarkdownVisualTransformation(),
                                                minLines = 5,
                                                maxLines = 25,
                                                shape = MaterialTheme.shapes.large,
                                                colors =
                                                        OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor =
                                                                        MaterialTheme.colorScheme.primary,
                                                                unfocusedBorderColor =
                                                                        MaterialTheme.colorScheme.outline
                                                        )
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = MaterialTheme.shapes.medium,
                                                modifier = Modifier.fillMaxWidth()
                                        ) {
                                                Text(
                                                        text = stringResource(R.string.quick_capture_helper_shortcodes),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                                )
                                        }
                                }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.large
                )
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTodoDialog(
        onDismissRequest: () -> Unit,
        onSave: (String, LocalDate, QuickAddKind) -> Unit,
        initialDate: LocalDate = LocalDate.now(),
        allowDateSelection: Boolean = false,
        title: String,
        initialKind: QuickAddKind = QuickAddKind.TODO
) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val settingsRepository = remember { com.mj.yaja.data.SettingsRepository.getInstance(context) }
        val customShortcodes by
                settingsRepository.customShortcodes.collectAsState(initial = emptyMap())

        var quickAddKind by remember { mutableStateOf(initialKind) }
        var textFieldValue by remember {
                mutableStateOf(
                        if (initialKind == QuickAddKind.TODO) {
                                TextFieldValue("[ ] ", selection = TextRange(4))
                        } else {
                                TextFieldValue("")
                        }
                )
        }
        var selectedDate by remember { mutableStateOf(initialDate) }
        var showDatePicker by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        var showSuccess by remember { mutableStateOf(false) }
        val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
        val datePickerState =
                rememberDatePickerState(
                        initialSelectedDateMillis =
                                selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                                        .toEpochMilli()
                )

        LaunchedEffect(Unit) {
                delay(100)
                try {
                        focusRequester.requestFocus()
                } catch (_: Exception) {
                }
        }

        if (showSuccess) {
                AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {},
                        title = {
                                Text(
                                        title,
                                        style = MaterialTheme.typography.headlineSmall
                                )
                        },
                        text = {
                                Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        var isAnimating by remember { mutableStateOf(false) }
                                        LaunchedEffect(Unit) { isAnimating = true }
                                        val scale by
                                                animateFloatAsState(
                                                        targetValue = if (isAnimating) 1f else 0f,
                                                        animationSpec =
                                                                LocalAnimationPreference.current.floatSpring(
                                                                        dampingRatio =
                                                                                Spring.DampingRatioMediumBouncy,
                                                                        stiffness =
                                                                                Spring.StiffnessLow
                                                                ),
                                                        label = "QuickTodoSuccessScale"
                                                )
                                        Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = stringResource(R.string.cd_saved),
                                                modifier = Modifier.size(64.dp).scale(scale),
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.large
                )

                LaunchedEffect(Unit) {
                        delay(800)
                        onSave(textFieldValue.text, selectedDate, quickAddKind)
                }
        } else {
                AlertDialog(
                        onDismissRequest = onDismissRequest,
                        confirmButton = {
                                Button(
                                        onClick = { showSuccess = true },
                                        enabled = quickAddPayloadText(textFieldValue.text, quickAddKind) != null
                                ) { Text(stringResource(R.string.action_save)) }
                        },
                        dismissButton = {
                                TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.action_cancel)) }
                        },
                        title = { Text(title) },
                        text = {
                                Column {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                QuickAddModeChip(
                                                        label = stringResource(R.string.addentry_todo_chip),
                                                        icon = Icons.Rounded.TaskAlt,
                                                        selected = quickAddKind == QuickAddKind.TODO,
                                                        onClick = {
                                                                quickAddKind = QuickAddKind.TODO
                                                                textFieldValue = normalizeQuickAddTextField(textFieldValue, QuickAddKind.TODO)
                                                        }
                                                )
                                                QuickAddModeChip(
                                                        label = stringResource(R.string.addentry_event_chip),
                                                        icon = Icons.Rounded.Event,
                                                        selected = quickAddKind == QuickAddKind.EVENT,
                                                        onClick = {
                                                                quickAddKind = QuickAddKind.EVENT
                                                                textFieldValue = normalizeQuickAddTextField(textFieldValue, QuickAddKind.EVENT)
                                                        }
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedTextField(
                                                value = textFieldValue,
                                                onValueChange = { newValue ->
                                                        val expandedValue =
                                                                handleEditorValueChange(
                                                                        textFieldValue,
                                                                        newValue,
                                                                        customShortcodes
                                                                )
                                                        textFieldValue =
                                                                normalizeQuickAddTextField(
                                                                        expandedValue,
                                                                        quickAddKind
                                                                )
                                                },
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .focusRequester(focusRequester),
                                                placeholder = {
                                                        Text(
                                                                if (quickAddKind == QuickAddKind.TODO) {
                                                                        stringResource(R.string.quick_todo_placeholder)
                                                                } else {
                                                                        stringResource(R.string.quick_event_placeholder)
                                                                }
                                                        )
                                                },
                                                visualTransformation = MarkdownVisualTransformation(),
                                                minLines = 4,
                                                maxLines = 20,
                                                shape = MaterialTheme.shapes.large,
                                                colors =
                                                        OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor =
                                                                        MaterialTheme.colorScheme.primary,
                                                                unfocusedBorderColor =
                                                                        MaterialTheme.colorScheme.outline
                                                        )
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        if (allowDateSelection) {
                                                val dateLabel =
                                                        if (quickAddKind == QuickAddKind.TODO) {
                                                                stringResource(R.string.quick_todo_task_date)
                                                        } else {
                                                                stringResource(R.string.quick_todo_event_date)
                                                        }
                                                Surface(
                                                        onClick = { showDatePicker = true },
                                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
                                                        shape = MaterialTheme.shapes.large,
                                                        modifier = Modifier.fillMaxWidth()
                                                ) {
                                                        Row(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .padding(horizontal = 16.dp, vertical = 16.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                                Text(
                                                                        text = stringResource(
                                                                                R.string.quick_todo_date_button_format,
                                                                                dateLabel,
                                                                                selectedDate.format(dateFormatter)
                                                                        ),
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                                        modifier = Modifier.weight(1f)
                                                                )
                                                                Icon(
                                                                        imageVector = Icons.Rounded.EditCalendar,
                                                                        contentDescription = stringResource(
                                                                                R.string.quick_todo_pick_date_cd_format,
                                                                                dateLabel.lowercase()
                                                                        ),
                                                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                                                )
                                                        }
                                                }
                                        } else {
                                                Surface(
                                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = MaterialTheme.shapes.medium,
                                                        modifier = Modifier.fillMaxWidth()
                                                ) {
                                                        Text(
                                                                text = if (quickAddKind == QuickAddKind.TODO) {
                                                                        stringResource(R.string.quick_todo_selected_day_hint)
                                                                } else {
                                                                        stringResource(R.string.quick_event_selected_day_hint)
                                                                },
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                                        )
                                                }
                                        }
                                }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.large
                )
        }

        if (showDatePicker) {
                DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                datePickerState.selectedDateMillis?.let { millis ->
                                                        selectedDate =
                                                                java.time.Instant.ofEpochMilli(millis)
                                                                        .atZone(ZoneOffset.UTC)
                                                                        .toLocalDate()
                                                }
                                                showDatePicker = false
                                        }
                                ) {
                                        Text(stringResource(R.string.action_select))
                                }
                        },
                        dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                        Text(stringResource(R.string.action_cancel))
                                }
                        }
                ) {
                        DatePicker(state = datePickerState, showModeToggle = false)
                }
        }
}

@Composable
private fun QuickAddModeChip(
        label: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        selected: Boolean,
        onClick: () -> Unit
) {
        Surface(
                onClick = onClick,
                color =
                        if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                        } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                shape = MaterialTheme.shapes.medium
        ) {
                Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint =
                                        if (selected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                        )
                        Text(
                                text = label,
                                color =
                                        if (selected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                        )
                }
        }
}

private fun quickAddPayloadText(text: String, kind: QuickAddKind): String? =
        when (kind) {
                QuickAddKind.TODO ->
                        text.trim()
                                .removePrefix("[ ]")
                                .removePrefix("[x]")
                                .removePrefix("[X]")
                                .trim()
                                .takeIf { it.isNotBlank() }
                QuickAddKind.EVENT ->
                        text.trim()
                                .removePrefix("[ ]")
                                .removePrefix("[x]")
                                .removePrefix("[X]")
                                .trim()
                                .takeIf { it.isNotBlank() }
        }

private fun normalizeQuickAddTextField(
        value: TextFieldValue,
        kind: QuickAddKind
): TextFieldValue {
        val expanded = value.text
        return when (kind) {
                QuickAddKind.TODO -> {
                        val normalized =
                                if (expanded.startsWith("[ ]") ||
                                                expanded.startsWith("[x]") ||
                                                expanded.startsWith("[X]")
                                ) {
                                        expanded
                                } else {
                                        "[ ] " + expanded.removePrefix("- ").trimStart()
                                }
                        val cursorShift = if (normalized == expanded) 0 else 4
                        value.copy(
                                text = normalized,
                                selection =
                                        TextRange(
                                                (value.selection.end + cursorShift)
                                                        .coerceAtLeast(4)
                                                        .coerceAtMost(normalized.length)
                                        )
                        )
                }
                QuickAddKind.EVENT -> {
                        val normalized =
                                expanded.removePrefix("[ ]")
                                        .removePrefix("[x]")
                                        .removePrefix("[X]")
                                        .trimStart()
                        value.copy(
                                text = normalized,
                                selection =
                                        TextRange(
                                                value.selection.end.coerceAtMost(normalized.length)
                                        )
                        )
                }
        }
}
