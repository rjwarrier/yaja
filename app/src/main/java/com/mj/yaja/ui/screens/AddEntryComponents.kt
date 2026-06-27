package com.mj.yaja.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.data.EntryTemplate
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.KeywordType
import com.mj.yaja.ui.theme.contentTextStyle
import com.mj.yaja.ui.utils.MarkdownUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ReviewTodoSuggestion(
        val sourceLine: String,
        val sourceFragment: String,
        val checklistLine: String
)

data class ReviewMentionSuggestion(
        val keywordId: String,
        val name: String,
        val type: KeywordType,
        val matches: Int
)

fun buildReviewTodoSuggestions(text: String): List<ReviewTodoSuggestion> {
        return text.lineSequence()
                .map { it.trim() }
                .filter { line ->
                        line.isNotBlank() &&
                                !line.startsWith("[ ]") &&
                                !line.startsWith("[x]", ignoreCase = true)
                }
                .flatMap { line ->
                        extractTodoTexts(line).map { fragment ->
                                ReviewTodoSuggestion(
                                        sourceLine = line,
                                        sourceFragment = fragment,
                                        checklistLine = "[ ] $fragment"
                                )
                        }
                }
                .distinctBy { it.checklistLine.lowercase() }
                .toList()
}

fun convertReviewTodosToChecklist(text: String): String {
        return text.lineSequence()
                .joinToString("\n") { rawLine ->
                        val todoItems = extractTodoTexts(rawLine)
                        if (todoItems.isEmpty()) {
                                rawLine
                        } else {
                                val checklistBlock = todoItems.joinToString("\n") { "[ ] $it" }
                                if (startsWithTodoTrigger(rawLine.trim())) {
                                        checklistBlock
                                } else {
                                        rawLine + "\n" + checklistBlock
                                }
                        }
                }
}

private fun extractTodoTexts(line: String): List<String> {
        val clauses =
                line.split(Regex("(?<=[.!?;])\\s+|\\s+[-+*>\\u2022]\\s+"))
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
        val lineStartsWithTodo = startsWithTodoTrigger(line.trim())
        return clauses.mapIndexedNotNull { index, clause ->
                        extractTodoTextFromClause(clause)
                                ?: if (lineStartsWithTodo && index > 0) {
                                        normalizeImplicitTodoClause(clause)
                                } else {
                                        null
                                }
                }
                .distinctBy { it.lowercase() }
}

private fun extractTodoTextFromClause(clause: String): String? {
        if (clause.startsWith("[ ]") || clause.startsWith("[x]", ignoreCase = true)) return null
        if (clause.startsWith("- ")) return clause.removePrefix("- ").trim()
        if (clause.startsWith("+ ")) return clause.removePrefix("+ ").trim()
        if (clause.startsWith("* ")) return clause.removePrefix("* ").trim()
        if (clause.startsWith("> ")) return clause.removePrefix("> ").trim()

        val match = todoTriggerRegex.find(clause) ?: return null
        val extracted =
                clause.substring(match.range.last + 1)
                        .trim()
                        .trimStart(':', '-', ' ', ',', '.')
                        .trimEnd('.', '!', '?', ';', ',')
                        .trim()
        return extracted.takeIf { it.isNotBlank() }
}

private fun normalizeImplicitTodoClause(clause: String): String? {
        if (clause.startsWith("[ ]") || clause.startsWith("[x]", ignoreCase = true)) return null
        val normalized =
                clause.removePrefix("- ")
                        .removePrefix("* ")
                        .trim()
                        .trimStart(':', '-', ' ', ',', '.')
                        .trimEnd('.', '!', '?', ';', ',')
                        .trim()
        return normalized.takeIf { it.isNotBlank() }
}

private fun startsWithTodoTrigger(line: String): Boolean {
        if (line.startsWith("[ ]") || line.startsWith("[x]", ignoreCase = true)) return false
        if (line.startsWith("- ") || line.startsWith("* ")) return true
        return todoTriggerRegex.find(line)?.range?.first == 0
}

private val todoTriggerRegex =
        Regex(
                "(?i)\\b(todo:|todo\\s+|need to\\s+|have to\\s+|should\\s+|must\\s+|plan to\\s+|remember to\\s+|remember\\s+|i should\\s+|i need to\\s+|i have to\\s+|got to\\s+|dont forget to\\s+|don't forget to\\s+|follow up with\\s+)"
        )

fun buildReviewMentionSuggestions(
        text: String,
        keywords: List<KeywordDefinition>
): List<ReviewMentionSuggestion> {
        if (text.isBlank()) return emptyList()
        return keywords.asSequence()
                .filter { it.isEnabled }
                .mapNotNull { keyword ->
                        val terms =
                                (listOf(keyword.name) + keyword.aliases)
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                        val matches =
                                terms.sumOf { term ->
                                        Regex(
                                                        "\\b${Regex.escape(term)}\\b",
                                                        RegexOption.IGNORE_CASE
                                                )
                                                .findAll(text)
                                                .count()
                                }
                        if (matches > 0) {
                                ReviewMentionSuggestion(
                                        keywordId = keyword.id,
                                        name = keyword.name,
                                        type = keyword.type,
                                        matches = matches
                                )
                        } else {
                                null
                        }
                }
                .sortedWith(
                        compareByDescending<ReviewMentionSuggestion> { it.matches }
                                .thenBy { it.name.lowercase() }
                )
                .toList()
}

fun applyFormatting(
        currentValue: TextFieldValue,
        prefix: String,
        suffix: String,
        onValueChange: (TextFieldValue) -> Unit
) {
        val text = currentValue.text
        val selection = currentValue.selection
        val start = selection.min
        val end = selection.max

        if (selection.collapsed) {
                val pairs = MarkdownUtils.findPairs(text)
                val styleToMatch = MarkdownUtils.findStyleByPrefix(prefix)
                val activePair =
                        pairs.find { pair: MarkdownUtils.TagPair ->
                                pair.style == styleToMatch &&
                                        start >= pair.startRange.first &&
                                        start <= pair.endRange.last
                        }

                if (activePair != null) {
                        val newCursorPos = activePair.endRange.last + 1
                        onValueChange(TextFieldValue(text, selection = TextRange(newCursorPos)))
                } else {
                        val newText = text.substring(0, start) + prefix + suffix + text.substring(end)
                        val newCursorPos = start + prefix.length
                        onValueChange(TextFieldValue(newText, selection = TextRange(newCursorPos)))
                }
                return
        }

        val selectedText = text.substring(start, end)
        val isWrapped = selectedText.startsWith(prefix) && selectedText.endsWith(suffix)

        if (isWrapped) {
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
                val cleanedSelectedText = selectedText.replace(prefix, "").replace(suffix, "")
                val wrappedText = prefix + cleanedSelectedText + suffix
                val newText = text.substring(0, start) + wrappedText + text.substring(end)
                onValueChange(
                        TextFieldValue(
                                newText,
                                selection = TextRange(start, start + wrappedText.length)
                        )
                )
        }
}
