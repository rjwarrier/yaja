package com.mj.yaja.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventIndexDao {
    @Query("SELECT * FROM event_index ORDER BY date DESC, entryIndex ASC")
    fun observeAllEvents(): Flow<List<EventIndexEntity>>

    @Query("SELECT * FROM event_index ORDER BY date DESC, entryIndex ASC")
    fun getAllEventsSync(): List<EventIndexEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(events: List<EventIndexEntity>): List<Long>

    @Query("DELETE FROM event_index WHERE date = :date")
    fun deleteByDate(date: String): Int

    @Query("DELETE FROM event_index WHERE date IN (:dates)")
    fun deleteByDates(dates: List<String>): Int

    @Query("DELETE FROM event_index")
    fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM event_index")
    fun getCount(): Int
}
