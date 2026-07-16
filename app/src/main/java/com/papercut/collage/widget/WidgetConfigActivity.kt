package com.papercut.collage.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.papercut.collage.data.WidgetRepository
import com.papercut.collage.ui.theme.PaperCutTheme
import com.papercut.collage.ui.widget.WidgetConfigScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs when a widget is added: decides which collage this appWidgetId shows.
 * If the user came from the editor's "add to home screen" button the choice is
 * already made ([PendingPin]) and we bind and finish without a prompt.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Cancelled result by default: if the user backs out, no widget is added.
        setResult(Activity.RESULT_CANCELED, resultIntent())

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        PendingPin.consume(this)?.let { collageId ->
            confirm(collageId)
            return
        }

        enableEdgeToEdge()
        setContent {
            PaperCutTheme {
                WidgetConfigScreen(
                    onPick = { collageId -> confirm(collageId) },
                    onCancel = { finish() },
                )
            }
        }
    }

    /**
     * Bind the collage, hand RESULT_OK back, and let the render wait for the
     * host to actually place the widget.
     *
     * RESULT_OK only authorises the widget — the launcher creates it some time
     * afterwards, and anything pushed in between is discarded (which is why a
     * freshly pinned widget came up blank while reconfiguring the same widget
     * worked). See [CollageWidgetUpdater.updateWhenPlaced].
     */
    private fun confirm(collageId: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                WidgetRepository.from(applicationContext).bind(appWidgetId, collageId)
            }
            setResult(Activity.RESULT_OK, resultIntent())
            finish()
            CollageWidgetProvider.requestUpdateWhenPlaced(applicationContext, appWidgetId)
        }
    }

    private fun resultIntent() =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
