package com.papercut.collage.data

import android.content.Context
import com.papercut.collage.model.BoardBackground
import com.papercut.collage.model.Collage
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Single access point for collage persistence. */
class CollageRepository(private val dao: CollageDao) {

    fun observeCollages(): Flow<List<Collage>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun load(id: String): Collage? = dao.getById(id)?.toDomain()

    suspend fun save(collage: Collage) {
        dao.save(
            collage.toEntity(),
            collage.pieces.map { it.toEntity(collage.id) },
            collage.texts.map { it.toEntity(collage.id) },
        )
    }

    /** Deletes the collage, its pieces (FK cascade), and their cutout files. */
    suspend fun delete(id: String) {
        val collage = load(id)
        dao.deleteCollage(id)
        collage?.pieces?.forEach { piece ->
            runCatching { File(piece.cutoutPath).delete() }
        }
        (collage?.background as? BoardBackground.Image)?.let {
            runCatching { File(it.path).delete() }
        }
    }

    companion object {
        fun from(context: Context) =
            CollageRepository(PaperCutDatabase.get(context).collageDao())
    }
}
