package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.yaja.data.KeywordMatchCache
import com.mj.yaja.data.KeywordType
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.viewmodel.JournalViewModel
@Composable
internal fun StatisticsSectionContainer(
    section: StatisticsSection,
    entranceTriggered: Boolean,
    entranceIndex: Int,
    haptics: HapticFeedback,
    viewModel: JournalViewModel,
    allTimeStats: AllTimeStatsData,
    datesWithEntries: Set<java.time.LocalDate>,
    heatmapData: Map<java.time.LocalDate, Int>,
    statisticsSettling: Boolean,
    keywordIndexingIds: Set<String>,
    keywordMatchState: KeywordMatchCache.RebuildState,
    keywords: List<com.mj.yaja.data.KeywordDefinition>,
    containerModifier: Modifier,
    dragHandleModifier: Modifier
) {
    AppStaggeredEntrance(
        visible = entranceTriggered,
        index = entranceIndex
    ) {
        Column(
            modifier = containerModifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatisticsSectionHeader(
                section = section,
                haptics = haptics,
                dragHandleModifier = dragHandleModifier
            )
            StatisticsSectionContent(
                section = section,
                viewModel = viewModel,
                allTimeStats = allTimeStats,
                datesWithEntries = datesWithEntries,
                heatmapData = heatmapData,
                statisticsSettling = statisticsSettling,
                keywordIndexingIds = keywordIndexingIds,
                keywordMatchState = keywordMatchState,
                keywords = keywords
            )
        }
    }
}

@Composable
private fun StatisticsSectionHeader(
    section: StatisticsSection,
    haptics: HapticFeedback,
    dragHandleModifier: Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            section.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = Icons.Rounded.DragIndicator,
            contentDescription = "Long press to reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = dragHandleModifier
        )
    }
}

@Composable
private fun StatisticsSectionContent(
    section: StatisticsSection,
    viewModel: JournalViewModel,
    allTimeStats: AllTimeStatsData,
    datesWithEntries: Set<java.time.LocalDate>,
    heatmapData: Map<java.time.LocalDate, Int>,
    statisticsSettling: Boolean,
    keywordIndexingIds: Set<String>,
    keywordMatchState: KeywordMatchCache.RebuildState,
    keywords: List<com.mj.yaja.data.KeywordDefinition>
) {
    when (section) {
        StatisticsSection.WRITING_INSIGHTS -> WritingInsightsCard(stats = allTimeStats)
        StatisticsSection.DISTRIBUTION -> WritingDistributionCard(stats = allTimeStats)
        StatisticsSection.WHEN_YOU_WRITE -> WritingTimeCard(dist = allTimeStats.writingTimeDistribution)
        StatisticsSection.MONTHLY_ACTIVITY -> MonthlyActivityChart(trend = allTimeStats.monthlyEntryTrend)
        StatisticsSection.HEATMAP -> EntryHeatmap(
            datesWithEntries = datesWithEntries,
            entryLengthMap = heatmapData,
            isSettling = statisticsSettling && heatmapData.isEmpty()
        )
        StatisticsSection.LANGUAGES -> {
            val useMLKit by viewModel.useMLKitDetection.collectAsStateWithLifecycle()
            LanguagesCard(
                distribution = allTimeStats.languageDistribution,
                useMLKitDetection = useMLKit,
                onToggleMLKit = { viewModel.setUseMLKitDetection(it) },
                isSettling = statisticsSettling && allTimeStats.languageDistribution.isEmpty()
            )
        }
        StatisticsSection.PEOPLE_PLACES -> KeywordStatsSection(
            topPeople = viewModel.getTopKeywords(KeywordType.PERSON, 3),
            topPlaces = viewModel.getTopKeywords(KeywordType.PLACE, 3),
            peopleKeywordCount = keywords.count { it.type == KeywordType.PERSON },
            placeKeywordCount = keywords.count { it.type == KeywordType.PLACE },
            isSettling =
                keywordIndexingIds.isNotEmpty() ||
                    keywordMatchState is KeywordMatchCache.RebuildState.Rebuilding
        )
        StatisticsSection.TEMPLATE_INSIGHTS -> {
            allTimeStats.templateInsights?.let {
                TemplateInsightsCard(insights = it)
            }
        }
    }
}
