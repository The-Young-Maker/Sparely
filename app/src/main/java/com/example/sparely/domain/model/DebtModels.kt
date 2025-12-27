package com.example.sparely.domain.model

import java.time.LocalDate

data class Debt(
    val id: Long = 0,
    val name: String,
    val balance: Double,
    val interestRate: Double, // Annual Percentage Rate (APR) e.g., 0.19 for 19%
    val minimumPayment: Double,
    val dueDateDayOfMonth: Int = 1
)

enum class DebtPayoffStrategy {
    AVALANCHE, // Highest interest first (Mathematically optimal)
    SNOWBALL   // Lowest balance first (Psychologically encouraging)
}

data class DebtPayoffPlan(
    val strategy: DebtPayoffStrategy,
    val prioritizedDebts: List<Debt>,
    val debtFreeDate: LocalDate,
    val totalInterestPaid: Double,
    val monthlyPaymentBudget: Double
)
