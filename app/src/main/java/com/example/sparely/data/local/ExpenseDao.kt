package com.example.sparely.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun observeExpensesBetween(from: LocalDate, to: LocalDate): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    suspend fun getExpensesBetween(from: LocalDate, to: LocalDate): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(entity: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(entity: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun findExpenseById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getMostRecentExpense(): ExpenseEntity?

    @Query("SELECT IFNULL(SUM(amount), 0.0) FROM expenses WHERE date BETWEEN :from AND :to")
    suspend fun getTotalSpentBetween(from: LocalDate, to: LocalDate): Double

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}
