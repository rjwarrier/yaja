package com.mj.yaja.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.floatSpring
import com.mj.yaja.data.AnimationPreference
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.data.KeywordDefinition
import androidx.compose.ui.unit.sp
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class CalendarGraphPoint(
        val label: String,
        val shortLabel: String,
        val value: Float,
        val highlighted: Boolean
)

@Composable
fun CalendarConsistencySection(
        monthlyStats: List<Pair<YearMonth, Int>>,
        yearlyStats: List<Pair<Int, Float>>,
        graphMode: CalendarGraphMode,
        graphFrequency: CalendarGraphFrequency,
        onGraphModeChange: (CalendarGraphMode) -> Unit,
        onGraphFrequencyChange: (CalendarGraphFrequency) -> Unit
) {
        val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM", Locale.getDefault()) }
        val monthSeries =
                remember(monthlyStats) {
                        monthlyStats.mapIndexed { index, (month, value) ->
                                CalendarGraphPoint(
                                        label = month.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())),
                                        shortLabel = month.format(monthFormatter),
                                        value = value.toFloat(),
                                        highlighted = index == monthlyStats.lastIndex
                                )
                        }
                }
        val yearSeries =
                remember(yearlyStats) {
                        yearlyStats.takeLast(10).mapIndexed { index, (year, value) ->
                                CalendarGraphPoint(
                                        label = year.toString(),
                                        shortLabel = year.toString().takeLast(2),
                                        value = value,
                                        highlighted = index == yearlyStats.takeLast(10).lastIndex
                                )
                        }
                }
        val points = if (graphFrequency == CalendarGraphFrequency.YEAR) yearSeries else monthSeries
        if (points.isEmpty()) return

        val currentPoint = points.last()
        val previousPoint = points.getOrNull(points.lastIndex - 1)
        val deltaPercent =
                remember(currentPoint, previousPoint) {
                        val previousValue = previousPoint?.value ?: 0f
                        if (previousValue <= 0f) {
                                null
                        } else {
                                (((currentPoint.value - previousValue) / previousValue) * 100f).toInt()
                        }
                }
        val titleSuffix =
                if (graphFrequency == CalendarGraphFrequency.YEAR) {
                        currentPoint.label
                } else {
                        stringResource(R.string.calendar_consistency_last_12_months)
                }
        val consistencyTitle = stringResource(R.string.calendar_consistency_title_format, titleSuffix)
        val comparisonText =
                if (deltaPercent != null && previousPoint != null) {
                        stringResource(
                                R.string.calendar_consistency_delta_format,
                                deltaPercent,
                                previousPoint.label.takeLast(2)
                        )
                } else if (previousPoint != null) {
                        stringResource(
                                R.string.calendar_consistency_previous_period_format,
                                previousPoint.label.takeLast(2)
                        )
                } else {
                        stringResource(R.string.calendar_consistency_first_period)
                }

        ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(28.dp)
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val stackHeader = maxWidth < 380.dp
                                if (stackHeader) {
                                        Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text(
                                                                text = consistencyTitle,
                                                                style = MaterialTheme.typography.labelLarge,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontWeight = FontWeight.SemiBold
                                                        )
                                                        Row(
                                                                verticalAlignment = Alignment.Bottom,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                                Text(
                                                                        text = currentPoint.value.toInt().toString(),
                                                                        style = MaterialTheme.typography.headlineLarge,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Text(
                                                                        text = comparisonText,
                                                                        style = MaterialTheme.typography.titleSmall,
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        modifier = Modifier.padding(bottom = 4.dp)
                                                                )
                                                        }
                                                }
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                                                        verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                        CalendarGraphSwitches(graphMode, graphFrequency, onGraphModeChange, onGraphFrequencyChange)
                                                }
                                        }
                                } else {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                        ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text(
                                                                text = consistencyTitle,
                                                                style = MaterialTheme.typography.labelLarge,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontWeight = FontWeight.SemiBold
                                                        )
                                                        Row(
                                                                verticalAlignment = Alignment.Bottom,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                                Text(
                                                                        text = currentPoint.value.toInt().toString(),
                                                                        style = MaterialTheme.typography.headlineLarge,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Text(
                                                                        text = comparisonText,
                                                                        style = MaterialTheme.typography.titleSmall,
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        modifier = Modifier.padding(bottom = 4.dp)
                                                                )
                                                        }
                                                }
                                                Row(
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                        CalendarGraphSwitches(graphMode, graphFrequency, onGraphModeChange, onGraphFrequencyChange)
                                                }
                                        }
                                }
                        }

                        if (graphMode == CalendarGraphMode.BAR) {
                                CalendarBarConsistencyGraph(points = points)
                        } else {
                                CalendarLineConsistencyGraph(points = points)
                        }
                }
        }
}

