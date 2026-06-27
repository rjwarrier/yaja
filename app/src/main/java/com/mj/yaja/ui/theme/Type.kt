package com.mj.yaja.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mj.yaja.R
import com.mj.yaja.data.AppFontFamily
import java.io.File

// JetBrains Mono ships as a variable font with a single wght axis (100-800).
const val MONO_WEIGHT_MIN = 100
const val MONO_WEIGHT_MAX = 800
const val MONO_WEIGHT_DEFAULT = 400

private fun clampedVariableWeight(weight: Int): Int =
    weight.coerceIn(MONO_WEIGHT_MIN, MONO_WEIGHT_MAX)

/**
 * Builds the JetBrains Mono family at a user-chosen base weight. Heavier styles
 * are offset from the base so bold text stays visually bolder at any setting.
 */
@OptIn(ExperimentalTextApi::class)
fun jetBrainsMonoFamily(baseWeight: Int = MONO_WEIGHT_DEFAULT): FontFamily {
    val base = clampedVariableWeight(baseWeight)
    fun monoFont(weight: FontWeight, axisWeight: Int) =
            Font(
                    R.font.jetbrains_mono_variable,
                    weight = weight,
                    variationSettings =
                            FontVariation.Settings(
                                    FontVariation.weight(axisWeight.coerceAtMost(MONO_WEIGHT_MAX))
                            )
            )
    return FontFamily(
            monoFont(FontWeight.Normal, base),
            monoFont(FontWeight.Medium, base + 100),
            monoFont(FontWeight.SemiBold, base + 200),
            monoFont(FontWeight.Bold, base + 300)
    )
}

val JetBrainsMono = jetBrainsMonoFamily()

@RequiresApi(Build.VERSION_CODES.Q)
private fun customFontSupportsWeightApi29(file: File): Boolean =
    try {
        android.graphics.fonts.Font.Builder(file)
            .build()
            .axes
            ?.any { it.tag == "wght" } == true
    } catch (_: Exception) {
        false
    }

fun customFontSupportsWeight(path: String?): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val file = path?.let { File(it) }?.takeIf { it.exists() } ?: return false
    return customFontSupportsWeightApi29(file)
}

/**
 * Loads a user-supplied font file (e.g. a Malayalam unicode font) from app storage.
 * Returns null when the path is unset, missing, or unreadable so callers can fall
 * back to a bundled family.
 */
@OptIn(ExperimentalTextApi::class)
fun customFontFamily(
    path: String?,
    baseWeight: Int = MONO_WEIGHT_DEFAULT
): FontFamily? {
    val file = path?.let { File(it) }?.takeIf { it.exists() } ?: return null
    return try {
        val supportsWeight = customFontSupportsWeight(path)
        val base = clampedVariableWeight(baseWeight)
        fun customFont(weight: FontWeight, axisWeight: Int) =
            if (supportsWeight) {
                Font(
                    file = file,
                    weight = weight,
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(clampedVariableWeight(axisWeight))
                    )
                )
            } else {
                Font(file = file, weight = weight)
            }
        FontFamily(
            customFont(FontWeight.Normal, base),
            customFont(FontWeight.Medium, base + 100),
            customFont(FontWeight.SemiBold, base + 200),
            customFont(FontWeight.Bold, base + 300)
        )
    } catch (_: Exception) {
        null
    }
}

val GoogleSansFamily =
        FontFamily(
                Font(R.font.google_sans_regular, FontWeight.Normal),
                Font(R.font.google_sans_medium, FontWeight.Medium),
                Font(R.font.google_sans_bold, FontWeight.Bold)
        )

val LibreBaskervilleFamily =
        FontFamily(
                Font(R.font.libre_baskerville_regular, FontWeight.Normal),
                Font(R.font.libre_baskerville_bold, FontWeight.Bold)
        )

val BodoniModaFamily = FontFamily(Font(R.font.bodoni_moda_extrabold, FontWeight.ExtraBold))

/**
 * A Compose font entry backed by a typeface we already built. Used to feed
 * [android.graphics.Typeface.CustomFallbackBuilder] results (primary font with
 * Manjari chained for Malayalam) into a normal font list, so per-weight matching
 * and inline bold spans keep working.
 */
private class PrebuiltTypefaceFont(
        val typeface: android.graphics.Typeface,
        override val weight: FontWeight,
        override val style: FontStyle = FontStyle.Normal
) :
        AndroidFont(
                FontLoadingStrategy.Blocking,
                PrebuiltTypefaceLoader,
                FontVariation.Settings()
        )

private object PrebuiltTypefaceLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): android.graphics.Typeface? =
            (font as? PrebuiltTypefaceFont)?.typeface

    override suspend fun awaitLoad(context: Context, font: AndroidFont): android.graphics.Typeface? =
            (font as? PrebuiltTypefaceFont)?.typeface
}

/**
 * Resolves the family used for app typography. On API 29+ every option gets true
 * per-glyph fallback to Manjari for Malayalam via [android.graphics.Typeface.CustomFallbackBuilder]
 * (Compose font lists only fall back on load failure, not missing glyphs). Below
 * API 29 Malayalam falls back to the system Noto font instead.
 */
