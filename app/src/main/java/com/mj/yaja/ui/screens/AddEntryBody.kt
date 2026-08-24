package com.mj.yaja.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.ui.components.ExpressiveCheckbox
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.data.countCharsIgnoringChecklistMarkers
import com.mj.yaja.data.countWordsIgnoringChecklistMarkers
import com.mj.yaja.data.estimateReadingTimeMinutes
import com.mj.yaja.data.DateKeywordEntry
import com.mj.yaja.data.DateOrderPreference
import com.mj.yaja.data.EntryKind
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.utils.DateLinkUtils
import com.mj.yaja.ui.utils.MarkdownUtils
import com.mj.yaja.R
import com.mj.yaja.ui.utils.MarkdownVisualTransformation
import com.mj.yaja.ui.utils.ShortcodeManager
import com.mj.yaja.ui.theme.DataFontScaleWrapper
import java.time.LocalDate
import kotlinx.coroutines.delay

internal enum class ListInsertMode {
        NONE,
        NUMBERED,
        BULLETED
}

@Composable
fun EntryViewBody(
        text: String,
        entryDate: LocalDate,
        keywords: List<KeywordDefinition>,
        keywordHighlightingEnabled: Boolean,
        dateOrderPreference: DateOrderPreference,
        customDateKeywords: List<DateKeywordEntry>,
        onDateClick: (LocalDate) -> Unit,
        modifier: Modifier = Modifier
) {
        val linkColor = MaterialTheme.colorScheme.primary
        val personHighlightColor = MaterialTheme.colorScheme.secondary
        val placeHighlightColor = MaterialTheme.colorScheme.tertiary
        val monthFirst = DateLinkUtils.resolveMonthFirst(dateOrderPreference)
        val viewText =
                MarkdownUtils.parseMarkdownWithDateLinks(
                        text = text,
                        entryDate = entryDate,
                        linkColor = linkColor,
                        personHighlightColor =
                                personHighlightColor.takeIf { keywordHighlightingEnabled },
                        placeHighlightColor =
                                placeHighlightColor.takeIf { keywordHighlightingEnabled },
                        keywords = if (keywordHighlightingEnabled) keywords else emptyList(),
                        monthFirst = monthFirst,
                        customKeywords = customDateKeywords,
                        onDateClick = onDateClick
                )

        Box(modifier = modifier) {
                DataFontScaleWrapper {
                        Text(
                                text = viewText,
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp, fontSize = 18.sp),
                                color = MaterialTheme.colorScheme.onSurface
                        )
                }
        }
}

@Composable
fun EntryEditorField(
        value: TextFieldValue,
        customShortcodes: Map<String, String>,
        onValueChange: (TextFieldValue) -> Unit,
        autoFocus: Boolean = false,
        modifier: Modifier = Modifier
) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(autoFocus) {
                if (autoFocus) {
                        delay(120)
                        focusRequester.requestFocus()
                        keyboardController?.show()
                }
        }

        DataFontScaleWrapper {
                OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                                onValueChange(handleEditorValueChange(value, newValue, customShortcodes))
                        },
                        modifier = modifier.focusRequester(focusRequester),
                        placeholder = { Text(stringResource(R.string.addentry_editor_placeholder)) },
                        keyboardOptions =
                                KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        singleLine = false,
                        visualTransformation = MarkdownVisualTransformation(),
                        colors =
                                OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                ),
                        textStyle =
                                MaterialTheme.typography.bodyLarge.copy(
                                        lineHeight = 24.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                )
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EntryQuickInsertChips(
        selectedEntryKind: EntryKind,
        selectedListMode: ListInsertMode,
        onSelectEvent: () -> Unit,
        onInsertTodo: () -> Unit,
        onToggleNumberedList: () -> Unit,
        onToggleBulletedList: () -> Unit,
        onInsertNumericList: () -> Unit,
        onInsertAlphabeticList: () -> Unit,
        onInsertPlusBullet: () -> Unit,
        onInsertQuoteBullet: () -> Unit,
        modifier: Modifier = Modifier
) {
        FlowRow(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                QuickInsertTile(
                        label = stringResource(R.string.addentry_event_chip),
                        footer = null,
                        selected = selectedEntryKind == EntryKind.EVENT,
                        icon = Icons.Rounded.Event,
                        onClick = onSelectEvent,
                        modifier = Modifier.width(88.dp)
                )
                QuickInsertTile(
                        label = stringResource(R.string.addentry_todo_chip),
                        footer = null,
                        selected = false,
                        onClick = onInsertTodo,
                        modifier = Modifier.width(108.dp),
                        icon = Icons.Rounded.CheckCircleOutline
                )
                QuickInsertTile(
                        label = stringResource(R.string.addentry_numbered_list_chip),
                        footer = null,
                        selected = selectedListMode == ListInsertMode.NUMBERED,
                        icon = Icons.Rounded.FormatListNumbered,
                        onClick = onToggleNumberedList,
                        modifier = Modifier.width(88.dp)
                )
                QuickInsertTile(
                        label = stringResource(R.string.addentry_bulleted_list_chip),
                        footer = null,
                        selected = selectedListMode == ListInsertMode.BULLETED,
                        icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                        onClick = onToggleBulletedList,
                        modifier = Modifier.width(88.dp)
                )
                when (selectedListMode) {
                        ListInsertMode.NUMBERED -> {
                                QuickInsertTile(
                                        label = "1,2",
                                        footer = "1.",
                                        selected = false,
                                        icon = Icons.Rounded.FormatListNumbered,
                                        onClick = onInsertNumericList,
                                        modifier = Modifier.width(88.dp)
                                )
                                QuickInsertTile(
                                        label = "a,b",
                                        footer = "a.",
                                        selected = false,
                                        icon = Icons.Rounded.FormatListNumbered,
                                        onClick = onInsertAlphabeticList,
                                        modifier = Modifier.width(88.dp)
                                )
                        }
                        ListInsertMode.BULLETED -> {
                                QuickInsertTile(
                                        label = "+",
                                        footer = null,
                                        selected = false,
                                        icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                                        onClick = onInsertPlusBullet,
                                        modifier = Modifier.width(88.dp)
                                )
                                QuickInsertTile(
                                        label = ">",
                                        footer = null,
                                        selected = false,
                                        icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                                        onClick = onInsertQuoteBullet,
                                        modifier = Modifier.width(88.dp)
                                )
                        }
                        ListInsertMode.NONE -> Unit
                }
        }
}

