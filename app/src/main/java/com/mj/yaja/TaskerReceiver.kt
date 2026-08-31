package com.mj.yaja

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TaskerReceiver : BroadcastReceiver() {
    companion object {
        private const val ACTION_ADD_ENTRY = TaskerIntegration.ACTION_ADD_ENTRY
        private const val TASKER_PACKAGE = TaskerIntegration.TASKER_PACKAGE
        private const val TASKER_SAVE_TIMEOUT_MS = 15_000L
    }

    // Coroutine scope for background operations tied to the application
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun finishPendingResultSafely(pendingResult: BroadcastReceiver.PendingResult?) {
        try {
            pendingResult?.finish()
        } catch (t: Throwable) {
            Log.w("TaskerReceiver", "PendingResult.finish() failed safely", t)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ADD_ENTRY) {
            val appContext = context.applicationContext
            val settingsRepository = SettingsRepository.getInstance(appContext)
            if (!settingsRepository.allowTaskerAccess.value) {
                Log.w("TaskerReceiver", "Blocked add-entry intent because Tasker access is disabled")
                return
            }
            val senderPackage = resolveSenderPackage(context)
            if (senderPackage != TASKER_PACKAGE) {
                Log.w("TaskerReceiver", "Blocked add-entry intent from non-Tasker sender: $senderPackage")
                return
            }
            val entryText = intent.getStringExtra("entry_text")
            
            if (!entryText.isNullOrBlank()) {
                val pendingResult = goAsync()
                
                scope.launch {
                    try {
                        Log.d("TaskerReceiver", "Received entry from Tasker: length=${entryText.length}")
                        val fileManager = MarkdownFileManager.getInstance(appContext, settingsRepository)
                        
                        val formatter = DateTimeFormatter.ofPattern("HH:mm")
                        val timeString = LocalTime.now().format(formatter)
                        val finalEntry = "<!--time:$timeString-->\n${entryText.trim()}"
                        val entryDate = LocalDate.now()
                        
                        val result = withTimeout(TASKER_SAVE_TIMEOUT_MS) {
                            fileManager.tryAddEntryForDate(entryDate, finalEntry)
                        }
                        if (!result.success) {
                            Log.e("TaskerReceiver", "Tasker entry save failed")
                            return@launch
                        }
                        appContext.sendBroadcast(
                            Intent(TaskerIntegration.ACTION_INTERNAL_ENTRY_ADDED).apply {
                                setPackage(appContext.packageName)
                                putExtra(TaskerIntegration.EXTRA_DATE, entryDate.toString())
                            }
                        )
                        Log.d("TaskerReceiver", "Entry added successfully")
                    } catch (e: Exception) {
                        Log.e("TaskerReceiver", "Error adding entry", e)
                    } finally {
                        finishPendingResultSafely(pendingResult)
                    }
                }
            } else {
                Log.w("TaskerReceiver", "Received intent but entry_text was null or blank")
            }
        }
    }

    private fun resolveSenderPackage(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.w("TaskerReceiver", "Sender verification is unavailable before Android 14; blocking legacy Tasker broadcast")
            return null
        }
        val directSender = getSentFromPackage()
        if (!directSender.isNullOrBlank()) return directSender
        val senderUid = getSentFromUid()
        if (senderUid <= 0) return null
        return context.packageManager.getPackagesForUid(senderUid)?.firstOrNull { it == TASKER_PACKAGE }
    }
}
