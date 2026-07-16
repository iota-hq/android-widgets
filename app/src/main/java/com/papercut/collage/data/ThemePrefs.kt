package com.papercut.collage.data

import android.content.Context
import com.papercut.collage.ui.theme.AccentPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * App-wide appearance preferences.
 *
 * SharedPreferences rather than DataStore: these are read synchronously at first
 * composition, and DataStore's async read would flash the wrong theme on launch.
 * Revisit if prefs ever grow beyond a couple of enums.
 */
object ThemePrefs {

    private const val PREFS = "papercut_prefs"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_ACCENT = "accent_palette"

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private val _accent = MutableStateFlow(AccentPalette.DYNAMIC)
    val accent: StateFlow<AccentPalette> = _accent.asStateFlow()

    /** Call once on app start, before the first composition. */
    fun init(context: Context) {
        val p = prefs(context)
        _mode.value = runCatching { ThemeMode.valueOf(p.getString(KEY_THEME, null)!!) }
            .getOrDefault(ThemeMode.SYSTEM)
        _accent.value = runCatching { AccentPalette.valueOf(p.getString(KEY_ACCENT, null)!!) }
            .getOrDefault(AccentPalette.DYNAMIC)
    }

    fun setMode(context: Context, mode: ThemeMode) {
        _mode.value = mode
        prefs(context).edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setAccent(context: Context, accent: AccentPalette) {
        _accent.value = accent
        prefs(context).edit().putString(KEY_ACCENT, accent.name).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
