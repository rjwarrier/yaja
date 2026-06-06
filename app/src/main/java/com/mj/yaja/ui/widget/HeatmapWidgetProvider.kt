package com.mj.yaja.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Log
import android.widget.RemoteViews
import com.mj.yaja.R
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.SettingsRepository
import com.mj.yaja.ui.screens.QuickCaptureActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/** Action to perform when the heatmap widget is tapped. */
enum class HeatmapTapAction(val key: String, val label: String) {
    LAUNCH_APP("launch_app", "Launch App"),
    QUICK_CAPTURE("quick_capture", "Quick Capture"),
    DO_NOTHING("do_nothing", "Do Nothing");

    companion object {
        fun fromKey(key: String?): HeatmapTapAction =
            entries.firstOrNull { it.key == key } ?: QUICK_CAPTURE
    }
}

class HeatmapWidgetProvider : AppWidgetProvider() {
    private fun logPerf(phase: String, elapsedMs: Long) {
        Log.d("HeatmapWidget", "perf:$phase=${elapsedMs}ms")
    }

    private fun finishPendingResultSafely(pendingResult: BroadcastReceiver.PendingResult?) {
        try {
            pendingResult?.finish()
        } catch (t: Throwable) {
            Log.w("HeatmapWidget", "PendingResult.finish() failed safely", t)
        }
    }

    private data class GridSpec(
        val rows: Int,
        val cols: Int
    )

    private data class WidgetSizeDp(
        val width: Int,
        val height: Int
    )

    private data class HeatmapLoadResult(
        val wordCounts: Map<LocalDate, Int>,
        val failed: Boolean
    )

    companion object {
        private const val ACTION_REFRESH_WIDGET_APPEARANCE =
            "com.mj.yaja.ACTION_REFRESH_WIDGET_APPEARANCE"
        // ── Prefs keys ────────────────────────────────────────────────────────
        private const val PREF_HEATMAP_CORNER = "heatmap_widget_corner_"
        private const val PREF_HEATMAP_MARGIN = "heatmap_widget_margin_"
        private const val PREF_HEATMAP_SHAPE  = "heatmap_widget_shape_"
        private const val PREF_HEATMAP_TAP    = "heatmap_widget_tap_"
        private const val PREF_HEATMAP_LAST_WIDTH_DP = "heatmap_widget_last_width_dp_"
        private const val PREF_HEATMAP_LAST_HEIGHT_DP = "heatmap_widget_last_height_dp_"

        // ── Layout constants ──────────────────────────────────────────────────
        private const val GAP_DP        = 1.5f
        private const val MIN_CELL_DP   = 6f    // never draw cells smaller than this
        private const val MAX_CELL_DP   = 28f   // never draw cells larger than this

        // ── Word-count cache (survives config-only updates) ───────────────────
        @Volatile private var cachedWordCounts: Map<LocalDate, Int>? = null
        @Volatile private var cacheTimestamp: Long = 0L
        private const val CACHE_TTL_MS = 60_000L
        private const val RECENT_HORIZON_DAYS = 548L // ~18 months

        fun invalidateCache() {
            cachedWordCounts = null
            cacheTimestamp = 0L
        }

        fun refreshAll(context: Context) {
            invalidateCache()
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, HeatmapWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(context, HeatmapWidgetProvider::class.java).apply {
                        action = ACTION_REFRESH_WIDGET_APPEARANCE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                )
            }
        }

