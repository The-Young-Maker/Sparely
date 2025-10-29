package com.example.sparely.data.local

import com.example.sparely.domain.model.Achievement
import com.example.sparely.domain.model.AutoDepositSchedule
import com.example.sparely.domain.model.CategoryBudget
import com.example.sparely.domain.model.ChallengeMilestone
import com.example.sparely.domain.model.RecurringExpense
import com.example.sparely.domain.model.SavingsAccount
import com.example.sparely.domain.model.SavingsAccountInput
import com.example.sparely.domain.model.SavingsChallenge
import com.example.sparely.domain.model.SavingsPercentages
import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.VaultBalanceAdjustment
import com.example.sparely.domain.model.VaultContribution
import java.time.YearMonth

fun CategoryBudgetEntity.toDomain(): CategoryBudget = CategoryBudget(
    id = id,
    category = category,
    monthlyLimit = monthlyLimit,
    yearMonth = YearMonth.of(year, month),
    isActive = isActive
)

fun CategoryBudget.toEntity(): CategoryBudgetEntity = CategoryBudgetEntity(
    id = id,
    category = category,
    monthlyLimit = monthlyLimit,
    year = yearMonth.year,
    month = yearMonth.monthValue,
    isActive = isActive
)

fun RecurringExpenseEntity.toDomain(): RecurringExpense = RecurringExpense(
    id = id,
    description = description,
    amount = amount,
    category = category,
    frequency = frequency,
    startDate = startDate,
    endDate = endDate,
    lastProcessedDate = lastProcessedDate,
    isActive = isActive,
    autoLog = autoLog,
    reminderDaysBefore = reminderDaysBefore,
    merchantName = merchantName,
    notes = notes,
    includesTax = includesTax,
    deductFromMainAccount = deductFromMainAccount,
    deductedFromVaultId = deductedFromVaultId,
    manualPercentages = if (manualPercentEmergency != null || manualPercentInvest != null || manualPercentFun != null || manualSafeSplit != null) {
        SavingsPercentages(
            emergency = manualPercentEmergency ?: 0.0,
            invest = manualPercentInvest ?: 0.0,
            `fun` = manualPercentFun ?: 0.0,
            safeInvestmentSplit = manualSafeSplit ?: 0.5
        )
    } else null
)

fun RecurringExpense.toEntity(): RecurringExpenseEntity = RecurringExpenseEntity(
    id = id,
    description = description,
    amount = amount,
    category = category,
    frequency = frequency,
    startDate = startDate,
    endDate = endDate,
    lastProcessedDate = lastProcessedDate,
    isActive = isActive,
    autoLog = autoLog,
    reminderDaysBefore = reminderDaysBefore,
    merchantName = merchantName,
    notes = notes,
    includesTax = includesTax,
    deductFromMainAccount = deductFromMainAccount,
    deductedFromVaultId = deductedFromVaultId,
    manualPercentEmergency = manualPercentages?.emergency,
    manualPercentInvest = manualPercentages?.invest,
    manualPercentFun = manualPercentages?.`fun`,
    manualSafeSplit = manualPercentages?.safeInvestmentSplit
)

fun ChallengeMilestoneEntity.toDomain(): ChallengeMilestone = ChallengeMilestone(
    description = description,
    targetAmount = targetAmount,
    isAchieved = isAchieved,
    achievedDate = achievedDate,
    rewardPoints = rewardPoints
)

fun ChallengeMilestone.toEntity(challengeId: Long): ChallengeMilestoneEntity = ChallengeMilestoneEntity(
    challengeId = challengeId,
    description = description,
    targetAmount = targetAmount,
    isAchieved = isAchieved,
    achievedDate = achievedDate,
    rewardPoints = rewardPoints
)

