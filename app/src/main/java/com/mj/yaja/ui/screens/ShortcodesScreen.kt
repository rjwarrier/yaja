package com.mj.yaja.ui.screens

import android.util.Log
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.tweenSpec
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
    val customShortcodes by viewModel.customShortcodes.collectAsStateWithLifecycle()
    var showAddShortcodeDialog by remember { mutableStateOf(false) }
    var editingShortcode by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val fabPlacement by viewModel.fabPlacement.collectAsStateWithLifecycle()
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
                Box(
                        modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                ) {
                        ShortcodesFab(
                                visible = entranceTriggered,
                                onClick = { showAddShortcodeDialog = true },
                                modifier = Modifier.align(fabPlacement.fabAlignment())
                        )
                }
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
                onDelete = { code -> viewModel.removeCustomShortcode(code) },
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
                        val originalCode = editingShortcode?.first
                        if (originalCode != null && originalCode != code) {
                                viewModel.removeCustomShortcode(originalCode)
                        }
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
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val preference = LocalAnimationPreference.current
    val isDynamic = value.contains("{{")

    AppStaggeredEntrance(
            visible = animateIn,
            index = index,
            strength = if (index == 0) AppEntranceStrength.HERO else AppEntranceStrength.SECTION,
            modifier = modifier
    ) {
        ElevatedCard(
                onClick = { expanded = !expanded },
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .animateContentSize(animationSpec = preference.tweenSpec(200)),
                colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.large
        ) {
                Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                        if (isDynamic) {
                                                Surface(
                                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
                                                        shape = RoundedCornerShape(999.dp),
                                                        modifier = Modifier.padding(top = 6.dp)
                                                ) {
                                                        Text(
                                                                text = stringResource(R.string.shortcodes_dynamic_badge),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                                        )
                                                }
                                        }
                                }
                                Icon(
                                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }

                        AnimatedVisibility(
                                visible = expanded,
                                enter = preference.enterOrNone(
                                        fadeIn(preference.floatTween(150)) + expandVertically(preference.tweenSpec(200))
                                ),
                                exit = preference.exitOrNone(
                                        fadeOut(preference.floatTween(100)) + shrinkVertically(preference.tweenSpec(180))
                                )
                        ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                        ) {
                                                TextButton(
                                                        onClick = onDelete,
                                                        colors = ButtonDefaults.textButtonColors(
                                                                contentColor = MaterialTheme.colorScheme.error
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                        Text(stringResource(R.string.action_delete), style = MaterialTheme.typography.labelLarge)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                TextButton(
                                                        onClick = onEdit,
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                        Text(stringResource(R.string.action_edit), style = MaterialTheme.typography.labelLarge)
                                                }
                                        }
                                }
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
    var code by remember(initialCode) { mutableStateOf(initialCode) }
    var value by remember(initialValue) { mutableStateOf(TextFieldValue(initialValue)) }

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
                            onValueChange = { newCode ->
                                code = newCode
                                if (value.text.isEmpty()) {
                                    value = dynamicScaffoldValue(newCode)
                                }
                            },
                            label = { Text(stringResource(R.string.shortcodes_code_label)) },
                            supportingText = { Text(stringResource(R.string.shortcodes_code_hint)) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Rounded.ShortText, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text(stringResource(R.string.shortcodes_expansion_label)) },
                            supportingText = { Text(stringResource(R.string.shortcodes_expansion_hint)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Description, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                    )
                    Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                            )
                            Text(
                                    stringResource(R.string.shortcodes_dynamic_dates_hint),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                        onClick = { if (code.isNotBlank()) onConfirm(code, value.text) },
                        enabled = code.startsWith("@") && code.length > 1
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/**
 * Seeds an empty expansion field with a ready-to-fill dynamic-date placeholder, parking the caret
 * between the colon and the closing braces so the next keystrokes land in the format slot.
 * [ShortcodeManager] only resolves today/yesterday/tomorrow/now, so the scaffold always names one
 * of those rather than echoing the code text back, which would never expand.
 */
internal fun dynamicScaffoldValue(code: String): TextFieldValue {
    if (!code.startsWith("@") || code.length < 2) return TextFieldValue("")
    val scaffold = "{{today:}}"
    return TextFieldValue(scaffold, selection = TextRange(scaffold.indexOf(':') + 1))
}

@OptIn(ExperimentalLayoutApi::class)
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

                    HorizontalDivider()

                    Text(
                            stringResource(R.string.shortcodes_help_format_codes_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                    )
                    FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FormatCodeChip("yyyy", stringResource(R.string.shortcodes_help_format_year4))
                        FormatCodeChip("yy", stringResource(R.string.shortcodes_help_format_year2))
                        FormatCodeChip("MM", stringResource(R.string.shortcodes_help_format_month_num))
                        FormatCodeChip("MMM", stringResource(R.string.shortcodes_help_format_month_short))
                        FormatCodeChip("MMMM", stringResource(R.string.shortcodes_help_format_month_full))
                        FormatCodeChip("dd", stringResource(R.string.shortcodes_help_format_day))
                        FormatCodeChip("EEE", stringResource(R.string.shortcodes_help_format_weekday_short))
                        FormatCodeChip("EEEE", stringResource(R.string.shortcodes_help_format_weekday_full))
                        FormatCodeChip("HH", stringResource(R.string.shortcodes_help_format_hour24))
                        FormatCodeChip("hh", stringResource(R.string.shortcodes_help_format_hour12))
                        FormatCodeChip("mm", stringResource(R.string.shortcodes_help_format_minute))
                        FormatCodeChip("ss", stringResource(R.string.shortcodes_help_format_second))
                        FormatCodeChip("a", stringResource(R.string.shortcodes_help_format_ampm))
                        FormatCodeChip("ww", stringResource(R.string.shortcodes_help_format_week))
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

@Composable
private fun FormatCodeChip(code: String, meaning: String) {
    Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(999.dp)
    ) {
        Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = code,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
            )
            Text(
                    text = meaning,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
