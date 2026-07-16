package com.papercut.collage.ui.editor

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/** Fire [onTap] on a tap anywhere on this element. */
fun Modifier.pointerInputTap(onTap: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        detectTapGestures(onTap = { onTap() })
    }

/**
 * Combined pan/zoom/rotate. Only active when [enabled] (i.e. the piece is
 * selected), so unselected pieces stay tappable without hijacking drags.
 * Re-keyed on [key] + [enabled] so the gesture detector restarts correctly.
 */
fun Modifier.pointerInputTransform(
    key: Any,
    enabled: Boolean,
    onTransform: (pan: Offset, zoom: Float, rotationDeg: Float) -> Unit,
): Modifier =
    if (!enabled) this else this.pointerInput(key, enabled) {
        detectTransformGestures { _, pan, zoom, rotation ->
            onTransform(pan, zoom, rotation)
        }
    }
