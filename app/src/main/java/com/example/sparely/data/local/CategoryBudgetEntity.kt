package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sparely.domain.model.ExpenseCategory

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: ExpenseCategory,
    val monthlyLimit: Double,
    val year: Int,
    val month: Int,
    val isActive: Boolean = true
)
