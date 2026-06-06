package com.mj.yaja.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        JournalDayCacheEntity::class,
        KeywordEntity::class,
        TodoIndexEntity::class,
        EventIndexEntity::class,
        KeywordMatchEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(JournalTypeConverters::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalCacheDao(): JournalCacheDao
    abstract fun keywordDao(): KeywordDao
    abstract fun todoIndexDao(): TodoIndexDao
    abstract fun eventIndexDao(): EventIndexDao
    abstract fun keywordMatchDao(): KeywordMatchDao

    companion object {
        @Volatile
        private var INSTANCE: JournalDatabase? = null

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `keywords` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`name` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`relation` TEXT NOT NULL, " +
                    "`aliases` TEXT NOT NULL, " +
                    "`isEnabled` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL" +
                    ")"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `todo_index` (" +
                    "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`date` TEXT NOT NULL, " +
                    "`entryIndex` INTEGER NOT NULL, " +
                    "`lineIndexInEntry` INTEGER NOT NULL, " +
                    "`displayText` TEXT NOT NULL, " +
                    "`isChecked` INTEGER NOT NULL, " +
                    "`dayLabel` TEXT NOT NULL, " +
                    "`lineHash` TEXT NOT NULL" +
                    ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_index_date` ON `todo_index` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_index_lineHash` ON `todo_index` (`lineHash`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `event_index` (" +
                    "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`date` TEXT NOT NULL, " +
                    "`entryIndex` INTEGER NOT NULL, " +
                    "`recordedTime` TEXT, " +
                    "`mentionedTime` TEXT, " +
                    "`displayText` TEXT NOT NULL, " +
                    "`lineHash` TEXT NOT NULL" +
                    ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_event_index_date` ON `event_index` (`date`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `keyword_matches` (" +
                    "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`keywordId` TEXT NOT NULL, " +
                    "`date` TEXT NOT NULL, " +
                    "`entryIndex` INTEGER NOT NULL, " +
                    "`matchedText` TEXT NOT NULL, " +
                    "`confidence` REAL NOT NULL, " +
                    "`matchType` TEXT NOT NULL, " +
                    "`snippet` TEXT NOT NULL, " +
                    "`startIndex` INTEGER NOT NULL, " +
                    "`endExclusive` INTEGER NOT NULL" +
                    ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_keyword_matches_keywordId` ON `keyword_matches` (`keywordId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_keyword_matches_date` ON `keyword_matches` (`date`)")
            }
        }

        fun getDatabase(context: Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    "journal_database"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
