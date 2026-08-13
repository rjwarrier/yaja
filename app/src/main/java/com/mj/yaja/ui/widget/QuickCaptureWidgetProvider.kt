package com.mj.yaja.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.content.res.Configuration
import android.widget.RemoteViews
import com.mj.yaja.R
import com.mj.yaja.ui.screens.QuickCaptureActivity

class QuickCaptureWidgetProvider : AppWidgetProvider() {

        companion object {
                const val ACTION_WIDGET_STATUS_CHANGED = "com.mj.yaja.ACTION_WIDGET_STATUS_CHANGED"
                private const val ACTION_REFRESH_WIDGET_APPEARANCE =
                        "com.mj.yaja.ACTION_REFRESH_WIDGET_APPEARANCE"
                private const val PREF_QC_CORNER = "qc_widget_corner_"
                private const val PREF_QC_LABEL  = "qc_widget_label_"
                private const val PREF_QC_ICON   = "qc_widget_icon_"
                private const val PREF_QC_LABEL_TEXT = "qc_widget_label_text_"

                fun refreshAll(context: Context) {
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val componentName = ComponentName(context, QuickCaptureWidgetProvider::class.java)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                        if (appWidgetIds.isNotEmpty()) {
                                context.sendBroadcast(
                                        Intent(context, QuickCaptureWidgetProvider::class.java).apply {
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

                fun getCornerRadius(context: Context, appWidgetId: Int): Int {
                        val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                        val saved = prefs.getInt("$PREF_QC_CORNER$appWidgetId", -1)
                        return if (saved >= 0) saved else prefs.getInt("widget_corner_radius", 24)
                }

                fun setCornerRadius(context: Context, appWidgetId: Int, radiusDp: Int) {
                        context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                                .edit().putInt("$PREF_QC_CORNER$appWidgetId", radiusDp).apply()
                }

                fun getShowLabel(context: Context, appWidgetId: Int): Boolean {
                        val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                        // Fall back to old global pref for existing users, then default true
                        return if (prefs.contains("$PREF_QC_LABEL$appWidgetId"))
                            prefs.getBoolean("$PREF_QC_LABEL$appWidgetId", true)
                        else
                            prefs.getBoolean("show_widget_label", true)
                }

                fun setShowLabel(context: Context, appWidgetId: Int, show: Boolean) {
                        context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                                .edit().putBoolean("$PREF_QC_LABEL$appWidgetId", show).apply()
                }

                fun getShowIcon(context: Context, appWidgetId: Int): Boolean {
                        val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                        return prefs.getBoolean("$PREF_QC_ICON$appWidgetId", true)
                }

                fun setShowIcon(context: Context, appWidgetId: Int, show: Boolean) {
                        context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                                .edit().putBoolean("$PREF_QC_ICON$appWidgetId", show).apply()
                }

                fun getLabelText(context: Context, appWidgetId: Int): String {
                        val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                        val defaultLabel = context.getString(R.string.widget_qc_label_placeholder)
                        return prefs.getString("$PREF_QC_LABEL_TEXT$appWidgetId", defaultLabel) ?: defaultLabel
                }

                fun setLabelText(context: Context, appWidgetId: Int, text: String) {
                        context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                                .edit().putString("$PREF_QC_LABEL_TEXT$appWidgetId", text).apply()
                }

                private const val PREF_QC_SHAPE = "qc_widget_shape_"

                fun getCellShape(context: Context, appWidgetId: Int): CellShape {
                        val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                        val saved = prefs.getString("$PREF_QC_SHAPE$appWidgetId", null)
                        return CellShape.fromKey(saved)
                }

                fun setCellShape(context: Context, appWidgetId: Int, shape: CellShape) {
                        context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                                .edit().putString("$PREF_QC_SHAPE$appWidgetId", shape.key).commit()
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

        override fun onUpdate(
                context: Context,
                appWidgetManager: AppWidgetManager,
                appWidgetIds: IntArray
        ) {
                // Perform this loop procedure for each App Widget that belongs to this provider
                appWidgetIds.forEach { appWidgetId ->
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                }
        }

        override fun onReceive(context: Context, intent: Intent) {
                super.onReceive(context, intent)
                when (intent.action) {
                        ACTION_REFRESH_WIDGET_APPEARANCE -> {
                                val appWidgetManager = AppWidgetManager.getInstance(context)
                                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                                        ComponentName(context, QuickCaptureWidgetProvider::class.java)
                                )
                                if (appWidgetIds.isNotEmpty()) {
                                        onUpdate(context, appWidgetManager, appWidgetIds)
                                }
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
                updateAppWidget(context, appWidgetManager, appWidgetId)
        }

        private fun updateAppWidget(
                context: Context,
                appWidgetManager: AppWidgetManager,
                appWidgetId: Int
        ) {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

                // Orientation-aware dimensions (same logic as HeatmapWidgetProvider):
                //   Portrait  → width = MIN_WIDTH,  height = MAX_HEIGHT
                //   Landscape → width = MAX_WIDTH,  height = MIN_HEIGHT
                val isPortrait = context.resources.configuration.orientation ==
                        Configuration.ORIENTATION_PORTRAIT
                val minWidthDp = if (isPortrait)
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 80)
                else
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 80))
                val minHeightDp = if (isPortrait)
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80))
                else
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80)

                // Read preferences
                val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                val cornerRadiusDp = getCornerRadius(context, appWidgetId)
                val showLabelPref = getShowLabel(context, appWidgetId)

                // Read colors from resources — resolves night variants automatically
                val (bgColor, fgColor) = resolveWidgetColors(context)

                // Determine layout based on dimensions
                val layoutResId =
                        when {
                                minWidthDp > minHeightDp * 1.5 ->
                                        R.layout.widget_quick_capture_horizontal
                                minWidthDp >= 100 && minHeightDp >= 100 ->
                                        R.layout.widget_quick_capture_large
                                else -> R.layout.widget_quick_capture
                        }

                // Create an Intent to launch QuickCaptureActivity
                val intent =
                        Intent(context, QuickCaptureActivity::class.java).apply {
                                flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }

                val pendingIntent: PendingIntent =
                        PendingIntent.getActivity(
                                context,
                                0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                // Get the layout for the App Widget
                val views = RemoteViews(context.packageName, layoutResId)
                views.setOnClickPendingIntent(R.id.widget_root_layout, pendingIntent)

                // Apply corner radius dynamically
                val displayMetrics = context.resources.displayMetrics
                val density = displayMetrics.density
                val radiusPx = cornerRadiusDp * density

                // Determine actual pixel dimensions (fallback to rough defaults if 0)
                val widthPx =
                        if (minWidthDp > 0) (minWidthDp * density).toInt()
                        else (200 * density).toInt()
                val heightPx =
                        if (minHeightDp > 0) (minHeightDp * density).toInt()
                        else (100 * density).toInt()

                // Shaped card background — explicit colour so it matches the heatmap widget
                val widgetShape = getCellShape(context, appWidgetId)
                val bitmap = createShapedBitmap(widthPx, heightPx, radiusPx, bgColor, widgetShape)
                views.setImageViewBitmap(R.id.widget_background, bitmap)

                // Icon + label tint — set programmatically to match the explicit colour
                val showIconPref = getShowIcon(context, appWidgetId)
                // If label is off, icon must always be visible
                val showIcon = if (!showLabelPref) true else showIconPref
                views.setInt(R.id.widget_icon, "setColorFilter", fgColor)
                views.setTextColor(R.id.widget_label, fgColor)
                views.setViewVisibility(
                        R.id.widget_icon,
                        if (showIcon) android.view.View.VISIBLE else android.view.View.GONE
                )

                // Set custom label text
                val labelText = getLabelText(context, appWidgetId)
                views.setTextViewText(R.id.widget_label, labelText)

                // Dynamic Sizing & Label Visibility
                val smallestDim = minOf(minWidthDp, minHeightDp)
                // Auto-hide label if widget is too small (e.g. 1x1) even if preference is ON
                val showLabel = showLabelPref && smallestDim >= 80

                views.setViewVisibility(
                        R.id.widget_label,
                        if (showLabel) android.view.View.VISIBLE else android.view.View.GONE
                )

                // Adjust text size based on widget height/width
                val textSizeSp =
                        when {
                                smallestDim >= 300 -> 24f
                                smallestDim >= 200 -> 20f
                                smallestDim >= 150 -> 18f
                                smallestDim >= 100 -> 14f
                                else -> 12f
                        }
                views.setTextViewTextSize(
                        R.id.widget_label,
                        android.util.TypedValue.COMPLEX_UNIT_SP,
                        textSizeSp
                )

                // Apply adaptive padding ONLY to content to prevent clipping by rounded corners
                // Background (widget_background) remains unpadded to fill the handles
                val hFactor = if (showLabel) 0.35f else 0.2f
                val vFactor = if (showLabel) 0.25f else 0.2f
                val hPadding = (radiusPx * hFactor).toInt().coerceAtLeast((4 * density).toInt())
                val vPadding = (radiusPx * vFactor).toInt().coerceAtLeast((4 * density).toInt())
                views.setViewPadding(
                        R.id.widget_content_container,
                        hPadding,
                        vPadding,
                        hPadding,
                        vPadding
                )

                // Tell the AppWidgetManager to perform an update on the current app widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createShapedBitmap(
                width: Int,
                height: Int,
                radiusPx: Float,
                color: Int,
                shape: CellShape
        ): Bitmap {
                val w = width.coerceAtLeast(1).toFloat()
                val h = height.coerceAtLeast(1).toFloat()
                val bitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                val rect = RectF(0f, 0f, w, h)
                val cx = w / 2f
                val cy = h / 2f
                val half = minOf(w, h) / 2f
                val path = Path()

                when (shape) {
                        CellShape.SQUARE -> {
                                canvas.drawRect(rect, paint)
                        }

                        CellShape.ROUNDED -> {
                                val r = radiusPx.coerceAtMost(half)
                                canvas.drawRoundRect(rect, r, r, paint)
                        }

                        CellShape.CIRCLE -> {
                                canvas.drawCircle(cx, cy, half, paint)
                        }

                        CellShape.CUT_CORNER -> {
                                val cut = half * 0.35f
                                path.moveTo(cut, 0f)
                                path.lineTo(w - cut, 0f)
                                path.lineTo(w, cut)
                                path.lineTo(w, h - cut)
                                path.lineTo(w - cut, h)
                                path.lineTo(cut, h)
                                path.lineTo(0f, h - cut)
                                path.lineTo(0f, cut)
                                path.close()
                                canvas.drawPath(path, paint)
                        }

                        CellShape.DIAMOND -> {
                                path.moveTo(cx, 0f)
                                path.lineTo(w, cy)
                                path.lineTo(cx, h)
                                path.lineTo(0f, cy)
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
                                        val px = cx + radius * Math.cos(angle).toFloat()
                                        val py = cy + radius * Math.sin(angle).toFloat()
                                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                }
                                path.close()
                                canvas.drawPath(path, paint)
                        }

                        CellShape.SUNNY -> {
                                val points = 8
                                val outerR = half
                                val innerR = half * 0.72f
                                val step = Math.PI / points
                                path.reset()
                                for (i in 0 until points * 2) {
                                        val r = if (i % 2 == 0) outerR else innerR
                                        val angle = step * i - Math.PI / 2
                                        val px = cx + r * Math.cos(angle).toFloat()
                                        val py = cy + r * Math.sin(angle).toFloat()
                                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                }
                                path.close()
                                canvas.drawPath(path, paint)
                        }

                        CellShape.COOKIE_4 -> {
                                val r = half * 0.58f
                                val offset = half - r
                                canvas.drawCircle(cx - offset, cy - offset, r, paint)
                                canvas.drawCircle(cx + offset, cy - offset, r, paint)
                                canvas.drawCircle(cx - offset, cy + offset, r, paint)
                                canvas.drawCircle(cx + offset, cy + offset, r, paint)
                        }

                        CellShape.COOKIE_6 -> {
                                val r = half * 0.52f
                                val offset = half - r
                                val points = 6
                                val angleStep = (2 * Math.PI) / points
                                for (i in 0 until points) {
                                        val angle = angleStep * i - Math.PI / 2
                                        canvas.drawCircle(
                                                cx + offset * Math.cos(angle).toFloat(),
                                                cy + offset * Math.sin(angle).toFloat(),
                                                r, paint
                                        )
                                }
                        }
                }
                return bitmap
        }
}
