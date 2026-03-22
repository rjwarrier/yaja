package com.mj.yaja.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.mj.yaja.data.AppFontFamily
import com.mj.yaja.data.FontScalePreference
import com.mj.yaja.data.SwipeDirection
import com.mj.yaja.data.ThemePreference
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit,
        onNavigateBack: () -> Unit,
        onNavigateToPinSetup: () -> Unit = {},
        onNavigateToPinDisable: () -> Unit,
        onNavigateToHelp: () -> Unit,
        onNavigateToShortcodes: () -> Unit,
        onNavigateToJournal: () -> Unit,
        onNavigateToCalendar: () -> Unit,
        onNavigateToLookback: () -> Unit,
        onNavigateToGestures: () -> Unit = {}
) {
        val themePreference by viewModel.themePreference.collectAsState()
        val fontScalePreference by viewModel.fontScalePreference.collectAsState()
        val isPreviewLimitEnabled by viewModel.isPreviewLimitEnabled.collectAsState()
        val previewLimitLength by viewModel.previewLimitLength.collectAsState()
        val storageUriString by viewModel.storageUri.collectAsState()
        val showTimestamps by viewModel.showTimestamps.collectAsState()
        val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsState()
        val firstDayOfWeek by viewModel.firstDayOfWeek.collectAsState()
        val isPinEnabled by viewModel.isPinEnabled.collectAsState()
        val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
        val showStatistics by viewModel.showStatistics.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
                viewModel.toastEvents.collect { message ->
                        android.widget.Toast.makeText(
                                        context,
                                        message,
                                        android.widget.Toast.LENGTH_SHORT
                                )
                                .show()
                }
        }

        val formattedBackupDate =
                remember(lastBackupTimestamp) {
                        if (lastBackupTimestamp == 0L) {
                                "Never"
                        } else {
                                val instant = java.time.Instant.ofEpochMilli(lastBackupTimestamp)
                                val dateTime =
                                        java.time.LocalDateTime.ofInstant(
                                                instant,
                                                java.time.ZoneId.systemDefault()
                                        )
                                val formatter =
                                        java.time.format.DateTimeFormatter.ofPattern(
                                                "dd-MMM-yy HH:mm 'hrs'"
                                        )
                                dateTime.format(formatter)
                        }
                }

        var pendingUriString by remember { mutableStateOf<String?>(null) }
        var showDialog by remember { mutableStateOf(false) }

        val confirmLocationChange = { uriString: String? ->
                pendingUriString = uriString
                showDialog = true
        }

        val launcher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri
                        ->
                        if (uri != null) {
                                val contentResolver = context.contentResolver
                                val takeFlags: Int =
                                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                android.content.Intent
                                                        .FLAG_GRANT_WRITE_URI_PERMISSION
                                contentResolver.takePersistableUriPermission(uri, takeFlags)
                                val newUriString = uri.toString()
                                if (newUriString != storageUriString) {
                                        confirmLocationChange(newUriString)
                                }
                        }
                }

        if (showDialog) {
                val destName =
                        if (pendingUriString == null) "App Internal Storage"
                        else
                                pendingUriString?.toUri()?.path?.substringAfterLast(":")
                                        ?: "the new folder"
                val currentName =
                        if (storageUriString == null) "App Internal Storage"
                        else
                                storageUriString?.toUri()?.path?.substringAfterLast(":")
                                        ?: "the current folder"

                AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Change Storage Location") },
                        text = {
                                Text(
                                        "All existing entries will be moved from $currentName to $destName. Do you want to continue?"
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                viewModel.setStorageUri(pendingUriString)
                                                showDialog = false
                                        }
                                ) { Text("Yes") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showDialog = false }) { Text("No") }
                        }
                )
        }

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                "Settings",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                },
                                navigationIcon = {
                                        IconButton(onClick = onNavigateBack) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Rounded
                                                                        .ArrowBack,
                                                        contentDescription = "Back"
                                                )
                                        }
                                },
                                actions = {
                                        IconButton(onClick = onNavigateToGestures) {
                                                Icon(
                                                        imageVector = Icons.Rounded.Fingerprint,
                                                        contentDescription = "Gestures"
                                                )
                                        }
                                },
                                colors =
                                        TopAppBarDefaults.topAppBarColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.background,
                                                titleContentColor =
                                                        MaterialTheme.colorScheme.primary,
                                                navigationIconContentColor =
                                                        MaterialTheme.colorScheme.onSurface,
                                                actionIconContentColor =
                                                        MaterialTheme.colorScheme.onSurface
                                        )
                        )
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
        ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        Column(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .padding(horizontal = 20.dp)
                                                .verticalScroll(rememberScrollState())
                        ) {
                                Spacer(modifier = Modifier.height(16.dp))

                                // ── Appearance Section ──
                                SettingsSectionHeader(
                                        icon = Icons.Rounded.Palette,
                                        title = "Appearance"
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                        text = "Theme",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                val options =
                                        listOf(
                                                ThemePreference.SYSTEM to "System Default",
                                                ThemePreference.LIGHT to "Light",
                                                ThemePreference.DARK to "Dark",
                                                ThemePreference.AMOLED to "Amoled Black"
                                        )

                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        options.forEach { (preference, label) ->
                                                ThemeSelectionCard(
                                                        modifier = Modifier.weight(1f),
                                                        label = label,
                                                        isSelected = themePreference == preference,
                                                        onClick = {
                                                                viewModel.setThemePreference(
                                                                        preference
                                                                )
                                                        },
                                                        preference = preference
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                        text = "Font",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                )

                                val appFontFamily by viewModel.appFontFamily.collectAsState()
                                val fontOptions =
                                        listOf(
                                                AppFontFamily.SANS_SERIF to "Sans-Serif",
                                                AppFontFamily.SERIF to "Serif",
                                                AppFontFamily.MONO to "Mono"
                                        )

                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        fontOptions.forEach { (font, label) ->
                                                FontSelectionCard(
                                                        modifier = Modifier.weight(1f),
                                                        label = label,
                                                        isSelected = appFontFamily == font,
                                                        onClick = {
                                                                viewModel.setAppFontFamily(font)
                                                        },
                                                        fontFamily = font
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                        text = "Font Size",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.elevatedCardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceContainerLow
                                                ),
                                        elevation =
                                                CardDefaults.elevatedCardElevation(
                                                        defaultElevation = 0.dp
                                                ),
                                        shape = MaterialTheme.shapes.medium
                                ) {
                                        Column(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 20.dp,
                                                                        vertical = 16.dp
                                                                ),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                                val sliderValue =
                                                        when (fontScalePreference) {
                                                                FontScalePreference.SMALLER -> 0f
                                                                FontScalePreference.NORMAL -> 1f
                                                                FontScalePreference.LARGER -> 2f
                                                        }

                                                Slider(
                                                        value = sliderValue,
                                                        onValueChange = { value ->
                                                                val preference =
                                                                        when (value.toInt()) {
                                                                                0 ->
                                                                                        FontScalePreference
                                                                                                .SMALLER
                                                                                1 ->
                                                                                        FontScalePreference
                                                                                                .NORMAL
                                                                                else ->
                                                                                        FontScalePreference
                                                                                                .LARGER
                                                                        }
                                                                viewModel.setFontScalePreference(
                                                                        preference
                                                                )
                                                        },
                                                        valueRange = 0f..2f,
                                                        steps = 1,
                                                        colors =
                                                                SliderDefaults.colors(
                                                                        thumbColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                        activeTrackColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                        inactiveTrackColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceVariant,
                                                                        activeTickColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onPrimary
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.4f
                                                                                        ),
                                                                        inactiveTickColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.4f
                                                                                        )
                                                                )
                                                )

                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(horizontal = 4.dp),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween
                                                ) {
                                                        Text(
                                                                "A",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                        Text(
                                                                "Normal",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                        Text(
                                                                "A",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                // ── Preferences Section ──
                                SettingsSectionHeader(
                                        icon = Icons.Rounded.Settings,
                                        title = "Preferences"
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Spacer(modifier = Modifier.height(4.dp))

                                ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.elevatedCardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceContainerLow
                                                ),
                                        elevation =
                                                CardDefaults.elevatedCardElevation(
                                                        defaultElevation = 0.dp
                                                ),
                                        shape = MaterialTheme.shapes.medium
                                ) {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 8.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Column {
                                                        Text(
                                                                text = "Show Timestamps",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        Text(
                                                                text =
                                                                        "Display entry time on the timeline",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                                Switch(
                                                        checked = showTimestamps,
                                                        onCheckedChange = {
                                                                viewModel.setShowTimestamps(it)
                                                        }
                                                )
                                        }

                                        HorizontalDivider(
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                        )

                                        val allowFutureEntries by
                                                viewModel.allowFutureEntries.collectAsState()
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 8.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Column(Modifier.weight(1f)) {
                                                        Text(
                                                                text = "Allow Future Entries",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        Text(
                                                                text =
                                                                        "Enable adding entries to future dates",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                                Switch(
                                                        checked = allowFutureEntries,
                                                        onCheckedChange = {
                                                                viewModel.setAllowFutureEntries(it)
                                                        }
                                                )
                                        }


                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 8.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Column(Modifier.weight(1f)) {
                                                        Text(
                                                                text = "Truncate Long Entries",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        Text(
                                                                text =
                                                                        "Shorten long entries on the home screen",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                                Switch(
                                                        checked = isPreviewLimitEnabled,
                                                        onCheckedChange = {
                                                                viewModel.setPreviewLimitEnabled(it)
                                                        }
                                                )
                                        }

                                        androidx.compose.animation.AnimatedVisibility(
                                                visible = isPreviewLimitEnabled
                                        ) {
                                                Column {
                                                        HorizontalDivider(
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceVariant
                                                        )
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                                Text(
                                                                        text = "Character Limit",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyLarge,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurface
                                                                )
                                                                Text(
                                                                        text =
                                                                                "Set how many characters to display before truncating",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodySmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )

                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        8.dp
                                                                                )
                                                                )

                                                                var _sliderValue by
                                                                        remember(
                                                                                previewLimitLength
                                                                        ) {
                                                                                mutableStateOf(
                                                                                        previewLimitLength
                                                                                                .toFloat()
                                                                                )
                                                                        }

                                                                Slider(
                                                                        value = _sliderValue,
                                                                        onValueChange = {
                                                                                _sliderValue = it
                                                                        },
                                                                        onValueChangeFinished = {
                                                                                viewModel
                                                                                        .setPreviewLimitLength(
                                                                                                _sliderValue
                                                                                                        .toInt()
                                                                                        )
                                                                        },
                                                                        valueRange = 50f..200f,
                                                                        steps = 14,
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        colors =
                                                                                SliderDefaults
                                                                                        .colors(
                                                                                                thumbColor =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primary,
                                                                                                activeTrackColor =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primary,
                                                                                                inactiveTrackColor =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .surfaceVariant
                                                                                        )
                                                                )

                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceBetween
                                                                ) {
                                                                        Text(
                                                                                "50 chars",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .labelSmall,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant
                                                                        )
                                                                        Text(
                                                                                "${_sliderValue.toInt()} characters",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .labelMedium,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        )
                                                                        Text(
                                                                                "200 chars",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .labelSmall,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant
                                                                        )
                                                                }
                                                        }
                                                }
                                        }

                                        HorizontalDivider(
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                        )

                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 8.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Column(Modifier.weight(1f)) {
                                                        Text(
                                                                text = "First Day of Week",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        Text(
                                                                text = "Start calendar on",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }

                                                // A simple Segmented-Control-like layout using Row
                                                // and OutlinedButton or
                                                // basic TextButtons
                                                Row(
                                                        modifier =
                                                                Modifier.background(
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceVariant,
                                                                                MaterialTheme.shapes
                                                                                        .small
                                                                        )
                                                                        .padding(2.dp),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Box(
                                                                modifier =
                                                                        Modifier.background(
                                                                                        if (firstDayOfWeek ==
                                                                                                        DayOfWeek
                                                                                                                .SUNDAY
                                                                                        )
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .primaryContainer
                                                                                        else
                                                                                                androidx.compose
                                                                                                        .ui
                                                                                                        .graphics
                                                                                                        .Color
                                                                                                        .Transparent,
                                                                                        shape =
                                                                                                MaterialTheme
                                                                                                        .shapes
                                                                                                        .small
                                                                                )
                                                                                .clickable {
                                                                                        viewModel
                                                                                                .setFirstDayOfWeek(
                                                                                                        DayOfWeek
                                                                                                                .SUNDAY
                                                                                                )
                                                                                }
                                                                                .padding(
                                                                                        horizontal =
                                                                                                12.dp,
                                                                                        vertical =
                                                                                                6.dp
                                                                                )
                                                        ) {
                                                                Text(
                                                                        "Sun",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelLarge,
                                                                        color =
                                                                                if (firstDayOfWeek ==
                                                                                                DayOfWeek
                                                                                                        .SUNDAY
                                                                                )
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onPrimaryContainer
                                                                                else
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant
                                                                )
                                                        }

                                                        Box(
                                                                modifier =
                                                                        Modifier.background(
                                                                                        if (firstDayOfWeek ==
                                                                                                        DayOfWeek
                                                                                                                .MONDAY
                                                                                        )
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .primaryContainer
                                                                                        else
                                                                                                androidx.compose
                                                                                                        .ui
                                                                                                        .graphics
                                                                                                        .Color
                                                                                                        .Transparent,
                                                                                        shape =
                                                                                                MaterialTheme
                                                                                                        .shapes
                                                                                                        .small
                                                                                )
                                                                                .clickable {
                                                                                        viewModel
                                                                                                .setFirstDayOfWeek(
                                                                                                        DayOfWeek
                                                                                                                .MONDAY
                                                                                                )
                                                                                }
                                                                                .padding(
                                                                                        horizontal =
                                                                                                12.dp,
                                                                                        vertical =
                                                                                                6.dp
                                                                                )
                                                        ) {
                                                                Text(
                                                                        "Mon",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelLarge,
                                                                        color =
                                                                                if (firstDayOfWeek ==
                                                                                                DayOfWeek
                                                                                                        .MONDAY
                                                                                )
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onPrimaryContainer
                                                                                else
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant
                                                                )
                                                        }
                                                }
                                        }

                                        HorizontalDivider(
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                        )

                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 8.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Column(Modifier.weight(1f)) {
                                                        Text(
                                                                text = "Show Statistics",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        Text(
                                                                text =
                                                                        "Display statistics button in menu",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                                Switch(
                                                        checked = showStatistics,
                                                        onCheckedChange = {
                                                                viewModel.setShowStatistics(it)
                                                        }
                                                )
                                        }

                                }


                                // ── Data & Storage Section ──
                                SettingsSectionHeader(
                                        icon = Icons.Rounded.Storage,
                                        title = "Data & Storage"
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                        text = "Data Cache Sync",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val swipeToSyncEnabled by
                                        viewModel.swipeToSyncEnabled.collectAsState()
                                ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.elevatedCardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceContainerLow
                                                ),
                                        elevation =
                                                CardDefaults.elevatedCardElevation(
                                                        defaultElevation = 0.dp
                                                ),
                                        shape = MaterialTheme.shapes.medium
                                ) {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 8.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Column(Modifier.weight(1f)) {
                                                        Text(
                                                                text =
                                                                        "Swipe down on home screen to sync",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                }
                                                Switch(
                                                        checked = swipeToSyncEnabled,
                                                        onCheckedChange = {
                                                                viewModel.setSwipeToSyncEnabled(it)
                                                        }
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                        text = "Storage Location",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.elevatedCardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceContainerLow
                                                ),
                                        elevation =
                                                CardDefaults.elevatedCardElevation(
                                                        defaultElevation = 0.dp
                                                ),
                                        shape = MaterialTheme.shapes.medium
                                ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                                val locationText =
                                                        if (storageUriString == null) {
                                                                "App Internal Storage (Default)"
                                                        } else {
                                                                "Custom Folder:\n" +
                                                                        storageUriString
                                                                                ?.toUri()
                                                                                ?.path
                                                                                ?.substringAfterLast(
                                                                                        ":"
                                                                                )
                                                        }

                                                Text(
                                                        text = locationText,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End
                                                ) {
                                                        if (storageUriString != null) {
                                                                TextButton(
                                                                        onClick = {
                                                                                confirmLocationChange(
                                                                                        null
                                                                                )
                                                                        }
                                                                ) { Text("Reset to Default") }
                                                        }
                                                        TextButton(
                                                                onClick = { launcher.launch(null) }
                                                        ) { Text("Choose Folder") }
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                        text = "Backup",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.elevatedCardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceContainerLow
                                                ),
                                        elevation =
                                                CardDefaults.elevatedCardElevation(
                                                        defaultElevation = 0.dp
                                                ),
                                        shape = MaterialTheme.shapes.medium
                                ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                        text =
                                                                "Create a compressed zip file containing all your journal entries to share or save elsewhere.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text =
                                                                        "Last Backup: $formattedBackupDate",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primary,
                                                                fontWeight = FontWeight.SemiBold
                                                        )
                                                        TextButton(
                                                                onClick = {
                                                                        viewModel.backupData(
                                                                                context
                                                                        )
                                                                }
                                                        ) { Text("Backup Now") }
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                        text = "Cache Management",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.elevatedCardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceContainerLow
                                                ),
                                        elevation =
                                                CardDefaults.elevatedCardElevation(
                                                        defaultElevation = 0.dp
                                                ),
                                        shape = MaterialTheme.shapes.medium
                                ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                        text =
                                                                "Reload all entries from storage to refresh the in-memory cache. Useful if filesystem changes aren't appearing.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        TextButton(
                                                                onClick = {
                                                                        viewModel.refreshCache()
                                                                },
                                                                contentPadding =
                                                                        PaddingValues(
                                                                                horizontal = 16.dp
                                                                        )
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Rounded
                                                                                        .Refresh,
                                                                        contentDescription = null,
                                                                        modifier =
                                                                                Modifier.size(18.dp)
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(8.dp)
                                                                )
                                                                Text("Sync Data Cache")
                                                        }
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                // ── Security Section ──
                                SettingsSectionHeader(icon = Icons.Rounded.Lock, title = "Security")

                                Spacer(modifier = Modifier.height(12.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.elevatedCardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceContainerLow
                                                ),
                                        elevation =
                                                CardDefaults.elevatedCardElevation(
                                                        defaultElevation = 0.dp
                                                ),
                                        shape = MaterialTheme.shapes.medium
                                ) {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 16.dp,
                                                                        vertical = 12.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Rounded.Lock,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                                text = "PIN Lock",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        Text(
                                                                text =
                                                                        if (isPinEnabled)
                                                                                "App is locked"
                                                                        else "App is unlocked",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                                Switch(
                                                        checked = isPinEnabled,
                                                        onCheckedChange = { enabled ->
                                                                if (enabled) onNavigateToPinSetup()
                                                                else onNavigateToPinDisable()
                                                        }
                                                )
                                        }
                                        if (isPinEnabled) {
                                                HorizontalDivider(
                                                        modifier =
                                                                Modifier.padding(horizontal = 16.dp)
                                                )
                                                TextButton(
                                                        onClick = onNavigateToPinSetup,
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(horizontal = 8.dp)
                                                ) { Text("Change PIN") }
                                        }
                                }

                                // Biometric unlock option (only shown if PIN is enabled)
                                if (isPinEnabled) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        ElevatedCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                        CardDefaults.elevatedCardColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceContainerLow
                                                        ),
                                                elevation =
                                                        CardDefaults.elevatedCardElevation(
                                                                defaultElevation = 0.dp
                                                        ),
                                                shape = MaterialTheme.shapes.medium
                                        ) {
                                                Column(
                                                        modifier = Modifier.fillMaxWidth()
                                                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                                ) {
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                                Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.weight(1f)
                                                                ) {
                                                                        Icon(
                                                                                imageVector = Icons.Rounded.Fingerprint,
                                                                                contentDescription = null,
                                                                                tint = MaterialTheme.colorScheme.primary,
                                                                                modifier = Modifier.size(24.dp)
                                                                        )
                                                                        Spacer(modifier = Modifier.width(12.dp))
                                                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                                Text(
                                                                                        text = "Biometric Unlock",
                                                                                        style =
                                                                                                MaterialTheme.typography
                                                                                                        .bodyLarge,
                                                                                        color =
                                                                                                MaterialTheme.colorScheme
                                                                                                        .onSurface
                                                                                )
                                                                                Text(
                                                                                        text =
                                                                                                if (isBiometricEnabled)
                                                                                                        "Fingerprint or Face ID enabled"
                                                                                                else "Use biometric instead of PIN",
                                                                                        style =
                                                                                                MaterialTheme.typography
                                                                                                        .bodySmall,
                                                                                        color =
                                                                                                MaterialTheme.colorScheme
                                                                                                        .onSurfaceVariant
                                                                                )
                                                                        }
                                                                }
                                                                Switch(
                                                                        checked = isBiometricEnabled,
                                                                        onCheckedChange = { enabled ->
                                                                                try {
                                                                                        if (enabled) viewModel.enableBiometric()
                                                                                        else viewModel.disableBiometric()
                                                                                } catch (e: Exception) {
                                                                                        Log.e("SettingsScreen", "Failed to toggle biometric", e)
                                                                                }
                                                                        }
                                                                )
                                                        }

                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        HorizontalDivider()

                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.Top
                                                        ) {
                                                                Icon(
                                                                        imageVector = Icons.Rounded.Info,
                                                                        contentDescription = null,
                                                                        tint =
                                                                                MaterialTheme.colorScheme
                                                                                        .onSurfaceVariant,
                                                                        modifier = Modifier.size(14.dp).padding(top = 1.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(
                                                                        text =
                                                                                "PIN lock prevents unauthorized access to the app but does not encrypt your journal data on disk. Someone with direct file access could still read your entries.",
                                                                        style =
                                                                                MaterialTheme.typography.bodySmall,
                                                                        color =
                                                                                MaterialTheme.colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                        }
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                SettingsSectionHeader(icon = Icons.Rounded.Info, title = "About")
                                Spacer(modifier = Modifier.height(12.dp))
                                ElevatedCard(
                                        onClick = onNavigateToHelp,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        colors =
                                                CardDefaults.elevatedCardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceContainerLow
                                                ),
                                        elevation =
                                                CardDefaults.elevatedCardElevation(
                                                        defaultElevation = 0.dp
                                                )
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Rounded
                                                                        .HelpOutline,
                                                        contentDescription = "Help And About",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                                text = "Help And About",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        Text(
                                                                text = "Learn how to use yaja",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(32.dp))
                        }
                }
        }
}
