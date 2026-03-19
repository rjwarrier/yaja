package com.mj.yaja.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

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

    fun parseMarkdown(text: String): AnnotatedString {
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
}

class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val pairs = MarkdownUtils.findPairs(originalText)

        val builder = AnnotatedString.Builder()
        val mapping = FlatOffsetMapping(originalText.length)

        for (i in originalText.indices) {
            val tagPair = pairs.find { i in it.startRange || i in it.endRange }

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
                    builder.append(originalText[i])
                    builder.pop()
                } else {
                    builder.append(originalText[i])
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
