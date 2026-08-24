package com.mj.yaja.ui.screens

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.ShortText
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.components.AnimatedMenuButton
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.expressiveFabMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShortcodesTopBar(
    onOpenDrawer: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onHelp: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    LargeTopAppBar(
        title = {
            Text(
                stringResource(R.string.shortcodes_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        navigationIcon = {
            AnimatedMenuButton(
                onClick = onOpenDrawer,
                modifier = Modifier.padding(start = 8.dp)
            )
        },
        actions = {
            IconButton(onClick = onImport) {
                Icon(Icons.Rounded.FileOpen, contentDescription = stringResource(R.string.shortcodes_import_cd))
            }
            IconButton(onClick = onExport) {
                Icon(Icons.Rounded.SaveAlt, contentDescription = stringResource(R.string.shortcodes_export_cd))
            }
            IconButton(onClick = onHelp) {
                Icon(Icons.AutoMirrored.Rounded.Help, contentDescription = stringResource(R.string.shortcodes_help_cd))
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            titleContentColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
internal fun ShortcodesFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fabInteraction = androidx.compose.runtime.remember { MutableInteractionSource() }
    AppStaggeredEntrance(
        visible = visible,
        index = 0,
        strength = AppEntranceStrength.SUBTLE,
        modifier = modifier
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            interactionSource = fabInteraction,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.shortcodes_new)) },
            modifier = Modifier
                .height(64.dp)
                .expressiveFabMotion(fabInteraction),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
internal fun ShortcodesScreenContent(
    customShortcodes: Map<String, String>,
    entranceTriggered: Boolean,
    paddingValues: PaddingValues,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    AnimatedContent(
        targetState = customShortcodes.isEmpty(),
        transitionSpec = {
            (fadeIn(animationSpec = tween(220)) + slideInVertically(tween(240)) { it / 10 }) togetherWith
                fadeOut(animationSpec = tween(180))
        },
        modifier = Modifier.padding(paddingValues),
        label = "ScreenContent"
    ) { isEmpty ->
        if (isEmpty) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ShortText,
                    contentDescription = null,
                    modifier = Modifier.height(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.shortcodes_empty_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.shortcodes_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                val shortcodeList = customShortcodes.toList().sortedBy { it.first }
                items(
                    items = shortcodeList.indices.toList(),
                    key = { index -> shortcodeList[index].first }
                ) { index ->
                    val item = shortcodeList[index]
                    val code = item.first
                    val value = item.second
                    ShortcodeItem(
                        code = code,
                        value = value,
                        index = index,
                        animateIn = entranceTriggered,
                        onEdit = { onEdit(code, value) },
                        onDelete = { onDelete(code) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ShortcodesDialogs(
    showAddShortcodeDialog: Boolean,
    editingShortcode: Pair<String, String>?,
    showHelpDialog: Boolean,
    onDismissEdit: () -> Unit,
    onConfirmEdit: (String, String) -> Unit,
    onDismissHelp: () -> Unit
) {
    if (showAddShortcodeDialog || editingShortcode != null) {
        ShortcodeEditDialog(
            initialCode = editingShortcode?.first ?: "",
            initialValue = editingShortcode?.second ?: "",
            onDismiss = onDismissEdit,
            onConfirm = onConfirmEdit
        )
    }

    if (showHelpDialog) {
        ShortcodeHelpDialog(onDismiss = onDismissHelp)
    }
}
