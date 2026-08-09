package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.rememberAppEntrance
import com.mj.yaja.ui.design.AppScreenReveal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onOpenDrawer: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val entranceTriggered = rememberAppEntrance()
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val helpSections = defaultHelpSections()
    val onboardingCards = defaultHelpOnboardingCards()
    val groupedHelpSections = remember(helpSections) {
        helpSectionGroupOrder.mapNotNull { group ->
            val sections = helpSections.filter { it.group == group }
            sections.takeIf { it.isNotEmpty() }?.let { group to it }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.help_screen_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    com.mj.yaja.ui.components.AnimatedMenuButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        AppScreenReveal(
            visible = true,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
            ) {
            item {
                AppStaggeredEntrance(
                    visible = entranceTriggered,
                    index = 0,
                    strength = AppEntranceStrength.HERO
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.help_welcome_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = stringResource(R.string.help_welcome_body),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }

            item {
                AppStaggeredEntrance(
                    visible = entranceTriggered,
                    index = 1
                ) {
                    HelpOnboardingCards(cards = onboardingCards)
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            groupedHelpSections.forEachIndexed { groupIndex, groupEntry ->
                val groupTitle = groupEntry.first
                val sections = groupEntry.second

                item(key = "group_$groupTitle") {
                    AppStaggeredEntrance(
                        visible = entranceTriggered,
                        index = groupIndex + 2
                    ) {
                        HelpGroupHeader(groupId = groupTitle)
                    }
                }

                itemsIndexed(
                    sections,
                    key = { _, section -> "${groupTitle}_${section.title}" }
                ) { sectionIndex, section ->
                    val staggerIndex =
                        2 + groupedHelpSections.take(groupIndex).sumOf { it.second.size + 1 } + sectionIndex
                    AppStaggeredEntrance(
                        visible = entranceTriggered,
                        index = staggerIndex
                    ) {
                        CollapsibleHelpCard(
                            icon = section.icon,
                            title = section.title,
                            preview = section.preview,
                            content = section.content,
                            expanded = expandedSection == section.title,
                            onToggle = {
                                expandedSection = if (expandedSection == section.title) null else section.title
                            }
                        )
                    }
                }
            }
        }
    }
    }
}
