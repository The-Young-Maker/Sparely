package com.example.sparely.domain.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * Category-specific budget limits for expense tracking.
 */
data class CategoryBudget(
    val id: Long = 0,
    val category: ExpenseCategory,
    val monthlyLimit: Double,
    val yearMonth: YearMonth = YearMonth.now(),
    val isActive: Boolean = true
)

/**
 * Budget status for a specific category in a given month.
 */
data class BudgetStatus(
    val category: ExpenseCategory,
    val limit: Double,
    val spent: Double,
    val remaining: Double,
    val percentageUsed: Double,
    val status: BudgetHealthStatus,
    val yearMonth: YearMonth
) {
    val isOverBudget: Boolean = spent > limit
    val daysRemainingInMonth: Int = YearMonth.now().lengthOfMonth() - LocalDate.now().dayOfMonth
}

/**
 * Health status of a budget category.
 */
enum class BudgetHealthStatus {
    HEALTHY,       // < 70% used
    WARNING,       // 70-90% used
    CRITICAL,      // 90-100% used
    OVER_BUDGET    // > 100% used
}

/**
 * Overall budget summary across all categories.
 */
data class BudgetSummary(
    val totalBudget: Double,
    val totalSpent: Double,
    val totalRemaining: Double,
    val categoryStatuses: List<BudgetStatus>,
    val overallHealth: BudgetHealthStatus,
    val yearMonth: YearMonth = YearMonth.now()
) {
    val percentageUsed: Double = if (totalBudget > 0) (totalSpent / totalBudget).coerceIn(0.0, 2.0) else 0.0
    val categoriesOverBudget: Int = categoryStatuses.count { it.isOverBudget }
    val categoriesAtRisk: Int = categoryStatuses.count { it.status == BudgetHealthStatus.CRITICAL || it.status == BudgetHealthStatus.WARNING }
}

/**
 * Input for creating or updating a category budget.
 */
data class BudgetInput(
    val category: ExpenseCategory,
    val monthlyLimit: Double
)

data class BudgetSuggestion(
    val category: ExpenseCategory,
    val suggestedLimit: Double,
    val currentLimit: Double?,
    val historicalAverage: Double,
    val profileTarget: Double,
    val rationale: String,
    val confidence: SuggestionConfidence
)

enum class SuggestionConfidence {
    HIGH,
    MEDIUM,
    LOW
}

data class BudgetOverrunPrompt(
    val category: ExpenseCategory,
    val month: YearMonth,
    val status: BudgetStatus,
    val overspendAmount: Double,
    val latestExpense: Expense?,
    val suggestion: BudgetSuggestion?,
    val reason: BudgetPromptReason
)

enum class BudgetPromptReason {
    POTENTIAL_ONE_OFF,
    TRENDING_HIGH,
    UNPLANNED_CATEGORY
}
