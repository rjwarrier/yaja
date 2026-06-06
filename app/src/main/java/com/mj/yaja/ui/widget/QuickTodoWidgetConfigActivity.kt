package com.mj.yaja.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mj.yaja.ui.theme.JournalTheme

class QuickTodoWidgetConfigActivity : ComponentActivity() {

        private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)

                appWidgetId = intent?.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID
                ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                        finish()
                        return
                }

                setResult(RESULT_CANCELED)

                val currentRadius = QuickTodoWidgetProvider.getCornerRadius(this, appWidgetId)
                val currentShowLabel = QuickTodoWidgetProvider.getShowLabel(this, appWidgetId)
                val currentShowIcon = QuickTodoWidgetProvider.getShowIcon(this, appWidgetId)
                val currentLabelText = QuickTodoWidgetProvider.getLabelText(this, appWidgetId)
                val currentShape = QuickTodoWidgetProvider.getCellShape(this, appWidgetId)

                setContent {
                        JournalTheme {
                                QcConfigDialog(
                                        initialRadius = currentRadius,
                                        initialShowLabel = currentShowLabel,
                                        initialShowIcon = currentShowIcon,
                                        initialLabelText = currentLabelText,
                                        initialShape = currentShape,
                                        dialogTitle = "Yaja Quick Todo",
                                        labelPlaceholder = "Todo",
                                        onApply = { newRadius, showLabel, showIcon, labelText, newShape ->
                                                applyAndFinish(newRadius, showLabel, showIcon, labelText, newShape)
                                        },
                                        onCancel = { finish() }
                                )
                        }
                }
        }

        private fun applyAndFinish(
                radiusDp: Int,
                showLabel: Boolean,
                showIcon: Boolean,
                labelText: String,
                shape: CellShape
        ) {
                QuickTodoWidgetProvider.saveOptions(
                        context = this,
                        appWidgetId = appWidgetId,
                        radiusDp = radiusDp,
                        showLabel = showLabel,
                        showIcon = showIcon,
                        labelText = labelText,
                        shape = shape
                )

                QuickTodoWidgetProvider.refreshAll(applicationContext)

                val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultIntent)
                finish()
        }
}
