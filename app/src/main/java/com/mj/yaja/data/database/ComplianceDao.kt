package com.mj.yaja.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ComplianceDao {
    @Query("SELECT * FROM compliance_masters WHERE journalId = :journalId AND retiredOn IS NULL ORDER BY title COLLATE NOCASE ASC")
    fun observeActiveMasters(journalId: String): Flow<List<ComplianceMasterEntity>>

    @Query("SELECT * FROM compliance_masters WHERE journalId = :journalId ORDER BY title COLLATE NOCASE ASC")
    fun getAllMastersSync(journalId: String): List<ComplianceMasterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateMaster(master: ComplianceMasterEntity): Long

    @Query("UPDATE compliance_masters SET retiredOn = :retiredOn WHERE id = :id")
    fun retireMaster(id: String, retiredOn: String): Int

    @Query("DELETE FROM compliance_masters WHERE id = :id")
    fun deleteMaster(id: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM compliance_generations WHERE itemId = :itemId AND targetDate = :targetDate)")
    fun hasGeneration(itemId: String, targetDate: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGeneration(generation: ComplianceGenerationEntity): Long

    @Query("DELETE FROM compliance_generations WHERE itemId = :itemId")
    fun deleteGenerationsByItem(itemId: String): Int

    @Query("DELETE FROM compliance_generations WHERE itemId = :itemId AND targetDate >= :fromDate")
    fun clearFutureGenerations(itemId: String, fromDate: String): Int

    @Query("SELECT targetDate FROM compliance_generations WHERE itemId = :itemId AND targetDate > :fromDate ORDER BY targetDate ASC")
    fun getFutureGenerationDates(itemId: String, fromDate: String): List<String>

    @Query("DELETE FROM compliance_generations")
    fun deleteAllGenerations(): Int

    @Query("DELETE FROM compliance_masters")
    fun deleteAllMasters(): Int
}
