package com.papercut.collage.render

import android.graphics.Bitmap
import kotlin.math.ceil
import kotlin.math.min

/**
 * Turns a clean segmentation cutout into a hand-torn paper cutout (spec §2/§3).
 *
 * Algorithm:
 *   1. Build a binary subject mask from the cutout's alpha.
 *   2. Distance transform → distance of each pixel from the subject (outward).
 *   3. The paper margin is the ring `0 < d <= border`. Carve that boundary with
 *      seeded fractal noise so it wanders inward in organic bites (the "tear"),
 *      plus a high-frequency term for fibrous roughness.
 *   4. Fill the paper with warm cream, shaded slightly darker toward the torn
 *      edge for depth, then composite the original subject on top.
 *
 * Everything is deterministic for a given [seed]. The outer drop shadow that
 * makes pieces look layered is added at placement time by [CollageRenderer].
 */
object PaperEdgeProcessor {

    data class Params(
        /** Paper margin thickness around the subject, in px. */
        val borderPx: Float = 16f,
        /** 0..1 — how deeply tears eat into the margin. */
        val tearAmplitude: Float = 0.75f,
        /** Noise frequency (per px) for the big torn lobes. Smaller = chunkier. */
        val tearScale: Float = 0.045f,
        /** Noise frequency for fine fiber roughness at the very edge. */
        val fiberScale: Float = 0.35f,
        val paperColor: Int = 0xFFFDF7EC.toInt(),   // warm cream
        val edgeShade: Int = 0xFFE7D9C2.toInt(),    // slightly darker torn rim
    )

    private const val SQRT2 = 1.41421356f
    private const val BIG = 1e9f

    fun toPaperCutout(cutout: Bitmap, seed: Long, params: Params = Params()): Bitmap {
        val sw = cutout.width
        val sh = cutout.height
        val margin = ceil(params.borderPx).toInt() + 4
        val w = sw + margin * 2
        val h = sh + margin * 2

        val src = IntArray(sw * sh)
        cutout.getPixels(src, 0, sw, 0, 0, sw, sh)

        // 1. Subject mask on the padded grid (alpha > 127).
        val inside = BooleanArray(w * h)
        for (y in 0 until sh) {
            val row = y * sw
            val prow = (y + margin) * w + margin
            for (x in 0 until sw) {
                if ((src[row + x] ushr 24) > 127) inside[prow + x] = true
            }
        }

        // 2. Distance from subject, outward.
        val dist = distanceTransform(inside, w, h)

        // 3 + 4. Carve the margin with noise and paint the paper.
        val noise = ValueNoise(seed)
        val out = IntArray(w * h)
        val limit = params.borderPx + 2f

        for (y in 0 until h) {
            val base = y * w
            for (x in 0 until w) {
                val p = base + x
                val d = dist[p]

                if (inside[p]) {
                    // Under the subject: full paper backing (subject drawn later).
                    out[p] = params.paperColor
                    continue
                }
                if (d > limit) continue // far outside → transparent

                // Torn threshold: margin thickness modulated by fractal noise,
                // minus a little fiber jitter.
                val lobe = noise.fbm(x * params.tearScale, y * params.tearScale, octaves = 4)
                val fiber = noise.value(x * params.fiberScale, y * params.fiberScale)
                val threshold =
                    params.borderPx * (1f - params.tearAmplitude * lobe) - fiber * 1.5f

                // Anti-aliased paper coverage: paper where d <= threshold.
                val coverage = 1f - smoothstep(threshold - 1f, threshold + 1f, d)
                if (coverage <= 0f) continue

                // Shade toward the torn edge for a touch of depth.
                val shade = smoothstep(threshold - 3f, threshold, d)
                val color = lerpColor(params.paperColor, params.edgeShade, shade * 0.7f)
                out[p] = withAlpha(color, (coverage * 255f).toInt())
            }
        }

        // 5. Composite the original subject over the paper.
        for (y in 0 until sh) {
            val row = y * sw
            val prow = (y + margin) * w + margin
            for (x in 0 until sw) {
                val s = src[row + x]
                val sa = s ushr 24
                if (sa == 0) continue
                val p = prow + x
                out[p] = if (sa == 255) s else srcOver(s, out[p])
            }
        }

        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    /** Two-pass chamfer distance transform; 0 inside the mask, growing outward. */
    private fun distanceTransform(inside: BooleanArray, w: Int, h: Int): FloatArray {
        val d = FloatArray(w * h) { if (inside[it]) 0f else BIG }
        // Forward pass.
        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = y * w + x
                if (d[p] == 0f) continue
                var m = d[p]
                if (x > 0) m = min(m, d[p - 1] + 1f)
                if (y > 0) m = min(m, d[p - w] + 1f)
                if (x > 0 && y > 0) m = min(m, d[p - w - 1] + SQRT2)
                if (x < w - 1 && y > 0) m = min(m, d[p - w + 1] + SQRT2)
                d[p] = m
            }
        }
        // Backward pass.
        for (y in h - 1 downTo 0) {
            for (x in w - 1 downTo 0) {
                val p = y * w + x
                if (d[p] == 0f) continue
                var m = d[p]
                if (x < w - 1) m = min(m, d[p + 1] + 1f)
                if (y < h - 1) m = min(m, d[p + w] + 1f)
                if (x < w - 1 && y < h - 1) m = min(m, d[p + w + 1] + SQRT2)
                if (x > 0 && y < h - 1) m = min(m, d[p + w - 1] + SQRT2)
                d[p] = m
            }
        }
        return d
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        if (edge0 == edge1) return if (x < edge0) 0f else 1f
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun lerpColor(a: Int, b: Int, t: Float): Int {
        val ar = a ushr 16 and 0xFF; val ag = a ushr 8 and 0xFF; val ab = a and 0xFF
        val br = b ushr 16 and 0xFF; val bg = b ushr 8 and 0xFF; val bb = b and 0xFF
        val r = (ar + (br - ar) * t).toInt()
        val g = (ag + (bg - ag) * t).toInt()
        val bl = (ab + (bb - ab) * t).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (alpha.coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)

    /** Straight-alpha "source over destination" composite. */
    private fun srcOver(src: Int, dst: Int): Int {
        val sa = src ushr 24
        val da = dst ushr 24
        val saf = sa / 255f
        val outA = sa + (da * (1f - saf)).toInt()
        if (outA == 0) return 0
        fun ch(shift: Int): Int {
            val sc = src ushr shift and 0xFF
            val dc = dst ushr shift and 0xFF
            val v = (sc * saf + dc * (da / 255f) * (1f - saf)) / (outA / 255f)
            return v.toInt().coerceIn(0, 255)
        }
        return (outA.coerceIn(0, 255) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
