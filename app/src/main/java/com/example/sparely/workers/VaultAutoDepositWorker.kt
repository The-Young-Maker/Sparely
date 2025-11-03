package com.example.sparely.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sparely.data.local.SparelyDatabase
import com.example.sparely.data.local.toDomain
import com.example.sparely.data.repository.SavingsRepository
import com.example.sparely.domain.model.AutoDepositFrequency
import com.example.sparely.domain.model.VaultContribution
import com.example.sparely.domain.model.VaultContributionSource
import com.example.sparely.notifications.NotificationHelper
import java.time.LocalDate
import java.time.temporal.ChronoUnit
// No additional imports required here

/**
 * Background worker that processes scheduled auto-deposits for Smart Vaults.
 * Runs daily to check for due deposits and creates pending contributions.
 */
class VaultAutoDepositWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = SparelyDatabase.getInstance(applicationContext)
            val savingsRepository = SavingsRepository(
                expenseDao = database.expenseDao(),
                transferDao = database.transferDao(),
                budgetDao = database.budgetDao(),
                recurringExpenseDao = database.recurringExpenseDao(),
                challengeDao = database.challengeDao(),
                achievementDao = database.achievementDao(),
                savingsAccountDao = database.savingsAccountDao(),
                smartVaultDao = database.smartVaultDao(),
                mainAccountDao = database.mainAccountDao(),
                frozenFundDao = database.frozenFundDao()
            )

            val today = LocalDate.now()
            val dueDeposits = findDueAutoDeposits(database, today)

            if (dueDeposits.isNotEmpty()) {
                dueDeposits.forEach { deposit ->
                    val scheduleEntity = database.smartVaultDao().getAutoDepositForVault(deposit.vaultId)
                    val executeAutomatically = scheduleEntity?.executeAutomatically ?: false

                    if (executeAutomatically) {
                        // Execute immediately: log contribution as reconciled and insert a main account transaction
                        val contribution = VaultContribution(
                            vaultId = deposit.vaultId,
                            amount = deposit.amount,
                            date = today,
                            source = VaultContributionSource.AUTO_DEPOSIT,
                            note = "Auto deposit - ${deposit.frequency.displayName}",
                            reconciled = true
                        )
                        savingsRepository.logVaultContribution(contribution)
                        // Record last execution and update vault balance
                        savingsRepository.recordAutoDepositExecution(deposit.vaultId, deposit.amount, today)

                        // Insert a corresponding main account transaction decrementing balance
                        val currentBalance = savingsRepository.getLatestMainAccountBalance()
                        val vaultTransaction = com.example.sparely.domain.model.MainAccountTransaction(
                            type = com.example.sparely.data.local.MainAccountTransactionType.VAULT_CONTRIBUTION,
                            amount = deposit.amount,
                            balanceAfter = (currentBalance - deposit.amount).coerceAtLeast(0.0),
                            timestamp = java.time.LocalDateTime.now(),
                            description = "Auto deposit to vault ${deposit.vaultId}",
                            relatedExpenseId = null,
                            relatedVaultContributionIds = listOf() // will be empty; mapping stored elsewhere
                        )
                        savingsRepository.insertMainAccountTransaction(vaultTransaction)
                    } else {
                        // Create pending contribution (not reconciled) and freeze funds logically by inserting a pending main transaction
                        val contribution = VaultContribution(
                            vaultId = deposit.vaultId,
                            amount = deposit.amount,
                            date = today,
                            source = VaultContributionSource.AUTO_DEPOSIT,
                            note = "Auto deposit - ${deposit.frequency.displayName}",
                            reconciled = false
                        )
                        val contributionId = savingsRepository.logVaultContribution(contribution)
                        // Update last execution date so we don't recreate repeatedly
                        savingsRepository.recordAutoDepositExecution(deposit.vaultId, deposit.amount, today)
                        // Mark funds as frozen (do not remove from canonical main account balance yet)
                        savingsRepository.insertFrozenFund(
                            pendingType = "VAULT_CONTRIBUTION",
                            pendingId = contributionId,
                            amount = contribution.amount,
                            description = "Pending auto-deposit for vault ${deposit.vaultId}"
                        )
                    }
                }

                // Show notification to user
                NotificationHelper.showAutoDepositReminder(
                    applicationContext,
                    dueDeposits.size,
                    dueDeposits.sumOf { it.amount }
                )
            }

            // Process recurring expenses due today
            val recurringEntities = database.recurringExpenseDao().getAll()
            val dueRecurring = recurringEntities.mapNotNull { entity ->
                if (!entity.isActive) return@mapNotNull null
                val last = entity.lastProcessedDate ?: entity.startDate.minusDays(1)
                val daysSince = ChronoUnit.DAYS.between(last, today)
                val isDue = when (entity.frequency) {
                    com.example.sparely.domain.model.RecurringFrequency.DAILY -> daysSince >= 1
                    com.example.sparely.domain.model.RecurringFrequency.WEEKLY -> daysSince >= 7
                    com.example.sparely.domain.model.RecurringFrequency.BIWEEKLY -> daysSince >= 14
                    com.example.sparely.domain.model.RecurringFrequency.MONTHLY -> {
                        val lastMonth = last.monthValue
                        val currentMonth = today.monthValue
                        val lastYear = last.year
                        val currentYear = today.year
                        (currentYear > lastYear) || (currentYear == lastYear && currentMonth > lastMonth)
                    }
                    com.example.sparely.domain.model.RecurringFrequency.QUARTERLY -> daysSince >= 90
                    com.example.sparely.domain.model.RecurringFrequency.YEARLY -> daysSince >= 365
                }
                if (isDue) entity else null
            }

            if (dueRecurring.isNotEmpty()) {
                dueRecurring.forEach { re ->
                    val executeAuto = re.executeAutomatically
                    if (executeAuto) {
                        // Create expense immediately. ExpenseEntity requires many fields; fill defaults where not available from recurring entry.
                        val expenseEntity = com.example.sparely.data.local.ExpenseEntity(
                            id = 0L,
                            description = re.description,
                            amount = re.amount,
                            category = re.category,
                            date = today,
                            includesTax = re.includesTax,
                            emergencyAmount = 0.0,
                            investmentAmount = 0.0,
                            funAmount = 0.0,
                            safeInvestmentAmount = 0.0,
                            highRiskInvestmentAmount = 0.0,
                            autoRecommended = false,
                            appliedPercentEmergency = 0.0,
                            appliedPercentInvest = 0.0,
                            appliedPercentFun = 0.0,
                            appliedSafeSplit = 0.5,
                            riskLevelUsed = com.example.sparely.domain.model.RiskLevel.BALANCED,
                            deductedFromVaultId = re.deductedFromVaultId
                        )
                        savingsRepository.upsertExpense(expenseEntity)

                        // insert main account transaction to reflect deduction
                        val currentBalance = savingsRepository.getLatestMainAccountBalance()
                        val trans = com.example.sparely.domain.model.MainAccountTransaction(
                            type = com.example.sparely.data.local.MainAccountTransactionType.EXPENSE,
                            amount = re.amount,
                            balanceAfter = (currentBalance - re.amount).coerceAtLeast(0.0),
                            timestamp = java.time.LocalDateTime.now(),
                            description = "Auto-logged recurring expense: ${re.description}",
                            relatedExpenseId = null,
                            relatedVaultContributionIds = listOf()
                        )
                        savingsRepository.insertMainAccountTransaction(trans)
                        // mark processed
                        savingsRepository.updateRecurringExpenseProcessed(re.id, today)
                    } else {
                        // Create a logical freeze by inserting a main account transaction that reduces the available balance.
                        val currentBalance = savingsRepository.getLatestMainAccountBalance()
                        // Mark funds as frozen for this pending recurring payment
                        val pendingExpenseId = 0L // no expense row created yet; use recurring id as reference
                        savingsRepository.insertFrozenFund(
                            pendingType = "RECURRING_PAYMENT",
                            pendingId = re.id,
                            amount = re.amount,
                            description = "Pending recurring payment: ${re.description}"
                        )
                        // mark processed so it doesn't create repeatedly; reconciliation/approval flows can be added later
                        savingsRepository.updateRecurringExpenseProcessed(re.id, today)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun findDueAutoDeposits(
        database: SparelyDatabase,
        today: LocalDate
    ): List<DueDeposit> {
        val dao = database.smartVaultDao()
        val allVaultsWithSchedules = dao.observeActiveVaults()
        
        // Since we're in a suspend function, we need to get the current value
        // In a real scenario, you'd query directly from DAO
        val activeSchedules = database.smartVaultDao().getActiveAutoDepositSchedules()
        
        return activeSchedules.mapNotNull { scheduleEntity ->
            val schedule = scheduleEntity.toDomain()
            
            // Check if schedule is active and not ended
            if (!scheduleEntity.active) return@mapNotNull null
            if (schedule.endDate != null && !today.isBefore(schedule.endDate)) return@mapNotNull null
            if (today.isBefore(schedule.startDate)) return@mapNotNull null
            
            // Check if deposit is due
            val lastExecution = schedule.lastExecutionDate ?: schedule.startDate.minusDays(1)
            val daysSinceLastExecution = ChronoUnit.DAYS.between(lastExecution, today)
            
            val isDue = when (schedule.frequency) {
                AutoDepositFrequency.WEEKLY -> daysSinceLastExecution >= 7
                AutoDepositFrequency.BIWEEKLY -> daysSinceLastExecution >= 14
                AutoDepositFrequency.MONTHLY -> {
                    // Check if we're in a new month since last execution
                    val lastMonth = lastExecution.monthValue
                    val currentMonth = today.monthValue
                    val lastYear = lastExecution.year
                    val currentYear = today.year
                    
                    (currentYear > lastYear) || (currentYear == lastYear && currentMonth > lastMonth)
                }
            }
            
            if (isDue) {
                DueDeposit(
                    vaultId = scheduleEntity.vaultId,
                    amount = schedule.amount,
                    frequency = schedule.frequency
                )
            } else {
                null
            }
        }
    }

    private data class DueDeposit(
        val vaultId: Long,
        val amount: Double,
        val frequency: AutoDepositFrequency
    )
}

private val AutoDepositFrequency.displayName: String
    get() = when (this) {
        AutoDepositFrequency.WEEKLY -> "Weekly"
        AutoDepositFrequency.BIWEEKLY -> "Bi-weekly"
        AutoDepositFrequency.MONTHLY -> "Monthly"
    }