@Composable
private fun QuickInsertTile(
        label: String,
        footer: String?,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: ImageVector? = null,
        iconContent: (@Composable () -> Unit)? = null
) {
        Surface(
                onClick = onClick,
                modifier = modifier.height(54.dp),
                shape = RoundedCornerShape(18.dp),
                color =
                        if (selected) {
                                MaterialTheme.colorScheme.primary
                        } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                        },
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                        Box(
                                modifier = Modifier.height(20.dp),
                                contentAlignment = Alignment.Center
                        ) {
                                when {
                                        iconContent != null -> iconContent()
                                        icon != null -> Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint =
                                                        if (selected) {
                                                                MaterialTheme.colorScheme.onPrimary
                                                        } else {
                                                                MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                        )
                                }
                        }
                        Text(
                                text = label,
                                color =
                                        if (selected) {
                                                MaterialTheme.colorScheme.onPrimary
                                        } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                style = MaterialTheme.typography.labelMedium
                        )
                        if (footer != null) {
                                Text(
                                        text = footer,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.labelSmall
                                )
                        }
                }
        }
}

fun handleEditorValueChange(
        currentValue: TextFieldValue,
        newValue: TextFieldValue,
        customShortcodes: Map<String, String>
): TextFieldValue {
        var cursorPos = newValue.selection.end
        val shortcodeExpandedValue = expandShortcodesBeforeCursor(newValue, cursorPos, customShortcodes)
        var transformedText = shortcodeExpandedValue.text
        cursorPos = shortcodeExpandedValue.selection.end

        transformedText = replaceDashBulletAtLineStart(transformedText, cursorPos)

        val newlineInserted =
                newValue.text.length == currentValue.text.length + 1 &&
                        cursorPos > 0 &&
                        newValue.text.getOrNull(cursorPos - 1) == '\n'

        val continuationPrefix =
                if (newlineInserted) {
                        buildContinuationPrefix(transformedText, cursorPos)
                } else {
                        null
                }

        if (continuationPrefix != null) {
                transformedText =
                        transformedText.substring(0, cursorPos) +
                                continuationPrefix +
                                transformedText.substring(cursorPos)
                cursorPos += continuationPrefix.length
        }

        return if (transformedText != newValue.text || cursorPos != newValue.selection.end) {
                val safeCursor = cursorPos.coerceIn(0, transformedText.length)
                newValue.copy(text = transformedText, selection = TextRange(safeCursor))
        } else {
                newValue
        }
}

private fun expandShortcodesBeforeCursor(
        value: TextFieldValue,
        cursorPos: Int,
        customShortcodes: Map<String, String>
): TextFieldValue {
        var text = value.text
        var cursor = cursorPos

        customShortcodes.forEach { (code, rawSnippet) ->
                var searchStart = 0
                while (true) {
                        val index = text.indexOf(code, searchStart)
                        if (index < 0) break
                        val expanded = ShortcodeManager.expandValue(rawSnippet)
                        text = text.replaceRange(index, index + code.length, expanded)
                        if (index < cursor) {
                                cursor += expanded.length - code.length
                        }
                        searchStart = index + expanded.length
                }
        }

        val expandedText = ShortcodeManager.expand(text, emptyMap())
        return value.copy(
                text = expandedText,
                selection = TextRange(cursor.coerceIn(0, expandedText.length))
        )
}

