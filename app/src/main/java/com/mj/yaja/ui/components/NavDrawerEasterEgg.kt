package com.mj.yaja.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.theme.BodoniModaFamily
import com.mj.yaja.ui.theme.LibreBaskervilleFamily
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
internal fun YajaLogoEasterEgg(
        onClose: () -> Unit
) {
        BackHandler(onBack = onClose)
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        MaterialTheme.colorScheme.background,
                                                                        MaterialTheme.colorScheme.surfaceContainerLow,
                                                                        MaterialTheme.colorScheme.surfaceContainer.copy(
                                                                                alpha = 0.98f
                                                                        )
                                                                )
                                                )
                                        )
                                        .padding(horizontal = 28.dp, vertical = 32.dp)
                ) {
                        Box(
                                modifier =
                                        Modifier.align(Alignment.TopStart)
                                                .padding(top = 44.dp, start = 112.dp)
                                                .size(width = 132.dp, height = 40.dp)
                                                .border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                                        shape = RoundedCornerShape(999.dp)
                                                )
                        )
                        Box(
                                modifier =
                                        Modifier.align(Alignment.TopStart)
                                                .padding(top = 86.dp, start = 12.dp)
                                                .size(132.dp)
                                                .background(
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                        shape = RoundedCornerShape(36.dp)
                                                )
                        )
                        Box(
                                modifier =
                                        Modifier.align(Alignment.TopEnd)
                                                .padding(top = 88.dp, end = 40.dp)
                                                .size(156.dp)
                                                .border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                                        shape = CircleShape
                                                )
                        )
                        Box(
                                modifier =
                                        Modifier.align(Alignment.TopEnd)
                                                .padding(top = 150.dp, end = 18.dp)
                                                .size(width = 110.dp, height = 110.dp)
                                                .background(
                                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.07f),
                                                        shape = CircleShape
                                                )
                        )
                        Box(
                                modifier =
                                        Modifier.align(Alignment.CenterStart)
                                                .padding(start = 8.dp)
                                                .size(width = 116.dp, height = 184.dp)
                                                .graphicsLayer { rotationZ = -14f }
                                                .border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.13f),
                                                        shape = RoundedCornerShape(34.dp)
                                                )
                        )
                        Box(
                                modifier =
                                        Modifier.align(Alignment.CenterEnd)
                                                .padding(end = 24.dp)
                                                .size(width = 68.dp, height = 180.dp)
                                                .background(
                                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                                                        shape = RoundedCornerShape(28.dp)
                                                )
                        )
                        Box(
                                modifier =
                                        Modifier.align(Alignment.BottomEnd)
                                                .padding(end = 42.dp, bottom = 188.dp)
                                                .size(width = 136.dp, height = 34.dp)
                                                .border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                        shape = RoundedCornerShape(20.dp)
                                                )
                        )
                        Box(
                                modifier =
                                        Modifier.align(Alignment.BottomStart)
                                                .padding(start = 18.dp, bottom = 150.dp)
                                                .size(width = 150.dp, height = 54.dp)
                                                .background(
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                                        shape = RoundedCornerShape(20.dp)
                                                )
                        )
                        Box(
                                modifier =
                                        Modifier.align(Alignment.Center)
                                                .offset(y = (-132).dp)
                                                .size(width = 220.dp, height = 1.dp)
                                                .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                                                        RoundedCornerShape(999.dp)
                                                )
                        )
                        Box(
                                modifier =
                                        Modifier.align(Alignment.BottomCenter)
                                                .padding(bottom = 132.dp)
                                                .size(width = 1.dp, height = 88.dp)
                                                .background(
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                                        RoundedCornerShape(999.dp)
                                                )
                        )

                        Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                        ) {
                                Spacer(Modifier.height(24.dp))

                                Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(26.dp)
                                ) {
                                        Surface(
                                                modifier = Modifier.size(186.dp),
                                                shape = RoundedCornerShape(52.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                tonalElevation = 10.dp,
                                                shadowElevation = 12.dp
                                        ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                                painter = painterResource(id = R.drawable.rj_logo),
                                                                contentDescription = "Yaja Logo",
                                                                modifier =
                                                                        Modifier.size(186.dp)
                                                                                .padding(30.dp),
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .onPrimaryContainer
                                                        )
                                                }
                                        }

                                        Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(18.dp)
                                        ) {
                                                Text(
                                                        text = "yaja",
                                                        style =
                                                                MaterialTheme.typography.displayLarge
                                                                        .copy(
                                                                                fontFamily =
                                                                                        BodoniModaFamily,
                                                                                fontWeight =
                                                                                        FontWeight.ExtraBold
                                                                        ),
                                                        color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                        text = "yet another journaling app",
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        textAlign = TextAlign.Center
                                                )
                                                Text(
                                                        text =
                                                                buildAnnotatedString {
                                                                        append("\"Write down a passing thought or what you had for lunch; it doesn't have to be profound.\n")
                                                                        withStyle(
                                                                                SpanStyle(
                                                                                        fontStyle =
                                                                                                FontStyle.Italic
                                                                                )
                                                                        ) {
                                                                                append("Your words can just exist.")
                                                                        }
                                                                        append("\"")
                                                                },
                                                        style =
                                                                MaterialTheme.typography.headlineSmall
                                                                        .copy(
                                                                                fontFamily =
                                                                                        LibreBaskervilleFamily,
                                                                                fontWeight =
                                                                                        FontWeight.Normal
                                                                        ),
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .primary
                                                                        .copy(alpha = 0.88f),
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(top = 158.dp)
                                                )
                                        }
                                }

                                Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                        Text(
                                                text = "from the labs of RJ",
                                                style = MaterialTheme.typography.titleSmall,
                                                color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.82f),
                                                textAlign = TextAlign.Center
                                        )
                                        FilledTonalButton(
                                                onClick = onClose,
                                                shape = RoundedCornerShape(24.dp),
                                                contentPadding =
                                                        PaddingValues(
                                                                horizontal = 24.dp,
                                                                vertical = 14.dp
                                                        )
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(10.dp))
                                                Text(
                                                        text = "Close",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                )
                                        }
                                        Text(
                                                text = "",
                                                style = MaterialTheme.typography.labelMedium,
                                                color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.72f)
                                        )
                                }
                        }
                }
        }
}
