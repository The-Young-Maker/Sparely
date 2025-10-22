package com.example.sparely.domain.model

/**
 * Holds recommendation details for the current spending profile.
 */
data class RecommendationResult(
    val recommendedPercentages: SavingsPercentages,
    val safeInvestmentRatio: Double,
    val highRiskInvestmentRatio: Double,
    val rationale: String,
    val savingsPlan: SavingsPlan,
    val autoAdjusted: Boolean
)
