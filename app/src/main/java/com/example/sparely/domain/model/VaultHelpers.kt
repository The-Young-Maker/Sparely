package com.example.sparely.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * Helper functions for SmartVault calculations.
 */

fun SmartVault.monthsUntil(target: LocalDate?, today: LocalDate = LocalDate.now()): Int {
    val t = target ?: return 0
    val start = today.withDayOfMonth(1)
    val end = t.withDayOfMonth(1)
    val months = ChronoUnit.MONTHS.between(start, end).toInt()
    return months.coerceAtLeast(0)
}

fun SmartVault.calculateTotalNeed(): Double {
    // If this vault defines a monthlyNeed and has a start/end window, use that
    if (monthlyNeed != null && startDate != null && endDate != null) {
        val months = ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), endDate.withDayOfMonth(1)).toInt().coerceAtLeast(0)
        return (monthlyNeed * months).coerceAtLeast(0.0)
    }

    // Fallback to targetAmount minus currentBalance
    return (targetAmount - currentBalance).coerceAtLeast(0.0)
}

fun SmartVault.calculateMonthlyContribution(today: LocalDate = LocalDate.now()): Double {
    // If monthlyNeed explicitly set (flow goals), prefer it
    if (monthlyNeed != null) return monthlyNeed

    // If there's a target date, spread remaining need over remaining months
    val remaining = (targetAmount - currentBalance).coerceAtLeast(0.0)
    if (targetDate != null) {
        val months = monthsUntil(targetDate, today).coerceAtLeast(1)
        return remaining / months
    }

    // Otherwise, no monthly suggestion
    return 0.0
}
