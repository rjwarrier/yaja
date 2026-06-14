package com.mj.yaja.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.mj.yaja.ui.design.expressiveFabMotion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.data.EventItem
import com.mj.yaja.data.TodoItem
import com.mj.yaja.ui.components.ExpressiveCheckbox
import com.mj.yaja.ui.theme.metaSmallTextStyle
import com.mj.yaja.ui.theme.metaTextStyle
import com.mj.yaja.ui.utils.MarkdownUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val todoDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val todoWeekdayFormatter = DateTimeFormatter.ofPattern("EEE")

@Composable
internal fun TodoHeroCard(
    selectedFilter: TodoFilter,
    metrics: TodoHeroMetrics
) {
    val accentContainer = when (selectedFilter) {
        TodoFilter.EVENTS -> lerp(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.tertiaryContainer,
            0.58f
        )
        TodoFilter.DONE -> lerp(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.secondaryContainer,
            0.54f
        )
        else -> lerp(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.primaryContainer,
            0.54f
        )
    }
    val onAccentContainer = when (selectedFilter) {
        TodoFilter.EVENTS -> MaterialTheme.colorScheme.onTertiaryContainer
        TodoFilter.DONE -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val mainTint = when (selectedFilter) {
        TodoFilter.EVENTS -> MaterialTheme.colorScheme.tertiary
        TodoFilter.DONE -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    val heroContainer = lerp(
        MaterialTheme.colorScheme.surfaceContainerLow,
        MaterialTheme.colorScheme.surfaceContainer,
        0.6f
    )
    val statContainer = lerp(
        MaterialTheme.colorScheme.surfaceContainerLowest,
        MaterialTheme.colorScheme.surfaceContainerLow,
        0.55f
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = heroContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentContainer.copy(alpha = 0.88f)
            ) {
                Text(
                    text = metrics.accentTitle,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = onAccentContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = metrics.mainValue.toString(),
                    modifier = Modifier.offset(y = (-2).dp),
                    style = MaterialTheme.typography.displaySmall,
                    color = mainTint,
                    fontWeight = FontWeight.ExtraBold
                )
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = metrics.mainLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.todos_hero_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TodoHeroStatPill(
                    selectedFilter = selectedFilter,
                    value = metrics.statAValue,
                    label = metrics.statALabel,
                    containerColor = statContainer,
                    modifier = Modifier.weight(1f)
                )
                TodoHeroStatPill(
                    selectedFilter = selectedFilter,
                    value = metrics.statBValue,
                    label = metrics.statBLabel,
                    containerColor = statContainer,
                    modifier = Modifier.weight(1f)
                )
                TodoHeroStatPill(
                    selectedFilter = selectedFilter,
                    value = metrics.statCValue,
                    label = metrics.statCLabel,
                    containerColor = statContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TodoHeroStatPill(
    selectedFilter: TodoFilter,
    value: Int,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val pillContainer = containerColor
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = pillContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TodoFilterChips(
    selectedFilter: TodoFilter,
    allCount: Int,
    openCount: Int,
    doneCount: Int,
    eventCount: Int,
    onFilterSelected: (TodoFilter) -> Unit
) {
    val selectedContainer = when (selectedFilter) {
        TodoFilter.EVENTS -> lerp(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.tertiaryContainer,
            0.64f
        )
        TodoFilter.DONE -> lerp(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.secondaryContainer,
            0.58f
        )
        else -> lerp(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.primaryContainer,
            0.58f
        )
    }
    val selectedContent = when (selectedFilter) {
        TodoFilter.EVENTS -> MaterialTheme.colorScheme.onTertiaryContainer
        TodoFilter.DONE -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val items = listOf(
        TodoFilter.ALL to stringResource(R.string.todos_filter_all_format, allCount),
        TodoFilter.OPEN to stringResource(R.string.todos_filter_open_format, openCount),
        TodoFilter.DONE to stringResource(R.string.todos_filter_done_format, doneCount),
        TodoFilter.EVENTS to stringResource(R.string.todos_filter_events_format, eventCount)
    )
    val selectedIndex = items.indexOfFirst { it.first == selectedFilter }.coerceAtLeast(0)
    val segmentInset = 3.dp
    val barHeight = 28.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val slotWidth = maxWidth / items.size
        val selectedWidth = (slotWidth - 12.dp).coerceAtLeast(52.dp)
        val animatedOffset by animateDpAsState(
            targetValue = slotWidth * selectedIndex + ((slotWidth - selectedWidth) / 2),
            label = "todo_filter_segment_offset"
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .padding(segmentInset)
            ) {
                Surface(
                    modifier = Modifier
                        .offset(x = animatedOffset)
                        .width(selectedWidth)
                        .height(barHeight - (segmentInset * 2)),
                    shape = RoundedCornerShape(12.dp),
                    color = selectedContainer,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {}
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    items.forEach { (filter, label) ->
                        TodoFilterSegment(
                            label = label,
                            selected = filter == selectedFilter,
                            enabled = filter != TodoFilter.OPEN || openCount > 0,
                            selectedContentColor = selectedContent,
                            modifier = Modifier
                                .width(slotWidth)
                                .height(barHeight - (segmentInset * 2)),
                            onClick = { onFilterSelected(filter) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoFilterSegment(
    label: String,
    selected: Boolean,
    selectedContentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .alpha(if (enabled || selected) 1f else 0.45f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
internal fun TodoSectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title $count",
            style = MaterialTheme.typography.labelLarge,
            color = if (title == stringResource(R.string.todos_section_open)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) {
                stringResource(R.string.todos_collapse_section, title)
            } else {
                stringResource(R.string.todos_expand_section, title)
            },
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun TodoDateHeader(
    date: LocalDate,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Text(
                text = date.format(todoDateFormatter),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = date.format(todoWeekdayFormatter),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        val periodLabel = getRelativePeriodLabel(date)
        if (periodLabel != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = periodLabel,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        if (label.isNotBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.metaSmallTextStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = stringResource(R.string.todos_cd_open_day),
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun getRelativePeriodLabel(date: LocalDate): String? {
    val today = LocalDate.now()
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> stringResource(R.string.settings_meaning_today)
        days == 1L -> stringResource(R.string.settings_meaning_tomorrow)
        days == 2L -> stringResource(R.string.settings_meaning_day_after_tomorrow)
        days == -1L -> stringResource(R.string.settings_meaning_yesterday)
        days == -2L -> stringResource(R.string.settings_meaning_day_before_yesterday)
        days in 3L..6L -> pluralStringResource(R.plurals.todos_relative_in_days, days.toInt(), days.toInt())
        days in -6L..-3L -> pluralStringResource(R.plurals.todos_relative_days_ago, Math.abs(days).toInt(), Math.abs(days).toInt())
        days in 7L..13L -> stringResource(R.string.settings_meaning_next_week)
        days in -13L..-7L -> stringResource(R.string.settings_meaning_last_week)
        days in 14L..29L -> pluralStringResource(R.plurals.todos_relative_in_weeks, (days / 7).toInt(), (days / 7).toInt())
        days in -29L..-14L -> pluralStringResource(R.plurals.todos_relative_weeks_ago, Math.abs(days / 7).toInt(), Math.abs(days / 7).toInt())
        days in 30L..59L -> stringResource(R.string.todos_relative_in_one_month)
        days in -59L..-30L -> stringResource(R.string.todos_relative_one_month_ago)
        days >= 60L -> pluralStringResource(R.plurals.todos_relative_in_months, (days / 30).toInt(), (days / 30).toInt())
        days <= -60L -> pluralStringResource(R.plurals.todos_relative_months_ago, Math.abs(days / 30).toInt(), Math.abs(days / 30).toInt())
        else -> null
    }
}

@Composable
internal fun TodoItemCard(
    item: TodoItem,
    renderCheckboxesAsText: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (item.isChecked) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "todo_item_container"
    )
    val textColor by animateColorAsState(
        targetValue = if (item.isChecked) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "todo_item_text"
    )
    val chipColor by animateColorAsState(
        targetValue = if (item.isChecked) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        },
        label = "todo_item_chip_container"
    )
    val chipTextColor by animateColorAsState(
        targetValue = if (item.isChecked) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        label = "todo_item_chip_text"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (item.isChecked) 0.985f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "todo_item_card_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .scale(cardScale),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (renderCheckboxesAsText) {
                        Text(
                            text = if (item.isChecked) "[x]" else "[ ]",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                        )
                    } else {
                        ExpressiveCheckbox(
                            checked = item.isChecked,
                            modifier = Modifier.padding(top = 1.dp),
                            size = 22.dp
                        )
                    }
                    Text(
                        text = MarkdownUtils.parseMarkdown(item.displayText),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = chipColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = chipTextColor
                        )
                        Text(
                            text = "Entry ${item.entryIndex + 1}",
                            style = MaterialTheme.typography.metaSmallTextStyle(),
                            color = chipTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TodoAddFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    FloatingActionButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .size(64.dp)
            .expressiveFabMotion(interactionSource),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Icon(
            imageVector = Icons.Rounded.AddTask,
            contentDescription = stringResource(R.string.todo_cd_add_todo),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun EventItemCard(
    item: EventItem,
    modifier: Modifier = Modifier
) {
    val cardContainer = MaterialTheme.colorScheme.surfaceContainerLow
    val eventChipContainer = lerp(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.tertiaryContainer,
        0.56f
    )
    val metaChipContainer = lerp(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.tertiaryContainer,
        0.38f
    )
    val timeChipContainer = lerp(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        0.5f
    )
    val timeChipContent = MaterialTheme.colorScheme.onTertiaryContainer
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.recordedTime?.takeIf { it.isNotBlank() }?.let { recordedTime ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = timeChipContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = timeChipContent
                            )
                            Text(
                                text = "$recordedTime Hrs",
                                style = MaterialTheme.typography.metaTextStyle().copy(fontWeight = FontWeight.SemiBold),
                                color = timeChipContent
                            )
                        }
                    }
                }
                item.mentionedTime
                    ?.takeIf { it.isNotBlank() && it != item.recordedTime && it != "${item.recordedTime} Hrs" }
                    ?.let { mentionedTime ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = timeChipContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Alarm,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = timeChipContent
                                )
                                Text(
                                text = mentionedTime,
                                style = MaterialTheme.typography.metaTextStyle().copy(fontWeight = FontWeight.SemiBold),
                                color = timeChipContent
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = eventChipContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = stringResource(R.string.addentry_event_chip),
                            style = MaterialTheme.typography.metaTextStyle().copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = MarkdownUtils.parseMarkdown(item.displayText),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = metaChipContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Entry ${item.entryIndex + 1}",
                            style = MaterialTheme.typography.metaSmallTextStyle(),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TodosEmptyState(
    message: String
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
