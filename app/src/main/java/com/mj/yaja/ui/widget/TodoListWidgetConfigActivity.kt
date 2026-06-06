package com.mj.yaja.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mj.yaja.ui.theme.JournalTheme
import kotlin.math.roundToInt

class TodoListWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_CANCELED)

        setContent {
            JournalTheme {
                TodoListWidgetConfigDialog(
                    initialRadius = TodoListWidgetProvider.getCornerRadius(this, appWidgetId),
                    initialPadding = TodoListWidgetProvider.getPadding(this, appWidgetId),
                    initialShowCompleted = TodoListWidgetProvider.getShowCompleted(this, appWidgetId),
                    initialIncludeEvents = TodoListWidgetProvider.getIncludeEvents(this, appWidgetId),
                    initialShowHeader = TodoListWidgetProvider.getShowHeader(this, appWidgetId),
                    initialAutoHideWhenEmpty =
                        TodoListWidgetProvider.getAutoHideWhenEmpty(this, appWidgetId) &&
                            !TodoListWidgetProvider.getShowCompleted(this, appWidgetId),
                    onApply = { radius, padding, showCompleted, includeEvents, showHeader, autoHideWhenEmpty ->
                        applyAndFinish(radius, padding, showCompleted, includeEvents, showHeader, autoHideWhenEmpty)
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun applyAndFinish(
        radiusDp: Int,
        paddingDp: Int,
        showCompleted: Boolean,
        includeEvents: Boolean,
        showHeader: Boolean,
        autoHideWhenEmpty: Boolean
    ) {
        TodoListWidgetProvider.saveOptions(
            context = this,
            appWidgetId = appWidgetId,
            radiusDp = radiusDp,
            paddingDp = paddingDp,
            showCompleted = showCompleted,
            includeEvents = includeEvents,
            showHeader = showHeader,
            autoHideWhenEmpty = autoHideWhenEmpty
        )

        WidgetRefreshCoordinator.requestTodoListUpdate(applicationContext)

        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}

@Composable
private fun TodoListWidgetConfigDialog(
    initialRadius: Int,
    initialPadding: Int,
    initialShowCompleted: Boolean,
    initialIncludeEvents: Boolean,
    initialShowHeader: Boolean,
    initialAutoHideWhenEmpty: Boolean,
    onApply: (Int, Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var radius by remember { mutableFloatStateOf(initialRadius.toFloat()) }
    var padding by remember { mutableFloatStateOf(initialPadding.toFloat()) }
    var showCompleted by remember { mutableStateOf(initialShowCompleted) }
    var includeEvents by remember { mutableStateOf(initialIncludeEvents) }
    var showHeader by remember { mutableStateOf(initialShowHeader) }
    var autoHideWhenEmpty by remember { mutableStateOf(initialAutoHideWhenEmpty) }

    AlertDialog(
        modifier = Modifier.systemBarsPadding(),
        onDismissRequest = onCancel,
        title = { Text("Todo Widget") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Rounded, heatmap-like todo list widget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Corner radius: ${radius.roundToInt()}dp",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = radius,
                        onValueChange = { radius = it },
                        valueRange = 0f..40f,
                        steps = 19
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Padding: ${padding.roundToInt()}dp",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = padding,
                        onValueChange = { padding = it },
                        valueRange = 0f..28f,
                        steps = 13
                    )
                }

                ToggleRow(
                    title = "Show completed tasks",
                    subtitle = "Include checked items in widget list",
                    checked = showCompleted,
                    onCheckedChange = {
                        showCompleted = it
                        if (it) autoHideWhenEmpty = false
                    }
                )

                ToggleRow(
                    title = "Include events",
                    subtitle = "Show event entries alongside todos in the widget",
                    checked = includeEvents,
                    onCheckedChange = { includeEvents = it }
                )

                ToggleRow(
                    title = "Show Todo header",
                    subtitle = "Show or hide widget title row",
                    checked = showHeader,
                    onCheckedChange = { showHeader = it }
                )

                ToggleRow(
                    title = "Auto hide widget when all todos completed",
                    subtitle =
                        if (showCompleted) {
                            "Unavailable while completed tasks are shown"
                        } else {
                            "Make the widget transparent when there are no pending tasks"
                        },
                    checked = autoHideWhenEmpty,
                    enabled = !showCompleted,
                    onCheckedChange = { autoHideWhenEmpty = it && !showCompleted }
                )
            }
        },
        confirmButton = {
            WidgetConfigApplyButton(onClick = {
                onApply(
                    radius.roundToInt(),
                    padding.roundToInt(),
                    showCompleted,
                    includeEvents,
                    showHeader,
                    autoHideWhenEmpty && !showCompleted
                )
            })
        },
        dismissButton = {
            WidgetConfigCancelButton(onClick = onCancel)
        }
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
