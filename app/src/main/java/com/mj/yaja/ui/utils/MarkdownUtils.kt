package com.mj.yaja.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.KeywordType
import java.time.LocalDate

object MarkdownUtils {

    data class MarkdownStyle(val startTag: String, val endTag: String, val spanStyle: SpanStyle)

    data class TagPair(val style: SpanStyle, val startRange: IntRange, val endRange: IntRange)

    val STYLES =
            listOf(
                    MarkdownStyle("**", "**", SpanStyle(fontWeight = FontWeight.Bold)),
                    MarkdownStyle("*", "*", SpanStyle(fontStyle = FontStyle.Italic)),
                    MarkdownStyle("_", "_", SpanStyle(fontStyle = FontStyle.Italic))
            )

    fun findStyleByPrefix(prefix: String): SpanStyle? {
        return STYLES.find { it.startTag == prefix }?.spanStyle
    }

    private val headingRegex = Regex("""^\s*(#{2,3})\s+""")
    private val todoMarkerRegex = Regex("""^(\s*(?:[+*\-]\s+)?)\[( |x|X)\](\s+.*)?$""")

    fun parseMarkdown(text: String): AnnotatedString {
        val lines = text.split("\n")
        return buildAnnotatedString {
            lines.forEachIndexed { index, line ->
                val headingMatch = headingRegex.find(line)
                val contentLine =
                    if (headingMatch != null) {
                        line.substring(headingMatch.range.last + 1).trimStart()
                    } else {
                        normalizeTodoMarkerForDisplay(line)
                    }
                val parsedLine = parseInlineMarkdown(contentLine)
                val start = length
                append(parsedLine)
                if (headingMatch != null && length > start) {
                    addStyle(
                        style = headingStyle(headingMatch.groupValues[1].length),
                        start = start,
                        end = length
                    )
                }
                if (index < lines.lastIndex) append('\n')
            }
        }
    }

    fun normalizeTodoMarkerForDisplay(line: String): String {
        val match = todoMarkerRegex.find(line) ?: return line
        val marker = if (match.groupValues[2].equals("x", ignoreCase = true)) "x" else " "
        return match.groupValues[1] + "[$marker]" + match.groupValues[3]
    }

    private fun parseInlineMarkdown(text: String): AnnotatedString {
        val pairs = findPairs(text)
        return buildAnnotatedString {
            var i = 0
            var pairIndex = 0
            while (i < text.length) {
                // Advance past any pairs that have ended before position i
                while (pairIndex < pairs.size && pairs[pairIndex].endRange.last < i) pairIndex++

                val pair = if (pairIndex < pairs.size) pairs[pairIndex] else null

                when {
                    pair != null && i in pair.startRange -> {
                        // Skip start-tag characters (hidden by VisualTransformation)
                        i++
                    }
                    pair != null && i in pair.endRange -> {
                        // Skip end-tag characters
                        i++
                    }
                    pair != null && i > pair.startRange.last && i < pair.endRange.first -> {
                        // Inside a styled span — collect all overlapping pairs for this char
                        val activeStyles = pairs
                            .filter { i > it.startRange.last && i < it.endRange.first }
                            .map { it.style }
                        pushStyle(activeStyles.reduce { acc, style -> acc.merge(style) })
                        append(text[i])
                        pop()
                        i++
                    }
                    else -> {
                        append(text[i])
                        i++
                    }
                }
            }
        }
    }

