package com.freelauncher.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class LauncherFont(val id: String, val displayName: String, val fontFamily: FontFamily, val defaultWeight: FontWeight) {
    MINIMAL_SANS("minimal_sans", "Minimal Sans", FontFamily.SansSerif, FontWeight.Normal),
    MODERN_SANS("modern_sans", "Modern Sans", FontFamily.Default, FontWeight.Medium),
    MONOSPACE("monospace", "Monospace", FontFamily.Monospace, FontWeight.Normal),
    CONDENSED("condensed", "Condensed", FontFamily.SansSerif, FontWeight.Normal),
    SERIF("serif", "Serif", FontFamily.Serif, FontWeight.Normal),
    THIN_ELEGANT("thin", "Thin / Elegant", FontFamily.SansSerif, FontWeight.Light)
}

val LocalLauncherFont = compositionLocalOf { LauncherFont.MINIMAL_SANS }

fun getTypographyForFont(launcherFont: LauncherFont): Typography {
    val family = launcherFont.fontFamily
    val baseWeight = launcherFont.defaultWeight

    return Typography(
        displayLarge = TextStyle(
            fontFamily = family,
            fontWeight = if (launcherFont == LauncherFont.THIN_ELEGANT) FontWeight.ExtraLight else FontWeight.Light,
            fontSize = 58.sp,
            lineHeight = 66.sp,
            letterSpacing = if (launcherFont == LauncherFont.CONDENSED) (-1.5).sp else (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = family,
            fontWeight = if (launcherFont == LauncherFont.THIN_ELEGANT) FontWeight.Light else FontWeight.Normal,
            fontSize = 44.sp,
            lineHeight = 52.sp,
            letterSpacing = if (launcherFont == LauncherFont.CONDENSED) (-1).sp else 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = family,
            fontWeight = baseWeight,
            fontSize = 34.sp,
            lineHeight = 42.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = baseWeight,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.2.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = baseWeight,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.4.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = baseWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.8.sp
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.2.sp
        )
    )
}

val Typography = getTypographyForFont(LauncherFont.MINIMAL_SANS)
