package com.mj.yaja.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import com.mj.yaja.ui.design.scaledDuration
import com.mj.yaja.ui.design.scaledDistance
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.data.CalendarDensityPreference
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun CalendarWeekdayHeader(
        firstDayOfWeekPref: java.time.DayOfWeek,
        density: CalendarDensityPreference
) {
        val daysOfWeek =
                if (firstDayOfWeekPref == java.time.DayOfWeek.SUNDAY) {
                        listOf(
                                stringResource(R.string.calendar_weekday_sun_short),
                                stringResource(R.string.calendar_weekday_mon_short),
                                stringResource(R.string.calendar_weekday_tue_short),
                                stringResource(R.string.calendar_weekday_wed_short),
                                stringResource(R.string.calendar_weekday_thu_short),
                                stringResource(R.string.calendar_weekday_fri_short),
                                stringResource(R.string.calendar_weekday_sat_short)
                        )
                } else {
                        listOf(
                                stringResource(R.string.calendar_weekday_mon_short),
                                stringResource(R.string.calendar_weekday_tue_short),
                                stringResource(R.string.calendar_weekday_wed_short),
                                stringResource(R.string.calendar_weekday_thu_short),
                                stringResource(R.string.calendar_weekday_fri_short),
                                stringResource(R.string.calendar_weekday_sat_short),
                                stringResource(R.string.calendar_weekday_sun_short)
                        )
                }

        Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                        daysOfWeek.forEach { day ->
                                Box(
                                        modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = if (density == CalendarDensityPreference.COMFORTABLE) 3.dp else 2.dp)
                                                .background(
                                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                                        shape = RoundedCornerShape(if (density == CalendarDensityPreference.DENSE) 8.dp else 12.dp)
                                                )
                                                .padding(vertical = if (density == CalendarDensityPreference.COMFORTABLE) 6.dp else 4.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(
                                                text = day,
                                                style =
                                                        MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold
                                                        ),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                                textAlign = TextAlign.Center
                                        )
                                }
                        }
                }

                Spacer(
                        modifier = Modifier.height(
                                when (density) {
                                        CalendarDensityPreference.COMFORTABLE -> 16.dp
                                        CalendarDensityPreference.COMPACT -> 10.dp
                                        CalendarDensityPreference.DENSE -> 6.dp
                                }
                        )
                )
        }
}

@Composable
fun CalendarViewModeBody(
        viewMode: CalendarViewMode,
        currentMonth: YearMonth,
        currentYearWindowStart: Int,
        selectedDate: LocalDate,
        datesWithEntries: Set<LocalDate>,
        allowFutureEntries: Boolean,
        favoritedDates: Set<String>,
        revisitTargetDates: Set<LocalDate>,
        firstDayOfWeekPref: java.time.DayOfWeek,
        density: CalendarDensityPreference,
        onSelectDate: (LocalDate) -> Unit,
        onSelectFutureDateWithEntries: (LocalDate) -> Unit,
        onFutureDateRequest: (LocalDate) -> Unit,
        onEnableFutureDateRequest: (LocalDate) -> Unit,
        onMonthPicked: (Int) -> Unit,
        onYearPicked: (Int) -> Unit
) {
        val preference = LocalAnimationPreference.current
        AnimatedContent(
                targetState = viewMode,
                transitionSpec = {
                        val duration = preference.scaledDuration(300)
                        val slideOffsetIn = preference.scaledDistance(1f)
                        val slideOffsetOut = preference.scaledDistance(1f)

                        val enterSpec = if (preference == AnimationPreference.OFF) {
                                EnterTransition.None
                        } else {
                                slideInHorizontally(animationSpec = tween(duration)) { width -> (width / 3 * slideOffsetIn).toInt() } +
                                        fadeIn(animationSpec = tween(duration))
                        }

                        val exitSpec = if (preference == AnimationPreference.OFF) {
                                ExitTransition.None
                        } else {
                                slideOutHorizontally(animationSpec = tween(duration)) { width -> (-width / 3 * slideOffsetOut).toInt() } +
                                        fadeOut(animationSpec = tween(duration))
                        }

                        enterSpec.togetherWith(exitSpec)
                },
                modifier = Modifier.fillMaxWidth(),
                label = "ViewModeSwitch"
        ) { currentViewMode ->
                when (currentViewMode) {
                        CalendarViewMode.DAYS ->
                                CalendarDaysGrid(
                                        currentMonth = currentMonth,
                                        selectedDate = selectedDate,
                                        datesWithEntries = datesWithEntries,
                                        allowFutureEntries = allowFutureEntries,
                                        favoritedDates = favoritedDates,
                                        revisitTargetDates = revisitTargetDates,
                                        firstDayOfWeekPref = firstDayOfWeekPref,
                                        density = density,
                                        onSelectDate = onSelectDate,
                                        onSelectFutureDateWithEntries =
                                                onSelectFutureDateWithEntries,
                                        onFutureDateRequest = onFutureDateRequest,
                                        onEnableFutureDateRequest =
                                                onEnableFutureDateRequest
                                )

                        CalendarViewMode.MONTHS ->
                                CalendarMonthsGrid(
                                        currentMonth = currentMonth,
                                        onMonthPicked = onMonthPicked
                                )

                        CalendarViewMode.YEARS ->
                                CalendarYearsGrid(
                                        currentYearWindowStart = currentYearWindowStart,
                                        onYearPicked = onYearPicked
                                )
                }
        }
}

