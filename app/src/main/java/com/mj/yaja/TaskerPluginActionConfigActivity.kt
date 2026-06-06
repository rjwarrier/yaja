package com.mj.yaja

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.mj.yaja.data.SettingsRepository
import com.mj.yaja.ui.theme.JournalTheme

@TaskerInputRoot
class YajaTaskerActionInput @JvmOverloads constructor(
    @field:TaskerInputField("action_kind")
    var actionKind: String = TaskerIntegration.PluginActionKind.ADD_ENTRY.name,
    @field:TaskerInputField("entry_text")
    var entryText: String = "",
    @field:TaskerInputField("match_header")
    var matchHeader: String = ""
)

class YajaTaskerActionRunner : TaskerPluginRunnerActionNoOutput<YajaTaskerActionInput>() {
    override fun run(
        context: Context,
        input: TaskerInput<YajaTaskerActionInput>
    ): TaskerPluginResult<Unit> {
        val regular = input.regular
        val entryText = regular.entryText.trim()
        val actionKind = TaskerIntegration.parsePluginActionKind(regular.actionKind)
        val matchHeader = regular.matchHeader.trim()
        if (actionKind == TaskerIntegration.PluginActionKind.APPEND && matchHeader.isBlank()) {
            return TaskerPluginResultError(1, "Match / header is empty")
        }
        if (entryText.isBlank()) {
            return TaskerPluginResultError(1, if (actionKind == TaskerIntegration.PluginActionKind.APPEND) "Line to append is empty" else "Entry text is empty")
        }
        return try {
            TaskerIntegration.executePluginAction(
                context = context.applicationContext,
                settingsRepository = SettingsRepository.getInstance(context.applicationContext),
                actionKind = actionKind,
                entryText = regular.entryText,
                matchHeader = regular.matchHeader
            )
            TaskerPluginResultSucess()
        } catch (t: Throwable) {
            TaskerPluginResultError(t)
        }
    }
}

class YajaTaskerActionConfigHelper(config: TaskerPluginConfig<YajaTaskerActionInput>) :
    TaskerPluginConfigHelperNoOutput<YajaTaskerActionInput, YajaTaskerActionRunner>(config) {

    override val runnerClass: Class<YajaTaskerActionRunner>
        get() = YajaTaskerActionRunner::class.java

    override val inputClass: Class<YajaTaskerActionInput>
        get() = YajaTaskerActionInput::class.java

    override fun addToStringBlurb(
        input: TaskerInput<YajaTaskerActionInput>,
        blurbBuilder: StringBuilder
    ) {
        blurbBuilder.append(
            TaskerIntegration.buildPluginBlurb(
                TaskerIntegration.parsePluginActionKind(input.regular.actionKind),
                input.regular.entryText,
                input.regular.matchHeader
            )
        )
    }
}

class TaskerPluginActionConfigActivity : ComponentActivity(), TaskerPluginConfig<YajaTaskerActionInput> {
    override val context: Context
        get() = applicationContext

    private val helper by lazy { YajaTaskerActionConfigHelper(this) }

    private var actionKind by mutableStateOf(TaskerIntegration.PluginActionKind.ADD_ENTRY)
    private var entryText by mutableStateOf("")
    private var matchHeader by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper.onCreate()
        setContent {
            JournalTheme {
                TaskerPluginActionConfigScreen(
                    initialKind = actionKind,
                    initialText = entryText,
                    initialMatchHeader = matchHeader,
                    onCancel = { finish() },
                    onSave = { kind, header, text ->
                        actionKind = kind
                        matchHeader = header
                        entryText = text
                        helper.finishForTasker()
                    }
                )
            }
        }
    }

    override fun assignFromInput(input: TaskerInput<YajaTaskerActionInput>) {
        actionKind = TaskerIntegration.parsePluginActionKind(input.regular.actionKind)
        entryText = input.regular.entryText
        matchHeader = input.regular.matchHeader
    }

    override val inputForTasker: TaskerInput<YajaTaskerActionInput>
        get() = TaskerInput(
            YajaTaskerActionInput(
                actionKind = actionKind.name,
                entryText = entryText,
                matchHeader = matchHeader
            )
        )
}

@Composable
private fun TaskerPluginActionConfigScreen(
    initialKind: TaskerIntegration.PluginActionKind,
    initialText: String,
    initialMatchHeader: String,
    onCancel: () -> Unit,
    onSave: (TaskerIntegration.PluginActionKind, String, String) -> Unit
) {
    var actionKind by rememberSaveable { mutableStateOf(initialKind) }
    var entryText by rememberSaveable { mutableStateOf(initialText) }
    var matchHeader by rememberSaveable { mutableStateOf(initialMatchHeader) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Yaja Quick Capture",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Configure what Tasker should quickly capture in Yaja.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PluginChoiceChip(
                    label = "Add Entry",
                    selected = actionKind == TaskerIntegration.PluginActionKind.ADD_ENTRY,
                    onClick = { actionKind = TaskerIntegration.PluginActionKind.ADD_ENTRY }
                )
                PluginChoiceChip(
                    label = "Add Event",
                    selected = actionKind == TaskerIntegration.PluginActionKind.ADD_EVENT,
                    onClick = { actionKind = TaskerIntegration.PluginActionKind.ADD_EVENT }
                )
                PluginChoiceChip(
                    label = "Add Todo",
                    selected = actionKind == TaskerIntegration.PluginActionKind.ADD_TODO,
                    onClick = { actionKind = TaskerIntegration.PluginActionKind.ADD_TODO }
                )
                PluginChoiceChip(
                    label = "Append",
                    selected = actionKind == TaskerIntegration.PluginActionKind.APPEND,
                    onClick = { actionKind = TaskerIntegration.PluginActionKind.APPEND }
                )
            }

            if (actionKind == TaskerIntegration.PluginActionKind.APPEND) {
                OutlinedTextField(
                    value = matchHeader,
                    onValueChange = { matchHeader = it },
                    label = { Text("Match / header") },
                    placeholder = { Text("Missed Calls") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }

            OutlinedTextField(
                value = entryText,
                onValueChange = { entryText = it },
                label = { Text(if (actionKind == TaskerIntegration.PluginActionKind.APPEND) "Line to append" else "Text") },
                placeholder = {
                    Text(
                        when (actionKind) {
                            TaskerIntegration.PluginActionKind.ADD_EVENT -> "Event note from Tasker"
                            TaskerIntegration.PluginActionKind.ADD_TODO -> "Buy milk"
                            TaskerIntegration.PluginActionKind.APPEND -> "%antextbig"
                            else -> "Quick note from Tasker"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Text(
                text = if (actionKind == TaskerIntegration.PluginActionKind.APPEND) {
                    "First call creates an entry starting with the match text. Later calls append the line under it."
                } else {
                    "Tip: Tasker variables can be placed in the text field."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSave(actionKind, matchHeader, entryText) },
                    enabled = entryText.trim().isNotEmpty() &&
                        (actionKind != TaskerIntegration.PluginActionKind.APPEND || matchHeader.trim().isNotEmpty()),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun PluginChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
