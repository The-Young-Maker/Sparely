package com.example.sparely.domain.logic

import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.VaultAllocationMode
import com.example.sparely.domain.model.VaultPriority
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * Computes dynamic allocation weights for Smart Vault contributions.
 */
object DynamicAllocationEngine {

    data class AllocationWeight(
        val vaultId: Long,
        val weight: Double
    )

    fun calculateWeights(
        vaults: List<SmartVault>,
        globalMode: VaultAllocationMode,
        today: LocalDate = LocalDate.now()
    ): List<AllocationWeight> {
        if (vaults.isEmpty()) return emptyList()

        val manualCandidates = when (globalMode) {
            VaultAllocationMode.MANUAL -> vaults
            else -> vaults.filter { it.allocationMode == VaultAllocationMode.MANUAL && it.manualAllocationPercent != null }
        }

        val rawManual = manualCandidates.associate { vault ->
            val fallback = 1.0 / vaults.size
            val percent = when (globalMode) {
                VaultAllocationMode.MANUAL -> vault.manualAllocationPercent ?: fallback
                else -> vault.manualAllocationPercent ?: 0.0
            }
            vault.id to percent.coerceAtLeast(0.0)
        }.toMutableMap()

        val manualTotalRaw = rawManual.values.sum()
        val manualScale = when {
            manualTotalRaw <= 0.0 -> 0.0
            manualTotalRaw <= 1.0 -> 1.0
            else -> 1.0 / manualTotalRaw
        }
        val manualWeights = rawManual.mapValues { it.value * manualScale }.toMutableMap()
        var manualShare = manualWeights.values.sum()

        val dynamicVaults = vaults.filter { it.id !in manualWeights.keys }
        val dynamicShare = (1.0 - manualShare).coerceAtLeast(0.0)

        val dynamicWeights = if (dynamicVaults.isEmpty() || dynamicShare <= 0.0) {
            emptyMap()
        } else {
            val rawScores = dynamicVaults.associate { it.id to rawScore(it, today) }
            val totalScore = rawScores.values.sum()
            if (totalScore <= 0.0) {
                val equalWeight = dynamicShare / dynamicVaults.size
                dynamicVaults.associate { it.id to equalWeight }
            } else {
                rawScores.mapValues { (_, score) ->
                    if (score <= 0.0) 0.0 else (score / totalScore) * dynamicShare
                }
            }
        }

        val combined = mutableListOf<AllocationWeight>()
        manualWeights.forEach { (vaultId, weight) ->
            if (weight > 0.0) {
                combined.add(AllocationWeight(vaultId, weight))
            }
        }
        dynamicWeights.forEach { (vaultId, weight) ->
            if (weight > 0.0) {
                combined.add(AllocationWeight(vaultId, weight))
            }
        }

        val total = combined.sumOf { it.weight }
        if (total <= 0.0) {
            val equalWeight = 1.0 / vaults.size
            return vaults.map { AllocationWeight(it.id, equalWeight) }
        }

        return combined.map { weight ->
            AllocationWeight(weight.vaultId, weight.weight / total)
        }
    }

    private fun rawScore(vault: SmartVault, today: LocalDate): Double {
        val gap = max(0.0, vault.targetAmount - vault.currentBalance)
        val base = if (gap > 0.0) gap else 1.0
        val priorityFactor = when (vault.priority) {
            VaultPriority.LOW -> 0.7
            VaultPriority.MEDIUM -> 1.0
            VaultPriority.HIGH -> 1.25
            VaultPriority.CRITICAL -> 1.55
        }
        val urgencyFactor = vault.targetDate?.let { targetDate ->
            val daysRemaining = ChronoUnit.DAYS.between(today, targetDate).toInt()
            when {
                daysRemaining <= 0 -> 1.6
                daysRemaining <= 30 -> 1.45
                daysRemaining <= 90 -> 1.25
                daysRemaining <= 180 -> 1.1
                daysRemaining <= 365 -> 1.0
                else -> 0.85
            }
        } ?: 1.0
        val progressRatio = if (vault.targetAmount > 0.0) {
            (vault.currentBalance / vault.targetAmount).coerceIn(0.0, 1.5)
        } else {
            0.0
        }
        val progressFactor = when {
            progressRatio >= 1.0 -> 0.2
            progressRatio >= 0.8 -> 0.55
            progressRatio >= 0.6 -> 0.85
            progressRatio >= 0.4 -> 1.0
            progressRatio >= 0.2 -> 1.2
            else -> 1.4
        }
        return base * priorityFactor * urgencyFactor * progressFactor
    }
}
