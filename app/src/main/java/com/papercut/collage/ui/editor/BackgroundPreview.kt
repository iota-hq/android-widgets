package com.papercut.collage.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.papercut.collage.model.BoardBackground
import com.papercut.collage.render.BoardBackgroundRenderer

/**
 * Paints a [BoardBackground] through the same renderer the widget export uses,
 * so a swatch, the editor board, and the home-screen widget can't disagree.
 */
@Composable
fun BackgroundPreview(
    background: BoardBackground,
    modifier: Modifier = Modifier,
    cornerRadius: Float = 0f,
) {
    Canvas(modifier) {
        drawIntoCanvas { canvas ->
            BoardBackgroundRenderer.draw(
                canvas.nativeCanvas,
                size.width.toInt().coerceAtLeast(1),
                size.height.toInt().coerceAtLeast(1),
                background,
                cornerRadius,
            )
        }
    }
}