        private fun resolveWidgetColors(context: Context): Pair<Int, Int> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val darkMode = (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                if (darkMode) {
                    context.getColor(android.R.color.system_accent1_800) to
                        context.getColor(android.R.color.system_accent1_100)
                } else {
                    context.getColor(android.R.color.system_accent1_100) to
                        context.getColor(android.R.color.system_accent1_800)
                }
            } else {
                androidx.core.content.ContextCompat.getColor(
                    context,
                    com.mj.yaja.R.color.ic_launcher_background
                ) to androidx.core.content.ContextCompat.getColor(
                    context,
                    com.mj.yaja.R.color.ic_launcher_foreground
                )
            }
        }

        // ── Pref accessors ────────────────────────────────────────────────────
        fun getCornerRadius(context: Context, appWidgetId: Int): Int {
            val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
            val saved = prefs.getInt("$PREF_HEATMAP_CORNER$appWidgetId", -1)
            return if (saved >= 0) saved else prefs.getInt("widget_corner_radius", 24)
        }

        fun setCornerRadius(context: Context, appWidgetId: Int, radiusDp: Int) {
            context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                .edit().putInt("$PREF_HEATMAP_CORNER$appWidgetId", radiusDp).commit()
        }

        fun getMargin(context: Context, appWidgetId: Int): Int {
            val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
            val saved = prefs.getInt("$PREF_HEATMAP_MARGIN$appWidgetId", -1)
            return if (saved >= 0) saved else 10
        }

        fun setMargin(context: Context, appWidgetId: Int, marginDp: Int) {
            context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                .edit().putInt("$PREF_HEATMAP_MARGIN$appWidgetId", marginDp).commit()
        }

        fun getCellShape(context: Context, appWidgetId: Int): CellShape {
            val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
            return CellShape.fromKey(prefs.getString("$PREF_HEATMAP_SHAPE$appWidgetId", null))
        }

        fun setCellShape(context: Context, appWidgetId: Int, shape: CellShape) {
            context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                .edit().putString("$PREF_HEATMAP_SHAPE$appWidgetId", shape.key).commit()
        }

        fun getTapAction(context: Context, appWidgetId: Int): HeatmapTapAction {
            val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
            return HeatmapTapAction.fromKey(prefs.getString("$PREF_HEATMAP_TAP$appWidgetId", null))
        }

        fun setTapAction(context: Context, appWidgetId: Int, action: HeatmapTapAction) {
            context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                .edit().putString("$PREF_HEATMAP_TAP$appWidgetId", action.key).commit()
        }

        private fun sanitizeDimensionDp(value: Int?): Int? = value?.takeIf { it > 0 }

        private fun resolveAxisDp(
            primary: Int?,
            secondary: Int?,
            stored: Int?,
            fallback: Int
        ): Int {
            return sanitizeDimensionDp(primary)
                ?: sanitizeDimensionDp(secondary)
                ?: sanitizeDimensionDp(stored)
                ?: fallback
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ── AppWidgetProvider callbacks ───────────────────────────────────────────

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.sendBroadcast(Intent(QuickCaptureWidgetProvider.ACTION_WIDGET_STATUS_CHANGED))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.sendBroadcast(Intent(QuickCaptureWidgetProvider.ACTION_WIDGET_STATUS_CHANGED))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val editor = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach {
            editor.remove("$PREF_HEATMAP_CORNER$it")
            editor.remove("$PREF_HEATMAP_MARGIN$it")
            editor.remove("$PREF_HEATMAP_SHAPE$it")
            editor.remove("$PREF_HEATMAP_TAP$it")
            editor.remove("$PREF_HEATMAP_LAST_WIDTH_DP$it")
            editor.remove("$PREF_HEATMAP_LAST_HEIGHT_DP$it")
        }
        editor.apply()
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                appWidgetIds.forEach { updateWidget(appContext, appWidgetManager, it) }
            } finally {
                finishPendingResultSafely(pendingResult)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                updateWidget(appContext, appWidgetManager, appWidgetId)
            } finally {
                finishPendingResultSafely(pendingResult)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH_WIDGET_APPEARANCE -> {
                invalidateCache()
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, HeatmapWidgetProvider::class.java)
                )
                if (appWidgetIds.isNotEmpty()) {
                    val pendingResult = goAsync()
                    val appContext = context.applicationContext
                    scope.launch {
                        try {
                            appWidgetIds.forEach { widgetId ->
                                updateWidget(appContext, appWidgetManager, widgetId)
                            }
                        } finally {
                            finishPendingResultSafely(pendingResult)
                        }
                    }
                }
            }
        }
    }

    // ── Internal update ───────────────────────────────────────────────────────

    private suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val startedAt = System.currentTimeMillis()
        val loadResult = resolveWordCounts(context)
        val views = buildRemoteViews(context, appWidgetManager, appWidgetId, loadResult)
        appWidgetManager.updateAppWidget(appWidgetId, views)
        logPerf("updateWidget", System.currentTimeMillis() - startedAt)
    }

    /** Returns cached word counts or loads fresh from IO. */
    private suspend fun resolveWordCounts(
        context: Context
    ): HeatmapLoadResult {
        val startedAt = System.currentTimeMillis()
        val now = System.currentTimeMillis()
        cachedWordCounts?.let { cached ->
            if (now - cacheTimestamp < CACHE_TTL_MS) {
                logPerf("resolveWordCounts.cached", System.currentTimeMillis() - startedAt)
                return HeatmapLoadResult(wordCounts = cached, failed = false)
            }
        }
        return withContext(Dispatchers.IO) {
            loadWordCounts(context)
        }.also {
            cachedWordCounts = it.wordCounts
            cacheTimestamp = System.currentTimeMillis()
            logPerf("resolveWordCounts.fresh", System.currentTimeMillis() - startedAt)
        }
    }

    private fun loadWordCounts(context: Context): HeatmapLoadResult {
        val result = mutableMapOf<LocalDate, Int>()
        return try {
            val settingsRepo = SettingsRepository.getInstance(context)
            val fileManager = MarkdownFileManager.getInstance(context, settingsRepo)
            val startDate = LocalDate.now().minusDays(RECENT_HORIZON_DAYS - 1)
            result.putAll(fileManager.getWordCountSnapshotSince(startDate))
            HeatmapLoadResult(wordCounts = result, failed = false)
        } catch (_: Exception) {
            HeatmapLoadResult(wordCounts = result, failed = true)
        }
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        loadResult: HeatmapLoadResult
    ): RemoteViews {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val density = context.resources.displayMetrics.density
        val sizeDp = resolveWidgetSizeDp(context, appWidgetId, options)

        val widthPx  = (sizeDp.width * density).toInt().coerceAtLeast(1)
        val heightPx = (sizeDp.height * density).toInt().coerceAtLeast(1)
        val cornerDp = getCornerRadius(context, appWidgetId)
        val marginDp = getMargin(context, appWidgetId)
        val shape    = getCellShape(context, appWidgetId)
        val wordCounts = loadResult.wordCounts

        val (bgColor, primaryColor) = resolveWidgetColors(context)

        val views = RemoteViews(context.packageName, R.layout.widget_heatmap)

        // ── Card background (rounded shape) ───────────────────────────────────
        views.setImageViewBitmap(
            R.id.heatmap_widget_background,
            buildCardBackground(widthPx, heightPx, cornerDp * density, bgColor)
        )

        // ── Heatmap bitmap — exact grid size, fitCenter keeps cells square ────
        views.setImageViewBitmap(
            R.id.heatmap_image,
            buildHeatmapBitmap(
                widthPx, heightPx,
                marginPx = marginDp * density,
                density  = density,
                wordCounts   = wordCounts,
                bgColor      = bgColor,
                primaryColor = primaryColor,
                cellShape    = shape
            )
        )

        val statusText = when {
            loadResult.failed -> context.getString(R.string.heatmap_widget_status_refresh)
            wordCounts.isEmpty() -> context.getString(R.string.heatmap_widget_status_empty)
            else -> null
        }
        views.setTextViewText(R.id.heatmap_status_text, statusText ?: "")
        views.setViewVisibility(
            R.id.heatmap_status_text,
            if (statusText == null) android.view.View.GONE else android.view.View.VISIBLE
        )

        // ── Tap action ──────────────────────────────────────────────────────
        val tapAction = getTapAction(context, appWidgetId)
        when (tapAction) {
            HeatmapTapAction.LAUNCH_APP -> {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                if (launchIntent != null) {
                    val pendingIntent = PendingIntent.getActivity(
                        context, appWidgetId, launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.heatmap_widget_root, pendingIntent)
                }
            }
            HeatmapTapAction.QUICK_CAPTURE -> {
                val captureIntent = Intent(context, QuickCaptureActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, appWidgetId, captureIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.heatmap_widget_root, pendingIntent)
            }
            HeatmapTapAction.DO_NOTHING -> {
                // No click action — explicitly clear any existing intent
                views.setOnClickPendingIntent(R.id.heatmap_widget_root, null)
            }
        }

        return views
    }


    private fun resolveWidgetSizeDp(
        context: Context,
        appWidgetId: Int,
        options: android.os.Bundle
    ): WidgetSizeDp {
        val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
        val storedWidth = prefs.getInt("$PREF_HEATMAP_LAST_WIDTH_DP$appWidgetId", 0)
        val storedHeight = prefs.getInt("$PREF_HEATMAP_LAST_HEIGHT_DP$appWidgetId", 0)

        // Some launchers intermittently report 0 or -1 here during ordinary refreshes.
        // Falling back to the opposite bound and then the last known-good size keeps
        // the grid from collapsing to a single visible "today" cell.
        val isPortrait = context.resources.configuration.orientation ==
            Configuration.ORIENTATION_PORTRAIT
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

        val widthDp = resolveAxisDp(
            primary = if (isPortrait) minWidth else maxWidth,
            secondary = if (isPortrait) maxWidth else minWidth,
            stored = storedWidth,
            fallback = 180
        )
        val heightDp = resolveAxisDp(
            primary = if (isPortrait) maxHeight else minHeight,
            secondary = if (isPortrait) minHeight else maxHeight,
            stored = storedHeight,
            fallback = 110
        )

        prefs.edit()
            .putInt("$PREF_HEATMAP_LAST_WIDTH_DP$appWidgetId", widthDp)
            .putInt("$PREF_HEATMAP_LAST_HEIGHT_DP$appWidgetId", heightDp)
            .apply()

        return WidgetSizeDp(width = widthDp, height = heightDp)
    }

    private fun computeGridSpec(
        context: Context,
        options: android.os.Bundle,
        density: Float,
        marginDp: Int
    ): GridSpec {
        val isPortrait = context.resources.configuration.orientation ==
            Configuration.ORIENTATION_PORTRAIT
        val widthDp = if (isPortrait) {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
        } else {
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            )
        }
        val heightDp = if (isPortrait) {
            options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            )
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        }

        val widgetWidthPx = (widthDp * density).toInt().coerceAtLeast(1)
        val widgetHeightPx = (heightDp * density).toInt().coerceAtLeast(1)
        val gapPx = GAP_DP * density
        val minCellPx = MIN_CELL_DP * density
        val maxCellPx = MAX_CELL_DP * density
        val marginPx = marginDp * density
        val usableW = (widgetWidthPx - marginPx * 2).coerceAtLeast(minCellPx * 2)
        val usableH = (widgetHeightPx - marginPx * 2).coerceAtLeast(minCellPx * 2)
        val targetCellPx = (12f * density).coerceIn(minCellPx, maxCellPx)
        val rows = ((usableH + gapPx) / (targetCellPx + gapPx))
            .toInt()
            .coerceIn(1, 500)
        val cellPx = ((usableH - gapPx * (rows - 1)) / rows)
            .coerceIn(minCellPx, maxCellPx)
        val cols = ((usableW + gapPx) / (cellPx + gapPx))
            .toInt()
            .coerceIn(1, 500)
        return GridSpec(rows = rows, cols = cols)
    }
    // ── Bitmap builders ───────────────────────────────────────────────────────

    private fun buildCardBackground(
        widthPx: Int, heightPx: Int, cornerRadiusPx: Float, color: Int
    ): Bitmap {
        val bmp    = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val r      = cornerRadiusPx.coerceAtMost(minOf(widthPx, heightPx) / 2f)
        canvas.drawRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), r, r, paint)
        return bmp
    }

    /**
     * Renders the heatmap cells into a bitmap sized exactly to the grid plus [marginPx] border.
     *
     * Both [numRows] and [numCols] are derived from the widget's actual dimensions so cells
     * are always square. Row count is computed from HEIGHT so the grid fills it exactly;
     * column count fills the width with the same cell size. The ImageView uses fitCenter
     * for uniform scaling. Today is always the bottom-right cell.
     */
    private fun buildHeatmapBitmap(
        widgetWidthPx: Int,
        widgetHeightPx: Int,
        marginPx: Float,
        density: Float,
        wordCounts: Map<LocalDate, Int>,
        bgColor: Int,
        primaryColor: Int,
        cellShape: CellShape
    ): Bitmap {
        val gapPx     = GAP_DP      * density
        val minCellPx = MIN_CELL_DP * density
        val maxCellPx = MAX_CELL_DP * density
        val usableW   = (widgetWidthPx  - marginPx * 2).coerceAtLeast(minCellPx * 2)
        val usableH   = (widgetHeightPx - marginPx * 2).coerceAtLeast(minCellPx * 2)

        // ── Step 1: how many rows fill the height at a comfortable cell size ────────
        // No fixed row count — both dimensions adapt to the widget size.
        // Target ~12 dp cells; actual cell size is then back-computed from the
        // integer row count so that gridH == usableH exactly (no top/bottom gap).
        val targetCellPx = (12f * density).coerceIn(minCellPx, maxCellPx)
        val numRows = ((usableH + gapPx) / (targetCellPx + gapPx))
            .toInt().coerceIn(1, 500)

        // ── Step 2: cell size derived FROM HEIGHT — guarantees height fills exactly ─
        val cellPx = ((usableH - gapPx * (numRows - 1)) / numRows)
            .coerceIn(minCellPx, maxCellPx)

        // ── Step 3: columns fill the width with the same square cell size ──────────
        val numCols = ((usableW + gapPx) / (cellPx + gapPx))
            .toInt().coerceIn(1, 500)

        // ── Step 4: build date grid — today is always bottom-right cell ──────────
        val today      = LocalDate.now()
        val totalCells = numRows * numCols
        val startDate  = today.minusDays(totalCells.toLong() - 1)

        val columns = mutableListOf<List<LocalDate>>()
        var current = startDate
        repeat(numCols) {
            val col = mutableListOf<LocalDate>()
            repeat(numRows) { col.add(current); current = current.plusDays(1) }
            columns.add(col)
        }

        val emptyColor = alphaBlend(0xFF888888.toInt(), 0.20f, bgColor)

        // ── Step 5: build bitmap at exact grid dimensions + margin border ────────
        // Transparent background → card background colour shows through.
        // fitCenter on the ImageView scales this bitmap uniformly → cells stay square.
        val gridW = numCols * (cellPx + gapPx) - gapPx
        val gridH = numRows * (cellPx + gapPx) - gapPx
        val bmpW  = (gridW + marginPx * 2).toInt().coerceAtLeast(1)
        val bmpH  = (gridH + marginPx * 2).toInt().coerceAtLeast(1)

        val bmp    = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
        val path   = Path()

        columns.forEachIndexed { col, column ->
            column.forEachIndexed { row, date ->
                val wordCount = wordCounts[date] ?: 0
                val hasEntry  = wordCounts.containsKey(date)
                paint.color = resolveHeatmapCellColor(
                    wordCount = wordCount,
                    hasEntry = hasEntry,
                    emptyColor = emptyColor,
                    primaryColor = primaryColor
                )
                val left = marginPx + col * (cellPx + gapPx)
                val top  = marginPx + row * (cellPx + gapPx)
                drawCell(canvas, paint, path, left, top, cellPx, cellShape, density)
            }
        }

        if (wordCounts.isEmpty()) {
            val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = alphaBlend(primaryColor, 0.10f, bgColor)
                style = Paint.Style.STROKE
                strokeWidth = density.coerceAtLeast(1f)
            }
            val previewCols = numCols.coerceAtMost(ceil(numCols / 2f).toInt().coerceAtLeast(6))
            val previewRows = numRows.coerceAtMost(ceil(numRows / 2f).toInt().coerceAtLeast(3))
            for (col in 0 until previewCols) {
                for (row in 0 until previewRows) {
                    val left = marginPx + col * (cellPx + gapPx)
                    val top = marginPx + row * (cellPx + gapPx)
                    drawCell(canvas, guidePaint, path, left, top, cellPx, cellShape, density)
                }
            }
        }
        return bmp
    }

    private fun resolveHeatmapCellColor(
        wordCount: Int,
        hasEntry: Boolean,
        emptyColor: Int,
        primaryColor: Int
    ): Int {
        if (!hasEntry) return emptyColor
        val intenseColor = 0xFFE53935.toInt()
        return when {
            wordCount < 25 -> alphaBlend(primaryColor, 0.24f, emptyColor)
            wordCount < 50 -> alphaBlend(primaryColor, 0.34f, emptyColor)
            wordCount < 75 -> alphaBlend(primaryColor, 0.44f, emptyColor)
            wordCount < 100 -> alphaBlend(primaryColor, 0.54f, emptyColor)
            wordCount < 150 -> alphaBlend(primaryColor, 0.64f, emptyColor)
            wordCount < 200 -> alphaBlend(primaryColor, 0.74f, emptyColor)
            wordCount < 300 -> alphaBlend(primaryColor, 0.84f, emptyColor)
            wordCount < 500 -> alphaBlend(primaryColor, 0.94f, emptyColor)
            wordCount < 800 -> alphaBlend(intenseColor, 0.45f, primaryColor)
            else -> intenseColor
        }
    }

    private fun drawCell(
        canvas: Canvas, paint: Paint, path: Path,
        left: Float, top: Float, size: Float,
        shape: CellShape, density: Float
    ) {
        val cx   = left + size / 2f
        val cy   = top  + size / 2f
        val half = size / 2f
        val rect = RectF(left, top, left + size, top + size)

        when (shape) {
            CellShape.SQUARE -> canvas.drawRect(rect, paint)

            CellShape.ROUNDED -> {
                val r = (3f * density).coerceAtMost(half)
                canvas.drawRoundRect(rect, r, r, paint)
            }

            CellShape.CIRCLE -> canvas.drawCircle(cx, cy, half, paint)

            CellShape.CUT_CORNER -> {
                val cut = half * 0.35f
                path.reset()
                path.moveTo(left + cut, top)
                path.lineTo(left + size - cut, top)
                path.lineTo(left + size, top + cut)
                path.lineTo(left + size, top + size - cut)
                path.lineTo(left + size - cut, top + size)
                path.lineTo(left + cut, top + size)
                path.lineTo(left, top + size - cut)
                path.lineTo(left, top + cut)
                path.close()
                canvas.drawPath(path, paint)
            }

            CellShape.DIAMOND -> {
                path.reset()
                path.moveTo(cx, top)
                path.lineTo(left + size, cy)
                path.lineTo(cx, top + size)
                path.lineTo(left, cy)
                path.close()
                canvas.drawPath(path, paint)
            }

            CellShape.HEART -> {
                // Pentagon: 5-sided regular shape
                val points = 5
                val radius = half * 0.85f
                val angleStep = (2 * Math.PI) / points
                path.reset()
                for (i in 0 until points) {
                    val angle = angleStep * i - Math.PI / 2  // Start from top
                    val px = cx + radius * cos(angle).toFloat()
                    val py = cy + radius * sin(angle).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }

            CellShape.SUNNY -> {
                val points = 8
                val outerR = half
                val innerR = half * 0.72f
                val step   = Math.PI / points
                path.reset()
                for (i in 0 until points * 2) {
                    val r     = if (i % 2 == 0) outerR else innerR
                    val angle = step * i - Math.PI / 2
                    val px    = cx + r * cos(angle).toFloat()
                    val py    = cy + r * sin(angle).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }

            CellShape.COOKIE_4 -> {
                val r      = half * 0.58f
                val offset = half - r
                canvas.drawCircle(cx - offset, cy - offset, r, paint)
                canvas.drawCircle(cx + offset, cy - offset, r, paint)
                canvas.drawCircle(cx - offset, cy + offset, r, paint)
                canvas.drawCircle(cx + offset, cy + offset, r, paint)
            }

            CellShape.COOKIE_6 -> {
                val r         = half * 0.52f
                val offset    = half - r
                val angleStep = (2 * Math.PI) / 6
                for (i in 0 until 6) {
                    val angle = angleStep * i - Math.PI / 2
                    canvas.drawCircle(
                        cx + offset * cos(angle).toFloat(),
                        cy + offset * sin(angle).toFloat(),
                        r, paint
                    )
                }
            }
        }
    }

    private fun alphaBlend(fg: Int, alpha: Float, bg: Int): Int {
        val a = alpha.coerceIn(0f, 1f)
        val r = ((fg shr 16 and 0xFF) * a + (bg shr 16 and 0xFF) * (1 - a)).toInt()
        val g = ((fg shr 8  and 0xFF) * a + (bg shr 8  and 0xFF) * (1 - a)).toInt()
        val b = ((fg        and 0xFF) * a + (bg        and 0xFF) * (1 - a)).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}






