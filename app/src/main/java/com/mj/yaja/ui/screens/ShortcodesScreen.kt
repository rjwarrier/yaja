package com.mj.yaja.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcodesScreen(
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit,
        onNavigateBack: () -> Unit,
        onNavigateToJournal: () -> Unit,
        onNavigateToCalendar: () -> Unit,
        onNavigateToLookback: () -> Unit,
        onNavigateToShortcodes: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToHelp: () -> Unit
) {
    val customShortcodes by viewModel.customShortcodes.collectAsState()
    var showAddShortcodeDialog by remember { mutableStateOf(false) }
    var editingShortcode by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
                    uri ->
                uri?.let {
                    scope.launch {
                        try {
                            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                OutputStreamWriter(outputStream).use { writer ->
                                    customShortcodes.forEach { (code, value) ->
                                        writer.write("$code,$value\n")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ShortcodesScreen", "Failed to export shortcodes", e)
                        }
                    }
                }
            }

    val importLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let {
                    scope.launch {
                        try {
                            context.contentResolver.openInputStream(it)?.use { inputStream ->
                                val imported = mutableMapOf<String, String>()
                                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                    var line: String? = reader.readLine()
                                    while (line != null) {
                                        if (line.contains(",")) {
                                            val parts = line.split(",", limit = 2)
                                            if (parts.size == 2) {
                                                imported[parts[0].trim()] = parts[1].trim()
                                            }
                                        }
                                        line = reader.readLine()
                                    }
                                }
                                if (imported.isNotEmpty()) {
                                    viewModel.importCustomShortcodes(imported)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ShortcodesScreen", "Failed to import shortcodes", e)
                        }
                    }
                }
            }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                        title = {
                            Text(
                                    "Shortcodes",
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
                        actions = {
                            IconButton(
                                    onClick = {
                                        importLauncher.launch(
                                                arrayOf(
                                                        "text/comma-separated-values",
                                                        "text/csv",
                                                        "application/csv"
                                                )
                                        )
                                    }
                            ) { Icon(Icons.Rounded.FileOpen, contentDescription = "Import") }
                            IconButton(onClick = { exportLauncher.launch("shortcodes.csv") }) {
                                Icon(Icons.Rounded.SaveAlt, contentDescription = "Export")
                            }
                            IconButton(onClick = { showHelpDialog = true }) {
                                Icon(Icons.AutoMirrored.Rounded.Help, contentDescription = "Help")
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors =
                                TopAppBarDefaults.largeTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background,
                                        scrolledContainerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow,
                                        titleContentColor = MaterialTheme.colorScheme.primary,
                                        navigationIconContentColor =
                                                MaterialTheme.colorScheme.onSurface,
                                        actionIconContentColor =
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                        onClick = { showAddShortcodeDialog = true },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text("New Shortcode") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
    ) { paddingValues ->
        AnimatedContent(
                targetState = customShortcodes.isEmpty(),
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + slideInVertically { it / 8 }) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                modifier = Modifier.padding(paddingValues),
                label = "ScreenContent"
        ) { isEmpty ->
            if (isEmpty) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                            Icons.AutoMirrored.Rounded.ShortText,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                            text = "No custom shortcodes",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            text = "Add one to expand snippets like @yday into dynamic text.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    val shortcodeList = customShortcodes.toList().sortedBy { it.first }
                    items(
                            items = shortcodeList.indices.toList(),
                            key = { index -> shortcodeList[index].first }
                    ) { index ->
                        val item = shortcodeList[index]
                        val code = item.first
                        val value = item.second
                        ShortcodeItem(
                                code = code,
                                value = value,
                                index = index,
                                onClick = { editingShortcode = Pair(code, value) },
                                onDelete = { viewModel.removeCustomShortcode(code) },
                                modifier = Modifier
                        )
                    }
                }
            }
        }

        if (showAddShortcodeDialog || editingShortcode != null) {
            ShortcodeEditDialog(
                    initialCode = editingShortcode?.first ?: "",
                    initialValue = editingShortcode?.second ?: "",
                    onDismiss = {
                        showAddShortcodeDialog = false
                        editingShortcode = null
                    },
                    onConfirm = { code, value ->
                        viewModel.setCustomShortcode(code, value)
                        showAddShortcodeDialog = false
                        editingShortcode = null
                    }
            )
        }

        if (showHelpDialog) {
            ShortcodeHelpDialog(onDismiss = { showHelpDialog = false })
        }
    }
}

@Composable
fun ShortcodeItem(
        code: String,
        value: String,
        index: Int,
        onClick: () -> Unit,
        onDelete: () -> Unit,
        modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 50L) // Staggered entrance
        isVisible = true
    }

    AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 10 },
            modifier = modifier
    ) {
        ListItem(
                headlineContent = {
                    Text(
                            text = code,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                    )
                },
                supportingContent = {
                    Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                    )
                },
                trailingContent = {
                    IconButton(onClick = onDelete) {
                        Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

@Composable
fun ShortcodeEditDialog(
        initialCode: String,
        initialValue: String,
        onDismiss: () -> Unit,
        onConfirm: (String, String) -> Unit
) {
    var code by remember { mutableStateOf(initialCode) }
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
            title = { Text(if (initialCode.isEmpty()) "Add Shortcode" else "Edit Shortcode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Shortcode (e.g. @yday)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text("Expansion Text") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                    )
                    Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                                "Use {{today:format}} for dynamic dates.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                        onClick = { if (code.isNotBlank()) onConfirm(code, value) },
                        enabled = code.startsWith("@") && code.length > 1
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ShortcodeHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null) },
            title = { Text("Shortcode Help") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                            "Built-in Shortcodes:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                            "@today, @now, @week, @day, @t (checkbox), @x (toggle checkbox)",
                            style = MaterialTheme.typography.bodyMedium
                    )

                    HorizontalDivider()

                    Text(
                            "Dynamic Patterns:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                            "Use {{today:FORMAT}}, {{yesterday:FORMAT}}, {{tomorrow:FORMAT}}, or {{now:FORMAT}}.",
                            style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                            "Example: @yday -> Yesterday: {{yesterday:dd-MMM-yy}}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                            "Common formats: dd-MM-yy, HH:mm, EEEE (day name).",
                            style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
