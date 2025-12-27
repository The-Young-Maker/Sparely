package com.example.sparely.domain.logic

import com.example.sparely.domain.model.Debt
import com.example.sparely.domain.model.DebtPayoffPlan
import com.example.sparely.domain.model.DebtPayoffStrategy
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.round

object DebtEngine {

    fun generatePlan(
        debts: List<Debt>,
        monthlyBudget: Double,
        strategy: DebtPayoffStrategy
    ): DebtPayoffPlan {
        if (debts.isEmpty()) {
            return DebtPayoffPlan(
                strategy = strategy,
                prioritizedDebts = emptyList(),
                debtFreeDate = LocalDate.now(),
                totalInterestPaid = 0.0,
                monthlyPaymentBudget = monthlyBudget
            )
        }

        val sortedDebts = when (strategy) {
            DebtPayoffStrategy.AVALANCHE -> debts.sortedWith(
                compareByDescending<Debt> { it.interestRate }.thenBy { it.balance }
            )
            DebtPayoffStrategy.SNOWBALL -> debts.sortedWith(
                compareBy<Debt> { it.balance }.thenByDescending { it.interestRate }
            )
        }

        val result = simulatePayoff(sortedDebts, monthlyBudget)
        
        return DebtPayoffPlan(
            strategy = strategy,
            prioritizedDebts = sortedDebts,
            debtFreeDate = LocalDate.now().plusMonths(result.first.toLong()),
            totalInterestPaid = result.second,
            monthlyPaymentBudget = monthlyBudget
        )
    }

    /**
     * Simulates the payoff and returns Pair(monthsToFreedom, totalInterest).
     */
    private fun simulatePayoff(orderedDebts: List<Debt>, totalBudget: Double): Pair<Int, Double> {
        var months = 0
        var totalInterest = 0.0
        val tempDebts = orderedDebts.map { it.copy() }.toMutableList()
        val minimums = tempDebts.sumOf { it.minimumPayment }

        // If budget is less than minimums, we just pay minimums (and likely never finish if interest > payment, but simplified here)
        val budget = if (totalBudget < minimums) minimums else totalBudget

        while (tempDebts.any { it.balance > 0.01 } && months < 600) { // Cap at 50 years to prevent infinite loops
            months++
            var availableForExtra = budget
            
            // 1. Accrue interest and pay minimums
            tempDebts.forEachIndexed { index, debt ->
                if (debt.balance > 0) {
                    val monthlyInterest = debt.balance * (debt.interestRate / 12.0)
                    totalInterest += monthlyInterest
                    val currentBalanceWithInterest = debt.balance + monthlyInterest
                    
                    val payment = kotlin.math.min(currentBalanceWithInterest, debt.minimumPayment)
                    tempDebts[index] = debt.copy(balance = currentBalanceWithInterest - payment)
                    availableForExtra -= payment
                }
            }

            // 2. Apply snowball/avalanche to the first non-zero debt in priority list
            if (availableForExtra > 0) {
                for (i in tempDebts.indices) {
                    if (tempDebts[i].balance > 0) {
                        val current = tempDebts[i]
                        val extraPayment = kotlin.math.min(current.balance, availableForExtra)
                        tempDebts[i] = current.copy(balance = current.balance - extraPayment)
                        availableForExtra -= extraPayment
                        if (availableForExtra <= 0.01) break
                    }
                }
            }
        }

        return Pair(months, round(totalInterest * 100) / 100.0)
    }
}
