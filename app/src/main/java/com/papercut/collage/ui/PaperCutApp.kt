package com.papercut.collage.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.papercut.collage.ui.editor.EditorScreen
import com.papercut.collage.ui.home.HomeScreen
import com.papercut.collage.ui.settings.SettingsScreen

/** Navigation host: Home ↔ Editor. */
object Routes {
    const val HOME = "home"
    const val EDITOR = "editor/{collageId}"
    fun editor(collageId: String) = "editor/$collageId"
    const val NEW_COLLAGE = "new"
    const val SETTINGS = "settings"
}

/**
 * @param startCollageId when non-null (a widget tap), open straight into that
 *   collage's editor with Home still behind it on the back stack.
 */
@Composable
fun PaperCutApp(startCollageId: String? = null) {
    val navController = rememberNavController()

    LaunchedEffect(startCollageId) {
        if (startCollageId != null) navController.navigate(Routes.editor(startCollageId))
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewCollage = { navController.navigate(Routes.editor(Routes.NEW_COLLAGE)) },
                onOpenCollage = { id -> navController.navigate(Routes.editor(id)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.EDITOR) { backStackEntry ->
            val collageId = backStackEntry.arguments?.getString("collageId") ?: Routes.NEW_COLLAGE
            EditorScreen(
                collageId = collageId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
