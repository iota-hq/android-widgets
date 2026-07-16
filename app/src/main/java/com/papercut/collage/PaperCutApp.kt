package com.papercut.collage

import android.app.Application
import com.papercut.collage.data.ThemePrefs

/**
 * Application entry point. Holds process-wide singletons (DB, repositories)
 * as the project grows. Kept minimal for now.
 */
class PaperCutApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Read before the first composition so we don't flash the wrong theme.
        ThemePrefs.init(this)
    }
}
