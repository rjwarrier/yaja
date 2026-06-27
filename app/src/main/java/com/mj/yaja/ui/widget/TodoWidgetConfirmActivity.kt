package com.mj.yaja.ui.widget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mj.yaja.MainActivity
import com.mj.yaja.R
import com.mj.yaja.ui.theme.JournalTheme

class TodoWidgetConfirmActivity : ComponentActivity() {

    private fun closeToHomeScreen() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
        finishAndRemoveTask()
    }

    private fun openYaja(sourceIntent: Intent? = null) {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                sourceIntent?.getStringExtra(TodoListWidgetProvider.EXTRA_DATE)?.let {
                    putExtra(TodoListWidgetProvider.EXTRA_DATE, it)
                }
                val entryIndex = sourceIntent?.getIntExtra(TodoListWidgetProvider.EXTRA_ENTRY_INDEX, -1) ?: -1
                if (entryIndex >= 0) {
                    putExtra(TodoListWidgetProvider.EXTRA_ENTRY_INDEX, entryIndex)
                }
            }
        )
        finish()
        finishAndRemoveTask()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = TodoListWidgetProvider.readTodoTarget(applicationContext, intent)
        if (target == null) {
            WidgetRefreshCoordinator.requestTodoListUpdate(applicationContext)
            closeToHomeScreen()
            return
        }
        if (target.isEvent) {
            openYaja(intent)
            return
        }

        setContent {
            JournalTheme {
                val nextStateLabel = if (target.isChecked) {
                    stringResource(R.string.todo_confirm_mark_not_done)
                } else {
                    stringResource(R.string.todo_confirm_mark_done)
                }
                AlertDialog(
                    onDismissRequest = { closeToHomeScreen() },
                    icon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.todo_confirm_title, nextStateLabel),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text =
                                    if (target.isChecked) {
                                        stringResource(R.string.todo_confirm_reopen)
                                    } else {
                                        stringResource(R.string.todo_confirm_complete)
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                onClick = { openYaja(intent) }
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (target.isChecked) "[x]" else "[ ]",
                                        modifier = Modifier.width(44.dp),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = target.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                            }

                            Text(
                                text = stringResource(R.string.todo_confirm_accidental_tap),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                TodoListWidgetProvider.toggleTodoFromWidgetAsync(applicationContext, intent)
                                closeToHomeScreen()
                            }
                        ) {
                            Text(stringResource(R.string.todo_confirm_yes_mark, nextStateLabel))
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { openYaja(intent) }) {
                                Text(stringResource(R.string.todo_confirm_open_yaja))
                            }
                            TextButton(onClick = { closeToHomeScreen() }) {
                                Text(stringResource(R.string.todo_confirm_keep_as_is))
                            }
                        }
                    }
                )
            }
        }
    }
}