@Composable
private fun CalendarGraphSwitches(
        graphMode: CalendarGraphMode,
        graphFrequency: CalendarGraphFrequency,
        onGraphModeChange: (CalendarGraphMode) -> Unit,
        onGraphFrequencyChange: (CalendarGraphFrequency) -> Unit
) {
        Row(
                modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Box(
                        modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                ) {
                        CalendarTogglePill(
                                selected = true,
                                icon =
                                        if (graphMode == CalendarGraphMode.BAR) {
                                                Icons.AutoMirrored.Rounded.ShowChart
                                        } else {
                                                Icons.Rounded.BarChart
                                        },
                                text = null,
                                modifier = Modifier.fillMaxSize(),
                                onClick = {
                                        onGraphModeChange(
                                                if (graphMode == CalendarGraphMode.BAR) {
                                                        CalendarGraphMode.LINE
                                                } else {
                                                        CalendarGraphMode.BAR
                                                }
                                        )
                                }
                        )
                }
                Box(
                        modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                ) {
                        CalendarTogglePill(
                                selected = true,
                                text =
                                        if (graphFrequency == CalendarGraphFrequency.YEAR) {
                                                stringResource(R.string.calendar_graph_frequency_month)
                                        } else {
                                                stringResource(R.string.calendar_graph_frequency_year)
                                        },
                                modifier = Modifier.fillMaxSize(),
                                onClick = {
                                        onGraphFrequencyChange(
                                                if (graphFrequency == CalendarGraphFrequency.YEAR) {
                                                        CalendarGraphFrequency.MONTH
                                                } else {
                                                        CalendarGraphFrequency.YEAR
                                                }
                                        )
                                }
                        )
                }
        }
}

