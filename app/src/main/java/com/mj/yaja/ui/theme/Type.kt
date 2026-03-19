package com.mj.yaja.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mj.yaja.R
import com.mj.yaja.data.AppFontFamily

val JetBrainsMono =
        FontFamily(
                Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                Font(R.font.jetbrains_mono_bold, FontWeight.SemiBold),
                Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
        )

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

fun getTypography(appFontFamily: AppFontFamily): Typography {
    val fontFamily =
            when (appFontFamily) {
                AppFontFamily.SANS_SERIF -> GoogleSansFamily
                AppFontFamily.SERIF -> LibreBaskervilleFamily
                AppFontFamily.MONO -> JetBrainsMono
            }

    return Typography(
            displayLarge =
                    TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 57.sp,
                            lineHeight = 64.sp,
                            letterSpacing = (-0.25).sp
                    ),
            // ... (rest of the file remains same, just ensure fontFamily is used)
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
                            fontWeight = FontWeight.Normal,
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
