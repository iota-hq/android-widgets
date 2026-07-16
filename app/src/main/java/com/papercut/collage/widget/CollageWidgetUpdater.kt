package com.papercut.collage.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.papercut.collage.MainActivity
import com.papercut.collage.R
import com.papercut.collage.data.CollageRepository
import com.papercut.collage.data.WidgetRepository
import com.papercut.collage.model.ClockOverlay
import com.papercut.collage.model.OverlayPosition
import com.papercut.collage.render.CollageRenderer
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.delay

/**
 * Renders the collage bound to a widget at that widget's current pixel size and
 * pushes it as the ImageView bitmap (spec §7). Sizing to the actual widget
 * matters: RemoteViews bitmaps count against a per-widget memory budget, so we
 * never hand the launcher a bitmap larger than it can draw.
 */
object CollageWidgetUpdater {

    /** Largest bitmap we will hand to RemoteViews, in pixels. */
    private const val MAX_PIXELS = 720 * 720

    internal const val TAG = "PaperCutWidget"

    /** How long to wait for a launcher to actually place a just-added widget. */
    private const val PLACEMENT_TIMEOUT_MS = 8_000L
    private const val PLACEMENT_POLL_MS = 250L

    suspend fun update(context: Context, appWidgetId: Int) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetRepo = WidgetRepository.from(context)
        val collageId = widgetRepo.collageIdFor(appWidgetId)
        val collage = collageId?.let { CollageRepository.from(context).load(it) }

        val views = RemoteViews(context.packageName, R.layout.widget_collage)

        if (collage == null) {
            Log.w(TAG, "update($appWidgetId): no collage bound (collageId=$collageId) — rendering empty")
            // Unbound (or the collage was deleted) — leave the ImageView empty;
            // tapping still opens the app so the user can pick a collage.
            views.setImageViewBitmap(R.id.collage_image, null)
            hideAllClocks(views)
        } else {
            val (wPx, hPx) = targetSize(context, manager.getAppWidgetOptions(appWidgetId), collage.aspect.ratio)
            // The clock is a live view on top, so the bitmap must not include it.
            val bitmap = CollageRenderer.render(collage, wPx, hPx, drawClock = false)
            Log.i(
                TAG,
                "update($appWidgetId): collage=${collage.name} pieces=${collage.pieces.size} " +
                    "bitmap=${wPx}x$hPx clock=${collage.clock.enabled}",
            )
            views.setImageViewBitmap(R.id.collage_image, bitmap)
            applyClock(context, views, collage.clock, minOf(wPx, hPx))
            persist(context, widgetRepo, appWidgetId, bitmap)
        }

