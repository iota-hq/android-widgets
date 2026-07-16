package com.papercut.collage.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.papercut.collage.model.Collage
import java.io.File

/**
 * Draws a [Collage] into a single transparent bitmap. This bitmap is what the
 * home-screen widget displays and what the editor exports (spec §6). Piece
 * transforms are normalized (0..1) so this is resolution-independent — pass the
 * exact widget pixel size to avoid oversized RemoteViews bitmaps.
 */
object CollageRenderer {

    /**
     * @param widthPx  target output width (e.g. current widget width in px)
     * @param heightPx target output height
     * @param drawClock bake a static clock into the bitmap. True for previews;
     *   **false for the widget**, where a live TextClock sits on top and would
     *   otherwise show twice.
     * @param is24Hour the device's clock setting; only read when [drawClock].
     */
    fun render(
        collage: Collage,
        widthPx: Int,
        heightPx: Int,
        drawClock: Boolean = false,
        is24Hour: Boolean = false,
    ): Bitmap {
        val out = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        BoardBackgroundRenderer.draw(
            canvas,
            widthPx,
            heightPx,
            collage.background,
            collage.cornerRadius,
        )

        // Soft drop shadow so pieces look like layered paper.
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setShadowLayer(widthPx * 0.02f, 0f, widthPx * 0.008f, 0x66000000)
        }

        collage.pieces.sortedBy { it.zIndex }.forEach { piece ->
            val bmp = decode(piece.cutoutPath) ?: return@forEach
            val targetW = piece.scale * widthPx
            val targetH = targetW * (bmp.height.toFloat() / bmp.width.toFloat())

            val matrix = Matrix().apply {
                postScale(targetW / bmp.width, targetH / bmp.height)
                postRotate(piece.rotation, targetW / 2f, targetH / 2f)
                postTranslate(
                    piece.centerX * widthPx - targetW / 2f,
                    piece.centerY * heightPx - targetH / 2f,
                )
            }
            canvas.drawBitmap(bmp, matrix, shadow)
        }

        if (drawClock) {
            ClockRenderer.draw(canvas, widthPx, heightPx, collage.clock, is24Hour)
        }
        return out
    }

    private fun decode(path: String): Bitmap? =
        runCatching { BitmapFactory.decodeFile(File(path).absolutePath) }.getOrNull()
}
