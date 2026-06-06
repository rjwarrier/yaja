package com.mj.yaja.ui.screens

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
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

                setContent {
                        val settingsRepository = remember { SettingsRepository.getInstance(applicationContext) }
                        val animationPreference by settingsRepository.animationPreference.collectAsState(initial = AnimationPreference.FULL)
                        ProvideAnimationPreference(animationPreference) {
                                JournalTheme {
                                        QuickTodoDialog(
                                                onDismissRequest = { finish() },
                                                onSave = { text, date, kind -> saveQuickEntry(text, date, kind) },
                                                allowDateSelection = true,
                                                title = "Quick Add"
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
                                        "Todo save delayed. Please reopen Yaja to verify.",
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

        private companion object {
                private const val TAG = "QuickTodoActivity"
                private const val QUICK_TODO_SAVE_TIMEOUT_MS = 15_000L
        }
}
