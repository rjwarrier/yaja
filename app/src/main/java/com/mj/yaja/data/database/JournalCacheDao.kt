package com.mj.yaja.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalCacheDao {
    @Query("SELECT * FROM journal_day_cache WHERE journalId = :journalId AND date = :date LIMIT 1")
    fun getDayCache(journalId: String, date: String): JournalDayCacheEntity?

    @Query("SELECT * FROM journal_day_cache WHERE journalId = :journalId ORDER BY date DESC")
    fun observeAllDays(journalId: String): Flow<List<JournalDayCacheEntity>>

    @Query("SELECT * FROM journal_day_cache WHERE journalId = :journalId ORDER BY date DESC")
    fun getAllDaysSync(journalId: String): List<JournalDayCacheEntity>

    @Query("SELECT * FROM journal_day_cache WHERE journalId = :journalId AND isStarred = 1 ORDER BY date DESC")
    fun observeStarredDays(journalId: String): Flow<List<JournalDayCacheEntity>>

    @Query("SELECT * FROM journal_day_cache WHERE journalId = :journalId AND isStarred = 1 ORDER BY date DESC")
    fun getStarredDaysSync(journalId: String): List<JournalDayCacheEntity>

    @Query("SELECT * FROM journal_day_cache WHERE journalId = :journalId AND revisitOn IS NOT NULL ORDER BY date DESC")
    fun observeRevisitDays(journalId: String): Flow<List<JournalDayCacheEntity>>

    @Query("SELECT * FROM journal_day_cache WHERE journalId = :journalId AND revisitOn IS NOT NULL ORDER BY date DESC")
    fun getRevisitDaysSync(journalId: String): List<JournalDayCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(cache: JournalDayCacheEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(caches: List<JournalDayCacheEntity>): List<Long>

    @Query("DELETE FROM journal_day_cache WHERE journalId = :journalId AND date = :date")
    fun delete(journalId: String, date: String): Int

    @Query("DELETE FROM journal_day_cache WHERE journalId = :journalId AND date IN (:dates)")
    fun deleteDays(journalId: String, dates: List<String>): Int

    @Query("DELETE FROM journal_day_cache WHERE journalId = :journalId")
    fun deleteAll(journalId: String): Int

    @Query("SELECT COUNT(*) FROM journal_day_cache WHERE journalId = :journalId")
    fun getCount(journalId: String): Int
}
