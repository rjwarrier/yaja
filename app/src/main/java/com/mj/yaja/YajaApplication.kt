package com.mj.yaja

import android.app.Application
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class YajaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Global Uncaught Exception Handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()
            
            Log.e("YajaApplication", "GLOBAL CRASH in thread ${thread.name}:\n$stackTrace")
            
            try {
                // Write crash to a file in cache directory
                // This will be accessible if the user uses a file manager or if we read it next time
                val crashFile = File(cacheDir, "crash_log.txt")
                crashFile.writeText("Thread: ${thread.name}\n\n$stackTrace")
            } catch (e: Exception) {
                Log.e("YajaApplication", "Failed to write crash log to file", e)
            }
            
            // Still let the system handle the crash (shows the "has stopped" dialog)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
