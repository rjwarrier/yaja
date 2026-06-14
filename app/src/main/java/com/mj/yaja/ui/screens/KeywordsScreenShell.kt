package com.mj.yaja.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.ui.components.AnimatedMenuButton
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.expressiveFabMotion
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.ui.design.floatTween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KeywordsTopBar(
    onOpenDrawer: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                stringResource(R.string.nav_people_places),
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.primary,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
internal fun KeywordsFab(
    visible: Boolean,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motionPreference = LocalAnimationPreference.current
    var iconRotation by remember { mutableStateOf(0f) }
    val fabInteraction = remember { MutableInteractionSource() }
    val animatedIconRotation by animateFloatAsState(
        targetValue = if (motionPreference == AnimationPreference.OFF) 0f else iconRotation,
        animationSpec = motionPreference.floatTween(500),
        label = "fab_rotation"
    )
    LaunchedEffect(Unit) {
        iconRotation = 360f
    }
    AppStaggeredEntrance(
        visible = visible,
        index = 0,
        strength = AppEntranceStrength.SUBTLE,
        modifier = modifier.padding(bottom = bottomPadding)
    ) {
        FloatingActionButton(
            onClick = onClick,
            interactionSource = fabInteraction,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(64.dp)
                .expressiveFabMotion(fabInteraction),
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 10.dp,
                focusedElevation = 9.dp,
                hoveredElevation = 9.dp
            )
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = stringResource(R.string.keywords_cd_add),
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { rotationZ = animatedIconRotation }
            )
        }
    }
}

@Composable
internal fun KeywordScreenDialogs(
    showCreateDialog: Boolean,
    editingKeyword: KeywordDefinition?,
    pendingDeleteKeyword: KeywordDefinition?,
    onDismissCreate: () -> Unit,
    onCreateKeyword: (String, com.mj.yaja.data.KeywordType, String, List<String>, Boolean) -> Unit,
    onDismissEdit: () -> Unit,
    onSaveEditedKeyword: (KeywordDefinition, String, com.mj.yaja.data.KeywordType, String, List<String>, Boolean) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (KeywordDefinition) -> Unit
) {
    if (showCreateDialog) {
        KeywordEditorDialog(
            title = stringResource(R.string.keywords_dialog_add_title),
            initialKeyword = null,
            onDismiss = onDismissCreate,
            onSave = { name, type, relation, aliases, enabled ->
                onCreateKeyword(name, type, relation, aliases, enabled)
            }
        )
    }

    editingKeyword?.let { keyword ->
        KeywordEditorDialog(
            title = stringResource(R.string.keywords_dialog_edit_title),
            initialKeyword = keyword,
            onDismiss = onDismissEdit,
            onSave = { name, type, relation, aliases, enabled ->
                onSaveEditedKeyword(keyword, name, type, relation, aliases, enabled)
            }
        )
    }

    pendingDeleteKeyword?.let { keyword ->
        DeleteKeywordDialog(
            keyword = keyword,
            onDismiss = onDismissDelete,
            onConfirmDelete = { onConfirmDelete(keyword) }
        )
    }
}
