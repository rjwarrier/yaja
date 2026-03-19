package com.mj.yaja.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mj.yaja.ui.theme.JournalTheme
import com.mj.yaja.ui.utils.MarkdownVisualTransformation
import com.mj.yaja.ui.utils.ShortcodeManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

class QuickCaptureActivity : ComponentActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                enableEdgeToEdge()

                // This is a transparent activity. We don't want a full screen background.
                setContent {
                        JournalTheme {
                                QuickCaptureDialog(
                                        onDismissRequest = { finish() },
                                        onSave = { text -> saveEntry(text) }
                                )
                        }
                }
        }
        private fun saveEntry(text: String) {
                if (text.isNotBlank()) {
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        val timeString = LocalTime.now().format(timeFormatter)
                        val finalEntry = "<!--time:$timeString-->\n${text.trim()}"

                        // Use singletons so the shared in-memory cache is updated
                        val settingsRepository =
                                com.mj.yaja.data.SettingsRepository.getInstance(applicationContext)
                        val fileManager =
                                com.mj.yaja.data.MarkdownFileManager.getInstance(
                                        applicationContext,
                                        settingsRepository
                                )
                        fileManager.addEntryForDate(LocalDate.now(), finalEntry)

                        // Replaced toast with the visual checkmark animation in the dialog
                }
                finish()
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
                        title = { Text("Quick Capture") },
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
                                                                spring(
                                                                        dampingRatio =
                                                                                Spring.DampingRatioMediumBouncy,
                                                                        stiffness =
                                                                                Spring.StiffnessLow
                                                                ),
                                                        label = "SuccessScale"
                                                )
                                        Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "Saved",
                                                modifier = Modifier.size(64.dp).scale(scale),
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.medium
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
                                ) { Text("Save") }
                        },
                        dismissButton = {
                                TextButton(onClick = onDismissRequest) { Text("Cancel") }
                        },
                        title = { Text("Quick Capture") },
                        text = {
                                OutlinedTextField(
                                        value = textFieldValue,
                                        onValueChange = { newValue ->
                                                val expanded =
                                                        ShortcodeManager.expand(
                                                                newValue.text,
                                                                customShortcodes
                                                        )

                                                // Auto-continue todo lists on Enter
                                                val cursorPos = newValue.selection.end
                                                val newlineInserted =
                                                        expanded.length ==
                                                                textFieldValue.text.length + 1 &&
                                                                cursorPos > 0 &&
                                                                expanded.getOrNull(cursorPos - 1) ==
                                                                        '\n'
                                                val todoPrefix =
                                                        if (newlineInserted) {
                                                                val beforeCursor =
                                                                        expanded.substring(
                                                                                0,
                                                                                cursorPos - 1
                                                                        )
                                                                val prevLineStart =
                                                                        beforeCursor.lastIndexOf(
                                                                                '\n'
                                                                        ) + 1
                                                                val prevLine =
                                                                        beforeCursor
                                                                                .substring(
                                                                                        prevLineStart
                                                                                )
                                                                                .trimStart()
                                                                if (prevLine.startsWith("[ ] ") ||
                                                                                prevLine.startsWith(
                                                                                        "[x] "
                                                                                ) ||
                                                                                prevLine == "[ ]" ||
                                                                                prevLine == "[x]"
                                                                )
                                                                        "[ ] "
                                                                else null
                                                        } else null

                                                if (todoPrefix != null) {
                                                        val newText =
                                                                expanded.substring(0, cursorPos) +
                                                                        todoPrefix +
                                                                        expanded.substring(
                                                                                cursorPos
                                                                        )
                                                        textFieldValue =
                                                                newValue.copy(
                                                                        text = newText,
                                                                        selection =
                                                                                TextRange(
                                                                                        cursorPos +
                                                                                                todoPrefix
                                                                                                        .length
                                                                                )
                                                                )
                                                } else if (expanded != newValue.text) {
                                                        val diff =
                                                                expanded.length -
                                                                        newValue.text.length
                                                        textFieldValue =
                                                                newValue.copy(
                                                                        text = expanded,
                                                                        selection =
                                                                                TextRange(
                                                                                        cursorPos +
                                                                                                diff
                                                                                )
                                                                )
                                                } else {
                                                        textFieldValue = newValue
                                                }
                                        },
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .focusRequester(focusRequester),
                                        placeholder = { Text("What happened?") },
                                        visualTransformation = MarkdownVisualTransformation(),
                                        minLines = 5,
                                        maxLines = 25,
                                        colors =
                                                OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor =
                                                                MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor =
                                                                MaterialTheme.colorScheme.outline
                                                )
                                )
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.medium
                )
        }
}
