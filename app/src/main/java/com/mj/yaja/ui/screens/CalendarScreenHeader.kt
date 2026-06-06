package com.mj.yaja.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.floatSpring
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.scaledDuration
import com.mj.yaja.ui.design.scaledDistance
import com.mj.yaja.data.AnimationPreference
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarHeaderNavigator(
        viewMode: CalendarViewMode,
        currentMonth: YearMonth,
        currentYearWindowStart: Int,
        headerSubtitle: String? = null,
        allowFutureEntries: Boolean,
        onPreviousClick: () -> Unit,
        onNextClick: () -> Unit,
        onHeaderClick: () -> Unit
) {
        val preference = LocalAnimationPreference.current
        val prevInteractionSource = remember { MutableInteractionSource() }
        val prevIsPressed by prevInteractionSource.collectIsPressedAsState()
        val prevScale = if (preference == AnimationPreference.OFF) 1f else animateFloatAsState(
                if (prevIsPressed) 0.88f else 1f,
                animationSpec = preference.floatSpring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                ),
                label = "PrevScale"
        ).value

        val nextInteractionSource = remember { MutableInteractionSource() }
        val nextIsPressed by nextInteractionSource.collectIsPressedAsState()
        val nextScale = if (preference == AnimationPreference.OFF) 1f else animateFloatAsState(
                if (nextIsPressed) 0.88f else 1f,
                animationSpec = preference.floatSpring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                ),
                label = "NextScale"
        ).value

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                FilledTonalIconButton(
                        modifier =
                                Modifier.graphicsLayer {
                                        scaleX = prevScale
                                        scaleY = prevScale
                                },
                        interactionSource = prevInteractionSource,
                        onClick = onPreviousClick,
                        shape = RoundedCornerShape(18.dp)
                ) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous")
                }

                AnimatedContent(
                        targetState =
                                when (viewMode) {
                                        CalendarViewMode.DAYS ->
                                                "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}"
                                        CalendarViewMode.MONTHS -> "${currentMonth.year}"
                                        CalendarViewMode.YEARS ->
                                                "$currentYearWindowStart – ${currentYearWindowStart + 9}"
                                },
                        transitionSpec = {
                                if (preference == AnimationPreference.OFF) {
                                        EnterTransition.None.togetherWith(ExitTransition.None)
                                } else {
                                        val duration = preference.scaledDuration(200)
                                        val slideDist = preference.scaledDistance(1f)
                                        (slideInVertically(animationSpec = tween(duration)) { h -> (-h / 2 * slideDist).toInt() } +
                                                        fadeIn(animationSpec = tween(duration)))
                                                .togetherWith(
                                                        slideOutVertically(animationSpec = tween(preference.scaledDuration(180))) { h ->
                                                                (h / 2 * slideDist).toInt()
                                                        } + fadeOut(animationSpec = tween(preference.scaledDuration(160)))
                                                )
                                }
                        },
                        modifier =
                                Modifier.clickable(onClick = onHeaderClick)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                        label = "HeaderText"
                ) { headerText ->
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                                Text(
                                        text = headerText,
                                        style =
                                                MaterialTheme.typography.headlineMedium.copy(
                                                        fontWeight = FontWeight.ExtraBold
                                                ),
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                if (viewMode == CalendarViewMode.DAYS && !headerSubtitle.isNullOrBlank()) {
                                        Text(
                                                text = headerSubtitle,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                                                fontWeight = FontWeight.SemiBold
                                        )
                                }
                        }
                }

                val maxYearMonth =
                        if (allowFutureEntries) YearMonth.now().plusYears(1) else YearMonth.now()
                FilledTonalIconButton(
                        enabled =
                                when (viewMode) {
                                        CalendarViewMode.DAYS -> currentMonth < maxYearMonth
                                        CalendarViewMode.MONTHS -> currentMonth < maxYearMonth
                                        CalendarViewMode.YEARS ->
                                                currentYearWindowStart + 9 < maxYearMonth.year
                                },
                        modifier =
                                Modifier.graphicsLayer {
                                        scaleX = nextScale
                                        scaleY = nextScale
                                },
                        interactionSource = nextInteractionSource,
                        onClick = onNextClick,
                        shape = RoundedCornerShape(18.dp)
                ) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Next")
                }
        }
}
