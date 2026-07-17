package com.papercut.collage.model

import java.util.UUID

/**
 * One paper cutout placed on the board. Transform values are normalized to the
 * board (0..1) so the same collage renders identically at any output size —
 * editor preview, exported PNG, or widget bitmap.
 */
data class CollagePiece(
    val id: String = UUID.randomUUID().toString(),
    /** Path to the processed transparent-PNG cutout. */
    val cutoutPath: String,
    /** Center position, normalized to board width/height. */
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    /** Piece width as a fraction of board width; height derives from aspect. */
    val scale: Float = 0.4f,
    /** Rotation in degrees. */
    val rotation: Float = 0f,
    /** Stacking order; higher draws on top. */
    val zIndex: Int = 0,
    /** Seed keeping the torn edge stable across re-renders. */
    val edgeSeed: Long = System.nanoTime(),
)

/**
 * Board shapes offered in the editor. Plain photo ratios rather than widget cell
 * counts — that's how people actually think about a picture, and the widget
 * renders whatever ratio its collage carries.
 */
enum class BoardAspect(val label: String, val ratio: Float) {
    WIDE_16_9("16:9", 16f / 9f),
    CLASSIC_4_3("4:3", 4f / 3f),
    SQUARE_1_1("1:1", 1f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    TALL_9_16("9:16", 9f / 16f),
    ;

    companion object {
        /** A rectangle reads better on a home screen than a square. */
        val DEFAULT = WIDE_16_9
    }
}

data class Collage(
    val id: String = UUID.randomUUID().toString(),
    val name: String = NameGenerator.random(),
    val aspect: BoardAspect = BoardAspect.DEFAULT,
    val background: BoardBackground = BoardBackground.None,
    /**
     * Board corner rounding: 0 = square, 1 = fully rounded, as a fraction of the
     * short edge. Applies to the background only — cutouts still overhang.
     */
    val cornerRadius: Float = 0f,
    /** Live clock drawn over the collage; see [ClockOverlay]. */
    val clock: ClockOverlay = ClockOverlay.OFF,
    val pieces: List<CollagePiece> = emptyList(),
    /** User text baked into the render; see [TextPiece]. Shares z-order with pieces. */
    val texts: List<TextPiece> = emptyList(),
)
