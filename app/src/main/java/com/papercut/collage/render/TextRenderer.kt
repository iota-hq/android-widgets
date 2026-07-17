package com.papercut.collage.render

import android.graphics.Canvas
import android.graphics.Paint
import com.papercut.collage.model.TextPiece

/**
 * Draws a [TextPiece] into the collage bitmap. Text is static, so unlike the
 * clock it's baked into every render — editor preview, thumbnails, and the
 * widget export all go through here and can't disagree.
 */
object TextRenderer {

    fun draw(canvas: Canvas, widthPx: Int, heightPx: Int, piece: TextPiece) {
        if (piece.text.isBlank()) return

        val shortEdge = minOf(widthPx, heightPx)
        val textSize = shortEdge * piece.sizeFraction

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = piece.color
            this.textSize = textSize
            typeface = piece.font.typeface()
            textAlign = Paint.Align.CENTER
            // Same soft shadow treatment as the clock, so overlays feel related.
            setShadowLayer(textSize * 0.12f, 0f, textSize * 0.04f, 0x80000000.toInt())
        }

        val lines = piece.text.split("\n")
        val lineHeight = textSize * 1.1f
        val blockHeight = lineHeight * lines.size

        val cx = piece.centerX * widthPx
        val cy = piece.centerY * heightPx

        canvas.save()
        canvas.rotate(piece.rotation, cx, cy)
        val top = cy - blockHeight / 2f
        lines.forEachIndexed { i, line ->
            val baseline = top + lineHeight * i - paint.fontMetrics.ascent * 0.86f
            canvas.drawText(line, cx, baseline, paint)
        }
        canvas.restore()
    }

    /** Rendered footprint (width, height) in px, for editor hit-targets. */
    fun measure(widthPx: Int, heightPx: Int, piece: TextPiece): Pair<Float, Float> {
        val shortEdge = minOf(widthPx, heightPx)
        val textSize = shortEdge * piece.sizeFraction
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            typeface = piece.font.typeface()
        }
        val lines = piece.text.split("\n")
        val blockWidth = lines.maxOf { paint.measureText(it) }.coerceAtLeast(textSize)
        val blockHeight = textSize * 1.1f * lines.size
        return blockWidth to blockHeight
    }
}
