package com.mj.yaja.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoIndexDao {
    @Query("SELECT * FROM todo_index ORDER BY date DESC, entryIndex ASC, lineIndexInEntry ASC")
    fun observeAllTodos(): Flow<List<TodoIndexEntity>>

    @Query("SELECT * FROM todo_index ORDER BY date DESC, entryIndex ASC, lineIndexInEntry ASC")
    fun getAllTodosSync(): List<TodoIndexEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(todos: List<TodoIndexEntity>): List<Long>

    @Query("DELETE FROM todo_index WHERE date = :date")
    fun deleteByDate(date: String): Int

    @Query("DELETE FROM todo_index WHERE date IN (:dates)")
    fun deleteByDates(dates: List<String>): Int

    @Query("DELETE FROM todo_index")
    fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM todo_index")
    fun getCount(): Int

    @Query("UPDATE todo_index SET isChecked = :isChecked WHERE date = :date AND entryIndex = :entryIndex AND lineIndexInEntry = :lineIndexInEntry")
    fun updateCheckedState(date: String, entryIndex: Int, lineIndexInEntry: Int, isChecked: Boolean): Int

    @Query("UPDATE todo_index SET isChecked = :isChecked WHERE date = :date AND lineHash = :lineHash")
    fun updateCheckedStateByHash(date: String, lineHash: String, isChecked: Boolean): Int

    @Query("UPDATE todo_index SET isChecked = :isChecked WHERE date = :date AND LOWER(TRIM(displayText)) = LOWER(TRIM(:displayText))")
    fun updateCheckedStateByText(date: String, displayText: String, isChecked: Boolean): Int
}
