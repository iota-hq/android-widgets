package com.papercut.collage.data

import com.papercut.collage.model.BoardBackground
import com.papercut.collage.model.GradientPreset
import com.papercut.collage.model.TextureKind

/**
 * Flattens [BoardBackground] to a single text column and back.
 *
 * A one-column encoding keeps the v2→v3 migration to an ADD COLUMN, and the
 * shapes are small enough that a typed column per variant would be mostly nulls.
 * Anything unparseable decodes to [BoardBackground.None] rather than throwing —
 * a collage that loses its background still opens.
 */
object BackgroundCodec {

    const val NONE = "none"

    fun encode(background: BoardBackground): String = when (background) {
        is BoardBackground.None -> NONE
        is BoardBackground.Image -> "image:${background.path}"
        is BoardBackground.Gradient -> "gradient:${background.preset.name}"
        is BoardBackground.Texture -> "texture:${background.kind.name}:${background.seed}"
    }

    fun decode(raw: String?): BoardBackground {
        if (raw.isNullOrBlank() || raw == NONE) return BoardBackground.None
        // Split into at most 3 so an image path containing ':' survives.
        val parts = raw.split(":", limit = 3)
        return runCatching {
            when (parts[0]) {
                "image" -> BoardBackground.Image(raw.substringAfter("image:"))
                "gradient" -> BoardBackground.Gradient(GradientPreset.valueOf(parts[1]))
                "texture" -> BoardBackground.Texture(
                    kind = TextureKind.valueOf(parts[1]),
                    seed = parts.getOrNull(2)?.toLongOrNull() ?: BoardBackground.DEFAULT_SEED,
                )
                else -> BoardBackground.None
            }
        }.getOrDefault(BoardBackground.None)
    }
}
