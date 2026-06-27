package com.mj.yaja.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.tweenSpec
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.data.AnimationPreference
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.KeywordType

@Composable
internal fun KeywordSectionHeader(
    icon: ImageVector,
    title: String,
    count: Int,
    containerColor: Color,
    onContainerColor: Color
) {
    Surface(
        color = containerColor.copy(alpha = 0.28f),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = containerColor.copy(alpha = 0.7f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onContainerColor,
                    modifier = Modifier.padding(5.dp).size(14.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (count > 0) {
                Surface(
                    color = containerColor,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = onContainerColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KeywordCard(
    keyword: KeywordDefinition,
    matchCount: Int,
    isIndexing: Boolean,
    onNavigateToDetail: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val preference = LocalAnimationPreference.current
    val compactCountAlpha = if (preference == AnimationPreference.OFF) {
        if (expanded) 0f else 1f
    } else {
        animateFloatAsState(
            targetValue = if (expanded) 0f else 1f,
            animationSpec = preference.floatTween(180),
            label = "keyword_compact_count_alpha"
        ).value
    }
    val compactCountScale = if (preference == AnimationPreference.OFF) {
        if (expanded) 0.92f else 1f
    } else {
        animateFloatAsState(
            targetValue = if (expanded) 0.92f else 1f,
            animationSpec = preference.floatTween(220),
            label = "keyword_compact_count_scale"
        ).value
    }

    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = preference.tweenSpec(200)),
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
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (keyword.type == KeywordType.PERSON) {
                                Icons.Rounded.Person
                            } else {
                                Icons.Rounded.LocationOn
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        keyword.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (keyword.relation.isNotBlank()) {
                        Text(
                            keyword.relation,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .alpha(compactCountAlpha)
                    ) {
                        Text(
                            text = "$matchCount entries",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = compactCountScale
                                    scaleY = compactCountScale
                                }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
                Switch(
                    checked = keyword.isEnabled,
                    onCheckedChange = onToggleEnabled
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KeywordMetricPill(
                            label = stringResource(R.string.home_stat_entries),
                            value = matchCount.toString(),
                            accent = MetricPillAccent.PRIMARY,
                            modifier = Modifier.weight(1f)
                        )
                        KeywordMetricPill(
                            label = stringResource(R.string.keywords_aliases_label_short),
                            value = keyword.aliases.size.toString(),
                            accent = MetricPillAccent.NEUTRAL,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isIndexing) {
                        KeywordIndexingIndicator()
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KeywordStatusBadge(
                            label = if (isIndexing) stringResource(R.string.keywords_status_indexing) else stringResource(R.string.keywords_status_indexed),
                            isIndexing = isIndexing
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = onDelete,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(stringResource(R.string.keywords_action_delete), style = MaterialTheme.typography.labelLarge)
                        }
                        TextButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(stringResource(R.string.keywords_action_edit), style = MaterialTheme.typography.labelLarge)
                        }
                        TextButton(
                            onClick = onNavigateToDetail,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(stringResource(R.string.keywords_action_view), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeywordStatusBadge(
    label: String,
    isIndexing: Boolean
) {
    Surface(
        color = if (isIndexing) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!isIndexing) {
                Icon(
                    imageVector = Icons.Rounded.Done,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isIndexing) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun KeywordIndexingIndicator() {
    val preference = LocalAnimationPreference.current
    val shimmerAlpha = if (preference == AnimationPreference.OFF) {
        1f
    } else {
        val startAlpha = if (preference == AnimationPreference.REDUCED) 0.8f else 0.45f
        val duration = if (preference == AnimationPreference.REDUCED) 1800 else 900
        val shimmerAlphaState by rememberInfiniteTransition(label = "keyword-indexing")
            .animateFloat(
                initialValue = startAlpha,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "keyword-indexing-alpha"
            )
        shimmerAlphaState
    }

    LinearProgressIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .alpha(shimmerAlpha),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    )
}

@Composable
private fun KeywordMetricPill(
    label: String,
    value: String,
    accent: MetricPillAccent = MetricPillAccent.NEUTRAL,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) =
        when (accent) {
            MetricPillAccent.PRIMARY ->
                MaterialTheme.colorScheme.secondaryContainer to
                    MaterialTheme.colorScheme.onSecondaryContainer
            MetricPillAccent.NEUTRAL ->
                MaterialTheme.colorScheme.surfaceContainerLow to
                    MaterialTheme.colorScheme.onSurface
        }
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.78f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

private enum class MetricPillAccent {
    PRIMARY,
    NEUTRAL
}

@Composable
internal fun KeywordEditorDialog(
    title: String,
    initialKeyword: KeywordDefinition?,
    onDismiss: () -> Unit,
    onSave: (String, KeywordType, String, List<String>, Boolean) -> Unit
) {
    var name by remember(initialKeyword) { mutableStateOf(initialKeyword?.name.orEmpty()) }
    var relation by remember(initialKeyword) { mutableStateOf(initialKeyword?.relation.orEmpty()) }
    var aliases by remember(initialKeyword) {
        mutableStateOf(initialKeyword?.aliases?.joinToString(", ").orEmpty())
    }
    var type by remember(initialKeyword) {
        mutableStateOf(initialKeyword?.type ?: KeywordType.PERSON)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.keywords_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = relation,
                    onValueChange = { relation = it },
                    label = { Text(stringResource(R.string.keywords_relation_context_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = aliases,
                    onValueChange = { aliases = it },
                    label = { Text(stringResource(R.string.keywords_aliases_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = type == KeywordType.PERSON,
                        onCheckedChange = { checked ->
                            type = if (checked) KeywordType.PERSON else KeywordType.PLACE
                        }
                    )
                    Text(stringResource(R.string.keywords_type_person))
                    Spacer(modifier = Modifier.width(12.dp))
                    Checkbox(
                        checked = type == KeywordType.PLACE,
                        onCheckedChange = { checked ->
                            type = if (checked) KeywordType.PLACE else KeywordType.PERSON
                        }
                    )
                    Text(stringResource(R.string.keywords_type_place))
                }
                Text(
                    text = stringResource(R.string.keywords_save_disabled_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name.trim(),
                        type,
                        relation.trim(),
                        aliases.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        false
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