@Composable
private fun CalendarTogglePill(
        selected: Boolean,
        icon: ImageVector? = null,
        text: String? = null,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
) {
        val containerColor by animateColorAsState(
                targetValue =
                        if (selected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "calendar_toggle_container"
        )
        val contentColor by animateColorAsState(
                targetValue =
                        if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "calendar_toggle_content"
        )
        Surface(
                onClick = onClick,
                shape = RoundedCornerShape(18.dp),
                color = containerColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = modifier.widthIn(min = 54.dp).heightIn(min = 24.dp)
        ) {
                Row(
                        modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                ) {
                        if (icon != null) {
                                Icon(
                                        imageVector = icon,
                                        contentDescription = text,
                                        tint = contentColor,
                                        modifier = Modifier.size(14.dp)
                                )
                        }
                        if (text != null) {
                                Text(
                                        text = text,
                                        color = contentColor,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                )
                        }
                }
        }
}

@Composable
private fun CalendarBarConsistencyGraph(points: List<CalendarGraphPoint>) {
        val maxValue = points.maxOf { it.value }.coerceAtLeast(1f)
        val gridLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                Box(modifier = Modifier.fillMaxWidth().height(278.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                                val bottomOfBars = size.height
                                val topOfBars = size.height - 260.dp.toPx()
                                val barAreaHeight = bottomOfBars - topOfBars
                                val gridLineCount = 4
                                for (i in 0..gridLineCount) {
                                        val y = topOfBars + barAreaHeight * (i.toFloat() / gridLineCount)
                                        drawLine(
                                                color = gridLineColor,
                                                start = Offset(0f, y),
                                                end = Offset(size.width, y),
                                                strokeWidth = 1.dp.toPx()
                                        )
                                }
                        }

                        Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Bottom
                        ) {
                                points.forEach { point ->
                                        val ratio = (point.value / maxValue).coerceIn(0f, 1f)
                                        val barHeight = (ratio * 260f).dp.coerceAtLeast(6.dp)
                                        Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Bottom
                                        ) {
                                                if (point.highlighted) {
                                                        Text(
                                                                text = point.value.toInt().toString(),
                                                                style = MaterialTheme.typography.labelLarge,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(bottom = 4.dp)
                                                        )
                                                } else {
                                                        Spacer(modifier = Modifier.height(18.dp))
                                                }
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .height(barHeight)
                                                                        .background(
                                                                                color =
                                                                                        if (point.highlighted) {
                                                                                                MaterialTheme.colorScheme.primary
                                                                                        } else {
                                                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                                                                        },
                                                                                shape = RoundedCornerShape(8.dp)
                                                                        )
                                                )
                                        }
                                }
                        }
                }
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        points.forEach { point ->
                                Text(
                                        text = point.shortLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color =
                                                if (point.highlighted) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (point.highlighted) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                        }
                }
        }
}

@Composable
private fun CalendarLineConsistencyGraph(points: List<CalendarGraphPoint>) {
        val maxValue = points.maxOf { it.value }.coerceAtLeast(1f)
        val lineColor = MaterialTheme.colorScheme.primary
        val fillTopColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        val fadedLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
        val transparentColor = Color.Transparent
        val gridLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                Box(
                        modifier = Modifier.fillMaxWidth().height(278.dp)
                ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                                val spacing = if (points.size > 1) size.width / (points.size - 1) else 0f
                                val bottomOfChart = size.height
                                val topOfChart = size.height - 260.dp.toPx()
                                val chartHeight = bottomOfChart - topOfChart

                                // Subtle horizontal grid lines in the background
                                val gridLineCount = 4
                                for (i in 0..gridLineCount) {
                                        val y = topOfChart + chartHeight * (i.toFloat() / gridLineCount)
                                        drawLine(
                                                color = gridLineColor,
                                                start = Offset(0f, y),
                                                end = Offset(size.width, y),
                                                strokeWidth = 1.dp.toPx()
                                        )
                                }

                                // Subtle vertical grid lines in the background
                                points.forEachIndexed { index, _ ->
                                        val x = spacing * index
                                        drawLine(
                                                color = gridLineColor,
                                                start = Offset(x, topOfChart),
                                                end = Offset(x, bottomOfChart),
                                                strokeWidth = 1.dp.toPx()
                                        )
                                }

                                val pointOffsets =
                                        points.mapIndexed { index, point ->
                                                Offset(
                                                        x = spacing * index,
                                                        y = bottomOfChart - ((point.value / maxValue) * chartHeight)
                                                )
                                        }
                                val linePath = Path().apply {
                                        moveTo(pointOffsets.first().x, pointOffsets.first().y)
                                        for (i in 1 until pointOffsets.size) {
                                                lineTo(pointOffsets[i].x, pointOffsets[i].y)
                                        }
                                }
                                val fillPath = Path().apply {
                                        addPath(linePath)
                                        lineTo(pointOffsets.last().x, bottomOfChart)
                                        lineTo(pointOffsets.first().x, bottomOfChart)
                                        close()
                                }
                                drawPath(
                                        path = fillPath,
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        fillTopColor,
                                                                        transparentColor
                                                                )
                                                )
                                )
                                drawPath(
                                        path = linePath,
                                        color = lineColor,
                                        style =
                                                Stroke(
                                                        width = 4.dp.toPx(),
                                                        cap = StrokeCap.Round,
                                                        join = StrokeJoin.Round
                                                )
                                )
                                pointOffsets.forEachIndexed { index, point ->
                                        drawCircle(
                                                color =
                                                        if (points[index].highlighted) lineColor
                                                        else fadedLineColor,
                                                radius = if (points[index].highlighted) 5.dp.toPx() else 4.dp.toPx(),
                                                center = point
                                        )
                                }
                        }
                }
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        points.forEach { point ->
                                Text(
                                        text = point.shortLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color =
                                                if (point.highlighted) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (point.highlighted) FontWeight.Bold else FontWeight.Medium
                                )
                        }
                }
        }
}


