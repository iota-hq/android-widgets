package com.papercut.collage.render

/**
 * Deterministic value noise + fractal Brownian motion, seeded per piece so a
 * cutout's torn edge is identical every time it re-renders (editor, export,
 * widget). Pure integer-hash lattice noise — no allocations in the hot path.
 */
class ValueNoise(seed: Long) {

    // Fold the 64-bit seed into a 32-bit salt used by the lattice hash.
    private val salt: Int = (seed xor (seed ushr 32)).toInt() * -0x61c88647

    /** Integer hash → float in [0,1) for lattice point (x,y). */
    private fun hash(x: Int, y: Int): Float {
        var h = x * -0x61c88647 xor y * 0x27d4eb2f xor salt
        h = h xor (h ushr 15)
        h *= -0x7ee3623b
        h = h xor (h ushr 13)
        // Top 24 bits → [0,1)
        return (h ushr 8 and 0xFFFFFF) / 16_777_216f
    }

    private fun fade(t: Float): Float = t * t * (3f - 2f * t) // smoothstep

    /** Bilinearly interpolated value noise at (x,y). Range [0,1]. */
    fun value(x: Float, y: Float): Float {
        val x0 = kotlin.math.floor(x).toInt()
        val y0 = kotlin.math.floor(y).toInt()
        val fx = fade(x - x0)
        val fy = fade(y - y0)
        val v00 = hash(x0, y0)
        val v10 = hash(x0 + 1, y0)
        val v01 = hash(x0, y0 + 1)
        val v11 = hash(x0 + 1, y0 + 1)
        val a = v00 + (v10 - v00) * fx
        val b = v01 + (v11 - v01) * fx
        return a + (b - a) * fy
    }

    /** Fractal noise: sum of [octaves] halving in amplitude, doubling in freq. */
    fun fbm(x: Float, y: Float, octaves: Int = 4): Float {
        var amp = 0.5f
        var freq = 1f
        var sum = 0f
        var norm = 0f
        repeat(octaves) {
            sum += amp * value(x * freq, y * freq)
            norm += amp
            amp *= 0.5f
            freq *= 2f
        }
        return sum / norm
    }
}
