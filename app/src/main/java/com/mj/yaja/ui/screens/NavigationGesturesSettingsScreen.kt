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
        val uiState by viewModel.navigationGesturesSettingsUiState.collectAsStateWithLifecycle()

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
                                        navigationChromeMode = uiState.navigationChromeMode,
                                        onNavigationChromeModeChange = {
                                                viewModel.setNavigationChromeMode(it)
                                        },
                                        showBottomPanelLabels = uiState.showBottomPanelLabels,
                                        onShowBottomPanelLabelsChange = {
                                                viewModel.setShowBottomPanelLabels(it)
                                        },
                                        adaptiveBottomNav = uiState.adaptiveBottomNav,
                                        onAdaptiveBottomNavChange = {
                                                viewModel.setAdaptiveBottomNav(it)
                                        },
                                        showLookbackInNavBar = uiState.showLookbackInNavBar,
                                        onShowLookbackChange = {
                                                viewModel.setShowLookbackInNavBar(it)
                                        },
                                        showKeywordsInNavBar = uiState.showKeywordsInNavBar,
                                        onShowKeywordsChange = {
                                                viewModel.setShowKeywordsInNavBar(it)
                                        },
                                        showTodosInNavBar = uiState.showTodosInNavBar,
                                        onShowTodosChange = {
                                                viewModel.setShowTodosInNavBar(it)
                                        },
                                        showStatistics = uiState.showStatistics,
                                        showStatisticsInNavBar = uiState.showStatisticsInNavBar,
                                        onShowStatisticsInNavBarChange = {
                                                viewModel.setShowStatisticsInNavBar(it)
                                        }
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                GesturesSection(
                                        entryDeleteSelectionEnabled = uiState.entryDeleteSelectionEnabled,
                                        onEntryDeleteSelectionEnabledChange = {
                                                viewModel.setEntryDeleteSelectionEnabled(it)
                                        },
                                        swipeToNavigateDatesEnabled = uiState.swipeToNavigateDatesEnabled,
                                        onSwipeToNavigateDatesEnabledChange = {
                                                viewModel.setSwipeToNavigateDatesEnabled(it)
                                        },
                                        enableDragAndDrop = uiState.enableDragAndDrop,
                                        onEnableDragAndDropChange = {
                                                viewModel.setEnableDragAndDrop(it)
                                        }
                                )
                        }
                }
        }
}
