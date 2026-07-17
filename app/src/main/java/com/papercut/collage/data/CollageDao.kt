package com.papercut.collage.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CollageDao {

    @Transaction
    @Query("SELECT * FROM collages ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CollageWithPieces>>

    @Transaction
    @Query("SELECT * FROM collages WHERE id = :id")
    suspend fun getById(id: String): CollageWithPieces?

    @Upsert
    suspend fun upsertCollage(collage: CollageEntity)

    @Upsert
    suspend fun upsertPieces(pieces: List<PieceEntity>)

    @Upsert
    suspend fun upsertTexts(texts: List<TextPieceEntity>)

    @Query("DELETE FROM pieces WHERE collageId = :collageId")
    suspend fun clearPieces(collageId: String)

    @Query("DELETE FROM text_pieces WHERE collageId = :collageId")
    suspend fun clearTexts(collageId: String)

    @Query("DELETE FROM collages WHERE id = :id")
    suspend fun deleteCollage(id: String)

    /** Replace a collage, its pieces, and its text pieces atomically. */
    @Transaction
    suspend fun save(
        collage: CollageEntity,
        pieces: List<PieceEntity>,
        texts: List<TextPieceEntity>,
    ) {
        upsertCollage(collage)
        clearPieces(collage.id)
        if (pieces.isNotEmpty()) upsertPieces(pieces)
        clearTexts(collage.id)
        if (texts.isNotEmpty()) upsertTexts(texts)
    }
}
