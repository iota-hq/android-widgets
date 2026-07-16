package com.papercut.collage.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Accent choices for the app UI.
 *
 * [DYNAMIC] is Material You — it seeds the scheme from the wallpaper, which is
 * why the app can arrive unexpectedly green. It stays the default on Android 12+
 * because most people expect it, but everything else here is a fixed palette
 * that ignores the wallpaper entirely.
 *
 * Note this is the *app's* chrome only. Collage boards and cutouts are the
 * user's artwork and are never tinted by it.
 */
enum class AccentPalette(val label: String) {
    DYNAMIC("Material You"),
    PAPER("Paper"),
    INDIGO("Indigo"),
    ROSE("Rose"),
    FOREST("Forest"),
    SLATE("Slate");

    /** Null for [DYNAMIC], which is built from the wallpaper at runtime. */
    fun scheme(dark: Boolean): ColorScheme? = when (this) {
        DYNAMIC -> null
        PAPER -> if (dark) paperDark else paperLight
        INDIGO -> if (dark) indigoDark else indigoLight
        ROSE -> if (dark) roseDark else roseLight
        FOREST -> if (dark) forestDark else forestLight
        SLATE -> if (dark) slateDark else slateLight
    }
}

// Warm terracotta on cream — the app's own identity, closest to torn paper.
private val paperLight = lightColorScheme(
    primary = Color(0xFF9A4F2C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF390C00),
    secondary = Color(0xFF77574B),
    secondaryContainer = Color(0xFFFFDBCC),
    surfaceVariant = Color(0xFFF5DED4),
)
private val paperDark = darkColorScheme(
    primary = Color(0xFFFFB690),
    onPrimary = Color(0xFF5C1A00),
    primaryContainer = Color(0xFF7C3813),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFFE7BEAC),
    secondaryContainer = Color(0xFF5D4036),
    surfaceVariant = Color(0xFF52443D),
)

private val indigoLight = lightColorScheme(
    primary = Color(0xFF4A55A2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5B5D72),
    secondaryContainer = Color(0xFFE0E1F9),
    surfaceVariant = Color(0xFFE3E1EC),
)
private val indigoDark = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF1A2472),
    primaryContainer = Color(0xFF323C89),
    onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFFC4C5DD),
    secondaryContainer = Color(0xFF434659),
    surfaceVariant = Color(0xFF46464F),
)

private val roseLight = lightColorScheme(
    primary = Color(0xFFA23A5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E1),
    onPrimaryContainer = Color(0xFF3E001C),
    secondary = Color(0xFF75565D),
    secondaryContainer = Color(0xFFFFD9E1),
    surfaceVariant = Color(0xFFF3DDE1),
)
private val roseDark = darkColorScheme(
    primary = Color(0xFFFFB1C4),
    onPrimary = Color(0xFF5F1130),
    primaryContainer = Color(0xFF7E2946),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFFE3BDC5),
    secondaryContainer = Color(0xFF5B3F46),
    surfaceVariant = Color(0xFF524347),
)

private val forestLight = lightColorScheme(
    primary = Color(0xFF2E6B4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2F1CC),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4E6355),
    secondaryContainer = Color(0xFFD1E8D6),
    surfaceVariant = Color(0xFFDCE5DC),
)
private val forestDark = darkColorScheme(
    primary = Color(0xFF97D5B1),
    onPrimary = Color(0xFF003825),
    primaryContainer = Color(0xFF105237),
    onPrimaryContainer = Color(0xFFB2F1CC),
    secondary = Color(0xFFB5CCBB),
    secondaryContainer = Color(0xFF374B3E),
    surfaceVariant = Color(0xFF414941),
)

private val slateLight = lightColorScheme(
    primary = Color(0xFF3F6375),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC3E8FD),
    onPrimaryContainer = Color(0xFF001F2A),
    secondary = Color(0xFF4E616D),
    secondaryContainer = Color(0xFFD1E5F4),
    surfaceVariant = Color(0xFFDCE3E9),
)
private val slateDark = darkColorScheme(
    primary = Color(0xFFA8CCE0),
    onPrimary = Color(0xFF0B3445),
    primaryContainer = Color(0xFF264B5D),
    onPrimaryContainer = Color(0xFFC3E8FD),
    secondary = Color(0xFFB5C9D7),
    secondaryContainer = Color(0xFF364955),
    surfaceVariant = Color(0xFF40484D),
)
