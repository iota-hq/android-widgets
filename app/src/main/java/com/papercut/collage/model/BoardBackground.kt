package com.papercut.collage.model

/**
 * What sits behind the cutouts. Everything here is produced on-device: the user
 * picks an image from their gallery, or we generate the pixels procedurally.
 *
 * Gradients and textures are described by a seed/preset rather than stored as a
 * bitmap, so they cost nothing on disk and re-render sharp at whatever size the
 * widget happens to be — the same reason piece edges carry an `edgeSeed`.
 */
sealed interface BoardBackground {

    /** Transparent — cutouts float on the wallpaper. The signature look. */
    data object None : BoardBackground

    /** A photo from the gallery, center-cropped to the board. */
    data class Image(val path: String) : BoardBackground

    data class Gradient(val preset: GradientPreset) : BoardBackground

    data class Texture(val kind: TextureKind, val seed: Long = DEFAULT_SEED) : BoardBackground

    companion object {
        const val DEFAULT_SEED = 42L
    }
}

/**
 * Curated gradient palettes — hand-picked rather than random, since random hue
 * pairs mostly look muddy and these have to sit *under* paper cutouts without
 * fighting them.
 *
 * [mesh] presets blend several soft radial fields instead of running one axis,
 * which is the organic look gradient packs usually sell. Generated here, so it
 * costs nothing on disk and needs no download.
 */
enum class GradientPreset(
    val label: String,
    /** Two colours for a linear ramp; three or four seed a mesh. */
    val colors: List<Int>,
    /** Angle in degrees, 0 = left→right, 90 = top→bottom. Linear only. */
    val angleDeg: Float = 90f,
    val mesh: Boolean = false,
) {
    // --- Linear ---
    DUSK("Dusk", listOf(0xFF2B5876.toInt(), 0xFF4E4376.toInt()), 90f),
    PEACH("Peach", listOf(0xFFFFD3A5.toInt(), 0xFFFD6585.toInt()), 120f),
    MINT("Mint", listOf(0xFFA8EDEA.toInt(), 0xFFFED6E3.toInt()), 60f),
    SAND("Sand", listOf(0xFFF6D365.toInt(), 0xFFFDA085.toInt()), 90f),
    SLATE("Slate", listOf(0xFF3E5151.toInt(), 0xFFDECBA4.toInt()), 135f),
    BLUSH("Blush", listOf(0xFFFFDEE9.toInt(), 0xFFB5FFFC.toInt()), 45f),
    MIDNIGHT("Midnight", listOf(0xFF0F2027.toInt(), 0xFF2C5364.toInt()), 110f),
    MOSS("Moss", listOf(0xFF134E5E.toInt(), 0xFF71B280.toInt()), 70f),
    PLUM("Plum", listOf(0xFF614385.toInt(), 0xFF516395.toInt()), 100f),
    EMBER("Ember", listOf(0xFFFF512F.toInt(), 0xFFF09819.toInt()), 130f),

    // --- Mesh ---
    AURORA("Aurora", listOf(0xFF1B2735.toInt(), 0xFF00C9A7.toInt(), 0xFF845EC2.toInt(), 0xFF2C73D2.toInt()), mesh = true),
    SORBET("Sorbet", listOf(0xFFFFF3E6.toInt(), 0xFFFF9671.toInt(), 0xFFFFC75F.toInt(), 0xFFF9F871.toInt()), mesh = true),
    LAGOON("Lagoon", listOf(0xFF004E64.toInt(), 0xFF00A5CF.toInt(), 0xFF9FFFCB.toInt(), 0xFF25A18E.toInt()), mesh = true),
    ORCHID("Orchid", listOf(0xFF2D0A31.toInt(), 0xFFB33771.toInt(), 0xFF6D214F.toInt(), 0xFFEE5A24.toInt()), mesh = true),
    LINEN_SKY("Linen Sky", listOf(0xFFF8F0E3.toInt(), 0xFFAECBEB.toInt(), 0xFFE8D5C4.toInt(), 0xFFC7CEEA.toInt()), mesh = true),
    CLAY("Clay", listOf(0xFF3E2723.toInt(), 0xFFA1665E.toInt(), 0xFFD4A373.toInt(), 0xFF6D4C41.toInt()), mesh = true),
}

/** Procedural paper-craft surfaces, generated from the same noise as the edges. */
enum class TextureKind(val label: String) {
    PAPER("Paper"),
    CORK("Cork"),
    WOOD("Wood"),
    LINEN("Linen"),
}
