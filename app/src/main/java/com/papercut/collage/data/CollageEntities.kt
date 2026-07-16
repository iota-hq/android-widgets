package com.papercut.collage.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.papercut.collage.model.BoardAspect
import com.papercut.collage.model.Collage
import com.papercut.collage.model.CollagePiece

@Entity(tableName = "collages")
data class CollageEntity(
    @PrimaryKey val id: String,
    val name: String,
    val aspect: String,
    /**
     * Legacy v1 column, no longer read. Kept so v2→v3 could be a plain ADD
     * COLUMN: rebuilding this table would mean dropping it, and SQLite runs an
     * implicit DELETE FROM first — which would cascade through the pieces FK and
     * wipe every cutout. A dead column is the cheaper trade.
     */
    val backgroundColor: Int? = null,
    /** Encoded [com.papercut.collage.model.BoardBackground]; see BackgroundCodec. */
    val background: String = BackgroundCodec.NONE,
    val cornerRadius: Float = 0f,
    /** Encoded [com.papercut.collage.model.ClockOverlay]; see ClockCodec. */
    val clock: String = ClockCodec.OFF,
    val updatedAt: Long,
)

@Entity(
    tableName = "pieces",
    foreignKeys = [
        ForeignKey(
            entity = CollageEntity::class,
            parentColumns = ["id"],
            childColumns = ["collageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("collageId")],
)
data class PieceEntity(
    @PrimaryKey val id: String,
    val collageId: String,
    val cutoutPath: String,
    val centerX: Float,
    val centerY: Float,
    val scale: Float,
    val rotation: Float,
    val zIndex: Int,
    val edgeSeed: Long,
)

data class CollageWithPieces(
    @Embedded val collage: CollageEntity,
    @Relation(parentColumn = "id", entityColumn = "collageId")
    val pieces: List<PieceEntity>,
)

// --- Mapping between persistence and domain model ---

fun CollageWithPieces.toDomain(): Collage = Collage(
    id = collage.id,
    name = collage.name,
    // v1 stored cell-based names (SQUARE_2x2 …). Those no longer exist, so old
    // rows fall back to the default rather than failing to load.
    aspect = runCatching { BoardAspect.valueOf(collage.aspect) }.getOrDefault(BoardAspect.DEFAULT),
    background = BackgroundCodec.decode(collage.background),
    cornerRadius = collage.cornerRadius,
    clock = ClockCodec.decode(collage.clock),
    pieces = pieces.sortedBy { it.zIndex }.map {
        CollagePiece(
            id = it.id,
            cutoutPath = it.cutoutPath,
            centerX = it.centerX,
            centerY = it.centerY,
            scale = it.scale,
            rotation = it.rotation,
            zIndex = it.zIndex,
            edgeSeed = it.edgeSeed,
        )
    },
)

fun Collage.toEntity(updatedAt: Long = System.currentTimeMillis()) = CollageEntity(
    id = id,
    name = name,
    aspect = aspect.name,
    background = BackgroundCodec.encode(background),
    cornerRadius = cornerRadius,
    clock = ClockCodec.encode(clock),
    updatedAt = updatedAt,
)

fun CollagePiece.toEntity(collageId: String) = PieceEntity(
    id = id,
    collageId = collageId,
    cutoutPath = cutoutPath,
    centerX = centerX,
    centerY = centerY,
    scale = scale,
    rotation = rotation,
    zIndex = zIndex,
    edgeSeed = edgeSeed,
)
