package com.mj.yaja.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object WidgetRefreshCoordinator {
    private const val WIDGET_REFRESH_DEBOUNCE_MS = 1_500L
    private const val TODO_VISIBILITY_RETRY_MS = 450L
    private const val APPEARANCE_RETRY_MS = 650L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private var refreshJob: Job? = null
    private var todoRefreshJob: Job? = null
    private var appearanceRefreshJob: Job? = null
    private var refreshQuickCapture = false
    private var refreshHeatmap = false
    private var refreshTodoList = false
    private var invalidateHeatmapCache = false

    fun requestQuickCaptureUpdate(context: Context) {
        enqueue(
            context,
            quickCapture = true,
            heatmap = false,
            todoList = false,
            invalidateHeatmap = false
        )
    }

    fun requestHeatmapUpdate(context: Context, invalidateCache: Boolean = true) {
        enqueue(
            context,
            quickCapture = false,
            heatmap = true,
            todoList = false,
            invalidateHeatmap = invalidateCache
        )
    }

    fun requestTodoListUpdate(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            todoRefreshJob?.cancel()
            val job = scope.launch {
                TodoListWidgetProvider.refreshVisibilityAll(appContext)
                delay(TODO_VISIBILITY_RETRY_MS)
                TodoListWidgetProvider.refreshVisibilityAll(appContext)
                TodoListWidgetProvider.refreshAll(appContext)
            }
            todoRefreshJob = job
            job.invokeOnCompletion {
                synchronized(lock) {
                    if (todoRefreshJob === job) {
                        todoRefreshJob = null
                    }
                }
            }
        }
    }

    fun requestAppearanceUpdate(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            appearanceRefreshJob?.cancel()
            val job = scope.launch {
                refreshAppearanceProviders(appContext)
                delay(APPEARANCE_RETRY_MS)
                refreshAppearanceProviders(appContext)
            }
            appearanceRefreshJob = job
            job.invokeOnCompletion {
                synchronized(lock) {
                    if (appearanceRefreshJob === job) {
                        appearanceRefreshJob = null
                    }
                }
            }
        }
    }

    fun requestAllUpdates(context: Context, invalidateHeatmap: Boolean = true) {
        enqueue(
            context,
            quickCapture = true,
            heatmap = true,
            todoList = true,
            invalidateHeatmap = invalidateHeatmap
        )
    }

    private fun enqueue(
        context: Context,
        quickCapture: Boolean,
        heatmap: Boolean,
        todoList: Boolean,
        invalidateHeatmap: Boolean
    ) {
        val appContext = context.applicationContext
        synchronized(lock) {
            refreshQuickCapture = refreshQuickCapture || quickCapture
            refreshHeatmap = refreshHeatmap || heatmap
            refreshTodoList = refreshTodoList || todoList
            invalidateHeatmapCache = invalidateHeatmapCache || invalidateHeatmap
            refreshJob?.cancel()
            refreshJob = scope.launch {
                delay(WIDGET_REFRESH_DEBOUNCE_MS)
                val shouldRefreshQuickCapture: Boolean
                val shouldRefreshHeatmap: Boolean
                val shouldRefreshTodoList: Boolean
                val shouldInvalidateHeatmap: Boolean
                synchronized(lock) {
                    shouldRefreshQuickCapture = refreshQuickCapture
                    shouldRefreshHeatmap = refreshHeatmap
                    shouldRefreshTodoList = refreshTodoList
                    shouldInvalidateHeatmap = invalidateHeatmapCache
                    refreshQuickCapture = false
                    refreshHeatmap = false
                    refreshTodoList = false
                    invalidateHeatmapCache = false
                    refreshJob = null
                }
                if (shouldRefreshQuickCapture) {
                    updateProvider(appContext, QuickCaptureWidgetProvider::class.java)
                    updateProvider(appContext, QuickTodoWidgetProvider::class.java)
                }
                if (shouldRefreshHeatmap) {
                    if (shouldInvalidateHeatmap) {
                        HeatmapWidgetProvider.invalidateCache()
                    }
                    updateProvider(appContext, HeatmapWidgetProvider::class.java)
                }
                if (shouldRefreshTodoList) {
                    updateProvider(appContext, TodoListWidgetProvider::class.java)
                }
            }
        }
    }

    private fun updateProvider(context: Context, providerClass: Class<*>) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, providerClass)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isNotEmpty()) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                component = componentName
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }

    private fun refreshAppearanceProviders(context: Context) {
        updateProvider(context, HeatmapWidgetProvider::class.java)
        updateProvider(context, QuickCaptureWidgetProvider::class.java)
        updateProvider(context, QuickTodoWidgetProvider::class.java)
        TodoListWidgetProvider.refreshVisibilityAll(context)
        TodoListWidgetProvider.refreshAll(context)
    }
}
