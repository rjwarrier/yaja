package com.mj.yaja.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.mj.yaja.ui.utils.MarkdownUtils
import com.mj.yaja.ui.utils.MarkdownVisualTransformation
import com.mj.yaja.ui.utils.ShortcodeManager
import com.mj.yaja.ui.viewmodel.JournalViewModel
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(viewModel: JournalViewModel, onNavigateBack: () -> Unit) {
        val uiState by viewModel.uiState.collectAsState()
        val customShortcodes by viewModel.customShortcodes.collectAsState()
        val editingEntry = uiState.editingEntry

        var isEditingMode by remember { mutableStateOf(editingEntry == null) }

        val initialText =
                remember(editingEntry) {
                        var text = editingEntry ?: ""
                        // Strip out timestamp tag for editing explicitly
                        text = text.replace(Regex("^<!--time:.*?-->\\n?"), "")
                        text
                }

        var textFieldValue by
                remember(initialText) {
                        mutableStateOf(
                                TextFieldValue(
                                        initialText,
                                        selection = TextRange(initialText.length)
                                )
                        )
                }
        val context = LocalContext.current

        val hasUnsavedChanges = textFieldValue.text != initialText
        var showDiscardDialog by remember { mutableStateOf(false) }
        var showHelpDialog by remember { mutableStateOf(false) }

        BackHandler(enabled = hasUnsavedChanges) { showDiscardDialog = true }

        // Morphing Shape logic (Sync with Home screen)
        val dayFormatter = remember { DateTimeFormatter.ofPattern("dd") }
        val monthYearFormatter = remember { DateTimeFormatter.ofPattern("MMMM - yyyy") }
        val weekdayFormatter = remember { DateTimeFormatter.ofPattern("EEEE") }

        val shapeStep = uiState.selectedDate.dayOfMonth % 4
        val tlRadius by
                animateDpAsState(
                        targetValue =
                                when (shapeStep) {
                                        1 -> 24.dp
                                        2 -> 8.dp
                                        3 -> 20.dp
                                        else -> 16.dp
                                },
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "TL"
                )
        val trRadius by
                animateDpAsState(
                        targetValue =
                                when (shapeStep) {
                                        1 -> 8.dp
                                        2 -> 24.dp
                                        3 -> 20.dp
                                        else -> 16.dp
                                },
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "TR"
                )
        val brRadius by
                animateDpAsState(
                        targetValue =
                                when (shapeStep) {
                                        1 -> 24.dp
                                        2 -> 8.dp
                                        3 -> 4.dp
                                        else -> 16.dp
                                },
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "BR"
                )
        val blRadius by
                animateDpAsState(
                        targetValue =
                                when (shapeStep) {
                                        1 -> 8.dp
                                        2 -> 24.dp
                                        3 -> 20.dp
                                        else -> 16.dp
                                },
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "BL"
                )

        val expressiveShape =
                RoundedCornerShape(
                        topStart = tlRadius,
                        topEnd = trRadius,
                        bottomEnd = brRadius,
                        bottomStart = blRadius
                )

        val timeMatch =
                remember(editingEntry) { editingEntry?.let { Regex("<!--time:(.*?)-->").find(it) } }
        val timeStr = timeMatch?.groupValues?.get(1)

        if (showDiscardDialog) {
                AlertDialog(
                        onDismissRequest = { showDiscardDialog = false },
                        title = { Text("Discard Changes") },
                        text = {
                                Text(
                                        "You have unsaved changes. Go back and discard them, or stay to keep editing."
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                showDiscardDialog = false
                                                viewModel.clearEditing()
                                                onNavigateBack()
                                        }
                                ) { Text("Go Back") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showDiscardDialog = false }) { Text("Stay") }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }

        if (showHelpDialog) {
                AlertDialog(
                        onDismissRequest = { showHelpDialog = false },
                        title = { Text("Shortcuts") },
                        text = {
                                Column {
                                        Text(
                                                buildAnnotatedString {
                                                        withStyle(
                                                                SpanStyle(
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        ) { append("**Bold**") }
                                                        append(
                                                                " to make text bold (or select text and tap Bold Button)"
                                                        )
                                                }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                buildAnnotatedString {
                                                        withStyle(
                                                                SpanStyle(
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        ) { append("*Italic*") }
                                                        append(
                                                                " to make text italic (or select text and tap Italic Button)"
                                                        )
                                                }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("-------------------------------")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                buildAnnotatedString {
                                                        withStyle(
                                                                SpanStyle(
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        ) { append("@today") }
                                                        append(
                                                                ": Expands to current date (e.g., 25-Dec-25)"
                                                        )
                                                }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                buildAnnotatedString {
                                                        withStyle(
                                                                SpanStyle(
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        ) { append("@now") }
                                                        append(
                                                                ": Expands to current time (e.g., 14:30)"
                                                        )
                                                }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                buildAnnotatedString {
                                                        withStyle(
                                                                SpanStyle(
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        ) { append("@week") }
                                                        append(
                                                                ": Expands to current week (e.g., Week 52)"
                                                        )
                                                }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                buildAnnotatedString {
                                                        withStyle(
                                                                SpanStyle(
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        ) { append("@day") }
                                                        append(
                                                                ": Expands to current weekday (e.g., Monday)"
                                                        )
                                                }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                buildAnnotatedString {
                                                        withStyle(
                                                                SpanStyle(
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        ) { append("@t ") }
                                                        append(
                                                                ": Expands to a todo checkbox (e.g., [ ] )"
                                                        )
                                                }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                buildAnnotatedString {
                                                        withStyle(
                                                                SpanStyle(
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        ) { append("@x") }
                                                        append(
                                                                ": When entered anywhere in the line containing a todo checkbox, it will toggle it."
                                                        )
                                                }
                                        )
                                }
                        },
                        confirmButton = {
                                TextButton(onClick = { showHelpDialog = false }) { Text("Okey!") }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }

        Scaffold(
                topBar = {
                        val titleText =
                                when {
                                        editingEntry == null -> "New Entry"
                                        isEditingMode -> "Edit Entry"
                                        else -> "View Entry"
                                }
                        val formatter = remember { DateTimeFormatter.ofPattern("dd-MMM-yyyy") }
                        val dateText = uiState.selectedDate.format(formatter)
                        CenterAlignedTopAppBar(
                                title = {
                                        Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                        ) {
                                                Box(
                                                        modifier =
                                                                Modifier.size(48.dp)
                                                                        .background(
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primaryContainer
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.4f
                                                                                                ),
                                                                                shape =
                                                                                        expressiveShape
                                                                        ),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Text(
                                                                text =
                                                                        uiState.selectedDate.format(
                                                                                dayFormatter
                                                                        ),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleLarge,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(horizontalAlignment = Alignment.Start) {
                                                        Text(
                                                                text =
                                                                        uiState.selectedDate.format(
                                                                                weekdayFormatter
                                                                        ),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primary.copy(
                                                                                alpha = 0.7f
                                                                        ),
                                                                fontWeight = FontWeight.Bold,
                                                                letterSpacing = 0.5.sp
                                                        )
                                                        Text(
                                                                text =
                                                                        uiState.selectedDate.format(
                                                                                monthYearFormatter
                                                                        ),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface,
                                                                fontWeight = FontWeight.Medium
                                                        )
                                                        if (!isEditingMode && timeStr != null) {
                                                                Text(
                                                                        text =
                                                                                "Recorded at $timeStr",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                        fontWeight =
                                                                                FontWeight.Bold,
                                                                        letterSpacing = 0.3.sp,
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        top = 2.dp
                                                                                )
                                                                )
                                                        }
                                                }
                                        }
                                },
                                navigationIcon = {
                                        IconButton(
                                                onClick = {
                                                        if (hasUnsavedChanges) {
                                                                showDiscardDialog = true
                                                        } else {
                                                                viewModel.clearEditing()
                                                                onNavigateBack()
                                                        }
                                                }
                                        ) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Rounded
                                                                        .ArrowBack,
                                                        contentDescription = "Cancel"
                                                )
                                        }
                                },
                                actions = {
                                        if (isEditingMode) {
                                                // Info / help button — always visible in editing
                                                // mode
                                                IconButton(onClick = { showHelpDialog = true }) {
                                                        Icon(
                                                                imageVector = Icons.Outlined.Info,
                                                                contentDescription =
                                                                        "Shortcuts help",
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                                }
                                                if (editingEntry != null) {
                                                        IconButton(
                                                                onClick = {
                                                                        viewModel.deleteEntry(
                                                                                uiState.editingIndex
                                                                        )
                                                                        viewModel.clearEditing()
                                                                        onNavigateBack()
                                                                }
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Rounded
                                                                                        .Delete,
                                                                        contentDescription =
                                                                                "Delete"
                                                                )
                                                        }
                                                }
                                        } else {
                                                IconButton(
                                                        onClick = {
                                                                val sendIntent: Intent = Intent().apply {
                                                                        action = Intent.ACTION_SEND
                                                                        putExtra(Intent.EXTRA_TEXT, textFieldValue.text)
                                                                        type = "text/plain"
                                                                }
                                                                val shareIntent = Intent.createChooser(sendIntent, null)
                                                                context.startActivity(shareIntent)
                                                        }
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Rounded.Share,
                                                                contentDescription = "Share"
                                                        )
                                                }
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
                bottomBar = {}, // Moved to floating Box inside content for better control
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.ime
        ) { paddingValues ->
                // Track save tap to drive reverse icon morph (✓ → +)
                var saveFabPressed by remember { mutableStateOf(false) }

                val isFormattingBarVisible = isEditingMode && !textFieldValue.selection.collapsed

                LaunchedEffect(saveFabPressed) {
                        if (saveFabPressed) {
                                delay(380)
                                if (editingEntry != null) {
                                        viewModel.updateEntry(textFieldValue.text)
                                } else {
                                        viewModel.addEntry(textFieldValue.text)
                                }
                                // Entry is now saved to disk and UI is updated, safe to navigate back
                                onNavigateBack()
                        }
                }

                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(paddingValues)
                                        .then(
                                                if (isFormattingBarVisible) Modifier
                                                else Modifier.navigationBarsPadding()
                                        )
                ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                                        // Normal Mode
                                        Column(
                                                modifier =
                                                        Modifier.fillMaxSize().padding(horizontal = 24.dp)
                                        ) {
                                                Spacer(modifier = Modifier.height(24.dp))

                                                OutlinedTextField(
                                                        value = textFieldValue,
                                                        onValueChange = { newValue ->
                                                        val expanded =
                                                                ShortcodeManager.expand(
                                                                        newValue.text,
                                                                        customShortcodes
                                                                )

                                                        // Auto-continue todo lists on Enter
                                                        val cursorPos = newValue.selection.end
                                                        val newlineInserted =
                                                                expanded.length ==
                                                                        textFieldValue.text.length +
                                                                                1 &&
                                                                        cursorPos > 0 &&
                                                                        expanded.getOrNull(
                                                                                cursorPos - 1
                                                                        ) == '\n'
                                                        val todoPrefix =
                                                                if (newlineInserted) {
                                                                        val beforeCursor =
                                                                                expanded.substring(
                                                                                        0,
                                                                                        cursorPos -
                                                                                                1
                                                                                )
                                                                        val prevLineStart =
                                                                                beforeCursor
                                                                                        .lastIndexOf(
                                                                                                '\n'
                                                                                        ) + 1
                                                                        val prevLine =
                                                                                beforeCursor
                                                                                        .substring(
                                                                                                prevLineStart
                                                                                        )
                                                                                        .trimStart()
                                                                        if (prevLine.startsWith(
                                                                                        "[ ] "
                                                                                ) ||
                                                                                        prevLine.startsWith(
                                                                                                "[x] "
                                                                                        ) ||
                                                                                        prevLine ==
                                                                                                "[ ]" ||
                                                                                        prevLine ==
                                                                                                "[x]"
                                                                        )
                                                                                "[ ] "
                                                                        else null
                                                                } else null

                                                        if (todoPrefix != null) {
                                                                val newText =
                                                                        expanded.substring(
                                                                                0,
                                                                                cursorPos
                                                                        ) +
                                                                                todoPrefix +
                                                                                expanded.substring(
                                                                                        cursorPos
                                                                                )
                                                                textFieldValue =
                                                                        newValue.copy(
                                                                                text = newText,
                                                                                selection =
                                                                                        TextRange(
                                                                                                cursorPos +
                                                                                                        todoPrefix
                                                                                                                .length
                                                                                        )
                                                                        )
                                                        } else if (expanded != newValue.text) {
                                                                val delta =
                                                                        expanded.length -
                                                                                newValue.text.length
                                                                val newCursor =
                                                                        (newValue.selection.end +
                                                                                        delta)
                                                                                .coerceIn(
                                                                                        0,
                                                                                        expanded.length
                                                                                )
                                                                textFieldValue =
                                                                        newValue.copy(
                                                                                text = expanded,
                                                                                selection =
                                                                                        TextRange(
                                                                                                newCursor
                                                                                        )
                                                                        )
                                                        } else {
                                                                textFieldValue = newValue
                                                        }
                                                },
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .weight(1f),
                                                placeholder = { Text("What happened?") },
                                                keyboardOptions =
                                                        KeyboardOptions(
                                                                capitalization =
                                                                        KeyboardCapitalization
                                                                                .Sentences
                                                        ),
                                                singleLine = false,
                                                readOnly = !isEditingMode,
                                                visualTransformation =
                                                        MarkdownVisualTransformation(),
                                                colors =
                                                        OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor =
                                                                        Color.Transparent,
                                                                unfocusedBorderColor =
                                                                        Color.Transparent,
                                                                focusedContainerColor =
                                                                        Color.Transparent,
                                                                unfocusedContainerColor =
                                                                        Color.Transparent
                                                        ),
                                                textStyle =
                                                        MaterialTheme.typography.bodyLarge.copy(
                                                                lineHeight = 24.sp,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                        )

                                                // Word and Character Count Indicator
                                                val currentText = textFieldValue.text
                                                val wordCount = currentText.split(Regex("\\s+")).count { it.isNotBlank() }
                                                val charCount = currentText.length

                                                AnimatedVisibility(
                                                        visible = currentText.isNotEmpty(),
                                                        enter = fadeIn(tween(200)),
                                                        exit = fadeOut(tween(200)),
                                                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
                                                ) {
                                                        Text(
                                                                text = "$wordCount words • $charCount chars",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                }

                                        }

                                // Formatting Toolbar
                                AnimatedVisibility(
                                        visible = isFormattingBarVisible,
                                        enter =
                                                slideInVertically(
                                                        spring(
                                                                dampingRatio = 0.7f,
                                                                stiffness =
                                                                        Spring.StiffnessMediumLow
                                                        )
                                                ) { it } + fadeIn(tween(150)),
                                        exit =
                                                slideOutVertically(tween(180)) { it } +
                                                        fadeOut(tween(100)),
                                        modifier =
                                                Modifier.align(Alignment.BottomCenter)
                                                        .padding(bottom = 80.dp) // Sit above FAB
                                ) {
                                        Surface(
                                                shape = CircleShape,
                                                color =
                                                        MaterialTheme.colorScheme
                                                                .surfaceContainerHigh,
                                                contentColor = MaterialTheme.colorScheme.primary,
                                                tonalElevation = 6.dp,
                                                shadowElevation = 4.dp,
                                                modifier =
                                                        Modifier.border(
                                                                1.dp,
                                                                MaterialTheme.colorScheme
                                                                        .outlineVariant.copy(
                                                                        alpha = 0.5f
                                                                ),
                                                                CircleShape
                                                        )
                                        ) {
                                                Row(
                                                        modifier =
                                                                Modifier.padding(
                                                                        horizontal = 8.dp,
                                                                        vertical = 4.dp
                                                                ),
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(8.dp),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        IconButton(
                                                                onClick = {
                                                                        applyFormatting(
                                                                                textFieldValue,
                                                                                "**",
                                                                                "**"
                                                                        ) { textFieldValue = it }
                                                                }
                                                        ) {
                                                                Icon(
                                                                        Icons.Rounded.FormatBold,
                                                                        contentDescription = "Bold"
                                                                )
                                                        }
                                                        IconButton(
                                                                onClick = {
                                                                        applyFormatting(
                                                                                textFieldValue,
                                                                                "*",
                                                                                "*"
                                                                        ) { textFieldValue = it }
                                                                }
                                                        ) {
                                                                Icon(
                                                                        Icons.Rounded.FormatItalic,
                                                                        contentDescription =
                                                                                "Italic"
                                                                )
                                                        }
                                                }
                                        }
                                }

                                // FAB overlay — sits just above the keyboard
                                FloatingActionButton(
                                        onClick = {
                                                if (!isEditingMode) {
                                                        isEditingMode = true
                                                } else if (textFieldValue.text.isNotBlank() &&
                                                                !saveFabPressed
                                                ) {
                                                        saveFabPressed = true
                                                }
                                        },
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier =
                                                Modifier.align(Alignment.BottomEnd).padding(16.dp)
                                ) {
                                        AnimatedContent(
                                                targetState = isEditingMode && !saveFabPressed,
                                                transitionSpec = {
                                                        (scaleIn(
                                                                spring(
                                                                        dampingRatio = 0.5f,
                                                                        stiffness =
                                                                                Spring.StiffnessMediumLow
                                                                )
                                                        ) + fadeIn(tween(220))) togetherWith
                                                                (scaleOut(
                                                                        spring(
                                                                                stiffness =
                                                                                        Spring.StiffnessMedium
                                                                        )
                                                                ) + fadeOut(tween(160)))
                                                },
                                                contentAlignment = Alignment.Center,
                                                label = "SaveFabIconMorph"
                                        ) { showCheck ->
                                                when {
                                                        showCheck ->
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Rounded.Check,
                                                                        contentDescription = "Save"
                                                                )
                                                        isEditingMode ->
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Rounded.Add,
                                                                        contentDescription = "Done"
                                                                )
                                                        else ->
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Rounded.Edit,
                                                                        contentDescription = "Edit"
                                                                )
                                                }
                                        }
                                }
                        }

                }

        }
}

private fun applyFormatting(
        currentValue: TextFieldValue,
        prefix: String,
        suffix: String,
        onValueChange: (TextFieldValue) -> Unit
) {
        val text = currentValue.text
        val selection = currentValue.selection
        val start = selection.min
        val end = selection.max

        // If nothing selected, handle "sticky" toggle behavior
        if (selection.collapsed) {
                val pairs = MarkdownUtils.findPairs(text)
                val styleToMatch = MarkdownUtils.findStyleByPrefix(prefix)

                // Find if cursor is inside or at the boundary of a matching pair
                val activePair =
                        pairs.find { pair: MarkdownUtils.TagPair ->
                                pair.style == styleToMatch &&
                                        start >= pair.startRange.first &&
                                        start <= pair.endRange.last
                        }

                if (activePair != null) {
                        // Jump OUT of the current formatting
                        val newCursorPos = activePair.endRange.last + 1
                        onValueChange(TextFieldValue(text, selection = TextRange(newCursorPos)))
                } else {
                        // Insert NEW tag pair
                        val newText =
                                text.substring(0, start) + prefix + suffix + text.substring(end)
                        val newCursorPos = start + prefix.length
                        onValueChange(TextFieldValue(newText, selection = TextRange(newCursorPos)))
                }
                return
        }

        val selectedText = text.substring(start, end)

        // Check if the selection is perfectly wrapped (Toggle off)
        val isWrapped = selectedText.startsWith(prefix) && selectedText.endsWith(suffix)

        if (isWrapped) {
                // Toggle OFF: Remove the outer wrapper
                val unwrappedText =
                        selectedText.substring(prefix.length, selectedText.length - suffix.length)
                val newText = text.substring(0, start) + unwrappedText + text.substring(end)
                onValueChange(
                        TextFieldValue(
                                newText,
                                selection = TextRange(start, start + unwrappedText.length)
                        )
                )
        } else {
                // Toggle ON:
                // 1. First, strip any existing instances of THIS specific prefix/suffix WITHIN the
                // selection
                //    to prevent nested/interleaved messes like **word****
                val cleanedSelectedText = selectedText.replace(prefix, "").replace(suffix, "")

                // 2. Wrap the cleaned text
                val wrappedText = prefix + cleanedSelectedText + suffix
                val newText = text.substring(0, start) + wrappedText + text.substring(end)

                // 3. Keep selection encompassing the wrapped content
                onValueChange(
                        TextFieldValue(
                                newText,
                                selection = TextRange(start, start + wrappedText.length)
                        )
                )
        }
}
