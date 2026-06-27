package com.mj.yaja.ui.design

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import com.mj.yaja.data.AnimationPreference

enum class AppEntranceStrength {
    HERO,
    SECTION,
    SUBTLE
}

private val MaterialStandardEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val MaterialEmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

val LocalAnimationPreference = staticCompositionLocalOf { AnimationPreference.FULL }

fun AnimationPreference.scaledDuration(baseMillis: Int): Int =
    when (this) {
        AnimationPreference.FULL -> baseMillis
        AnimationPreference.REDUCED -> (baseMillis * 0.6f).roundToInt().coerceAtLeast(80)
        AnimationPreference.OFF -> 0
    }

fun AnimationPreference.scaledDelay(baseMillis: Int): Int =
    when (this) {
        AnimationPreference.FULL -> baseMillis
        AnimationPreference.REDUCED -> (baseMillis * 0.45f).roundToInt()
        AnimationPreference.OFF -> 0
    }

fun AnimationPreference.scaledDistance(base: Float): Float =
    when (this) {
        AnimationPreference.FULL -> base
        AnimationPreference.REDUCED -> base * 0.55f
        AnimationPreference.OFF -> 0f
    }

fun AnimationPreference.scaledDp(base: Dp): Dp =
    when (this) {
        AnimationPreference.FULL -> base
        AnimationPreference.REDUCED -> base * 0.65f
        AnimationPreference.OFF -> 0.dp
    }

fun AnimationPreference.floatSpring(
    dampingRatio: Float,
    stiffness: Float,
    reducedDampingRatio: Float = (dampingRatio + 0.1f).coerceAtMost(1f),
    reducedStiffness: Float = stiffness * 1.15f
): FiniteAnimationSpec<Float> =
    when (this) {
        AnimationPreference.OFF -> snap()
        AnimationPreference.REDUCED ->
            spring(
                dampingRatio = reducedDampingRatio,
                stiffness = reducedStiffness
            )
        AnimationPreference.FULL ->
            spring(
                dampingRatio = dampingRatio,
                stiffness = stiffness
            )
    }

fun AnimationPreference.dpSpring(
    dampingRatio: Float,
    stiffness: Float,
    reducedDampingRatio: Float = (dampingRatio + 0.1f).coerceAtMost(1f),
    reducedStiffness: Float = stiffness * 1.15f
): FiniteAnimationSpec<Dp> =
    when (this) {
        AnimationPreference.OFF -> snap()
        AnimationPreference.REDUCED ->
            spring(
                dampingRatio = reducedDampingRatio,
                stiffness = reducedStiffness
            )
        AnimationPreference.FULL ->
            spring(
                dampingRatio = dampingRatio,
                stiffness = stiffness
            )
    }

fun <T> AnimationPreference.itemSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    reducedDampingRatio: Float = (dampingRatio + 0.1f).coerceAtMost(1f),
    reducedStiffness: Float = stiffness * 1.15f
): FiniteAnimationSpec<T> =
    when (this) {
        AnimationPreference.OFF -> snap()
        AnimationPreference.REDUCED ->
            spring(
                dampingRatio = reducedDampingRatio,
                stiffness = reducedStiffness
            )
        AnimationPreference.FULL ->
            spring(
                dampingRatio = dampingRatio,
                stiffness = stiffness
            )
    }

fun AnimationPreference.floatTween(
    durationMillis: Int,
    delayMillis: Int = 0
): FiniteAnimationSpec<Float> =
    when (this) {
        AnimationPreference.OFF -> snap()
        else -> tween(
            durationMillis = scaledDuration(durationMillis),
            delayMillis = scaledDelay(delayMillis),
            easing = MaterialStandardEasing
        )
    }

fun <T> AnimationPreference.tweenSpec(
    durationMillis: Int,
    delayMillis: Int = 0
): FiniteAnimationSpec<T> =
    if (this == AnimationPreference.OFF) snap()
    else tween(
        durationMillis = scaledDuration(durationMillis),
        delayMillis = scaledDelay(delayMillis),
        easing = MaterialStandardEasing
    )

