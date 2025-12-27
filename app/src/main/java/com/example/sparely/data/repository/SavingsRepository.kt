package com.example.sparely.data.repository

import com.example.sparely.data.local.AchievementDao
import com.example.sparely.data.local.BudgetDao
import com.example.sparely.data.local.ChallengeDao
import com.example.sparely.data.local.ExpenseDao
import com.example.sparely.data.local.ExpenseEntity
import com.example.sparely.data.local.RecurringExpenseDao
import com.example.sparely.data.local.SavingsAccountDao
import com.example.sparely.data.local.SavingsTransferDao
import com.example.sparely.data.local.SavingsTransferEntity
import com.example.sparely.data.local.SmartVaultDao
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
import com.example.sparely.domain.model.VaultBalanceAdjustment
import com.example.sparely.domain.model.VaultContribution
import com.example.sparely.domain.model.VaultContributionSource
import com.example.sparely.domain.model.VaultAdjustmentType
import com.example.sparely.domain.model.VaultSchedule
import com.example.sparely.domain.model.VaultScheduleType
import com.example.sparely.domain.model.VaultTransferDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class SavingsRepository(
    private val expenseDao: ExpenseDao,
    private val transferDao: SavingsTransferDao,
    private val budgetDao: BudgetDao,
    private val recurringExpenseDao: RecurringExpenseDao,
    private val challengeDao: ChallengeDao,
    private val achievementDao: AchievementDao,
    private val savingsAccountDao: SavingsAccountDao,
    private val smartVaultDao: SmartVaultDao,
    private val mainAccountDao: com.example.sparely.data.local.MainAccountDao,
    private val frozenFundDao: com.example.sparely.data.local.FrozenFundDao,
    private val allocationHistoryDao: com.example.sparely.data.local.AllocationHistoryDao,
    private val preferencesRepository: com.example.sparely.data.preferences.UserPreferencesRepository
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

    suspend fun clearSmartVaults() {
        smartVaultDao.clearAllVaults()
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
        if (savingsAccountDao.observeAccounts().first().isNotEmpty()) return
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
        syncVaultSchedules(resolvedId, vault.schedules)
    }

    suspend fun seedSmartVaults(vaults: List<SmartVault>) {
        if (smartVaultDao.observeActiveVaults().first().isNotEmpty()) return
        if (vaults.isEmpty()) return
        vaults.forEach { vault ->
            upsertSmartVault(vault.copy(id = 0L))
        }
    }

    suspend fun deleteSmartVault(id: Long) {
        smartVaultDao.deleteVault(id)
    }

    suspend fun logVaultContribution(contribution: VaultContribution): Long {
        val entity = contribution.toEntity()
        val id = smartVaultDao.upsertContribution(entity)
        if (contribution.reconciled) {
            smartVaultDao.incrementVaultBalance(contribution.vaultId, contribution.amount, contribution.date)
        }
        return id
    }

    suspend fun logVaultContributions(contributions: List<VaultContribution>): List<Long> {
        if (contributions.isEmpty()) return emptyList()
        return contributions.map { logVaultContribution(it) }
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

    /**
     * Approve a pending contribution: reconcile it and remove any frozen funds associated with it.
     */
    suspend fun approvePendingContribution(contributionId: Long) {
        reconcileVaultContribution(contributionId)
        removeFrozenForPending("VAULT_CONTRIBUTION", contributionId)
    }

    /**
     * Cancel a pending contribution: delete the pending contribution record and remove frozen funds.
     */
    suspend fun cancelPendingContribution(contributionId: Long) {
        val contribution = smartVaultDao.getContributionById(contributionId) ?: return
        // delete the pending contribution row
        smartVaultDao.deleteContribution(contributionId)
        // remove the frozen record(s) associated with this pending contribution
        removeFrozenForPending("VAULT_CONTRIBUTION", contributionId)
    }

    suspend fun getVaultContributions(vaultId: Long): List<VaultContribution> =
        smartVaultDao.getContributionsForVault(vaultId).map { it.toDomain() }

    suspend fun getVaultAdjustments(vaultId: Long): List<VaultBalanceAdjustment> =
        smartVaultDao.getAdjustmentsForVault(vaultId).map { it.toDomain() }

    suspend fun depositToVault(vaultId: Long, amount: Double, reason: String?, adjustMainAccount: Boolean) {
        if (amount <= 0.0) return
        val vault = smartVaultDao.getVaultById(vaultId) ?: return
        val sanitizedAmount = amount.coerceAtLeast(0.0)
        val newBalance = vault.currentBalance + sanitizedAmount
        recordVaultBalanceAdjustment(
            vaultId = vaultId,
            previousBalance = vault.currentBalance,
            newBalance = newBalance,
            type = VaultAdjustmentType.MANUAL_DEPOSIT,
            reason = reason
        )

        if (adjustMainAccount) {
            val currentBalance = getLatestMainAccountBalance()
            val transaction = com.example.sparely.domain.model.MainAccountTransaction(
                type = com.example.sparely.data.local.MainAccountTransactionType.WITHDRAWAL,
                amount = sanitizedAmount,
                balanceAfter = (currentBalance - sanitizedAmount).coerceAtLeast(0.0),
                timestamp = java.time.LocalDateTime.now(),
                description = reason?.take(100) ?: "Manual vault deposit"
            )
            insertMainAccountTransaction(transaction)
        }
    }

    suspend fun deductFromVault(vaultId: Long, amount: Double, reason: String?, creditMainAccount: Boolean) {
        if (amount <= 0.0) return
        val vault = smartVaultDao.getVaultById(vaultId) ?: return
        val sanitizedAmount = amount.coerceAtLeast(0.0)
        val newBalance = (vault.currentBalance - sanitizedAmount).coerceAtLeast(0.0)
        recordVaultBalanceAdjustment(
            vaultId = vaultId,
            previousBalance = vault.currentBalance,
            newBalance = newBalance,
            type = VaultAdjustmentType.MANUAL_DEDUCTION,
            reason = reason
        )

        if (creditMainAccount) {
            val currentBalance = getLatestMainAccountBalance()
            val transaction = com.example.sparely.domain.model.MainAccountTransaction(
                type = com.example.sparely.data.local.MainAccountTransactionType.DEPOSIT,
                amount = sanitizedAmount,
                balanceAfter = currentBalance + sanitizedAmount,
                timestamp = java.time.LocalDateTime.now(),
                description = reason?.take(100) ?: "Manual vault withdrawal"
            )
            insertMainAccountTransaction(transaction)
        }
    }

    suspend fun overrideVaultBalance(vaultId: Long, newBalance: Double, reason: String?) {
        if (newBalance < 0.0) return
        val vault = smartVaultDao.getVaultById(vaultId) ?: return
        val sanitized = newBalance.coerceAtLeast(0.0)
        recordVaultBalanceAdjustment(
            vaultId = vaultId,
            previousBalance = vault.currentBalance,
            newBalance = sanitized,
            type = VaultAdjustmentType.MANUAL_EDIT,
            reason = reason
        )
    }

    suspend fun updateVaultArchived(vaultId: Long, archived: Boolean) {
        smartVaultDao.updateVaultArchived(vaultId, archived)
    }

    suspend fun recordVaultBalanceAdjustment(
        vaultId: Long,
        previousBalance: Double,
        newBalance: Double,
        type: VaultAdjustmentType,
        reason: String?
    ) {
        if (newBalance == previousBalance) return
        val delta = newBalance - previousBalance
        val timestamp = Instant.now()
        smartVaultDao.setVaultBalance(vaultId, newBalance, LocalDate.now())
        val adjustment = VaultBalanceAdjustment(
            vaultId = vaultId,
            type = type,
            delta = delta,
            resultingBalance = newBalance,
            createdAt = timestamp,
            reason = reason?.takeIf { it.isNotBlank() }
        )
        smartVaultDao.insertAdjustment(adjustment.toEntity())
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

    suspend fun clearBudgets() {
        budgetDao.clear()
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

    suspend fun clearRecurringExpenses() {
        recurringExpenseDao.clear()
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

    // Main Account Transaction Methods
    fun observeMainAccountTransactions(): Flow<List<com.example.sparely.domain.model.MainAccountTransaction>> =
        mainAccountDao.observeAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getRecentMainAccountTransactions(limit: Int = 50): List<com.example.sparely.domain.model.MainAccountTransaction> =
        mainAccountDao.getRecentTransactions(limit).map { it.toDomain() }

    suspend fun insertMainAccountTransaction(transaction: com.example.sparely.domain.model.MainAccountTransaction): Long =
        mainAccountDao.insertTransaction(transaction.toEntity())

    suspend fun getLatestMainAccountBalance(): Double {
        val transactionBalance = mainAccountDao.getLatestTransaction()?.balanceAfter
        // Fall back to preferences if no transactions exist yet
        // This fixes the bug where the first deduction would use 0.0 instead of the actual balance
        return transactionBalance ?: preferencesRepository.getSettingsSnapshot().mainAccountBalance
    }

    suspend fun calculateMainAccountBalance(): Double =
        mainAccountDao.calculateBalanceFromTransactions()

    suspend fun getAvailableMainAccountBalance(): Double {
        val canonical = getLatestMainAccountBalance()
        val frozen = getTotalFrozenAmount()
        return (canonical - frozen).coerceAtLeast(0.0)
    }

    suspend fun clearMainAccountTransactions() {
        mainAccountDao.deleteAllTransactions()
    }

    // Frozen funds methods
    suspend fun insertFrozenFund(pendingType: String, pendingId: Long, amount: Double, description: String? = null): Long {
        val frozen = com.example.sparely.data.local.FrozenFundEntity(
            pendingType = pendingType,
            pendingId = pendingId,
            amount = amount,
            createdAt = java.time.LocalDateTime.now(),
            description = description
        )
        return frozenFundDao.insert(frozen)
    }

    suspend fun removeFrozenForPending(pendingType: String, pendingId: Long) {
        frozenFundDao.deleteForPending(pendingType, pendingId)
    }

    suspend fun getTotalFrozenAmount(): Double {
        return frozenFundDao.totalFrozen()
    }

    fun observeFrozenFunds(): Flow<List<com.example.sparely.data.local.FrozenFundEntity>> = frozenFundDao.observeAll()

    suspend fun clearFrozenFunds() {
        frozenFundDao.deleteAll()
    }

    suspend fun upsertFrozenFund(entity: com.example.sparely.data.local.FrozenFundEntity) {
        frozenFundDao.insert(entity)
    }

    private suspend fun syncVaultSchedules(vaultId: Long, schedules: List<VaultSchedule>) {
        val existing = smartVaultDao.getSchedulesForVault(vaultId)
        val now = Instant.now()
        val incoming = schedules.map { schedule ->
            val created = if (schedule.id == 0L) now else schedule.createdAt
            schedule.copy(
                id = schedule.id,
                vaultId = vaultId,
                createdAt = created,
                updatedAt = now
            )
        }

        val incomingById = incoming.associateBy { it.id }

        // Update or delete existing
        val existingIds = existing.map { it.id }.toSet()

        incoming.forEach { schedule ->
            val entity = schedule.toEntity()
            if (schedule.id == 0L) {
                smartVaultDao.upsertSchedule(entity)
            } else {
                smartVaultDao.updateSchedule(entity)
            }
        }

        existing.filter { it.id !in incomingById.keys }.forEach { obsolete ->
            smartVaultDao.deleteSchedule(obsolete)
        }
    }

    suspend fun addVaultSchedule(vaultId: Long, schedule: VaultSchedule): Long {
        val now = Instant.now()
        val toPersist = schedule.copy(
            id = 0L,
            vaultId = vaultId,
            createdAt = now,
            updatedAt = now
        )
        return smartVaultDao.upsertSchedule(toPersist.toEntity())
    }

    suspend fun updateVaultSchedule(schedule: VaultSchedule) {
        if (schedule.id == 0L) {
            addVaultSchedule(schedule.vaultId, schedule)
        } else {
            smartVaultDao.updateSchedule(
                schedule.copy(updatedAt = Instant.now()).toEntity()
            )
        }
    }

    suspend fun deleteVaultSchedule(scheduleId: Long) {
        smartVaultDao.deleteSchedule(scheduleId)
    }

    suspend fun getSchedulesForVault(vaultId: Long): List<VaultSchedule> {
        return smartVaultDao.getSchedulesForVault(vaultId).map { it.toDomain() }
    }

    suspend fun getSmartVaultById(vaultId: Long): SmartVault? {
        val entity = smartVaultDao.getVaultById(vaultId) ?: return null
        val schedules = smartVaultDao.getSchedulesForVault(vaultId).map { it.toDomain() }
        return entity.toDomain(schedules)
    }

    suspend fun recordScheduleExecution(
        scheduleId: Long,
        vaultId: Long,
        amount: Double,
        runTimestamp: LocalDateTime,
        nextRunAt: LocalDateTime?
    ) {
        val scheduleEntity = smartVaultDao.getSchedulesForVault(vaultId).firstOrNull { it.id == scheduleId }
            ?: return

        smartVaultDao.updateSchedule(
            scheduleEntity.copy(
                lastRunAt = runTimestamp,
                nextRunAt = nextRunAt,
                updatedAt = Instant.now(),
                enabled = if (nextRunAt == null) false else scheduleEntity.enabled
            )
        )

        val contribution = VaultContribution(
            vaultId = vaultId,
            amount = amount,
            date = runTimestamp.toLocalDate(),
            source = VaultContributionSource.AUTO_DEPOSIT,
            note = "Scheduled transfer",
            reconciled = true
        )
        smartVaultDao.upsertContribution(contribution.toEntity())
    }

    suspend fun executeVaultTransfer(
        schedule: VaultSchedule,
        amount: Double,
        runTimestamp: LocalDateTime,
        notes: String?
    ): Boolean {
        val vault = smartVaultDao.getVaultById(schedule.vaultId) ?: return false
        return when (schedule.direction) {
            VaultTransferDirection.MAIN_TO_VAULT -> {
                val available = if (schedule.onlyIfBalanceAvailable) getAvailableMainAccountBalance() else Double.MAX_VALUE
                if (available + 1e-6 < amount) {
                    false
                } else {
                    val newBalance = vault.currentBalance + amount
                    recordVaultBalanceAdjustment(
                        vaultId = vault.id,
                        previousBalance = vault.currentBalance,
                        newBalance = newBalance,
                        type = VaultAdjustmentType.AUTOMATIC_RECURRING_TRANSFER,
                        reason = notes
                    )
                    val currentMain = getLatestMainAccountBalance()
                    val transaction = com.example.sparely.domain.model.MainAccountTransaction(
                        type = com.example.sparely.data.local.MainAccountTransactionType.VAULT_CONTRIBUTION,
                        amount = amount,
                        balanceAfter = (currentMain - amount).coerceAtLeast(0.0),
                        timestamp = runTimestamp,
                        description = notes ?: "Scheduled transfer to vault ${vault.name}"
                    )
                    insertMainAccountTransaction(transaction)
                    true
                }
            }
            VaultTransferDirection.VAULT_TO_MAIN -> {
                val available = vault.currentBalance
                if (schedule.onlyIfBalanceAvailable && available + 1e-6 < amount) {
                    false
                } else {
                    val newBalance = (vault.currentBalance - amount).coerceAtLeast(0.0)
                    recordVaultBalanceAdjustment(
                        vaultId = vault.id,
                        previousBalance = vault.currentBalance,
                        newBalance = newBalance,
                        type = VaultAdjustmentType.AUTOMATIC_RECURRING_TRANSFER,
                        reason = notes
                    )
                    val currentMain = getLatestMainAccountBalance()
                    val transaction = com.example.sparely.domain.model.MainAccountTransaction(
                        type = com.example.sparely.data.local.MainAccountTransactionType.DEPOSIT,
                        amount = amount,
                        balanceAfter = currentMain + amount,
                        timestamp = runTimestamp,
                        description = notes ?: "Scheduled transfer from vault ${vault.name}"
                    )
                    insertMainAccountTransaction(transaction)
                    true
                }
            }
        }
    }

    // Backup & Restore methods
    suspend fun getAllChallenges(): List<SavingsChallenge> =
        challengeDao.observeChallenges().first().map { it.toDomain() }

    suspend fun getAllAchievements(): List<Achievement> =
        achievementDao.observeAchievements().first().map { it.toDomain() }

    suspend fun getAllTransfers(): List<SavingsTransfer> =
        transferDao.observeTransfers().first().map { it.toDomain() }

    suspend fun getAllVaultContributions(): List<VaultContribution> =
        smartVaultDao.getAllContributions().map { it.toDomain() }

    suspend fun getAllVaultAdjustments(): List<VaultBalanceAdjustment> =
        smartVaultDao.getAllAdjustments().map { it.toDomain() }

    suspend fun getAllAllocationHistory(): List<com.example.sparely.data.local.AllocationHistoryEntity> =
        allocationHistoryDao.observeAll().first()

    suspend fun clearChallenges() {
        challengeDao.deleteAllMilestones()
        challengeDao.deleteAllChallenges()
    }

    suspend fun clearVaultData() {
        smartVaultDao.clearAllContributions()
        smartVaultDao.clearAllAdjustments()
        smartVaultDao.clearAllSchedules()
    }

    suspend fun clearAllocationHistory() {
        allocationHistoryDao.deleteAll()
    }

    suspend fun insertAchievements(achievements: List<Achievement>) {
        achievementDao.upsertAll(achievements.map { it.toEntity() })
    }

    suspend fun insertTransfers(transfers: List<SavingsTransfer>) {
        transfers.forEach { transferDao.upsert(it.toEntity()) }
    }

    suspend fun insertVaultContributions(contributions: List<VaultContribution>) {
        contributions.forEach { smartVaultDao.upsertContribution(it.toEntity()) }
    }

    suspend fun insertVaultAdjustments(adjustments: List<VaultBalanceAdjustment>) {
        adjustments.forEach { smartVaultDao.insertAdjustment(it.toEntity()) }
    }

    suspend fun insertAllocationHistory(history: List<com.example.sparely.data.local.AllocationHistoryEntity>) {
        history.forEach { allocationHistoryDao.insert(it) }
    }
}
