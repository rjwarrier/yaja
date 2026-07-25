package com.mj.yaja.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.theme.AppIcon
import com.mj.yaja.ui.theme.AppIconManager
import com.mj.yaja.ui.viewmodel.JournalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
        viewModel: JournalViewModel,
        onNavigateBack: () -> Unit
) {
        val context = LocalContext.current

        val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
        val colorSource by viewModel.colorSource.collectAsStateWithLifecycle()
        val customPalette by viewModel.customPalette.collectAsStateWithLifecycle()
        val themeColorIntensity by viewModel.themeColorIntensity.collectAsStateWithLifecycle()
        val backgroundTintLevel by viewModel.backgroundTintLevel.collectAsStateWithLifecycle()
        val personalThemeSlots by viewModel.personalThemeSlots.collectAsStateWithLifecycle()
        val activePersonalThemeSlotId by viewModel.activePersonalThemeSlotId.collectAsStateWithLifecycle()
        val fontScalePreference by viewModel.fontScalePreference.collectAsStateWithLifecycle()
        val dataFontScalePreference by viewModel.dataFontScalePreference.collectAsStateWithLifecycle()
        val followUiFontScale by viewModel.followUiFontScale.collectAsStateWithLifecycle()
        val appFontFamily by viewModel.appFontFamily.collectAsStateWithLifecycle()
        val monoFontWeight by viewModel.monoFontWeight.collectAsStateWithLifecycle()
        val customFontPath by viewModel.customFontPath.collectAsStateWithLifecycle()
        val customFontName by viewModel.customFontName.collectAsStateWithLifecycle()
        val fabPlacement by viewModel.fabPlacement.collectAsStateWithLifecycle()
        var selectedAppIcon by remember { mutableStateOf(AppIconManager.getSelectedIcon(context)) }
        var pendingAppIcon by remember { mutableStateOf<AppIcon?>(null) }

        // Font files come through with inconsistent MIME types across file managers,
        // so accept everything and validate the bytes after picking.
        val customFontLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
        ) { uri -> if (uri != null) viewModel.setCustomFontFromUri(uri, context) }

        val scrollState = rememberScrollState()

        LaunchedEffect(followUiFontScale) {
                if (!followUiFontScale) {
                        // Wait for AnimatedVisibility to start expanding, then scroll to the bottom
                        kotlinx.coroutines.delay(200)
                        scrollState.animateScrollTo(scrollState.maxValue)
                }
        }

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = { Text(stringResource(R.string.settings_appearance), color = MaterialTheme.colorScheme.primary) },
                                navigationIcon = {
                                        IconButton(onClick = onNavigateBack) {
                                                Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                        contentDescription = stringResource(R.string.action_back)
                                                )
                                        }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                )
                        )
                },
                containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
                AppScreenReveal(visible = true, modifier = Modifier.fillMaxSize()) {
                        Column(
                                modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                                        .padding(horizontal = 20.dp)
                                        .verticalScroll(scrollState)
                        ) {
                                AppearanceSection(
                                        themePreference = themePreference,
                                        onThemeSelected = { viewModel.setThemePreference(it) },
                                        colorSource = colorSource,
                                        onColorSourceSelected = { viewModel.setColorSource(it) },
                                        customPalette = customPalette,
                                        onCustomPaletteSelected = { viewModel.setCustomPalette(it) },
                                        themeColorIntensity = themeColorIntensity,
                                        onThemeColorIntensitySelected = {
                                                viewModel.setThemeColorIntensity(it)
                                        },
                                        backgroundTintLevel = backgroundTintLevel,
                                        onBackgroundTintLevelSelected = {
                                                viewModel.setBackgroundTintLevel(it)
                                        },
                                        personalThemeSlots = personalThemeSlots,
                                        activePersonalThemeSlotId = activePersonalThemeSlotId,
                                        onActivePersonalThemeSlotSelected = {
                                                viewModel.setActivePersonalThemeSlotId(it)
                                        },
                                        onRenamePersonalThemeSlot = { slotId, name ->
                                                viewModel.renamePersonalThemeSlot(slotId, name)
                                        },
                                        onPersonalThemeHueChange = { slotId, hue ->
                                                viewModel.setPersonalThemeHue(slotId, hue)
                                        },
                                        onPersonalThemeSaturationChange = { slotId, saturation ->
                                                viewModel.setPersonalThemeSaturation(slotId, saturation)
                                        },
                                        onPersonalThemeBrightnessChange = { slotId, brightness ->
                                                viewModel.setPersonalThemeBrightness(slotId, brightness)
                                        },
                                        onPersonalThemeAccentStyleSelected = { slotId, style ->
                                                viewModel.setPersonalThemeAccentStyle(slotId, style)
                                        },
                                        appFontFamily = appFontFamily,
                                        onFontFamilySelected = { viewModel.setAppFontFamily(it) },
                                        monoFontWeight = monoFontWeight,
                                        onMonoFontWeightChange = {
                                                viewModel.setMonoFontWeight(it)
                                        },
                                        customFontPath = customFontPath,
                                        customFontName = customFontName,
                                        onPickCustomFont = {
                                                customFontLauncher.launch(arrayOf("*/*"))
                                        },
                                        onClearCustomFont = {
                                                viewModel.clearCustomFont()
                                        },
                                        fabPlacement = fabPlacement,
                                        onFabPlacementSelected = {
                                                viewModel.setFabPlacement(it)
                                        },
                                        fontScalePreference = fontScalePreference,
                                        onFontScaleSelected = { viewModel.setFontScalePreference(it) },
                                        dataFontScalePreference = dataFontScalePreference,
                                        onDataFontScaleSelected = { viewModel.setDataFontScalePreference(it) },
                                        followUiFontScale = followUiFontScale,
                                        onFollowUiFontScaleChanged = { viewModel.setFollowUiFontScale(it) },
                                        selectedAppIcon = selectedAppIcon,
                                        onAppIconSelected = { icon -> pendingAppIcon = icon }
                                )
                        }
                }
        }

        val iconToConfirm = pendingAppIcon
        if (iconToConfirm != null) {
                AlertDialog(
                        onDismissRequest = { pendingAppIcon = null },
                        title = { Text(stringResource(R.string.settings_app_icon_confirm_title)) },
                        text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Image(
                                                painter = painterResource(id = iconToConfirm.foregroundRes),
                                                contentDescription = stringResource(iconToConfirm.labelRes),
                                                modifier = Modifier.size(64.dp)
                                        )
                                        Text(
                                                text = stringResource(iconToConfirm.labelRes),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                                        )
                                        Text(
                                                text = stringResource(R.string.settings_app_icon_confirm_message),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                        },
                        confirmButton = {
                                TextButton(onClick = {
                                        selectedAppIcon = iconToConfirm
                                        AppIconManager.setSelectedIcon(context, iconToConfirm)
                                        pendingAppIcon = null
                                }) { Text(stringResource(R.string.action_ok)) }
                        },
                        dismissButton = {
                                TextButton(onClick = { pendingAppIcon = null }) {
                                        Text(stringResource(R.string.action_cancel))
                                }
                        }
                )
        }
}
