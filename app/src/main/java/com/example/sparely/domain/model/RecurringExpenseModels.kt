package com.example.sparely.domain.model

import java.time.LocalDate

/**
 * Represents a recurring expense (subscription, rent, etc.)
 * Contains the template for creating actual expenses when processed.
 */
data class RecurringExpense(
    val id: Long = 0,
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
    // Expense-related fields (same as ExpenseInput)
    val includesTax: Boolean = false,
    val deductFromMainAccount: Boolean = false,
    val deductedFromVaultId: Long? = null,
    val manualPercentages: SavingsPercentages? = null
)

/**
 * Frequency options for recurring expenses.
 */
enum class RecurringFrequency(val daysInterval: Int) {
    DAILY(1),
    WEEKLY(7),
    BIWEEKLY(14),
    MONTHLY(30),
    QUARTERLY(90),
    YEARLY(365)
}

/**
 * Input for creating recurring expense.
 */
data class RecurringExpenseInput(
    val description: String,
    val amount: Double,
    val category: ExpenseCategory,
    val frequency: RecurringFrequency,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val autoLog: Boolean = true,
    val reminderDaysBefore: Int = 2,
    val notes: String? = null,
    // Expense-related fields (same as ExpenseInput)
    val includesTax: Boolean = false,
    val deductFromMainAccount: Boolean = false,
    val deductedFromVaultId: Long? = null,
    val manualPercentages: SavingsPercentages? = null
)

/**
 * Upcoming recurring expense notification.
 */
data class UpcomingRecurringExpense(
    val recurringExpense: RecurringExpense,
    val dueDate: LocalDate,
    val daysUntilDue: Int
)
