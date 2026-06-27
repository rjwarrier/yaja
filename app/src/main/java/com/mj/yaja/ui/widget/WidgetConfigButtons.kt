package com.mj.yaja.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun WidgetConfigApplyButton(
    onClick: () -> Unit,
    text: String = "Apply"
) {
    TextButton(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.padding(start = 6.dp)
    ) {
        Text(text)
    }
}

@Composable
internal fun WidgetConfigCancelButton(
    onClick: () -> Unit,
    text: String = "Cancel"
) {
    TextButton(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text)
    }
}

@Composable
internal fun WidgetConfigButtonRow(
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WidgetConfigCancelButton(onClick = onCancel)
        WidgetConfigApplyButton(onClick = onApply)
    }
}
