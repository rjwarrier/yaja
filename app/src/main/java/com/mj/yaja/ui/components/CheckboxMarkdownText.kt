package com.mj.yaja.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mj.yaja.data.DateKeywordEntry
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.ui.utils.DateLinkUtils
import com.mj.yaja.ui.utils.MarkdownUtils
import java.time.LocalDate

private val checkboxLineRegex = Regex("""^(\s*(?:[+*\-]\s+)?)\[( |x|X)\](?:\s+(.*))?$""")

@Composable
fun ExpressiveCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    val shape = RoundedCornerShape(7.dp)
    val containerColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        },
        label = "expressive_checkbox_container"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "expressive_checkbox_border"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.72f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "expressive_checkbox_scale"
    )

    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = containerColor,
        border = BorderStroke(2.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(Color.Transparent)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier
                        .scale(checkScale)
                        .size((size.value * 0.54f).dp)
                )
            }
        }
    }
}

@Composable
fun CheckboxMarkdownText(
    text: String,
    renderCheckboxesAsText: Boolean,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    entryDate: LocalDate? = null,
    onDateLinkClick: ((LocalDate) -> Unit)? = null,
    keywords: List<KeywordDefinition> = emptyList(),
    monthFirst: Boolean = DateLinkUtils.isMonthFirst(),
    customKeywords: List<DateKeywordEntry> = emptyList(),
    lineSpacing: Dp = 6.dp,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    val hasTodoLines = remember(text) {
        text.lineSequence().any { checkboxLineRegex.matches(it) }
    }

    if (renderCheckboxesAsText || !hasTodoLines) {
        Text(
            text = buildAnnotatedMarkdownText(
                text = text,
                entryDate = entryDate,
                onDateLinkClick = onDateLinkClick,
                linkColor = MaterialTheme.colorScheme.primary,
                keywords = keywords,
                monthFirst = monthFirst,
                customKeywords = customKeywords
            ),
            modifier = modifier,
            style = style,
            color = color,
            overflow = overflow,
            maxLines = maxLines,
            lineHeight = style.lineHeight
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(lineSpacing)
    ) {
        text.lines().forEach { line ->
            val checkboxMatch = checkboxLineRegex.find(line)
            if (checkboxMatch == null) {
                Text(
                    text = buildAnnotatedMarkdownText(
                        text = line,
                        entryDate = entryDate,
                        onDateLinkClick = onDateLinkClick,
                        linkColor = MaterialTheme.colorScheme.primary,
                        keywords = keywords,
                        monthFirst = monthFirst,
                        customKeywords = customKeywords
                    ),
                    style = style,
                    color = color,
                    lineHeight = style.lineHeight
                )
            } else {
                val checked = checkboxMatch.groupValues[2].equals("x", ignoreCase = true)
                val content = checkboxMatch.groupValues.getOrElse(3) { "" }.trim()
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExpressiveCheckbox(
                        checked = checked,
                        modifier = Modifier.padding(top = 1.dp),
                        size = 22.dp
                    )
                    Text(
                        text = buildAnnotatedMarkdownText(
                            text = content,
                            entryDate = entryDate,
                            onDateLinkClick = onDateLinkClick,
                            linkColor = MaterialTheme.colorScheme.primary,
                            keywords = keywords,
                            monthFirst = monthFirst,
                            customKeywords = customKeywords
                        ),
                        modifier = Modifier.weight(1f, fill = false),
                        style = style,
                        color = color,
                        lineHeight = style.lineHeight
                    )
                }
            }
        }
    }
}

@Composable
private fun buildAnnotatedMarkdownText(
    text: String,
    entryDate: LocalDate?,
    onDateLinkClick: ((LocalDate) -> Unit)?,
    linkColor: Color,
    keywords: List<KeywordDefinition>,
    monthFirst: Boolean,
    customKeywords: List<DateKeywordEntry>
) = if (entryDate != null && onDateLinkClick != null) {
    MarkdownUtils.parseMarkdownWithDateLinks(
        text = text,
        entryDate = entryDate,
        linkColor = linkColor,
        personHighlightColor = MaterialTheme.colorScheme.secondary,
        placeHighlightColor = MaterialTheme.colorScheme.tertiary,
        keywords = keywords,
        monthFirst = monthFirst,
        customKeywords = customKeywords,
        onDateClick = onDateLinkClick
    )
} else {
    MarkdownUtils.parseMarkdown(text)
}
