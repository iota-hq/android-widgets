package com.papercut.collage.widget

import android.content.Context

/**
 * Hand-off for the "add to home screen" flow. When the user pins a widget from
 * the editor we already know which collage they meant, but the launcher assigns
 * the appWidgetId later and doesn't tell us which request it belongs to. So the
 * editor parks the collage id here and the config activity claims it.
 */
internal object PendingPin {

    private const val PREFS = "papercut_widget"
    private const val KEY_COLLAGE_ID = "pending_collage_id"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun set(context: Context, collageId: String) {
        prefs(context).edit().putString(KEY_COLLAGE_ID, collageId).apply()
    }

    /** Returns the parked collage id (once) and clears it. */
    fun consume(context: Context): String? {
        val p = prefs(context)
        val id = p.getString(KEY_COLLAGE_ID, null)
        if (id != null) p.edit().remove(KEY_COLLAGE_ID).apply()
        return id
    }
}
