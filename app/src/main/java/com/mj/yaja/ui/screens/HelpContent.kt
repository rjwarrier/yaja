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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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

internal fun defaultHelpOnboardingCards(): List<HelpOnboardingCard> = listOf(
    HelpOnboardingCard(
        icon = Icons.Rounded.TaskAlt,
        title = "Todos & Events",
        body = "Keep personal reminders, family plans, and little things you do not want to forget inside the same journal flow."
    ),
    HelpOnboardingCard(
        icon = Icons.Rounded.Widgets,
        title = "Widgets",
        body = "Add Heatmap, Quick Capture, Quick Todo, or Todo List widgets from launcher."
    ),
    HelpOnboardingCard(
        icon = Icons.Rounded.History,
        title = "Version History",
        body = "Enable History Files in Settings > Data to keep safe snapshots before risky changes."
    ),
    HelpOnboardingCard(
        icon = Icons.Rounded.Bolt,
        title = "Tasker",
        body = "Use Tasker to quickly add entries, todos, or events when something personal needs to be captured right away."
    )
)

internal fun defaultHelpSections(): List<HelpSection> = listOf(
    HelpSection(
        group = "Everyday",
        icon = Icons.Rounded.Edit,
        title = "Writing & Editor",
        preview = "Templates, markdown headings, todos, events, and flexible draft insertion.",
        content = "+ Tap the floating '+' button to write for the selected day.\n" +
            "+ Use templates like Meeting Note, Travel Day, Health Log, Reflection, Work Log, and more.\n" +
            "+ Templates can be inserted into a blank draft, appended below, used to replace a draft, or inserted at the cursor.\n" +
            "+ Editor chips can quickly insert todos, events, numbered lists, and bullet lists.\n" +
            "+ View Entry and Edit Entry support markdown-style headings such as ### Section."
    ),
    HelpSection(
        group = "Everyday",
        icon = Icons.Rounded.AutoAwesome,
        title = "Post-Write Review",
        preview = "Todo extraction, keyword detection, starring, labels, and revisit setup.",
        content = "+ After writing, Yaja can open a review sheet before save.\n" +
            "+ It can extract todo items, detect People & Places, star the day, set a day label, and schedule revisit dates.\n" +
            "+ You can disable Post-write Review from Settings if you prefer direct save."
    ),
    HelpSection(
        group = "Everyday",
        icon = Icons.Rounded.EventRepeat,
        title = "Revisit & Follow-up Dates",
        preview = "Journal-linked follow-up dates with quick picks and a custom option.",
        content = "+ Add revisit dates with Tomorrow, Next week, Next month, or a custom date.\n" +
            "+ Due revisit items surface on Home and are marked in Calendar and Timeline.\n" +
            "+ This works well for promises, decisions, health checks, and reminders tied to journal context."
    ),
    HelpSection(
        group = "Everyday",
        icon = Icons.Rounded.Swipe,
        title = "Navigation & Gestures",
        preview = "Date movement, selection delete, swipe preferences, and rebuild cache behavior.",
        content = "+ Home supports date navigation with arrows and optional swipe navigation when enabled.\n" +
            "+ Long-press an entry to select it. Tap more entries to select them too, then use the Delete pill near the Add button.\n" +
            "+ Tapping outside selected entries clears selection. Deleted entries can be restored together from the restore countdown.\n" +
            "+ You can rebuild cache when journal files change outside Yaja."
    ),
    HelpSection(
        group = "Everyday",
        icon = Icons.Rounded.Bolt,
        title = "Shortcodes & Quick Capture",
        preview = "Fast personal capture plus reusable dynamic text snippets.",
        content = "+ Create shortcodes to insert repeated text quickly.\n" +
            "+ Dynamic placeholders like {{today:dd-MMM}} and {{now:HH:mm}} work inside shortcodes.\n" +
            "+ Quick Capture helps log something fast without opening the full editor.\n" +
            "+ Quick add can save either a todo or an event for a chosen date from the widget, Todos screen, or Tasker."
    ),
    HelpSection(
        group = "Everyday",
        icon = Icons.Rounded.Bolt,
        title = "Todos & Events",
        preview = "Personal reminders, plans, and noteworthy events grouped by date.",
        content = "+ Todos are written directly in journal entries as [ ] and [x] lines.\n" +
            "+ Events are normal journal entries marked as Event, useful for family plans, appointments, travel, or anything you want to notice later.\n" +
            "+ The Todos screen helps you view open todos, completed todos, and events without turning Yaja into a productivity dashboard.\n" +
            "+ Tap a date header to open that day in Journal.\n" +
            "+ Pull down on Todos to rebuild the todo index if files changed outside Yaja.\n" +
            "+ Show Completed Tasks is remembered, so your preferred view sticks when you leave and return.\n" +
            "+ Todo widgets can show pending tasks, confirm before toggling, hide completed tasks, and optionally auto-hide when nothing is pending."
    ),
    HelpSection(
        group = "Reviews & Insights",
        icon = Icons.Rounded.People,
        title = "People & Places",
        preview = "Keywords, aliases, trends, and ranked connection views.",
        content = "+ Track people and places with aliases and relation labels.\n" +
            "+ Search, filter, and sort the keyword list.\n" +
            "+ Keyword detail pages show recency, mentions over time, co-mentions, and ranked Connections.\n" +
            "+ Match Sensitivity can be adjusted from Settings."
    ),
    HelpSection(
        group = "Reviews & Insights",
        icon = Icons.Rounded.Timeline,
        title = "Timeline & Lookback",
        preview = "Past-day browsing, highlights, Surprise Me, and review entry points.",
        content = "+ Timeline lets you browse past days with filters, year chips, and optional all-date mode.\n" +
            "+ Lookback shows entries from the same day in past years and your starred highlights.\n" +
            "+ Surprise Me opens a random past entry from Lookback.\n" +
            "+ Weekly Review and Monthly Review live inside Lookback."
    ),
    HelpSection(
        group = "Reviews & Insights",
        icon = Icons.Rounded.Insights,
        title = "Statistics & Reviews",
        preview = "Compare mode, template insights, custom visible sections, and summaries.",
        content = "+ Statistics include compare mode, keyword deltas, and Template Insights.\n" +
            "+ You can choose which statistics sections stay visible from inside the Statistics screen.\n" +
            "+ Compare Mode is available for All Time and summarizes month and year deltas.\n" +
            "+ Review pages summarize most active days, top people and places, favorite moments, labels, language split, and streak or gap patterns."
    ),
    HelpSection(
        group = "Advanced Tools",
        icon = Icons.Rounded.Widgets,
        title = "Widgets & Home Surface",
        preview = "Launcher widgets, highlighted keywords, and day-level home stats.",
        content = "+ Writing Heatmap, Quick Capture, Quick Add, and Todo List widgets are available on the launcher.\n" +
            "+ Widget configure screens let you adjust corners, padding, shape, headers, completed-task visibility, and other widget options where supported.\n" +
            "+ The heatmap widget uses more intensity shades for a smoother writing-history view.\n" +
            "+ Home can highlight People & Places inside entries when the setting is enabled.\n" +
            "+ The Home date header can show or hide entry count, words, and character count."
    ),
    HelpSection(
        group = "Reviews & Insights",
        icon = Icons.Rounded.Language,
        title = "Language & Text Analysis",
        preview = "Local language detection for statistics and review insights.",
        content = "+ Yaja detects dominant script or language across entries for insights.\n" +
            "+ Language split appears in Statistics and Reviews.\n" +
            "+ Detection runs locally and does not send journal text out of the device."
    ),
    HelpSection(
        group = "Advanced Tools",
        icon = Icons.Rounded.Security,
        title = "Data, Privacy & Cache",
        preview = "Markdown storage, version history, app log, app lock, and rebuild cache.",
        content = "+ Data Privacy: All personal journal data resides strictly on your physical device and never leaves it at any time. Yaja does not run cloud databases, remote backup servers, or telemetry to track your text.\n" +
            "+ Journal entries are stored as standard Markdown files arranged by year and month.\n" +
            "+ Version History can keep safe snapshots before edits, deletes, labels, and revisit changes.\n" +
            "+ You can choose how many versions to keep and how many days history is retained.\n" +
            "+ App Log records technical events, crash details, and cache activity without storing journal text.\n" +
            "+ Rebuild App Cache lets you refresh app data modularly, including todos and full cache rebuilds.\n" +
            "+ App lock supports PIN and fingerprint.\n" +
            "+ Yaja keeps heavy indexing and stats work deferred where possible so app launch and saves stay responsive."
    )
)

internal val helpSectionGroupOrder = listOf(
    "Everyday",
    "Reviews & Insights",
    "Advanced Tools"
)

@Composable
internal fun HelpGroupHeader(title: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = when (title) {
                "Everyday" -> "Journaling, writing flow, and daily navigation."
                "Reviews & Insights" -> "Lookbacks, trends, people, places, and analysis."
                else -> "Widgets, storage, privacy, backups, and technical tools."
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
            text = "Start Here",
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
                    contentDescription = if (expanded) "Collapse" else "Expand",
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
