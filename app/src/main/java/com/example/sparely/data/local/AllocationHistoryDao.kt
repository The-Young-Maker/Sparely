package com.example.sparely.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AllocationHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AllocationHistoryEntity): Long

    @Query("SELECT * FROM allocation_history WHERE vaultId = :vaultId ORDER BY date DESC")
    suspend fun getForVault(vaultId: Long): List<AllocationHistoryEntity>

    @Query("SELECT * FROM allocation_history ORDER BY date DESC")
    fun observeAll(): Flow<List<AllocationHistoryEntity>>

    @Query("DELETE FROM allocation_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
