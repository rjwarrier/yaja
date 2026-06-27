package com.mj.yaja

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied
import com.mj.yaja.ui.theme.JournalTheme

@TaskerInputRoot
class YajaTaskerEventFilter @JvmOverloads constructor(
    @field:TaskerInputField("saved") var saved: Boolean = true,
    @field:TaskerInputField("edited") var edited: Boolean = true,
    @field:TaskerInputField("deleted") var deleted: Boolean = true
)

@TaskerInputRoot
class YajaTaskerEventUpdate @JvmOverloads constructor(
    @field:TaskerInputField("event_type") var eventType: String? = null,
    @field:TaskerInputField("date") var date: String? = null,
    @field:TaskerInputField("entry_text") var entryText: String? = null,
    @field:TaskerInputField("day_label") var dayLabel: String? = null,
    @field:TaskerInputField("recorded_time") var recordedTime: String? = null
)

@TaskerOutputObject
class YajaTaskerEventOutput @JvmOverloads constructor(
    @get:TaskerOutputVariable("yaja_event_type")
    val eventType: String? = null,
    @get:TaskerOutputVariable("yaja_date")
    val date: String? = null,
    @get:TaskerOutputVariable("yaja_entry_text")
    val entryText: String? = null,
    @get:TaskerOutputVariable("yaja_day_label")
    val dayLabel: String? = null,
    @get:TaskerOutputVariable("yaja_recorded_time")
    val recordedTime: String? = null
)

class YajaTaskerEventRunner :
    TaskerPluginRunnerConditionEvent<
        YajaTaskerEventFilter,
        YajaTaskerEventOutput,
        YajaTaskerEventUpdate
    >() {
    override fun getSatisfiedCondition(
        context: Context,
        input: TaskerInput<YajaTaskerEventFilter>,
        update: YajaTaskerEventUpdate?
    ): TaskerPluginResultCondition<YajaTaskerEventOutput> {
        val eventType = update?.eventType ?: return TaskerPluginResultConditionUnsatisfied()
        val filter = input.regular
        val matches = when (eventType) {
            TaskerIntegration.EVENT_TYPE_SAVED -> filter.saved
            TaskerIntegration.EVENT_TYPE_EDITED -> filter.edited
            TaskerIntegration.EVENT_TYPE_DELETED -> filter.deleted
            else -> false
        }
        return if (matches) {
            TaskerPluginResultConditionSatisfied(
                context,
                YajaTaskerEventOutput(
                    eventType = when (eventType) {
                        TaskerIntegration.EVENT_TYPE_SAVED -> "New Entry"
                        TaskerIntegration.EVENT_TYPE_EDITED -> "Entry Modified"
                        TaskerIntegration.EVENT_TYPE_DELETED -> "Entry Deleted"
                        else -> eventType
                    },
                    date = update.date ?: "",
                    entryText = update.entryText ?: "",
                    dayLabel = update.dayLabel ?: "",
                    recordedTime = update.recordedTime ?: ""
                )
            )
        } else TaskerPluginResultConditionUnsatisfied()
    }
}

class YajaTaskerEventConfigHelper(config: TaskerPluginConfig<YajaTaskerEventFilter>) :
    TaskerPluginConfigHelper<
        YajaTaskerEventFilter,
        YajaTaskerEventOutput,
        YajaTaskerEventRunner
    >(config) {

    override val runnerClass: Class<YajaTaskerEventRunner>
        get() = YajaTaskerEventRunner::class.java

    override val inputClass: Class<YajaTaskerEventFilter>
        get() = YajaTaskerEventFilter::class.java
    override val outputClass: Class<YajaTaskerEventOutput>
        get() = YajaTaskerEventOutput::class.java

    override fun addToStringBlurb(
        input: TaskerInput<YajaTaskerEventFilter>,
        blurbBuilder: StringBuilder
    ) {
        val parts = buildList {
            if (input.regular.saved) add("saved")
            if (input.regular.edited) add("edited")
            if (input.regular.deleted) add("deleted")
        }
        blurbBuilder.append(if (parts.isEmpty()) "Yaja: saved" else "Yaja: ${parts.joinToString(", ")}")
    }
}

class TaskerPluginEventConfigActivity : ComponentActivity(), TaskerPluginConfig<YajaTaskerEventFilter> {
    override val context: Context
        get() = applicationContext

    private val helper by lazy { YajaTaskerEventConfigHelper(this) }
    private var savedEnabled by mutableStateOf(true)
    private var editedEnabled by mutableStateOf(true)
    private var deletedEnabled by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper.onCreate()
        setContent {
            JournalTheme {
                TaskerPluginEventConfigScreen(
                    savedEnabled = savedEnabled,
                    editedEnabled = editedEnabled,
                    deletedEnabled = deletedEnabled,
                    onSavedChange = { savedEnabled = it },
                    onEditedChange = { editedEnabled = it },
                    onDeletedChange = { deletedEnabled = it },
                    onCancel = { finish() },
                    onSave = { helper.finishForTasker() }
                )
            }
        }
    }

    override fun assignFromInput(input: TaskerInput<YajaTaskerEventFilter>) {
        savedEnabled = input.regular.saved
        editedEnabled = input.regular.edited
        deletedEnabled = input.regular.deleted
    }

    override val inputForTasker: TaskerInput<YajaTaskerEventFilter>
        get() {
            val safeSaved = savedEnabled || (!editedEnabled && !deletedEnabled)
            return TaskerInput(YajaTaskerEventFilter(safeSaved, editedEnabled, deletedEnabled))
        }
}

@Composable
private fun TaskerPluginEventConfigScreen(
    savedEnabled: Boolean,
    editedEnabled: Boolean,
    deletedEnabled: Boolean,
    onSavedChange: (Boolean) -> Unit,
    onEditedChange: (Boolean) -> Unit,
    onDeletedChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val noneSelected = !savedEnabled && !editedEnabled && !deletedEnabled
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.tasker_event_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.tasker_event_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            EventOptionRow(stringResource(R.string.tasker_event_saved), savedEnabled, onSavedChange)
            EventOptionRow(stringResource(R.string.tasker_event_edited), editedEnabled, onEditedChange)
            EventOptionRow(stringResource(R.string.tasker_event_deleted), deletedEnabled, onDeletedChange)
            if (noneSelected) {
                Text(
                    stringResource(R.string.tasker_event_default_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}

@Composable
private fun EventOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

fun Context.triggerYajaTaskerEvent(update: YajaTaskerEventUpdate) =
    TaskerPluginEventConfigActivity::class.java.requestQuery(this, update)
