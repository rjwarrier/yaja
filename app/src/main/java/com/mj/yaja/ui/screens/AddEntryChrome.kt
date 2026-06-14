package com.mj.yaja.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.data.AnimationPreference
import com.mj.yaja.ui.design.AppEntranceStrength
import com.mj.yaja.ui.design.AppStaggeredEntrance
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.expressiveFabMotion
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.ui.design.floatSpring
import com.mj.yaja.ui.design.floatTween
import com.mj.yaja.ui.design.scaledDuration

@Composable
fun AddEntryTopBar(
        headerMode: String,
        isEditingMode: Boolean,
        isSaving: Boolean,
        canDelete: Boolean,
        showJumpToToday: Boolean,
        onBack: () -> Unit,
        onShowHelp: () -> Unit,
        onShowTemplates: () -> Unit,
        onDelete: () -> Unit,
        onJumpToToday: () -> Unit,
        shareText: String
) {
        val context = LocalContext.current

        Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
        ) {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(
                                                        start = 8.dp,
                                                        end = 8.dp,
                                                        top = 8.dp,
                                                        bottom = 6.dp
                                                )
                        ) {
                                IconButton(
                                        onClick = onBack,
                                        modifier = Modifier.align(Alignment.CenterStart)
                                ) {
                                        Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                contentDescription = stringResource(R.string.action_cancel)
                                        )
                                }

                                Text(
                                        text = headerMode,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .align(Alignment.Center)
                                                        .padding(horizontal = 96.dp)
                                )

                                Row(
                                        modifier = Modifier.align(Alignment.CenterEnd),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        if (isEditingMode) {
                                                IconButton(
                                                        onClick = onShowHelp,
                                                        enabled = !isSaving
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Outlined.Info,
                                                                contentDescription = stringResource(R.string.addentry_cd_shortcuts_help),
                                                                tint = MaterialTheme.colorScheme.primary
                                                        )
                                                }
                                                FilledTonalIconButton(
                                                        onClick = onShowTemplates,
                                                        enabled = !isSaving,
                                                        shape = RoundedCornerShape(18.dp)
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Rounded.Description,
                                                                contentDescription = stringResource(R.string.addentry_cd_insert_template),
                                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                }
                                                if (canDelete) {
                                                        IconButton(
                                                                onClick = onDelete,
                                                                enabled = !isSaving
                                                        ) {
                                                                Icon(
                                                                        imageVector = Icons.Rounded.Delete,
                                                                        contentDescription = stringResource(R.string.action_delete)
                                                                )
                                                        }
                                                }
                                        } else {
                                                if (showJumpToToday) {
                                                        IconButton(
                                                                onClick = onJumpToToday,
                                                                enabled = !isSaving
                                                        ) {
                                                                Icon(
                                                                        imageVector = Icons.Rounded.Today,
                                                                        contentDescription = stringResource(R.string.addentry_cd_jump_to_today)
                                                                )
                                                        }
                                                }
                                                IconButton(
                                                        onClick = {
                                                                val sendIntent: Intent = Intent().apply {
                                                                        action = Intent.ACTION_SEND
                                                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                                                        type = "text/plain"
                                                                }
                                                                val shareIntent = Intent.createChooser(sendIntent, null)
                                                                context.startActivity(shareIntent)
                                                        },
                                                        enabled = !isSaving
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Rounded.Share,
                                                                contentDescription = stringResource(R.string.addentry_cd_share)
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
fun AddEntryFloatingChrome(
        isFormattingBarVisible: Boolean,
        motionPreference: AnimationPreference,
        onBold: () -> Unit,
        onItalic: () -> Unit,
        isSaving: Boolean,
        mutationLabel: String?,
        isEditingMode: Boolean,
        hasContentToSave: Boolean,
        onPrimaryAction: () -> Unit
) {
        val primaryFabInteraction = remember { MutableInteractionSource() }
        Box(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                        visible = isFormattingBarVisible,
                        enter =
                                motionPreference.enterOrNone(
                                        slideInVertically(
                                                tween(
                                                        durationMillis =
                                                                motionPreference.scaledDuration(220)
                                                )
                                        ) { it } + fadeIn(
                                                motionPreference.floatTween(150)
                                        )
                                ),
                        exit =
                                motionPreference.exitOrNone(
                                        slideOutVertically(
                                                tween(
                                                        durationMillis =
                                                                motionPreference.scaledDuration(180)
                                                )
                                        ) { it } + fadeOut(
                                                motionPreference.floatTween(100)
                                        )
                                ),
                        modifier =
                                Modifier.align(Alignment.BottomCenter)
                                        .imePadding()
                                        .padding(bottom = 12.dp)
                ) {
                        Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.primary,
                                tonalElevation = 6.dp,
                                shadowElevation = 4.dp,
                                modifier =
                                        Modifier.border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                CircleShape
                                        )
                        ) {
                                Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        IconButton(onClick = onBold) {
                                                Icon(
                                                        Icons.Rounded.FormatBold,
                                                        contentDescription = stringResource(R.string.addentry_cd_bold)
                                                )
                                        }
                                        IconButton(onClick = onItalic) {
                                                Icon(
                                                        Icons.Rounded.FormatItalic,
                                                        contentDescription = stringResource(R.string.addentry_cd_italic)
                                                )
                                        }
                                }
                        }
                }

                AnimatedVisibility(
                        visible = isSaving && mutationLabel != null,
                        enter =
                                motionPreference.enterOrNone(
                                        slideInVertically(
                                                tween(
                                                        durationMillis =
                                                                motionPreference.scaledDuration(180)
                                                )
                                        ) { it / 3 } + fadeIn(
                                                motionPreference.floatTween(160)
                                        )
                                ),
                        exit =
                                motionPreference.exitOrNone(
                                        slideOutVertically(
                                                tween(
                                                        durationMillis =
                                                                motionPreference.scaledDuration(140)
                                                )
                                        ) { it / 3 } + fadeOut(
                                                motionPreference.floatTween(120)
                                        )
                                ),
                        modifier =
                                Modifier.align(Alignment.BottomEnd)
                                        .navigationBarsPadding()
                                        .padding(end = 88.dp, bottom = 20.dp)
                ) {
                        Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = 3.dp,
                                shadowElevation = 2.dp
                        ) {
                                Text(
                                        text = mutationLabel.orEmpty(),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier =
                                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                        }
                }

                AppStaggeredEntrance(
                        visible = true,
                        index = 0,
                        strength = AppEntranceStrength.SUBTLE,
                        modifier =
                                Modifier.align(Alignment.BottomEnd)
                                        .padding(end = 16.dp, bottom = 16.dp)
                ) {
                        FloatingActionButton(
                                onClick = onPrimaryAction,
                                interactionSource = primaryFabInteraction,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier =
                                        Modifier.size(64.dp)
                                                .expressiveFabMotion(primaryFabInteraction)
                        ) {
                                AnimatedContent(
                                        targetState =
                                                when {
                                                        isSaving -> "saving"
                                                        isEditingMode -> "save"
                                                        else -> "view"
                                                },
                                        transitionSpec = {
                                                if (motionPreference == AnimationPreference.OFF) {
                                                        EnterTransition.None togetherWith ExitTransition.None
                                                } else {
                                                        (scaleIn(
                                                                motionPreference.floatSpring(
                                                                        dampingRatio = 0.5f,
                                                                        stiffness = Spring.StiffnessMediumLow
                                                                )
                                                        ) + fadeIn(
                                                                motionPreference.floatTween(220)
                                                        )) togetherWith
                                                                (scaleOut(
                                                                        motionPreference.floatSpring(
                                                                                dampingRatio = 0.8f,
                                                                                stiffness = Spring.StiffnessMedium
                                                                        )
                                                                ) + fadeOut(
                                                                        motionPreference.floatTween(160)
                                                                ))
                                                }
                                        },
                                        contentAlignment = Alignment.Center,
                                        label = "SaveFabIconMorph"
                                ) { fabState ->
                                        when (fabState) {
                                                "saving" ->
                                                        CircularProgressIndicator(
                                                                modifier = Modifier.size(22.dp),
                                                                strokeWidth = 2.5.dp,
                                                                color = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                "save" ->
                                                        Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = stringResource(R.string.action_save)
                                                        )
                                                else ->
                                                        Icon(
                                                                imageVector = Icons.Rounded.Edit,
                                                                contentDescription = stringResource(R.string.action_edit)
                                                        )
                                        }
                                }
                        }
                }
        }
}
