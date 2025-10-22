package com.example.sparely.domain.model

import java.time.LocalDate

/**
 * Onboarding state tracking.
 */
data class OnboardingState(
    val isCompleted: Boolean = false,
    val currentStep: Int = 0,
    val userName: String? = null,
    val hasSeenWelcome: Boolean = false,
    val hasSetIncome: Boolean = false,
    val hasSetAge: Boolean = false,
    val hasSetRiskLevel: Boolean = false,
    val hasSetPercentages: Boolean = false,
    val hasCreatedFirstGoal: Boolean = false
) {
    val completionProgress: Float =
        listOf(hasSeenWelcome, hasSetIncome, hasSetAge, hasSetRiskLevel, hasSetPercentages)
            .count { it } / 5f
}

/**
 * Onboarding step definition.
 */
data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: String,
    val stepNumber: Int,
    val totalSteps: Int
)

/**
 * Initial user profile setup.
 */
data class UserProfileSetup(
    val name: String?,
    val age: Int,
    val monthlyIncome: Double,
    val riskLevel: RiskLevel,
    val hasDebts: Boolean = false,
    val currentEmergencyFund: Double = 0.0,
    val primaryGoal: String? = null,
    val savingsAccounts: List<SavingsAccountInput> = emptyList(),
    val smartVaults: List<SmartVaultSetup> = emptyList(),
    val transferReminder: TransferReminderPreference? = null,
    val educationStatus: EducationStatus = EducationStatus.OTHER,
    val employmentStatus: EmploymentStatus = EmploymentStatus.EMPLOYED,
    val birthday: LocalDate? = null,
    val joinedDate: LocalDate = LocalDate.now()
)

enum class EducationStatus {
    HIGH_SCHOOL,
    UNIVERSITY,
    GRADUATED,
    OTHER
}

enum class EmploymentStatus {
    STUDENT,
    EMPLOYED,
    SELF_EMPLOYED,
    UNEMPLOYED,
    RETIRED
}

data class SavingsAccountInput(
    val name: String,
    val category: SavingsCategory,
    val institution: String?,
    val accountNumber: String?,
    val currentBalance: Double,
    val targetBalance: Double?,
    val isPrimary: Boolean,
    val reminderFrequencyDays: Int?,
    val reminderEnabled: Boolean,
    val syncProvider: BankSyncProvider? = null,
    val externalAccountId: String? = null,
    val autoRefreshEnabled: Boolean = false
)

data class SmartVaultSetup(
    val name: String,
    val targetAmount: Double,
    val currentBalance: Double = 0.0,
    val targetDate: LocalDate? = null,
    val priority: VaultPriority = VaultPriority.MEDIUM,
    val type: VaultType = VaultType.SHORT_TERM,
    val interestRate: Double? = null,
    val allocationMode: VaultAllocationMode = VaultAllocationMode.DYNAMIC_AUTO,
    val manualAllocationPercent: Double? = null,
    val savingTaxRateOverride: Double? = null
)

fun SmartVaultSetup.toSmartVault(): SmartVault = SmartVault(
    name = name.trim().ifEmpty { "Vault" },
    targetAmount = targetAmount.coerceAtLeast(0.0),
    currentBalance = currentBalance.coerceAtLeast(0.0),
    targetDate = targetDate,
    priority = priority,
    type = type,
    interestRate = interestRate,
    allocationMode = allocationMode,
    manualAllocationPercent = manualAllocationPercent?.coerceIn(0.0, 1.0),
    nextExpectedContribution = null,
    lastContributionDate = null,
    autoDepositSchedule = null,
    savingTaxRateOverride = savingTaxRateOverride?.coerceIn(0.0, 1.0),
    archived = false
)

fun SavingsAccountInput.toSmartVaultSetup(monthlyIncome: Double): SmartVaultSetup {
    val safeIncome = monthlyIncome.coerceAtLeast(0.0)
    val fallbackTarget = when (category) {
        SavingsCategory.EMERGENCY -> maxOf(500.0, safeIncome.takeIf { it > 0 } ?: 1000.0)
        SavingsCategory.INVESTMENT -> (safeIncome * 1.5).takeIf { it > 0 } ?: 1500.0
        SavingsCategory.FUN -> (safeIncome * 0.3).takeIf { it > 0 } ?: 600.0
    }
    val priority = when (category) {
        SavingsCategory.EMERGENCY -> if (isPrimary) VaultPriority.CRITICAL else VaultPriority.HIGH
        SavingsCategory.INVESTMENT -> if (isPrimary) VaultPriority.HIGH else VaultPriority.MEDIUM
        SavingsCategory.FUN -> VaultPriority.MEDIUM
    }
    val type = when (category) {
        SavingsCategory.EMERGENCY -> VaultType.SHORT_TERM
        SavingsCategory.INVESTMENT -> VaultType.LONG_TERM
        SavingsCategory.FUN -> VaultType.SHORT_TERM
    }
    return SmartVaultSetup(
        name = name,
        targetAmount = targetBalance?.takeIf { it > 0.0 } ?: fallbackTarget,
        currentBalance = currentBalance.coerceAtLeast(0.0),
        priority = priority,
        type = type
    )
}
