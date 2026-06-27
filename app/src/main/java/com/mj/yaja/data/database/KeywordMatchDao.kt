package com.mj.yaja.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface KeywordMatchDao {
    @Query("SELECT * FROM keyword_matches ORDER BY date DESC, entryIndex ASC")
    fun getAllMatchesSync(): List<KeywordMatchEntity>

    @Query("SELECT * FROM keyword_matches WHERE keywordId = :keywordId ORDER BY date DESC, entryIndex ASC")
    fun getMatchesForKeywordSync(keywordId: String): List<KeywordMatchEntity>

    @Query("SELECT * FROM keyword_matches WHERE date = :date ORDER BY entryIndex ASC")
    fun getMatchesForDateSync(date: String): List<KeywordMatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(matches: List<KeywordMatchEntity>): List<Long>

    @Query("DELETE FROM keyword_matches WHERE keywordId = :keywordId")
    fun deleteByKeyword(keywordId: String): Int

    @Query("DELETE FROM keyword_matches WHERE date = :date")
    fun deleteByDate(date: String): Int

    @Query("DELETE FROM keyword_matches WHERE date IN (:dates)")
    fun deleteByDates(dates: List<String>): Int

    @Query("DELETE FROM keyword_matches")
    fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM keyword_matches")
    fun getCount(): Int
}