fun <T> AnimationPreference.emphasizedTweenSpec(
    durationMillis: Int,
    delayMillis: Int = 0
): FiniteAnimationSpec<T> =
    if (this == AnimationPreference.OFF) snap()
    else tween(
        durationMillis = scaledDuration(durationMillis),
        delayMillis = scaledDelay(delayMillis),
        easing = MaterialEmphasizedEasing
    )

fun <T> AnimationPreference.navSlideTween(): FiniteAnimationSpec<T> =
    when (this) {
        AnimationPreference.OFF -> snap()
        AnimationPreference.REDUCED ->
            spring(
                dampingRatio = 0.85f,
                stiffness = 650f
            )
        AnimationPreference.FULL ->
            spring(
                dampingRatio = 0.78f,
                stiffness = 500f
            )
    }

fun AnimationPreference.navFadeTween(entering: Boolean): FiniteAnimationSpec<Float> =
    tween(
        durationMillis = when (this) {
            AnimationPreference.FULL -> if (entering) 200 else 160
            AnimationPreference.REDUCED -> if (entering) 140 else 110
            AnimationPreference.OFF -> 0
        },
        easing = MaterialStandardEasing
    )

fun AnimationPreference.navScaleTween(): FiniteAnimationSpec<Float> =
    when (this) {
        AnimationPreference.OFF -> snap()
        AnimationPreference.REDUCED ->
            spring(
                dampingRatio = 0.85f,
                stiffness = 650f
            )
        AnimationPreference.FULL ->
            spring(
                dampingRatio = 0.72f,
                stiffness = 450f
            )
    }

fun AnimationPreference.enterOrNone(transition: EnterTransition): EnterTransition =
    if (this == AnimationPreference.OFF) EnterTransition.None else transition

fun AnimationPreference.exitOrNone(transition: ExitTransition): ExitTransition =
    if (this == AnimationPreference.OFF) ExitTransition.None else transition

@Composable
fun ProvideAnimationPreference(
    preference: AnimationPreference,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAnimationPreference provides preference) {
        content()
    }
}

@Composable
fun rememberAppEntrance(delayMillis: Long = 90L): Boolean {
    val animationPreference = LocalAnimationPreference.current
    if (animationPreference == AnimationPreference.OFF) return true
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(animationPreference, delayMillis) {
        entered = false
        val scaledDelayMillis = animationPreference.scaledDelay(delayMillis.toInt()).toLong()
        if (scaledDelayMillis > 0L) delay(scaledDelayMillis)
        entered = true
    }
    return entered
}

