package com.mj.yaja.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.data.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSection(
    appLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    SettingsSectionHeader(icon = Icons.Rounded.Language, title = stringResource(R.string.settings_language))
    Spacer(modifier = Modifier.height(12.dp))

    var expanded by remember { mutableStateOf(false) }
    val systemDefaultLabel = stringResource(R.string.settings_language_system_default)

    val currentLanguageLabel = if (appLanguage == AppLanguage.SYSTEM) {
        systemDefaultLabel
    } else {
        appLanguage.nativeName ?: appLanguage.name
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .clickable { expanded = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_language_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentLanguageLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_language_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.exposedDropdownSize(true)
                ) {
                    AppLanguage.entries.forEach { language ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (language == AppLanguage.SYSTEM) {
                                        systemDefaultLabel
                                    } else {
                                        language.nativeName ?: language.name
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            onClick = {
                                onLanguageSelected(language)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
}
