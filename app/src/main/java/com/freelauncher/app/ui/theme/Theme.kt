package com.freelauncher.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

enum class LauncherThemeMode(val id: String, val displayName: String, val isDark: Boolean) {
    OLED_BLACK("oled_black", "OLED Pure Black", true),
    CHARCOAL("charcoal", "Charcoal Dark", true),
    PAPER_WARM("paper_warm", "Warm Paper (E-Ink)", false),
    MINIMAL_LIGHT("minimal_light", "Minimal Light", false)
}

val OledColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = OledCard,
    onPrimaryContainer = Color.White,
    secondary = OledOnMuted,
    onSecondary = Color.Black,
    background = OledBackground,
    onBackground = OledOnBackground,
    surface = OledSurface,
    onSurface = OledOnBackground,
    surfaceVariant = OledCard,
    onSurfaceVariant = OledOnMuted,
    outline = OledBorder
)

val CharcoalColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = CharcoalBackground,
    primaryContainer = CharcoalCard,
    onPrimaryContainer = CharcoalOnBackground,
    secondary = CharcoalOnMuted,
    onSecondary = Color.White,
    background = CharcoalBackground,
    onBackground = CharcoalOnBackground,
    surface = CharcoalSurface,
    onSurface = CharcoalOnBackground,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = CharcoalOnMuted,
    outline = CharcoalBorder
)

val PaperColorScheme = lightColorScheme(
    primary = PaperOnBackground,
    onPrimary = PaperBackground,
    primaryContainer = PaperCard,
    onPrimaryContainer = PaperOnBackground,
    secondary = PaperOnMuted,
    onSecondary = PaperBackground,
    background = PaperBackground,
    onBackground = PaperOnBackground,
    surface = PaperSurface,
    onSurface = PaperOnBackground,
    surfaceVariant = PaperCard,
    onSurfaceVariant = PaperOnMuted,
    outline = PaperBorder
)

val LightColorScheme = lightColorScheme(
    primary = LightOnBackground,
    onPrimary = Color.White,
    primaryContainer = LightCard,
    onPrimaryContainer = LightOnBackground,
    secondary = LightOnMuted,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightOnMuted,
    outline = LightBorder
)

@Composable
fun FreeLauncherTheme(
    themeMode: LauncherThemeMode = LauncherThemeMode.OLED_BLACK,
    launcherFont: LauncherFont = LauncherFont.MINIMAL_SANS,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when (themeMode) {
        LauncherThemeMode.OLED_BLACK -> OledColorScheme
        LauncherThemeMode.CHARCOAL -> CharcoalColorScheme
        LauncherThemeMode.PAPER_WARM -> PaperColorScheme
        LauncherThemeMode.MINIMAL_LIGHT -> LightColorScheme
    }
    val typography = getTypographyForFont(launcherFont)

    CompositionLocalProvider(
        LocalLauncherFont provides launcherFont
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
