package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance

@Composable
internal fun StatisticsCompareToggleItem(
    entranceTriggered: Boolean,
    compareMode: Boolean,
    onToggle: (Boolean) -> Unit
) {
    AppStaggeredEntrance(
        visible = entranceTriggered,
        index = 2,
        strength = AppEntranceStrength.SUBTLE
    ) {
        CompareModeToggle(
            enabled = compareMode,
            onToggle = onToggle
        )
    }
}

@Composable
internal fun StatisticsCompareSectionItem(
    entranceTriggered: Boolean,
    statisticsComparison: StatisticsComparisonData?
) {
    AppStaggeredEntrance(
        visible = entranceTriggered,
        index = 3,
        strength = AppEntranceStrength.SECTION
    ) {
        statisticsComparison?.let { comparison ->
            StatisticsComparisonSection(comparison = comparison)
        } ?: ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
