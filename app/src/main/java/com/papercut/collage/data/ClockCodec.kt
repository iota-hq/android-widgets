package com.papercut.collage.data

import com.papercut.collage.model.ClockOverlay
import com.papercut.collage.model.ClockStyle
import com.papercut.collage.model.OverlayPosition

/**
 * Flattens [ClockOverlay] into one text column, matching [BackgroundCodec]'s
 * approach. Unparseable values decode to "off" rather than throwing — a bad
 * column shouldn't stop a collage from opening.
 */
object ClockCodec {

    const val OFF = "off"

    fun encode(clock: ClockOverlay): String =
        if (!clock.enabled) {
            OFF
        } else {
            listOf(
                "on",
                clock.position.name,
                clock.style.name,
                clock.color.toString(),
                clock.sizeFraction.toString(),
            ).joinToString(":")
        }

    fun decode(raw: String?): ClockOverlay {
        if (raw.isNullOrBlank() || raw == OFF) return ClockOverlay.OFF
        val parts = raw.split(":")
        return runCatching {
            ClockOverlay(
                enabled = true,
                position = OverlayPosition.valueOf(parts[1]),
                style = ClockStyle.valueOf(parts[2]),
                color = parts[3].toInt(),
                sizeFraction = parts[4].toFloat(),
            )
        }.getOrDefault(ClockOverlay.OFF)
    }
}
