package com.example.sparely.data.local

import com.example.sparely.domain.model.Achievement
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
import com.example.sparely.domain.model.VaultSchedule
import com.example.sparely.domain.model.Expense
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
    executeAutomatically = executeAutomatically,
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
    executeAutomatically = executeAutomatically,
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

fun SmartVaultEntity.toDomain(schedules: List<VaultSchedule> = emptyList()): SmartVault = SmartVault(
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
    allowAutoIncome = allowAutoIncome,
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
    createdAt = createdAt,
    defaultManualDepositDeductFromMain = defaultManualDepositDeductFromMain,
    defaultManualWithdrawalCreditMain = defaultManualWithdrawalCreditMain,
    schedules = schedules,
    iconName = iconName
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
    allowAutoIncome = allowAutoIncome,
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
    createdAt = createdAt,
    defaultManualDepositDeductFromMain = defaultManualDepositDeductFromMain,
    defaultManualWithdrawalCreditMain = defaultManualWithdrawalCreditMain,
    iconName = iconName
)

fun VaultScheduleEntity.toDomain(): VaultSchedule = VaultSchedule(
    id = id,
    vaultId = vaultId,
    type = type,
    amount = amount,
    percentage = percentage,
    direction = direction,
    dateValue = dateValue,
    repeatAnnually = repeatAnnually,
    dayOfMonth = dayOfMonth,
    dayOfWeek = dayOfWeek,
    weekInterval = weekInterval,
    onlyIfBalanceAvailable = onlyIfBalanceAvailable,
    notifyBefore = notifyBefore,
    notifyAfter = notifyAfter,
    notifyOnFailure = notifyOnFailure,
    nextRunAt = nextRunAt,
    lastRunAt = lastRunAt,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun VaultSchedule.toEntity(): VaultScheduleEntity = VaultScheduleEntity(
    id = id,
    vaultId = vaultId,
    type = type,
    amount = amount,
    percentage = percentage,
    direction = direction,
    dateValue = dateValue,
    repeatAnnually = repeatAnnually,
    dayOfMonth = dayOfMonth,
    dayOfWeek = dayOfWeek,
    weekInterval = weekInterval,
    onlyIfBalanceAvailable = onlyIfBalanceAvailable,
    notifyBefore = notifyBefore,
    notifyAfter = notifyAfter,
    notifyOnFailure = notifyOnFailure,
    nextRunAt = nextRunAt,
    lastRunAt = lastRunAt,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt
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

fun SmartVaultWithSchedules.toDomain(): SmartVault {
    val schedules = schedules.map { it.toDomain() }
    return vault.toDomain(schedules)
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

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    description = description,
    amount = amount,
    category = category,
    date = date,
    includesTax = includesTax,
    allocation = com.example.sparely.domain.model.AllocationBreakdown(
        emergencyAmount = emergencyAmount,
        investmentAmount = investmentAmount,
        funAmount = funAmount,
        safeInvestmentAmount = safeInvestmentAmount,
        highRiskInvestmentAmount = highRiskInvestmentAmount
    ),
    appliedPercentages = com.example.sparely.domain.model.SavingsPercentages(
        emergency = appliedPercentEmergency,
        invest = appliedPercentInvest,
        `fun` = appliedPercentFun,
        safeInvestmentSplit = appliedSafeSplit
    ),
    autoRecommended = autoRecommended,
    riskLevelUsed = riskLevelUsed,
    deductedFromVaultId = deductedFromVaultId
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    description = description,
    amount = amount,
    category = category,
    date = date,
    includesTax = includesTax,
    emergencyAmount = allocation.emergencyAmount,
    investmentAmount = allocation.investmentAmount,
    funAmount = allocation.funAmount,
    safeInvestmentAmount = allocation.safeInvestmentAmount,
    highRiskInvestmentAmount = allocation.highRiskInvestmentAmount,
    autoRecommended = autoRecommended,
    appliedPercentEmergency = appliedPercentages.emergency,
    appliedPercentInvest = appliedPercentages.invest,
    appliedPercentFun = appliedPercentages.`fun`,
    appliedSafeSplit = appliedPercentages.safeInvestmentSplit,
    riskLevelUsed = riskLevelUsed,
    deductedFromVaultId = deductedFromVaultId
)
