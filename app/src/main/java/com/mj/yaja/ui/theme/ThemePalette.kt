package com.mj.yaja.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.mj.yaja.data.BackgroundTintLevel
import com.mj.yaja.data.CustomPalette
import com.mj.yaja.data.PersonalAccentStyle
import com.mj.yaja.data.PersonalThemeSlot
import com.mj.yaja.data.ThemeColorIntensity

data class ThemePaletteSpec(
    val palette: CustomPalette,
    val label: String,
    val preview: List<Color>,
    val primarySeed: Color,
    val secondarySeed: Color,
    val tertiarySeed: Color
)

val YajaCustomPalettes: List<ThemePaletteSpec> =
    listOf(
        ThemePaletteSpec(
            palette = CustomPalette.YAJA,
            label = "Yaja",
            preview = listOf(Color(0xFF5EC7FF), Color(0xFFFF9A73), Color(0xFF68E3D4)),
            primarySeed = Color(0xFF5EC7FF),
            secondarySeed = Color(0xFF8B5A45),
            tertiarySeed = Color(0xFF68E3D4)
        ),
        ThemePaletteSpec(
            palette = CustomPalette.OCEAN,
            label = "Ocean",
            preview = listOf(Color(0xFF6EA0FF), Color(0xFF68E3D4), Color(0xFFFFD48D)),
            primarySeed = Color(0xFF6EA0FF),
            secondarySeed = Color(0xFF68E3D4),
            tertiarySeed = Color(0xFFFFD48D)
        ),
        ThemePaletteSpec(
            palette = CustomPalette.FOREST,
            label = "Forest",
            preview = listOf(Color(0xFF8EF08B), Color(0xFFD3DE63), Color(0xFFE2A96C)),
            primarySeed = Color(0xFF8EF08B),
            secondarySeed = Color(0xFFD3DE63),
            tertiarySeed = Color(0xFFE2A96C)
        ),
        ThemePaletteSpec(
            palette = CustomPalette.ROSE,
            label = "Rose",
            preview = listOf(Color(0xFFFF8BB7), Color(0xFFB49BFF), Color(0xFFFFC5A4)),
            primarySeed = Color(0xFFFF8BB7),
            secondarySeed = Color(0xFFB49BFF),
            tertiarySeed = Color(0xFFFFC5A4)
        ),
        ThemePaletteSpec(
            palette = CustomPalette.AMBER,
            label = "Amber",
            preview = listOf(Color(0xFFFFC14D), Color(0xFF8B74F8), Color(0xFF58E0B8)),
            primarySeed = Color(0xFFFFC14D),
            secondarySeed = Color(0xFF8B74F8),
            tertiarySeed = Color(0xFF58E0B8)
        ),
        ThemePaletteSpec(
            palette = CustomPalette.MONO,
            label = "Mono",
            preview = listOf(Color(0xFFE6E8EF), Color(0xFFB8C1D1), Color(0xFFFF9DA4)),
            primarySeed = Color(0xFFE6E8EF),
            secondarySeed = Color(0xFFB8C1D1),
            tertiarySeed = Color(0xFFFF9DA4)
        ),
        ThemePaletteSpec(
            palette = CustomPalette.SUNSET,
            label = "Sunset",
            preview = listOf(Color(0xFFFF9B84), Color(0xFFFFBE4D), Color(0xFFF45EB0)),
            primarySeed = Color(0xFFFF9B84),
            secondarySeed = Color(0xFFFFBE4D),
            tertiarySeed = Color(0xFFF45EB0)
        ),
        ThemePaletteSpec(
            palette = CustomPalette.LAVENDER,
            label = "Lavender",
            preview = listOf(Color(0xFFBA9CFF), Color(0xFFF27ACC), Color(0xFFA7C7FF)),
            primarySeed = Color(0xFFBA9CFF),
            secondarySeed = Color(0xFFF27ACC),
            tertiarySeed = Color(0xFFA7C7FF)
        ),
        ThemePaletteSpec(
            palette = CustomPalette.EARTH,
            label = "Earth",
            preview = listOf(Color(0xFFD1A06B), Color(0xFFAFBF72), Color(0xFFE2A78C)),
            primarySeed = Color(0xFFD1A06B),
            secondarySeed = Color(0xFFAFBF72),
            tertiarySeed = Color(0xFFE2A78C)
        ),
        ThemePaletteSpec(
            palette = CustomPalette.CYBER,
            label = "Cyber",
            preview = listOf(Color(0xFF46E5F2), Color(0xFFF04AD9), Color(0xFFCFFF3E)),
            primarySeed = Color(0xFF46E5F2),
            secondarySeed = Color(0xFFF04AD9),
            tertiarySeed = Color(0xFFCFFF3E)
        ),
        ThemePaletteSpec(
            palette = CustomPalette.PERSONAL,
            label = "Personal",
            preview = listOf(Color(0xFF4DB7F2), Color(0xFFFFBB57), Color(0xFF7AC77D)),
            primarySeed = Color(0xFF4DB7F2),
            secondarySeed = Color(0xFFFFBB57),
            tertiarySeed = Color(0xFF7AC77D)
        )
    )

