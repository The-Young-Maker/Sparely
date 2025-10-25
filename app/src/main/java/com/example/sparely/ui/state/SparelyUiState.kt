package com.example.sparely.ui.state

import com.example.sparely.domain.model.*

data class SparelyUiState(
    val settings: SparelySettings = SparelySettings(),
    val expenses: List<Expense> = emptyList(),
    val analytics: AnalyticsSnapshot = AnalyticsSnapshot(),
    val recommendation: RecommendationResult? = null,
    val manualTransfers: List<SavingsTransfer> = emptyList(),
    val savingsPlan: SavingsPlan? = null,
    val smartSavingSummary: SmartSavingSummary? = null,
    val alerts: List<AlertMessage> = emptyList(),
    val smartVaults: List<SmartVault> = emptyList(),
    val totalVaultBalance: Double = 0.0,
    val vaultAdjustments: Map<Long, List<VaultBalanceAdjustment>> = emptyMap(),
    val emergencyFundGoal: EmergencyFundGoal? = null,
    val onboardingCompleted: Boolean = false,
    val activeSaveRate: Double = 0.0,
    val activeSavingTaxRate: Double = 0.0,
    val automationRationale: List<String> = emptyList(),
    
    // New features
    val budgets: List<CategoryBudget> = emptyList(),
    val budgetSummary: BudgetSummary? = null,
    val budgetSuggestions: List<BudgetSuggestion> = emptyList(),
    val budgetPrompts: List<BudgetOverrunPrompt> = emptyList(),
    val recurringExpenses: List<RecurringExpense> = emptyList(),
    val upcomingRecurring: List<UpcomingRecurringExpense> = emptyList(),
    val activeChallenges: List<SavingsChallenge> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val financialHealthScore: FinancialHealthScore? = null,
    val detectedRecurringTransactions: List<DetectedRecurringTransaction> = emptyList(),
    val vaultProjections: List<VaultProjection> = emptyList(),
    val upcomingVaultDeposits: List<VaultContribution> = emptyList(),
    val pendingVaultContributions: List<VaultContribution> = emptyList(),
    val autoDepositCheckHour: Int = 9,
    val mainAccountTransactions: List<MainAccountTransaction> = emptyList(),
    val vaultArchivePrompt: VaultArchivePrompt? = null,
    
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
