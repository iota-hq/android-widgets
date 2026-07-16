package com.papercut.collage.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import com.papercut.collage.model.BoardBackground
import com.papercut.collage.model.GradientPreset
import com.papercut.collage.model.TextureKind
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws a [BoardBackground] into a canvas. Both the editor preview and the
 * widget export call this, which is the point: the widget has to match what the
 * user arranged, and two implementations would drift.
 *
 * Textures are generated from [ValueNoise] rather than bundled as PNGs — no APK
 * weight, and they stay sharp at any widget size instead of being upscaled.
 */
object BoardBackgroundRenderer {

    private data class TextureKey(val kind: TextureKind, val seed: Long, val w: Int, val h: Int)

    /**
     * Textures are a per-pixel loop, so they're cached rather than regenerated
     * per frame.
     *
     * This was one entry keyed by size, which turned out to be worse than no
     * cache: the board and the sheet's swatches are different sizes, so they
     * evicted each other and every frame regenerated a full texture — the lag
     * you'd see with a texture background selected. Several entries fixes it.
     */
    private const val CACHE_ENTRIES = 8
    private val cache = LruCache<TextureKey, Bitmap>(CACHE_ENTRIES)

    /**
     * Cap on generated texture resolution. It's noise: generating at most this
     * on the long edge and letting the canvas scale it up is indistinguishable,
     * and keeps a full-screen board from doing a million-pixel loop.
     */
    private const val MAX_TEXTURE_EDGE = 512

    /**
     * @param cornerRadius 0 = square, 1 = fully rounded, as a fraction of the
     *   board's short edge. Only the background is clipped — cutouts stay free
     *   to overhang the edge, which is what makes it read as paper on a card
     *   rather than a photo in a frame.
     */
    fun draw(
        canvas: Canvas,
        widthPx: Int,
        heightPx: Int,
        background: BoardBackground,
        cornerRadius: Float = 0f,
    ) {
        if (background is BoardBackground.None) {
            canvas.drawColor(Color.TRANSPARENT)
            return
        }

        val radiusPx = cornerRadius.coerceIn(0f, 1f) * minOf(widthPx, heightPx) / 2f
        val clipped = radiusPx > 0.5f
        if (clipped) {
            canvas.save()
            canvas.clipPath(
                Path().apply {
                    addRoundRect(
                        RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()),
                        radiusPx,
                        radiusPx,
                        Path.Direction.CW,
                    )
                },
            )
        }

        when (background) {
            is BoardBackground.None -> Unit // handled above
            is BoardBackground.Image -> drawImage(canvas, widthPx, heightPx, background.path)
            is BoardBackground.Gradient -> drawGradient(canvas, widthPx, heightPx, background.preset)
            is BoardBackground.Texture ->
                drawTexture(canvas, widthPx, heightPx, background.kind, background.seed)
        }

