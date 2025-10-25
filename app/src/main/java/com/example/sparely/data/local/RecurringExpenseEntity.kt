package com.example.sparely.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sparely.domain.model.ExpenseCategory
import com.example.sparely.domain.model.RecurringFrequency
import java.time.LocalDate

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Double,
    val category: ExpenseCategory,
    val frequency: RecurringFrequency,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val lastProcessedDate: LocalDate? = null,
    val isActive: Boolean = true,
    val autoLog: Boolean = true,
    val reminderDaysBefore: Int = 2,
    val merchantName: String? = null,
    val notes: String? = null,
    // Expense-related fields (same as ExpenseEntity)
    val includesTax: Boolean = false,
    val deductFromMainAccount: Boolean = false,
    val deductedFromVaultId: Long? = null,
    val manualPercentEmergency: Double? = null,
    val manualPercentInvest: Double? = null,
    val manualPercentFun: Double? = null,
    val manualSafeSplit: Double? = null
)
