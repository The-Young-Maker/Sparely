package com.example.sparely.domain.logic

import com.example.sparely.domain.model.EmergencyFundGoal
import com.example.sparely.domain.model.SparelySettings
import com.example.sparely.domain.model.LivingSituation
import com.example.sparely.domain.model.EmploymentStatus
import kotlin.math.ceil
import kotlin.math.max

object EmergencyFundCalculator {
    fun calculate(settings: SparelySettings, monthlyExpenseEstimate: Double, existingEmergency: Double): EmergencyFundGoal {
        val baselineMonths = resolveTargetMonths(settings)
        val monthlyBaseline = resolveMonthlyBaseline(settings, monthlyExpenseEstimate)
        val candidateAmount = baselineMonths * monthlyBaseline
        // Special-case: allow a much smaller emergency fund for young users who still live with parents
        // and are working part-time or earning very little. In that case, if the calculated candidate
        // amount is small, recommend a small, realistic floor between $200 and $300.
        val targetAmount = if (settings.age <= 18
            && settings.livingSituation == LivingSituation.WITH_PARENTS
            && (settings.employmentStatus == EmploymentStatus.PART_TIME || settings.monthlyIncome < 1000.0)
        ) {
            if (candidateAmount < 300.0) candidateAmount.coerceAtLeast(200.0) else candidateAmount
        } else {
            candidateAmount.coerceAtLeast(500.0)
        }
        val liquidityBoost = (settings.mainAccountBalance * 0.25) +
            (settings.savingsAccountBalance * 0.6) +
            (settings.vaultsBalance * 0.8)
        val existing = max(existingEmergency, settings.currentEmergencyFund).coerceAtLeast(0.0) + liquidityBoost.coerceAtLeast(0.0)
        val shortfall = (targetAmount - existing).coerceAtLeast(0.0)
        val recommendedContribution = if (baselineMonths <= 0.0) 0.0 else (shortfall / ceil(baselineMonths)).coerceAtLeast(0.0)
        return EmergencyFundGoal(
            targetMonths = baselineMonths,
            targetAmount = targetAmount,
            shortfallAmount = shortfall,
            recommendedMonthlyContribution = recommendedContribution
        )
    }

    private fun resolveTargetMonths(settings: SparelySettings): Double {
        var months = 3.0
        months += when (settings.livingSituation) {
            LivingSituation.WITH_PARENTS -> -1.2
            LivingSituation.RENTING -> 0.5
            LivingSituation.HOMEOWNER -> 1.5
            LivingSituation.OTHER -> 0.0
        }
        months += when (settings.employmentStatus) {
            EmploymentStatus.STUDENT -> -0.8
            EmploymentStatus.PART_TIME -> -0.3
            EmploymentStatus.FULL_TIME, EmploymentStatus.EMPLOYED -> 0.4
            EmploymentStatus.SELF_EMPLOYED -> 2.0
            EmploymentStatus.UNEMPLOYED -> 1.2
            EmploymentStatus.RETIRED -> 0.8
        }
        if (settings.hasDebts) {
            months += 0.6
        }
        months += when {
            settings.age < 22 -> -0.6
            settings.age > 50 -> 0.6
            else -> 0.0
        }
        val income = settings.monthlyIncome
        if (income > 8000.0) months += 0.5 else if (income < 2500.0) months -= 0.4
        return months.coerceIn(1.0, 9.0)
    }

    private fun resolveMonthlyBaseline(settings: SparelySettings, monthlyExpenseEstimate: Double): Double {
        val income = settings.monthlyIncome.coerceAtLeast(0.0)
        val baselineFromIncome = if (income > 0.0) income * 0.65 else 0.0
        val subscriptionCushion = if (settings.subscriptionTotal > 0.0) settings.subscriptionTotal * 1.1 else 0.0
        val baseline = listOf(
            monthlyExpenseEstimate,
            baselineFromIncome,
            subscriptionCushion,
            1000.0
        ).filter { it > 0.0 }.maxOrNull() ?: 1000.0
        return baseline
    }
}
