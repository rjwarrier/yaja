package com.mj.yaja.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.data.AppFontFamily
import com.mj.yaja.data.ThemePreference
import com.mj.yaja.ui.theme.GoogleSansFamily
import com.mj.yaja.ui.theme.JetBrainsMono
import com.mj.yaja.ui.theme.LibreBaskervilleFamily

@Composable
fun FontSelectionCard(
        label: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        fontFamily: AppFontFamily,
        modifier: Modifier = Modifier,
        customFamily: androidx.compose.ui.text.font.FontFamily? = null
) {
        val targetFontFamily =
                when (fontFamily) {
                        AppFontFamily.SANS_SERIF -> GoogleSansFamily
                        AppFontFamily.SERIF -> LibreBaskervilleFamily
                        AppFontFamily.MONO -> JetBrainsMono
                        // Falls back to the system default until a font is uploaded.
                        AppFontFamily.CUSTOM -> customFamily
                }

        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                ElevatedCard(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors =
                                CardDefaults.elevatedCardColors(
                                        containerColor =
                                                if (isSelected)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                        elevation =
                                CardDefaults.elevatedCardElevation(
                                        defaultElevation = if (isSelected) 4.dp else 0.dp
                                )
                ) {
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) {
                                Text(
                                        text = "A",
                                        style =
                                                TextStyle(
                                                        fontFamily = targetFontFamily,
                                                        fontSize = 32.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color =
                                                                if (isSelected)
                                                                        MaterialTheme.colorScheme
                                                                                .onPrimaryContainer
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                )
                                )
                        }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color =
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
        }
}

@Composable
fun SettingsSectionHeader(icon: ImageVector, title: String) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                )
        }
}

@Composable
fun ThemeSelectionCard(
        label: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        preference: ThemePreference,
        modifier: Modifier = Modifier
) {
        ElevatedCard(
                onClick = onClick,
                modifier = modifier.height(100.dp),
                shape = MaterialTheme.shapes.medium,
                colors =
                        CardDefaults.elevatedCardColors(
                                containerColor =
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                elevation =
                        CardDefaults.elevatedCardElevation(
                                defaultElevation = if (isSelected) 4.dp else 0.dp
                        )
        ) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                        // Preview Box
                        Box(
                                modifier =
                                        Modifier.size(40.dp)
                                                .background(
                                                        color =
                                                                when (preference) {
                                                                        ThemePreference.LIGHT ->
                                                                                Color(0xFFF8F9FA)
                                                                        ThemePreference.DARK ->
                                                                                Color(0xFF1A1C1E)
                                                                        ThemePreference.AMOLED ->
                                                                                Color.Black
                                                                        ThemePreference.SYSTEM ->
                                                                                Color.Gray // Fallback
                                                                },
                                                        shape = CircleShape
                                                )
                                                .let { baseModifier ->
                                                        if (preference == ThemePreference.SYSTEM) {
                                                                baseModifier.background(
                                                                        brush =
                                                                                androidx.compose.ui
                                                                                        .graphics
                                                                                        .Brush
                                                                                        .linearGradient(
                                                                                                colors =
                                                                                                        listOf(
                                                                                                                Color(
                                                                                                                        0xFFF8F9FA
                                                                                                                ),
                                                                                                                Color(
                                                                                                                        0xFF1A1C1E
                                                                                                                )
                                                                                                        )
                                                                                        ),
                                                                        shape = CircleShape
                                                                )
                                                        } else baseModifier
                                                }
                                                .padding(8.dp),
                                contentAlignment = Alignment.Center
                        ) {
                                if (isSelected) {
                                        Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint =
                                                        if (preference == ThemePreference.LIGHT)
                                                                Color.Black
                                                        else Color.White,
                                                modifier = Modifier.size(20.dp)
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = label.replace(" Default", "").replace(" Black", ""),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color =
                                        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                        )
                }
        }
}
