package com.freelauncher.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.freelauncher.app.data.db.CustomWallpaperEntity

enum class WallpaperCategory(val title: String) {
    MINIMAL_DARK("Minimal Dark"),
    GRADIENT_ELEGANT("Atmospheric Gradients"),
    LIGHT_EINK("Paper & Light"),
    TRANSPARENT_GLASS("Frosted Glass & Glassmorphism"),
    CUSTOM("My Creations")
}

data class LauncherWallpaper(
    val id: String,
    val name: String,
    val subtitle: String,
    val category: WallpaperCategory,
    val colors: List<Color>,
    val isDark: Boolean = true,
    val brush: Brush = Brush.verticalGradient(colors)
) {
    companion object {
        val OLED_VOID = LauncherWallpaper(
            id = "oled_void",
            name = "Pitch Black Void",
            subtitle = "True 0% OLED power saving canvas",
            category = WallpaperCategory.MINIMAL_DARK,
            colors = listOf(Color(0xFF000000), Color(0xFF000000)),
            isDark = true,
            brush = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF000000)))
        )

        val MIDNIGHT_SLATE = LauncherWallpaper(
            id = "midnight_slate",
            name = "Midnight Slate",
            subtitle = "Deep graphite with subtle charcoal grain",
            category = WallpaperCategory.MINIMAL_DARK,
            colors = listOf(Color(0xFF14171D), Color(0xFF0B0D11)),
            isDark = true,
            brush = Brush.verticalGradient(listOf(Color(0xFF161922), Color(0xFF0C0E14)))
        )

        val DEEP_INDIGO = LauncherWallpaper(
            id = "deep_indigo",
            name = "Twilight Indigo",
            subtitle = "Quiet dusk gradient with deep purple aura",
            category = WallpaperCategory.GRADIENT_ELEGANT,
            colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF020617)),
            isDark = true,
            brush = Brush.verticalGradient(listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF020617)))
        )

        val AURORA_EMERALD = LauncherWallpaper(
            id = "aurora_emerald",
            name = "Forest Aurora",
            subtitle = "Calm evergreen twilight mist",
            category = WallpaperCategory.GRADIENT_ELEGANT,
            colors = listOf(Color(0xFF064E3B), Color(0xFF022C22), Color(0xFF00140E)),
            isDark = true,
            brush = Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF022C22), Color(0xFF00140E)))
        )

        val SUNSET_AMBER = LauncherWallpaper(
            id = "sunset_amber",
            name = "Amber Horizon",
            subtitle = "Warm mahogany and subtle copper glow",
            category = WallpaperCategory.GRADIENT_ELEGANT,
            colors = listOf(Color(0xFF451A03), Color(0xFF1C0A00), Color(0xFF0C0400)),
            isDark = true,
            brush = Brush.verticalGradient(listOf(Color(0xFF451A03), Color(0xFF1C0A00), Color(0xFF0A0300)))
        )

        val CYBER_NOIR = LauncherWallpaper(
            id = "cyber_noir",
            name = "Cosmic Violet",
            subtitle = "Deep cosmic nebula with celestial blue undertones",
            category = WallpaperCategory.GRADIENT_ELEGANT,
            colors = listOf(Color(0xFF2E1065), Color(0xFF0F172A), Color(0xFF030712)),
            isDark = true,
            brush = Brush.verticalGradient(listOf(Color(0xFF2E1065), Color(0xFF0F172A), Color(0xFF030712)))
        )

        val PAPER_PARCHMENT = LauncherWallpaper(
            id = "paper_parchment",
            name = "E-Ink Parchment",
            subtitle = "Gentle warm book-paper feel for calm reading",
            category = WallpaperCategory.LIGHT_EINK,
            colors = listOf(Color(0xFFF7F5EE), Color(0xFFEBE6D8)),
            isDark = false,
            brush = Brush.verticalGradient(listOf(Color(0xFFF7F5EE), Color(0xFFEAE5D7)))
        )

        val NORDIC_MIST = LauncherWallpaper(
            id = "nordic_mist",
            name = "Nordic Mist",
            subtitle = "Clean minimalist frosted alabaster",
            category = WallpaperCategory.LIGHT_EINK,
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)),
            isDark = false,
            brush = Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)))
        )

        val FROSTED_GLASS = LauncherWallpaper(
            id = "frosted_glass",
            name = "Frosted Glassmorphism",
            subtitle = "Semi-translucent glass overlay with soft luminous blur",
            category = WallpaperCategory.TRANSPARENT_GLASS,
            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
            isDark = true,
            brush = Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A)))
        )

        val SOLAR_FLARE = LauncherWallpaper(
            id = "solar_flare",
            name = "Solar Flare",
            subtitle = "Radiant cosmic orange with deep ember glow",
            category = WallpaperCategory.GRADIENT_ELEGANT,
            colors = listOf(Color(0xFF7C2D12), Color(0xFF450A0A), Color(0xFF180303)),
            isDark = true
        )

        val DEEP_SEA = LauncherWallpaper(
            id = "deep_sea",
            name = "Abyssal Trench",
            subtitle = "Mysterious ocean depths with teal undertones",
            category = WallpaperCategory.GRADIENT_ELEGANT,
            colors = listOf(Color(0xFF164E63), Color(0xFF083344), Color(0xFF020617)),
            isDark = true
        )

        val MIDNIGHT_ROSE = LauncherWallpaper(
            id = "midnight_rose",
            name = "Midnight Rose",
            subtitle = "Sultry crimson gradients for quiet nights",
            category = WallpaperCategory.GRADIENT_ELEGANT,
            colors = listOf(Color(0xFF4C0519), Color(0xFF270211), Color(0xFF0F0108)),
            isDark = true
        )

        val LUNAR_GREY = LauncherWallpaper(
            id = "lunar_grey",
            name = "Lunar Shadow",
            subtitle = "Clean monochromatic grey for deep focus",
            category = WallpaperCategory.GRADIENT_ELEGANT,
            colors = listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A)),
            isDark = true
        )

        val ALL_WALLPAPERS: List<LauncherWallpaper> = listOf(
            OLED_VOID,
            MIDNIGHT_SLATE,
            DEEP_INDIGO,
            AURORA_EMERALD,
            SUNSET_AMBER,
            CYBER_NOIR,
            SOLAR_FLARE,
            DEEP_SEA,
            MIDNIGHT_ROSE,
            LUNAR_GREY,
            PAPER_PARCHMENT,
            NORDIC_MIST,
            FROSTED_GLASS
        )

        fun getById(id: String): LauncherWallpaper {
            return ALL_WALLPAPERS.find { it.id == id } ?: OLED_VOID
        }

        fun fromEntity(entity: CustomWallpaperEntity): LauncherWallpaper {
            val colors = entity.colors.map { Color(it.toInt()) }
            return LauncherWallpaper(
                id = "custom_theme_${entity.id}",
                name = entity.name,
                subtitle = "User-created atmosphere",
                category = WallpaperCategory.CUSTOM,
                colors = colors,
                isDark = entity.isDark,
                brush = Brush.verticalGradient(colors)
            )
        }
    }
}