        if (clipped) canvas.restore()
    }

    /** Center-crop (fill), so the photo never letterboxes inside the board. */
    private fun drawImage(canvas: Canvas, w: Int, h: Int, path: String) {
        val bmp = runCatching { BitmapFactory.decodeFile(File(path).absolutePath) }.getOrNull()
        if (bmp == null) {
            canvas.drawColor(Color.TRANSPARENT)
            return
        }
        val scale = maxOf(w.toFloat() / bmp.width, h.toFloat() / bmp.height)
        val srcW = (w / scale).roundToInt().coerceAtMost(bmp.width)
        val srcH = (h / scale).roundToInt().coerceAtMost(bmp.height)
        val src = Rect(
            (bmp.width - srcW) / 2,
            (bmp.height - srcH) / 2,
            (bmp.width - srcW) / 2 + srcW,
            (bmp.height - srcH) / 2 + srcH,
        )
        canvas.drawBitmap(bmp, src, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun drawGradient(canvas: Canvas, w: Int, h: Int, preset: GradientPreset) {
        if (preset.mesh) {
            drawMesh(canvas, w, h, preset)
            return
        }
        val rad = Math.toRadians(preset.angleDeg.toDouble())
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        // Project the board onto the gradient axis so the ramp spans it fully at
        // any angle, instead of running out early on the diagonal.
        val half = (abs(dx) * w + abs(dy) * h) / 2f
        val cx = w / 2f
        val cy = h / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx - dx * half, cy - dy * half,
                cx + dx * half, cy + dy * half,
                preset.colors[0], preset.colors[1],
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    /** Where mesh blobs sit, as fractions of the board. */
    private val meshPositions = listOf(
        0.22f to 0.24f,
        0.80f to 0.30f,
        0.30f to 0.78f,
        0.76f to 0.74f,
    )

    /**
     * Mesh gradient: a base fill with soft radial fields blended over it. Each
     * blob fades to its own colour at zero alpha, so they melt together instead
     * of showing rings.
     */
    private fun drawMesh(canvas: Canvas, w: Int, h: Int, preset: GradientPreset) {
        canvas.drawColor(preset.colors.first())
        val radius = maxOf(w, h) * 0.72f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        preset.colors.drop(1).forEachIndexed { i, color ->
            val (fx, fy) = meshPositions[i % meshPositions.size]
            paint.shader = RadialGradient(
                w * fx, h * fy, radius,
                intArrayOf(color, color and 0x00FFFFFF),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        }
    }

    private fun drawTexture(canvas: Canvas, w: Int, h: Int, kind: TextureKind, seed: Long) {
        // Generate at a capped resolution, then stretch to the target rect.
        val scale = minOf(1f, MAX_TEXTURE_EDGE.toFloat() / maxOf(w, h))
        val genW = (w * scale).roundToInt().coerceAtLeast(1)
        val genH = (h * scale).roundToInt().coerceAtLeast(1)

        val key = TextureKey(kind, seed, genW, genH)
        val bmp = cache.get(key)?.takeIf { !it.isRecycled }
            ?: generateTexture(genW, genH, kind, seed).also { cache.put(key, it) }

        canvas.drawBitmap(bmp, Rect(0, 0, genW, genH), Rect(0, 0, w, h), texturePaint)
    }

    private val texturePaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private fun generateTexture(w: Int, h: Int, kind: TextureKind, seed: Long): Bitmap {
        val noise = ValueNoise(seed)
        val pixels = IntArray(w * h)
        // Noise is sampled in board-relative units so a texture looks the same
        // at editor size and widget size — only the sampling density changes.
        val u = 1f / maxOf(w, h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val fx = x * u
                val fy = y * u
                pixels[y * w + x] = when (kind) {
                    TextureKind.PAPER -> paperPixel(noise, fx, fy)
                    TextureKind.CORK -> corkPixel(noise, fx, fy)
                    TextureKind.WOOD -> woodPixel(noise, fx, fy)
                    TextureKind.LINEN -> linenPixel(noise, fx, fy)
                }
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    // --- Procedural surfaces -------------------------------------------------

    /** Cream stock with faint mottling and a few darker flecks. */
    private fun paperPixel(n: ValueNoise, x: Float, y: Float): Int {
        val mottle = n.fbm(x * 22f, y * 22f, octaves = 4)
        val fleck = n.value(x * 260f, y * 260f)
        var shade = 0.94f + (mottle - 0.5f) * 0.09f
        if (fleck > 0.985f) shade -= 0.10f
        return tint(shade, 246, 242, 230)
    }

    /** Tan board with dense granular speckle. */
    private fun corkPixel(n: ValueNoise, x: Float, y: Float): Int {
        val grain = n.fbm(x * 90f, y * 90f, octaves = 3)
        val chunk = n.value(x * 34f, y * 34f)
        var shade = 0.80f + (grain - 0.5f) * 0.42f + (chunk - 0.5f) * 0.14f
        if (grain > 0.72f) shade -= 0.16f
        return tint(shade, 198, 152, 92)
    }

    /**
     * Grain: stripes along y, with fbm warping x so the rings wander like real
     * timber. `fract` of the warped coordinate gives the ring boundaries.
     */
    private fun woodPixel(n: ValueNoise, x: Float, y: Float): Int {
        val warp = n.fbm(x * 3.5f, y * 14f, octaves = 4)
        val rings = (x * 13f + warp * 3.2f) % 1f
        val ring = abs(rings - 0.5f) * 2f
        val fibre = n.value(x * 200f, y * 24f)
        val shade = 0.62f + ring * 0.30f + (fibre - 0.5f) * 0.06f
        return tint(shade, 168, 112, 66)
    }

    /** Woven cloth: crossed high-frequency bands plus a soft slub. */
    private fun linenPixel(n: ValueNoise, x: Float, y: Float): Int {
        val warpX = sin(x * 380f).let { it * it }
        val warpY = sin(y * 380f).let { it * it }
        val weave = (warpX + warpY) * 0.5f
        val slub = n.fbm(x * 30f, y * 30f, octaves = 3)
        val shade = 0.88f + (weave - 0.5f) * 0.10f + (slub - 0.5f) * 0.10f
        return tint(shade, 226, 216, 198)
    }

    /** Multiply a base colour by [shade], clamped, at full alpha. */
    private fun tint(shade: Float, r: Int, g: Int, b: Int): Int {
        val s = shade.coerceIn(0f, 1.4f)
        return Color.argb(
            255,
            (r * s).roundToInt().coerceIn(0, 255),
            (g * s).roundToInt().coerceIn(0, 255),
            (b * s).roundToInt().coerceIn(0, 255),
        )
    }
}
