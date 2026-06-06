package com.mj.yaja.ui.app

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CrashLogDialog(
    crashLog: String,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Previous Session Crash Log") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    Text(
                        text = crashLog,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onClear()
                    onDismiss()
                }
            ) {
                Text("Clear & Close")
            }
        }
    )
}
