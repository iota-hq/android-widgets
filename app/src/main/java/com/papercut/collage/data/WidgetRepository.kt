package com.papercut.collage.data

import android.content.Context
import java.io.File

/** Persistence for widget↔collage bindings and their rendered bitmaps. */
class WidgetRepository(private val dao: WidgetDao) {

    suspend fun collageIdFor(appWidgetId: Int): String? = dao.getBinding(appWidgetId)?.collageId

    suspend fun bind(appWidgetId: Int, collageId: String) {
        val existing = dao.getBinding(appWidgetId)
        dao.upsert(
            WidgetBindingEntity(
                appWidgetId = appWidgetId,
                collageId = collageId,
                lastRenderedPath = existing?.lastRenderedPath.takeIf { existing?.collageId == collageId },
            ),
        )
    }

    /** Records the freshly rendered PNG and deletes the one it replaces. */
    suspend fun setLastRendered(appWidgetId: Int, path: String) {
        val existing = dao.getBinding(appWidgetId) ?: return
        if (existing.lastRenderedPath != path) {
            existing.lastRenderedPath?.let { runCatching { File(it).delete() } }
        }
        dao.upsert(existing.copy(lastRenderedPath = path))
    }

    /** Widget ids showing the given collage, so edits can push a re-render. */
    suspend fun widgetIdsFor(collageId: String): List<Int> =
        dao.bindingsFor(collageId).map { it.appWidgetId }

    suspend fun unbind(appWidgetId: Int) {
        dao.getBinding(appWidgetId)?.lastRenderedPath?.let { runCatching { File(it).delete() } }
        dao.deleteBinding(appWidgetId)
    }

    companion object {
        fun from(context: Context) =
            WidgetRepository(PaperCutDatabase.get(context).widgetDao())
    }
}
