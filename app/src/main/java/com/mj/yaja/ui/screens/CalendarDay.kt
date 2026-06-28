package com.mj.yaja.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.floatSpring
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.data.AnimationPreference
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import com.mj.yaja.ui.design.expressivePressMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CalendarDay(
        day: Int,
        isSelected: Boolean,
        isToday: Boolean,
        hasEntries: Boolean,
        hasFollowUp: Boolean = false,
        isFuture: Boolean = false,
        isDimmed: Boolean = false,
        isFavorited: Boolean = false,
        onClick: () -> Unit
) {
        val expressiveShape = RoundedCornerShape(18.dp)
        val interactionSource = remember { MutableInteractionSource() }

        val animationPreference = LocalAnimationPreference.current
        // Today's gentle pulse. Kept as State (no .value read here) so it's consumed inside
        // graphicsLayer's draw-phase lambda below — the cell redraws each frame without recomposing.
        val pulseScale: State<Float> = if (isToday && !isSelected && animationPreference != AnimationPreference.OFF) {
                val infiniteTransition = rememberInfiniteTransition(label = "TodayPulse")
                val targetPulse = if (animationPreference == AnimationPreference.REDUCED) 1.01f else 1.03f
                val duration = if (animationPreference == AnimationPreference.REDUCED) 2400 else 1200
                infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = targetPulse,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(duration, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "Pulse"
                )
        } else {
                remember { mutableStateOf(1f) }
        }

        val containerColor =
                when {
                        isSelected && isToday -> MaterialTheme.colorScheme.primary
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isFavorited ->
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.36f)
                        hasFollowUp ->
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        isToday ->
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                        hasEntries ->
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.68f)
                }
        val borderColor =
                when {
                        isSelected -> Color.Transparent
                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)
                }
        val dayColor =
                when {
                        isDimmed ->
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f)
                        isSelected && isToday -> MaterialTheme.colorScheme.onPrimary
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                }
        val indicatorColor =
                when {
                        isSelected && isToday -> MaterialTheme.colorScheme.onPrimary
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        hasFollowUp -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                }

        Box(
                modifier =
                        Modifier.aspectRatio(1.15f)
                                .padding(3.dp)
                                .graphicsLayer {
                                        val s = pulseScale.value
                                        scaleX = s
                                        scaleY = s
                                }
                                .clip(expressiveShape)
                                .background(containerColor)
                                .border(width = 1.dp, color = borderColor, shape = expressiveShape)
                                .expressivePressMotion(interactionSource, pressedScale = 0.90f)
                                .clickable(
                                        interactionSource = interactionSource,
                                        indication = androidx.compose.foundation.LocalIndication.current,
                                        onClick = onClick
                                ),
                contentAlignment = Alignment.Center
        ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                        Text(
                                text = day.toString(),
                                style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight =
                                                        if (isToday) FontWeight.ExtraBold
                                                        else if (isSelected) FontWeight.Bold
                                                        else FontWeight.Medium
                                        ),
                                color = dayColor
                        )

                        AnimatedVisibility(
                                visible = hasEntries || hasFollowUp,
                                enter = animationPreference.enterOrNone(
                                        scaleIn(
                                                animationSpec =
                                                        animationPreference.floatSpring(
                                                                dampingRatio =
                                                                        Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                        ),
                                                initialScale = 0f
                                        ) + fadeIn(animationSpec = animationPreference.floatTween(220))
                                ),
                                exit = animationPreference.exitOrNone(
                                        scaleOut(
                                                animationSpec =
                                                        animationPreference.floatSpring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                        ),
                                                targetScale = 0f
                                        ) + fadeOut(animationSpec = animationPreference.floatTween(180))
                                )
                        ) {
                                Box(
                                        modifier = Modifier.height(6.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (hasEntries) {
                                                        Box(
                                                                modifier =
                                                                        Modifier.size(4.dp)
                                                                                .background(
                                                                                        indicatorColor,
                                                                                        shape = CircleShape
                                                                                )
                                                        )
                                                }
                                                if (hasFollowUp) {
                                                        Box(
                                                                modifier =
                                                                        Modifier.size(5.dp)
                                                                                .background(
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .secondary,
                                                                                        shape = CircleShape
                                                                                )
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}