@Composable
private fun CalendarDaysGrid(
        currentMonth: YearMonth,
        selectedDate: LocalDate,
        datesWithEntries: Set<LocalDate>,
        allowFutureEntries: Boolean,
        favoritedDates: Set<String>,
        revisitTargetDates: Set<LocalDate>,
        firstDayOfWeekPref: java.time.DayOfWeek,
        density: CalendarDensityPreference,
        onSelectDate: (LocalDate) -> Unit,
        onSelectFutureDateWithEntries: (LocalDate) -> Unit,
        onFutureDateRequest: (LocalDate) -> Unit,
        onEnableFutureDateRequest: (LocalDate) -> Unit
) {
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfMonthVal = currentMonth.atDay(1).dayOfWeek.value

        val prePadding =
                if (firstDayOfWeekPref == java.time.DayOfWeek.SUNDAY) {
                        firstDayOfMonthVal % 7
                } else {
                        firstDayOfMonthVal - 1
                }

        val totalCells = prePadding + daysInMonth
        val totalRows = ceil(totalCells / 7.0).toInt()

        Column(modifier = Modifier.fillMaxWidth()) {
                for (row in 0 until totalRows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0 until 7) {
                                        val index = row * 7 + col
                                        Box(modifier = Modifier.weight(1f)) {
                                                if (
                                                        index >= prePadding &&
                                                                index < prePadding + daysInMonth
                                                ) {
                                                        val day = index - prePadding + 1
                                                        val date = currentMonth.atDay(day)
                                                        val isSelected = date == selectedDate
                                                        val isToday = date == LocalDate.now()
                                                        val hasEntries =
                                                                datesWithEntries.contains(date)
                                                        val isFuture = date.isAfter(LocalDate.now())
                                                        val isFavorited =
                                                                favoritedDates.contains(
                                                                        date.toString()
                                                                )
                                                        val hasFollowUp =
                                                                revisitTargetDates.contains(date)

                                                        CalendarDay(
                                                                day = day,
                                                                isSelected = isSelected,
                                                                isToday = isToday,
                                                                hasEntries = hasEntries,
                                                                hasFollowUp = hasFollowUp,
                                                                isFuture = isFuture,
                                                                isDimmed =
                                                                        isFuture &&
                                                                                !allowFutureEntries,
                                                                isFavorited = isFavorited,
                                                                density = density,
                                                                onClick = {
                                                                        if (isFuture) {
                                                                                if (hasEntries) {
                                                                                        onSelectFutureDateWithEntries(
                                                                                                date
                                                                                        )
                                                                                } else if (
                                                                                        allowFutureEntries
                                                                                ) {
                                                                                        onFutureDateRequest(
                                                                                                date
                                                                                        )
                                                                                } else {
                                                                                        onEnableFutureDateRequest(
                                                                                                date
                                                                                        )
                                                                                }
                                                                        } else {
                                                                                onSelectDate(date)
                                                                        }
                                                                }
                                                        )
                                                } else {
                                                        Spacer(
                                                                modifier = Modifier.aspectRatio(
                                                                        when (density) {
                                                                                CalendarDensityPreference.COMFORTABLE -> 1.15f
                                                                                CalendarDensityPreference.COMPACT -> 1.28f
                                                                                CalendarDensityPreference.DENSE -> 1.42f
                                                                        }
                                                                )
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun CalendarMonthsGrid(
        currentMonth: YearMonth,
        onMonthPicked: (Int) -> Unit
) {
        Column(modifier = Modifier.fillMaxWidth()) {
                val months = java.time.Month.values()
                for (row in 0 until 4) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                for (col in 0 until 3) {
                                        val month = months[row * 3 + col]
                                        Box(
                                                modifier =
                                                        Modifier.weight(1f)
                                                                .aspectRatio(1.8f)
                                                                .background(
                                                                        if (
                                                                                currentMonth.month ==
                                                                                        month &&
                                                                                        currentMonth
                                                                                                .year ==
                                                                                                LocalDate
                                                                                                        .now()
                                                                                                        .year
                                                                        ) {
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer
                                                                        } else {
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceContainerHighest
                                                                        },
                                                                        shape = CircleShape
                                                                )
                                                                .clickable {
                                                                        onMonthPicked(month.value)
                                                                },
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        text =
                                                                month.getDisplayName(
                                                                        TextStyle.SHORT,
                                                                        Locale.getDefault()
                                                                ),
                                                        style =
                                                                MaterialTheme.typography.labelLarge
                                                                        .copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                        color =
                                                                if (
                                                                        currentMonth.month ==
                                                                                        month &&
                                                                                currentMonth.year ==
                                                                                        LocalDate
                                                                                                .now()
                                                                                                .year
                                                                ) {
                                                                        MaterialTheme.colorScheme
                                                                                .onPrimaryContainer
                                                                } else {
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                }
                                                )
                                        }
                                }
                        }
                        if (row < 3) {
                                Spacer(modifier = Modifier.height(8.dp))
                        }
                }
        }
}

@Composable
private fun CalendarYearsGrid(
        currentYearWindowStart: Int,
        onYearPicked: (Int) -> Unit
) {
        Column(modifier = Modifier.fillMaxWidth()) {
                for (row in 0 until 4) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                for (col in 0 until 3) {
                                        val year = currentYearWindowStart + (row * 3 + col)
                                        Box(
                                                modifier =
                                                        Modifier.weight(1f)
                                                                .aspectRatio(1.8f)
                                                                .background(
                                                                        if (
                                                                                year ==
                                                                                        LocalDate
                                                                                                .now()
                                                                                                .year
                                                                        ) {
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer
                                                                        } else {
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceContainerHighest
                                                                        },
                                                                        shape = CircleShape
                                                                )
                                                                .clickable { onYearPicked(year) },
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        text = year.toString(),
                                                        style =
                                                                MaterialTheme.typography.labelLarge
                                                                        .copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                        color =
                                                                if (
                                                                        year ==
                                                                                LocalDate.now().year
                                                                ) {
                                                                        MaterialTheme.colorScheme
                                                                                .onPrimaryContainer
                                                                } else {
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                }
                                                )
                                        }
                                }
                        }
                        if (row < 3) {
                                Spacer(modifier = Modifier.height(8.dp))
                        }
                }
        }
}