fun resolveAppFontFamily(
        context: Context,
        appFontFamily: AppFontFamily,
        monoFontWeight: Int = MONO_WEIGHT_DEFAULT,
        customFontPath: String? = null
): FontFamily {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        buildManjariChainedFamily(context, appFontFamily, monoFontWeight, customFontPath)
                ?.let { return it }
    }
    return when (appFontFamily) {
        AppFontFamily.SANS_SERIF -> GoogleSansFamily
        AppFontFamily.SERIF -> LibreBaskervilleFamily
        AppFontFamily.MONO -> jetBrainsMonoFamily(monoFontWeight)
        AppFontFamily.CUSTOM -> customFontFamily(customFontPath, monoFontWeight) ?: GoogleSansFamily
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun buildManjariChainedFamily(
        context: Context,
        appFontFamily: AppFontFamily,
        monoFontWeight: Int,
        customFontPath: String?
): FontFamily? {
    return try {
        val resources = context.resources
        fun platformFont(resId: Int, variationSettings: String? = null) =
                android.graphics.fonts.Font.Builder(resources, resId)
                        .apply { variationSettings?.let { setFontVariationSettings(it) } }
                        .build()

        val manjariRegular = platformFont(R.font.manjari_regular)
        val manjariBold = platformFont(R.font.manjari_bold)

        fun chained(
                primary: android.graphics.fonts.Font,
                manjari: android.graphics.fonts.Font,
                weight: FontWeight
        ): PrebuiltTypefaceFont {
            val typeface =
                    android.graphics.Typeface.CustomFallbackBuilder(
                                    android.graphics.fonts.FontFamily.Builder(primary).build()
                            )
                            .addCustomFallback(
                                    android.graphics.fonts.FontFamily.Builder(manjari).build()
                            )
                            // Keep system fonts (emoji, other scripts) after Manjari.
                            .setSystemFallback("sans-serif")
                            .build()
            return PrebuiltTypefaceFont(typeface, weight)
        }

        when (appFontFamily) {
            AppFontFamily.SANS_SERIF ->
                    FontFamily(
                            chained(platformFont(R.font.google_sans_regular), manjariRegular, FontWeight.Normal),
                            chained(platformFont(R.font.google_sans_medium), manjariRegular, FontWeight.Medium),
                            chained(platformFont(R.font.google_sans_bold), manjariBold, FontWeight.SemiBold),
                            chained(platformFont(R.font.google_sans_bold), manjariBold, FontWeight.Bold)
                    )
            AppFontFamily.SERIF ->
                    FontFamily(
                            chained(platformFont(R.font.libre_baskerville_regular), manjariRegular, FontWeight.Normal),
                            chained(platformFont(R.font.libre_baskerville_bold), manjariBold, FontWeight.SemiBold),
                            chained(platformFont(R.font.libre_baskerville_bold), manjariBold, FontWeight.Bold)
                    )
            AppFontFamily.MONO -> {
                val base = monoFontWeight.coerceIn(MONO_WEIGHT_MIN, MONO_WEIGHT_MAX)
                fun mono(axisWeight: Int) =
                        platformFont(
                                R.font.jetbrains_mono_variable,
                                "'wght' ${axisWeight.coerceAtMost(MONO_WEIGHT_MAX)}"
                        )
                FontFamily(
                        chained(mono(base), manjariRegular, FontWeight.Normal),
                        chained(mono(base + 100), manjariRegular, FontWeight.Medium),
                        chained(mono(base + 200), manjariBold, FontWeight.SemiBold),
                        chained(mono(base + 300), manjariBold, FontWeight.Bold)
                )
            }
            AppFontFamily.CUSTOM -> {
                val file = customFontPath?.let { File(it) }?.takeIf { it.exists() } ?: return null
                val supportsWeight = customFontSupportsWeightApi29(file)
                val base = clampedVariableWeight(monoFontWeight)
                fun customFont(axisWeight: Int) =
                    android.graphics.fonts.Font.Builder(file)
                        .apply {
                            if (supportsWeight) {
                                setFontVariationSettings(
                                    "'wght' ${clampedVariableWeight(axisWeight)}"
                                )
                            }
                        }
                        .build()
                FontFamily(
                        chained(customFont(base), manjariRegular, FontWeight.Normal),
                        chained(customFont(base + 100), manjariRegular, FontWeight.Medium),
                        chained(customFont(base + 200), manjariBold, FontWeight.SemiBold),
                        chained(customFont(base + 300), manjariBold, FontWeight.Bold)
                )
            }
        }
    } catch (_: Exception) {
        null
    }
}

fun Typography.contentTextStyle(): TextStyle =
        bodyLarge.copy(fontWeight = FontWeight.Normal)

fun Typography.metaTextStyle(): TextStyle =
        labelMedium.copy(
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.25.sp
        )

fun Typography.metaSmallTextStyle(): TextStyle =
        labelSmall.copy(
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.2.sp
        )

fun getTypography(fontFamily: FontFamily): Typography {
    return Typography(
            displayLarge =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 57.sp,
                            lineHeight = 64.sp,
                            letterSpacing = (-0.25).sp
                    ),
            displayMedium =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 45.sp,
                            lineHeight = 52.sp,
                            letterSpacing = 0.sp
                    ),
            displaySmall =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 36.sp,
                            lineHeight = 44.sp,
                            letterSpacing = 0.sp
                    ),
            headlineLarge =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 32.sp,
                            lineHeight = 40.sp,
                            letterSpacing = 0.sp
                    ),
            headlineMedium =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 28.sp,
                            lineHeight = 36.sp,
                            letterSpacing = 0.sp
                    ),
            headlineSmall =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            lineHeight = 32.sp,
                            letterSpacing = 0.sp
                    ),
            titleLarge =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                            letterSpacing = 0.sp
                    ),
            titleMedium =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            letterSpacing = 0.15.sp
                    ),
            titleSmall =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.1.sp
                    ),
            bodyLarge =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            letterSpacing = 0.5.sp
                    ),
            bodyMedium =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.25.sp
                    ),
            bodySmall =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            letterSpacing = 0.4.sp
                    ),
            labelLarge =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.1.sp
                    ),
            labelMedium =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            letterSpacing = 0.5.sp
                    ),
            labelSmall =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            letterSpacing = 0.5.sp
                    )
    )
}
