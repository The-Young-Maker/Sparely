package com.example.sparely.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseItemDao {
    @Query("SELECT * FROM expense_items WHERE expenseId = :expenseId ORDER BY id")
    fun observeItemsForExpense(expenseId: Long): Flow<List<ExpenseItemEntity>>

    @Query("SELECT * FROM expense_items WHERE expenseId = :expenseId ORDER BY id")
    suspend fun getItemsForExpense(expenseId: Long): List<ExpenseItemEntity>

    @Query("SELECT * FROM expense_items ORDER BY id")
    suspend fun getAllItems(): List<ExpenseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ExpenseItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ExpenseItemEntity>)

    @Update
    suspend fun updateItem(item: ExpenseItemEntity)

    @Delete
    suspend fun deleteItem(item: ExpenseItemEntity)

    @Query("DELETE FROM expense_items WHERE expenseId = :expenseId")
    suspend fun deleteItemsForExpense(expenseId: Long)

    @Query("DELETE FROM expense_items")
    suspend fun clearAll()
}
