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
import com.papercut.collage.model.OverlayFont
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
        var collageId = widgetRepo.collageIdFor(appWidgetId)

        // The invisible-widget bug, finally: most launchers do NOT launch the
        // configure activity for a *pinned* widget — they just create it and
        // broadcast onUpdate. Our pin flow parked the collage id for the config
        // activity to claim, so on those launchers nothing ever bound the
        // widget and it rendered empty (reconfiguring via long-press → Edit
        // *did* open the config activity, which is why that "fixed" it). So an
        // unbound widget claims the parked pin right here, on whatever
        // launcher, with no config round-trip.
        if (collageId == null) {
            PendingPin.consume(context)?.let { pinned ->
                widgetRepo.bind(appWidgetId, pinned)
                collageId = pinned
                Log.i(TAG, "update($appWidgetId): unbound widget claimed pending pin → $pinned")
            }
        }

        val collage = collageId?.let { CollageRepository.from(context).load(it) }

        val views = RemoteViews(context.packageName, R.layout.widget_collage)

        if (collage == null) {
            Log.w(TAG, "update($appWidgetId): no collage bound (collageId=$collageId) — rendering empty")
            // Unbound (or the collage was deleted) — leave the ImageView empty;
            // tapping still opens the app so the user can pick a collage.
            views.setImageViewBitmap(R.id.collage_image, null)
            hideAllClocks(views)
        } else {
            val options = manager.getAppWidgetOptions(appWidgetId)
            val (frameW, frameH) = framePx(context, options)
            // Displayed collage area: the board's aspect fitted inside the frame.
            // fitCenter scales the bitmap into exactly this rect, clamped or not.
            val (dispW, dispH) = fitAspect(frameW, frameH, collage.aspect.ratio)
            val (wPx, hPx) = clampPixels(dispW, dispH)
            // The clock is a live view on top, so the bitmap must not include it.
            val bitmap = CollageRenderer.render(collage, wPx, hPx, drawClock = false)
            Log.i(
                TAG,
                "update($appWidgetId): collage=${collage.name} pieces=${collage.pieces.size} " +
                    "bitmap=${wPx}x$hPx clock=${collage.clock.enabled}",
            )
            views.setImageViewBitmap(R.id.collage_image, bitmap)
            applyClock(context, views, collage.clock, frameW, frameH, dispW, dispH)
            persist(context, widgetRepo, appWidgetId, bitmap)
        }

        views.setOnClickPendingIntent(R.id.collage_image, tapIntent(context, appWidgetId, collageId))
        manager.updateAppWidget(appWidgetId, views)
        Log.i(TAG, "update($appWidgetId): pushed to host")
    }

    private val clockIds = mapOf(
        OverlayFont.CLASSIC to R.id.clock_classic,
        OverlayFont.THIN to R.id.clock_thin,
        OverlayFont.CONDENSED to R.id.clock_condensed,
        OverlayFont.SERIF to R.id.clock_serif,
        OverlayFont.MONO to R.id.clock_mono,
        OverlayFont.SCRIPT to R.id.clock_script,
    )

    private fun hideAllClocks(views: RemoteViews) {
        clockIds.values.forEach { views.setViewVisibility(it, View.GONE) }
    }

    /**
     * Point the right TextClock (one per font — RemoteViews can't set a
     * Typeface) at the chosen format, then move it to the stored free position.
     *
     * Positioning trick: every clock view fills the widget frame with
     * `gravity=center`, and text centres itself in the *padded* area — so
     * asymmetric padding shifts the centre anywhere, and `setViewPadding` is
     * remotable. To put the centre at c in a frame of size W: pad the near side
     * by |2c − W| and leave the far side at 0.
     */
    private fun applyClock(
        context: Context,
        views: RemoteViews,
        clock: ClockOverlay,
        frameW: Int,
        frameH: Int,
        dispW: Int,
        dispH: Int,
    ) {
        hideAllClocks(views)
        if (!clock.enabled) return

        val id = clockIds[clock.font] ?: return
        views.setViewVisibility(id, View.VISIBLE)
        views.setCharSequence(id, "setFormat12Hour", clock.style.pattern12)
        views.setCharSequence(id, "setFormat24Hour", clock.style.pattern24)
        views.setTextColor(id, clock.color)

        // Size is stored relative to the board so the clock keeps its proportions
        // when the widget is resized. RemoteViews wants sp, so convert px → sp.
        val density = context.resources.displayMetrics.scaledDensity
        val sizeSp = (minOf(dispW, dispH) * clock.sizeFraction) / density
        views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sizeSp)

        // posX/posY are fractions of the *collage*, which sits letterboxed in
        // the frame — map into frame coordinates first.
        val cx = (frameW - dispW) / 2f + clock.posX * dispW
        val cy = (frameH - dispH) / 2f + clock.posY * dispH
        var pl = 0
        var pt = 0
        var pr = 0
        var pb = 0
        if (cx >= frameW / 2f) pl = (2 * cx - frameW).roundToInt() else pr = (frameW - 2 * cx).roundToInt()
        if (cy >= frameH / 2f) pt = (2 * cy - frameH).roundToInt() else pb = (frameH - 2 * cy).roundToInt()
        views.setViewPadding(id, pl, pt, pr, pb)
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
     * Widget frame in px. The options bundle reports dp for the portrait
     * (min width / max height) and landscape footprints; we render the portrait
     * one and let `fitCenter` handle the other orientation.
     */
    private fun framePx(context: Context, options: Bundle): Pair<Int, Int> {
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        // Before the launcher reports a size, fall back to the 2×2 target cell.
        if (widthDp <= 0 || heightDp <= 0) {
            return dpToPx(context, 110) to dpToPx(context, 110)
        }
        return dpToPx(context, widthDp).coerceAtLeast(1) to dpToPx(context, heightDp).coerceAtLeast(1)
    }

    /**
     * Fit the board's aspect inside the frame so pieces keep their proportions
     * — the same math the editor preview uses.
     */
    private fun fitAspect(frameW: Int, frameH: Int, boardAspect: Float): Pair<Int, Int> {
        var w = frameW
        var h = frameH
        if (w.toFloat() / h > boardAspect) w = (h * boardAspect).roundToInt() else h = (w / boardAspect).roundToInt()
        return w.coerceAtLeast(1) to h.coerceAtLeast(1)
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
