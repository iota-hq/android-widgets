package com.papercut.collage.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.papercut.collage.data.WidgetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Home-screen widget. The collage is custom-drawn, so we render it to a bitmap
 * in-app ([com.papercut.collage.render.CollageRenderer]) and show that bitmap
 * in a single ImageView (spec §7). Re-render on update and on resize; size the
 * bitmap to the actual widget to respect RemoteViews memory limits.
 *
 * Rendering touches Room and decodes bitmaps, so every callback hops onto a
 * background dispatcher inside [goAsync].
 */
class CollageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) = goAsyncIO {
        Log.i(CollageWidgetUpdater.TAG, "onUpdate(${appWidgetIds.joinToString()})")
        CollageWidgetUpdater.updateAll(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) = goAsyncIO {
        // Widget was resized — re-render at the new size. This also fires when a
        // widget is first laid out, which is a second chance at the first render.
        Log.i(CollageWidgetUpdater.TAG, "onAppWidgetOptionsChanged($appWidgetId)")
        CollageWidgetUpdater.update(context, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) = goAsyncIO {
        Log.i(CollageWidgetUpdater.TAG, "onDeleted(${appWidgetIds.joinToString()})")
        val repo = WidgetRepository.from(context)
        appWidgetIds.forEach { repo.unbind(it) }
    }

    /** Runs [block] off the main thread while holding the broadcast alive. */
    private fun goAsyncIO(block: suspend () -> Unit) {
        val result = goAsync()
        WidgetScope.launch(onFinally = { result.finish() }) {
            withContext(Dispatchers.Default) { block() }
        }
    }

    companion object {
        /**
         * Ask this provider to re-render [appWidgetIds]. Routing through a
         * broadcast keeps [onUpdate] the single render path, so a widget looks
         * the same however it got there — first placement, resize, or an edit.
         */
        fun requestUpdate(context: Context, appWidgetIds: IntArray) {
            if (appWidgetIds.isEmpty()) return
            val intent = Intent(context, CollageWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }

        /**
         * Render a freshly configured widget once its host has placed it. Runs
         * in the process-lifetime [WidgetScope], not the config activity's
         * scope — the activity finishes immediately, and the wait outlives it.
         */
        fun requestUpdateWhenPlaced(context: Context, appWidgetId: Int) {
            val app = context.applicationContext
            WidgetScope.launch {
                CollageWidgetUpdater.updateWhenPlaced(app, appWidgetId)
            }
        }
    }
}
