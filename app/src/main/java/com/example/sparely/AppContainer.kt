package com.example.sparely

import android.content.Context
import com.example.sparely.data.local.SparelyDatabase
import com.example.sparely.data.preferences.UserPreferencesRepository
import com.example.sparely.data.repository.SavingsRepository
import com.example.sparely.domain.logic.RecommendationEngine
import com.example.sparely.notifications.NotificationScheduler
import com.example.sparely.workers.VaultAutoDepositScheduler

interface AppContainer {
    val savingsRepository: SavingsRepository
    val preferencesRepository: UserPreferencesRepository
    val recommendationEngine: RecommendationEngine
    val notificationScheduler: NotificationScheduler
    val vaultAutoDepositScheduler: VaultAutoDepositScheduler
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext
    private val database by lazy { SparelyDatabase.getInstance(appContext) }

    override val savingsRepository: SavingsRepository by lazy {
        SavingsRepository(
            expenseDao = database.expenseDao(),
            transferDao = database.transferDao(),
            budgetDao = database.budgetDao(),
            recurringExpenseDao = database.recurringExpenseDao(),
            challengeDao = database.challengeDao(),
            achievementDao = database.achievementDao(),
            savingsAccountDao = database.savingsAccountDao(),
            smartVaultDao = database.smartVaultDao(),
            mainAccountDao = database.mainAccountDao()
        )
    }

    override val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    override val recommendationEngine: RecommendationEngine by lazy {
        RecommendationEngine()
    }

    override val notificationScheduler: NotificationScheduler by lazy {
        NotificationScheduler(appContext)
    }
    
    override val vaultAutoDepositScheduler: VaultAutoDepositScheduler by lazy {
        VaultAutoDepositScheduler(appContext)
    }
}
