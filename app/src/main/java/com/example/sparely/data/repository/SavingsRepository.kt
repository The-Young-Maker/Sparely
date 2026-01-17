package com.example.sparely.data.repository

import androidx.room.withTransaction
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
import com.example.sparely.data.local.StoreDao
import com.example.sparely.data.local.PaymentMethodDao
import com.example.sparely.data.local.CreditCardPaymentDao
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
import com.example.sparely.domain.model.Store
import com.example.sparely.domain.model.PaymentMethod
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
    private val storeDao: StoreDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val creditCardPaymentDao: CreditCardPaymentDao,
    private val expenseItemDao: com.example.sparely.data.local.ExpenseItemDao,
    private val preferencesRepository: com.example.sparely.data.preferences.UserPreferencesRepository,
    private val database: com.example.sparely.data.local.SparelyDatabase
) {

    suspend fun runInTransaction(block: suspend () -> Unit) {
        database.withTransaction {
            block()
        }
    }

    fun observeExpenses(): Flow<List<com.example.sparely.domain.model.Expense>> =
        expenseDao.observeExpenses().map { entities ->
            entities.map { relation -> 
                val expense = relation.expense.toDomain()
                expense.copy(items = relation.items.map { it.toDomain() })
            }
        }

    fun observeExpensesBetween(from: LocalDate, to: LocalDate): Flow<List<ExpenseEntity>> =
        expenseDao.observeExpensesBetween(from, to)

    suspend fun upsertExpense(entity: ExpenseEntity): Long {
        return expenseDao.upsertExpense(entity)
    }

    suspend fun deleteExpense(entity: ExpenseEntity) {
        expenseDao.deleteExpense(entity)
    }

    suspend fun deleteExpensesBefore(date: LocalDate): Int {
        return expenseDao.deleteExpensesBefore(date)
    }

    suspend fun countExpensesBefore(date: LocalDate): Int {
        return expenseDao.countExpensesBefore(date)
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
        val vault = smartVaultDao.getVaultById(id) ?: return
        if (vault.currentBalance > 0) {
            deductFromVault(
                vaultId = id,
                amount = vault.currentBalance,
                reason = "Vault closure: ${vault.name}",
                creditMainAccount = true
            )
        }
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

    fun observePendingVaultContributions(): Flow<List<VaultContribution>> =
        smartVaultDao.observePendingContributions().map { list -> list.map { it.toDomain() } }
    

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

    suspend fun getReconciledVaultContributions(vaultId: Long): List<VaultContribution> =
        smartVaultDao.getReconciledContributionsForVault(vaultId).map { it.toDomain() }

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
                description = reason?.take(100) ?: "Manual deposit to ${vault.name}"
            )
            insertMainAccountTransaction(transaction)
            preferencesRepository.updateMainAccountBalance(transaction.balanceAfter)
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
                description = reason?.take(100) ?: "Manual withdrawal from ${vault.name}"
            )
            insertMainAccountTransaction(transaction)
            preferencesRepository.updateMainAccountBalance(transaction.balanceAfter)
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

    suspend fun insertMainAccountTransaction(transaction: com.example.sparely.domain.model.MainAccountTransaction): Long {
        val transactionId = mainAccountDao.insertTransaction(transaction.toEntity())
        transaction.relatedVaultContributionIds?.forEach { contributionId ->
            mainAccountDao.insertTransactionVaultCrossRef(
                com.example.sparely.data.local.TransactionVaultContributionCrossRef(
                    transactionId = transactionId,
                    contributionId = contributionId
                )
            )
        }
        return transactionId
    }

    suspend fun getLatestMainAccountBalance(): Double {
        val transactionBalance = mainAccountDao.getLatestTransaction()?.transaction?.balanceAfter
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

    suspend fun flagExpenseAsRefunded(expenseId: Long, refundedAmount: Double, totalRefunded: Double) {
        val expense = expenseDao.findExpenseById(expenseId) ?: return
        val isFullyRefunded = totalRefunded >= expense.amount
        val updated = expense.copy(
            refundedAmount = totalRefunded,
            isRefunded = isFullyRefunded
        )
        expenseDao.upsertExpense(updated)
    }

    suspend fun deletePendingContributionsForExpense(expenseId: Long) {
        // Find them first to remove frozen funds
        val contributions = smartVaultDao.getContributionsForExpense(expenseId).filter { !it.reconciled }
        contributions.forEach { c ->
             removeFrozenForPending("VAULT_CONTRIBUTION", c.id)
        }
        smartVaultDao.deletePendingContributionsForExpense(expenseId)
    }

    suspend fun getContributionsForExpense(expenseId: Long): List<VaultContribution> = 
        smartVaultDao.getContributionsForExpense(expenseId).map { it.toDomain() }

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
                    preferencesRepository.updateMainAccountBalance(transaction.balanceAfter)
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
                    preferencesRepository.updateMainAccountBalance(transaction.balanceAfter)
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

    // Store functions
    fun observeStores(): Flow<List<Store>> =
        storeDao.observeStores().map { entities -> entities.map { it.toDomain() } }

    suspend fun searchStores(query: String): List<Store> =
        storeDao.searchStores(query).map { it.toDomain() }

    suspend fun getStoreById(id: Long): Store? =
        storeDao.getStoreById(id)?.toDomain()

    suspend fun insertStore(store: Store): Long =
        storeDao.insertStore(store.toEntity())

    suspend fun updateStore(store: Store) {
        storeDao.updateStore(store.toEntity())
    }

    suspend fun deleteStore(store: Store) {
        storeDao.deleteStore(store.toEntity())
    }

    suspend fun clearStores() {
        storeDao.clearAll()
    }

    // Payment Method functions
    fun observePaymentMethods(): Flow<List<PaymentMethod>> =
        paymentMethodDao.getAllPaymentMethods().map { entities -> entities.map { it.toDomain() } }

    suspend fun getPaymentMethodById(id: Long): PaymentMethod? =
        paymentMethodDao.getPaymentMethodById(id)?.toDomain()

    suspend fun insertPaymentMethod(method: PaymentMethod): Long {
        if (method.isDefault) {
            paymentMethodDao.clearDefaultPaymentMethod()
        }
        return paymentMethodDao.insertPaymentMethod(method.toEntity())
    }

    suspend fun updatePaymentMethod(method: PaymentMethod) {
        if (method.isDefault) {
            paymentMethodDao.clearDefaultPaymentMethod()
        }
        paymentMethodDao.updatePaymentMethod(method.toEntity())
    }

    suspend fun deletePaymentMethod(method: PaymentMethod) {
        paymentMethodDao.deletePaymentMethod(method.toEntity())
    }

    suspend fun clearPaymentMethods() {
        paymentMethodDao.deleteAll()
         // Re-seed defaults if needed, but for now we just clear
    }

    // Credit Card specific functions
    fun observeCreditCards(): Flow<List<PaymentMethod>> =
        paymentMethodDao.getCreditCards().map { entities -> entities.map { it.toDomain() } }

    suspend fun addToCreditCardBalance(paymentMethodId: Long, amount: Double) {
        paymentMethodDao.addToBalance(paymentMethodId, amount)
    }

    suspend fun recordCreditCardPayment(
        paymentMethodId: Long,
        amount: Double,
        note: String? = null,
        date: LocalDate = LocalDate.now(),
        deductFromMainAccount: Boolean = false
    ) {
        // Insert payment record
        val payment = com.example.sparely.data.local.CreditCardPaymentEntity(
            paymentMethodId = paymentMethodId,
            amount = amount,
            date = date,
            note = note
        )
        creditCardPaymentDao.insertPayment(payment)
        // Update balance on payment method
        paymentMethodDao.recordPayment(paymentMethodId, amount, date)
        
        // If deducting from main account, update balance and log transaction
        if (deductFromMainAccount) {
            val paymentMethod = paymentMethodDao.getPaymentMethodById(paymentMethodId)
            val cardName = paymentMethod?.name ?: "Credit Card"
            
            val settings = preferencesRepository.getSettingsSnapshot()
            val currentBalance = settings.mainAccountBalance
            val newBalance = currentBalance - amount
            preferencesRepository.updateMainAccountBalance(newBalance)
            
            // Log transaction with CREDIT_CARD_PAYMENT type
            val transaction = com.example.sparely.data.local.MainAccountTransactionEntity(
                type = com.example.sparely.data.local.MainAccountTransactionType.CREDIT_CARD_PAYMENT,
                amount = -amount,
                balanceAfter = newBalance,
                timestamp = java.time.LocalDateTime.now(),
                description = "Payment to $cardName${note?.let { ": $it" } ?: ""}"
            )
            mainAccountDao.insertTransaction(transaction)
        }
    }

    suspend fun getAllCreditCardPayments(): List<com.example.sparely.domain.model.CreditCardPayment> =
        creditCardPaymentDao.getAllPayments().first().map { it.toDomain() }

    fun observeCreditCardPayments(): Flow<List<com.example.sparely.domain.model.CreditCardPayment>> =
        creditCardPaymentDao.getAllPayments().map { entities -> entities.map { it.toDomain() } }

    suspend fun insertCreditCardPayment(payment: com.example.sparely.domain.model.CreditCardPayment) {
        creditCardPaymentDao.insertPayment(payment.toEntity())
    }

    suspend fun clearCreditCardPayments() {
        creditCardPaymentDao.deleteAll()
    }

    /**
     * Process a recurring expense payment with full expense logic.
     * This mirrors the logic in SparelyViewModel.addExpense() but runs in the background worker context.
     * Handles:
     * - Creating expense with all field mappings (storeId, paymentMethodId, isRecurring)
     * - Credit card balance updates
     * - Vault contributions (saving tax)
     * - Main account deductions
     */
    suspend fun processRecurringExpensePayment(
        recurringEntity: com.example.sparely.data.local.RecurringExpenseEntity,
        processDate: LocalDate,
        settings: com.example.sparely.domain.model.SparelySettings,
        vaults: List<com.example.sparely.domain.model.SmartVault>
    ) {
        // Create expense entity with all field mappings
        val percentages = if (recurringEntity.manualPercentEmergency != null) {
            com.example.sparely.domain.model.SavingsPercentages(
                emergency = recurringEntity.manualPercentEmergency,
                invest = recurringEntity.manualPercentInvest ?: 0.0,
                `fun` = recurringEntity.manualPercentFun ?: 0.0,
                safeInvestmentSplit = recurringEntity.manualSafeSplit ?: 0.5
            )
        } else {
            settings.defaultPercentages
        }
        
        val adjusted = percentages.adjustWithinBudget()
        val amount = recurringEntity.amount
        val emergency = amount * adjusted.emergency
        val invest = amount * adjusted.invest
        val funAmount = amount * adjusted.`fun`
        val safe = invest * adjusted.safeInvestmentSplit
        val risky = invest - safe
        
        val expenseEntity = com.example.sparely.data.local.ExpenseEntity(
            id = 0L,
            description = recurringEntity.description,
            amount = amount,
            category = recurringEntity.category,
            date = processDate,
            includesTax = recurringEntity.includesTax,
            emergencyAmount = emergency.roundCurrency(),
            investmentAmount = invest.roundCurrency(),
            funAmount = funAmount.roundCurrency(),
            safeInvestmentAmount = safe.roundCurrency(),
            highRiskInvestmentAmount = risky.roundCurrency(),
            autoRecommended = false,
            appliedPercentEmergency = adjusted.emergency,
            appliedPercentInvest = adjusted.invest,
            appliedPercentFun = adjusted.`fun`,
            appliedSafeSplit = adjusted.safeInvestmentSplit,
            riskLevelUsed = settings.riskLevel,
            deductedFromVaultId = recurringEntity.deductedFromVaultId,
            storeId = recurringEntity.storeId,
            paymentMethodId = recurringEntity.paymentMethodId,
            isRecurring = true
        )
        upsertExpense(expenseEntity)
        
        // Get current balance
        var currentBalance = getLatestMainAccountBalance()
        
        // Handle vault deduction if specified
        if (recurringEntity.deductedFromVaultId != null) {
            val vault = vaults.find { it.id == recurringEntity.deductedFromVaultId }
            if (vault != null) {
                val vaultBalanceBefore = vault.currentBalance
                val deductFromVault = amount.coerceAtMost(vaultBalanceBefore)
                val overflowToMainAccount = (amount - vaultBalanceBefore).coerceAtLeast(0.0)
                val vaultBalanceAfter = (vaultBalanceBefore - deductFromVault).coerceAtLeast(0.0)
                
                if (deductFromVault > 0.0) {
                    recordVaultBalanceAdjustment(
                        vaultId = vault.id,
                        previousBalance = vaultBalanceBefore,
                        newBalance = vaultBalanceAfter,
                        type = VaultAdjustmentType.MANUAL_DEDUCTION,
                        reason = "Recurring expense: ${recurringEntity.description.take(100)}"
                    )
                }
                
                if (overflowToMainAccount > 0.0 && recurringEntity.deductFromMainAccount) {
                    val newBalance = (currentBalance - overflowToMainAccount).coerceAtLeast(0.0)
                    val transaction = com.example.sparely.domain.model.MainAccountTransaction(
                        type = com.example.sparely.data.local.MainAccountTransactionType.EXPENSE,
                        amount = overflowToMainAccount,
                        balanceAfter = newBalance,
                        timestamp = java.time.LocalDateTime.now(),
                        description = "Overflow from ${vault.name} - recurring: ${recurringEntity.description.take(70)}"
                    )
                    insertMainAccountTransaction(transaction)
                    preferencesRepository.updateMainAccountBalance(newBalance)
                    currentBalance = newBalance
                }
            }
        } else if (recurringEntity.deductFromMainAccount) {
            val newBalance = (currentBalance - amount).coerceAtLeast(0.0)
            val transaction = com.example.sparely.domain.model.MainAccountTransaction(
                type = com.example.sparely.data.local.MainAccountTransactionType.EXPENSE,
                amount = amount,
                balanceAfter = newBalance,
                timestamp = java.time.LocalDateTime.now(),
                description = "Auto-logged recurring: ${recurringEntity.description.take(100)}"
            )
            insertMainAccountTransaction(transaction)
            preferencesRepository.updateMainAccountBalance(newBalance)
            currentBalance = newBalance
        }
        
        // Update credit card balance if payment method is a credit card
        if (recurringEntity.paymentMethodId != null) {
            val paymentMethod = getPaymentMethodById(recurringEntity.paymentMethodId)
            if (paymentMethod?.isCreditCard == true) {
                addToCreditCardBalance(paymentMethod.id, amount)
            }
        }
        
        // Apply saving tax to vaults
        val savingTaxContext = com.example.sparely.domain.logic.SavingTaxEngine.Context(
            expenseAmount = amount,
            expenseDate = processDate,
            settings = settings,
            vaults = vaults
        )
        val savingTaxPlans = com.example.sparely.domain.logic.SavingTaxEngine.calculate(savingTaxContext)
        
        if (savingTaxPlans.isNotEmpty()) {
            val contributions = savingTaxPlans.map { plan ->
                VaultContribution(
                    vaultId = plan.vaultId,
                    amount = plan.amount,
                    date = processDate,
                    source = VaultContributionSource.SAVING_TAX,
                    note = "Saving tax from recurring: ${recurringEntity.description}".take(120)
                )
            }
            val contributionIds = logVaultContributions(contributions)
            
            val totalSavingTax = savingTaxPlans.sumOf { it.amount }
            if (totalSavingTax > 0.0) {
                val newBalance = (currentBalance - totalSavingTax).coerceAtLeast(0.0)
                val transaction = com.example.sparely.domain.model.MainAccountTransaction(
                    type = com.example.sparely.data.local.MainAccountTransactionType.VAULT_CONTRIBUTION,
                    amount = totalSavingTax,
                    balanceAfter = newBalance,
                    timestamp = java.time.LocalDateTime.now(),
                    description = "Saving tax to ${savingTaxPlans.size} vault(s)",
                    relatedVaultContributionIds = contributionIds
                )
                insertMainAccountTransaction(transaction)
                preferencesRepository.updateMainAccountBalance(newBalance)
            }
        }
    }
    
    private fun Double.roundCurrency(): Double = kotlin.math.round(this * 100) / 100.0

    // Expense Item methods
    fun observeItemsForExpense(expenseId: Long): Flow<List<com.example.sparely.domain.model.ExpenseItem>> =
        expenseItemDao.observeItemsForExpense(expenseId).map { entities -> entities.map { it.toDomain() } }

    suspend fun getItemsForExpense(expenseId: Long): List<com.example.sparely.domain.model.ExpenseItem> =
        expenseItemDao.getItemsForExpense(expenseId).map { it.toDomain() }

    suspend fun getAllExpenseItems(): List<com.example.sparely.domain.model.ExpenseItem> =
        expenseItemDao.getAllItems().map { it.toDomain() }

    suspend fun insertExpenseItem(item: com.example.sparely.domain.model.ExpenseItem): Long =
        expenseItemDao.insertItem(item.toEntity())

    suspend fun insertExpenseItems(items: List<com.example.sparely.domain.model.ExpenseItem>) {
        expenseItemDao.insertItems(items.map { it.toEntity() })
    }

    suspend fun updateExpenseItem(item: com.example.sparely.domain.model.ExpenseItem) {
        expenseItemDao.updateItem(item.toEntity())
    }

    suspend fun deleteExpenseItem(item: com.example.sparely.domain.model.ExpenseItem) {
        expenseItemDao.deleteItem(item.toEntity())
    }

    suspend fun deleteItemsForExpense(expenseId: Long) {
        expenseItemDao.deleteItemsForExpense(expenseId)
    }

    suspend fun clearExpenseItems() {
        expenseItemDao.clearAll()
    }
}

