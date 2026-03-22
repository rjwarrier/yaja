package com.mj.yaja.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun YearlyConsistencyGraph(stats: List<Pair<Int, Float>>) {
        ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                elevation = CardDefaults.elevatedCardElevation(0.dp),
                shape = MaterialTheme.shapes.medium
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                                text = "Consistency (Yearly)",
                                style =
                                        MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                        ),
                                color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        // Fixed Y-axis markings: 0, 50, 100, ..., 400
                        val yMarkers = listOf(0, 50, 100, 150, 200, 250, 300, 350, 400)
                        val maxEntries = 400f

                        val primaryColor = MaterialTheme.colorScheme.primary
                        val secondaryColor = MaterialTheme.colorScheme.secondary
                        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(200.dp)
                                                .padding(start = 32.dp, end = 16.dp, bottom = 40.dp)
                        ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                        val width = size.width
                                        val height = size.height

                                        // Draw Grid Lines and Y-Axis Labels
                                        yMarkers.forEach { marker ->
                                                val y =
                                                        height -
                                                                (marker.toFloat() / maxEntries *
                                                                        height)

                                                // Horizontal Grid Line
                                                drawLine(
                                                        color = onSurfaceVariant.copy(alpha = 0.1f),
                                                        start = Offset(0f, y),
                                                        end = Offset(width, y),
                                                        strokeWidth = 1.dp.toPx()
                                                )
                                        }

                                        // Draw Data Path
                                        if (stats.size > 1) {
                                                val spacing = width / (stats.size - 1)
                                                val points =
                                                        stats.mapIndexed { index, pair ->
                                                                val x = index * spacing
                                                                val y =
                                                                        height -
                                                                                (pair.second
                                                                                        .coerceAtMost(
                                                                                                maxEntries
                                                                                        ) /
                                                                                        maxEntries *
                                                                                        height)
                                                                Offset(x, y)
                                                        }

                                                val path =
                                                        Path().apply {
                                                                moveTo(points[0].x, points[0].y)
                                                                for (i in 0 until points.size - 1) {
                                                                        val p0 = points[i]
                                                                        val p1 = points[i + 1]
                                                                        cubicTo(
                                                                                p0.x +
                                                                                        (p1.x -
                                                                                                p0.x) /
                                                                                                2f,
                                                                                p0.y,
                                                                                p0.x +
                                                                                        (p1.x -
                                                                                                p0.x) /
                                                                                                2f,
                                                                                p1.y,
                                                                                p1.x,
                                                                                p1.y
                                                                        )
                                                                }
                                                        }

                                                val fillPath =
                                                        Path().apply {
                                                                addPath(path)
                                                                lineTo(points.last().x, height)
                                                                lineTo(points.first().x, height)
                                                                close()
                                                        }

                                                drawPath(
                                                        path = fillPath,
                                                        brush =
                                                                Brush.verticalGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        primaryColor
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.2f
                                                                                                ),
                                                                                        Color.Transparent
                                                                                )
                                                                )
                                                )

                                                drawPath(
                                                        path = path,
                                                        brush =
                                                                Brush.horizontalGradient(
                                                                        listOf(
                                                                                primaryColor,
                                                                                secondaryColor
                                                                        )
                                                                ),
                                                        style =
                                                                Stroke(
                                                                        width = 3.dp.toPx(),
                                                                        cap = StrokeCap.Round,
                                                                        join = StrokeJoin.Round
                                                                )
                                                )

                                                points.forEach { point ->
                                                        drawCircle(
                                                                Color.White,
                                                                radius = 5.dp.toPx(),
                                                                center = point
                                                        )
                                                        drawCircle(
                                                                primaryColor,
                                                                radius = 4.dp.toPx(),
                                                                center = point,
                                                                style = Stroke(width = 2.dp.toPx())
                                                        )
                                                }
                                        } else if (stats.size == 1) {
                                                // Single point case
                                                val point =
                                                        Offset(
                                                                width / 2,
                                                                height -
                                                                        (stats[0].second
                                                                                .coerceAtMost(
                                                                                        maxEntries
                                                                                ) / maxEntries *
                                                                                height)
                                                        )
                                                drawCircle(
                                                        Color.White,
                                                        radius = 5.dp.toPx(),
                                                        center = point
                                                )
                                                drawCircle(
                                                        primaryColor,
                                                        radius = 4.dp.toPx(),
                                                        center = point,
                                                        style = Stroke(width = 2.dp.toPx())
                                                )
                                        }
                                }

                                // Y-Axis Markings (Labels on the left)
                                yMarkers.forEach { marker ->
                                        val yProgress = marker.toFloat() / maxEntries
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxSize().padding(end = 4.dp),
                                                contentAlignment = Alignment.TopStart
                                        ) {
                                                Text(
                                                        text = marker.toString(),
                                                        style =
                                                                MaterialTheme.typography.labelSmall
                                                                        .copy(fontSize = 10.sp),
                                                        color = onSurfaceVariant.copy(alpha = 0.5f),
                                                        modifier =
                                                                Modifier.align(
                                                                                Alignment
                                                                                        .BottomStart
                                                                        )
                                                                        .offset(
                                                                                x = (-32).dp,
                                                                                y =
                                                                                        (-200 *
                                                                                                        yProgress)
                                                                                                .dp +
                                                                                                6.dp
                                                                        )
                                                )
                                        }
                                }

                                // X-Axis Labels (Years)
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .align(Alignment.BottomCenter)
                                                        .offset(y = 40.dp),
                                        horizontalArrangement =
                                                if (stats.size > 1) Arrangement.SpaceBetween
                                                else Arrangement.Center
                                ) {
                                        stats.forEach { pair ->
                                                val yearYY = pair.first.toString().takeLast(2)
                                                Column(
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally
                                                ) {
                                                        Text(
                                                                text = "${pair.second.toInt()}",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primary,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                                text = yearYY,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant,
                                                                fontWeight = FontWeight.SemiBold
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
fun MorphingBackground(
        modifier: Modifier = Modifier,
        color: Color = Color.Magenta.copy(alpha = 0.2f)
) {
        val infiniteTransition = rememberInfiniteTransition(label = "material_morph")

        val tr by
                infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.8f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(3200, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "tr"
                )
        val tl by
                infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 0.2f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(2800, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "tl"
                )
        val br by
                infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0.9f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(3500, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "br"
                )
        val bl by
                infiniteTransition.animateFloat(
                        initialValue = 0.7f,
                        targetValue = 0.3f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(3000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "bl"
                )

        Box(
                modifier =
                        modifier
                                .graphicsLayer {
                                        shape =
                                                RoundedCornerShape(
                                                        topStartPercent = (tl * 100).toInt(),
                                                        topEndPercent = (tr * 100).toInt(),
                                                        bottomEndPercent = (br * 100).toInt(),
                                                        bottomStartPercent = (bl * 100).toInt()
                                                )
                                        clip = true
                                }
                                .background(color)
        )
}
