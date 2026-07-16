package com.papercut.collage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.papercut.collage.ui.PaperCutApp as PaperCutRoot
import com.papercut.collage.ui.theme.PaperCutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Launched from a widget tap: open that collage straight away (spec §7).
        val startCollageId = intent?.getStringExtra(EXTRA_COLLAGE_ID)
        setContent {
            PaperCutTheme {
                PaperCutRoot(startCollageId = startCollageId)
            }
        }
    }

    companion object {
        const val EXTRA_COLLAGE_ID = "com.papercut.collage.extra.COLLAGE_ID"
    }
}
