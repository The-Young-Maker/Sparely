package com.example.sparely.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sparely.AppContainer
import com.example.sparely.data.local.ExpenseEntity
import com.example.sparely.data.local.GoalEntity
import com.example.sparely.data.local.SavingsTransferEntity
import com.example.sparely.data.local.toDomain
import com.example.sparely.data.repository.SavingsRepository
import com.example.sparely.domain.logic.AlertsGenerator
import com.example.sparely.domain.logic.AnalyticsEngine
import com.example.sparely.domain.logic.BudgetEngine
import com.example.sparely.domain.logic.ChallengeEngine
import com.example.sparely.domain.logic.FinancialHealthEngine
import com.example.sparely.domain.logic.GoalPlanner
import com.example.sparely.domain.logic.RecommendationEngine
import com.example.sparely.domain.logic.SavingsAdvisor
import com.example.sparely.domain.logic.SavingsCalculator
import com.example.sparely.domain.logic.SavingTaxEngine
import com.example.sparely.domain.logic.SmartTransferEngine
import com.example.sparely.domain.logic.DynamicAllocationEngine
import com.example.sparely.domain.logic.IncomeAutomationEngine
import com.example.sparely.domain.model.Achievement
import com.example.sparely.domain.model.AlertMessage
import com.example.sparely.domain.model.AlertType
import com.example.sparely.domain.model.AllocationBreakdown
import com.example.sparely.domain.model.AnalyticsSnapshot
import com.example.sparely.domain.model.BudgetInput
import com.example.sparely.domain.model.BudgetOverrunPrompt
import com.example.sparely.domain.model.BudgetSummary
import com.example.sparely.domain.model.CategoryBudget
import com.example.sparely.domain.model.ChallengeInput
import com.example.sparely.domain.model.ChallengeType
import com.example.sparely.domain.model.EducationStatus
import com.example.sparely.domain.model.EmploymentStatus
import com.example.sparely.domain.model.Expense
import com.example.sparely.domain.model.ExpenseCategory
import com.example.sparely.domain.model.ExpenseInput
import com.example.sparely.domain.model.GoalInput
import com.example.sparely.domain.model.RecurringExpense
import com.example.sparely.domain.model.RecurringExpenseInput
import com.example.sparely.domain.model.RiskLevel
import com.example.sparely.domain.model.SavingsCategory
import com.example.sparely.domain.model.SavingsChallenge
import com.example.sparely.domain.model.SavingsPercentages
import com.example.sparely.domain.model.SparelySettings
import com.example.sparely.domain.model.SmartAllocationMode
import com.example.sparely.domain.model.SmartSavingSummary
import com.example.sparely.domain.model.SmartTransferSnapshot
import com.example.sparely.domain.model.SmartTransferStatus
import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.TransferReminderPreference
import com.example.sparely.domain.model.UpcomingRecurringExpense
import com.example.sparely.domain.model.UserProfileSetup
import com.example.sparely.domain.model.DetectedRecurringTransaction
import com.example.sparely.domain.model.RecommendationResult
import com.example.sparely.domain.model.VaultAllocationMode
import com.example.sparely.domain.model.VaultContribution
import com.example.sparely.domain.model.VaultContributionSource
import com.example.sparely.domain.model.toSmartVault
import com.example.sparely.domain.model.toSmartVaultSetup
import com.example.sparely.domain.model.IncomeTrackingMode
import com.example.sparely.domain.model.PayInterval
import com.example.sparely.domain.model.PayScheduleSettings
import com.example.sparely.notifications.NotificationScheduler
import com.example.sparely.workers.VaultAutoDepositScheduler
import com.example.sparely.data.preferences.UserPreferencesRepository
import com.example.sparely.ui.state.SparelyUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

