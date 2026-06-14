package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KeywordDetailScreen(
    viewModel: JournalViewModel,
    keywordId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDate: (LocalDate) -> Unit,
    onNavigateToKeyword: (String) -> Unit
) {
    val keywords by viewModel.keywords.collectAsStateWithLifecycle()
    val keywordMatchState by viewModel.keywordMatchState.collectAsStateWithLifecycle()
    val keywordIndexingIds by viewModel.keywordIndexingIds.collectAsStateWithLifecycle()
    val keywordLastIndexedAt by viewModel.keywordLastIndexedAt.collectAsStateWithLifecycle()
    val keyword = keywords.firstOrNull { it.id == keywordId }
    var contentVisible by remember(keywordId) { mutableStateOf(false) }

    LaunchedEffect(keywordId) {
        contentVisible = true
    }

    val stats = remember(keyword, keywords, keywordMatchState) {
        keyword?.let { viewModel.getKeywordStats(it.id) }
    }
    val keywordMatches = remember(keyword?.id, keywordMatchState) {
        keyword?.let { viewModel.getMatchesForKeyword(it.id) } ?: emptyList()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        keyword?.name ?: stringResource(R.string.keyword_detail_title_fallback),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (keyword == null || stats == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.keyword_detail_not_found))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    AppStaggeredEntrance(visible = contentVisible, index = 0) {
                        KeywordHeroCard(
                            name = keyword.name,
                            type = keyword.type,
                            relation = keyword.relation,
                            totalMentions = stats.totalMentions,
                            uniqueDays = stats.uniqueDays,
                            isIndexing = keyword.id in keywordIndexingIds
                        )
                    }
                }

                item {
                    AppStaggeredEntrance(visible = contentVisible, index = 1) {
                        LastSeenCard(
                            lastSeen = stats.lastSeen,
                            firstSeen = stats.firstSeen
                        )
                    }
                }

                item {
                    AppStaggeredEntrance(visible = contentVisible, index = 2) {
                        KeywordTimelineCard(
                            mentionsByMonth = stats.mentionsByMonth,
                            totalMentions = stats.totalMentions,
                            uniqueDays = stats.uniqueDays
                        )
                    }
                }

                keywordLastIndexedAt?.let { lastIndexedAt ->
                    item {
                        AppStaggeredEntrance(visible = contentVisible, index = 3) {
                            Text(
                                text = stringResource(
                                    R.string.keyword_detail_last_indexed,
                                    formatKeywordIndexedAt(lastIndexedAt)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                            )
                        }
                    }
                }

                if (stats.coOccurring.isNotEmpty()) {
                    item {
                        AppStaggeredEntrance(visible = contentVisible, index = 4) {
                            RankedConnectionsCard(
                                coOccurring = stats.coOccurring,
                                onOpenKeyword = onNavigateToKeyword
                            )
                        }
                    }
                }

                if (keywordMatches.isNotEmpty()) {
                    item {
                        AppStaggeredEntrance(visible = contentVisible, index = 5) {
                            Text(
                                text = stringResource(R.string.keyword_detail_mentions_section),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 6.dp, start = 6.dp)
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = keywordMatches,
                    key = { index, match ->
                        "${match.date}-${match.entryIndex}-${match.matchedText}-${match.matchType}-${index}"
                    }
                ) { index, match ->
                    AppStaggeredEntrance(visible = contentVisible, index = index.coerceAtMost(4) + 6) {
                        MatchCard(match = match, onNavigateToDate = onNavigateToDate)
                    }
                }
            }
        }
    }
}
