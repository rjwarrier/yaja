package com.mj.yaja.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.mj.yaja.ui.theme.JournalTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Dialog-style activity that lets the user configure
 * an individual Quick Capture widget instance (shape, corner radius, label).
 * Launched via the system reconfigure gesture (long-press → Configure widget).
 */
class QuickCaptureWidgetConfigActivity : ComponentActivity() {

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

        val currentRadius    = QuickCaptureWidgetProvider.getCornerRadius(this, appWidgetId)
        val currentShowLabel = QuickCaptureWidgetProvider.getShowLabel(this, appWidgetId)
        val currentShowIcon  = QuickCaptureWidgetProvider.getShowIcon(this, appWidgetId)
        val currentLabelText = QuickCaptureWidgetProvider.getLabelText(this, appWidgetId)
        val currentShape     = QuickCaptureWidgetProvider.getCellShape(this, appWidgetId)

        setContent {
            JournalTheme {
                QcConfigDialog(
                    initialRadius    = currentRadius,
                    initialShowLabel = currentShowLabel,
                    initialShowIcon  = currentShowIcon,
                    initialLabelText = currentLabelText,
                    initialShape     = currentShape,
                    dialogTitle      = "Yaja Quick Capture",
                    labelPlaceholder = "Capture",
                    onApply          = { newRadius, showLabel, showIcon, labelText, newShape ->
                        applyAndFinish(newRadius, showLabel, showIcon, labelText, newShape)
                    },
                    onCancel         = { finish() }
                )
            }
        }
    }

    private fun applyAndFinish(radiusDp: Int, showLabel: Boolean, showIcon: Boolean, labelText: String, shape: CellShape) {
        QuickCaptureWidgetProvider.setCornerRadius(this, appWidgetId, radiusDp)
        QuickCaptureWidgetProvider.setShowLabel(this, appWidgetId, showLabel)
        QuickCaptureWidgetProvider.setShowIcon(this, appWidgetId, showIcon)
        QuickCaptureWidgetProvider.setLabelText(this, appWidgetId, labelText)
        QuickCaptureWidgetProvider.setCellShape(this, appWidgetId, shape)

        QuickCaptureWidgetProvider.refreshAll(applicationContext)

        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QcConfigDialog(
    initialRadius: Int,
    initialShowLabel: Boolean,
    initialShowIcon: Boolean,
    initialLabelText: String,
    initialShape: CellShape,
    dialogTitle: String = "Yaja Quick Capture",
    labelPlaceholder: String = "Capture",
    onApply: (Int, Boolean, Boolean, String, CellShape) -> Unit,
    onCancel: () -> Unit
) {
    var radius        by remember { mutableFloatStateOf(initialRadius.toFloat()) }
    var showLabel     by remember { mutableStateOf(initialShowLabel) }
    var showIcon      by remember { mutableStateOf(initialShowIcon) }
    var labelText     by remember { mutableStateOf(initialLabelText) }
    var selectedShape by remember { mutableStateOf(initialShape) }

    // If label is off, icon must stay on
    if (!showLabel) showIcon = true

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(dialogTitle) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Widget shape ──────────────────────────────────────────────
                Text(
                    text = "Widget Shape",
                    style = MaterialTheme.typography.bodyMedium
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(CellShape.entries) { shape ->
                        val isSelected = shape == selectedShape
                        OutlinedCard(
                            onClick = { selectedShape = shape },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            val shapeColor = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(30.dp)) {
                                    drawShapePreview(shape, shapeColor)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // ── Show label toggle ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Show Label", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Display 'Capture' text on the widget",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = showLabel, onCheckedChange = { showLabel = it })
                }

                // ── Show icon toggle ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Show Icon",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (showLabel) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            if (!showLabel) "Icon required when label is hidden"
                            else "Display pen icon on the widget",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showIcon,
                        onCheckedChange = { showIcon = it },
                        enabled = showLabel  // Only toggleable when label is on
                    )
                }

                // ── Custom label text ───────────────────────────────────────
                if (showLabel) {
                    OutlinedTextField(
                        value = labelText,
                        onValueChange = { if (it.length <= 20) labelText = it },
                        label = { Text("Label Text") },
                        placeholder = { Text(labelPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("${labelText.length}/20")
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // ── Corner radius slider ──────────────────────────────────────
                Text(
                    text = "Corner Radius: ${radius.roundToInt()}dp",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 0f..40f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Square", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Round", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            WidgetConfigApplyButton(onClick = {
                val finalLabel = labelText.ifBlank { labelPlaceholder }
                onApply(radius.roundToInt(), showLabel, showIcon, finalLabel, selectedShape)
            })
        },
        dismissButton = {
            WidgetConfigCancelButton(onClick = onCancel)
        }
    )
}

// ── Shape preview drawing (Compose Canvas) ───────────────────────────────────

private fun DrawScope.drawShapePreview(
    shape: CellShape,
    color: androidx.compose.ui.graphics.Color
) {
    val s = min(size.width, size.height)
    val left = (size.width - s) / 2f
    val top = (size.height - s) / 2f
    val cx = left + s / 2f
    val cy = top + s / 2f
    val half = s / 2f

    when (shape) {
        CellShape.SQUARE -> {
            drawRect(color = color, topLeft = Offset(left, top), size = Size(s, s))
        }

        CellShape.ROUNDED -> {
            val r = s * 0.18f
            drawRoundRect(
                color = color, topLeft = Offset(left, top),
                size = Size(s, s), cornerRadius = CornerRadius(r, r)
            )
        }

        CellShape.CIRCLE -> {
            drawCircle(color = color, radius = half, center = Offset(cx, cy))
        }

        CellShape.CUT_CORNER -> {
            val cut = half * 0.35f
            val path = Path().apply {
                moveTo(left + cut, top)
                lineTo(left + s - cut, top)
                lineTo(left + s, top + cut)
                lineTo(left + s, top + s - cut)
                lineTo(left + s - cut, top + s)
                lineTo(left + cut, top + s)
                lineTo(left, top + s - cut)
                lineTo(left, top + cut)
                close()
            }
            drawPath(path, color = color)
        }

        CellShape.DIAMOND -> {
            val path = Path().apply {
                moveTo(cx, top)
                lineTo(left + s, cy)
                lineTo(cx, top + s)
                lineTo(left, cy)
                close()
            }
            drawPath(path, color = color)
        }

        CellShape.HEART -> {
            // Pentagon: 5-sided regular shape
            val points = 5
            val radius = half * 0.85f
            val angleStep = (2 * Math.PI) / points
            val path = Path().apply {
                for (i in 0 until points) {
                    val angle = angleStep * i - Math.PI / 2  // Start from top
                    val px = cx + radius * cos(angle).toFloat()
                    val py = cy + radius * sin(angle).toFloat()
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(path, color = color)
        }

        CellShape.SUNNY -> {
            val points = 8
            val outerR = half
            val innerR = half * 0.72f
            val step = Math.PI / points
            val path = Path().apply {
                for (i in 0 until points * 2) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val angle = step * i - Math.PI / 2
                    val px = cx + r * cos(angle).toFloat()
                    val py = cy + r * sin(angle).toFloat()
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(path, color = color)
        }

        CellShape.COOKIE_4 -> {
            val r = half * 0.58f
            val offset = half - r
            drawCircle(color, r, Offset(cx - offset, cy - offset))
            drawCircle(color, r, Offset(cx + offset, cy - offset))
            drawCircle(color, r, Offset(cx - offset, cy + offset))
            drawCircle(color, r, Offset(cx + offset, cy + offset))
        }

        CellShape.COOKIE_6 -> {
            val r = half * 0.52f
            val offset = half - r
            val points = 6
            val angleStep = (2 * Math.PI) / points
            for (i in 0 until points) {
                val angle = angleStep * i - Math.PI / 2
                drawCircle(
                    color, r,
                    Offset(
                        cx + offset * cos(angle).toFloat(),
                        cy + offset * sin(angle).toFloat()
                    )
                )
            }
        }
    }
}
