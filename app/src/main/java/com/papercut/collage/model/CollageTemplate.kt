package com.papercut.collage.model

/**
 * Prebuilt starting points. A template is just a [Collage] with the board and
 * clock already set and no pieces — the user adds photos on top.
 *
 * The clock ones are "animated" in the only sense a widget allows: the system
 * ticks a real TextClock over the collage, so they're live without costing a
 * re-render or a wakeup.
 */
enum class CollageTemplate(
    val label: String,
    val description: String,
) {
    BLANK("Blank", "Transparent board, no clock"),

    BIG_CLOCK("Big clock", "Large time over a dark mesh"),

    CLOCK_DATE("Clock + date", "Time and date on soft linen"),

    PHOTO_CLOCK("Photo clock", "Small clock, room for cutouts"),

    CORKBOARD("Corkboard", "Pinboard texture, no clock"),

    SUNSET("Sunset", "Warm gradient, clock at the bottom"),
    ;

    /** A fresh collage from this template. Name is generated, as usual. */
    fun toCollage(): Collage = when (this) {
        BLANK -> Collage()

        BIG_CLOCK -> Collage(
            aspect = BoardAspect.WIDE_16_9,
            background = BoardBackground.Gradient(GradientPreset.AURORA),
            cornerRadius = 0.25f,
            clock = ClockOverlay(
                enabled = true,
                position = OverlayPosition.CENTER,
                style = ClockStyle.TIME,
                sizeFraction = 0.34f,
            ),
        )

        CLOCK_DATE -> Collage(
            aspect = BoardAspect.WIDE_16_9,
            background = BoardBackground.Gradient(GradientPreset.LINEN_SKY),
            cornerRadius = 0.3f,
            clock = ClockOverlay(
                enabled = true,
                position = OverlayPosition.CENTER,
                style = ClockStyle.TIME_DATE,
                color = 0xFF1A1A1A.toInt(),
                sizeFraction = 0.16f,
            ),
        )

        PHOTO_CLOCK -> Collage(
            aspect = BoardAspect.SQUARE_1_1,
            background = BoardBackground.Texture(TextureKind.PAPER),
            cornerRadius = 0.2f,
            clock = ClockOverlay(
                enabled = true,
                position = OverlayPosition.TOP,
                style = ClockStyle.TIME,
                color = 0xFF1A1A1A.toInt(),
                sizeFraction = 0.12f,
            ),
        )

        CORKBOARD -> Collage(
            aspect = BoardAspect.CLASSIC_4_3,
            background = BoardBackground.Texture(TextureKind.CORK),
            cornerRadius = 0.12f,
        )

        SUNSET -> Collage(
            aspect = BoardAspect.WIDE_16_9,
            background = BoardBackground.Gradient(GradientPreset.EMBER),
            cornerRadius = 0.25f,
            clock = ClockOverlay(
                enabled = true,
                position = OverlayPosition.BOTTOM,
                style = ClockStyle.TIME_AMPM,
                sizeFraction = 0.18f,
            ),
        )
    }
}
