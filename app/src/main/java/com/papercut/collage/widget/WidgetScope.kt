package com.papercut.collage.widget

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process-lifetime scope for widget work. A broadcast receiver has no lifecycle
 * to scope to, so renders run here and the caller keeps the broadcast alive with
 * `goAsync()` until the job finishes.
 */
internal object WidgetScope {

    private val scope = CoroutineScope(SupervisorJob())

    /**
     * Runs [block], always invoking [onFinally] — a broadcast's PendingResult
     * must be finished even when the render throws, or the receiver leaks.
     */
    fun launch(onFinally: () -> Unit = {}, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e("PaperCutWidget", "Widget update failed", e)
            } finally {
                onFinally()
            }
        }
    }
}
