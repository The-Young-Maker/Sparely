package com.example.sparely.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM category_budgets ORDER BY year DESC, month DESC, category")
    fun observeBudgets(): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM category_budgets WHERE year = :year AND month = :month")
    suspend fun getBudgetsForMonth(year: Int, month: Int): List<CategoryBudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: CategoryBudgetEntity)

    @Query("DELETE FROM category_budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM category_budgets")
    suspend fun clear()
}
