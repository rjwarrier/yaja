package com.mj.yaja.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.res.Configuration
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.mj.yaja.R
import com.mj.yaja.ui.screens.QuickTodoActivity

class QuickTodoWidgetProvider : AppWidgetProvider() {

        companion object {
                const val ACTION_WIDGET_STATUS_CHANGED = "com.mj.yaja.ACTION_WIDGET_STATUS_CHANGED"
                private const val ACTION_REFRESH_WIDGET_APPEARANCE =
                        "com.mj.yaja.ACTION_REFRESH_TODO_WIDGET_APPEARANCE"
                private const val PREF_QT_CORNER = "qt_widget_corner_"
                private const val PREF_QT_LABEL = "qt_widget_label_"
                private const val PREF_QT_ICON = "qt_widget_icon_"
                private const val PREF_QT_LABEL_TEXT = "qt_widget_label_text_"
                private const val PREF_QT_SHAPE = "qt_widget_shape_"

                fun refreshAll(context: Context) {
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val componentName = ComponentName(context, QuickTodoWidgetProvider::class.java)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                        if (appWidgetIds.isNotEmpty()) {
                                context.sendBroadcast(
                                        Intent(context, QuickTodoWidgetProvider::class.java).apply {
                                                action = ACTION_REFRESH_WIDGET_APPEARANCE
                                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                                        }
                                )
                        }
                }

                fun getCornerRadius(context: Context, appWidgetId: Int): Int {
                        val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                        val saved = prefs.getInt("$PREF_QT_CORNER$appWidgetId", -1)
                        return if (saved >= 0) saved else prefs.getInt("widget_corner_radius", 24)
                }

                fun setCornerRadius(context: Context, appWidgetId: Int, radiusDp: Int) {
                        context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                                .edit().putInt("$PREF_QT_CORNER$appWidgetId", radiusDp).apply()
                }

                fun getShowLabel(context: Context, appWidgetId: Int): Boolean {
                        val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                        return if (prefs.contains("$PREF_QT_LABEL$appWidgetId")) {
                                prefs.getBoolean("$PREF_QT_LABEL$appWidgetId", true)
                        } else {
                                prefs.getBoolean("show_widget_label", true)
                        }
                }

                fun setShowLabel(context: Context, appWidgetId: Int, show: Boolean) {
                        context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                                .edit().putBoolean("$PREF_QT_LABEL$appWidgetId", show).apply()
                }

                fun getShowIcon(context: Context, appWidgetId: Int): Boolean {
                        val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                        return prefs.getBoolean("$PREF_QT_ICON$appWidgetId", true)
                }

                fun setShowIcon(context: Context, appWidgetId: Int, show: Boolean) {
                        context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                                .edit().putBoolean("$PREF_QT_ICON$appWidgetId", show).apply()
                }

                fun getLabelText(context: Context, appWidgetId: Int): String {
                        val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                        return prefs.getString("$PREF_QT_LABEL_TEXT$appWidgetId", "Todo") ?: "Todo"
                }

                fun setLabelText(context: Context, appWidgetId: Int, text: String) {
                        context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                                .edit().putString("$PREF_QT_LABEL_TEXT$appWidgetId", text).apply()
                }

                fun getCellShape(context: Context, appWidgetId: Int): CellShape {
                        val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                        return CellShape.fromKey(prefs.getString("$PREF_QT_SHAPE$appWidgetId", null))
                }

                fun setCellShape(context: Context, appWidgetId: Int, shape: CellShape) {
                        context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                                .edit().putString("$PREF_QT_SHAPE$appWidgetId", shape.key).commit()
                }

                fun saveOptions(
                        context: Context,
                        appWidgetId: Int,
                        radiusDp: Int,
                        showLabel: Boolean,
                        showIcon: Boolean,
                        labelText: String,
                        shape: CellShape
                ) {
                        context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                                .edit()
                                .putInt("$PREF_QT_CORNER$appWidgetId", radiusDp)
                                .putBoolean("$PREF_QT_LABEL$appWidgetId", showLabel)
                                .putBoolean("$PREF_QT_ICON$appWidgetId", showIcon)
                                .putString("$PREF_QT_LABEL_TEXT$appWidgetId", labelText)
                                .putString("$PREF_QT_SHAPE$appWidgetId", shape.key)
                                .commit()
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
                appWidgetIds.forEach { appWidgetId ->
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                }
        }

        override fun onReceive(context: Context, intent: Intent) {
                super.onReceive(context, intent)
                if (intent.action == ACTION_REFRESH_WIDGET_APPEARANCE) {
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(
                                ComponentName(context, QuickTodoWidgetProvider::class.java)
                        )
                        if (appWidgetIds.isNotEmpty()) {
                                onUpdate(context, appWidgetManager, appWidgetIds)
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
                val isPortrait = context.resources.configuration.orientation ==
                        android.content.res.Configuration.ORIENTATION_PORTRAIT
                val minWidthDp = if (isPortrait) {
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 80)
                } else {
                        options.getInt(
                                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 80)
                        )
                }
                val minHeightDp = if (isPortrait) {
                        options.getInt(
                                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80)
                        )
                } else {
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80)
                }

