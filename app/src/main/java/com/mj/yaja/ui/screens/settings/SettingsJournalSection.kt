package com.mj.yaja.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.data.DateKeywordEntry
import com.mj.yaja.data.DateOrderPreference
import com.mj.yaja.data.EntryStyle
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.tweenSpec
import java.time.DayOfWeek
import kotlin.math.roundToInt

@Composable
fun JournalExperienceSection(
    renderCheckboxesAsText: Boolean,
    onRenderCheckboxesAsTextChange: (Boolean) -> Unit,
    showDayHeaderStats: Boolean,
    onShowDayHeaderStatsChange: (Boolean) -> Unit,
    entryStyle: EntryStyle,
    onEntryStyleSelected: (EntryStyle) -> Unit,
    dateOrderPreference: DateOrderPreference,
    onDateOrderChange: (DateOrderPreference) -> Unit,
    animationPreference: AnimationPreference,
    onAnimationPreferenceChange: (AnimationPreference) -> Unit,
    customDateKeywords: List<DateKeywordEntry>,
    onSetCustomDateKeywords: (List<DateKeywordEntry>) -> Unit,
    fuzzyThreshold: Float,
    onFuzzyThresholdChange: (Float) -> Unit,
    showTimestamps: Boolean,
    onShowTimestampsChange: (Boolean) -> Unit,
    allowFutureEntries: Boolean,
    onAllowFutureEntriesChange: (Boolean) -> Unit,
    isPreviewLimitEnabled: Boolean,
    onIsPreviewLimitEnabledChange: (Boolean) -> Unit,
    previewLimitLength: Int,
    onPreviewLimitLengthChange: (Int) -> Unit,
    firstDayOfWeek: DayOfWeek,
    onFirstDayOfWeekChange: (DayOfWeek) -> Unit
) {
    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var showKeywordsListDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Entry Style",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose between elevated cards and a flatter journal look.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small
                    )
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "Cards" to EntryStyle.CARDS,
                    "Flat" to EntryStyle.FLAT
                ).forEach { (label, style) ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (entryStyle == style) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable { onEntryStyleSelected(style) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (entryStyle == style) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = "Render Checkboxes as Text",
            subtitle = "Show todos as [ ] and [x] across the app instead of expressive checkboxes.",
            checked = renderCheckboxesAsText,
            onCheckedChange = onRenderCheckboxesAsTextChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = "Show Day Header Counts",
            subtitle = "Show entry, word, and character counts in the journal day header.",
            checked = showDayHeaderStats,
            onCheckedChange = onShowDayHeaderStatsChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = "Show Timestamps",
            subtitle = "Display entry time on the timeline.",
            checked = showTimestamps,
            onCheckedChange = onShowTimestampsChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = "Allow Future Entries",
            subtitle = "Enable adding entries to future dates.",
            checked = allowFutureEntries,
            onCheckedChange = onAllowFutureEntriesChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = "Truncate Long Entries",
            subtitle = "Shorten long entries on the home screen.",
            checked = isPreviewLimitEnabled,
            onCheckedChange = onIsPreviewLimitEnabledChange
        )

        val preference = LocalAnimationPreference.current
        AnimatedVisibility(
            visible = isPreviewLimitEnabled,
            enter = preference.enterOrNone(
                fadeIn(preference.floatTween(150)) + expandVertically(preference.tweenSpec(200))
            ),
            exit = preference.exitOrNone(
                fadeOut(preference.floatTween(100)) + shrinkVertically(preference.tweenSpec(180))
            )
        ) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "Character Limit",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Set how many characters to display before truncating.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    var _sliderValue by remember(previewLimitLength) {
                        mutableStateOf(previewLimitLength.toFloat())
                    }

                    Slider(
                        value = _sliderValue,
                        onValueChange = { _sliderValue = it },
                        onValueChangeFinished = {
                            onPreviewLimitLengthChange(_sliderValue.toInt())
                        },
                        valueRange = 50f..200f,
                        steps = 14,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "50 chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${_sliderValue.toInt()} characters",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "200 chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "First Day of Week",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Start calendar on",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small
                    )
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (firstDayOfWeek == DayOfWeek.SUNDAY) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { onFirstDayOfWeekChange(DayOfWeek.SUNDAY) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Sun",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (firstDayOfWeek == DayOfWeek.SUNDAY) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (firstDayOfWeek == DayOfWeek.MONDAY) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { onFirstDayOfWeekChange(DayOfWeek.MONDAY) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Mon",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (firstDayOfWeek == DayOfWeek.MONDAY) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Text(
            text = "Date Recognition",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Date Order",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "How to read numeric dates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small
                    )
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "Auto" to DateOrderPreference.AUTO,
                    "DD/MM" to DateOrderPreference.DMY,
                    "MM/DD" to DateOrderPreference.MDY
                ).forEach { (label, pref) ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (dateOrderPreference == pref) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable { onDateOrderChange(pref) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (dateOrderPreference == pref) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Date Keywords",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Words in other languages that mean a date (for example a local word for yesterday).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (customDateKeywords.isNotEmpty()) {
                    androidx.compose.material3.TextButton(
                        onClick = { showKeywordsListDialog = true }
                    ) {
                        Text(
                            text = "View (${customDateKeywords.size})",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                IconButton(onClick = { showAddKeywordDialog = true }) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add keyword",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Text(
            text = "Motion",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Animations",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Control motion across the app. Reduced keeps things fluid with less movement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small
                    )
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "Full" to AnimationPreference.FULL,
                    "Reduced" to AnimationPreference.REDUCED,
                    "Off" to AnimationPreference.OFF
                ).forEach { (label, preference) ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (animationPreference == preference) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable { onAnimationPreferenceChange(preference) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (animationPreference == preference) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }

    if (showKeywordsListDialog) {
        ViewKeywordsDialog(
            keywords = customDateKeywords,
            onDismiss = { showKeywordsListDialog = false },
            onRemove = { index ->
                onSetCustomDateKeywords(customDateKeywords.toMutableList().also {
                    it.removeAt(index)
                })
            }
        )
    }

    if (showAddKeywordDialog) {
        AddDateKeywordDialog(
            onDismiss = { showAddKeywordDialog = false },
            onAdd = { keyword, meaning ->
                onSetCustomDateKeywords(customDateKeywords + DateKeywordEntry(keyword, meaning))
                showAddKeywordDialog = false
            }
        )
    }

    Spacer(modifier = Modifier.size(32.dp))
}

@Composable
fun ReviewAndInsightsSection(
    entryReviewEnabled: Boolean,
    onEntryReviewEnabledChange: (Boolean) -> Unit,
    keywordHighlightingEnabled: Boolean,
    onKeywordHighlightingEnabledChange: (Boolean) -> Unit,
    fuzzyThreshold: Float,
    onFuzzyThresholdChange: (Float) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = "Post-save Review",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Post-write Review",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Show review sheet after tapping save to extract todos, check mentions, star day, or set label.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = entryReviewEnabled,
                onCheckedChange = onEntryReviewEnabledChange
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Text(
            text = "People & Places",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Highlighting",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Highlight tracked people and places in Home cards and View Entry mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = keywordHighlightingEnabled,
                onCheckedChange = onKeywordHighlightingEnabledChange
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Match Sensitivity",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(fuzzyThreshold * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "How closely a word must match to be detected by People & Places (higher = stricter).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = fuzzyThreshold,
                onValueChange = onFuzzyThresholdChange,
                valueRange = 0.70f..1.00f,
                steps = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Spacer(modifier = Modifier.size(32.dp))
}
