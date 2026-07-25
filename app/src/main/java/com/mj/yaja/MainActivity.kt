package com.mj.yaja

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.mj.yaja.data.KeywordMatchCache
import com.mj.yaja.data.KeywordRepository
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.SettingsRepository
import com.mj.yaja.ui.app.JournalApp
import com.mj.yaja.ui.viewmodel.JournalViewModel
import com.mj.yaja.ui.widget.WidgetAppearanceHelper
import com.mj.yaja.ui.widget.QuickCaptureWidgetProvider
import com.mj.yaja.ui.widget.TodoListWidgetProvider
import java.time.LocalDate
import java.io.File

class MainActivity : AppCompatActivity() {

    private var lastActivityTime: Long = System.currentTimeMillis()
    private var wasInBackground = false
    private var isWidgetStatusReceiverRegistered = false
    private var isTaskerRefreshReceiverRegistered = false

    private val settingsRepository by lazy { SettingsRepository.getInstance(applicationContext) }
    private val fileManager by lazy { MarkdownFileManager.getInstance(applicationContext, settingsRepository) }
    private val keywordRepository by lazy { KeywordRepository.getInstance(applicationContext) }
    private val keywordMatchCache by lazy { KeywordMatchCache.getInstance(applicationContext) }

    private val viewModel: JournalViewModel by viewModels {
        JournalViewModel.Factory(fileManager, settingsRepository, keywordRepository, keywordMatchCache)
    }

    private val widgetStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == QuickCaptureWidgetProvider.ACTION_WIDGET_STATUS_CHANGED) {
                viewModel.refreshWidgetStatus()
            }
        }
    }

    private val taskerRefreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TaskerIntegration.ACTION_INTERNAL_ENTRY_ADDED) {
                val rawDate = intent.getStringExtra(TaskerIntegration.EXTRA_DATE) ?: return
                runCatching { java.time.LocalDate.parse(rawDate) }
                    .onSuccess { viewModel.onExternalEntryAdded(it) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)

            setContent {
                val crashLog = remember { checkCrashLog() }
                JournalApp(
                    viewModel = viewModel,
                    initialCrashLog = crashLog
                )
            }
            handleExternalOpenIntent(intent)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Critical crash in onCreate", e)
            super.onCreate(savedInstanceState)
            throw e
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalOpenIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        WidgetAppearanceHelper.refreshWidgetsIfAppearanceChanged(
            context = applicationContext,
            immediate = false
        )
        if (wasInBackground) {
            viewModel.checkAutoLockTimeout(lastActivityTime)
            viewModel.onAppResume()
            wasInBackground = false
        }
        if (!isWidgetStatusReceiverRegistered) {
            val filter = IntentFilter(QuickCaptureWidgetProvider.ACTION_WIDGET_STATUS_CHANGED)
            ContextCompat.registerReceiver(
                this,
                widgetStatusReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isWidgetStatusReceiverRegistered = true
        }
        if (!isTaskerRefreshReceiverRegistered) {
            val filter = IntentFilter(TaskerIntegration.ACTION_INTERNAL_ENTRY_ADDED)
            ContextCompat.registerReceiver(
                this,
                taskerRefreshReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isTaskerRefreshReceiverRegistered = true
        }
    }

    override fun onStop() {
        super.onStop()
        if (isWidgetStatusReceiverRegistered) {
            try {
                unregisterReceiver(widgetStatusReceiver)
            } catch (_: IllegalArgumentException) {
            } finally {
                isWidgetStatusReceiverRegistered = false
            }
        }
        if (isTaskerRefreshReceiverRegistered) {
            try {
                unregisterReceiver(taskerRefreshReceiver)
            } catch (_: IllegalArgumentException) {
            } finally {
                isTaskerRefreshReceiverRegistered = false
            }
        }
        wasInBackground = true
        viewModel.onAppBackgrounded()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (!wasInBackground) {
            lastActivityTime = System.currentTimeMillis()
        }
    }

    private fun checkCrashLog(): String? {
        return try {
            val file = File(cacheDir, "crash_log.txt")
            if (file.exists()) file.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    private fun handleExternalOpenIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == ACTION_OPEN_TODAY) {
            // Consume the action so configuration changes don't re-trigger navigation.
            intent.action = Intent.ACTION_MAIN
            viewModel.openExternalDateOrEntry(LocalDate.now())
            return
        }
        val rawDate = intent.getStringExtra(TodoListWidgetProvider.EXTRA_DATE) ?: return
        val date = runCatching { LocalDate.parse(rawDate) }.getOrNull() ?: return
        val entryIndex = intent.getIntExtra(TodoListWidgetProvider.EXTRA_ENTRY_INDEX, -1)
        viewModel.openExternalDateOrEntry(date, entryIndex.takeIf { it >= 0 })
        intent.removeExtra(TodoListWidgetProvider.EXTRA_DATE)
        intent.removeExtra(TodoListWidgetProvider.EXTRA_ENTRY_INDEX)
    }

    fun getAppActivityContext(): Context = applicationContext

    companion object {
        /** Launched by the "Today" app shortcut (res/xml/shortcuts.xml). */
        const val ACTION_OPEN_TODAY = "com.mj.yaja.action.OPEN_TODAY"
    }
}
