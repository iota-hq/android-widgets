package com.papercut.collage.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CollageEntity::class, PieceEntity::class, WidgetBindingEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class PaperCutDatabase : RoomDatabase() {
    abstract fun collageDao(): CollageDao
    abstract fun widgetDao(): WidgetDao

    companion object {
        @Volatile private var instance: PaperCutDatabase? = null

        /** v2 adds the appWidgetId → collage mapping for home-screen widgets. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS widget_bindings (
                        appWidgetId INTEGER NOT NULL PRIMARY KEY,
                        collageId TEXT NOT NULL,
                        lastRenderedPath TEXT
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * v3 adds the board background. Deliberately an ADD COLUMN and not a
         * table rebuild — see the note on [CollageEntity.backgroundColor].
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE collages ADD COLUMN background TEXT NOT NULL DEFAULT '${BackgroundCodec.NONE}'",
                )
            }
        }

        /** v4 adds the board's corner radius. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collages ADD COLUMN cornerRadius REAL NOT NULL DEFAULT 0")
            }
        }

        /** v5 adds the live clock overlay. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE collages ADD COLUMN clock TEXT NOT NULL DEFAULT '${ClockCodec.OFF}'",
                )
            }
        }

        fun get(context: Context): PaperCutDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PaperCutDatabase::class.java,
                    "papercut.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { instance = it }
            }
    }
}