                val cornerRadiusDp = getCornerRadius(context, appWidgetId)
                val showLabelPref = getShowLabel(context, appWidgetId)
                val (bgColor, fgColor) = resolveWidgetColors(context)

                val layoutResId =
                        when {
                                minWidthDp > minHeightDp * 1.5 -> R.layout.widget_quick_capture_horizontal
                                minWidthDp >= 100 && minHeightDp >= 100 -> R.layout.widget_quick_capture_large
                                else -> R.layout.widget_quick_capture
                        }

                val intent = Intent(context, QuickTodoActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val views = RemoteViews(context.packageName, layoutResId)
                views.setOnClickPendingIntent(R.id.widget_root_layout, pendingIntent)

                val density = context.resources.displayMetrics.density
                val radiusPx = cornerRadiusDp * density
                val widthPx = if (minWidthDp > 0) (minWidthDp * density).toInt() else (200 * density).toInt()
                val heightPx = if (minHeightDp > 0) (minHeightDp * density).toInt() else (100 * density).toInt()
                val widgetShape = getCellShape(context, appWidgetId)
                val bitmap = createShapedBitmap(widthPx, heightPx, radiusPx, bgColor, widgetShape)
                views.setImageViewBitmap(R.id.widget_background, bitmap)

                val showIconPref = getShowIcon(context, appWidgetId)
                val showIcon = if (!showLabelPref) true else showIconPref
                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_todo)
                views.setInt(R.id.widget_icon, "setColorFilter", fgColor)
                views.setTextColor(R.id.widget_label, fgColor)
                views.setViewVisibility(
                        R.id.widget_icon,
                        if (showIcon) android.view.View.VISIBLE else android.view.View.GONE
                )

                val labelText = getLabelText(context, appWidgetId)
                views.setTextViewText(R.id.widget_label, labelText)

                val smallestDim = minOf(minWidthDp, minHeightDp)
                val showLabel = showLabelPref && smallestDim >= 80
                views.setViewVisibility(
                        R.id.widget_label,
                        if (showLabel) android.view.View.VISIBLE else android.view.View.GONE
                )

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

                val hFactor = if (showLabel) 0.35f else 0.2f
                val vFactor = if (showLabel) 0.25f else 0.2f
                val hPadding = (radiusPx * hFactor).toInt().coerceAtLeast((4 * density).toInt())
                val vPadding = (radiusPx * vFactor).toInt().coerceAtLeast((4 * density).toInt())
                views.setViewPadding(R.id.widget_content_container, hPadding, vPadding, hPadding, vPadding)

                appWidgetManager.updateAppWidget(appWidgetId, views)
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
                        ContextCompat.getColor(context, R.color.ic_launcher_background) to
                                ContextCompat.getColor(context, R.color.ic_launcher_foreground)
                }
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
                        CellShape.SQUARE -> canvas.drawRect(rect, paint)
                        CellShape.ROUNDED -> {
                                val r = radiusPx.coerceAtMost(half)
                                canvas.drawRoundRect(rect, r, r, paint)
                        }
                        CellShape.CIRCLE -> canvas.drawCircle(cx, cy, half, paint)
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
                                val points = 5
                                val radius = half * 0.85f
                                val angleStep = (2 * Math.PI) / points
                                path.reset()
                                for (i in 0 until points) {
                                        val angle = angleStep * i - Math.PI / 2
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
                                                r,
                                                paint
                                        )
                                }
                        }
                }
                return bitmap
        }
}