@Composable
fun CalendarMonthCoverageSummary(
        viewMode: CalendarViewMode,
        currentMonth: YearMonth,
        datesWithEntries: Set<LocalDate>,
        density: CalendarDensityPreference = CalendarDensityPreference.COMFORTABLE
) {
        if (viewMode != CalendarViewMode.DAYS) return

        val preference = LocalAnimationPreference.current

        val today = LocalDate.now()
        val thisYearMonth = YearMonth.from(today)
        val isFutureMonth = currentMonth.isAfter(thisYearMonth)
        if (isFutureMonth) return

        val denominator =
                if (currentMonth == thisYearMonth) {
                        today.dayOfMonth
                } else {
                        currentMonth.lengthOfMonth()
                }
        val entriesInMonth =
                datesWithEntries.count {
                        YearMonth.from(it) == currentMonth && !it.isAfter(today)
                }
        val pct = if (denominator > 0) (entriesInMonth * 100) / denominator else 0

        Surface(
                modifier = Modifier.fillMaxWidth().padding(top = if (density == CalendarDensityPreference.DENSE) 10.dp else 18.dp),
                shape = RoundedCornerShape(if (density == CalendarDensityPreference.DENSE) 18.dp else 24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
        ) {
                val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                val progressColor = MaterialTheme.colorScheme.primary
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(
                                                horizontal = 16.dp,
                                                vertical = if (density == CalendarDensityPreference.DENSE) 8.dp else 12.dp
                                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                        Box(contentAlignment = Alignment.Center) {
                                val progress = if (denominator > 0) entriesInMonth / denominator.toFloat() else 0f
                                val animatedProgress by animateFloatAsState(
                                        targetValue = progress.coerceIn(0f, 1f),
                                        animationSpec = tween(
                                                durationMillis = preference.scaledDuration(650),
                                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                                        ),
                                        label = "month_coverage_progress"
                                )
                                val phaseShift by rememberInfiniteTransition(label = "month_coverage_wave")
                                        .animateFloat(
                                                initialValue = 0f,
                                                targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                        animation = tween(
                                                                durationMillis = preference.scaledDuration(18000).coerceAtLeast(7200),
                                                                easing = LinearEasing
                                                        )
                                                ),
                                                label = "month_coverage_wave_phase"
                                        )
                                Canvas(
                                        modifier = Modifier.size(if (density == CalendarDensityPreference.DENSE) 40.dp else 48.dp)
                                ) {
                                        val strokeWidth = 4.dp.toPx()
                                        val radius = (size.minDimension / 2f) - strokeWidth
                                        val center = center
                                        drawWavyArc(
                                                center = center,
                                                baseRadius = radius,
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                amplitude = 0.9.dp.toPx(),
                                                waves = 8,
                                                phaseShift = phaseShift,
                                                color = trackColor,
                                                strokeWidth = strokeWidth
                                        )
                                        if (animatedProgress > 0f) {
                                                drawWavyArc(
                                                        center = center,
                                                        baseRadius = radius,
                                                        startAngle = -90f,
                                                        sweepAngle = 360f * animatedProgress,
                                                        amplitude = 0.9.dp.toPx(),
                                                        waves = 8,
                                                        phaseShift = phaseShift,
                                                        color = progressColor,
                                                        strokeWidth = strokeWidth
                                                )
                                        }
                                }
                                Text(
                                        text = stringResource(R.string.calendar_percent_format, pct),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                )
                        }

                        Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                                Text(
                                        text = stringResource(R.string.calendar_entries_this_month),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.SemiBold
                                )
                                val coverageText =
                                        pluralStringResource(
                                                R.plurals.calendar_entries_this_month_days,
                                                denominator,
                                                entriesInMonth,
                                                denominator
                                        )
                                AnimatedContent(
                                        targetState = coverageText,
                                        transitionSpec = {
                                                if (preference == AnimationPreference.OFF) {
                                                        EnterTransition.None.togetherWith(ExitTransition.None)
                                                } else {
                                                        val slideDist = preference.scaledDistance(1f)
                                                        val duration = preference.scaledDuration(300)
                                                        (slideInVertically(animationSpec = tween(duration)) { height -> (height * slideDist).toInt() } + fadeIn(animationSpec = tween(preference.scaledDuration(220))))
                                                                .togetherWith(
                                                                        slideOutVertically(animationSpec = tween(duration)) { height -> (-height * slideDist).toInt() } + fadeOut(animationSpec = tween(preference.scaledDuration(180)))
                                                                )
                                                }
                                        },
                                        label = "MonthCoverageCount"
                                ) { targetText ->
                                        Text(
                                                text = targetText,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.ExtraBold
                                        )
                                }
                                if (entriesInMonth == 0) {
                                        Text(
                                                text = stringResource(R.string.calendar_empty_month_hint),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                        }
                }
        }
}

private fun DrawScope.drawWavyArc(
        center: Offset,
        baseRadius: Float,
        startAngle: Float,
        sweepAngle: Float,
        amplitude: Float,
        waves: Int,
        phaseShift: Float,
        color: Color,
        strokeWidth: Float
) {
        val steps = max(48, (kotlin.math.abs(sweepAngle) / 4f).toInt())
        val path = Path()
        for (i in 0..steps) {
                val t = i / steps.toFloat()
                val angle = Math.toRadians((startAngle + sweepAngle * t).toDouble())
                val wave = sin((t + phaseShift) * waves * Math.PI * 2).toFloat()
                val radius = baseRadius + (wave * amplitude)
                val x = center.x + (cos(angle).toFloat() * radius)
                val y = center.y + (sin(angle).toFloat() * radius)
                if (i == 0) {
                        path.moveTo(x, y)
                } else {
                        path.lineTo(x, y)
                }
        }
        drawPath(
                path = path,
                color = color,
                style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                )
        )
}

@Composable
fun CalendarMemorySummary(memoryCount: Int) {
        Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = RoundedCornerShape(28.dp),
                                tonalElevation = 2.dp,
                                shadowElevation = 0.dp
                        ) {
                                Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 28.dp,
                                                        vertical = 12.dp
                                                )
                                ) {
                                        Text(
                                                text = stringResource(R.string.calendar_days_of_memories),
                                                style = MaterialTheme.typography.labelSmall,
                                                color =
                                                        MaterialTheme.colorScheme
                                                                .onSecondaryContainer
                                                                .copy(alpha = 0.72f),
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                                text = "$memoryCount",
                                                style =
                                                        MaterialTheme.typography.headlineLarge
                                                                .copy(
                                                                        fontWeight =
                                                                                FontWeight.Black
                                                                ),
                                                color =
                                                        MaterialTheme.colorScheme
                                                                .onSecondaryContainer
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.navigationBarsPadding().height(108.dp))
                }
        }
}
