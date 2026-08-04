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
        KeywordMatchEntity::class,
        RecurringTaskEntity::class,
        RecurringTaskGenerationEntity::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(JournalTypeConverters::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalCacheDao(): JournalCacheDao
    abstract fun keywordDao(): KeywordDao
    abstract fun todoIndexDao(): TodoIndexDao
    abstract fun eventIndexDao(): EventIndexDao
    abstract fun keywordMatchDao(): KeywordMatchDao
    abstract fun recurringTaskDao(): RecurringTaskDao

    companion object {
        @Volatile
        private var INSTANCE: JournalDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `journal_day_cache_migration`")
                createJournalDayCacheTable(db, "journal_day_cache_migration")
                if (hasTable(db, "journal_day_cache")) {
                    val columns = tableColumns(db, "journal_day_cache")
                    db.execSQL(
                        "INSERT OR REPLACE INTO `journal_day_cache_migration` (" +
                            "`date`, `journalId`, `entries`, `isStarred`, `label`, `revisitOn`, " +
                            "`revisitNote`, `wordCount`, `entryCount`, `fileModifiedAt`, `fileSize`" +
                            ") SELECT " +
                            selectColumn(columns, "date", "''") + ", " +
                            selectColumn(columns, "journalId", "'default'") + ", " +
                            selectColumn(columns, "entries", "'[]'") + ", " +
                            selectColumn(columns, "isStarred", "0") + ", " +
                            selectColumn(columns, "label", "''") + ", " +
                            selectColumn(columns, "revisitOn", "NULL") + ", " +
                            selectColumn(columns, "revisitNote", "''") + ", " +
                            selectColumn(columns, "wordCount", "0") + ", " +
                            selectColumn(columns, "entryCount", "0") + ", " +
                            selectColumn(columns, "fileModifiedAt", "0") + ", " +
                            selectColumn(columns, "fileSize", "0") +
                            " FROM `journal_day_cache` WHERE " + selectColumn(columns, "date", "''") + " != ''"
                    )
                    db.execSQL("DROP TABLE `journal_day_cache`")
                }
                db.execSQL("ALTER TABLE `journal_day_cache_migration` RENAME TO `journal_day_cache`")
                createJournalDayCacheIndexes(db)
            }
        }

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

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `compliance_masters` (" +
                    "`id` TEXT NOT NULL PRIMARY KEY, " +
                    "`journalId` TEXT NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`scheduleMode` TEXT NOT NULL, " +
                    "`frequency` TEXT NOT NULL, " +
                    "`dueDayOfMonth` INTEGER, " +
                    "`dueDayOfWeek` INTEGER, " +
                    "`leadDays` INTEGER NOT NULL, " +
                    "`anchorDate` TEXT NOT NULL, " +
                    "`startMonth` TEXT NOT NULL, " +
                    "`retiredOn` TEXT, " +
                    "`createdAt` INTEGER NOT NULL" +
                    ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_compliance_masters_journalId` ON `compliance_masters` (`journalId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `compliance_generations` (" +
                    "`itemId` TEXT NOT NULL, " +
                    "`targetDate` TEXT NOT NULL, " +
                    "`generatedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`itemId`, `targetDate`)" +
                    ")"
                )
                if (!hasColumn(db, "todo_index", "complianceId")) {
                    db.execSQL("ALTER TABLE `todo_index` ADD COLUMN `complianceId` TEXT")
                }
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                if (!hasColumn(db, "compliance_masters", "description")) {
                    db.execSQL("ALTER TABLE `compliance_masters` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
                }
                if (!hasColumn(db, "compliance_masters", "isActive")) {
                    db.execSQL("ALTER TABLE `compliance_masters` ADD COLUMN `isActive` INTEGER NOT NULL DEFAULT 1")
                }
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                if (!hasColumn(db, "compliance_masters", "endMode")) {
                    db.execSQL("ALTER TABLE `compliance_masters` ADD COLUMN `endMode` TEXT NOT NULL DEFAULT 'NEVER'")
                }
                if (!hasColumn(db, "compliance_masters", "endDate")) {
                    db.execSQL("ALTER TABLE `compliance_masters` ADD COLUMN `endDate` TEXT")
                }
                if (!hasColumn(db, "compliance_masters", "endCount")) {
                    db.execSQL("ALTER TABLE `compliance_masters` ADD COLUMN `endCount` INTEGER")
                }
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                if (!hasColumn(db, "compliance_masters", "itemType")) {
                    db.execSQL("ALTER TABLE `compliance_masters` ADD COLUMN `itemType` TEXT NOT NULL DEFAULT 'TASK'")
                }
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                if (!hasColumn(db, "compliance_masters", "startTime")) {
                    db.execSQL("ALTER TABLE `compliance_masters` ADD COLUMN `startTime` TEXT")
                }
            }
        }

        private fun hasColumn(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            table: String,
            column: String
        ): Boolean =
            tableColumns(db, table).contains(column)

        private fun hasTable(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            table: String
        ): Boolean =
            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
                arrayOf(table)
            ).use { cursor -> cursor.moveToFirst() }

        private fun tableColumns(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            table: String
        ): Set<String> =
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) columns += cursor.getString(nameIndex)
                }
                columns
            }

        private fun selectColumn(columns: Set<String>, column: String, fallback: String): String =
            if (columns.contains(column)) "`$column`" else fallback

        private fun createJournalDayCacheTable(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            table: String
        ) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `$table` (" +
                    "`date` TEXT NOT NULL, " +
                    "`journalId` TEXT NOT NULL, " +
                    "`entries` TEXT NOT NULL, " +
                    "`isStarred` INTEGER NOT NULL, " +
                    "`label` TEXT NOT NULL, " +
                    "`revisitOn` TEXT, " +
                    "`revisitNote` TEXT NOT NULL, " +
                    "`wordCount` INTEGER NOT NULL, " +
                    "`entryCount` INTEGER NOT NULL, " +
                    "`fileModifiedAt` INTEGER NOT NULL, " +
                    "`fileSize` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`journalId`, `date`)" +
                    ")"
            )
        }

        private fun createJournalDayCacheIndexes(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_day_cache_journalId_isStarred_date` ON `journal_day_cache` (`journalId`, `isStarred`, `date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_day_cache_journalId_revisitOn_date` ON `journal_day_cache` (`journalId`, `revisitOn`, `date`)")
        }

        fun getDatabase(context: Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    "journal_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
