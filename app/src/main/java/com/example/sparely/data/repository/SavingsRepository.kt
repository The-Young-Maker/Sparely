package com.example.sparely.data.repository

import com.example.sparely.data.local.AchievementDao
import com.example.sparely.data.local.BudgetDao
import com.example.sparely.data.local.ChallengeDao
import com.example.sparely.data.local.ExpenseDao
import com.example.sparely.data.local.ExpenseEntity
import com.example.sparely.data.local.GoalDao
import com.example.sparely.data.local.GoalEntity
import com.example.sparely.data.local.RecurringExpenseDao
import com.example.sparely.data.local.SavingsAccountDao
import com.example.sparely.data.local.SavingsTransferDao
import com.example.sparely.data.local.SavingsTransferEntity
import com.example.sparely.data.local.SmartVaultDao
import com.example.sparely.data.local.VaultAutoDepositEntity
import com.example.sparely.data.local.toDomain
import com.example.sparely.data.local.toEntity
import com.example.sparely.domain.model.BankSyncProvider
import com.example.sparely.domain.model.SavingsAccount
import com.example.sparely.domain.model.Achievement
import com.example.sparely.domain.model.CategoryBudget
import com.example.sparely.domain.model.RecurringExpense
import com.example.sparely.domain.model.SavingsChallenge
import com.example.sparely.domain.model.SavingsAccountInput
import com.example.sparely.domain.model.SavingsCategory
import com.example.sparely.domain.model.SavingsTransfer
import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.VaultContribution
import com.example.sparely.domain.model.VaultContributionSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

