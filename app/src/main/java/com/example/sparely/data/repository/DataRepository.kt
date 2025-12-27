package com.example.sparely.data.repository

import com.example.sparely.data.preferences.UserPreferencesRepository
import com.example.sparely.domain.model.BackupData
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.example.sparely.data.local.toDomain
import com.example.sparely.data.local.toEntity

import com.google.gson.GsonBuilder
import com.example.sparely.data.utils.LocalDateAdapter
import com.example.sparely.data.utils.LocalDateTimeAdapter
import com.example.sparely.data.utils.InstantAdapter
import com.example.sparely.data.utils.YearMonthAdapter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Instant
import java.time.YearMonth

class DataRepository(
    private val savingsRepository: SavingsRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val gson: Gson = GsonBuilder()
        .registerTypeHierarchyAdapter(LocalDate::class.java, LocalDateAdapter())
        .registerTypeHierarchyAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .registerTypeHierarchyAdapter(Instant::class.java, InstantAdapter())
        .registerTypeHierarchyAdapter(YearMonth::class.java, YearMonthAdapter())
        .serializeNulls() // Ensure we don't skip null fields which might be meaningful or needed for structural integrity
        .create()
) {

    suspend fun exportData(): String = withContext(Dispatchers.IO) {
        val settings = preferencesRepository.settingsFlow.first()
        val vaults = savingsRepository.observeSmartVaults().first()
        val budgets = savingsRepository.observeBudgets().first()
        val expenses = savingsRepository.observeExpenses().first().map { it.toDomain() }
        val savingsAccounts = savingsRepository.observeSavingsAccounts().first()
        val recurring = savingsRepository.observeRecurringExpenses().first()
        val transactions = savingsRepository.observeMainAccountTransactions().first()
        val frozenFunds = savingsRepository.observeFrozenFunds().first()
        
        // Ensure we fetch lists, defaulting to empty if null/error (though repository methods shouldn't return null)
        val challenges = savingsRepository.getAllChallenges()
        val achievements = savingsRepository.getAllAchievements()
        val transfers = savingsRepository.getAllTransfers()
        val vaultContributions = savingsRepository.getAllVaultContributions()
        val vaultAdjustments = savingsRepository.getAllVaultAdjustments()
        val allocationHistory = savingsRepository.getAllAllocationHistory()
        val mainAccountBalance = savingsRepository.getLatestMainAccountBalance()
        
        val backup = BackupData(
            settings = settings,
            vaults = vaults,
            budgets = budgets,
            expenses = expenses,
            savingsAccounts = savingsAccounts,
            recurringExpenses = recurring,
            transactions = transactions,
            frozenFunds = frozenFunds,
            challenges = challenges,
            achievements = achievements,
            transfers = transfers,
            vaultContributions = vaultContributions,
            vaultAdjustments = vaultAdjustments,
            allocationHistory = allocationHistory,
            mainAccountBalance = mainAccountBalance
        )
        
        gson.toJson(backup)
    }

    suspend fun restoreData(json: String) = withContext(Dispatchers.IO) {
        android.util.Log.d("DataRepository", "Starting restore...")
        
        val backup = try {
            gson.fromJson(json, BackupData::class.java)
        } catch (e: Exception) {
            android.util.Log.e("DataRepository", "Failed to parse backup JSON", e)
            throw e
        }
        
        android.util.Log.d("DataRepository", "Parsed backup: ${backup.expenses.size} expenses, ${backup.vaults.size} vaults, ${backup.transactions.size} transactions")
        
        // 1. Clear existing data
        try {
            savingsRepository.clearExpenses()
            savingsRepository.clearSmartVaults()
            savingsRepository.clearBudgets()
            savingsRepository.clearTransfers()
            savingsRepository.clearRecurringExpenses()
            savingsRepository.clearMainAccountTransactions()
            savingsRepository.clearFrozenFunds()
            savingsRepository.clearChallenges()
            savingsRepository.clearAchievements()
            savingsRepository.clearVaultData()
            savingsRepository.clearAllocationHistory()
            android.util.Log.d("DataRepository", "Cleared existing data")
        } catch (e: Exception) {
            android.util.Log.e("DataRepository", "Failed to clear existing data", e)
            throw e
        }
        
        // 2. Restore Data
        // Restore Settings
        try {
            val s = backup.settings
            preferencesRepository.updateMonthlyIncome(s.monthlyIncome)
            preferencesRepository.updateAge(s.age)
            preferencesRepository.updateRiskLevel(s.riskLevel)
            preferencesRepository.updateEducationStatus(s.educationStatus)
            preferencesRepository.updateEmploymentStatus(s.employmentStatus)
            preferencesRepository.updateLivingSituation(s.livingSituation)
            preferencesRepository.updateOccupation(s.occupation)
            
            val balanceToRestore = backup.mainAccountBalance ?: s.mainAccountBalance
            preferencesRepository.updateMainAccountBalance(balanceToRestore)
            
            preferencesRepository.updateSavingsAccountBalance(s.savingsAccountBalance)
            preferencesRepository.updateHasDebts(s.hasDebts)
            preferencesRepository.updateEmergencyFund(s.currentEmergencyFund)
            preferencesRepository.updateSubscriptionTotal(s.subscriptionTotal)
            preferencesRepository.updatePrimaryGoal(s.primaryGoal)
            preferencesRepository.updateDisplayName(s.displayName)
            preferencesRepository.updateBirthday(s.birthday)
            s.joinedDate?.let { preferencesRepository.setJoinedDate(it) }
            preferencesRepository.setOnboardingCompleted(true)
            android.util.Log.d("DataRepository", "Restored settings")
        } catch (e: Exception) {
            android.util.Log.e("DataRepository", "Failed to restore settings", e)
            throw e
        }
        
        // Restore Entities in order
        // 1. Accounts & Vaults (parents)
        try {
            backup.savingsAccounts.forEach { savingsRepository.upsertSavingsAccount(it) }
            backup.vaults.forEach { savingsRepository.upsertSmartVault(it) }
            android.util.Log.d("DataRepository", "Restored ${backup.vaults.size} vaults")
        } catch (e: Exception) {
            android.util.Log.e("DataRepository", "Failed to restore vaults", e)
            throw e
        }
        
        // 2. Budgets & Recurring
        try {
            backup.budgets.forEach { savingsRepository.upsertBudget(it) }
            backup.recurringExpenses.forEach { savingsRepository.upsertRecurringExpense(it) }
            android.util.Log.d("DataRepository", "Restored budgets and recurring")
        } catch (e: Exception) {
            android.util.Log.e("DataRepository", "Failed to restore budgets/recurring", e)
            throw e
        }
        
        // 3. Transactions & History
        try {
            backup.transactions.forEach { savingsRepository.insertMainAccountTransaction(it) }
            android.util.Log.d("DataRepository", "Restored ${backup.transactions.size} transactions")
        } catch (e: Exception) {
            android.util.Log.e("DataRepository", "Failed to restore transactions", e)
            throw e
        }
        
        try {
            backup.frozenFunds.forEach { savingsRepository.upsertFrozenFund(it) }
            android.util.Log.d("DataRepository", "Restored frozen funds")
        } catch (e: Exception) {
            android.util.Log.e("DataRepository", "Failed to restore frozen funds", e)
            throw e
        }
        
        // Restore expenses
        // Handle corrupted dates from older backups (date: {} becomes 1970-01-01)
        val backupDate = java.time.Instant.ofEpochMilli(backup.timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val epochDate = LocalDate.of(1970, 1, 1)
        
        try {
            android.util.Log.d("DataRepository", "Attempting to restore ${backup.expenses.size} expenses... (backup date: $backupDate)")
            backup.expenses.forEachIndexed { index, domainExpense ->
                try {
                    // Fix corrupted epoch dates by using the backup timestamp
                    val fixedExpense = if (domainExpense.date == epochDate) {
                        domainExpense.copy(date = backupDate)
                    } else {
                        domainExpense
                    }
                    val entity = fixedExpense.toEntity()
                    savingsRepository.upsertExpense(entity)
                } catch (e: Exception) {
                    android.util.Log.e("DataRepository", "Failed to restore expense $index: ${domainExpense.description}", e)
                }
            }
            android.util.Log.d("DataRepository", "Restored expenses")
        } catch (e: Exception) {
            android.util.Log.e("DataRepository", "Failed in expense restoration block", e)
            throw e
        }
        
        // 4. Missing pieces (these fields may be null in older backups)
        try {
            savingsRepository.insertTransfers(backup.transfers.orEmpty())
            backup.challenges.orEmpty().forEach { savingsRepository.upsertSavingsChallenge(it) }
            savingsRepository.insertAchievements(backup.achievements.orEmpty())
            android.util.Log.d("DataRepository", "Restored transfers, challenges, achievements")
        } catch (e: Exception) {
            android.util.Log.e("DataRepository", "Failed to restore transfers/challenges/achievements", e)
            throw e
        }
        
        // 5. Vault History (may be null in older backups)
        try {
            savingsRepository.insertVaultContributions(backup.vaultContributions.orEmpty())
            savingsRepository.insertVaultAdjustments(backup.vaultAdjustments.orEmpty())
            savingsRepository.insertAllocationHistory(backup.allocationHistory.orEmpty())
            android.util.Log.d("DataRepository", "Restored vault history")
        } catch (e: Exception) {
            android.util.Log.e("DataRepository", "Failed to restore vault history", e)
            throw e
        }
        
        android.util.Log.d("DataRepository", "Restore complete!")
    }
}