fun SavingsChallengeWithMilestones.toDomain(): SavingsChallenge = SavingsChallenge(
    id = challenge.id,
    type = challenge.type,
    title = challenge.title,
    description = challenge.description,
    targetAmount = challenge.targetAmount,
    currentAmount = challenge.currentAmount,
    startDate = challenge.startDate,
    endDate = challenge.endDate,
    isActive = challenge.isActive,
    isCompleted = challenge.isCompleted,
    completedDate = challenge.completedDate,
    streakDays = challenge.streakDays,
    milestones = milestones.map { it.toDomain() }
)

fun SavingsChallenge.toEntity(): Pair<SavingsChallengeEntity, List<ChallengeMilestoneEntity>> {
    val entity = SavingsChallengeEntity(
        id = id,
        type = type,
        title = title,
        description = description,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        startDate = startDate,
        endDate = endDate,
        isActive = isActive,
        isCompleted = isCompleted,
        completedDate = completedDate,
        streakDays = streakDays
    )
    val milestoneEntities = milestones.map { it.toEntity(id) }
    return entity to milestoneEntities
}

fun AchievementEntity.toDomain(): Achievement = Achievement(
    id = id,
    title = title,
    description = description,
    icon = icon,
    earnedDate = earnedDate,
    category = category
)

fun Achievement.toEntity(): AchievementEntity = AchievementEntity(
    id = id,
    title = title,
    description = description,
    icon = icon,
    earnedDate = earnedDate,
    category = category
)

fun SavingsAccountEntity.toDomain(): SavingsAccount = SavingsAccount(
    id = id,
    name = name,
    category = category,
    institution = institution,
    accountNumber = accountNumber,
    currentBalance = currentBalance,
    targetBalance = targetBalance,
    isPrimary = isPrimary,
    reminderFrequencyDays = reminderFrequencyDays,
    reminderEnabled = reminderEnabled,
    syncProvider = syncProvider,
    externalAccountId = externalAccountId,
    lastSyncedAt = lastSyncedAt,
    autoRefreshEnabled = autoRefreshEnabled
)

fun SavingsAccountInput.toEntity(): SavingsAccountEntity = SavingsAccountEntity(
    id = 0L,
    name = name,
    category = category,
    institution = institution,
    accountNumber = accountNumber,
    currentBalance = currentBalance,
    targetBalance = targetBalance,
    isPrimary = isPrimary,
    reminderFrequencyDays = reminderFrequencyDays,
    reminderEnabled = reminderEnabled,
    syncProvider = syncProvider,
    externalAccountId = externalAccountId,
    autoRefreshEnabled = autoRefreshEnabled
)

fun SavingsAccount.toEntity(): SavingsAccountEntity = SavingsAccountEntity(
    id = id,
    name = name,
    category = category,
    institution = institution,
    accountNumber = accountNumber,
    currentBalance = currentBalance,
    targetBalance = targetBalance,
    isPrimary = isPrimary,
    reminderFrequencyDays = reminderFrequencyDays,
    reminderEnabled = reminderEnabled,
    syncProvider = syncProvider,
    externalAccountId = externalAccountId,
    lastSyncedAt = lastSyncedAt,
    autoRefreshEnabled = autoRefreshEnabled
)

fun SmartVaultEntity.toDomain(autoDeposit: AutoDepositSchedule? = null): SmartVault = SmartVault(
    id = id,
    name = name,
    targetAmount = targetAmount,
    currentBalance = currentBalance,
    targetDate = targetDate,
    startDate = startDate,
    endDate = endDate,
    monthlyNeed = monthlyNeed,
    priorityWeight = priorityWeight,
    autoSaveEnabled = autoSaveEnabled,
    priority = priority,
    type = type,
    interestRate = interestRate,
    allocationMode = allocationMode,
    manualAllocationPercent = manualAllocationPercent,
    nextExpectedContribution = nextExpectedContribution,
    lastContributionDate = lastContributionDate,
    autoDepositSchedule = autoDeposit,
    savingTaxRateOverride = savingTaxRateOverride,
    archived = archived,
    accountType = accountType,
    accountNumber = accountNumber,
    accountNotes = accountNotes,
    createdAt = createdAt
)

