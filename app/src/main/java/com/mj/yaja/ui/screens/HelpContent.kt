package com.mj.yaja.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mj.yaja.R

internal data class HelpSection(
    val group: String,
    val icon: ImageVector,
    val title: String,
    val preview: String,
    val content: String
)

internal data class HelpOnboardingCard(
    val icon: ImageVector,
    val title: String,
    val body: String
)

@Composable
internal fun defaultHelpOnboardingCards(): List<HelpOnboardingCard> = listOf(
    HelpOnboardingCard(
        icon = Icons.Rounded.TaskAlt,
        title = stringResource(R.string.help_onboard_todos_events_title),
        body = stringResource(R.string.help_onboard_todos_events_body)
    ),
    HelpOnboardingCard(
        icon = Icons.Rounded.Widgets,
        title = stringResource(R.string.help_onboard_widgets_title),
        body = stringResource(R.string.help_onboard_widgets_body)
    ),
    HelpOnboardingCard(
        icon = Icons.Rounded.History,
        title = stringResource(R.string.help_onboard_version_history_title),
        body = stringResource(R.string.help_onboard_version_history_body)
    ),
    HelpOnboardingCard(
        icon = Icons.Rounded.Bolt,
        title = stringResource(R.string.help_onboard_tasker_title),
        body = stringResource(R.string.help_onboard_tasker_body)
    )
)

@Composable
internal fun defaultHelpSections(): List<HelpSection> = listOf(
    HelpSection(
        group = "everyday",
        icon = Icons.Rounded.Edit,
        title = stringResource(R.string.help_section_writing_editor_title),
        preview = stringResource(R.string.help_section_writing_editor_preview),
        content = stringResource(R.string.help_section_writing_editor_content)
    ),
    HelpSection(
        group = "everyday",
        icon = Icons.Rounded.AutoAwesome,
        title = stringResource(R.string.help_section_post_write_review_title),
        preview = stringResource(R.string.help_section_post_write_review_preview),
        content = stringResource(R.string.help_section_post_write_review_content)
    ),
    HelpSection(
        group = "everyday",
        icon = Icons.Rounded.EventRepeat,
        title = stringResource(R.string.help_section_revisit_followup_title),
        preview = stringResource(R.string.help_section_revisit_followup_preview),
        content = stringResource(R.string.help_section_revisit_followup_content)
    ),
    HelpSection(
        group = "everyday",
        icon = Icons.Rounded.Swipe,
        title = stringResource(R.string.help_section_navigation_gestures_title),
        preview = stringResource(R.string.help_section_navigation_gestures_preview),
        content = stringResource(R.string.help_section_navigation_gestures_content)
    ),
    HelpSection(
        group = "everyday",
        icon = Icons.Rounded.Bolt,
        title = stringResource(R.string.help_section_shortcodes_quick_capture_title),
        preview = stringResource(R.string.help_section_shortcodes_quick_capture_preview),
        content = stringResource(R.string.help_section_shortcodes_quick_capture_content)
    ),
    HelpSection(
        group = "everyday",
        icon = Icons.Rounded.Bolt,
        title = stringResource(R.string.help_section_todos_events_title),
        preview = stringResource(R.string.help_section_todos_events_preview),
        content = stringResource(R.string.help_section_todos_events_content)
    ),
    HelpSection(
        group = "reviews_insights",
        icon = Icons.Rounded.People,
        title = stringResource(R.string.help_section_people_places_title),
        preview = stringResource(R.string.help_section_people_places_preview),
        content = stringResource(R.string.help_section_people_places_content)
    ),
    HelpSection(
        group = "reviews_insights",
        icon = Icons.Rounded.Timeline,
        title = stringResource(R.string.help_section_timeline_lookback_title),
        preview = stringResource(R.string.help_section_timeline_lookback_preview),
        content = stringResource(R.string.help_section_timeline_lookback_content)
    ),
    HelpSection(
        group = "reviews_insights",
        icon = Icons.Rounded.Insights,
        title = stringResource(R.string.help_section_statistics_reviews_title),
        preview = stringResource(R.string.help_section_statistics_reviews_preview),
        content = stringResource(R.string.help_section_statistics_reviews_content)
    ),
    HelpSection(
        group = "advanced_tools",
        icon = Icons.Rounded.Widgets,
        title = stringResource(R.string.help_section_widgets_home_title),
        preview = stringResource(R.string.help_section_widgets_home_preview),
        content = stringResource(R.string.help_section_widgets_home_content)
    ),
    HelpSection(
        group = "reviews_insights",
        icon = Icons.Rounded.Language,
        title = stringResource(R.string.help_section_language_text_analysis_title),
        preview = stringResource(R.string.help_section_language_text_analysis_preview),
        content = stringResource(R.string.help_section_language_text_analysis_content)
    ),
    HelpSection(
        group = "advanced_tools",
        icon = Icons.Rounded.Security,
        title = stringResource(R.string.help_section_data_privacy_cache_title),
        preview = stringResource(R.string.help_section_data_privacy_cache_preview),
        content = stringResource(R.string.help_section_data_privacy_cache_content)
    )
)

internal val helpSectionGroupOrder = listOf(
    "everyday",
    "reviews_insights",
    "advanced_tools"
)

@Composable
internal fun HelpGroupHeader(groupId: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = when (groupId) {
                "everyday" -> stringResource(R.string.help_group_everyday_title)
                "reviews_insights" -> stringResource(R.string.help_group_reviews_title)
                else -> stringResource(R.string.help_group_advanced_title)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = when (groupId) {
                "everyday" -> stringResource(R.string.help_group_everyday_desc)
                "reviews_insights" -> stringResource(R.string.help_group_reviews_desc)
                else -> stringResource(R.string.help_group_advanced_desc)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HelpOnboardingCards(cards: List<HelpOnboardingCard>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.help_start_here),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth - 10.dp) / 2
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                cards.forEach { card ->
                    Surface(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(156.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = card.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = card.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = card.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HelpFooterActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .clip(MaterialTheme.shapes.medium)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                MaterialTheme.shapes.medium
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun CollapsibleHelpCard(
    icon: ImageVector,
    title: String,
    preview: String,
    content: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) {
                        stringResource(R.string.help_cd_collapse)
                    } else {
                        stringResource(R.string.help_cd_expand)
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
