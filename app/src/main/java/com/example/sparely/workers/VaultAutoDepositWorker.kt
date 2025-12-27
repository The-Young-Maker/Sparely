package com.example.sparely.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sparely.data.local.SparelyDatabase
import com.example.sparely.data.local.toDomain
import com.example.sparely.data.repository.SavingsRepository
import com.example.sparely.domain.model.RecurringFrequency
import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.VaultSchedule
import com.example.sparely.domain.model.VaultScheduleType
import com.example.sparely.domain.model.VaultTransferDirection
import com.example.sparely.notifications.NotificationHelper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Background worker that evaluates advanced vault schedules and recurring expenses.
 */
class VaultAutoDepositWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = SparelyDatabase.getInstance(applicationContext)
            val preferencesRepository = com.example.sparely.data.preferences.UserPreferencesRepository(applicationContext)
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
                frozenFundDao = database.frozenFundDao(),
                allocationHistoryDao = database.allocationHistoryDao(),
                preferencesRepository = preferencesRepository
            )

            val now = LocalDateTime.now()
            val today = now.toLocalDate()

            val scheduleEntities = database.smartVaultDao().getEnabledSchedules()
            val schedules = scheduleEntities.map { it.toDomain() }

            var executedCount = 0
            var executedTotal = 0.0

            schedules.forEach { schedule ->
                if (!isScheduleDue(schedule, today, now)) return@forEach

                val vault = savingsRepository.getSmartVaultById(schedule.vaultId)
                val amount = computeTransferAmount(schedule, savingsRepository, vault)
                if (amount <= 0.0) return@forEach

                val vaultName = vault?.name ?: "Vault ${schedule.vaultId}"
                val note = buildTransferNote(schedule, amount, vaultName)

                if (schedule.notifyBefore) {
                    NotificationHelper.showVaultScheduleNotificationBefore(
                        context = applicationContext,
                        vaultName = vaultName,
                        amount = amount,
                        schedule = schedule
                    )
                }

                val executed = savingsRepository.executeVaultTransfer(schedule, amount, now, note)
                if (executed) {
                    executedCount += 1
                    executedTotal += amount

                    val nextRun = computeNextRun(schedule, today)
                    savingsRepository.recordScheduleExecution(schedule.id, schedule.vaultId, amount, now, nextRun)

                    if (schedule.notifyAfter) {
                        NotificationHelper.showVaultScheduleNotificationAfter(
                            context = applicationContext,
                            vaultName = vaultName,
                            amount = amount,
                            schedule = schedule
                        )
                    }
                } else {
                    if (schedule.notifyOnFailure) {
                        NotificationHelper.showVaultScheduleNotificationFailure(
                            context = applicationContext,
                            vaultName = vaultName,
                            amount = amount,
                            schedule = schedule
                        )
                    }
                }
            }

            if (executedCount > 0) {
                NotificationHelper.showVaultScheduleSummary(
                    applicationContext,
                    executedCount,
                    executedTotal
                )
            }

            processRecurringExpenses(database, savingsRepository, today)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun processRecurringExpenses(
        database: SparelyDatabase,
        repository: SavingsRepository,
        today: LocalDate
    ) {
        val recurringEntities = database.recurringExpenseDao().getAll()
        val dueRecurring = recurringEntities.mapNotNull { entity ->
            if (!entity.isActive) return@mapNotNull null
            val last = entity.lastProcessedDate ?: entity.startDate.minusDays(1)
            val daysSince = ChronoUnit.DAYS.between(last, today)
            val isDue = when (entity.frequency) {
                RecurringFrequency.DAILY -> daysSince >= 1
                RecurringFrequency.WEEKLY -> daysSince >= 7
                RecurringFrequency.BIWEEKLY -> daysSince >= 14
                RecurringFrequency.MONTHLY -> {
                    // Check if at least one month has passed
                    val lastMonth = last.monthValue
                    val currentMonth = today.monthValue
                    val lastYear = last.year
                    val currentYear = today.year
                    (currentYear > lastYear) || (currentYear == lastYear && currentMonth > lastMonth)
                }
                RecurringFrequency.QUARTERLY -> {
                    // Check if at least 3 months have passed
                    val monthsDiff = (today.year - last.year) * 12 + (today.monthValue - last.monthValue)
                    monthsDiff >= 3
                }
                RecurringFrequency.YEARLY -> {
                    // Check if at least one year has passed
                    today.year > last.year
                }
            }
            if (isDue) entity else null
        }

        if (dueRecurring.isEmpty()) return

        dueRecurring.forEach { re ->
            val executeAuto = re.executeAutomatically
            if (executeAuto) {
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
                repository.upsertExpense(expenseEntity)

                val currentBalance = repository.getLatestMainAccountBalance()
                val trans = com.example.sparely.domain.model.MainAccountTransaction(
                    type = com.example.sparely.data.local.MainAccountTransactionType.EXPENSE,
                    amount = re.amount,
                    balanceAfter = (currentBalance - re.amount).coerceAtLeast(0.0),
                    timestamp = LocalDateTime.now(),
                    description = "Auto-logged recurring expense: ${re.description}",
                    relatedExpenseId = null,
                    relatedVaultContributionIds = listOf()
                )
                repository.insertMainAccountTransaction(trans)
                repository.updateRecurringExpenseProcessed(re.id, today)
            } else {
                repository.insertFrozenFund(
                    pendingType = "RECURRING_PAYMENT",
                    pendingId = re.id,
                    amount = re.amount,
                    description = "Pending recurring payment: ${re.description}"
                )
                repository.updateRecurringExpenseProcessed(re.id, today)
            }
        }
    }

    private fun isScheduleDue(schedule: VaultSchedule, today: LocalDate, now: LocalDateTime): Boolean {
        if (!schedule.enabled) return false
        val lastRunDate = schedule.lastRunAt?.toLocalDate()
        if (lastRunDate != null && lastRunDate == today) return false

        schedule.nextRunAt?.let { nextRun ->
            return !nextRun.isAfter(now)
        }

        return when (schedule.type) {
            VaultScheduleType.SPECIFIC_DATE -> {
                val target = schedule.dateValue ?: return false
                if (!schedule.repeatAnnually) {
                    target == today && lastRunDate == null
                } else {
                    val candidate = adjustDateForYear(target, today.year)
                    val matchesToday = candidate == today
                    matchesToday && (lastRunDate == null || lastRunDate.year < today.year)
                }
            }
            VaultScheduleType.DAY_OF_MONTH -> {
                val desiredDay = schedule.dayOfMonth ?: today.dayOfMonth
                val monthLength = YearMonth.from(today).lengthOfMonth()
                val actualDay = min(desiredDay, monthLength)
                if (today.dayOfMonth != actualDay) return false
                if (lastRunDate == null) return true
                lastRunDate.year != today.year || lastRunDate.monthValue != today.monthValue
            }
            VaultScheduleType.DAY_OF_WEEK -> {
                val desired = schedule.dayOfWeek ?: today.dayOfWeek.value
                val desiredDow = DayOfWeek.of(((desired - 1) % 7) + 1)
                if (today.dayOfWeek != desiredDow) return false
                val interval = schedule.weekInterval?.takeIf { it > 0 } ?: 1
                if (lastRunDate == null) return true
                val weeksSince = ChronoUnit.WEEKS.between(lastRunDate, today)
                weeksSince >= interval
            }
        }
    }

    private fun computeNextRun(schedule: VaultSchedule, runDate: LocalDate): LocalDateTime? {
        return when (schedule.type) {
            VaultScheduleType.SPECIFIC_DATE -> {
                if (!schedule.repeatAnnually) return null
                val base = schedule.dateValue ?: return null
                val nextYearCandidate = adjustDateForYear(base, runDate.year)
                val nextDate = if (nextYearCandidate.isAfter(runDate)) {
                    nextYearCandidate
                } else {
                    adjustDateForYear(base, runDate.year + 1)
                }
                nextDate.atStartOfDay()
            }
            VaultScheduleType.DAY_OF_MONTH -> {
                val desiredDay = schedule.dayOfMonth ?: runDate.dayOfMonth
                val nextMonth = YearMonth.from(runDate).plusMonths(1)
                val day = min(desiredDay, nextMonth.lengthOfMonth())
                LocalDate.of(nextMonth.year, nextMonth.month, day).atStartOfDay()
            }
            VaultScheduleType.DAY_OF_WEEK -> {
                val interval = schedule.weekInterval?.takeIf { it > 0 } ?: 1
                val desired = schedule.dayOfWeek ?: runDate.dayOfWeek.value
                val desiredDow = DayOfWeek.of(((desired - 1) % 7) + 1)
                val nextBase = runDate.plusWeeks(interval.toLong())
                val nextDate = nextBase.with(TemporalAdjusters.nextOrSame(desiredDow))
                nextDate.atStartOfDay()
            }
        }
    }

    private fun adjustDateForYear(base: LocalDate, year: Int): LocalDate {
        val yearMonth = YearMonth.of(year, base.monthValue)
        val day = min(base.dayOfMonth, yearMonth.lengthOfMonth())
        return LocalDate.of(year, base.monthValue, day)
    }

    private suspend fun computeTransferAmount(
        schedule: VaultSchedule,
        repository: SavingsRepository,
        vault: SmartVault?
    ): Double {
        schedule.amount?.takeIf { it > 0.0 }?.let { return it.roundCurrency() }

        val percent = schedule.percentage ?: return 0.0
        val base = when (schedule.direction) {
            VaultTransferDirection.MAIN_TO_VAULT -> repository.getLatestMainAccountBalance()
            VaultTransferDirection.VAULT_TO_MAIN -> vault?.currentBalance ?: 0.0
        }
        if (base <= 0.0) return 0.0
        return (base * percent).roundCurrency()
    }

    private fun Double.roundCurrency(): Double = (this * 100.0).roundToInt() / 100.0

    private fun buildTransferNote(schedule: VaultSchedule, amount: Double, vaultName: String): String {
        val formattedAmount = NotificationHelper.formatAmount(amount)
        val descriptor = when (schedule.type) {
            VaultScheduleType.SPECIFIC_DATE -> "specific date"
            VaultScheduleType.DAY_OF_MONTH -> "day-of-month"
            VaultScheduleType.DAY_OF_WEEK -> "weekly"
        }
        val direction = when (schedule.direction) {
            VaultTransferDirection.MAIN_TO_VAULT -> "to"
            VaultTransferDirection.VAULT_TO_MAIN -> "from"
        }
        return "Scheduled $descriptor transfer $direction $vaultName ($formattedAmount)"
    }
}
