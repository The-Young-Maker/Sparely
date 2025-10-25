package com.example.sparely.domain.logic

import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.SparelySettings
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Distributes the "saving tax" portion of each expense across smart vaults.
 */
object SavingTaxEngine {

    data class Context(
        val expenseAmount: Double,
        val expenseDate: LocalDate,
        val settings: SparelySettings,
        val vaults: List<SmartVault>,
        val minimumContribution: Double = 0.0
    )

    data class PlannedContribution(
        val vaultId: Long,
        val amount: Double
    )

    fun calculate(context: Context): List<PlannedContribution> {
        val baseRate = context.settings.savingTaxRate.coerceIn(0.0, 1.0)
        if (baseRate <= 0.0) return emptyList()
        if (context.expenseAmount <= 0.0) return emptyList()

        val eligibleVaults = context.vaults.filter { vault ->
            !vault.archived && (vault.targetAmount > 0.0) && (vault.targetAmount - vault.currentBalance) > 0.0
        }
        if (eligibleVaults.isEmpty()) return emptyList()

        // Calculate base amount and round UP to nearest dollar for final transfer amount
        val rawBaseAmount = context.expenseAmount * baseRate
        val baseAmount = ceil(rawBaseAmount)
        if (baseAmount < context.minimumContribution) return emptyList()

        val weights = DynamicAllocationEngine.calculateWeights(
            vaults = eligibleVaults,
            globalMode = context.settings.vaultAllocationMode,
            today = context.expenseDate
        )
        if (weights.isEmpty()) return emptyList()

        val weightMap = weights.associateBy { it.vaultId }
        val adjustedWeights = eligibleVaults.map { vault ->
            val baseWeight = weightMap[vault.id]?.weight ?: 0.0
            val modifier = overrideMultiplier(baseRate, vault.savingTaxRateOverride)
            vault.id to baseWeight * modifier
        }

        val totalAdjusted = adjustedWeights.sumOf { it.second }
        if (totalAdjusted <= 0.0) return emptyList()

        val normalized = adjustedWeights.map { (vaultId, raw) ->
            vaultId to (raw / totalAdjusted)
        }

        val baseCents = (baseAmount * 100).roundToInt()
        if (baseCents <= 0) return emptyList()

        // Round UP to cents for each vault contribution
        val drafts = normalized.map { (vaultId, weight) ->
            val rawCents = weight * baseCents
            val ceilCents = ceil(rawCents).toInt()
            ContributionDraft(
                vaultId = vaultId,
                cents = ceilCents,
                fractional = rawCents - ceilCents
            )
        }.toMutableList()

        // No need for remainder distribution since we're rounding up each contribution
        return drafts
            .filter { it.cents > 0 }
            .map { PlannedContribution(it.vaultId, it.cents / 100.0) }
            .filter { it.amount >= context.minimumContribution }
    }

    private fun overrideMultiplier(baseRate: Double, override: Double?): Double {
        if (override == null) return 1.0
        val safeBase = baseRate.takeIf { it > 0.0 } ?: return 1.0
        val ratio = override / safeBase
        return when {
            ratio.isNaN() || ratio.isInfinite() -> 1.0
            ratio <= 0.0 -> 0.0
            else -> ratio
        }
    }

    private data class ContributionDraft(
        val vaultId: Long,
        var cents: Int,
        val fractional: Double
    )
}
