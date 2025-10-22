package com.example.sparely.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SavingsTransferDao {
    @Query("SELECT * FROM savings_transfers ORDER BY date DESC, id DESC")
    fun observeTransfers(): Flow<List<SavingsTransferEntity>>

    @Query("SELECT * FROM savings_transfers WHERE date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    suspend fun getTransfersBetween(start: LocalDate, end: LocalDate): List<SavingsTransferEntity>

    @Query("SELECT * FROM savings_transfers WHERE sourceAccountId = :accountId OR destinationAccountId = :accountId ORDER BY date DESC, id DESC")
    suspend fun getTransfersForAccount(accountId: Long): List<SavingsTransferEntity>

    @Query("SELECT IFNULL(SUM(amount), 0.0) FROM savings_transfers")
    suspend fun getTotalSaved(): Double

    @Query("SELECT IFNULL(SUM(amount), 0.0) FROM savings_transfers WHERE date BETWEEN :start AND :end")
    suspend fun getTotalSavedBetween(start: LocalDate, end: LocalDate): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transfer: SavingsTransferEntity)

    @Query("DELETE FROM savings_transfers")
    suspend fun clearAll()
}
