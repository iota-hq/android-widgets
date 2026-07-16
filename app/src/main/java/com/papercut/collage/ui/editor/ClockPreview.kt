package com.papercut.collage.ui.editor

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import com.papercut.collage.model.ClockOverlay
import com.papercut.collage.render.ClockRenderer
import kotlinx.coroutines.delay

/**
 * The clock as it will appear on the board, drawn by the same [ClockRenderer]
 * the thumbnails use.
 *
 * Unlike the widget — where the system ticks a real TextClock — nothing here
 * updates on its own, so this repaints once a second while the editor is open.
 * That's cheap and only while you're looking at it.
 */
@Composable
fun ClockPreview(clock: ClockOverlay, modifier: Modifier = Modifier) {
    if (!clock.enabled) return
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    var tick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(clock.enabled) {
        while (true) {
            delay(1_000)
            tick++
        }
    }

    Canvas(modifier) {
        @Suppress("UNUSED_EXPRESSION") tick // redraw each tick
        drawIntoCanvas { canvas ->
            ClockRenderer.draw(
                canvas.nativeCanvas,
                size.width.toInt().coerceAtLeast(1),
                size.height.toInt().coerceAtLeast(1),
                clock,
                is24Hour,
            )
        }
    }
}
