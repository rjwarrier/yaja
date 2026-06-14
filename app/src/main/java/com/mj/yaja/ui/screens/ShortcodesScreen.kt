package com.mj.yaja.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.rememberAppEntrance
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcodesScreen(
        viewModel: JournalViewModel,
        onOpenDrawer: () -> Unit,
        onNavigateBack: () -> Unit
) {
    val customShortcodes by viewModel.customShortcodes.collectAsState()
    var showAddShortcodeDialog by remember { mutableStateOf(false) }
    var editingShortcode by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entranceTriggered = rememberAppEntrance()

    val exportLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
                    uri ->
                uri?.let {
                    scope.launch {
                        try {
                            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                OutputStreamWriter(outputStream).use { writer ->
                                    customShortcodes.forEach { (code, value) ->
                                        writer.write(encodeCsvRow(code, value))
                                        writer.write("\n")
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
                                    parseCsvRows(reader.readText()).forEach { row ->
                                        if (row.size >= 2) {
                                            imported[row[0].trim()] = row[1]
                                        }
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
                ShortcodesTopBar(
                        onOpenDrawer = onOpenDrawer,
                        onImport = {
                                importLauncher.launch(
                                        arrayOf(
                                                "text/comma-separated-values",
                                                "text/csv",
                                                "application/csv"
                                        )
                                )
                        },
                        onExport = { exportLauncher.launch("shortcodes.csv") },
                        onHelp = { showHelpDialog = true },
                        scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                ShortcodesFab(
                        visible = entranceTriggered,
                        onClick = { showAddShortcodeDialog = true }
                )
            }
    ) { paddingValues ->
        AppScreenReveal(
            visible = true,
            modifier = Modifier.fillMaxSize()
        ) {
            ShortcodesScreenContent(
                customShortcodes = customShortcodes,
                entranceTriggered = entranceTriggered,
                paddingValues = paddingValues,
                onEdit = { code, value -> editingShortcode = Pair(code, value) },
                onDelete = { code -> viewModel.removeCustomShortcode(code) }
            )
        }

        ShortcodesDialogs(
                showAddShortcodeDialog = showAddShortcodeDialog,
                editingShortcode = editingShortcode,
                showHelpDialog = showHelpDialog,
                onDismissEdit = {
                        showAddShortcodeDialog = false
                        editingShortcode = null
                },
                onConfirmEdit = { code, value ->
                        viewModel.setCustomShortcode(code, value)
                        showAddShortcodeDialog = false
                        editingShortcode = null
                },
                onDismissHelp = { showHelpDialog = false }
        )
    }
}

@Composable
fun ShortcodeItem(
        code: String,
        value: String,
        index: Int,
        animateIn: Boolean,
        onClick: () -> Unit,
        onDelete: () -> Unit,
        modifier: Modifier = Modifier
) {
    AppStaggeredEntrance(
            visible = animateIn,
            index = index,
            strength = if (index == 0) AppEntranceStrength.HERO else AppEntranceStrength.SECTION,
            modifier = modifier
    ) {
        ElevatedCard(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clickable(onClick = onClick),
                colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.large
        ) {
                Row(
                        modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(40.dp)
                        ) {
                                Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                                Icons.AutoMirrored.Rounded.ShortText,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(20.dp)
                                        )
                                }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = code,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                )
                        }
                        IconButton(onClick = onDelete) {
                                Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = MaterialTheme.colorScheme.error
                                )
                        }
                }
        }
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
            title = {
                Text(
                    if (initialCode.isEmpty()) {
                        stringResource(R.string.shortcodes_add_title)
                    } else {
                        stringResource(R.string.shortcodes_edit_title)
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text(stringResource(R.string.shortcodes_code_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text(stringResource(R.string.shortcodes_expansion_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                    )
                    Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                                stringResource(R.string.shortcodes_dynamic_dates_hint),
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
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun ShortcodeHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.shortcodes_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                            stringResource(R.string.shortcodes_help_built_in),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                            stringResource(R.string.shortcodes_help_built_in_examples),
                            style = MaterialTheme.typography.bodyMedium
                    )

                    HorizontalDivider()

                    Text(
                            stringResource(R.string.shortcodes_help_dynamic_patterns),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                            stringResource(R.string.shortcodes_help_dynamic_desc),
                            style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                            stringResource(R.string.shortcodes_help_example),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                            stringResource(R.string.shortcodes_help_common_formats),
                            style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

private fun encodeCsvField(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}

private fun encodeCsvRow(code: String, value: String): String =
    "${encodeCsvField(code)},${encodeCsvField(value)}"

private fun parseCsvRows(text: String): List<List<String>> {
    if (text.isBlank()) return emptyList()

    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var index = 0

    fun flushField() {
        row.add(field.toString())
        field.setLength(0)
    }

    fun flushRow() {
        if (row.isNotEmpty()) {
            rows.add(row.toList())
            row.clear()
        }
    }

    while (index < text.length) {
        val ch = text[index]
        when {
            ch == '"' -> {
                if (inQuotes && index + 1 < text.length && text[index + 1] == '"') {
                    field.append('"')
                    index++
                } else {
                    inQuotes = !inQuotes
                }
            }
            ch == ',' && !inQuotes -> flushField()
            ch == '\r' && !inQuotes -> {
                flushField()
                flushRow()
                if (index + 1 < text.length && text[index + 1] == '\n') {
                    index++
                }
            }
            ch == '\n' && !inQuotes -> {
                flushField()
                flushRow()
            }
            else -> field.append(ch)
        }
        index++
    }

    if (field.isNotEmpty() || row.isNotEmpty()) {
        flushField()
        flushRow()
    }

    return rows.filter { it.size >= 2 }
}
