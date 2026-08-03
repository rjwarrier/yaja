package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.yaja.R
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.viewmodel.JournalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalExperienceSettingsScreen(
        viewModel: JournalViewModel,
        onNavigateBack: () -> Unit
) {
        val animationPreference by viewModel.animationPreference.collectAsStateWithLifecycle()
        val isPreviewLimitEnabled by viewModel.isPreviewLimitEnabled.collectAsStateWithLifecycle()
        val previewLimitLength by viewModel.previewLimitLength.collectAsStateWithLifecycle()
        val showTimestamps by viewModel.showTimestamps.collectAsStateWithLifecycle()
        val allowFutureEntries by viewModel.allowFutureEntries.collectAsStateWithLifecycle()
        val firstDayOfWeek by viewModel.firstDayOfWeek.collectAsStateWithLifecycle()
        val dateOrderPreference by viewModel.dateOrderPreference.collectAsStateWithLifecycle()
        val customDateKeywords by viewModel.customDateKeywords.collectAsStateWithLifecycle()
        val showDayHeaderStats by viewModel.showDayHeaderStats.collectAsStateWithLifecycle()
        val renderCheckboxesAsText by viewModel.renderCheckboxesAsText.collectAsStateWithLifecycle()
        val carryForwardTodosEnabled by viewModel.carryForwardTodosEnabled.collectAsStateWithLifecycle()
        val calendarDensityPreference by
                viewModel.calendarDensityPreference.collectAsStateWithLifecycle()
        val fuzzyThreshold by viewModel.fuzzyThreshold.collectAsStateWithLifecycle()
        val entryStyle by viewModel.entryStyle.collectAsStateWithLifecycle()

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                stringResource(
                                                        R.string.settings_section_journal_experience
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                },
                                navigationIcon = {
                                        IconButton(onClick = onNavigateBack) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Rounded.ArrowBack,
                                                        contentDescription =
                                                                stringResource(R.string.action_back)
                                                )
                                        }
                                },
                                colors =
                                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.background
                                        )
                        )
                },
                containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
                AppScreenReveal(visible = true, modifier = Modifier.fillMaxSize()) {
                        Column(
                                modifier =
                                        Modifier
                                                .fillMaxSize()
                                                .padding(paddingValues)
                                                .padding(horizontal = 20.dp)
                                                .verticalScroll(rememberScrollState())
                        ) {
                                JournalExperienceSection(
                                        renderCheckboxesAsText = renderCheckboxesAsText,
                                        onRenderCheckboxesAsTextChange = {
                                                viewModel.setRenderCheckboxesAsText(it)
                                        },
                                        showDayHeaderStats = showDayHeaderStats,
                                        onShowDayHeaderStatsChange = {
                                                viewModel.setShowDayHeaderStats(it)
                                        },
                                        carryForwardTodosEnabled = carryForwardTodosEnabled,
                                        onCarryForwardTodosEnabledChange = {
                                                viewModel.setCarryForwardTodosEnabled(it)
                                        },
                                        entryStyle = entryStyle,
                                        onEntryStyleSelected = { viewModel.setEntryStyle(it) },
                                        calendarDensityPreference = calendarDensityPreference,
                                        onCalendarDensityPreferenceChange = {
                                                viewModel.setCalendarDensityPreference(it)
                                        },
                                        dateOrderPreference = dateOrderPreference,
                                        onDateOrderChange = {
                                                viewModel.setDateOrderPreference(it)
                                        },
                                        animationPreference = animationPreference,
                                        onAnimationPreferenceChange = {
                                                viewModel.setAnimationPreference(it)
                                        },
                                        customDateKeywords = customDateKeywords,
                                        onSetCustomDateKeywords = {
                                                viewModel.setCustomDateKeywords(it)
                                        },
                                        fuzzyThreshold = fuzzyThreshold,
                                        onFuzzyThresholdChange = {
                                                viewModel.setKeywordFuzzyThreshold(it)
                                        },
                                        showTimestamps = showTimestamps,
                                        onShowTimestampsChange = {
                                                viewModel.setShowTimestamps(it)
                                        },
                                        allowFutureEntries = allowFutureEntries,
                                        onAllowFutureEntriesChange = {
                                                viewModel.setAllowFutureEntries(it)
                                        },
                                        isPreviewLimitEnabled = isPreviewLimitEnabled,
                                        onIsPreviewLimitEnabledChange = {
                                                viewModel.setPreviewLimitEnabled(it)
                                        },
                                        previewLimitLength = previewLimitLength,
                                        onPreviewLimitLengthChange = {
                                                viewModel.setPreviewLimitLength(it)
                                        },
                                        firstDayOfWeek = firstDayOfWeek,
                                        onFirstDayOfWeekChange = {
                                                viewModel.setFirstDayOfWeek(it)
                                        }
                                )
                        }
                }
        }
}
