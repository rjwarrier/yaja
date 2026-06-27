package com.mj.yaja.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.BuildConfig
import com.mj.yaja.R
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.rememberAppEntrance
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.theme.BodoniModaFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onOpenDrawer: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val entranceTriggered = rememberAppEntrance()
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
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

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                AppStaggeredEntrance(
                    visible = entranceTriggered,
                    index = helpSections.size + groupedHelpSections.size + 1,
                    strength = AppEntranceStrength.SUBTLE
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rj_logo),
                                    contentDescription = stringResource(R.string.help_cd_rj_logo),
                                    modifier = Modifier
                                        .size(80.dp)
                                        .padding(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "yaja",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = BodoniModaFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.help_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.help_version_prefix, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.help_from_labs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.help_made_in),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HelpFooterActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Shop,
                                label = stringResource(R.string.help_footer_play_store),
                                onClick = {
                                    uriHandler.openUri("https://play.google.com/store/apps/details?id=com.mj.yaja")
                                }
                            )
                            HelpFooterActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Language,
                                label = stringResource(R.string.help_footer_website),
                                onClick = {
                                    uriHandler.openUri("https://ranjithj.in/yaja/")
                                }
                            )
                            HelpFooterActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Share,
                                label = stringResource(R.string.help_footer_share),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.help_share_yaja_text))
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, context.getString(R.string.help_share_yaja))
                                    )
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HelpFooterActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Code,
                                label = stringResource(R.string.help_footer_github),
                                onClick = {
                                    uriHandler.openUri("https://github.com/rjwarrier/yaja/tree/feature/ui-improvements-and-fixes")
                                }
                            )
                            val clipboardManager = LocalClipboardManager.current
                            HelpFooterActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.AutoMirrored.Rounded.Chat,
                                label = stringResource(R.string.help_footer_discord),
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("medvl_jedi"))
                                    Toast.makeText(context, context.getString(R.string.help_discord_copied), Toast.LENGTH_SHORT).show()
                                }
                            )
                            HelpFooterActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Forum,
                                label = stringResource(R.string.help_footer_reddit),
                                onClick = {
                                    uriHandler.openUri("https://www.reddit.com/r/yaja_journal/")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    }
}
