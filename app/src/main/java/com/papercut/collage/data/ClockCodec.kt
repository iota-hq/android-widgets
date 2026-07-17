package com.papercut.collage.data

import com.papercut.collage.model.ClockOverlay
import com.papercut.collage.model.ClockStyle
import com.papercut.collage.model.OverlayFont

/**
 * Flattens [ClockOverlay] into one text column, matching [BackgroundCodec]'s
 * approach. Unparseable values decode to "off" rather than throwing — a bad
 * column shouldn't stop a collage from opening.
 *
 * Two generations on disk:
 * - `on:<POSITION>:<style>:<color>:<size>` — v1, position was a Top/Middle/Bottom
 *   enum. Mapped to the equivalent free coordinates on read.
 * - `v2:<posX>:<posY>:<style>:<font>:<color>:<size>` — free placement + font.
 */
object ClockCodec {

    const val OFF = "off"
    private const val V2 = "v2"

    fun encode(clock: ClockOverlay): String =
        if (!clock.enabled) {
            OFF
        } else {
            listOf(
                V2,
                clock.posX.toString(),
                clock.posY.toString(),
                clock.style.name,
                clock.font.name,
                clock.color.toString(),
                clock.sizeFraction.toString(),
            ).joinToString(":")
        }

    fun decode(raw: String?): ClockOverlay {
        if (raw.isNullOrBlank() || raw == OFF) return ClockOverlay.OFF
        val parts = raw.split(":")
        return runCatching {
            when (parts[0]) {
                V2 -> ClockOverlay(
                    enabled = true,
                    posX = parts[1].toFloat().coerceIn(0f, 1f),
                    posY = parts[2].toFloat().coerceIn(0f, 1f),
                    style = ClockStyle.valueOf(parts[3]),
                    font = OverlayFont.valueOf(parts[4]),
                    color = parts[5].toInt(),
                    sizeFraction = parts[6].toFloat(),
                )
                // Legacy "on": position presets become coordinates.
                "on" -> ClockOverlay(
                    enabled = true,
                    posX = 0.5f,
                    posY = when (parts[1]) {
                        "TOP" -> 0.16f
                        "BOTTOM" -> 0.84f
                        else -> 0.5f
                    },
                    style = ClockStyle.valueOf(parts[2]),
                    color = parts[3].toInt(),
                    sizeFraction = parts[4].toFloat(),
                )
                else -> ClockOverlay.OFF
            }
        }.getOrDefault(ClockOverlay.OFF)
    }
}
