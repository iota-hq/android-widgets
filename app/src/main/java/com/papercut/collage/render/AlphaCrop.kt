package com.papercut.collage.render

import android.graphics.Bitmap

/**
 * Trims fully-transparent margins so a cutout's bitmap hugs its subject.
 *
 * ML Kit hands back a foreground bitmap the size of the *whole photo*, with
 * everything but the subject transparent. Left uncropped, a piece's bounds are
 * the original frame: the selection border floats far from the paper, `scale`
 * sizes empty space rather than the subject, and we store megapixels of nothing.
 */
object AlphaCrop {

    /**
     * @param alphaThreshold alpha at or below this counts as empty. Slightly
     *   above zero so the matte's faint anti-aliased fringe doesn't defeat the
     *   crop.
     * @return a cropped copy, or [src] unchanged if it's empty or already tight.
     */
    fun cropToContent(src: Bitmap, alphaThreshold: Int = 8): Bitmap {
        val w = src.width
        val h = src.height
        if (w == 0 || h == 0) return src

        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1

        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                if ((pixels[row + x] ushr 24) > alphaThreshold) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        // Nothing opaque found — hand back the original rather than a 0×0 crop.
        if (maxX < minX || maxY < minY) return src
        if (minX == 0 && minY == 0 && maxX == w - 1 && maxY == h - 1) return src

        return Bitmap.createBitmap(src, minX, minY, maxX - minX + 1, maxY - minY + 1)
    }
}
