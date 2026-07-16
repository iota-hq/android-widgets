package com.papercut.collage.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image as ImageIcon
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import android.text.format.DateFormat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.papercut.collage.model.Collage
import com.papercut.collage.render.CollageRenderer
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A collage as it will actually look — same renderer as the widget export, so
 * the grid shows the real thing (background, z-order, torn edges) instead of a
 * stand-in. Rendered off the main thread and re-run only when the collage or the
 * available width changes.
 */
@Composable
fun CollageThumbnail(collage: Collage, modifier: Modifier = Modifier) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val widthPx = constraints.maxWidth
        val heightPx = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            (widthPx / collage.aspect.ratio).roundToInt()
        }

        val bitmap by produceState<ImageBitmap?>(null, collage, widthPx, heightPx, is24Hour) {
            value = if (widthPx <= 0 || heightPx <= 0) {
                null
            } else {
                withContext(Dispatchers.Default) {
                    runCatching {
                        CollageRenderer.render(
                            collage,
                            widthPx,
                            heightPx,
                            drawClock = true,
                            is24Hour = is24Hour,
                        ).asImageBitmap()
                    }.getOrNull()
                }
            }
        }

        val rendered = bitmap
        if (rendered != null) {
            Image(
                bitmap = rendered,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
            )
        } else if (collage.pieces.isEmpty()) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ImageIcon, contentDescription = null)
            }
        }
    }
}
