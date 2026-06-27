package com.mj.yaja.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.data.AnimationPreference
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.ui.design.expressiveFabMotion
import com.mj.yaja.ui.design.floatSpring
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.scaledDuration
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import java.time.LocalDate
import com.mj.yaja.data.DateKeywordEntry
import com.mj.yaja.ui.theme.DataFontScaleWrapper

@Composable
fun LookbackReviewSection(
        onNavigateToReview: (ReviewPeriodType) -> Unit
) {
        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                SectionHeader(
                        title = stringResource(R.string.lookback_reviews_title),
                        icon = Icons.Rounded.CalendarMonth
                )

                ElevatedCard(
                        colors =
                                CardDefaults.elevatedCardColors(
                                        containerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                )
                ) {
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                Text(
                                        text = stringResource(R.string.lookback_reviews_intro),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AssistChip(
                                                onClick = {},
                                                enabled = false,
                                                colors =
                                                        AssistChipDefaults.assistChipColors(
                                                                disabledContainerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceContainerHighest,
                                                                disabledLabelColor =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        ),
                                                border = null,
                                                label = { Text(stringResource(R.string.lookback_chip_weekly)) }
                                        )
                                        AssistChip(
                                                onClick = {},
                                                enabled = false,
                                                colors =
                                                        AssistChipDefaults.assistChipColors(
                                                                disabledContainerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceContainerHighest,
                                                                disabledLabelColor =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        ),
                                                border = null,
                                                label = { Text(stringResource(R.string.lookback_chip_monthly)) }
                                        )
                                }
                        }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val stacked = maxWidth < 420.dp
                        if (stacked) {
                                Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        ReviewLaunchCard(
                                                title = stringResource(R.string.lookback_weekly_review),
                                                subtitle = stringResource(R.string.lookback_weekly_review_subtitle),
                                                icon = Icons.Rounded.CalendarViewWeek,
                                                modifier = Modifier.fillMaxWidth(),
                                                onClick = { onNavigateToReview(ReviewPeriodType.WEEKLY) }
                                        )
                                        ReviewLaunchCard(
                                                title = stringResource(R.string.lookback_monthly_review),
                                                subtitle = stringResource(R.string.lookback_monthly_review_subtitle),
                                                icon = Icons.Rounded.CalendarMonth,
                                                modifier = Modifier.fillMaxWidth(),
                                                onClick = { onNavigateToReview(ReviewPeriodType.MONTHLY) }
                                        )
                                }
                        } else {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        ReviewLaunchCard(
                                                title = stringResource(R.string.lookback_weekly_review),
                                                subtitle = stringResource(R.string.lookback_weekly_review_subtitle),
                                                icon = Icons.Rounded.CalendarViewWeek,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onNavigateToReview(ReviewPeriodType.WEEKLY) }
                                        )
                                        ReviewLaunchCard(
                                                title = stringResource(R.string.lookback_monthly_review),
                                                subtitle = stringResource(R.string.lookback_monthly_review_subtitle),
                                                icon = Icons.Rounded.CalendarMonth,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onNavigateToReview(ReviewPeriodType.MONTHLY) }
                                        )
                                }
                        }
                }
        }
}

@Composable
fun LookbackFlashbacksSection(
        flashbacks: Map<Int, List<String>>,
        selectedDate: LocalDate,
        entranceTriggered: Boolean,
        isPreviewLimitEnabled: Boolean,
        previewLimitLength: Int,
        onNavigateToDate: (LocalDate) -> Unit,
        monthFirst: Boolean,
        customKeywords: List<DateKeywordEntry>
) {
        if (flashbacks.isEmpty()) return

        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                SectionHeader(
                        title = stringResource(R.string.lookback_flashbacks_title),
                        icon = Icons.Rounded.History
                )

                flashbacks.entries.forEachIndexed { index, entryMap ->
                        val yearsAgo = entryMap.key
                        val entries = entryMap.value
                        AppStaggeredEntrance(
                                visible = entranceTriggered,
                                index = index,
                                strength = AppEntranceStrength.SECTION
                        ) {
                                DataFontScaleWrapper {
                                        FlashbackCard(
                                                yearsAgo = yearsAgo,
                                                date = selectedDate.minusYears(yearsAgo.toLong()),
                                                entries = entries,
                                                isPreviewLimitEnabled = isPreviewLimitEnabled,
                                                previewLimitLength = previewLimitLength,
                                                onClick = {
                                                        onNavigateToDate(
                                                                selectedDate.minusYears(yearsAgo.toLong())
                                                        )
                                                },
                                                onDateLinkClick = onNavigateToDate,
                                                monthFirst = monthFirst,
                                                customKeywords = customKeywords
                                        )
                                }
                        }
                }
        }
}

