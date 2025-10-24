package com.example.sparely.domain.model

import java.time.LocalDate

/**
 * Domain representation of an expense with computed savings allocation.
 */
data class Expense(
    val id: Long,
    val description: String,
    val amount: Double,
    val category: ExpenseCategory,
    val date: LocalDate,
    val includesTax: Boolean,
    val allocation: AllocationBreakdown,
    val appliedPercentages: SavingsPercentages,
    val autoRecommended: Boolean,
    val riskLevelUsed: RiskLevel
)

/**
 * User input payload when creating a new expense entry.
 */
data class ExpenseInput(
    val id: Long? = null,
    val description: String,
    val amount: Double,
    val category: ExpenseCategory,
    val date: LocalDate,
    val includesTax: Boolean,
    val manualPercentages: SavingsPercentages? = null,
    val deductFromMainAccount: Boolean = false
)

/**
 * Supported quick filters for history screens.
 */
enum class DateRangeFilter {
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
    YEAR_TO_DATE,
    ALL_TIME
}
