package com.example.sparely.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MainAccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: MainAccountTransactionEntity): Long

    @Query("SELECT * FROM main_account_transactions ORDER BY timestamp DESC")
    fun observeAllTransactions(): Flow<List<MainAccountTransactionEntity>>

    @Query("SELECT * FROM main_account_transactions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTransactions(limit: Int = 50): List<MainAccountTransactionEntity>

    @Query("SELECT * FROM main_account_transactions WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getTransactionsSince(since: LocalDateTime): List<MainAccountTransactionEntity>

    @Query("SELECT * FROM main_account_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): MainAccountTransactionEntity?

    @Query("SELECT * FROM main_account_transactions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTransaction(): MainAccountTransactionEntity?

    @Query("DELETE FROM main_account_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM main_account_transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT IFNULL(SUM(CASE WHEN type = 'DEPOSIT' THEN amount WHEN type = 'WITHDRAWAL' THEN -amount WHEN type = 'EXPENSE' THEN -amount WHEN type = 'VAULT_CONTRIBUTION' THEN -amount WHEN type = 'ADJUSTMENT' THEN amount ELSE 0 END), 0.0) FROM main_account_transactions")
    suspend fun calculateBalanceFromTransactions(): Double
}