private fun replaceDashBulletAtLineStart(
        text: String,
        cursorPos: Int
): String {
        if (cursorPos < 2 || text.getOrNull(cursorPos - 1) != ' ') return text
        val lineStart = text.lastIndexOf('\n', startIndex = cursorPos - 2).let { it + 1 }
        val linePrefix = text.substring(lineStart, cursorPos)
        return if (linePrefix == "- ") {
                text.substring(0, lineStart) + "+ " + text.substring(cursorPos)
        } else {
                text
        }
}

private fun buildContinuationPrefix(
        text: String,
        cursorPos: Int
): String? {
        val beforeCursor = text.substring(0, cursorPos - 1)
        val prevLineStart = beforeCursor.lastIndexOf('\n') + 1
        val rawLine = beforeCursor.substring(prevLineStart)
        val leadingWhitespace = rawLine.takeWhile { it == ' ' || it == '\t' }
        val trimmed = rawLine.trimStart()

        return when {
                trimmed.startsWith("[ ] ") ||
                        trimmed.startsWith("[x] ") ||
                        trimmed == "[ ]" ||
                        trimmed == "[x]" -> leadingWhitespace + "[ ] "
                trimmed.startsWith("+ ") || trimmed == "+" -> leadingWhitespace + "+ "
                trimmed.startsWith("* ") || trimmed == "*" -> leadingWhitespace + "* "
                trimmed.startsWith("> ") || trimmed == ">" -> leadingWhitespace + "> "
                else -> {
                        buildNumericContinuation(trimmed, leadingWhitespace)
                                ?: buildAlphabeticContinuation(trimmed, leadingWhitespace)
                }
        }
}

private val NUMERIC_CONTINUATION_REGEX = Regex("""^(\d+)([.)]?)(?:\s+.*)?$""")
private val ALPHABETIC_CONTINUATION_REGEX = Regex("""^([a-zA-Z])([.)]?)(?:\s+.*)?$""")

private fun buildNumericContinuation(
        trimmedLine: String,
        leadingWhitespace: String
): String? {
        val match =
                NUMERIC_CONTINUATION_REGEX.matchEntire(trimmedLine) ?: return null
        val nextNumber = (match.groupValues[1].toIntOrNull() ?: return null) + 1
        val punctuation = match.groupValues[2]
        return leadingWhitespace + nextNumber + punctuation + " "
}

private fun buildAlphabeticContinuation(
        trimmedLine: String,
        leadingWhitespace: String
): String? {
        val match =
                ALPHABETIC_CONTINUATION_REGEX.matchEntire(trimmedLine) ?: return null
        val currentChar = match.groupValues[1].single()
        if (currentChar == 'z' || currentChar == 'Z') return null
        val nextChar = currentChar + 1
        val punctuation = match.groupValues[2]
        return leadingWhitespace + nextChar + punctuation + " "
}

fun insertEditorSnippetAtCursor(
        currentValue: TextFieldValue,
        snippet: String
): TextFieldValue {
        val cursor = currentValue.selection.end.coerceIn(0, currentValue.text.length)
        val before = currentValue.text.substring(0, cursor)
        val after = currentValue.text.substring(cursor)
        val needsLeadingNewline = before.isNotEmpty() && before.last() != '\n'
        val insertion = (if (needsLeadingNewline) "\n" else "") + snippet
        val updatedText = before + insertion + after
        val newCursor = (before + insertion).length
        return currentValue.copy(
                text = updatedText,
                selection = TextRange(newCursor)
        )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryCountFooter(
        text: String,
        motionPreference: AnimationPreference,
        modifier: Modifier = Modifier
) {
        val wordCount = countWordsIgnoringChecklistMarkers(text)
        val charCount = countCharsIgnoringChecklistMarkers(text)
        val readingTimeMinutes = estimateReadingTimeMinutes(wordCount)

        AnimatedVisibility(
                visible = text.isNotEmpty(),
                enter = motionPreference.enterOrNone(fadeIn(motionPreference.floatTween(200))),
                exit = motionPreference.exitOrNone(fadeOut(motionPreference.floatTween(200))),
                modifier = modifier
        ) {
                FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                        ) {
                                Text(
                                        text = stringResource(R.string.addentry_reading_time_format, readingTimeMinutes),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                        Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                        ) {
                                Text(
                                        text = stringResource(R.string.addentry_words_count_format, wordCount),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                        Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                        ) {
                                Text(
                                        text = stringResource(R.string.addentry_chars_count_format, charCount),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
        }
}
