package com.papercut.collage.model

import android.graphics.Typeface

/**
 * A live clock drawn over the collage.
 *
 * This is the one kind of animation a widget gets for free. RemoteViews can't
 * run arbitrary drawing, but `TextClock` is one of the handful of views the
 * system ticks *itself* — so the time stays right with no re-render, no alarm,
 * and no battery cost. The collage stays a static bitmap underneath; only the
 * clock is live.
 */
data class ClockOverlay(
    val enabled: Boolean = false,
    /**
     * Clock centre as fractions of the board (0..1) — free placement, not a
     * position preset. In the editor the clock is dragged directly; the widget
     * reproduces the spot with view padding (the only remotable layout knob).
     */
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val style: ClockStyle = ClockStyle.TIME,
    val font: OverlayFont = OverlayFont.CLASSIC,
    val color: Int = 0xFFFFFFFF.toInt(),
    /** Text size as a fraction of the board's short edge, so it scales with the widget. */
    val sizeFraction: Float = 0.22f,
) {
    companion object {
        val OFF = ClockOverlay(enabled = false)
    }
}

/**
 * Fonts for the clock and for user text. **System font families only** — the
 * widget's TextClock gets its typeface from `android:fontFamily` baked into the
 * layout (RemoteViews can't set a Typeface at runtime), and these families
 * exist on every device without bundling font files.
 */
enum class OverlayFont(val label: String, val familyName: String, val bold: Boolean) {
    CLASSIC("Classic", "sans-serif", true),
    THIN("Thin", "sans-serif-thin", false),
    CONDENSED("Narrow", "sans-serif-condensed", true),
    SERIF("Serif", "serif", true),
    MONO("Mono", "monospace", false),
    SCRIPT("Script", "cursive", false),
    ;

    /** The same face the widget layout's per-font TextClock uses. */
    fun typeface(): Typeface =
        Typeface.create(familyName, if (bold) Typeface.BOLD else Typeface.NORMAL)
}

/**
 * Clock formats, given as `TextClock` skeleton patterns. The same patterns feed
 * the editor's static preview, so preview and widget can't disagree.
 */
enum class ClockStyle(
    val label: String,
    val pattern12: String,
    val pattern24: String,
) {
    TIME("Time", "h:mm", "HH:mm"),
    TIME_SECONDS("Seconds", "h:mm:ss", "HH:mm:ss"),
    TIME_AMPM("Time + AM/PM", "h:mm a", "HH:mm"),
    TIME_DATE("Time + date", "h:mm\nEEE d MMM", "HH:mm\nEEE d MMM"),
}
