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
    private const val KEY_SET_AT = "pending_collage_at"

    /**
     * A pin older than this is stale — the widget it was parked for was never
     * placed. Prevents some ancient request from binding a random unbound
     * widget months later (the updater claims pins, not just the config
     * activity — see [CollageWidgetUpdater.update]).
     */
    private const val MAX_AGE_MS = 10 * 60 * 1000L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun set(context: Context, collageId: String) {
        prefs(context).edit()
            .putString(KEY_COLLAGE_ID, collageId)
            .putLong(KEY_SET_AT, System.currentTimeMillis())
            .apply()
    }

    /** Returns the parked collage id (once) and clears it. */
    fun consume(context: Context): String? {
        val p = prefs(context)
        val id = p.getString(KEY_COLLAGE_ID, null) ?: return null
        val age = System.currentTimeMillis() - p.getLong(KEY_SET_AT, 0L)
        p.edit().remove(KEY_COLLAGE_ID).remove(KEY_SET_AT).apply()
        return if (age in 0..MAX_AGE_MS) id else null
    }
}
