package com.mj.yaja.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
fun NavigationGesturesSettingsScreen(
        viewModel: JournalViewModel,
        onNavigateBack: () -> Unit
) {
        val showStatistics by viewModel.showStatistics.collectAsStateWithLifecycle()
        val showLookbackInNavBar by viewModel.showLookbackInNavBar.collectAsStateWithLifecycle()
        val showKeywordsInNavBar by viewModel.showKeywordsInNavBar.collectAsStateWithLifecycle()
        val showTodosInNavBar by viewModel.showTodosInNavBar.collectAsStateWithLifecycle()
        val showStatisticsInNavBar by viewModel.showStatisticsInNavBar.collectAsStateWithLifecycle()
        val navigationChromeMode by viewModel.navigationChromeMode.collectAsStateWithLifecycle()
        val showBottomPanelLabels by viewModel.showBottomPanelLabels.collectAsStateWithLifecycle()
        val adaptiveBottomNav by viewModel.adaptiveBottomNav.collectAsStateWithLifecycle()
        val swipeToNavigateDatesEnabled by
                viewModel.swipeToNavigateDatesEnabled.collectAsStateWithLifecycle()
        val enableDragAndDrop by viewModel.enableDragAndDrop.collectAsStateWithLifecycle()
        val entryDeleteSelectionEnabled by
                viewModel.entryDeleteSelectionEnabled.collectAsStateWithLifecycle()

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                stringResource(
                                                        R.string.settings_section_navigation_gestures
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
                                NavigationSection(
                                        navigationChromeMode = navigationChromeMode,
                                        onNavigationChromeModeChange = {
                                                viewModel.setNavigationChromeMode(it)
                                        },
                                        showBottomPanelLabels = showBottomPanelLabels,
                                        onShowBottomPanelLabelsChange = {
                                                viewModel.setShowBottomPanelLabels(it)
                                        },
                                        adaptiveBottomNav = adaptiveBottomNav,
                                        onAdaptiveBottomNavChange = {
                                                viewModel.setAdaptiveBottomNav(it)
                                        },
                                        showLookbackInNavBar = showLookbackInNavBar,
                                        onShowLookbackChange = {
                                                viewModel.setShowLookbackInNavBar(it)
                                        },
                                        showKeywordsInNavBar = showKeywordsInNavBar,
                                        onShowKeywordsChange = {
                                                viewModel.setShowKeywordsInNavBar(it)
                                        },
                                        showTodosInNavBar = showTodosInNavBar,
                                        onShowTodosChange = {
                                                viewModel.setShowTodosInNavBar(it)
                                        },
                                        showStatistics = showStatistics,
                                        showStatisticsInNavBar = showStatisticsInNavBar,
                                        onShowStatisticsInNavBarChange = {
                                                viewModel.setShowStatisticsInNavBar(it)
                                        }
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                GesturesSection(
                                        entryDeleteSelectionEnabled = entryDeleteSelectionEnabled,
                                        onEntryDeleteSelectionEnabledChange = {
                                                viewModel.setEntryDeleteSelectionEnabled(it)
                                        },
                                        swipeToNavigateDatesEnabled = swipeToNavigateDatesEnabled,
                                        onSwipeToNavigateDatesEnabledChange = {
                                                viewModel.setSwipeToNavigateDatesEnabled(it)
                                        },
                                        enableDragAndDrop = enableDragAndDrop,
                                        onEnableDragAndDropChange = {
                                                viewModel.setEnableDragAndDrop(it)
                                        }
                                )
                        }
                }
        }
}