        views.setOnClickPendingIntent(R.id.collage_image, tapIntent(context, appWidgetId, collageId))
        manager.updateAppWidget(appWidgetId, views)
        Log.i(TAG, "update($appWidgetId): pushed to host")
    }

    private val clockIds = mapOf(
        OverlayPosition.TOP to R.id.clock_top,
        OverlayPosition.CENTER to R.id.clock_center,
        OverlayPosition.BOTTOM to R.id.clock_bottom,
    )

    private fun hideAllClocks(views: RemoteViews) {
        clockIds.values.forEach { views.setViewVisibility(it, View.GONE) }
    }

    /**
     * Point the chosen TextClock at the right format and style it. Only these
     * setters are remotable, which is why position is a choice between three
     * pre-placed views rather than a gravity change.
     */
    private fun applyClock(context: Context, views: RemoteViews, clock: ClockOverlay, shortEdgePx: Int) {
        hideAllClocks(views)
        if (!clock.enabled) return

        val id = clockIds[clock.position] ?: return
        views.setViewVisibility(id, View.VISIBLE)
        views.setCharSequence(id, "setFormat12Hour", clock.style.pattern12)
        views.setCharSequence(id, "setFormat24Hour", clock.style.pattern24)
        views.setTextColor(id, clock.color)

        // Size is stored relative to the board so the clock keeps its proportions
        // when the widget is resized. RemoteViews wants sp, so convert px → sp.
        val density = context.resources.displayMetrics.scaledDensity
        val sizeSp = (shortEdgePx * clock.sizeFraction) / density
        views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    /**
     * Render once the launcher has actually placed the widget.
     *
     * A configuration result only *authorises* the widget; the host creates it
     * afterwards, and anything pushed before that is dropped on the floor. There
     * is no callback for "it exists now", but the host publishes the widget's
     * size when it lays it out — an empty options bundle means it isn't there
     * yet. So poll for a real width rather than guessing at a delay. This also
     * gets us the true size instead of the fallback guess.
     */
    suspend fun updateWhenPlaced(context: Context, appWidgetId: Int) {
        val manager = AppWidgetManager.getInstance(context)
        val deadline = SystemClock.elapsedRealtime() + PLACEMENT_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val width = manager.getAppWidgetOptions(appWidgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            if (width > 0) {
                Log.i(TAG, "updateWhenPlaced($appWidgetId): host reports ${width}dp — placed")
                update(context, appWidgetId)
                return
            }
            delay(PLACEMENT_POLL_MS)
        }
        // Never reported a size. Render anyway: better a fallback-sized widget
        // than a blank one, and onAppWidgetOptionsChanged will fix the size.
        Log.w(TAG, "updateWhenPlaced($appWidgetId): no size after ${PLACEMENT_TIMEOUT_MS}ms — rendering anyway")
        update(context, appWidgetId)
    }

    suspend fun updateAll(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { update(context, it) }
    }

    /** Re-render every widget currently showing [collageId] (called after edits). */
    suspend fun updateForCollage(context: Context, collageId: String) {
        WidgetRepository.from(context).widgetIdsFor(collageId).forEach { update(context, it) }
    }

    /**
     * Widget footprint in px. The options bundle reports dp for the portrait
     * (min width / max height) and landscape footprints; we render the portrait
     * one and let `fitCenter` handle the other orientation.
     */
    private fun targetSize(context: Context, options: Bundle, boardAspect: Float): Pair<Int, Int> {
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        var w = dpToPx(context, widthDp).coerceAtLeast(1)
        var h = dpToPx(context, heightDp).coerceAtLeast(1)

        // Before the launcher reports a size, fall back to the 2×2 target cell.
        if (widthDp <= 0 || heightDp <= 0) {
            w = dpToPx(context, 110)
            h = dpToPx(context, 110)
        }

        // Fit the board's aspect inside the widget so pieces keep their
        // proportions — the same math the editor preview uses.
        if (w.toFloat() / h > boardAspect) w = (h * boardAspect).roundToInt() else h = (w / boardAspect).roundToInt()

        return clampPixels(w.coerceAtLeast(1), h.coerceAtLeast(1))
    }

    private fun clampPixels(w: Int, h: Int): Pair<Int, Int> {
        val pixels = w.toLong() * h
        if (pixels <= MAX_PIXELS) return w to h
        val factor = sqrt(MAX_PIXELS.toDouble() / pixels).toFloat()
        return (w * factor).roundToInt().coerceAtLeast(1) to (h * factor).roundToInt().coerceAtLeast(1)
    }

    private fun dpToPx(context: Context, dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        context.resources.displayMetrics,
    ).roundToInt()

    /** Keep the last render on disk so a widget can be restored without a re-render. */
    private suspend fun persist(
        context: Context,
        repo: WidgetRepository,
        appWidgetId: Int,
        bitmap: Bitmap,
    ) {
        val file = File(context.filesDir, "widget_$appWidgetId.png")
        runCatching {
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            repo.setLastRendered(appWidgetId, file.absolutePath)
        }
    }

    /** Tapping the widget opens the editor for its collage (spec §7). */
    private fun tapIntent(context: Context, appWidgetId: Int, collageId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            collageId?.let { putExtra(MainActivity.EXTRA_COLLAGE_ID, it) }
        }
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
