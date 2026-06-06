package com.mj.yaja.ui.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mj.yaja.R
import java.util.concurrent.TimeUnit

object WidgetAppearanceHelper {
    private const val PREFS_NAME = "journal_settings"
    private const val KEY_LAST_WIDGET_APPEARANCE_SIGNATURE = "last_widget_appearance_signature"
    private const val APPEARANCE_REFRESH_WORK_NAME = "widget_appearance_refresh"
    private const val APPEARANCE_REFRESH_NOW_WORK_NAME = "widget_appearance_refresh_once"

    fun refreshWidgetsIfAppearanceChanged(context: Context): Boolean {
        return refreshWidgetsIfAppearanceChanged(context, immediate = true)
    }

    fun refreshWidgetsIfAppearanceChanged(context: Context, immediate: Boolean): Boolean {
        val signature = currentAppearanceSignature(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previous = prefs.getString(KEY_LAST_WIDGET_APPEARANCE_SIGNATURE, null)
        if (signature == previous) return false

        prefs.edit().putString(KEY_LAST_WIDGET_APPEARANCE_SIGNATURE, signature).apply()
        if (immediate) {
            WidgetRefreshCoordinator.requestAppearanceUpdate(context)
        } else {
            scheduleAppearanceRefresh(context)
        }
        return true
    }

    fun recordCurrentAppearance(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_WIDGET_APPEARANCE_SIGNATURE, currentAppearanceSignature(context))
            .apply()
    }

    fun ensureAppearanceRefreshWork(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetAppearanceRefreshWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            APPEARANCE_REFRESH_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleAppearanceRefresh(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetAppearanceRefreshWorker>()
            .setInitialDelay(2, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            APPEARANCE_REFRESH_NOW_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun currentAppearanceSignature(context: Context): String {
        val darkMode = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val (bgColor, fgColor) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkMode) {
                context.getColor(android.R.color.system_accent1_800) to
                    context.getColor(android.R.color.system_accent1_100)
            } else {
                context.getColor(android.R.color.system_accent1_100) to
                    context.getColor(android.R.color.system_accent1_800)
            }
        } else {
            ContextCompat.getColor(context, R.color.ic_launcher_background) to
                ContextCompat.getColor(context, R.color.ic_launcher_foreground)
        }
        return "${if (darkMode) "dark" else "light"}:$bgColor:$fgColor"
    }
}
