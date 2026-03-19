package com.mj.yaja

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TaskerReceiver : BroadcastReceiver() {
    
    // Coroutine scope for background operations tied to the application
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.mj.yaja.ACTION_ADD_ENTRY") {
            val entryText = intent.getStringExtra("entry_text")
            
            if (!entryText.isNullOrBlank()) {
                val pendingResult = goAsync()
                
                scope.launch {
                    try {
                        Log.d("TaskerReceiver", "Received entry: $entryText")
                        val settingsRepository = SettingsRepository.getInstance(context)
                        val fileManager = MarkdownFileManager.getInstance(context, settingsRepository)
                        
                        val formatter = DateTimeFormatter.ofPattern("HH:mm")
                        val timeString = LocalTime.now().format(formatter)
                        val finalEntry = "<!--time:$timeString-->\n${entryText.trim()}"
                        
                        fileManager.addEntryForDate(LocalDate.now(), finalEntry)
                        Log.d("TaskerReceiver", "Entry added successfully")
                    } catch (e: Exception) {
                        Log.e("TaskerReceiver", "Error adding entry", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            } else {
                Log.w("TaskerReceiver", "Received intent but entry_text was null or blank")
            }
        }
    }
}
