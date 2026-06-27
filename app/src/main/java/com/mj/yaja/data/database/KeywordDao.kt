package com.mj.yaja.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordDao {
    @Query("SELECT * FROM keywords ORDER BY createdAt ASC")
    fun observeAllKeywords(): Flow<List<KeywordEntity>>

    @Query("SELECT * FROM keywords ORDER BY createdAt ASC")
    fun getAllKeywordsSync(): List<KeywordEntity>

    @Query("SELECT * FROM keywords WHERE id = :id LIMIT 1")
    fun getKeywordById(id: String): KeywordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(keyword: KeywordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(keywords: List<KeywordEntity>): List<Long>

    @Query("DELETE FROM keywords WHERE id = :id")
    fun delete(id: String): Int

    @Query("DELETE FROM keywords")
    fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM keywords")
    fun getCount(): Int
}
