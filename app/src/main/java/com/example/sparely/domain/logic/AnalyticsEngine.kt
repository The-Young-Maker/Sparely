package com.example.sparely.domain.logic

import com.example.sparely.domain.model.AnalyticsSnapshot
import com.example.sparely.domain.model.Expense
import com.example.sparely.domain.model.ExpenseCategory
import com.example.sparely.domain.model.SavingsCategory
import com.example.sparely.domain.model.SavingsTransfer
import com.example.sparely.domain.model.TrendPoint
import java.time.YearMonth

object AnalyticsEngine {
    fun build(expenses: List<Expense>, transfers: List<SavingsTransfer> = emptyList()): AnalyticsSnapshot {
        if (expenses.isEmpty() && transfers.isEmpty()) return AnalyticsSnapshot()

        val emergencyTransfers = transfers.filter { it.category == SavingsCategory.EMERGENCY }.sumOf { it.amount }
        val investmentTransfers = transfers.filter { it.category == SavingsCategory.INVESTMENT }.sumOf { it.amount }
        val funTransfers = transfers.filter { it.category == SavingsCategory.FUN }.sumOf { it.amount }

        val totalEmergency = expenses.sumOf { it.allocation.emergencyAmount } + emergencyTransfers
        val totalInvested = expenses.sumOf { it.allocation.investmentAmount } + investmentTransfers
        val totalSafe = expenses.sumOf { it.allocation.safeInvestmentAmount } + investmentTransfers
        val totalHighRisk = expenses.sumOf { it.allocation.highRiskInvestmentAmount }
        val totalFun = expenses.sumOf { it.allocation.funAmount } + funTransfers
        val totalSpent = expenses.sumOf { it.amount }

        val monthlyExpenseAverage = computeAverageMonthlyExpense(expenses)

        val chartPoints = buildTrend(expenses)
        val categoryBreakdown = buildCategoryBreakdown(expenses)
        val (averageMonthlyReserve, projectedSix, projectedTwelve) = buildProjections(transfers)

        return AnalyticsSnapshot(
            totalEmergency = totalEmergency,
            totalInvested = totalInvested,
            totalSafeInvested = totalSafe,
            totalHighRiskInvested = totalHighRisk,
            totalFun = totalFun,
            totalSpent = totalSpent,
            chartPoints = chartPoints,
            categoryBreakdown = categoryBreakdown,
            averageMonthlyReserve = averageMonthlyReserve,
            averageMonthlyExpense = monthlyExpenseAverage,
            projectedReserveSixMonths = projectedSix,
            projectedReserveTwelveMonths = projectedTwelve
        )
    }

    private fun computeAverageMonthlyExpense(expenses: List<Expense>): Double {
        if (expenses.isEmpty()) return 0.0
        val grouped = expenses.groupBy { YearMonth.from(it.date) }
        if (grouped.isEmpty()) return 0.0
        val totals = grouped.values.map { monthExpenses -> monthExpenses.sumOf { it.amount } }
        return totals.average()
    }

    private fun buildTrend(expenses: List<Expense>): List<TrendPoint> {
        val sorted = expenses.sortedBy { it.date }
        var cumulativeSaved = 0.0
        var cumulativeInvested = 0.0
        return sorted.map { expense ->
            cumulativeSaved += expense.allocation.emergencyAmount + expense.allocation.funAmount
            cumulativeInvested += expense.allocation.investmentAmount
            TrendPoint(
                date = expense.date,
                cumulativeSaved = cumulativeSaved,
                cumulativeInvested = cumulativeInvested
            )
        }
    }

    private fun buildCategoryBreakdown(expenses: List<Expense>): Map<ExpenseCategory, Double> {
        return expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toSortedMap(compareBy { it.name })
    }

    private fun buildProjections(transfers: List<SavingsTransfer>): Triple<Double, Double, Double> {
        val monthlyTotals = transfers
            .groupBy { YearMonth.from(it.date) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        if (monthlyTotals.isEmpty()) return Triple(0.0, 0.0, 0.0)

        val averageMonthly = monthlyTotals.values.average()
        val projectedSix = averageMonthly * 6
        val projectedTwelve = averageMonthly * 12
        return Triple(averageMonthly, projectedSix, projectedTwelve)
    }
}