class SavingsRepository(
    private val expenseDao: ExpenseDao,
    private val goalDao: GoalDao,
    private val transferDao: SavingsTransferDao,
    private val budgetDao: BudgetDao,
    private val recurringExpenseDao: RecurringExpenseDao,
    private val challengeDao: ChallengeDao,
    private val achievementDao: AchievementDao,
    private val savingsAccountDao: SavingsAccountDao,
    private val smartVaultDao: SmartVaultDao
) {
    fun observeExpenses(): Flow<List<ExpenseEntity>> = expenseDao.observeExpenses()

    fun observeExpensesBetween(from: LocalDate, to: LocalDate): Flow<List<ExpenseEntity>> =
        expenseDao.observeExpensesBetween(from, to)

    suspend fun upsertExpense(entity: ExpenseEntity) {
        expenseDao.upsertExpense(entity)
    }

    suspend fun deleteExpense(entity: ExpenseEntity) {
        expenseDao.deleteExpense(entity)
    }

    suspend fun findExpenseById(id: Long): ExpenseEntity? = expenseDao.findExpenseById(id)

    suspend fun clearExpenses() {
        expenseDao.clearAll()
    }

    fun observeGoals(): Flow<List<GoalEntity>> = goalDao.observeGoals()

    suspend fun upsertGoal(goal: GoalEntity) {
        goalDao.upsertGoal(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.deleteGoal(goal)
    }

    suspend fun findGoalById(id: Long): GoalEntity? = goalDao.findGoalById(id)

    suspend fun clearGoals() {
        goalDao.clearAll()
    }

    fun observeTransfers(): Flow<List<SavingsTransferEntity>> = transferDao.observeTransfers()

    suspend fun logTransfer(entity: SavingsTransferEntity) {
        transferDao.upsert(entity)
        reconcileAccountsForTransfer(entity)
    }

    suspend fun getTransfersForAccount(accountId: Long): List<SavingsTransfer> =
        transferDao.getTransfersForAccount(accountId).map { it.toDomain() }

    suspend fun clearTransfers() {
        transferDao.clearAll()
    }

    fun observeSavingsAccounts(): Flow<List<SavingsAccount>> =
        savingsAccountDao.observeAccounts().map { accounts ->
            accounts.map { it.toDomain() }
        }

    fun observeSmartVaults(): Flow<List<SmartVault>> =
        smartVaultDao.observeActiveVaults().map { rows -> rows.map { it.toDomain() } }

    suspend fun upsertSavingsAccount(account: SavingsAccount) {
        val assignedId = savingsAccountDao.upsert(account.toEntity())
        val resolvedId = if (account.id == 0L) assignedId else account.id
        if (account.isPrimary && resolvedId != 0L) {
            savingsAccountDao.setPrimaryForCategory(account.category, resolvedId)
        }
    }

    suspend fun seedSavingsAccounts(inputs: List<SavingsAccountInput>) {
        savingsAccountDao.clearAll()
        if (inputs.isEmpty()) return
        inputs.forEach { input ->
            val assignedId = savingsAccountDao.upsert(input.toEntity())
            if (input.isPrimary && assignedId != 0L) {
                savingsAccountDao.setPrimaryForCategory(input.category, assignedId)
            }
        }
    }

    suspend fun deleteSavingsAccount(id: Long) {
        savingsAccountDao.deleteById(id)
    }

    suspend fun incrementPrimaryAccountBalance(category: SavingsCategory, amount: Double) {
        if (amount <= 0.0) return
        val accounts = savingsAccountDao.findByCategory(category)
        val primary = accounts.firstOrNull { it.isPrimary } ?: accounts.firstOrNull()
        primary?.let { account ->
            if (!account.isPrimary) {
                savingsAccountDao.setPrimaryForCategory(category, account.id)
            }
            savingsAccountDao.incrementBalance(account.id, amount)
        }
    }

    suspend fun updateLinkedAccount(
        accountId: Long,
        provider: BankSyncProvider?,
        externalAccountId: String?,
        autoRefreshEnabled: Boolean
    ) {
        savingsAccountDao.updateLinkMetadata(accountId, provider, externalAccountId, autoRefreshEnabled)
    }

    suspend fun updateSyncedBalance(accountId: Long, balance: Double, syncedAt: Instant = Instant.now()) {
        savingsAccountDao.updateSyncedBalance(accountId, balance, syncedAt)
    }

    suspend fun upsertSmartVault(vault: SmartVault) {
        val assignedId = smartVaultDao.upsertVault(vault.toEntity())
        val resolvedId = if (vault.id == 0L) assignedId else vault.id
        vault.autoDepositSchedule?.let { schedule ->
            smartVaultDao.attachAutoDeposit(resolvedId, schedule.toEntity(resolvedId))
        }
    }

    suspend fun seedSmartVaults(vaults: List<SmartVault>) {
        smartVaultDao.clearAllVaults()
        if (vaults.isEmpty()) return
        vaults.forEach { vault ->
            upsertSmartVault(vault.copy(id = 0L))
        }
    }

    suspend fun deleteSmartVault(id: Long) {
        smartVaultDao.deleteVault(id)
    }

    suspend fun logVaultContribution(contribution: VaultContribution) {
        val entity = contribution.toEntity()
        smartVaultDao.upsertContribution(entity)
        if (contribution.reconciled) {
            smartVaultDao.incrementVaultBalance(contribution.vaultId, contribution.amount, contribution.date)
        }
    }

    suspend fun logVaultContributions(contributions: List<VaultContribution>) {
        if (contributions.isEmpty()) return
        contributions.forEach { logVaultContribution(it) }
    }
    
    suspend fun getPendingVaultContributions(): List<VaultContribution> =
        smartVaultDao.getPendingContributions().map { it.toDomain() }
    
    suspend fun reconcileVaultContribution(contributionId: Long) {
        val contribution = smartVaultDao.getContributionById(contributionId)
        if (contribution != null && !contribution.reconciled) {
            smartVaultDao.markContributionReconciled(contributionId)
            smartVaultDao.incrementVaultBalance(contribution.vaultId, contribution.amount, contribution.date)
        }
    }

    suspend fun reconcileVaultContributions(contributionIds: List<Long>) {
        contributionIds.forEach { id ->
            reconcileVaultContribution(id)
        }
    }

    suspend fun recordAutoDepositExecution(vaultId: Long, amount: Double, date: LocalDate) {
        val schedule = smartVaultDao.getAutoDepositForVault(vaultId)
        if (schedule != null) {
            smartVaultDao.updateAutoDeposit(schedule.copy(lastExecutionDate = date))
        }
        val contribution = VaultContribution(
            vaultId = vaultId,
            amount = amount,
            date = date,
            source = VaultContributionSource.AUTO_DEPOSIT
        )
        logVaultContribution(contribution)
    }

    suspend fun getVaultContributions(vaultId: Long): List<VaultContribution> =
        smartVaultDao.getContributionsForVault(vaultId).map { it.toDomain() }

    suspend fun setVaultAutoDeposit(vaultId: Long, schedule: VaultAutoDepositEntity?) {
        if (schedule == null) {
            smartVaultDao.removeAutoDepositForVault(vaultId)
        } else {
            smartVaultDao.attachAutoDeposit(vaultId, schedule.copy(vaultId = vaultId))
        }
    }

    suspend fun refreshLinkedAccounts(fetchBalance: suspend (SavingsAccount) -> Double) {
        val linked = savingsAccountDao.getLinkedAccounts().map { it.toDomain() }
        if (linked.isEmpty()) return
        linked.forEach { account ->
            val updatedBalance = fetchBalance(account)
            updateSyncedBalance(account.id, updatedBalance, Instant.now())
        }
    }

    fun observeBudgets(): Flow<List<CategoryBudget>> =
        budgetDao.observeBudgets().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getBudgetsForMonth(year: Int, month: Int): List<CategoryBudget> =
        budgetDao.getBudgetsForMonth(year, month).map { it.toDomain() }

    suspend fun upsertBudget(budget: CategoryBudget) {
        budgetDao.upsert(budget.toEntity())
    }

    suspend fun deleteBudget(id: Long) {
        budgetDao.deleteById(id)
    }

    fun observeRecurringExpenses(): Flow<List<RecurringExpense>> =
        recurringExpenseDao.observeRecurringExpenses().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun upsertRecurringExpense(expense: RecurringExpense) {
        recurringExpenseDao.upsert(expense.toEntity())
    }

    suspend fun deleteRecurringExpense(id: Long) {
        recurringExpenseDao.deleteById(id)
    }

    suspend fun updateRecurringExpenseProcessed(id: Long, processedDate: LocalDate?) {
        recurringExpenseDao.updateLastProcessedDate(id, processedDate)
    }

    fun observeChallenges(): Flow<List<SavingsChallenge>> =
        challengeDao.observeChallenges().map { rows ->
            rows.map { it.toDomain() }
        }

    suspend fun upsertSavingsChallenge(challenge: SavingsChallenge) {
        val (entity, milestoneEntities) = challenge.toEntity()
        val challengeId = if (entity.id == 0L) {
            challengeDao.upsertChallenge(entity.copy(id = 0))
        } else {
            challengeDao.upsertChallenge(entity)
        }
        val resolvedId = if (entity.id == 0L) challengeId else entity.id
        challengeDao.deleteMilestonesForChallenge(resolvedId)
        if (milestoneEntities.isNotEmpty()) {
            val adjusted = milestoneEntities.map { it.copy(challengeId = resolvedId) }
            challengeDao.upsertMilestones(adjusted)
        }
    }

    suspend fun deleteSavingsChallenge(id: Long) {
        challengeDao.deleteMilestonesForChallenge(id)
        challengeDao.deleteChallengeById(id)
    }

    fun observeAchievements(): Flow<List<Achievement>> =
        achievementDao.observeAchievements().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun upsertAchievement(achievement: Achievement) {
        achievementDao.upsert(achievement.toEntity())
    }

    suspend fun upsertAchievements(achievements: List<Achievement>) {
        if (achievements.isEmpty()) return
        achievementDao.upsertAll(achievements.map { it.toEntity() })
    }

    suspend fun clearAchievements() {
        achievementDao.clear()
    }

    private suspend fun reconcileAccountsForTransfer(entity: SavingsTransferEntity) {
        if (entity.amount <= 0.0) return
        entity.sourceAccountId?.let { sourceId ->
            savingsAccountDao.incrementBalance(sourceId, -entity.amount)
        }
        val destinationId = entity.destinationAccountId
        if (destinationId != null) {
            savingsAccountDao.incrementBalance(destinationId, entity.amount)
        } else {
            incrementPrimaryAccountBalance(entity.category, entity.amount)
        }
    }
}
