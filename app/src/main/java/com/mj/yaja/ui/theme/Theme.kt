package com.mj.yaja.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider
import android.content.ContextWrapper
import androidx.core.view.WindowCompat
import com.mj.yaja.data.BackgroundTintLevel
import com.mj.yaja.data.ColorSource
import com.mj.yaja.data.CustomPalette
import com.mj.yaja.data.PersonalThemeSlot
import com.mj.yaja.data.ThemeColorIntensity
import android.graphics.Color as AndroidColor

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
)

private val AmoledColorScheme = DarkColorScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF1E1E1E),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF181818),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF242424)
)

@Composable
fun JournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledTheme: Boolean = false,
    fontScale: Float = 1.0f,
    appFontFamily: com.mj.yaja.data.AppFontFamily = com.mj.yaja.data.AppFontFamily.MONO,
    colorSource: ColorSource = ColorSource.MATERIAL_YOU,
    customPalette: CustomPalette = CustomPalette.YAJA,
    colorIntensity: ThemeColorIntensity = ThemeColorIntensity.NORMAL,
    backgroundTintLevel: BackgroundTintLevel = BackgroundTintLevel.SOFT,
    personalThemeSlot: PersonalThemeSlot? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        colorSource == ColorSource.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicScheme = if (darkTheme || amoledTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            val materialScheme = if (amoledTheme) {
                dynamicScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF1E1E1E),
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = Color(0xFF121212),
                    surfaceContainer = Color(0xFF181818),
                    surfaceContainerHigh = Color(0xFF1E1E1E),
                    surfaceContainerHighest = Color(0xFF242424)
                )
            } else dynamicScheme
            materialScheme.applyMaterialThemeTuning(
                darkTheme = darkTheme,
                amoledTheme = amoledTheme,
                colorIntensity = colorIntensity,
                backgroundTintLevel = backgroundTintLevel
            )
        }
        colorSource == ColorSource.CUSTOM ->
            buildCustomPaletteScheme(
                palette = customPalette,
                darkTheme = darkTheme,
                amoledTheme = amoledTheme,
                intensity = colorIntensity,
                backgroundTintLevel = backgroundTintLevel,
                personalThemeSlot = personalThemeSlot
            )
        amoledTheme -> AmoledColorScheme.applyMaterialThemeTuning(
            darkTheme = true,
            amoledTheme = true,
            colorIntensity = colorIntensity,
            backgroundTintLevel = backgroundTintLevel
        )
        darkTheme -> DarkColorScheme.applyMaterialThemeTuning(
            darkTheme = true,
            amoledTheme = false,
            colorIntensity = colorIntensity,
            backgroundTintLevel = backgroundTintLevel
        )
        else -> LightColorScheme.applyMaterialThemeTuning(
            darkTheme = false,
            amoledTheme = false,
            colorIntensity = colorIntensity,
            backgroundTintLevel = backgroundTintLevel
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            var currentContext = context
            while (currentContext is ContextWrapper) {
                if (currentContext is Activity) break
                currentContext = currentContext.baseContext
            }
            val activity = currentContext as? Activity
            
            @Suppress("DEPRECATION")
            activity?.window?.let { window ->
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    val currentDensity = LocalDensity.current
    val customDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * fontScale
    )

    CompositionLocalProvider(LocalDensity provides customDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = Shapes,
            typography = getTypography(appFontFamily),
            content = content
        )
    }
}

private fun androidx.compose.material3.ColorScheme.applyMaterialThemeTuning(
    darkTheme: Boolean,
    amoledTheme: Boolean,
    colorIntensity: ThemeColorIntensity,
    backgroundTintLevel: BackgroundTintLevel
) = copy(
    primary = primary.scaleColorIntensity(colorIntensity),
    primaryContainer = primaryContainer.scaleColorIntensity(colorIntensity),
    secondary = secondary.scaleColorIntensity(colorIntensity),
    secondaryContainer = secondaryContainer.scaleColorIntensity(colorIntensity),
    tertiary = tertiary.scaleColorIntensity(colorIntensity),
    tertiaryContainer = tertiaryContainer.scaleColorIntensity(colorIntensity),
    inversePrimary = inversePrimary.scaleColorIntensity(colorIntensity),
    background = tintSurface(background, primary, backgroundTintLevel, darkTheme, amoledTheme, 0.72f),
    surface = tintSurface(surface, primary, backgroundTintLevel, darkTheme, amoledTheme, 0.84f),
    surfaceVariant = tintSurface(surfaceVariant, primary, backgroundTintLevel, darkTheme, amoledTheme, 1.05f),
    surfaceContainerLowest = tintSurface(surfaceContainerLowest, primary, backgroundTintLevel, darkTheme, amoledTheme, 0.66f),
    surfaceContainerLow = tintSurface(surfaceContainerLow, primary, backgroundTintLevel, darkTheme, amoledTheme, 0.86f),
    surfaceContainer = tintSurface(surfaceContainer, primary, backgroundTintLevel, darkTheme, amoledTheme, 0.96f),
    surfaceContainerHigh = tintSurface(surfaceContainerHigh, primary, backgroundTintLevel, darkTheme, amoledTheme, 1.08f),
    surfaceContainerHighest = tintSurface(surfaceContainerHighest, primary, backgroundTintLevel, darkTheme, amoledTheme, 1.18f),
    outline = lerp(outline, primary.scaleColorIntensity(colorIntensity), if (darkTheme || amoledTheme) 0.18f else 0.12f)
)

private fun tintSurface(
    base: Color,
    primary: Color,
    backgroundTintLevel: BackgroundTintLevel,
    darkTheme: Boolean,
    amoledTheme: Boolean,
    strength: Float
): Color {
    if (amoledTheme && base == Color.Black) return Color.Black
    val alpha = (backgroundTintLevel.amount * strength).coerceIn(0f, if (darkTheme || amoledTheme) 0.22f else 0.14f)
    return primary.copy(alpha = alpha).compositeOver(base)
}

private fun Color.scaleColorIntensity(intensity: ThemeColorIntensity): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(this.toArgb(), hsv)
    hsv[1] = (hsv[1] * intensity.level).coerceIn(0f, 1f)
    hsv[2] = when (intensity) {
        ThemeColorIntensity.MUTED -> (hsv[2] * 0.94f).coerceIn(0f, 1f)
        ThemeColorIntensity.NORMAL -> hsv[2]
        ThemeColorIntensity.VIVID -> (hsv[2] * 1.03f).coerceIn(0f, 1f)
        ThemeColorIntensity.POP -> (hsv[2] * 1.07f).coerceIn(0f, 1f)
    }
    return Color(AndroidColor.HSVToColor(hsv))
}
