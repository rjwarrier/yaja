package com.mj.yaja.ui.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mj.yaja.data.AnimationPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class BottomNavIconTapMotion(
    val scale: Animatable<Float, AnimationVector1D>,
    val rotation: Animatable<Float, AnimationVector1D>,
    val liftDp: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope
) {
    fun play(motionPreference: AnimationPreference) {
        scope.launch {
            if (motionPreference == AnimationPreference.OFF) {
                scale.snapTo(1f)
                rotation.snapTo(0f)
                liftDp.snapTo(0f)
            } else {
                launch {
                    scale.snapTo(1f)
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = keyframes {
                            val reduced = motionPreference == AnimationPreference.REDUCED
                            durationMillis = if (reduced) 220 else 360
                            if (reduced) {
                                1.14f at 70
                                0.98f at 145
                                1f at 220
                            } else {
                                1.34f at 80
                                0.90f at 175
                                1.12f at 260
                                1f at 360
                            }
                        }
                    )
                }
                launch {
                    rotation.snapTo(0f)
                    rotation.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            val reduced = motionPreference == AnimationPreference.REDUCED
                            durationMillis = if (reduced) 220 else 360
                            if (reduced) {
                                5f at 70
                                -3f at 145
                                0f at 220
                            } else {
                                16f at 80
                                -12f at 175
                                5f at 260
                                0f at 360
                            }
                        }
                    )
                }
                launch {
                    liftDp.snapTo(0f)
                    liftDp.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            val reduced = motionPreference == AnimationPreference.REDUCED
                            durationMillis = if (reduced) 220 else 360
                            if (reduced) {
                                -3f at 70
                                1f at 145
                                0f at 220
                            } else {
                                -8f at 80
                                2f at 175
                                -2f at 260
                                0f at 360
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun rememberBottomNavIconTapMotion(routeKey: String): BottomNavIconTapMotion {
    val scope = rememberCoroutineScope()
    val scale = remember(routeKey) { Animatable(1f) }
    val rotation = remember(routeKey) { Animatable(0f) }
    val liftDp = remember(routeKey) { Animatable(0f) }
    return remember(routeKey, scope) { BottomNavIconTapMotion(scale, rotation, liftDp, scope) }
}