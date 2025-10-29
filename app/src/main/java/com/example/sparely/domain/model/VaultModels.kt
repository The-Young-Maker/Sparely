package com.example.sparely.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Domain models backing the Smart Vault system.
 */
data class SmartVault(
    val id: Long = 0L,
    val name: String,
    val targetAmount: Double,
    val currentBalance: Double = 0.0,
    val targetDate: LocalDate? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val monthlyNeed: Double? = null,
    val priorityWeight: Double = 1.0,
    val autoSaveEnabled: Boolean = true,
    val priority: VaultPriority = VaultPriority.MEDIUM,
    val type: VaultType = VaultType.SHORT_TERM,
    val interestRate: Double? = null,
    val allocationMode: VaultAllocationMode = VaultAllocationMode.DYNAMIC_AUTO,
    val manualAllocationPercent: Double? = null,
    val nextExpectedContribution: Double? = null,
    val lastContributionDate: LocalDate? = null,
    val autoDepositSchedule: AutoDepositSchedule? = null,
    val savingTaxRateOverride: Double? = null,
    val archived: Boolean = false,
    // Account information for linking vault to real-world account within the main account
    val accountType: AccountType? = null,
    val accountNumber: String? = null,
    val accountNotes: String? = null,
    val createdAt: LocalDate = LocalDate.now()
) {
    val progressPercent: Double = if (targetAmount > 0) (currentBalance / targetAmount).coerceIn(0.0, 1.0) else 0.0
}

data class AutoDepositSchedule(
    val amount: Double,
    val frequency: AutoDepositFrequency,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val sourceAccountId: Long? = null,
    val lastExecutionDate: LocalDate? = null
)

data class VaultContribution(
    val id: Long = 0L,
    val vaultId: Long,
    val amount: Double,
    val date: LocalDate = LocalDate.now(),
    val source: VaultContributionSource,
    val note: String? = null,
    val reconciled: Boolean = false
)

enum class VaultContributionSource {
    INCOME,
    SAVING_TAX,
    AUTO_DEPOSIT,
    MANUAL,
    TRANSFER
}

enum class VaultAdjustmentType {
    MANUAL_DEPOSIT,
    MANUAL_DEDUCTION,
    MANUAL_EDIT,
    AUTOMATIC_RECURRING_TRANSFER
}

data class VaultBalanceAdjustment(
    val id: Long = 0L,
    val vaultId: Long,
    val type: VaultAdjustmentType,
    val delta: Double,
    val resultingBalance: Double,
    val createdAt: Instant,
    val reason: String? = null
)

data class VaultProjection(
    val vaultId: Long,
    val projectedCompletionDate: LocalDate?,
    val monthlyContributionNeeded: Double,
    val riskFlag: VaultRiskFlag?
)

data class VaultRiskFlag(
    val type: VaultRiskType,
    val message: String
)

enum class VaultRiskType {
    BEHIND_SCHEDULE,
    TARGET_AT_RISK,
    COMPLETE
}

data class VaultArchivePrompt(
    val vaultId: Long,
    val vaultName: String,
    val expenseAmount: Double,
    val vaultBalanceBefore: Double,
    val vaultBalanceAfter: Double,
    val overflowToMainAccount: Double
)
