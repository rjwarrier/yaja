package com.mj.yaja.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CalendarDay(
        day: Int,
        isSelected: Boolean,
        isToday: Boolean,
        hasEntries: Boolean,
        isFuture: Boolean = false,
        isDimmed: Boolean = false,
        isFavorited: Boolean = false,
        onClick: () -> Unit
) {
        // Morphing Shape logic for the selection indicator
        val shapeStep = day % 4
        val tlRadius by
                animateDpAsState(
                        targetValue =
                                if (isSelected)
                                        when (shapeStep) {
                                                1 -> 16.dp
                                                2 -> 6.dp
                                                3 -> 14.dp
                                                else -> 12.dp
                                        }
                                else 16.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "TL"
                )
        val trRadius by
                animateDpAsState(
                        targetValue =
                                if (isSelected)
                                        when (shapeStep) {
                                                1 -> 6.dp
                                                2 -> 16.dp
                                                3 -> 14.dp
                                                else -> 12.dp
                                        }
                                else 16.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "TR"
                )
        val brRadius by
                animateDpAsState(
                        targetValue =
                                if (isSelected)
                                        when (shapeStep) {
                                                1 -> 16.dp
                                                2 -> 6.dp
                                                3 -> 4.dp
                                                else -> 12.dp
                                        }
                                else 16.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "BR"
                )
        val blRadius by
                animateDpAsState(
                        targetValue =
                                if (isSelected)
                                        when (shapeStep) {
                                                1 -> 6.dp
                                                2 -> 16.dp
                                                3 -> 14.dp
                                                else -> 12.dp
                                        }
                                else 16.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "BL"
                )

        val expressiveShape =
                RoundedCornerShape(
                        topStart = tlRadius,
                        topEnd = trRadius,
                        bottomEnd = brRadius,
                        bottomStart = blRadius
                )

        // Only run the pulse animation for today's unselected cell; the other 28-30 cells
        // don't need an InfiniteTransition ticking every frame.
        val finalScale = if (isToday && !isSelected) {
                val infiniteTransition = rememberInfiniteTransition(label = "TodayPulse")
                infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.05f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(1200, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "Pulse"
                ).value
        } else {
                1f
        }

        Box(
                modifier =
                        Modifier.aspectRatio(1f)
                                .padding(4.dp)
                                .scale(finalScale)
                                .clip(expressiveShape)
                                .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else if (isFavorited)
                                                MaterialTheme.colorScheme.tertiary.copy(
                                                        alpha = 0.12f
                                                )
                                        else if (isToday)
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.5f
                                                )
                                        else Color.Transparent
                                )
                                .clickable { onClick() },
                contentAlignment = Alignment.Center
        ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                        Text(
                                text = day.toString(),
                                style =
                                        MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight =
                                                        if (isToday) FontWeight.ExtraBold
                                                        else if (isSelected) FontWeight.Bold
                                                        else FontWeight.Normal
                                        ),
                                color =
                                        when {
                                                isDimmed ->
                                                        MaterialTheme.colorScheme.onBackground.copy(
                                                                alpha = 0.38f
                                                        )
                                                isSelected ->
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                isToday -> MaterialTheme.colorScheme.primary
                                                isFuture ->
                                                        MaterialTheme.colorScheme.onBackground.copy(
                                                                alpha = 0.45f
                                                        )
                                                else -> MaterialTheme.colorScheme.onBackground
                                        }
                        )

                        AnimatedVisibility(
                                visible = hasEntries,
                                enter =
                                        scaleIn(
                                                animationSpec =
                                                        spring(
                                                                dampingRatio =
                                                                        Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                        ),
                                                initialScale = 0f
                                        ) + fadeIn(),
                                exit =
                                        scaleOut(
                                                animationSpec =
                                                        spring(stiffness = Spring.StiffnessLow),
                                                targetScale = 0f
                                        ) + fadeOut()
                        ) {
                                Box(
                                        modifier = Modifier.height(6.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                        modifier =
                                                                Modifier.size(4.dp)
                                                                        .background(
                                                                                if (isSelected)
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary
                                                                                else
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .tertiary,
                                                                                shape = CircleShape
                                                                        )
                                                )
                                        }
                                }
                        }
                }
        }
}