@Composable
fun LookbackHighlightsSection(
        favoritedHighlights: List<LocalDate>,
        starredLabels: Map<LocalDate, String>,
        entranceTriggered: Boolean,
        onNavigateToDate: (LocalDate) -> Unit
) {
        if (favoritedHighlights.isEmpty()) return

        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                SectionHeader(
                        title = stringResource(R.string.lookback_highlights_title),
                        icon = Icons.Rounded.Star
                )

                favoritedHighlights.forEachIndexed { index, date ->
                        val label = starredLabels[date] ?: ""
                        AppStaggeredEntrance(
                                visible = entranceTriggered,
                                index = index,
                                strength = AppEntranceStrength.SUBTLE
                        ) {
                                DataFontScaleWrapper {
                                        HighlightCard(
                                                date = date,
                                                onClick = { onNavigateToDate(date) },
                                                label = label
                                        )
                                }
                        }
                }
        }
}

@Composable
fun LookbackEmptyState() {
        Box(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
        ) {
                val animationPreference = LocalAnimationPreference.current
                val scale = if (animationPreference == AnimationPreference.OFF) {
                        1f
                } else {
                        val infiniteTransition = rememberInfiniteTransition(label = "EmptyState")
                        val targetScale = if (animationPreference == AnimationPreference.REDUCED) 1.02f else 1.05f
                        val duration = if (animationPreference == AnimationPreference.REDUCED) 4000 else 2000
                        val scaleState by
                                infiniteTransition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = targetScale,
                                        animationSpec =
                                                androidx.compose.animation.core.infiniteRepeatable(
                                                        animation =
                                                                androidx.compose.animation.core.tween(
                                                                        duration,
                                                                        easing =
                                                                                androidx.compose.animation
                                                                                        .core
                                                                                        .FastOutSlowInEasing
                                                                ),
                                                        repeatMode =
                                                                androidx.compose.animation.core
                                                                        .RepeatMode.Reverse
                                                ),
                                        label = "IconBreathing"
                                )
                        scaleState
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp).scale(scale),
                                tint =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.2f
                                        )
                        )
                        Spacer(Modifier.size(16.dp))
                        Text(
                                stringResource(R.string.lookback_empty_title),
                                style =
                                        MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                        ),
                                color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.6f
                                        )
                        )
                        Text(
                                stringResource(R.string.lookback_empty_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.4f
                                        ),
                                textAlign = TextAlign.Center
                        )
                }
        }
}

@Composable
fun LookbackSurpriseFab(
        visible: Boolean,
        motionPreference: AnimationPreference,
        interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        AnimatedVisibility(
                visible = visible,
                enter =
                        motionPreference.enterOrNone(
                                fadeIn(animationSpec = motionPreference.floatTween(220)) +
                                        slideInVertically(
                                                animationSpec =
                                                        androidx.compose.animation.core.tween(
                                                                motionPreference.scaledDuration(
                                                                        240
                                                                )
                                                        ),
                                                initialOffsetY = { it }
                                        ) +
                                        scaleIn(
                                                animationSpec =
                                                        motionPreference.floatSpring(
                                                                dampingRatio = 0.62f,
                                                                stiffness = 420f
                                                        ),
                                                initialScale = 0.9f
                                        )
                        ),
                exit =
                        motionPreference.exitOrNone(
                                fadeOut(animationSpec = motionPreference.floatTween(160)) +
                                        slideOutVertically(
                                                animationSpec =
                                                        androidx.compose.animation.core.tween(
                                                                motionPreference.scaledDuration(
                                                                        180
                                                                )
                                                        ),
                                                targetOffsetY = { it / 2 }
                                        ) +
                                        scaleOut(
                                                animationSpec =
                                                        motionPreference.floatSpring(
                                                                dampingRatio = 0.86f,
                                                                stiffness = 540f
                                                        ),
                                                targetScale = 0.92f
                                        )
                        ),
                modifier = modifier
        ) {
                FloatingActionButton(
                        onClick = onClick,
                        interactionSource = interactionSource,
                        modifier =
                                Modifier.size(64.dp)
                                        .expressiveFabMotion(interactionSource),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                        Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = stringResource(R.string.lookback_cd_random_entry),
                                modifier = Modifier.size(28.dp)
                        )
                }
        }
}

@Composable
private fun ReviewLaunchCard(
        title: String,
        subtitle: String,
        icon: ImageVector,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
) {
        ElevatedCard(
                modifier = modifier.clickable(onClick = onClick),
                colors =
                        CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
        ) {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                androidx.compose.material3.Surface(
                                        shape = CircleShape,
                                        color =
                                                MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.14f
                                                )
                                ) {
                                        Box(
                                                modifier = Modifier.size(40.dp),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                )
                                        }
                                }
                                AssistChip(
                                        onClick = {},
                                        enabled = false,
                                        colors =
                                                AssistChipDefaults.assistChipColors(
                                                        disabledContainerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceContainerHighest,
                                                        disabledLabelColor =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                ),
                                        border = null,
                                        label = {
                                                Text(
                                                        text =
                                                                if (title.contains("Weekly")) {
                                                                        stringResource(R.string.lookback_seven_days)
                                                                } else {
                                                                        stringResource(R.string.lookback_one_month)
                                                                },
                                                        style = MaterialTheme.typography.labelMedium
                                                )
                                        }
                                )
                        }
                        Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                        )
                        Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                                Text(
                                        text = stringResource(R.string.action_open),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                )
                        }
                }
        }
}