fun SmartVault.toEntity(): SmartVaultEntity = SmartVaultEntity(
    id = id,
    name = name,
    targetAmount = targetAmount,
    currentBalance = currentBalance,
    targetDate = targetDate,
    startDate = startDate,
    endDate = endDate,
    monthlyNeed = monthlyNeed,
    priorityWeight = priorityWeight,
    autoSaveEnabled = autoSaveEnabled,
    priority = priority,
    type = type,
    interestRate = interestRate,
    allocationMode = allocationMode,
    manualAllocationPercent = manualAllocationPercent,
    nextExpectedContribution = nextExpectedContribution,
    lastContributionDate = lastContributionDate,
    savingTaxRateOverride = savingTaxRateOverride,
    archived = archived,
    accountType = accountType,
    accountNumber = accountNumber,
    accountNotes = accountNotes,
    createdAt = createdAt
)

fun VaultAutoDepositEntity.toDomain(): AutoDepositSchedule = AutoDepositSchedule(
    amount = amount,
    frequency = frequency,
    startDate = startDate,
    endDate = endDate,
    sourceAccountId = sourceAccountId,
    lastExecutionDate = lastExecutionDate
)

fun AutoDepositSchedule.toEntity(vaultId: Long, scheduleId: Long = 0L): VaultAutoDepositEntity = VaultAutoDepositEntity(
    id = scheduleId,
    vaultId = vaultId,
    amount = amount,
    frequency = frequency,
    startDate = startDate,
    endDate = endDate,
    sourceAccountId = sourceAccountId,
    lastExecutionDate = lastExecutionDate,
    active = true
)

fun VaultContributionEntity.toDomain(): VaultContribution = VaultContribution(
    id = id,
    vaultId = vaultId,
    amount = amount,
    date = date,
    source = source,
    note = note,
    reconciled = reconciled
)

fun VaultContribution.toEntity(): VaultContributionEntity = VaultContributionEntity(
    id = id,
    vaultId = vaultId,
    amount = amount,
    date = date,
    source = source,
    note = note,
    reconciled = reconciled
)

fun VaultBalanceAdjustmentEntity.toDomain(): VaultBalanceAdjustment = VaultBalanceAdjustment(
    id = id,
    vaultId = vaultId,
    type = type,
    delta = delta,
    resultingBalance = resultingBalance,
    createdAt = createdAt,
    reason = reason
)

fun VaultBalanceAdjustment.toEntity(): VaultBalanceAdjustmentEntity = VaultBalanceAdjustmentEntity(
    id = id,
    vaultId = vaultId,
    type = type,
    delta = delta,
    resultingBalance = resultingBalance,
    createdAt = createdAt,
    reason = reason
)

fun SmartVaultWithSchedule.toDomain(): SmartVault {
    val schedule = schedules.firstOrNull()?.toDomain()
    return vault.toDomain(schedule)
}

fun MainAccountTransactionEntity.toDomain(): com.example.sparely.domain.model.MainAccountTransaction =
    com.example.sparely.domain.model.MainAccountTransaction(
        id = id,
        type = type,
        amount = amount,
        balanceAfter = balanceAfter,
        timestamp = timestamp,
        description = description,
        relatedExpenseId = relatedExpenseId,
        relatedVaultContributionIds = relatedVaultContributionIds?.split(",")?.mapNotNull { it.toLongOrNull() }
    )

fun com.example.sparely.domain.model.MainAccountTransaction.toEntity(): MainAccountTransactionEntity =
    MainAccountTransactionEntity(
        id = id,
        type = type,
        amount = amount,
        balanceAfter = balanceAfter,
        timestamp = timestamp,
        description = description,
        relatedExpenseId = relatedExpenseId,
        relatedVaultContributionIds = relatedVaultContributionIds?.joinToString(",")
    )
