package com.example.sparely.domain.logic

import com.example.sparely.domain.model.AllocationBreakdown
import com.example.sparely.domain.model.ExpenseInput
import com.example.sparely.domain.model.RiskLevel
import com.example.sparely.domain.model.SavingsPercentages
import kotlin.math.round

object SavingsCalculator {
    fun calculateAllocation(
        input: ExpenseInput,
        percentages: SavingsPercentages,
        riskLevel: RiskLevel
    ): AllocationBreakdown {
        val adjusted = percentages.adjustWithinBudget()
        val emergency = input.amount * adjusted.emergency
        val invest = input.amount * adjusted.invest
        val funAmount = input.amount * adjusted.`fun`
        val safe = invest * adjusted.safeInvestmentSplit
        val risky = invest - safe

        return AllocationBreakdown(
            emergencyAmount = emergency.toCurrencyPrecision(),
            investmentAmount = invest.toCurrencyPrecision(),
            funAmount = funAmount.toCurrencyPrecision(),
            safeInvestmentAmount = safe.toCurrencyPrecision(),
            highRiskInvestmentAmount = risky.toCurrencyPrecision()
        )
    }

    private fun Double.toCurrencyPrecision(): Double = round(this * 100) / 100.0
}
