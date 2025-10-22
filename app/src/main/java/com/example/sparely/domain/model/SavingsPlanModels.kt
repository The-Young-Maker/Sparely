package com.example.sparely.domain.model

/**
 * Guidance on how much to set aside per savings bucket for the current month.
 */
data class SavingsPlan(
    val entries: List<SavingsPlanEntry>
) {
    val totalTarget: Double = entries.sumOf { it.targetAmount }
    val totalAlreadySetAside: Double = entries.sumOf { it.alreadySetAside }
    val totalRemaining: Double = entries.sumOf { it.remainingAmount }
}

/**
 * Per-category target and progress summary.
 */
data class SavingsPlanEntry(
    val category: SavingsCategory,
    val targetAmount: Double,
    val alreadySetAside: Double,
    val recommendedSafeAmount: Double? = null,
    val recommendedHighRiskAmount: Double? = null
) {
    val remainingAmount: Double = (targetAmount - alreadySetAside).coerceAtLeast(0.0)
}
