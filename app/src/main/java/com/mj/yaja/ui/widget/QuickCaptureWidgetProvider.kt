package com.mj.yaja.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.mj.yaja.R
import com.mj.yaja.ui.screens.QuickCaptureActivity

class QuickCaptureWidgetProvider : AppWidgetProvider() {

        companion object {
                const val ACTION_WIDGET_STATUS_CHANGED = "com.mj.yaja.ACTION_WIDGET_STATUS_CHANGED"
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
                val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

                // Read preferences
                val prefs = context.getSharedPreferences("journal_settings", Context.MODE_PRIVATE)
                val cornerRadiusDp = prefs.getInt("widget_corner_radius", 24)
                val showLabelPref = prefs.getBoolean("show_widget_label", true)

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

                // Create a high-quality bitmap background matching actual size
                val bgColor = ContextCompat.getColor(context, R.color.ic_launcher_background)
                val bitmap = createRoundedBitmap(widthPx, heightPx, radiusPx, bgColor)
                views.setImageViewBitmap(R.id.widget_background, bitmap)

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

        private fun createRoundedBitmap(
                width: Int,
                height: Int,
                radiusPx: Float,
                color: Int
        ): Bitmap {
                val bitmap =
                        Bitmap.createBitmap(
                                width.coerceAtLeast(1),
                                height.coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                        )
                val canvas = Canvas(bitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                // Cap radius to half of shortest side to prevent strange artifacts
                val clampedRadius = radiusPx.coerceAtMost(minOf(width, height) / 2f)
                canvas.drawRoundRect(rect, clampedRadius, clampedRadius, paint)
                return bitmap
        }
}