@Composable
fun AppStaggeredEntrance(
    visible: Boolean,
    index: Int,
    strength: AppEntranceStrength = AppEntranceStrength.SECTION,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animationPreference = LocalAnimationPreference.current
    if (animationPreference == AnimationPreference.OFF) {
        Box(modifier = modifier) { content() }
        return
    }
    var animatedVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible, animationPreference) {
        if (visible) {
            animatedVisible = false
            delay(animationPreference.scaledDelay(20).toLong())
            animatedVisible = true
        } else {
            animatedVisible = false
        }
    }
    val delayMs = when (strength) {
        AppEntranceStrength.HERO -> (index * 45).coerceAtMost(240)
        AppEntranceStrength.SECTION -> (index * 35).coerceAtMost(200)
        AppEntranceStrength.SUBTLE -> (index * 25).coerceAtMost(140)
    }
    val hiddenOffset = when (strength) {
        AppEntranceStrength.HERO -> 68f
        AppEntranceStrength.SECTION -> 50f
        AppEntranceStrength.SUBTLE -> 34f
    }
    val hiddenScale = when (strength) {
        AppEntranceStrength.HERO -> 0.92f
        AppEntranceStrength.SECTION -> 0.948f
        AppEntranceStrength.SUBTLE -> 0.972f
    }
    val alpha by animateFloatAsState(
        targetValue = if (animatedVisible) 1f else 0f,
        animationSpec = animationPreference.floatTween(
            durationMillis = when (strength) {
                AppEntranceStrength.HERO -> 380
                AppEntranceStrength.SECTION -> 320
                AppEntranceStrength.SUBTLE -> 260
            },
            delayMillis = delayMs
        ),
        label = "app_entrance_alpha_$index"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (animatedVisible) 0f else animationPreference.scaledDistance(hiddenOffset),
        animationSpec = animationPreference.floatSpring(
            dampingRatio = when (strength) {
                AppEntranceStrength.HERO -> 0.64f
                AppEntranceStrength.SECTION -> 0.70f
                AppEntranceStrength.SUBTLE -> 0.76f
            },
            stiffness = when (strength) {
                AppEntranceStrength.HERO -> 450f
                AppEntranceStrength.SECTION -> 500f
                AppEntranceStrength.SUBTLE -> 580f
            }
        ),
        label = "app_entrance_offset_$index"
    )
    val scale by animateFloatAsState(
        targetValue = if (animatedVisible) 1f else hiddenScale,
        animationSpec = animationPreference.floatSpring(
            dampingRatio = when (strength) {
                AppEntranceStrength.HERO -> 0.60f
                AppEntranceStrength.SECTION -> 0.68f
                AppEntranceStrength.SUBTLE -> 0.76f
            },
            stiffness = when (strength) {
                AppEntranceStrength.HERO -> 380f
                AppEntranceStrength.SECTION -> 440f
                AppEntranceStrength.SUBTLE -> 520f
            }
        ),
        label = "app_entrance_scale_$index"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY * density
            scaleX = scale
            scaleY = scale
        }
    ) {
        content()
    }
}

@Composable
fun AppScreenReveal(
    visible: Boolean,
    key: Any? = Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animationPreference = LocalAnimationPreference.current
    if (animationPreference == AnimationPreference.OFF) {
        Box(modifier = modifier) { content() }
        return
    }

    var revealed by remember(key) { mutableStateOf(false) }
    LaunchedEffect(visible, key, animationPreference) {
        if (visible) {
            revealed = false
            delay(animationPreference.scaledDelay(36).toLong())
            revealed = true
        } else {
            revealed = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = animationPreference.floatTween(
            durationMillis = 280,
            delayMillis = 15
        ),
        label = "screen_reveal_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (revealed) 0f else animationPreference.scaledDistance(34f),
        animationSpec = animationPreference.floatSpring(
            dampingRatio = 0.68f,
            stiffness = 480f
        ),
        label = "screen_reveal_offset"
    )
    val scale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.975f,
        animationSpec = animationPreference.floatSpring(
            dampingRatio = 0.64f,
            stiffness = 420f
        ),
        label = "screen_reveal_scale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY * density
            scaleX = scale
            scaleY = scale
        }
    ) {
        content()
    }
}

fun Modifier.expressiveFabMotion(
    interactionSource: InteractionSource,
    enabled: Boolean = true
): Modifier = composed {
    val animationPreference = LocalAnimationPreference.current
    if (!enabled || animationPreference == AnimationPreference.OFF) {
        this
    } else {
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.88f else 1f,
            animationSpec = animationPreference.floatSpring(
                dampingRatio = 0.45f,
                stiffness = 650f
            ),
            label = "fab_press_scale"
        )
        val translationY by animateFloatAsState(
            targetValue = if (isPressed) 6f else 0f,
            animationSpec = animationPreference.floatSpring(
                dampingRatio = 0.50f,
                stiffness = 550f
            ),
            label = "fab_press_lift"
        )

        this.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.translationY = translationY
        }
    }
}

fun Modifier.expressivePressMotion(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.95f
): Modifier = composed {
    val animationPreference = LocalAnimationPreference.current
    if (!enabled || animationPreference == AnimationPreference.OFF) {
        this
    } else {
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) pressedScale else 1f,
            animationSpec = animationPreference.floatSpring(
                dampingRatio = 0.45f,
                stiffness = 650f
            ),
            label = "press_scale"
        )
        this.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }
}