    private fun headingStyle(level: Int): SpanStyle =
        when (level) {
            2 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp)
            3 -> SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            else -> SpanStyle(fontWeight = FontWeight.SemiBold)
        }

    fun findPairs(text: String): List<TagPair> {
        val pairs = mutableListOf<TagPair>()
        val usedIndices = mutableSetOf<Int>()

        fun findNext(s: String, start: Int): Int {
            var idx = text.indexOf(s, start)
            while (idx != -1) {
                if ((idx until idx + s.length).none { it in usedIndices }) return idx
                idx = text.indexOf(s, idx + 1)
            }
            return -1
        }

        // Process longer start tags first
        val sortedStyles = STYLES.sortedByDescending { it.startTag.length }

        for (styleDef in sortedStyles) {
            var searchIdx = 0
            while (true) {
                val startIdx = findNext(styleDef.startTag, searchIdx)
                if (startIdx == -1) break

                val endIdx = findNext(styleDef.endTag, startIdx + styleDef.startTag.length)
                if (endIdx == -1) {
                    searchIdx = startIdx + 1
                    continue
                }

                pairs.add(
                        TagPair(
                                styleDef.spanStyle,
                                startIdx until startIdx + styleDef.startTag.length,
                                endIdx until endIdx + styleDef.endTag.length
                        )
                )

                // Mark tags as used
                for (i in startIdx until startIdx + styleDef.startTag.length) usedIndices.add(i)
                for (i in endIdx until endIdx + styleDef.endTag.length) usedIndices.add(i)

                searchIdx = startIdx + 1
            }
        }
        return pairs.sortedBy { it.startRange.first }
    }

    fun stripMetadata(text: String): String {
        return text.replace(Regex("<!--.*?-->"), "").trim()
    }

    /**
     * Parse markdown styling AND annotate date references as tappable [LinkAnnotation.Clickable]
     * links that call [onDateClick] when tapped.
     *
     * Date detection runs on the already-rendered (tag-stripped) text so indices are correct
     * regardless of how many markdown tags were removed.
     *
     * @param text       Raw entry text (may contain ** / * / _ markdown)
     * @param entryDate  The date this entry belongs to (used to resolve relative references)
     * @param linkColor  Color applied to date link spans
     * @param onDateClick Invoked with the resolved [LocalDate] when a link is tapped
     */
    fun parseMarkdownWithDateLinks(
        text: String,
        entryDate: LocalDate,
        linkColor: Color,
        personHighlightColor: Color? = null,
        placeHighlightColor: Color? = null,
        keywords: List<KeywordDefinition> = emptyList(),
        monthFirst: Boolean = DateLinkUtils.isMonthFirst(),
        customKeywords: List<com.mj.yaja.data.DateKeywordEntry> = emptyList(),
        onDateClick: (LocalDate) -> Unit
    ): AnnotatedString {
        // Apply markdown styling first; its output has tag chars stripped
        val markdownAnnotated = parseMarkdown(text)
        val cleanText = markdownAnnotated.text

        val dateLinks = DateLinkUtils.detectDateLinks(cleanText, entryDate, monthFirst, customKeywords)
        val keywordHighlights =
            if (keywords.isEmpty() || personHighlightColor == null || placeHighlightColor == null) {
                emptyList()
            } else {
                detectKeywordHighlights(
                    cleanText = cleanText,
                    keywords = keywords,
                    personColor = personHighlightColor,
                    placeColor = placeHighlightColor,
                    blockedRanges = dateLinks.map { it.range }
                )
            }

        if (dateLinks.isEmpty() && keywordHighlights.isEmpty()) return markdownAnnotated

        return buildAnnotatedString {
            // Copy all existing markdown SpanStyles
            append(markdownAnnotated)
            for (highlight in keywordHighlights) {
                addStyle(
                    style = SpanStyle(
                        color = highlight.color,
                        fontWeight = FontWeight.SemiBold
                    ),
                    start = highlight.range.first,
                    end = highlight.range.last + 1
                )
            }
            // Overlay link annotations
            for (link in dateLinks) {
                addLink(
                    clickable = LinkAnnotation.Clickable(
                        tag = link.date.toString(),
                        styles = TextLinkStyles(
                            style = SpanStyle(color = linkColor)
                        ),
                        linkInteractionListener = { onDateClick(link.date) }
                    ),
                    start = link.range.first,
                    end   = link.range.last + 1
                )
            }
        }
    }

    private data class KeywordHighlight(
        val range: IntRange,
        val color: Color
    )

    private fun detectKeywordHighlights(
        cleanText: String,
        keywords: List<KeywordDefinition>,
        personColor: Color,
        placeColor: Color,
        blockedRanges: List<IntRange>
    ): List<KeywordHighlight> {
        val highlights = mutableListOf<KeywordHighlight>()
        val occupied = blockedRanges.toMutableList()

        val sortedKeywords = keywords
            .filter { it.isEnabled }
            .sortedByDescending { it.name.length }

        for (keyword in sortedKeywords) {
            val color = if (keyword.type == KeywordType.PERSON) personColor else placeColor
            val terms = (listOf(keyword.name) + keyword.aliases)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .sortedByDescending { it.length }

            for (term in terms) {
                val regex = Regex("""(?<!\p{L})${Regex.escape(term)}(?!\p{L})""", RegexOption.IGNORE_CASE)
                regex.findAll(cleanText).forEach { match ->
                    val range = match.range
                    val overlaps = occupied.any { existing ->
                        existing.first <= range.last && range.first <= existing.last
                    }
                    if (!overlaps) {
                        highlights += KeywordHighlight(range = range, color = color)
                        occupied += range
                    }
                }
            }
        }

        return highlights.sortedBy { it.range.first }
    }
}

class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val pairs = MarkdownUtils.findPairs(originalText)

        val builder = AnnotatedString.Builder()
        val mapping = FlatOffsetMapping(originalText.length)

        for (i in originalText.indices) {
            val tagPair = pairs.find { i in it.startRange || i in it.endRange }
            val displayChar =
                    if (
                            originalText[i] == 'X' &&
                                    i > 0 &&
                                    i + 1 < originalText.length &&
                                    originalText[i - 1] == '[' &&
                                    originalText[i + 1] == ']'
                    ) {
                            'x'
                    } else {
                            originalText[i]
                    }

            if (tagPair != null) {
                mapping.addMapping(i, builder.length)
            } else {
                val activeStyles =
                        pairs.filter { i in it.startRange.last + 1 until it.endRange.first }.map {
                            it.style
                        }

                mapping.addMapping(i, builder.length)

                if (activeStyles.isNotEmpty()) {
                    builder.pushStyle(activeStyles.reduce { acc, style -> acc.merge(style) })
                    builder.append(displayChar)
                    builder.pop()
                } else {
                    builder.append(displayChar)
                }
            }
        }

        mapping.addMapping(originalText.length, builder.length)

        return TransformedText(builder.toAnnotatedString(), mapping)
    }

    private class FlatOffsetMapping(originalLength: Int) : OffsetMapping {
        private val originalToTransformed = IntArray(originalLength + 1)
        private val transformedToOriginal = mutableListOf<Int>()

        fun addMapping(originalOffset: Int, transformedOffset: Int) {
            originalToTransformed[originalOffset] = transformedOffset
            while (transformedToOriginal.size <= transformedOffset) {
                transformedToOriginal.add(originalOffset)
            }
        }

        override fun originalToTransformed(offset: Int): Int {
            val safeOffset = offset.coerceIn(0, originalToTransformed.size - 1)
            return originalToTransformed[safeOffset]
        }

        override fun transformedToOriginal(offset: Int): Int {
            if (transformedToOriginal.isEmpty()) return 0
            val safeOffset = offset.coerceIn(0, transformedToOriginal.size - 1)
            return transformedToOriginal[safeOffset]
        }
    }
}
