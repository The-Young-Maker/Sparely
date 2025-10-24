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
                goalDao = database.goalDao(),
                transferDao = database.transferDao(),
                budgetDao = database.budgetDao(),
                recurringExpenseDao = database.recurringExpenseDao(),
                challengeDao = database.challengeDao(),
                achievementDao = database.achievementDao(),
                savingsAccountDao = database.savingsAccountDao(),
                smartVaultDao = database.smartVaultDao(),
                mainAccountDao = database.mainAccountDao()
            )

            val today = LocalDate.now()
            val dueDeposits = findDueAutoDeposits(database, today)

            if (dueDeposits.isNotEmpty()) {
                dueDeposits.forEach { deposit ->
                    // Create pending contribution (not reconciled)
                    val contribution = VaultContribution(
                        vaultId = deposit.vaultId,
                        amount = deposit.amount,
                        date = today,
                        source = VaultContributionSource.AUTO_DEPOSIT,
                        note = "Auto deposit - ${deposit.frequency.displayName}",
                        reconciled = false
                    )
                    savingsRepository.logVaultContribution(contribution)
                    
                    // Update last execution date
                    savingsRepository.recordAutoDepositExecution(deposit.vaultId, deposit.amount, today)
                }

                // Show notification to user
                NotificationHelper.showAutoDepositReminder(
                    applicationContext,
                    dueDeposits.size,
                    dueDeposits.sumOf { it.amount }
                )
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
