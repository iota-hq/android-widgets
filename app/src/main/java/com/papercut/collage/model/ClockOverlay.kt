package com.papercut.collage.model

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
    val position: OverlayPosition = OverlayPosition.CENTER,
    val style: ClockStyle = ClockStyle.TIME,
    val color: Int = 0xFFFFFFFF.toInt(),
    /** Text size as a fraction of the board's short edge, so it scales with the widget. */
    val sizeFraction: Float = 0.22f,
) {
    companion object {
        val OFF = ClockOverlay(enabled = false)
    }
}

enum class OverlayPosition(val label: String) {
    TOP("Top"),
    CENTER("Middle"),
    BOTTOM("Bottom"),
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
