package com.example.sparely.domain.model

import java.time.LocalDate

/**
 * Aggregated analytics derived from the expense history.
 */
data class AnalyticsSnapshot(
    val totalEmergency: Double = 0.0,
    val totalInvested: Double = 0.0,
    val totalSafeInvested: Double = 0.0,
    val totalHighRiskInvested: Double = 0.0,
    val totalFun: Double = 0.0,
    val totalSpent: Double = 0.0,
    val chartPoints: List<TrendPoint> = emptyList(),
    val categoryBreakdown: Map<ExpenseCategory, Double> = emptyMap(),
    val averageMonthlyReserve: Double = 0.0,
    val averageMonthlyExpense: Double = 0.0,
    val projectedReserveSixMonths: Double = 0.0,
    val projectedReserveTwelveMonths: Double = 0.0
) {
    val totalReserved: Double = totalEmergency + totalInvested + totalFun
}

data class TrendPoint(
    val date: LocalDate,
    val cumulativeSaved: Double,
    val cumulativeInvested: Double
)

data class AlertMessage(
    val title: String,
    val description: String,
    val type: AlertType,
    val priority: Int = 5,
    val actionable: Boolean = false
)
