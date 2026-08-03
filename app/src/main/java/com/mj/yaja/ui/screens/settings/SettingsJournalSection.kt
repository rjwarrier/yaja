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
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.R
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.data.CalendarDensityPreference
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
    carryForwardTodosEnabled: Boolean,
    onCarryForwardTodosEnabledChange: (Boolean) -> Unit,
    entryStyle: EntryStyle,
    onEntryStyleSelected: (EntryStyle) -> Unit,
    calendarDensityPreference: CalendarDensityPreference,
    onCalendarDensityPreferenceChange: (CalendarDensityPreference) -> Unit,
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
                    text = stringResource(R.string.settings_journal_entry_style_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_journal_entry_style_subtitle),
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
                    stringResource(R.string.settings_journal_entry_style_cards) to EntryStyle.CARDS,
                    stringResource(R.string.settings_journal_entry_style_flat) to EntryStyle.FLAT
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_calendar_density_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_calendar_density_subtitle),
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
                    stringResource(R.string.settings_calendar_density_comfortable) to CalendarDensityPreference.COMFORTABLE,
                    stringResource(R.string.settings_calendar_density_compact) to CalendarDensityPreference.COMPACT,
                    stringResource(R.string.settings_calendar_density_dense) to CalendarDensityPreference.DENSE
                ).forEach { (label, preference) ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (calendarDensityPreference == preference) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable { onCalendarDensityPreferenceChange(preference) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (calendarDensityPreference == preference) {
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
            title = stringResource(R.string.settings_journal_render_checkboxes_title),
            subtitle = stringResource(R.string.settings_journal_render_checkboxes_subtitle),
            checked = renderCheckboxesAsText,
            onCheckedChange = onRenderCheckboxesAsTextChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = stringResource(R.string.settings_journal_day_header_counts_title),
            subtitle = stringResource(R.string.settings_journal_day_header_counts_subtitle),
            checked = showDayHeaderStats,
            onCheckedChange = onShowDayHeaderStatsChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = stringResource(R.string.settings_journal_show_timestamps_title),
            subtitle = stringResource(R.string.settings_journal_show_timestamps_subtitle),
            checked = showTimestamps,
            onCheckedChange = onShowTimestampsChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = stringResource(R.string.settings_journal_allow_future_title),
            subtitle = stringResource(R.string.settings_journal_allow_future_subtitle),
            checked = allowFutureEntries,
            onCheckedChange = onAllowFutureEntriesChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = stringResource(R.string.settings_journal_carry_forward_title),
            subtitle = stringResource(R.string.settings_journal_carry_forward_subtitle),
            checked = carryForwardTodosEnabled,
            onCheckedChange = onCarryForwardTodosEnabledChange
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        PreferencesSwitchRow(
            title = stringResource(R.string.settings_journal_truncate_title),
            subtitle = stringResource(R.string.settings_journal_truncate_subtitle),
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
                        text = stringResource(R.string.settings_journal_char_limit_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_journal_char_limit_subtitle),
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
                            stringResource(R.string.settings_journal_char_limit_min),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.settings_journal_char_count, _sliderValue.toInt()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.settings_journal_char_limit_max),
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
                    text = stringResource(R.string.settings_journal_first_day_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_journal_first_day_subtitle),
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
                        stringResource(R.string.settings_journal_day_sun),
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
                        stringResource(R.string.settings_journal_day_mon),
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
            text = stringResource(R.string.settings_journal_date_recognition),
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
                    text = stringResource(R.string.settings_journal_date_order_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_journal_date_order_subtitle),
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
                    stringResource(R.string.settings_journal_date_order_auto) to DateOrderPreference.AUTO,
                    stringResource(R.string.settings_journal_date_order_dmy) to DateOrderPreference.DMY,
                    stringResource(R.string.settings_journal_date_order_mdy) to DateOrderPreference.MDY
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
                    text = stringResource(R.string.settings_journal_date_keywords_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.settings_journal_date_keywords_subtitle),
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
                            text = stringResource(R.string.settings_journal_view_keywords, customDateKeywords.size),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                IconButton(onClick = { showAddKeywordDialog = true }) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.settings_journal_add_keyword_cd),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Text(
            text = stringResource(R.string.settings_journal_motion),
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
                    text = stringResource(R.string.settings_journal_animations_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_journal_animations_subtitle),
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
                    stringResource(R.string.settings_journal_animations_full) to AnimationPreference.FULL,
                    stringResource(R.string.settings_journal_animations_reduced) to AnimationPreference.REDUCED,
                    stringResource(R.string.settings_journal_animations_off) to AnimationPreference.OFF
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
fun JournalExperienceEntrySection(onNavigateToJournalExperience: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToJournalExperience),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings_section_journal_experience),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_section_journal_experience),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_journal_experience_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
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
            text = stringResource(R.string.settings_review_post_save),
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
                    text = stringResource(R.string.settings_review_post_write_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_review_post_write_subtitle),
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
            text = stringResource(R.string.settings_review_people_places),
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
                    text = stringResource(R.string.settings_review_highlighting_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_review_highlighting_subtitle),
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
                    text = stringResource(R.string.settings_review_match_sensitivity_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_review_match_sensitivity_percent, (fuzzyThreshold * 100).roundToInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(R.string.settings_review_match_sensitivity_subtitle),
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