fun customPaletteSpec(palette: CustomPalette): ThemePaletteSpec =
    YajaCustomPalettes.firstOrNull { it.palette == palette } ?: YajaCustomPalettes.first()

fun buildCustomPaletteScheme(
    palette: CustomPalette,
    darkTheme: Boolean,
    amoledTheme: Boolean,
    intensity: ThemeColorIntensity,
    backgroundTintLevel: BackgroundTintLevel,
    personalThemeSlot: PersonalThemeSlot? = null
): ColorScheme {
    val spec =
        if (palette == CustomPalette.PERSONAL && personalThemeSlot != null) {
            personalThemeSpec(personalThemeSlot)
        } else {
            customPaletteSpec(palette)
        }
    val primary = spec.primarySeed.scaleColorIntensity(intensity)
    val secondary = spec.secondarySeed.scaleColorIntensity(intensity)
    val tertiary = spec.tertiarySeed.scaleColorIntensity(intensity)
    val neutralBg = if (darkTheme || amoledTheme) Color(0xFF0D0D0F) else Color(0xFFF6F5F2)
    val surfaceBase = if (darkTheme || amoledTheme) Color(0xFF17171A) else Color(0xFFFFFFFF)
    val tintSource = primary.copy(alpha = backgroundTintLevel.amount)
    val background = if (amoledTheme) Color.Black else tintSource.compositeOver(neutralBg)
    val surface = if (amoledTheme) Color.Black else tintSource.copy(alpha = backgroundTintLevel.amount * 0.78f).compositeOver(surfaceBase)
    val surfaceLow = tintSource.copy(alpha = backgroundTintLevel.amount * 1.15f).compositeOver(
        if (amoledTheme) Color(0xFF101012) else lerp(surfaceBase, neutralBg, 0.18f)
    )
    val surfaceHigh = tintSource.copy(alpha = backgroundTintLevel.amount * 1.4f).compositeOver(
        if (amoledTheme) Color(0xFF1B1B1E) else lerp(surfaceBase, neutralBg, 0.28f)
    )
    val outline = if (darkTheme || amoledTheme) lerp(primary, Color.White, 0.58f) else lerp(primary, Color.Black, 0.45f)

    return if (darkTheme || amoledTheme) {
        darkColorScheme(
            primary = lerp(primary, Color.White, 0.14f),
            onPrimary = Color(0xFF081018),
            primaryContainer = tint(primary, Color.Black, 0.42f),
            onPrimaryContainer = lerp(primary, Color.White, 0.35f),
            secondary = lerp(secondary, Color.White, 0.1f),
            onSecondary = Color(0xFF101012),
            secondaryContainer = tint(secondary, Color.Black, 0.5f),
            onSecondaryContainer = lerp(secondary, Color.White, 0.28f),
            tertiary = lerp(tertiary, Color.White, 0.08f),
            onTertiary = Color(0xFF101012),
            tertiaryContainer = tint(tertiary, Color.Black, 0.48f),
            onTertiaryContainer = lerp(tertiary, Color.White, 0.24f),
            background = background,
            onBackground = Color(0xFFF4F5F7),
            surface = surface,
            onSurface = Color(0xFFF4F5F7),
            surfaceVariant = surfaceLow,
            onSurfaceVariant = Color(0xFFD6D8DE),
            surfaceContainerLowest = if (amoledTheme) Color.Black else lerp(background, Color.Black, 0.08f),
            surfaceContainerLow = surfaceLow,
            surfaceContainer = lerp(surfaceLow, surfaceHigh, 0.45f),
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = lerp(surfaceHigh, Color.White, 0.06f),
            outline = outline,
            inverseSurface = Color(0xFFECEEF3),
            inverseOnSurface = Color(0xFF191B1F),
            inversePrimary = primary
        )
    } else {
        lightColorScheme(
            primary = tint(primary, Color.Black, 0.08f),
            onPrimary = Color.White,
            primaryContainer = tint(primary, Color.White, 0.72f),
            onPrimaryContainer = tint(primary, Color.Black, 0.68f),
            secondary = tint(secondary, Color.Black, 0.1f),
            onSecondary = Color.White,
            secondaryContainer = tint(secondary, Color.White, 0.76f),
            onSecondaryContainer = tint(secondary, Color.Black, 0.66f),
            tertiary = tint(tertiary, Color.Black, 0.12f),
            onTertiary = Color.White,
            tertiaryContainer = tint(tertiary, Color.White, 0.74f),
            onTertiaryContainer = tint(tertiary, Color.Black, 0.66f),
            background = background,
            onBackground = Color(0xFF17181B),
            surface = surface,
            onSurface = Color(0xFF17181B),
            surfaceVariant = surfaceLow,
            onSurfaceVariant = Color(0xFF3D4149),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = surfaceLow,
            surfaceContainer = lerp(surfaceLow, surfaceHigh, 0.4f),
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = lerp(surfaceHigh, Color.Black, 0.03f),
            outline = outline,
            inverseSurface = Color(0xFF2C2F35),
            inverseOnSurface = Color(0xFFF4F5F7),
            inversePrimary = lerp(primary, Color.White, 0.2f)
        )
    }
}

