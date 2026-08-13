package com.mj.yaja.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.widget.RemoteViewsCompat
import com.mj.yaja.MainActivity
import com.mj.yaja.R
import com.mj.yaja.data.AppFontFamily
import com.mj.yaja.data.EventIndexRepository
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.SettingsRepository
import com.mj.yaja.data.TodoIndexRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class TodoWidgetTarget(
    val title: String,
    val isChecked: Boolean,
    val isEvent: Boolean
)

private data class TodoWidgetCollectionRow(
    val date: LocalDate,
    val entryIndex: Int,
    val lineIndexInEntry: Int,
    val title: String,
    val isChecked: Boolean,
    val dayLabel: String,
    val lineHash: String,
    val isEvent: Boolean = false
)

class TodoListWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "YajaTodoPipeline"
        private const val PREFS_NAME = "todo_list_widget_prefs"
        private const val KEY_CORNER_RADIUS_PREFIX = "corner_radius_"
        private const val KEY_SHOW_COMPLETED_PREFIX = "show_completed_"
        private const val KEY_INCLUDE_EVENTS_PREFIX = "include_events_"
        private const val KEY_SHOW_HEADER_PREFIX = "show_header_"
        private const val KEY_AUTO_HIDE_EMPTY_PREFIX = "auto_hide_empty_"
        private const val KEY_PADDING_PREFIX = "padding_"
        private const val KEY_CONFIG_VERSION_PREFIX = "config_version_"
        private const val KEY_WIDGET_MUTATION_TOKEN = "widget_mutation_token"

        const val ACTION_WIDGET_STATUS_CHANGED = "com.mj.yaja.ACTION_TODO_LIST_WIDGET_STATUS_CHANGED"
        const val ACTION_TOGGLE_TODO = "com.mj.yaja.ACTION_TOGGLE_TODO_FROM_WIDGET"
        const val ACTION_CONFIRM_TODO = "com.mj.yaja.ACTION_CONFIRM_TODO_FROM_WIDGET"
        const val ACTION_REFRESH_TODO_LIST_WIDGET =
            "com.mj.yaja.ACTION_REFRESH_TODO_LIST_WIDGET"

        const val EXTRA_DATE = "extra_date"
        const val EXTRA_ENTRY_INDEX = "extra_entry_index"
        const val EXTRA_LINE_INDEX = "extra_line_index"
        const val EXTRA_LINE_HASH = "extra_line_hash"
        const val EXTRA_DISPLAY_TEXT = "extra_display_text"
        const val EXTRA_IS_CHECKED = "extra_is_checked"
        const val EXTRA_IS_EVENT = "extra_is_event"
        const val EXTRA_SHOW_COMPLETED = "extra_show_completed"
        const val EXTRA_CONFIG_VERSION = "extra_config_version"
        const val EXTRA_PADDING = "extra_padding"
        const val EXTRA_WIDGET_MUTATION_TOKEN = "extra_widget_mutation_token"
        private const val WIDGET_TOGGLE_TIMEOUT_MS = 15_000L
        private val widgetMutationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun getCornerRadius(context: Context, appWidgetId: Int): Int =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_CORNER_RADIUS_PREFIX + appWidgetId, 24)

        fun setCornerRadius(context: Context, appWidgetId: Int, radiusDp: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val nextVersion = prefs.getInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, 0) + 1
            prefs
                .edit()
                .putInt(KEY_CORNER_RADIUS_PREFIX + appWidgetId, radiusDp)
                .putInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, nextVersion)
                .commit()
        }

        fun getShowCompleted(context: Context, appWidgetId: Int): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_COMPLETED_PREFIX + appWidgetId, false)

        fun setShowCompleted(context: Context, appWidgetId: Int, show: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val nextVersion = prefs.getInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, 0) + 1
            prefs
                .edit()
                .putBoolean(KEY_SHOW_COMPLETED_PREFIX + appWidgetId, show)
                .putBoolean(KEY_AUTO_HIDE_EMPTY_PREFIX + appWidgetId, if (show) false else getAutoHideWhenEmpty(context, appWidgetId))
                .putInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, nextVersion)
                .commit()
        }

        fun getIncludeEvents(context: Context, appWidgetId: Int): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_INCLUDE_EVENTS_PREFIX + appWidgetId, false)

        fun setIncludeEvents(context: Context, appWidgetId: Int, include: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val nextVersion = prefs.getInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, 0) + 1
            prefs
                .edit()
                .putBoolean(KEY_INCLUDE_EVENTS_PREFIX + appWidgetId, include)
                .putInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, nextVersion)
                .commit()
        }

        fun getShowHeader(context: Context, appWidgetId: Int): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_HEADER_PREFIX + appWidgetId, true)

        fun setShowHeader(context: Context, appWidgetId: Int, show: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val nextVersion = prefs.getInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, 0) + 1
            prefs
                .edit()
                .putBoolean(KEY_SHOW_HEADER_PREFIX + appWidgetId, show)
                .putInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, nextVersion)
                .commit()
        }

        fun getAutoHideWhenEmpty(context: Context, appWidgetId: Int): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_HIDE_EMPTY_PREFIX + appWidgetId, false)

        fun setAutoHideWhenEmpty(context: Context, appWidgetId: Int, hide: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val nextVersion = prefs.getInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, 0) + 1
            prefs
                .edit()
                .putBoolean(KEY_AUTO_HIDE_EMPTY_PREFIX + appWidgetId, hide && !getShowCompleted(context, appWidgetId))
                .putInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, nextVersion)
                .commit()
        }

        fun getPadding(context: Context, appWidgetId: Int): Int =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_PADDING_PREFIX + appWidgetId, 14)

        fun setPadding(context: Context, appWidgetId: Int, paddingDp: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val nextVersion = prefs.getInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, 0) + 1
            prefs
                .edit()
                .putInt(KEY_PADDING_PREFIX + appWidgetId, paddingDp)
                .putInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, nextVersion)
                .commit()
        }

        fun getConfigVersion(context: Context, appWidgetId: Int): Int =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, 0)

        fun saveOptions(
            context: Context,
            appWidgetId: Int,
            radiusDp: Int,
            paddingDp: Int,
            showCompleted: Boolean,
            includeEvents: Boolean,
            showHeader: Boolean,
            autoHideWhenEmpty: Boolean
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val nextVersion = prefs.getInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, 0) + 1
            val effectiveAutoHide = autoHideWhenEmpty && !showCompleted
            prefs.edit()
                .putInt(KEY_CORNER_RADIUS_PREFIX + appWidgetId, radiusDp)
                .putInt(KEY_PADDING_PREFIX + appWidgetId, paddingDp)
                .putBoolean(KEY_SHOW_COMPLETED_PREFIX + appWidgetId, showCompleted)
                .putBoolean(KEY_INCLUDE_EVENTS_PREFIX + appWidgetId, includeEvents)
                .putBoolean(KEY_SHOW_HEADER_PREFIX + appWidgetId, showHeader)
                .putBoolean(KEY_AUTO_HIDE_EMPTY_PREFIX + appWidgetId, effectiveAutoHide)
                .putInt(KEY_CONFIG_VERSION_PREFIX + appWidgetId, nextVersion)
                .commit()
        }

        fun deletePreferences(context: Context, appWidgetId: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_CORNER_RADIUS_PREFIX + appWidgetId)
                .remove(KEY_SHOW_COMPLETED_PREFIX + appWidgetId)
                .remove(KEY_INCLUDE_EVENTS_PREFIX + appWidgetId)
                .remove(KEY_SHOW_HEADER_PREFIX + appWidgetId)
                .remove(KEY_AUTO_HIDE_EMPTY_PREFIX + appWidgetId)
                .remove(KEY_PADDING_PREFIX + appWidgetId)
                .remove(KEY_CONFIG_VERSION_PREFIX + appWidgetId)
                .commit()
        }

        private fun getWidgetMutationToken(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existing = prefs.getString(KEY_WIDGET_MUTATION_TOKEN, null)
            if (!existing.isNullOrBlank()) return existing
            val created = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_WIDGET_MUTATION_TOKEN, created).commit()
            return created
        }

        private fun isTrustedWidgetMutationIntent(context: Context, intent: Intent): Boolean {
            val expected = getWidgetMutationToken(context)
            val actual = intent.getStringExtra(EXTRA_WIDGET_MUTATION_TOKEN)
            return actual == expected
        }

        fun refreshAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TodoListWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                Log.d(TAG, "Widget refresh requested: ids=${appWidgetIds.joinToString()}")
                context.sendBroadcast(
                    Intent(context, TodoListWidgetProvider::class.java).apply {
                        action = ACTION_REFRESH_TODO_LIST_WIDGET
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                )
            }
        }

        fun refreshVisibilityAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TodoListWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            val settingsRepository = SettingsRepository.getInstance(context.applicationContext)
            appWidgetIds.forEach { appWidgetId ->
                val autoHideWhenEmpty =
                    getAutoHideWhenEmpty(context, appWidgetId) && !getShowCompleted(context, appWidgetId)
                val showHeader = getShowHeader(context, appWidgetId)
                val includeEvents = getIncludeEvents(context, appWidgetId)
                val appFontFamily = settingsRepository.appFontFamily.value
                val views = RemoteViews(context.packageName, getWidgetLayoutRes(appFontFamily, showHeader))
                views.setInt(R.id.widget_root_layout, "setBackgroundColor", Color.TRANSPARENT)
                val hasVisibleRows = hasVisibleRows(context, showCompleted = false, includeEvents = includeEvents)
                if (autoHideWhenEmpty && !hasVisibleRows) {
                    views.setImageViewBitmap(R.id.widget_background, createTransparentBitmap())
                    views.setViewVisibility(R.id.widget_background, View.INVISIBLE)
                    views.setViewVisibility(R.id.widget_content_root, View.GONE)
                    Log.d(TAG, "Widget visibility hidden fast: id=$appWidgetId")
                } else {
                    views.setViewVisibility(R.id.widget_background, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_content_root, View.VISIBLE)
                    Log.d(TAG, "Widget visibility shown fast: id=$appWidgetId")
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun readTodoTarget(context: Context, intent: Intent): TodoWidgetTarget? {
            if (!isTrustedWidgetMutationIntent(context, intent)) return null
            val title = intent.getStringExtra(EXTRA_DISPLAY_TEXT)?.takeIf { it.isNotBlank() }
                ?: return null
            return TodoWidgetTarget(
                title = title,
                isChecked = intent.getBooleanExtra(EXTRA_IS_CHECKED, false),
                isEvent = intent.getBooleanExtra(EXTRA_IS_EVENT, false)
            )
        }

        fun toggleTodoFromWidget(context: Context, intent: Intent): Boolean {
            if (!isTrustedWidgetMutationIntent(context, intent)) return false
            val dateString = intent.getStringExtra(EXTRA_DATE) ?: return false
            val entryIndex = intent.getIntExtra(EXTRA_ENTRY_INDEX, -1)
            val lineIndex = intent.getIntExtra(EXTRA_LINE_INDEX, -1)
            val expectedLineHash = intent.getStringExtra(EXTRA_LINE_HASH)
            val expectedDisplayText = intent.getStringExtra(EXTRA_DISPLAY_TEXT)
            if (entryIndex < 0 || lineIndex < 0) return false

            val settingsRepository = SettingsRepository.getInstance(context.applicationContext)
            val fileManager =
                MarkdownFileManager.getInstance(context.applicationContext, settingsRepository)
            val date = runCatching { LocalDate.parse(dateString) }.getOrNull() ?: return false
            return fileManager.toggleTodoLine(
                date,
                entryIndex,
                lineIndex,
                expectedLineHash,
                expectedDisplayText
            )
        }

        fun toggleTodoFromWidgetAsync(
            context: Context,
            intent: Intent,
            onComplete: (Boolean) -> Unit = {}
        ) {
            val appContext = context.applicationContext
            val dateString = intent.getStringExtra(EXTRA_DATE)
            val date = dateString?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val entryIndex = intent.getIntExtra(EXTRA_ENTRY_INDEX, -1)
            val lineIndex = intent.getIntExtra(EXTRA_LINE_INDEX, -1)
            val lineHash = intent.getStringExtra(EXTRA_LINE_HASH)
            val displayText = intent.getStringExtra(EXTRA_DISPLAY_TEXT)
            val nextCheckedState = !intent.getBooleanExtra(EXTRA_IS_CHECKED, false)
            val todoIndexRepository = TodoIndexRepository.getInstance(appContext)
            val trustedMutation = isTrustedWidgetMutationIntent(appContext, intent)
            if (trustedMutation && date != null && entryIndex >= 0 && lineIndex >= 0) {
                val changed = todoIndexRepository.setCheckedState(
                    date = date,
                    entryIndex = entryIndex,
                    lineIndexInEntry = lineIndex,
                    lineHash = lineHash,
                    displayText = displayText,
                    isChecked = nextCheckedState
                )
                if (changed) {
                    Log.d(TAG, "Widget todo toggled optimistically: date=$date checked=$nextCheckedState")
                    WidgetRefreshCoordinator.requestTodoListUpdate(appContext)
                }
            }
            widgetMutationScope.launch {
                val saved = runCatching {
                    withTimeout(WIDGET_TOGGLE_TIMEOUT_MS) {
                        toggleTodoFromWidget(appContext, intent)
                    }
                }.getOrElse { error ->
                    Log.e(TAG, "Widget todo toggle failed or timed out", error)
                    false
                }
                if (!saved && date != null) {
                    val settingsRepository = SettingsRepository.getInstance(appContext)
                    val fileManager = MarkdownFileManager.getInstance(appContext, settingsRepository)
                    val diskEntries = fileManager.getEntriesForDate(date)
                    if (diskEntries.isEmpty()) {
                        todoIndexRepository.removeDate(date)
                    } else {
                        todoIndexRepository.replaceDate(date, diskEntries, fileManager.getDayLabel(date))
                    }
                    Log.w(TAG, "Widget todo optimistic state refreshed from disk: date=$date")
                }
                WidgetRefreshCoordinator.requestTodoListUpdate(appContext)
                onComplete(saved)
            }
        }

        private fun createTransparentBitmap(): Bitmap =
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.TRANSPARENT)
            }

        private fun getWidgetLayoutRes(appFontFamily: AppFontFamily, showHeader: Boolean): Int =
            when (appFontFamily) {
                AppFontFamily.SERIF -> if (showHeader) {
                    R.layout.widget_todo_list_serif
                } else {
                    R.layout.widget_todo_list_serif_no_header
                }
                // RemoteViews can't load file-based fonts; custom falls back to sans.
                AppFontFamily.SANS_SERIF, AppFontFamily.CUSTOM -> if (showHeader) {
                    R.layout.widget_todo_list_sans
                } else {
                    R.layout.widget_todo_list_sans_no_header
                }
                AppFontFamily.MONO -> if (showHeader) {
                    R.layout.widget_todo_list
                } else {
                    R.layout.widget_todo_list_no_header
                }
            }

        fun hasVisibleRows(
            context: Context,
            showCompleted: Boolean,
            includeEvents: Boolean
        ): Boolean {
            val todoIndexRepository = TodoIndexRepository.getInstance(context.applicationContext)
            val today = LocalDate.now()
            val farFutureCutoff = today.plusDays(10)

            val hasTodos = todoIndexRepository.getEntries(showCompleted)
                .any { !it.date.isAfter(farFutureCutoff) }
            if (hasTodos) return true

            if (includeEvents) {
                val eventIndexRepository = EventIndexRepository.getInstance(context.applicationContext)
                val hasEvents = eventIndexRepository.getEntries()
                    .any { !it.date.isBefore(today) && !it.date.isAfter(farFutureCutoff) }
                if (hasEvents) return true
            }
            return false
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.sendBroadcast(Intent(ACTION_WIDGET_STATUS_CHANGED))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.sendBroadcast(Intent(ACTION_WIDGET_STATUS_CHANGED))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { deletePreferences(context, it) }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId, resetAdapter = false)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_TODO -> {
                val pendingResult = goAsync()
                toggleTodoFromWidgetAsync(context, intent) {
                    pendingResult.finish()
                }
            }

            ACTION_REFRESH_TODO_LIST_WIDGET -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val requestedIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                val appWidgetIds = requestedIds?.takeIf { it.isNotEmpty() }
                    ?: appWidgetManager.getAppWidgetIds(
                        ComponentName(context, TodoListWidgetProvider::class.java)
                    )
                if (appWidgetIds.isNotEmpty()) {
                    onUpdate(context, appWidgetManager, appWidgetIds)
                }
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        resetAdapter: Boolean = true
    ) {
            val settingsRepository = SettingsRepository.getInstance(context.applicationContext)
            val todoIndexRepository = TodoIndexRepository.getInstance(context.applicationContext)
            val eventIndexRepository = EventIndexRepository.getInstance(context.applicationContext)
            val todoVersion = todoIndexRepository.liveVersion
            val eventVersion = eventIndexRepository.liveVersion
            val appFontFamily = settingsRepository.appFontFamily.value
        val (bgColor, fgColor) = resolveWidgetColors(context)
        val cornerRadiusDp = getCornerRadius(context, appWidgetId)
        val showCompleted = getShowCompleted(context, appWidgetId)
        val includeEvents = getIncludeEvents(context, appWidgetId)
        val showHeader = getShowHeader(context, appWidgetId)
        val autoHideWhenEmpty = getAutoHideWhenEmpty(context, appWidgetId) && !showCompleted
        val contentPadding = getPadding(context, appWidgetId)
        val configVersion = getConfigVersion(context, appWidgetId)
        val views = RemoteViews(context.packageName, getWidgetLayoutRes(appFontFamily, showHeader))
        val shouldAutoHide =
            autoHideWhenEmpty && !hasVisibleRows(context, showCompleted = false, includeEvents = includeEvents)

        views.setInt(R.id.widget_root_layout, "setBackgroundColor", Color.TRANSPARENT)
        if (shouldAutoHide) {
            views.setImageViewBitmap(R.id.widget_background, createTransparentBitmap())
            views.setViewVisibility(R.id.widget_background, View.INVISIBLE)
            views.setViewVisibility(R.id.widget_content_root, View.GONE)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget auto-hidden: id=$appWidgetId todoVersion=$todoVersion eventVersion=$eventVersion")
            return
        }

        views.setViewVisibility(R.id.widget_background, View.VISIBLE)
        views.setViewVisibility(R.id.widget_content_root, View.VISIBLE)

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val isPortrait = context.resources.configuration.orientation ==
            Configuration.ORIENTATION_PORTRAIT
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
        val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
        val widthDp = if (isPortrait) {
            minWidthDp
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidthDp)
        }
        val heightDp = if (isPortrait) {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeightDp)
        } else {
            minHeightDp
        }
        val density = context.resources.displayMetrics.density
        val bitmap = createRoundedBitmap(
            widthPx = (widthDp * density).toInt().coerceAtLeast(1),
            heightPx = (heightDp * density).toInt().coerceAtLeast(1),
            radiusPx = cornerRadiusDp * density,
            backgroundColor = bgColor
        )
        views.setImageViewBitmap(R.id.widget_background, bitmap)
        views.setTextColor(R.id.widget_title, fgColor)
        views.setTextColor(R.id.widget_empty, fgColor)
        views.setTextViewText(R.id.widget_empty, context.getString(R.string.widget_todo_empty))
        views.setTextViewTextSize(R.id.widget_empty, android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
        views.setInt(R.id.widget_empty_icon, "setColorFilter", fgColor)
        views.setViewVisibility(R.id.widget_title_row, if (showHeader) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widget_title, if (showHeader) {
            if (includeEvents) {
                context.getString(R.string.widget_todo_header_todos_events)
            } else {
                context.getString(R.string.todo_list_widget_name)
            }
        } else "")
        views.setViewPadding(
            R.id.widget_title_row,
            dpToPx(context, contentPadding),
            if (showHeader) dpToPx(context, contentPadding) else 0,
            dpToPx(context, contentPadding),
            if (showHeader) dpToPx(context, 8) else 0
        )
        val titleSizeSp = 15f
        val emptySizeSp = 12f
        views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, titleSizeSp)
        views.setTextViewTextSize(R.id.widget_empty, android.util.TypedValue.COMPLEX_UNIT_SP, emptySizeSp)
        views.setViewPadding(
            R.id.widget_content_root,
            0,
            0,
            0,
            0
        )
        views.setViewPadding(
            R.id.widget_todo_list,
            0,
            dpToPx(context, contentPadding),
            0,
            dpToPx(context, contentPadding)
        )

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val launchPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val configIntent = Intent(context, TodoListWidgetConfigActivity::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val configPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId + 1000,
            configIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root_layout, launchPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_title_row, configPendingIntent)

        bindTodoCollection(
            context = context,
            views = views,
            appWidgetId = appWidgetId,
            appFontFamily = appFontFamily,
            showCompleted = showCompleted,
            includeEvents = includeEvents,
            rowPaddingHorizontalDp = contentPadding
        )

        val toggleIntent = Intent(context, TodoWidgetConfirmActivity::class.java).apply {
            action = ACTION_CONFIRM_TODO
            putExtra(EXTRA_WIDGET_MUTATION_TOKEN, getWidgetMutationToken(context))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val togglePendingIntent = PendingIntent.getActivity(
            context,
            0,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_todo_list, togglePendingIntent)
        views.setEmptyView(R.id.widget_todo_list, R.id.widget_empty_state)

        appWidgetManager.updateAppWidget(appWidgetId, views)
        Log.d(TAG, "Widget updated: id=$appWidgetId todoVersion=$todoVersion eventVersion=$eventVersion")
    }

    private fun bindTodoCollection(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        appFontFamily: AppFontFamily,
        showCompleted: Boolean,
        includeEvents: Boolean,
        rowPaddingHorizontalDp: Int
    ) {
        val items = buildTodoCollectionItems(
            context = context,
            appFontFamily = appFontFamily,
            showCompleted = showCompleted,
            includeEvents = includeEvents,
            rowPaddingHorizontalDp = rowPaddingHorizontalDp
        )
        RemoteViewsCompat.setRemoteAdapter(
            context,
            views,
            appWidgetId,
            R.id.widget_todo_list,
            items
        )
    }

    private fun buildTodoCollectionItems(
        context: Context,
        appFontFamily: AppFontFamily,
        showCompleted: Boolean,
        includeEvents: Boolean,
        rowPaddingHorizontalDp: Int
    ): RemoteViewsCompat.RemoteCollectionItems {
        val todoIndexRepository = TodoIndexRepository.getInstance(context.applicationContext)
        val today = java.time.LocalDate.now()
        val farFutureCutoff = today.plusDays(10)
        
        val todoRows =
            todoIndexRepository.getEntries(showCompleted)
                .filter { !it.date.isAfter(farFutureCutoff) }
                .map { item ->
                TodoWidgetCollectionRow(
                    date = item.date,
                    entryIndex = item.entryIndex,
                    lineIndexInEntry = item.lineIndexInEntry,
                    title = item.displayText,
                    isChecked = item.isChecked,
                    dayLabel = item.dayLabel,
                    lineHash = item.lineHash
                )
            }
        val rows =
            if (includeEvents) {
                (todoRows + loadEventRows(context))
                    .sortedWith(
                        compareBy<TodoWidgetCollectionRow> { it.date }
                            .thenBy { it.entryIndex }
                            .thenBy { it.lineIndexInEntry }
                    )
            } else {
                todoRows.sortedWith(
                    compareBy<TodoWidgetCollectionRow> { it.date }
                        .thenBy { it.entryIndex }
                        .thenBy { it.lineIndexInEntry }
                )
            }
        val builder = RemoteViewsCompat.RemoteCollectionItems.Builder()
            .setHasStableIds(true)
        rows.forEach { row ->
            builder.addItem(buildTodoRowId(row), buildTodoRowRemoteViews(context, appFontFamily, row, rowPaddingHorizontalDp))
        }
        return builder.build()
    }

    private fun buildTodoRowRemoteViews(
        context: Context,
        appFontFamily: AppFontFamily,
        item: TodoWidgetCollectionRow,
        rowPaddingHorizontalDp: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, getItemLayoutRes(appFontFamily))
        val settingsRepository = SettingsRepository.getInstance(context.applicationContext)
        val fgColor = resolveForegroundColor(context)
        val mutedColor = resolveMutedColor(context)
        val eventColor = resolveEventColor(context)
        val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM")
        val rowPaddingVerticalDp = 3
        val renderCheckboxesAsText = settingsRepository.renderCheckboxesAsText.value

        views.setTextViewText(
            R.id.widget_todo_title,
            if (item.isEvent) {
                item.title
            } else {
                val prefix =
                    if (renderCheckboxesAsText) {
                        if (item.isChecked) "[x]" else "[ ]"
                    } else {
                        if (item.isChecked) "☑" else "☐"
                    }
                "$prefix ${item.title}"
            }
        )
        val periodLabel = getRelativePeriodLabel(context, item.date)
        val metaBuilder = android.text.SpannableStringBuilder()
        val highlightColor = if (item.isEvent) eventColor else fgColor
        val bgHighlightColor = androidx.core.graphics.ColorUtils.setAlphaComponent(highlightColor, 40)

        if (item.isEvent) {
            metaBuilder.append(context.getString(R.string.widget_todo_event_label))
            if (periodLabel != null) {
                metaBuilder.append(" |  ")
                val start = metaBuilder.length
                metaBuilder.append(" $periodLabel ")
                metaBuilder.setSpan(android.text.style.BackgroundColorSpan(bgHighlightColor), start, metaBuilder.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                metaBuilder.setSpan(android.text.style.ForegroundColorSpan(highlightColor), start, metaBuilder.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                metaBuilder.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, metaBuilder.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                metaBuilder.append("  | ")
            } else {
                metaBuilder.append(" | ")
            }
            metaBuilder.append(item.date.format(dateFormatter))
            if (item.dayLabel.isNotBlank()) {
                metaBuilder.append(" | ")
                metaBuilder.append(item.dayLabel)
            }
        } else {
            if (periodLabel != null) {
                val start = metaBuilder.length
                metaBuilder.append(" $periodLabel ")
                metaBuilder.setSpan(android.text.style.BackgroundColorSpan(bgHighlightColor), start, metaBuilder.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                metaBuilder.setSpan(android.text.style.ForegroundColorSpan(highlightColor), start, metaBuilder.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                metaBuilder.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, metaBuilder.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                metaBuilder.append("  |  ")
            }
            metaBuilder.append(item.date.format(dateFormatter))
            if (item.dayLabel.isNotBlank()) {
                metaBuilder.append(" | ")
                metaBuilder.append(item.dayLabel)
            }
        }

        views.setTextViewText(R.id.widget_todo_meta, metaBuilder)
        views.setTextColor(
            R.id.widget_todo_title,
            when {
                item.isEvent -> eventColor
                item.isChecked -> mutedColor
                else -> fgColor
            }
        )
        views.setTextColor(R.id.widget_todo_meta, if (item.isEvent) eventColor else mutedColor)
        views.setInt(
            R.id.widget_todo_card,
            "setBackgroundResource",
            if (item.isEvent) {
                R.drawable.bg_todo_widget_item_event
            } else if (item.isChecked) {
                R.drawable.bg_todo_widget_item_done
            } else {
                R.drawable.bg_todo_widget_item
            }
        )
        views.setTextViewTextSize(
            R.id.widget_todo_title,
            android.util.TypedValue.COMPLEX_UNIT_SP,
            15f
        )
        views.setTextViewTextSize(
            R.id.widget_todo_meta,
            android.util.TypedValue.COMPLEX_UNIT_SP,
            12f
        )
        views.setViewPadding(
            R.id.widget_todo_item_root,
            dpToPx(context, rowPaddingHorizontalDp),
            dpToPx(context, rowPaddingVerticalDp),
            dpToPx(context, rowPaddingHorizontalDp),
            dpToPx(context, rowPaddingVerticalDp)
        )

        val fillInIntent = Intent().apply {
            putExtra(EXTRA_DATE, item.date.toString())
            putExtra(EXTRA_ENTRY_INDEX, item.entryIndex)
            putExtra(EXTRA_LINE_INDEX, item.lineIndexInEntry)
            putExtra(EXTRA_LINE_HASH, item.lineHash)
            putExtra(EXTRA_DISPLAY_TEXT, item.title)
            putExtra(EXTRA_IS_CHECKED, item.isChecked)
            putExtra(EXTRA_IS_EVENT, item.isEvent)
        }
        views.setOnClickFillInIntent(R.id.widget_todo_item_root, fillInIntent)
        return views
    }

    private fun buildTodoRowId(item: TodoWidgetCollectionRow): Long =
        "${item.date}-${item.lineHash}-${item.title}-${item.isEvent}".hashCode().toLong()

    private fun loadEventRows(context: Context): List<TodoWidgetCollectionRow> {
        val eventIndexRepository = EventIndexRepository.getInstance(context.applicationContext)
        val today = java.time.LocalDate.now()
        val farFutureCutoff = today.plusDays(10)
        
        return eventIndexRepository.getEntries()
            .filter { !it.date.isBefore(today) && !it.date.isAfter(farFutureCutoff) }
            .map { event ->
            val timePrefix =
                when {
                    !event.mentionedTime.isNullOrBlank() &&
                        !com.mj.yaja.data.eventTextStartsWithTime(
                            event.displayText,
                            event.mentionedTime
                        ) -> "${event.mentionedTime} - "
                    !event.recordedTime.isNullOrBlank() &&
                        !com.mj.yaja.data.eventTextStartsWithTime(
                            event.displayText,
                            event.recordedTime
                        ) -> "${event.recordedTime} - "
                    else -> ""
                }
            TodoWidgetCollectionRow(
                date = event.date,
                entryIndex = event.entryIndex,
                lineIndexInEntry = -1,
                title = "$timePrefix${event.displayText}",
                isChecked = false,
                dayLabel = "",
                lineHash = event.lineHash,
                isEvent = true
            )
        }
    }

    private fun resolveWidgetColors(context: Context): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Match themed launcher icon tiles: dark accent surface, light accent content.
            context.getColor(android.R.color.system_accent1_900) to
                context.getColor(android.R.color.system_accent1_100)
        } else {
            ContextCompat.getColor(context, R.color.ic_launcher_foreground) to
                ContextCompat.getColor(context, R.color.ic_launcher_background)
        }
    }

    private fun createRoundedBitmap(
        widthPx: Int,
        heightPx: Int,
        radiusPx: Float,
        backgroundColor: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
        }
        val radius = radiusPx.coerceAtMost(minOf(widthPx, heightPx) / 2f)
        canvas.drawRoundRect(
            RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()),
            radius,
            radius,
            paint
        )
        return bitmap
    }

    private fun dpToPx(context: Context, dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()

    private fun getItemLayoutRes(appFontFamily: AppFontFamily): Int =
        when (appFontFamily) {
            AppFontFamily.SERIF -> R.layout.widget_todo_list_item_serif
            AppFontFamily.SANS_SERIF, AppFontFamily.CUSTOM -> R.layout.widget_todo_list_item_sans
            AppFontFamily.MONO -> R.layout.widget_todo_list_item
        }

    private fun resolveForegroundColor(context: Context): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val darkMode = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            if (darkMode) {
                context.getColor(android.R.color.system_accent1_100)
            } else {
                context.getColor(android.R.color.system_accent1_900)
            }
        } else {
            ContextCompat.getColor(context, R.color.ic_launcher_foreground)
        }
    }

    private fun resolveMutedColor(context: Context): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val darkMode = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            if (darkMode) {
                context.getColor(android.R.color.system_neutral1_300)
            } else {
                context.getColor(android.R.color.system_neutral1_700)
            }
        } else {
            ContextCompat.getColor(context, android.R.color.darker_gray)
        }
    }

    private fun resolveEventColor(context: Context): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val darkMode = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            if (darkMode) {
                context.getColor(android.R.color.system_accent3_100)
            } else {
                context.getColor(android.R.color.system_accent3_900)
            }
        } else {
            ContextCompat.getColor(context, R.color.ic_launcher_background)
        }
    }

    private fun lerpBySize(
        value: Int,
        minValue: Int,
        maxValue: Int,
        minOut: Float,
        maxOut: Float
    ): Float {
        if (value <= minValue) return minOut
        if (value >= maxValue) return maxOut
        val progress = (value - minValue).toFloat() / (maxValue - minValue).toFloat()
        return minOut + (maxOut - minOut) * progress
    }
    
    private fun getRelativePeriodLabel(context: Context, date: LocalDate): String? {
        val today = LocalDate.now()
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
        return when {
            days == 0L -> context.getString(R.string.settings_meaning_today)
            days == 1L -> context.getString(R.string.settings_meaning_tomorrow)
            days == 2L -> context.getString(R.string.settings_meaning_day_after_tomorrow)
            days == -1L -> context.getString(R.string.settings_meaning_yesterday)
            days == -2L -> context.getString(R.string.settings_meaning_day_before_yesterday)
            days in 3L..6L -> context.resources.getQuantityString(
                R.plurals.todos_relative_in_days,
                days.toInt(),
                days.toInt()
            )
            days in -6L..-3L -> context.resources.getQuantityString(
                R.plurals.todos_relative_days_ago,
                Math.abs(days).toInt(),
                Math.abs(days).toInt()
            )
            days in 7L..13L -> context.getString(R.string.settings_meaning_next_week)
            days in -13L..-7L -> context.getString(R.string.settings_meaning_last_week)
            days in 14L..29L -> context.resources.getQuantityString(
                R.plurals.todos_relative_in_weeks,
                (days / 7).toInt(),
                (days / 7).toInt()
            )
            days in -29L..-14L -> context.resources.getQuantityString(
                R.plurals.todos_relative_weeks_ago,
                Math.abs(days / 7).toInt(),
                Math.abs(days / 7).toInt()
            )
            days in 30L..59L -> context.getString(R.string.todos_relative_in_one_month)
            days in -59L..-30L -> context.getString(R.string.todos_relative_one_month_ago)
            days >= 60L -> context.resources.getQuantityString(
                R.plurals.todos_relative_in_months,
                (days / 30).toInt(),
                (days / 30).toInt()
            )
            days <= -60L -> context.resources.getQuantityString(
                R.plurals.todos_relative_months_ago,
                Math.abs(days / 30).toInt(),
                Math.abs(days / 30).toInt()
            )
            else -> null
        }
    }
}
