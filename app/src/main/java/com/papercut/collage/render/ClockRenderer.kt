package com.papercut.collage.render

import android.graphics.Canvas
import android.graphics.Paint
import com.papercut.collage.model.ClockOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Draws a **static** snapshot of the clock, for previews only — the editor
 * board, Home thumbnails, the widget picker.
 *
 * The real widget never uses this: there, the clock is a live `TextClock` laid
 * over the bitmap, so drawing it here too would double it. This exists so a
 * preview shows where the clock will sit, using the same format patterns the
 * widget will use.
 */
object ClockRenderer {

    /**
     * @param is24Hour the device's clock setting — pass
     *   `DateFormat.is24HourFormat(context)`. It's a user toggle, not a locale
     *   property, so it can't be inferred here; the widget's TextClock honours
     *   the same setting.
     */
    fun draw(canvas: Canvas, widthPx: Int, heightPx: Int, clock: ClockOverlay, is24Hour: Boolean) {
        if (!clock.enabled) return

        val shortEdge = minOf(widthPx, heightPx)
        val textSize = shortEdge * clock.sizeFraction
        val pattern = if (is24Hour) clock.style.pattern24 else clock.style.pattern12
        val now = Date()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = clock.color
            this.textSize = textSize
            typeface = clock.font.typeface()
            textAlign = Paint.Align.CENTER
            // Matches the shadow baked into the widget layout's TextClocks.
            setShadowLayer(textSize * 0.12f, 0f, textSize * 0.04f, 0x80000000.toInt())
        }

        // Patterns may carry a line break (time + date).
        val lines = pattern.split("\n").map { line ->
            runCatching { SimpleDateFormat(line, Locale.getDefault()).format(now) }.getOrDefault("")
        }

        val lineHeight = textSize * 1.1f
        val blockHeight = lineHeight * lines.size

        // The clock centre sits at the stored fraction of the board.
        val top = clock.posY * heightPx - blockHeight / 2f
        val centerX = clock.posX * widthPx

        lines.forEachIndexed { i, line ->
            // drawText takes a baseline, not a top edge.
            val baseline = top + lineHeight * i - paint.fontMetrics.ascent * 0.86f
            canvas.drawText(line, centerX, baseline, paint)
        }
    }

    /**
     * The clock's rendered footprint (width, height) in px — used by the editor
     * to place the drag handle over the text. Same maths as [draw].
     */
    fun measure(widthPx: Int, heightPx: Int, clock: ClockOverlay, is24Hour: Boolean): Pair<Float, Float> {
        val shortEdge = minOf(widthPx, heightPx)
        val textSize = shortEdge * clock.sizeFraction
        val pattern = if (is24Hour) clock.style.pattern24 else clock.style.pattern12
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            typeface = clock.font.typeface()
        }
        val now = Date()
        val lines = pattern.split("\n").map { line ->
            runCatching { SimpleDateFormat(line, Locale.getDefault()).format(now) }.getOrDefault("")
        }
        val blockWidth = lines.maxOf { paint.measureText(it) }
        val blockHeight = textSize * 1.1f * lines.size
        return blockWidth to blockHeight
    }
}