private fun tint(color: Color, target: Color, amount: Float): Color =
    lerp(color, target, amount.coerceIn(0f, 1f))

fun personalThemeSpec(slot: PersonalThemeSlot): ThemePaletteSpec {
    val main = hsvColor(slot.hue, slot.saturation, slot.brightness)
    val accents = generatePersonalAccents(main, slot.accentStyle)
    return ThemePaletteSpec(
        palette = CustomPalette.PERSONAL,
        label = slot.name,
        preview = listOf(main, accents.first, accents.second),
        primarySeed = main,
        secondarySeed = accents.first,
        tertiarySeed = accents.second
    )
}

private fun generatePersonalAccents(
    main: Color,
    style: PersonalAccentStyle
): Pair<Color, Color> {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(main.toArgb(), hsv)
    val hue = hsv[0]
    val sat = hsv[1]
    val bright = hsv[2]
    return when (style) {
        PersonalAccentStyle.COMPLEMENTARY -> {
            hsvColor((hue + 180f) % 360f, (sat * 0.68f).coerceIn(0f, 1f), bright) to
                hsvColor((hue + 28f) % 360f, (sat * 0.45f).coerceIn(0f, 1f), (bright * 0.92f).coerceIn(0f, 1f))
        }
        PersonalAccentStyle.ANALOGOUS -> {
            hsvColor((hue + 24f) % 360f, (sat * 0.72f).coerceIn(0f, 1f), bright) to
                hsvColor((hue + 336f) % 360f, (sat * 0.56f).coerceIn(0f, 1f), (bright * 0.96f).coerceIn(0f, 1f))
        }
        PersonalAccentStyle.TRIADIC -> {
            hsvColor((hue + 120f) % 360f, (sat * 0.78f).coerceIn(0f, 1f), bright) to
                hsvColor((hue + 240f) % 360f, (sat * 0.68f).coerceIn(0f, 1f), bright)
        }
        PersonalAccentStyle.SOFT -> {
            hsvColor((hue + 14f) % 360f, (sat * 0.34f).coerceIn(0f, 1f), (bright * 0.88f).coerceIn(0f, 1f)) to
                hsvColor((hue + 42f) % 360f, (sat * 0.24f).coerceIn(0f, 1f), (bright * 0.84f).coerceIn(0f, 1f))
        }
    }
}

fun hsvColor(hue: Float, saturation: Float, brightness: Float): Color =
    Color(
        AndroidColor.HSVToColor(
            floatArrayOf(
                hue.coerceIn(0f, 360f),
                saturation.coerceIn(0f, 1f),
                brightness.coerceIn(0f, 1f)
            )
        )
    )

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