@Composable
fun YearlyConsistencyGraph(stats: List<Pair<Int, Float>>) {
        ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                elevation = CardDefaults.elevatedCardElevation(0.dp),
                shape = MaterialTheme.shapes.large
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp)) {
                        Text(
                                text = stringResource(R.string.calendar_yearly_consistency_title),
                                style =
                                        MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold
                                        ),
                                color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(28.dp))

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
        val animationPreference = LocalAnimationPreference.current
        val tl: Float
        val tr: Float
        val bl: Float
        val br: Float

        if (animationPreference == AnimationPreference.OFF) {
                tl = 0.5f
                tr = 0.5f
                bl = 0.5f
                br = 0.5f
        } else {
                val infiniteTransition = rememberInfiniteTransition(label = "material_morph")
                val durationMultiplier = if (animationPreference == AnimationPreference.REDUCED) 2f else 1f
                val tlMin = if (animationPreference == AnimationPreference.REDUCED) 0.4f else 0.8f
                val tlMax = if (animationPreference == AnimationPreference.REDUCED) 0.6f else 0.2f

                val trMin = if (animationPreference == AnimationPreference.REDUCED) 0.4f else 0.2f
                val trMax = if (animationPreference == AnimationPreference.REDUCED) 0.6f else 0.8f

                val blMin = if (animationPreference == AnimationPreference.REDUCED) 0.4f else 0.7f
                val blMax = if (animationPreference == AnimationPreference.REDUCED) 0.6f else 0.3f

                val brMin = if (animationPreference == AnimationPreference.REDUCED) 0.4f else 0.4f
                val brMax = if (animationPreference == AnimationPreference.REDUCED) 0.6f else 0.9f

                tr = infiniteTransition.animateFloat(
                        initialValue = trMin,
                        targetValue = trMax,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween((3200 * durationMultiplier).toInt(), easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "tr"
                ).value

                tl = infiniteTransition.animateFloat(
                        initialValue = tlMin,
                        targetValue = tlMax,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween((2800 * durationMultiplier).toInt(), easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "tl"
                ).value

                br = infiniteTransition.animateFloat(
                        initialValue = brMin,
                        targetValue = brMax,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween((3500 * durationMultiplier).toInt(), easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "br"
                ).value

                bl = infiniteTransition.animateFloat(
                        initialValue = blMin,
                        targetValue = blMax,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween((3000 * durationMultiplier).toInt(), easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "bl"
                ).value
        }

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeywordStatsSection(
        topPeople: List<Pair<KeywordDefinition, Int>>,
        topPlaces: List<Pair<KeywordDefinition, Int>>,
        peopleKeywordCount: Int,
        placeKeywordCount: Int,
        isSettling: Boolean = false
) {
        val showPeople = peopleKeywordCount >= 3 && topPeople.isNotEmpty()
        val showPlaces = placeKeywordCount >= 3 && topPlaces.isNotEmpty()

        ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(0.dp),
                shape = MaterialTheme.shapes.large
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                        // Header
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                ) {
                                        Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                        Icons.Rounded.People,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(20.dp)
                                                )
                                        }
                                }
                                Text(
                                        text = stringResource(R.string.calendar_keywords_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                        }

                        if (isSettling) {
                                Text(
                                        text = stringResource(R.string.calendar_keywords_syncing),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                )
                        }

                        if (!showPeople && !showPlaces) {
                                Text(
                                        text = stringResource(R.string.calendar_keywords_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }

                        if (showPeople) {
                                KeywordRankGroup(
                                        label = stringResource(R.string.calendar_keywords_people),
                                        icon = Icons.Rounded.Person,
                                        items = topPeople.take(3),
                                        accentColor = MaterialTheme.colorScheme.primary,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        isSettling = isSettling
                                )
                        }

                        if (showPeople && showPlaces) {
                                HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                        }

                        if (showPlaces) {
                                KeywordRankGroup(
                                        label = stringResource(R.string.calendar_keywords_places),
                                        icon = Icons.Rounded.Place,
                                        items = topPlaces.take(3),
                                        accentColor = MaterialTheme.colorScheme.tertiary,
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        onContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        isSettling = isSettling
                                )
                        }
                }
        }
}

@Composable
private fun KeywordRankGroup(
        label: String,
        icon: ImageVector,
        items: List<Pair<KeywordDefinition, Int>>,
        accentColor: Color,
        containerColor: Color,
        onContainerColor: Color,
        isSettling: Boolean = false
) {
        val maxCount = items.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
        val contentAlpha = if (isSettling) 0.65f else 1f

        Column(
                modifier = Modifier.graphicsLayer { alpha = contentAlpha },
                verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
                // Section label
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                        Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = containerColor.copy(alpha = 0.7f)
                        ) {
                                Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = onContainerColor,
                                        modifier = Modifier.padding(4.dp).size(13.dp)
                                )
                        }
                        Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor
                        )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items.forEachIndexed { index, (keyword, count) ->
                                RankedKeywordRow(
                                        index = index,
                                        keyword = keyword,
                                        count = count,
                                        fill = count.toFloat() / maxCount.toFloat(),
                                        accentColor = accentColor,
                                        containerColor = containerColor
                                )
                        }
                }
        }
}

