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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.theme.JournalTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A small dialog-style activity that lets the user configure
 * an individual heatmap widget instance (corner radius, margin, cell shape).
 * Launched via the system reconfigure gesture (long-press → Configure widget).
 */
class HeatmapWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If no valid widget ID, finish immediately
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Set result to CANCELED in case the user backs out
        setResult(RESULT_CANCELED)

        val currentRadius    = HeatmapWidgetProvider.getCornerRadius(this, appWidgetId)
        val currentMargin    = HeatmapWidgetProvider.getMargin(this, appWidgetId)
        val currentShape     = HeatmapWidgetProvider.getCellShape(this, appWidgetId)
        val currentTapAction = HeatmapWidgetProvider.getTapAction(this, appWidgetId)

        setContent {
            JournalTheme {
                ConfigDialog(
                    initialRadius    = currentRadius,
                    initialMargin    = currentMargin,
                    initialShape     = currentShape,
                    initialTapAction = currentTapAction,
                    onApply = { newRadius, newMargin, newShape, newTapAction ->
                        applyAndFinish(newRadius, newMargin, newShape, newTapAction)
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun applyAndFinish(radiusDp: Int, marginDp: Int, shape: CellShape, tapAction: HeatmapTapAction) {
        HeatmapWidgetProvider.setCornerRadius(this, appWidgetId, radiusDp)
        HeatmapWidgetProvider.setMargin(this, appWidgetId, marginDp)
        HeatmapWidgetProvider.setCellShape(this, appWidgetId, shape)
        HeatmapWidgetProvider.setTapAction(this, appWidgetId, tapAction)

        // Direct RemoteViews update — no Glance overhead, near-instant
        val appWidgetManager = AppWidgetManager.getInstance(this)
        HeatmapWidgetProvider().onUpdate(this, appWidgetManager, intArrayOf(appWidgetId))

        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigDialog(
    initialRadius: Int,
    initialMargin: Int,
    initialShape: CellShape,
    initialTapAction: HeatmapTapAction,
    onApply: (Int, Int, CellShape, HeatmapTapAction) -> Unit,
    onCancel: () -> Unit
) {
    var radius by remember { mutableFloatStateOf(initialRadius.toFloat()) }
    var margin by remember { mutableFloatStateOf(initialMargin.toFloat()) }
    var selectedShape by remember { mutableStateOf(initialShape) }
    var tapAction by remember { mutableStateOf(initialTapAction) }
    var tapDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.widget_heatmap_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Cell shape ──────────────────────────────────────────────
                Text(
                    text = stringResource(R.string.widget_cell_shape),
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

                // ── Corner radius ─────────────────────────────────────────────
                Text(
                    text = stringResource(R.string.widget_corner_radius, radius.roundToInt()),
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
                    Text(stringResource(R.string.widget_square), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.widget_round), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // ── Inner margin ──────────────────────────────────────────────
                Text(
                    text = stringResource(R.string.widget_inner_margin, margin.roundToInt()),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.widget_inner_margin_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = margin,
                    onValueChange = { margin = it },
                    valueRange = 0f..24f,
                    steps = 11,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.widget_none), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.widget_max), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // ── Tap action ──────────────────────────────────────────────
                Text(
                    text = stringResource(R.string.widget_on_tap),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.widget_on_tap_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { tapDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                tapActionLabel(tapAction),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = tapDropdownExpanded,
                        onDismissRequest = { tapDropdownExpanded = false }
                    ) {
                        HeatmapTapAction.entries.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(tapActionLabel(action)) },
                                onClick = {
                                    tapAction = action
                                    tapDropdownExpanded = false
                                },
                                leadingIcon = if (action == tapAction) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            WidgetConfigApplyButton(onClick = {
                onApply(radius.roundToInt(), margin.roundToInt(), selectedShape, tapAction)
            })
        },
        dismissButton = {
            WidgetConfigCancelButton(onClick = onCancel)
        }
    )
}

@Composable
private fun tapActionLabel(action: HeatmapTapAction): String = when (action) {
    HeatmapTapAction.LAUNCH_APP -> stringResource(R.string.tap_action_launch_app)
    HeatmapTapAction.QUICK_CAPTURE -> stringResource(R.string.tap_action_quick_capture)
    HeatmapTapAction.DO_NOTHING -> stringResource(R.string.tap_action_do_nothing)
}

// ── Shape preview drawing (Compose Canvas) ───────────────────────────────────

/**
 * Draws a preview of the given [CellShape] filling the current [DrawScope].
 * Mirrors the drawing logic in [HeatmapWidget.drawCell] but uses Compose
 * [DrawScope] APIs instead of Android [android.graphics.Canvas].
 */
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
                color = color,
                topLeft = Offset(left, top),
                size = Size(s, s),
                cornerRadius = CornerRadius(r, r)
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
            // Four overlapping circles at diagonal positions
            val r = half * 0.58f
            val offset = half - r
            drawCircle(color, r, Offset(cx - offset, cy - offset))
            drawCircle(color, r, Offset(cx + offset, cy - offset))
            drawCircle(color, r, Offset(cx - offset, cy + offset))
            drawCircle(color, r, Offset(cx + offset, cy + offset))
        }

        CellShape.COOKIE_6 -> {
            // Six overlapping circles evenly spaced
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
