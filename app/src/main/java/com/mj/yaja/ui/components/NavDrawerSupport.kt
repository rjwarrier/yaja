package com.mj.yaja.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun DrawerQuickPreferenceSection(
        title: String,
        content: @Composable () -> Unit
) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                )
                content()
        }
}

@Composable
internal fun DrawerPreferenceRow(
        title: String,
        content: @Composable () -> Unit
) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                                androidx.compose.material3.Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                        }
                }
                content()
        }
}

@Composable
internal fun DrawerChipRow(
        modifier: Modifier = Modifier,
        content: @Composable RowScope.() -> Unit
) {
        Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                content()
        }
}

@Composable
internal fun DrawerHeaderIconButton(
        icon: ImageVector,
        contentDescription: String,
        onClick: () -> Unit
) {
        FilledTonalIconButton(
                onClick = onClick,
                colors =
                        IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.primary
                        ),
                modifier = Modifier.size(40.dp)
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(18.dp)
                )
        }
}

internal fun estimateNavRemainingTimeText(progress: Float, startedAtMillis: Long?): String? {
        val startedAt = startedAtMillis ?: return null
        val safeProgress = progress.coerceIn(0f, 1f)
        if (safeProgress < 0.01f || safeProgress >= 0.995f) return null
        val elapsedMillis = System.currentTimeMillis() - startedAt
        if (elapsedMillis < 500L) return null
        val estimatedTotalMillis = (elapsedMillis / safeProgress).toLong()
        val remainingMillis = (estimatedTotalMillis - elapsedMillis).coerceAtLeast(0L)
        if (remainingMillis < 1_000L) return "almost done"
        val remainingSeconds = (remainingMillis + 999L) / 1_000L
        return when {
                remainingSeconds < 60L -> "~${remainingSeconds}s left"
                remainingSeconds < 3_600L -> "~${(remainingSeconds + 30L) / 60L}m left"
                else -> "~${(remainingSeconds + 1_800L) / 3_600L}h left"
        }
}

@Composable
internal fun DrawerCacheStatus(
        progress: Float?,
        label: String,
        startedAtMillis: Long?
) {
        val percent = progress?.let { (it * 100).toInt().coerceIn(0, 100) }
        val eta = progress?.let { estimateNavRemainingTimeText(it, startedAtMillis) }
        Surface(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium
        ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        androidx.compose.material3.Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        if (progress != null) {
                                LinearProgressIndicator(
                                        progress = { progress.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth()
                                )
                        } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                                text = when {
                                        percent != null && eta != null -> "$percent% - $eta"
                                        percent != null -> "$percent%"
                                        else -> "Working..."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
        }
}
