package com.mj.yaja.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.floatSpring

@Composable
fun AnimatedMenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
        val motionPreference = LocalAnimationPreference.current
        val menuInteractionSource = remember { MutableInteractionSource() }
        val isMenuPressed by menuInteractionSource.collectIsPressedAsState()
        val menuScale by
                animateFloatAsState(
                        targetValue = if (isMenuPressed) 0.85f else 1f,
                        animationSpec =
                                motionPreference.floatSpring(
                                        dampingRatio = 0.5f,
                                        stiffness = Spring.StiffnessMedium
                                ),
                        label = "MenuScale"
                )

        Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(18.dp),
                modifier =
                        modifier.size(46.dp).graphicsLayer {
                                scaleX = menuScale
                                scaleY = menuScale
                        },
                interactionSource = menuInteractionSource,
                onClick = onClick
        ) {
                Box(contentAlignment = Alignment.Center) {
                        Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = stringResource(R.string.nav_cd_menu),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
        }
}
