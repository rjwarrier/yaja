package com.mj.yaja.ui.screens

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.mj.yaja.R
import com.mj.yaja.data.EntryKind
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.SettingsRepository
import com.mj.yaja.data.applyEntryKindMetadata
import com.mj.yaja.ui.theme.JournalTheme
import com.mj.yaja.ui.design.ProvideAnimationPreference
import com.mj.yaja.data.AnimationPreference
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class QuickTodoActivity : ComponentActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                enableEdgeToEdge()

                val initialKind =
                        if (intent?.action == ACTION_QUICK_ADD_EVENT) QuickAddKind.EVENT
                        else QuickAddKind.TODO

                setContent {
                        val settingsRepository = remember { SettingsRepository.getInstance(applicationContext) }
                        val animationPreference by settingsRepository.animationPreference.collectAsState(initial = AnimationPreference.FULL)
                        ProvideAnimationPreference(animationPreference) {
                                JournalTheme {
                                        QuickTodoDialog(
                                                onDismissRequest = { finish() },
                                                onSave = { text, date, kind -> saveQuickEntry(text, date, kind) },
                                                allowDateSelection = true,
                                                title = getString(R.string.quick_todo_dialog_title),
                                                initialKind = initialKind
                                        )
                                }
                        }
                }
        }

        private fun saveQuickEntry(text: String, date: LocalDate, kind: QuickAddKind) {
                val payloadText = normalizeQuickEntryText(text)
                if (payloadText == null) {
                        finish()
                        return
                }
                lifecycleScope.launch {
                        try {
                                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                                val timeString = LocalTime.now().format(timeFormatter)
                                val baseEntry = when (kind) {
                                        QuickAddKind.TODO -> "<!--time:$timeString-->\n[ ] $payloadText"
                                        QuickAddKind.EVENT -> "<!--time:$timeString-->\n$payloadText"
                                }
                                val finalEntry =
                                        if (kind == QuickAddKind.EVENT) {
                                                applyEntryKindMetadata(baseEntry, EntryKind.EVENT)
                                        } else {
                                                baseEntry
                                        }
                                withTimeout(QUICK_TODO_SAVE_TIMEOUT_MS) {
                                        withContext(Dispatchers.IO) {
                                                val settingsRepository =
                                                        SettingsRepository.getInstance(applicationContext)
                                                val fileManager =
                                                        MarkdownFileManager.getInstance(
                                                                applicationContext,
                                                                settingsRepository
                                                        )
                                                val result = fileManager.tryAddEntryForDate(date, finalEntry)
                                                if (!result.success) error("Quick todo save failed")
                                        }
                                }
                        } catch (t: Throwable) {
                                Log.e(TAG, "Quick todo save failed or timed out", t)
                                Toast.makeText(
                                        this@QuickTodoActivity,
                                        getString(R.string.quick_todo_save_delayed),
                                        Toast.LENGTH_SHORT
                                ).show()
                        } finally {
                                finish()
                        }
                }
        }

        private fun normalizeQuickEntryText(text: String): String? {
                val cleaned =
                        text.trim()
                                .removePrefix("[ ]")
                                .removePrefix("[x]")
                                .removePrefix("[X]")
                                .trim()
                return cleaned.takeIf { it.isNotBlank() }
        }

        companion object {
                private const val TAG = "QuickTodoActivity"
                private const val QUICK_TODO_SAVE_TIMEOUT_MS = 15_000L

                /** Launched by the "Add Event" app shortcut (res/xml/shortcuts.xml). */
                const val ACTION_QUICK_ADD_EVENT = "com.mj.yaja.action.QUICK_ADD_EVENT"
        }
}
