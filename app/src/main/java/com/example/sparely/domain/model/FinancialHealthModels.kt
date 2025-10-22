package com.example.sparely.domain.model

/**
 * Financial health score (0-100) based on multiple factors.
 */
data class FinancialHealthScore(
    val overallScore: Int,
    val savingsRateScore: Int,
    val emergencyFundScore: Int,
    val budgetAdherenceScore: Int,
    val goalProgressScore: Int,
    val debtRatioScore: Int,
    val healthLevel: HealthLevel,
    val topStrengths: List<String>,
    val improvementAreas: List<ImprovementTip>
) {
    val scoreBreakdown: Map<String, Int> = mapOf(
        "Savings Rate" to savingsRateScore,
        "Emergency Fund" to emergencyFundScore,
        "Budget Adherence" to budgetAdherenceScore,
        "Goal Progress" to goalProgressScore,
        "Debt Management" to debtRatioScore
    )
}

/**
 * Health level based on overall score.
 */
enum class HealthLevel(val minScore: Int, val maxScore: Int, val label: String) {
    EXCELLENT(90, 100, "Excellent"),
    GOOD(75, 89, "Good"),
    FAIR(60, 74, "Fair"),
    NEEDS_WORK(40, 59, "Needs Work"),
    CRITICAL(0, 39, "Critical");

    companion object {
        fun fromScore(score: Int): HealthLevel {
            return values().first { score >= it.minScore && score <= it.maxScore }
        }
    }
}

/**
 * Personalized improvement suggestion.
 */
data class ImprovementTip(
    val title: String,
    val description: String,
    val priority: Priority,
    val potentialScoreGain: Int,
    val actionable: String
)

enum class Priority {
    HIGH,
    MEDIUM,
    LOW
}
