package com.papercut.collage.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * Maps a home-screen widget instance to the collage it shows (spec §8), so one
 * app can back several widgets. [lastRenderedPath] is the PNG last pushed to the
 * widget — kept so it can be deleted when the widget goes away.
 */
@Entity(tableName = "widget_bindings")
data class WidgetBindingEntity(
    @PrimaryKey val appWidgetId: Int,
    val collageId: String,
    val lastRenderedPath: String? = null,
)

@Dao
interface WidgetDao {

    @Query("SELECT * FROM widget_bindings WHERE appWidgetId = :appWidgetId")
    suspend fun getBinding(appWidgetId: Int): WidgetBindingEntity?

    @Query("SELECT * FROM widget_bindings WHERE collageId = :collageId")
    suspend fun bindingsFor(collageId: String): List<WidgetBindingEntity>

    @Upsert
    suspend fun upsert(binding: WidgetBindingEntity)

    @Query("DELETE FROM widget_bindings WHERE appWidgetId = :appWidgetId")
    suspend fun deleteBinding(appWidgetId: Int)
}
