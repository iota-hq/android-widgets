package com.papercut.collage.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.papercut.collage.data.ThemeMode
import com.papercut.collage.data.ThemePrefs

private val LightColors = lightColorScheme(
    primary = md_light_primary,
    onPrimary = md_light_onPrimary,
    primaryContainer = md_light_primaryContainer,
    onPrimaryContainer = md_light_onPrimaryContainer,
    secondary = md_light_secondary,
    background = md_light_background,
    onBackground = md_light_onBackground,
    surface = md_light_surface,
    surfaceVariant = md_light_surfaceVariant,
    onSurface = md_light_onSurface,
)

private val DarkColors = darkColorScheme(
    primary = md_dark_primary,
    onPrimary = md_dark_onPrimary,
    primaryContainer = md_dark_primaryContainer,
    onPrimaryContainer = md_dark_onPrimaryContainer,
    secondary = md_dark_secondary,
    background = md_dark_background,
    onBackground = md_dark_onBackground,
    surface = md_dark_surface,
    surfaceVariant = md_dark_surfaceVariant,
    onSurface = md_dark_onSurface,
)

@Composable
fun PaperCutTheme(
    // Both follow the user's choice in Settings.
    themeMode: ThemeMode = ThemePrefs.mode.collectAsStateWithLifecycle().value,
    accent: AccentPalette = ThemePrefs.accent.collectAsStateWithLifecycle().value,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = accent.scheme(darkTheme)
        // DYNAMIC: Material You seeds from the wallpaper (API 31+). Below that
        // there's nothing to seed from, so fall back to the bundled scheme.
        ?: when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            darkTheme -> DarkColors
            else -> LightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PaperShapes,
        content = content,
    )
}

/**
 * Chunkier corner radii app-wide — cards, sheets, and buttons read softer,
 * closer to cut paper than to stock Material rectangles.
 */
private val PaperShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