class SparelyViewModel(
    private val savingsRepository: SavingsRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val recommendationEngine: RecommendationEngine,
    private val notificationScheduler: NotificationScheduler,
    private val vaultAutoDepositScheduler: VaultAutoDepositScheduler,
    private val container: AppContainer,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(SparelyUiState())
    val uiState: StateFlow<SparelyUiState> = _uiState.asStateFlow()

    private val processedAchievementTitles = mutableSetOf<String>()
    private val handledBudgetPrompts = mutableSetOf<String>()
    private var lastBirthdayGreetingDate: LocalDate? = null

    private fun promptKey(category: ExpenseCategory, month: YearMonth): String = "${category.name}-${month}"
    private fun promptKey(prompt: BudgetOverrunPrompt): String = promptKey(prompt.category, prompt.month)

    private fun isMeaningfullyDifferent(current: Double?, candidate: Double, tolerance: Double = 0.0025): Boolean {
        if (current == null || current.isNaN()) return true
        return abs(current - candidate) > tolerance
    }

    private fun paychecksPerMonth(schedule: PayScheduleSettings): Double {
        return when (schedule.interval) {
            PayInterval.WEEKLY -> 52.0 / 12.0
            PayInterval.BIWEEKLY -> 26.0 / 12.0
            PayInterval.SEMI_MONTHLY -> 2.0
            PayInterval.MONTHLY -> 1.0
            PayInterval.CUSTOM -> schedule.customDaysBetween?.let { 30.0 / it.coerceAtLeast(1) } ?: 1.0
        }
    }

    private data class AggregatedFeeds(
        val expenses: List<ExpenseEntity>,
        val goals: List<GoalEntity>,
        val transfers: List<SavingsTransferEntity>,
        val settings: SparelySettings,
        val smartSnapshot: SmartTransferSnapshot,
        val vaults: List<SmartVault> = emptyList(),
        val budgets: List<CategoryBudget> = emptyList(),
        val recurring: List<RecurringExpense> = emptyList(),
        val challenges: List<SavingsChallenge> = emptyList(),
        val achievements: List<Achievement> = emptyList()
    )

    init {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.refreshAgeFromBirthday()
            // Initialize auto-deposit scheduling
            val autoDepositsEnabled = preferencesRepository.getAutoDepositsEnabled()
            val checkHour = preferencesRepository.getAutoDepositCheckHour()
            vaultAutoDepositScheduler.schedule(autoDepositsEnabled, checkHour)
        }
        viewModelScope.launch {
            combine(
                savingsRepository.observeExpenses(),
                savingsRepository.observeGoals(),
                savingsRepository.observeTransfers(),
                preferencesRepository.settingsFlow,
                preferencesRepository.smartTransferFlow
            ) { expenses, goals, transfers, settings, smartSnapshot ->
                AggregatedFeeds(
                    expenses = expenses,
                    goals = goals,
                    transfers = transfers,
                    settings = settings,
                    smartSnapshot = smartSnapshot
                )
            }
                .combine(savingsRepository.observeSmartVaults()) { feed, vaults ->
                    feed.copy(vaults = vaults)
                }
                .combine(savingsRepository.observeBudgets()) { feed, budgets ->
                    feed.copy(budgets = budgets)
                }
                .combine(savingsRepository.observeRecurringExpenses()) { feed, recurring ->
                    feed.copy(recurring = recurring)
                }
                .combine(savingsRepository.observeChallenges()) { feed, challenges ->
                    feed.copy(challenges = challenges)
                }
                .combine(savingsRepository.observeAchievements()) { feed, achievements ->
                    feed.copy(achievements = achievements)
                }
                .combine(preferencesRepository.onboardingCompletedFlow) { feed, onboardingCompleted ->
                    feed to onboardingCompleted
                }
                .combine(preferencesRepository.autoDepositCheckHourFlow) { (feed, onboardingCompleted), autoDepositCheckHour ->
                    Triple(feed, onboardingCompleted, autoDepositCheckHour)
                }
                .catch { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message
                    )
                }
                .collect { (feed, onboardingCompleted, autoDepositCheckHour) ->
                    val domainExpenses = feed.expenses.map { it.toDomain() }
                    val domainTransfers = feed.transfers.map { it.toDomain() }
                    val domainGoals = GoalPlanner.toDomainGoals(feed.goals, domainExpenses, domainTransfers)
                    val analytics = AnalyticsEngine.build(domainExpenses, domainTransfers)

                    val currentMonth = YearMonth.now()
                    val activeBudgets = feed.budgets.filter { it.isActive && it.yearMonth == currentMonth }
                    val budgetSummary: BudgetSummary? = if (activeBudgets.isNotEmpty()) {
                        BudgetEngine.generateBudgetSummary(activeBudgets, domainExpenses, currentMonth)
                    } else {
                        null
                    }
                    val budgetSuggestions = BudgetEngine.suggestBudgetAdjustments(activeBudgets, domainExpenses, feed.settings)
                    val budgetPrompts = budgetSummary?.let {
                        BudgetEngine.detectBudgetPrompts(it, domainExpenses, budgetSuggestions, feed.settings)
                            .filterNot { handledBudgetPrompts.contains(promptKey(it.category, it.month)) }
                    }.orEmpty()
                    val budgetAlerts = budgetSummary?.let { BudgetEngine.generateBudgetAlerts(it) }.orEmpty()

                    val recommendationBundle = recommendationEngine.generate(
                        expenses = domainExpenses,
                        transfers = domainTransfers,
                        settings = feed.settings,
                        autoTune = feed.settings.autoRecommendationsEnabled
                    )
                    val recommendation = if (feed.settings.autoRecommendationsEnabled) recommendationBundle else null
                    val plan = recommendationBundle.savingsPlan

                    notificationScheduler.schedule(feed.settings)

                    val alerts = AlertsGenerator.buildAlerts(analytics, recommendation, feed.settings, domainGoals)
                    val smartTransfer = SmartTransferEngine.evaluate(feed.smartSnapshot)
                    var combinedAlerts = (alerts + budgetAlerts)
                        .distinctBy { it.title }
                        .sortedByDescending { it.priority }
                    feed.settings.takeIf { it.isBirthdayToday }?.let { settings ->
                        val today = LocalDate.now()
                        if (lastBirthdayGreetingDate != today) {
                            val message = AlertMessage(
                                title = "Happy birthday " + (settings.displayName ?: "!"),
                                description = "We boosted your celebrations by keeping spending tips fresh. Enjoy your day!",
                                type = AlertType.SUCCESS,
                                priority = 12,
                                actionable = false
                            )
                            combinedAlerts = (combinedAlerts + message)
                                .distinctBy { it.title }
                                .sortedByDescending { it.priority }
                            lastBirthdayGreetingDate = today
                        }
                    }

                    val upcomingRecurring = computeUpcomingRecurring(feed.recurring)
                    val enrichedChallenges = feed.challenges.map { challenge ->
                        val streak = ChallengeEngine.calculateStreak(challenge, domainExpenses)
                        if (challenge.streakDays != streak) {
                            challenge.copy(streakDays = streak)
                        } else {
                            challenge
                        }
                    }
                    val achievements = feed.achievements.sortedByDescending { it.earnedDate }

                    processedAchievementTitles.retainAll(achievements.map { it.title }.toSet())

                    val newAchievements = ChallengeEngine
                        .checkForNewAchievements(analytics, domainGoals, enrichedChallenges, achievements)
                        .filter { processedAchievementTitles.add(it.title) }
                    if (newAchievements.isNotEmpty()) {
                        viewModelScope.launch(dispatcher) {
                            savingsRepository.upsertAchievements(newAchievements)
                        }
                    }

                    val financialHealthScore = FinancialHealthEngine.calculateHealthScore(
                        expenses = domainExpenses,
                        transfers = domainTransfers,
                        goals = domainGoals,
                        budgetSummary = budgetSummary,
                        settings = feed.settings,
                        analytics = analytics
                    )

                    val totalVaultBalance = feed.vaults.sumOf { it.currentBalance }
                    val smartSavingSummary = buildSmartSavingSummary(feed.settings, analytics, recommendation)
                    val detectedRecurring = detectRecurringTransactions(domainExpenses)
                    val pendingContributions = savingsRepository.getPendingVaultContributions()

                    val automationActive = feed.settings.paySchedule.dynamicSaveRateEnabled || feed.settings.dynamicSavingTaxEnabled
                    var automationNotes: List<String> = emptyList()
                    var automatedSaveRate: Double? = feed.settings.paySchedule.lastComputedSaveRate
                    var automatedSavingTaxRate: Double? = feed.settings.lastComputedSavingTaxRate
                    if (automationActive) {
                        val referencePay = listOf(
                            feed.settings.paySchedule.lastPayAmount,
                            feed.settings.paySchedule.defaultNetPay,
                            feed.settings.monthlyIncome / (paychecksPerMonth(feed.settings.paySchedule).takeIf { it > 0.0 } ?: 1.0)
                        ).firstOrNull { it > 0.0 }
                        if (referencePay != null && referencePay > 0.0) {
                            val automationInput = IncomeAutomationEngine.Input(
                                currentPayAmount = referencePay,
                                schedule = feed.settings.paySchedule,
                                settings = feed.settings,
                                analytics = analytics,
                                recurringExpenses = feed.recurring
                            )
                            val automationResult = IncomeAutomationEngine.evaluate(automationInput)
                            automationNotes = automationResult.rationale
                            if (feed.settings.paySchedule.dynamicSaveRateEnabled) {
                                automatedSaveRate = automationResult.saveRate
                                if (isMeaningfullyDifferent(feed.settings.paySchedule.lastComputedSaveRate, automationResult.saveRate)) {
                                    val updatedSchedule = feed.settings.paySchedule.copy(lastComputedSaveRate = automationResult.saveRate)
                                    viewModelScope.launch(dispatcher) {
                                        preferencesRepository.updatePaySchedule(updatedSchedule)
                                    }
                                }
                            }
                            if (feed.settings.dynamicSavingTaxEnabled) {
                                automatedSavingTaxRate = automationResult.savingTaxRate
                                if (isMeaningfullyDifferent(feed.settings.lastComputedSavingTaxRate, automationResult.savingTaxRate)) {
                                    viewModelScope.launch(dispatcher) {
                                        preferencesRepository.updateSavingTaxRate(automationResult.savingTaxRate, fromAutomation = true)
                                    }
                                }
                            }
                        } else {
                            automationNotes = listOf("Awaiting paycheck history to compute automation.")
                        }
                    } else {
                        automationNotes = listOf("Automation toggles are off — adjust savings and tax rates manually.")
                    }

                    val activeSaveRate = when {
                        feed.settings.paySchedule.dynamicSaveRateEnabled -> (automatedSaveRate
                            ?: feed.settings.paySchedule.lastComputedSaveRate
                            ?: feed.settings.paySchedule.defaultSaveRate).coerceIn(0.0, 1.0)
                        else -> feed.settings.paySchedule.defaultSaveRate.coerceIn(0.0, 1.0)
                    }
                    val activeSavingTaxRate = when {
                        feed.settings.dynamicSavingTaxEnabled -> (automatedSavingTaxRate
                            ?: feed.settings.lastComputedSavingTaxRate
                            ?: feed.settings.savingTaxRate).coerceIn(0.0, 1.0)
                        else -> feed.settings.savingTaxRate.coerceIn(0.0, 1.0)
                    }

                    _uiState.value = SparelyUiState(
                        settings = feed.settings,
                        expenses = domainExpenses,
                        goals = domainGoals,
                        analytics = analytics,
                        recommendation = recommendation,
                        manualTransfers = domainTransfers,
                        savingsPlan = plan,
                        smartSavingSummary = smartSavingSummary,
                        alerts = combinedAlerts,
                        smartTransfer = smartTransfer,
                        smartVaults = feed.vaults,
                        totalVaultBalance = totalVaultBalance,
                        onboardingCompleted = onboardingCompleted,
                        activeSaveRate = activeSaveRate,
                        activeSavingTaxRate = activeSavingTaxRate,
                        automationRationale = automationNotes,
                        budgets = activeBudgets,
                        budgetSummary = budgetSummary,
                        budgetSuggestions = budgetSuggestions,
                        budgetPrompts = budgetPrompts,
                        recurringExpenses = feed.recurring,
                        upcomingRecurring = upcomingRecurring,
                        activeChallenges = enrichedChallenges,
                        achievements = achievements,
                        financialHealthScore = financialHealthScore,
                        detectedRecurringTransactions = detectedRecurring,
                        pendingVaultContributions = pendingContributions,
                        autoDepositCheckHour = autoDepositCheckHour,
                        isLoading = false,
                        errorMessage = null
                    )
                }
        }
    }
        fun addBudget(input: BudgetInput) {
            viewModelScope.launch(dispatcher) {
                val currentMonth = YearMonth.now()
                val existing = _uiState.value.budgets.firstOrNull { it.category == input.category && it.yearMonth == currentMonth }
                val sanitizedLimit = input.monthlyLimit.coerceAtLeast(0.0)
                val budget = existing?.copy(
                    monthlyLimit = sanitizedLimit,
                    isActive = true
                ) ?: CategoryBudget(
                    category = input.category,
                    monthlyLimit = sanitizedLimit,
                    yearMonth = currentMonth,
                    isActive = true
                )
                savingsRepository.upsertBudget(budget)
            }
        }

        fun updateBudget(budget: CategoryBudget) {
            viewModelScope.launch(dispatcher) {
                savingsRepository.upsertBudget(budget.copy(monthlyLimit = budget.monthlyLimit.coerceAtLeast(0.0)))
            }
        }

        fun deleteBudget(id: Long) {
            if (id == 0L) return
            viewModelScope.launch(dispatcher) {
                savingsRepository.deleteBudget(id)
            }
        }

        fun addRecurringExpense(input: RecurringExpenseInput) {
            viewModelScope.launch(dispatcher) {
                val expense = RecurringExpense(
                    description = input.description.trim().ifEmpty { "Recurring payment" },
                    amount = input.amount.coerceAtLeast(0.0),
                    category = input.category,
                    frequency = input.frequency,
                    startDate = input.startDate,
                    endDate = input.endDate,
                    autoLog = input.autoLog,
                    reminderDaysBefore = input.reminderDaysBefore.coerceAtLeast(0),
                    notes = input.notes
                )
                savingsRepository.upsertRecurringExpense(expense)
            }
        }

        fun updateRecurringExpense(expense: RecurringExpense) {
            viewModelScope.launch(dispatcher) {
                val sanitized = expense.copy(amount = expense.amount.coerceAtLeast(0.0))
                savingsRepository.upsertRecurringExpense(sanitized)
            }
        }

        fun deleteRecurringExpense(id: Long) {
            if (id == 0L) return
            viewModelScope.launch(dispatcher) {
                savingsRepository.deleteRecurringExpense(id)
            }
        }

        fun toggleRecurringActive(id: Long, active: Boolean) {
            val current = _uiState.value.recurringExpenses.firstOrNull { it.id == id } ?: return
            updateRecurringExpense(current.copy(isActive = active))
        }

        fun markRecurringProcessed(id: Long) {
            viewModelScope.launch(dispatcher) {
                savingsRepository.updateRecurringExpenseProcessed(id, LocalDate.now())
            }
        }

        fun startChallenge(input: ChallengeInput) {
            viewModelScope.launch(dispatcher) {
                val startDate = LocalDate.now()
                val resolved = when (input.type) {
                    ChallengeType.FIFTY_TWO_WEEK -> ChallengeEngine.createFiftyTwoWeekChallenge(startDate)
                    ChallengeType.DAILY_SAVINGS -> {
                        val rawDuration = ChronoUnit.DAYS.between(startDate, input.endDate)
                        val durationDays = when {
                            rawDuration < 1 -> 30
                            else -> rawDuration.toInt()
                        }.coerceAtLeast(1)
                        val dailyAmount = if (durationDays > 0) input.targetAmount / durationDays else input.targetAmount
                        ChallengeEngine.createDailySavingsChallenge(dailyAmount.coerceAtLeast(1.0), durationDays, startDate)
                    }
                    else -> SavingsChallenge(
                        type = input.type,
                        title = input.title,
                        description = input.description,
                        targetAmount = input.targetAmount,
                        startDate = startDate,
                        endDate = input.endDate
                    )
                }
                val challenge = resolved.copy(
                    title = input.title,
                    description = input.description,
                    targetAmount = input.targetAmount,
                    endDate = input.endDate
                )
                savingsRepository.upsertSavingsChallenge(challenge)
            }
        }

        private fun computeUpcomingRecurring(
            recurring: List<RecurringExpense>,
            today: LocalDate = LocalDate.now()
        ): List<UpcomingRecurringExpense> {
            val windowDays = 30
            return recurring
                .filter { it.isActive }
                .mapNotNull { expense ->
                    val frequencyDays = expense.frequency.daysInterval.toLong().coerceAtLeast(1)
                    var nextDue = expense.lastProcessedDate?.plusDays(frequencyDays) ?: expense.startDate
                    if (nextDue.isBefore(today)) {
                        val daysDiff = ChronoUnit.DAYS.between(nextDue, today)
                        val increments = (daysDiff / frequencyDays) + 1
                        nextDue = nextDue.plusDays(increments * frequencyDays)
                    }
                    expense.endDate?.let { end ->
                        if (nextDue.isAfter(end)) return@mapNotNull null
                    }
                    val daysUntilDue = ChronoUnit.DAYS.between(today, nextDue).toInt()
                    if (daysUntilDue < 0 || daysUntilDue > windowDays) return@mapNotNull null
                    UpcomingRecurringExpense(expense, nextDue, daysUntilDue)
                }
                .sortedBy { it.dueDate }
        }

    private fun buildSmartSavingSummary(
        settings: SparelySettings,
        analytics: AnalyticsSnapshot,
        recommendation: RecommendationResult?
    ): SmartSavingSummary {
        val monthlyIncome = settings.monthlyIncome.coerceAtLeast(0.0)
        val actualRate = if (monthlyIncome > 0.0) {
            (analytics.averageMonthlyReserve / monthlyIncome).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        val recommendedSplit = recommendation?.recommendedPercentages ?: settings.defaultPercentages
        return SmartSavingSummary(
            targetSavingsRate = settings.targetSavingsRate,
            actualSavingsRate = actualRate,
            allocationMode = settings.smartAllocationMode,
            recommendedSplit = recommendedSplit,
            manualSplit = settings.defaultPercentages
        )
    }

    private fun detectRecurringTransactions(expenses: List<Expense>): List<DetectedRecurringTransaction> {
        if (expenses.isEmpty()) return emptyList()
        val windowStart = LocalDate.now().minusDays(180)
        val grouped = expenses
            .filter { !it.date.isBefore(windowStart) }
            .groupBy { it.description.trim().lowercase() }
        return grouped.values.mapNotNull { cluster ->
            if (cluster.size < 3) return@mapNotNull null
            val sorted = cluster.sortedBy { it.date }
            val intervals = sorted.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.date, b.date).toInt() }
            val positiveIntervals = intervals.filter { it > 0 }
            if (positiveIntervals.isEmpty()) return@mapNotNull null
            val cadence = positiveIntervals.average().takeIf { !it.isNaN() } ?: return@mapNotNull null
            val roundedCadence = cadence.roundToInt().coerceAtLeast(1)
            val averageAmount = cluster.map { it.amount }.average()
            DetectedRecurringTransaction(
                description = sorted.first().description,
                averageAmount = averageAmount,
                cadenceDays = roundedCadence,
                lastOccurrence = sorted.last().date,
                suggestedCategory = sorted.first().category
            )
        }
            .sortedBy { it.cadenceDays }
            .take(8)
    }

    fun addExpense(input: ExpenseInput) {
        viewModelScope.launch(dispatcher) {
            val currentState = _uiState.value
            val settings = currentState.settings
            val recommendedPercentages = when {
                input.manualPercentages != null -> input.manualPercentages
                settings.autoRecommendationsEnabled && currentState.recommendation != null ->
                    currentState.recommendation.recommendedPercentages
                else -> settings.defaultPercentages
            }
            val allocation = SavingsCalculator.calculateAllocation(input, recommendedPercentages, settings.riskLevel)
            val applied = recommendedPercentages.adjustWithinBudget()
            val entity = ExpenseEntity(
                id = input.id ?: 0L,
                description = input.description.trim().ifEmpty { "General purchase" },
                amount = input.amount,
                category = input.category,
                date = input.date,
                includesTax = input.includesTax,
                emergencyAmount = allocation.emergencyAmount,
                investmentAmount = allocation.investmentAmount,
                funAmount = allocation.funAmount,
                safeInvestmentAmount = allocation.safeInvestmentAmount,
                highRiskInvestmentAmount = allocation.highRiskInvestmentAmount,
                autoRecommended = input.manualPercentages == null && settings.autoRecommendationsEnabled,
                appliedPercentEmergency = applied.emergency,
                appliedPercentInvest = applied.invest,
                appliedPercentFun = applied.`fun`,
                appliedSafeSplit = applied.safeInvestmentSplit,
                riskLevelUsed = settings.riskLevel
            )
            savingsRepository.upsertExpense(entity)
            if (input.id == null || input.id == 0L) {
                preferencesRepository.registerExpenseForSmartTransfer(
                    emergencyAmount = allocation.emergencyAmount,
                    investmentAmount = allocation.investmentAmount
                )
                val savingTaxPlans = SavingTaxEngine.calculate(
                    SavingTaxEngine.Context(
                        expenseAmount = input.amount,
                        expenseDate = input.date,
                        settings = settings,
                        vaults = currentState.smartVaults
                    )
                )
                if (savingTaxPlans.isNotEmpty()) {
                    val contributions = savingTaxPlans.map { plan ->
                        VaultContribution(
                            vaultId = plan.vaultId,
                            amount = plan.amount,
                            date = input.date,
                            source = VaultContributionSource.SAVING_TAX,
                            note = "Saving tax from ${input.description}".take(120)
                        )
                    }
                    savingsRepository.logVaultContributions(contributions)
                }
            }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch(dispatcher) {
            savingsRepository.findExpenseById(id)?.let { savingsRepository.deleteExpense(it) }
        }
    }

    fun resetHistory(clearGoals: Boolean = false) {
        viewModelScope.launch(dispatcher) {
            savingsRepository.clearExpenses()
            savingsRepository.clearTransfers()
            preferencesRepository.clearSmartTransferState()
            if (clearGoals) {
                savingsRepository.clearGoals()
            }
        }
    }

    fun addGoal(input: GoalInput) {
        viewModelScope.launch(dispatcher) {
            val entity = GoalEntity(
                title = input.title.trim().ifEmpty { "New goal" },
                targetAmount = input.targetAmount,
                category = input.category,
                targetDate = input.targetDate,
                createdAt = LocalDate.now(),
                notes = input.notes,
                archived = false
            )
            savingsRepository.upsertGoal(entity)
        }
    }

    fun toggleGoalArchived(goalId: Long, archived: Boolean) {
        viewModelScope.launch(dispatcher) {
            val goal = savingsRepository.findGoalById(goalId) ?: return@launch
            savingsRepository.upsertGoal(goal.copy(archived = archived))
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch(dispatcher) {
            savingsRepository.findGoalById(goalId)?.let { savingsRepository.deleteGoal(it) }
        }
    }

    fun updatePercentages(percentages: SavingsPercentages) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updatePercentages(percentages)
        }
    }

    fun toggleAutoMode(enabled: Boolean) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.toggleAutoRecommendations(enabled)
        }
    }

    fun updateRiskLevel(riskLevel: RiskLevel) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateRiskLevel(riskLevel)
        }
    }

    fun updateIncludeTax(defaultIncludeTax: Boolean) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateIncludeTax(defaultIncludeTax)
        }
    }

    fun updateMonthlyIncome(income: Double) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateMonthlyIncome(income)
        }
    }

    fun updatePaySchedule(schedule: PayScheduleSettings) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updatePaySchedule(schedule)
        }
    }

    fun recordPaycheck(
        amount: Double,
        payday: LocalDate,
        autoDistribute: Boolean,
        createPendingTransfers: Boolean
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch(dispatcher) {
            val settingsSnapshot = preferencesRepository.getSettingsSnapshot()
            val schedule = settingsSnapshot.paySchedule
            val normalizedAmount = amount.toCurrencyPrecision()
            var mutatedSchedule = schedule

            var automationSaveRate = (schedule.lastComputedSaveRate ?: schedule.defaultSaveRate).coerceIn(0.0, 1.0)
            if (schedule.dynamicSaveRateEnabled || settingsSnapshot.dynamicSavingTaxEnabled) {
                val automationInput = IncomeAutomationEngine.Input(
                    currentPayAmount = normalizedAmount,
                    schedule = schedule,
                    settings = settingsSnapshot,
                    analytics = _uiState.value.analytics,
                    recurringExpenses = _uiState.value.recurringExpenses
                )
                val recommendation = IncomeAutomationEngine.evaluate(automationInput)
                if (schedule.dynamicSaveRateEnabled) {
                    val recommendedRate = recommendation.saveRate.coerceIn(0.0, 1.0)
                    automationSaveRate = recommendedRate
                    if (isMeaningfullyDifferent(schedule.lastComputedSaveRate, recommendedRate)) {
                        mutatedSchedule = mutatedSchedule.copy(lastComputedSaveRate = recommendedRate)
                    }
                }
                if (settingsSnapshot.dynamicSavingTaxEnabled && isMeaningfullyDifferent(settingsSnapshot.lastComputedSavingTaxRate, recommendation.savingTaxRate)) {
                    preferencesRepository.updateSavingTaxRate(recommendation.savingTaxRate, fromAutomation = true)
                }
            }

            if (autoDistribute) {
                val saveRate = if (schedule.dynamicSaveRateEnabled) {
                    automationSaveRate
                } else {
                    schedule.defaultSaveRate.coerceIn(0.0, 1.0)
                }
                val amountForVaults = (normalizedAmount * saveRate).toCurrencyPrecision()
                if (amountForVaults > 0.0) {
                    val planned = planIncomeContributions(amountForVaults, payday, createPendingTransfers)
                    if (planned.isNotEmpty()) {
                        savingsRepository.logVaultContributions(planned)
                        if (createPendingTransfers) {
                            refreshPendingContributions()
                        }
                    }
                }
            }

            val nextDate = if (schedule.trackingMode == IncomeTrackingMode.MANUAL_PER_PAYCHECK) {
                null
            } else {
                computeNextPayDate(schedule, payday)
            }

            mutatedSchedule = mutatedSchedule.copy(
                lastPayDate = payday,
                lastPayAmount = normalizedAmount,
                nextPayDate = nextDate
            )
            preferencesRepository.updatePaySchedule(mutatedSchedule)
        }
    }

    fun updateTargetSavingsRate(rate: Double) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateTargetSavingsRate(rate)
        }
    }

    fun updateSmartAllocationMode(mode: SmartAllocationMode) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateSmartAllocationMode(mode)
        }
    }

    fun updateVaultAllocationMode(mode: VaultAllocationMode) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateVaultAllocationMode(mode)
        }
    }

    fun updateSavingTaxRate(rate: Double) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateSavingTaxRate(rate)
        }
    }

    fun updateDynamicSavingTaxEnabled(enabled: Boolean) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateDynamicSavingTaxEnabled(enabled)
        }
    }
    
    fun addSmartVault(vault: SmartVault) {
        viewModelScope.launch(dispatcher) {
            savingsRepository.upsertSmartVault(vault.copy(id = 0L))
        }
    }
    
    fun updateSmartVault(vault: SmartVault) {
        viewModelScope.launch(dispatcher) {
            savingsRepository.upsertSmartVault(vault)
        }
    }
    
    fun deleteSmartVault(vaultId: Long) {
        if (vaultId == 0L) return
        viewModelScope.launch(dispatcher) {
            savingsRepository.deleteSmartVault(vaultId)
        }
    }
    
    fun reconcileVaultContribution(contributionId: Long) {
        viewModelScope.launch(dispatcher) {
            savingsRepository.reconcileVaultContribution(contributionId)
            refreshPendingContributions()
        }
    }
    
    fun updateAutoDepositsEnabled(enabled: Boolean) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateAutoDepositsEnabled(enabled)
            val checkHour = preferencesRepository.getAutoDepositCheckHour()
            vaultAutoDepositScheduler.schedule(enabled, checkHour)
        }
    }
    
    fun updateAutoDepositCheckHour(hour: Int) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateAutoDepositCheckHour(hour)
            val enabled = preferencesRepository.getAutoDepositsEnabled()
            if (enabled) {
                vaultAutoDepositScheduler.schedule(enabled, hour)
            }
        }
    }
    
    fun triggerManualAutoDepositCheck() {
        vaultAutoDepositScheduler.runImmediateCheck()
    }
    
    private suspend fun refreshPendingContributions() {
        val pending = savingsRepository.getPendingVaultContributions()
        _uiState.value = _uiState.value.copy(pendingVaultContributions = pending)
    }

    private fun buildPendingVaultBreakdown(): List<Pair<String, Double>> {
        val state = _uiState.value
        if (state.pendingVaultContributions.isEmpty()) return emptyList()
        val vaultLookup = state.smartVaults.associateBy { it.id }
        return state.pendingVaultContributions
            .groupBy { it.vaultId }
            .mapNotNull { (vaultId, contributions) ->
                val total = contributions.sumOf { it.amount }
                if (total <= 0.0) {
                    null
                } else {
                    val name = vaultLookup[vaultId]?.name ?: "Vault ${vaultId}"
                    name to total
                }
            }
            .sortedByDescending { it.second }
    }

    fun reconcileVaultContributions(contributionIds: List<Long>) {
        if (contributionIds.isEmpty()) return
        viewModelScope.launch(dispatcher) {
            savingsRepository.reconcileVaultContributions(contributionIds)
            refreshPendingContributions()
        }
    }

    fun startVaultTransferNotificationWorkflow() {
        viewModelScope.launch(dispatcher) {
            notificationScheduler.showVaultTransferWorkflow(container)
        }
    }

    fun logManualTransfer(
        category: SavingsCategory,
        amount: Double,
        sourceAccountId: Long? = null,
        destinationAccountId: Long? = null,
        note: String? = null
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch(dispatcher) {
            val transfer = SavingsTransferEntity(
                category = category,
                amount = amount.toCurrencyPrecision(),
                date = LocalDate.now(),
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
                note = note
            )
            savingsRepository.logTransfer(transfer)
        }
    }

    fun confirmSmartTransfer() {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.beginSmartTransferConfirmation()
            val snapshot = preferencesRepository.getSmartTransferSnapshot()
            val recommendation = SmartTransferEngine.evaluate(snapshot)
            if (recommendation != null && recommendation.status == SmartTransferStatus.AWAITING_CONFIRMATION) {
                val vaultBreakdown = buildPendingVaultBreakdown()
                notificationScheduler.showSmartTransferTracker(recommendation, vaultBreakdown)
            }
        }
    }

    fun completeSmartTransfer() {
        viewModelScope.launch(dispatcher) {
            val (emergency, investment) = preferencesRepository.completeSmartTransferConfirmation()
            if (emergency > 0.0) {
                savingsRepository.logTransfer(
                    SavingsTransferEntity(
                        category = SavingsCategory.EMERGENCY,
                        amount = emergency.toCurrencyPrecision(),
                        date = LocalDate.now()
                    )
                )
            }
            if (investment > 0.0) {
                savingsRepository.logTransfer(
                    SavingsTransferEntity(
                        category = SavingsCategory.INVESTMENT,
                        amount = investment.toCurrencyPrecision(),
                        date = LocalDate.now()
                    )
                )
            }
            notificationScheduler.dismissSmartTransferTracker()
        }
    }

    fun cancelSmartTransfer(returnToPending: Boolean) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.cancelSmartTransferConfirmation(returnToPending)
            if (!returnToPending) {
                preferencesRepository.clearSmartTransferState()
            }
            notificationScheduler.dismissSmartTransferTracker()
        }
    }

    fun snoozeSmartTransfer() {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.snoozeSmartTransfer()
        }
    }

    fun dismissSmartTransfer() {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.clearSmartTransferState()
            notificationScheduler.dismissSmartTransferTracker()
        }
    }

    private fun planIncomeContributions(
        amount: Double,
        payday: LocalDate,
        createPending: Boolean
    ): List<VaultContribution> {
        if (amount <= 0.0) return emptyList()
        val state = _uiState.value
        val vaults = state.smartVaults.filter { !it.archived }
        if (vaults.isEmpty()) return emptyList()
        val weights = DynamicAllocationEngine.calculateWeights(
            vaults = vaults,
            globalMode = state.settings.vaultAllocationMode,
            today = payday
        )
        if (weights.isEmpty()) return emptyList()
        val sortedWeights = weights.sortedByDescending { it.weight }
        val contributions = mutableListOf<VaultContribution>()
        var allocated = 0.0
        sortedWeights.forEachIndexed { index, weight ->
            val rawShare = amount * weight.weight
            val share = if (index == sortedWeights.lastIndex) {
                (amount - allocated).coerceAtLeast(0.0)
            } else {
                rawShare
            }
            val rounded = share.toCurrencyPrecision()
            if (rounded > 0.0) {
                allocated += rounded
                contributions.add(
                    VaultContribution(
                        vaultId = weight.vaultId,
                        amount = rounded,
                        date = payday,
                        source = VaultContributionSource.INCOME,
                        note = "Paycheck allocation",
                        reconciled = !createPending
                    )
                )
            }
        }
        return contributions
    }

    private fun computeNextPayDate(schedule: PayScheduleSettings, lastPayDate: LocalDate): LocalDate? {
        return when (schedule.interval) {
            PayInterval.WEEKLY -> nextScheduledWeekday(lastPayDate, schedule.weeklyDayOfWeek, 7)
            PayInterval.BIWEEKLY -> nextScheduledWeekday(lastPayDate, schedule.weeklyDayOfWeek, 14)
            PayInterval.SEMI_MONTHLY -> nextSemiMonthlyDate(lastPayDate, schedule.semiMonthlyDay1, schedule.semiMonthlyDay2)
            PayInterval.MONTHLY -> nextMonthlyDate(lastPayDate, schedule.monthlyDay)
            PayInterval.CUSTOM -> schedule.customDaysBetween?.let { lastPayDate.plusDays(it.toLong()) }
        }
    }

    private fun nextScheduledWeekday(baseDate: LocalDate, target: java.time.DayOfWeek, minimumDays: Long): LocalDate {
        var candidate = baseDate.plusDays(minimumDays.coerceAtLeast(1))
        repeat(7) {
            if (candidate.dayOfWeek == target) {
                return candidate
            }
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun nextSemiMonthlyDate(baseDate: LocalDate, day1: Int, day2: Int): LocalDate {
        val ordered = listOf(day1, day2).map { it.coerceIn(1, 28) }.sorted()
        val currentMonth = YearMonth.from(baseDate)
        val currentDay = baseDate.dayOfMonth
        val withinMonth = ordered.firstOrNull { it > currentDay }
        return if (withinMonth != null) {
            val clamped = withinMonth.coerceAtMost(currentMonth.lengthOfMonth())
            currentMonth.atDay(clamped)
        } else {
            val nextMonth = currentMonth.plusMonths(1)
            val clamped = ordered.first().coerceAtMost(nextMonth.lengthOfMonth())
            nextMonth.atDay(clamped)
        }
    }

    private fun nextMonthlyDate(baseDate: LocalDate, desiredDay: Int): LocalDate {
        val nextMonth = YearMonth.from(baseDate).plusMonths(1)
        val clamped = desiredDay.coerceIn(1, nextMonth.lengthOfMonth())
        return nextMonth.atDay(clamped)
    }

    fun updateAge(age: Int) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateAge(age)
        }
    }

    fun updateEducationStatus(status: EducationStatus) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateEducationStatus(status)
        }
    }

    fun updateEmploymentStatus(status: EmploymentStatus) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateEmploymentStatus(status)
        }
    }

    fun updateHasDebts(hasDebts: Boolean) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateHasDebts(hasDebts)
        }
    }

    fun updateEmergencyFund(amount: Double) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateEmergencyFund(amount)
        }
    }

    fun updatePrimaryGoal(goal: String?) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updatePrimaryGoal(goal)
        }
    }

    fun updateDisplayName(name: String?) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateDisplayName(name)
        }
    }

    fun updateBirthday(date: LocalDate?) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateBirthday(date)
            preferencesRepository.refreshAgeFromBirthday()
        }
    }

    fun updateReminderSettings(enabled: Boolean, hour: Int, frequencyDays: Int) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateReminders(enabled, hour, frequencyDays)
            if (enabled) {
                notificationScheduler.schedule(_uiState.value.settings.copy(remindersEnabled = true, reminderHour = hour, reminderFrequencyDays = frequencyDays))
            } else {
                notificationScheduler.cancel()
            }
        }
    }

    fun completeOnboarding(profile: UserProfileSetup) {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.updateMonthlyIncome(profile.monthlyIncome)
            preferencesRepository.updateAge(profile.age)
            preferencesRepository.updateRiskLevel(profile.riskLevel)
            preferencesRepository.updateEducationStatus(profile.educationStatus)
            preferencesRepository.updateEmploymentStatus(profile.employmentStatus)
            preferencesRepository.updateHasDebts(profile.hasDebts)
            preferencesRepository.updateEmergencyFund(profile.currentEmergencyFund)
            preferencesRepository.updatePrimaryGoal(profile.primaryGoal)
            preferencesRepository.updateDisplayName(profile.name)
            preferencesRepository.updateBirthday(profile.birthday)
            preferencesRepository.setJoinedDate(profile.joinedDate)
            val resolvedVaultSetups = when {
                profile.smartVaults.isNotEmpty() -> profile.smartVaults
                profile.savingsAccounts.isNotEmpty() -> profile.savingsAccounts.map { it.toSmartVaultSetup(profile.monthlyIncome) }
                else -> SavingsAdvisor.recommendedVaults(profile)
            }
            val recommendedPercentages = SavingsAdvisor.recommendedPercentages(profile)
            preferencesRepository.updatePercentages(recommendedPercentages)
            profile.transferReminder?.let { reminder ->
                preferencesRepository.updateReminders(reminder.enabled, reminder.hourOfDay, reminder.frequencyDays)
            }
            val resolvedVaults = resolvedVaultSetups
                .map { setup -> setup.toSmartVault() }
                .mapIndexed { index, vault ->
                    vault.copy(name = vault.name.ifBlank { "Vault ${index + 1}" })
                }
            savingsRepository.seedSmartVaults(resolvedVaults)
            preferencesRepository.setOnboardingCompleted(true)
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch(dispatcher) {
            preferencesRepository.setOnboardingCompleted(true)
        }
    }

    fun markBudgetPromptAsOneOff(prompt: BudgetOverrunPrompt) {
        handledBudgetPrompts.add(promptKey(prompt))
        _uiState.update { state ->
            state.copy(budgetPrompts = state.budgetPrompts.filterNot { promptKey(it) == promptKey(prompt) })
        }
    }

    fun adjustBudgetFromPrompt(prompt: BudgetOverrunPrompt, newLimit: Double) {
        val sanitized = newLimit.coerceAtLeast(0.0)
        viewModelScope.launch(dispatcher) {
            val existing = _uiState.value.budgets.firstOrNull { it.category == prompt.category && it.yearMonth == prompt.month }
            val budget = existing?.copy(monthlyLimit = sanitized, isActive = true)
                ?: CategoryBudget(
                    category = prompt.category,
                    monthlyLimit = sanitized,
                    yearMonth = prompt.month,
                    isActive = true
                )
            savingsRepository.upsertBudget(budget)
            handledBudgetPrompts.add(promptKey(prompt))
            _uiState.update { state ->
                state.copy(budgetPrompts = state.budgetPrompts.filterNot { promptKey(it) == promptKey(prompt) })
            }
        }
    }

    fun applySuggestedBudgetFromPrompt(prompt: BudgetOverrunPrompt) {
        val suggested = prompt.suggestion?.suggestedLimit ?: return
        adjustBudgetFromPrompt(prompt, suggested)
    }

    private fun ExpenseEntity.toDomain(): Expense {
        val percentages = SavingsPercentages(
            emergency = appliedPercentEmergency,
            invest = appliedPercentInvest,
            `fun` = appliedPercentFun,
            safeInvestmentSplit = appliedSafeSplit
        )
        return Expense(
            id = id,
            description = description,
            amount = amount,
            category = category,
            date = date,
            includesTax = includesTax,
            allocation = AllocationBreakdown(
                emergencyAmount = emergencyAmount,
                investmentAmount = investmentAmount,
                funAmount = funAmount,
                safeInvestmentAmount = safeInvestmentAmount,
                highRiskInvestmentAmount = highRiskInvestmentAmount
            ),
            appliedPercentages = percentages,
            autoRecommended = autoRecommended,
            riskLevelUsed = riskLevelUsed
        )
    }

    private fun Double.toCurrencyPrecision(): Double = round(this * 100) / 100.0
}

class SparelyViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SparelyViewModel::class.java)) {
            return SparelyViewModel(
                savingsRepository = container.savingsRepository,
                preferencesRepository = container.preferencesRepository,
                recommendationEngine = container.recommendationEngine,
                notificationScheduler = container.notificationScheduler,
                vaultAutoDepositScheduler = container.vaultAutoDepositScheduler,
                container = container
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
    }
}