@Composable
private fun RankedKeywordRow(
        index: Int,
        keyword: KeywordDefinition,
        count: Int,
        fill: Float,
        accentColor: Color,
        containerColor: Color
) {
        val preference = LocalAnimationPreference.current
        val animatedFill = if (preference == AnimationPreference.OFF) {
                fill.coerceIn(0f, 1f)
        } else {
                animateFloatAsState(
                        targetValue = fill.coerceIn(0f, 1f),
                        animationSpec = preference.floatTween(
                                durationMillis = 700,
                                delayMillis = index * 80
                        ),
                        label = "bar_fill"
                ).value
        }
        val rankAlpha = when (index) { 0 -> 1.0f; 1 -> 0.75f; else -> 0.55f }

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        // Rank badge
                        Surface(
                                color = accentColor.copy(alpha = 0.13f),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.size(28.dp)
                        ) {
                                Box(contentAlignment = Alignment.Center) {
                                        Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor.copy(alpha = rankAlpha)
                                        )
                                }
                        }

                        // Name + relation
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = keyword.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = rankAlpha)
                                )
                                if (keyword.relation.isNotBlank()) {
                                        Text(
                                                text = keyword.relation,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accentColor.copy(alpha = 0.65f)
                                        )
                                }
                        }

                        // Count badge
                        Surface(
                                color = containerColor.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(999.dp)
                        ) {
                                Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor.copy(alpha = rankAlpha),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                        }
                }

                // Animated gradient bar
                Box(
                        modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(999.dp)
                                )
                ) {
                        Box(
                                modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animatedFill)
                                        .background(
                                                Brush.horizontalGradient(
                                                        colors = listOf(
                                                                accentColor,
                                                                accentColor.copy(alpha = 0.45f)
                                                        )
                                                ),
                                                RoundedCornerShape(999.dp)
                                        )
                        )
                }
        }
}
