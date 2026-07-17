package com.papercut.collage.model

import java.util.UUID

/**
 * A piece of user text placed on the board. Unlike the clock it never ticks, so
 * it's baked straight into the rendered bitmap — which means it just works on
 * the widget with zero RemoteViews tricks, and can rotate/overlap freely like a
 * photo piece.
 *
 * Transforms are normalized to the board (0..1), same as [CollagePiece], so the
 * text lands identically in the editor, thumbnails, and the widget export.
 */
data class TextPiece(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val font: OverlayFont = OverlayFont.CLASSIC,
    val color: Int = 0xFFFFFFFF.toInt(),
    /** Text size as a fraction of the board's short edge. */
    val sizeFraction: Float = 0.12f,
    /** Center position, normalized to board width/height. */
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    /** Rotation in degrees. */
    val rotation: Float = 0f,
    /** Stacking order, shared with photo pieces; higher draws on top. */
    val zIndex: Int = 0,
)
