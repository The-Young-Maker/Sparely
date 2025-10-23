package com.example.sparely.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.sparely.domain.model.EducationStatus
import com.example.sparely.domain.model.EmploymentStatus
import com.example.sparely.domain.model.LivingSituation
import com.example.sparely.domain.model.IncomeTrackingMode
import com.example.sparely.domain.model.PayInterval
import com.example.sparely.domain.model.PayScheduleSettings
import com.example.sparely.domain.model.RegionalSettings
import com.example.sparely.domain.model.RiskLevel
import com.example.sparely.domain.model.SmartAllocationMode
import com.example.sparely.domain.model.SparelySettings
import com.example.sparely.domain.model.SavingsPercentages
import com.example.sparely.domain.model.SmartTransferDefaults
import com.example.sparely.domain.model.SmartTransferSnapshot
import com.example.sparely.domain.model.VaultAllocationMode
import com.example.sparely.setAppLocale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

private const val DATASTORE_NAME = "sparely_settings"

val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class UserPreferencesRepository(private val context: Context) {

    val settingsFlow: Flow<SparelySettings> = context.dataStore.data.map { preferences ->
        preferences.toSettings()
    }

    val smartTransferFlow: Flow<SmartTransferSnapshot> = context.dataStore.data.map { preferences ->
        preferences.toSmartTransferSnapshot()
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.onboardingCompleted] ?: false
    }
    
    val autoDepositCheckHourFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.autoDepositCheckHour] ?: 9
    }

    suspend fun getSettingsSnapshot(): SparelySettings = settingsFlow.first()

    suspend fun updatePercentages(percentages: SavingsPercentages) {
        context.dataStore.edit { prefs ->
            val adjusted = percentages.adjustWithinBudget()
            prefs[PreferenceKeys.emergencyPercent] = adjusted.emergency
            prefs[PreferenceKeys.investPercent] = adjusted.invest
            prefs[PreferenceKeys.funPercent] = adjusted.`fun`
            prefs[PreferenceKeys.safeSplit] = adjusted.safeInvestmentSplit
        }
    }

    suspend fun updateRiskLevel(riskLevel: RiskLevel) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.riskLevel] = riskLevel.name
        }
    }

    suspend fun toggleAutoRecommendations(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.autoRecommend] = enabled
        }
    }

    suspend fun updateIncludeTax(includeTax: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.includeTax] = includeTax
        }
    }

    suspend fun updateMonthlyIncome(value: Double) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.monthlyIncome] = value.coerceAtLeast(0.0)
        }
    }

    suspend fun updatePaySchedule(schedule: PayScheduleSettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.incomeTrackingMode] = schedule.trackingMode.name
            prefs[PreferenceKeys.payInterval] = schedule.interval.name
            prefs[PreferenceKeys.payDefaultNet] = schedule.defaultNetPay.coerceAtLeast(0.0)
            prefs[PreferenceKeys.payDefaultSaveRate] = schedule.defaultSaveRate.coerceIn(0.0, 1.0)
            prefs[PreferenceKeys.payWeeklyDay] = schedule.weeklyDayOfWeek.value
            prefs[PreferenceKeys.paySemiDay1] = schedule.semiMonthlyDay1.coerceIn(1, 28)
            prefs[PreferenceKeys.paySemiDay2] = schedule.semiMonthlyDay2.coerceIn(1, 28)
            prefs[PreferenceKeys.payMonthlyDay] = schedule.monthlyDay.coerceIn(1, 28)
            if (schedule.customDaysBetween != null) {
                prefs[PreferenceKeys.payCustomDays] = schedule.customDaysBetween.coerceAtLeast(1)
            } else {
                prefs.remove(PreferenceKeys.payCustomDays)
            }
            if (schedule.nextPayDate != null) {
                prefs[PreferenceKeys.payNextDate] = schedule.nextPayDate.toEpochDay()
            } else {
                prefs.remove(PreferenceKeys.payNextDate)
            }
            if (schedule.lastPayDate != null) {
                prefs[PreferenceKeys.payLastDate] = schedule.lastPayDate.toEpochDay()
            } else {
                prefs.remove(PreferenceKeys.payLastDate)
            }
            prefs[PreferenceKeys.payLastAmount] = schedule.lastPayAmount.coerceAtLeast(0.0)
            prefs[PreferenceKeys.payAutoDistribute] = schedule.autoDistributeToVaults
            prefs[PreferenceKeys.payAutoPending] = schedule.autoCreatePendingTransfers
            prefs[PreferenceKeys.payDynamicSaveEnabled] = schedule.dynamicSaveRateEnabled
            if (schedule.lastComputedSaveRate != null) {
                prefs[PreferenceKeys.payLastComputedSaveRate] = schedule.lastComputedSaveRate.coerceIn(0.0, 1.0)
            } else {
                prefs.remove(PreferenceKeys.payLastComputedSaveRate)
            }
        }
    }

    suspend fun updateTargetSavingsRate(rate: Double) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.targetSavingsRate] = rate.coerceIn(0.0, 1.0)
        }
    }

    suspend fun updateSmartAllocationMode(mode: SmartAllocationMode) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.smartAllocationMode] = mode.name
        }
    }

    suspend fun updateVaultAllocationMode(mode: VaultAllocationMode) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.vaultAllocationMode] = mode.name
        }
    }

    suspend fun updateSavingTaxRate(rate: Double, fromAutomation: Boolean = false) {
        context.dataStore.edit { prefs ->
            val sanitized = rate.coerceIn(0.0, 1.0)
            prefs[PreferenceKeys.savingTaxRate] = sanitized
            if (fromAutomation) {
                prefs[PreferenceKeys.lastComputedSavingTaxRate] = sanitized
            } else {
                prefs.remove(PreferenceKeys.lastComputedSavingTaxRate)
            }
        }
    }

    suspend fun updateDynamicSavingTaxEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.dynamicSavingTaxEnabled] = enabled
        }
    }
    
    suspend fun updateAutoDepositsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.autoDepositsEnabled] = enabled
        }
    }
    
    suspend fun updateAutoDepositCheckHour(hour: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.autoDepositCheckHour] = hour.coerceIn(0, 23)
        }
    }
    
    suspend fun getAutoDepositsEnabled(): Boolean {
        return context.dataStore.data.first()[PreferenceKeys.autoDepositsEnabled] ?: true
    }
    
    suspend fun getAutoDepositCheckHour(): Int {
        return context.dataStore.data.first()[PreferenceKeys.autoDepositCheckHour] ?: 9
    }

    suspend fun updateAge(age: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.age] = age.coerceIn(13, 100)
        }
    }

    suspend fun updateEducationStatus(status: EducationStatus) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.educationStatus] = status.name
        }
    }

    suspend fun updateEmploymentStatus(status: EmploymentStatus) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.employmentStatus] = status.name
        }
    }

    suspend fun updateLivingSituation(situation: LivingSituation) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.livingSituation] = situation.name
        }
    }

    suspend fun updateOccupation(occupation: String?) {
        context.dataStore.edit { prefs ->
            if (occupation.isNullOrBlank()) {
                prefs.remove(PreferenceKeys.occupation)
            } else {
                prefs[PreferenceKeys.occupation] = occupation.trim()
            }
        }
    }

    suspend fun updateMainAccountBalance(balance: Double) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.mainAccountBalance] = balance.coerceAtLeast(0.0)
        }
    }

    suspend fun updateSavingsAccountBalance(balance: Double) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.savingsAccountBalance] = balance.coerceAtLeast(0.0)
        }
    }

    suspend fun updateVaultsBalance(balance: Double) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.vaultsBalance] = balance.coerceAtLeast(0.0)
        }
    }

    suspend fun updateSubscriptionTotal(amount: Double) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.subscriptionTotal] = amount.coerceAtLeast(0.0)
        }
    }

    suspend fun updateHasDebts(hasDebts: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.hasDebts] = hasDebts
        }
    }

    suspend fun updateEmergencyFund(amount: Double) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.emergencyFund] = amount.coerceAtLeast(0.0)
        }
    }

    suspend fun updatePrimaryGoal(goal: String?) {
        context.dataStore.edit { prefs ->
            if (goal.isNullOrBlank()) {
                prefs.remove(PreferenceKeys.primaryGoal)
            } else {
                prefs[PreferenceKeys.primaryGoal] = goal.trim()
            }
        }
    }

    suspend fun updateDisplayName(name: String?) {
        context.dataStore.edit { prefs ->
            if (name.isNullOrBlank()) {
                prefs.remove(PreferenceKeys.displayName)
            } else {
                prefs[PreferenceKeys.displayName] = name.trim()
            }
        }
    }

    suspend fun updateBirthday(date: LocalDate?) {
        context.dataStore.edit { prefs ->
            if (date == null) {
                prefs.remove(PreferenceKeys.birthdayEpochDay)
            } else {
                prefs[PreferenceKeys.birthdayEpochDay] = date.toEpochDay()
                prefs[PreferenceKeys.age] = ChronoUnit.YEARS.between(date, LocalDate.now()).coerceAtLeast(0).toInt()
            }
        }
    }

    suspend fun refreshAgeFromBirthday() {
        context.dataStore.edit { prefs ->
            val epoch = prefs[PreferenceKeys.birthdayEpochDay] ?: return@edit
            val birthday = LocalDate.ofEpochDay(epoch)
            val computed = ChronoUnit.YEARS.between(birthday, LocalDate.now()).coerceAtLeast(0).toInt()
            val stored = prefs[PreferenceKeys.age]
            if (stored == null || stored != computed) {
                prefs[PreferenceKeys.age] = computed
            }
        }
    }
    
    suspend fun updateRegionalSettings(countryCode: String, languageCode: String, currencyCode: String, customTaxRate: Double?) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.countryCode] = countryCode
            prefs[PreferenceKeys.languageCode] = languageCode
            prefs[PreferenceKeys.currencyCode] = currencyCode
            if (customTaxRate != null) {
                prefs[PreferenceKeys.customIncomeTaxRate] = customTaxRate
            } else {
                prefs.remove(PreferenceKeys.customIncomeTaxRate)
            }
        }
        // Apply the new locale immediately
        context.setAppLocale(languageCode)
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.onboardingCompleted] = completed
        }
    }

    suspend fun setJoinedDate(date: LocalDate) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.joinedDateEpochDay] = date.toEpochDay()
        }
    }

    suspend fun updateReminders(enabled: Boolean, hour: Int? = null, frequencyDays: Int? = null) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.remindersEnabled] = enabled
            hour?.let { prefs[PreferenceKeys.reminderHour] = it.coerceIn(0, 23) }
            frequencyDays?.let { prefs[PreferenceKeys.reminderFrequencyDays] = it.coerceAtLeast(1) }
        }
    }

    suspend fun updatePaydayReminder(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        suggestAverage: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.paydayReminderEnabled] = enabled
            prefs[PreferenceKeys.paydayReminderHour] = hour.coerceIn(0, 23)
            prefs[PreferenceKeys.paydayReminderMinute] = minute.coerceIn(0, 59)
            prefs[PreferenceKeys.paydaySuggestAverage] = suggestAverage
        }
    }

    suspend fun recordPaycheckHistory(amount: Double) {
        if (amount <= 0.0) return
        context.dataStore.edit { prefs ->
            val accumulated = prefs[PreferenceKeys.payHistoryTotal] ?: 0.0
            val count = prefs[PreferenceKeys.payHistoryCount] ?: 0
            prefs[PreferenceKeys.payHistoryTotal] = accumulated + amount
            prefs[PreferenceKeys.payHistoryCount] = count + 1
        }
    }

    suspend fun getPayHistoryStats(): PayHistoryStats {
        val prefs = context.dataStore.data.first()
        val count = prefs[PreferenceKeys.payHistoryCount] ?: 0
        val total = prefs[PreferenceKeys.payHistoryTotal] ?: 0.0
        return PayHistoryStats(count, total)
    }

    suspend fun registerExpenseForSmartTransfer(
        emergencyAmount: Double,
        investmentAmount: Double,
        nowEpochMillis: Long = System.currentTimeMillis()
    ) {
        context.dataStore.edit { prefs ->
            val emergencyCents = (emergencyAmount * 100).roundToLong()
            val investmentCents = (investmentAmount * 100).roundToLong()
            val currentEmergency = prefs[PreferenceKeys.smartPendingEmergencyCents] ?: 0L
            val currentInvestment = prefs[PreferenceKeys.smartPendingInvestmentCents] ?: 0L
            val currentCount = prefs[PreferenceKeys.smartPendingExpenseCount] ?: 0

            prefs[PreferenceKeys.smartPendingEmergencyCents] = currentEmergency + emergencyCents
            prefs[PreferenceKeys.smartPendingInvestmentCents] = currentInvestment + investmentCents
            prefs[PreferenceKeys.smartPendingExpenseCount] = currentCount + 1
            prefs[PreferenceKeys.smartLastExpenseEpoch] = nowEpochMillis
            prefs[PreferenceKeys.smartHoldUntilEpoch] = nowEpochMillis + SmartTransferDefaults.BATCH_WINDOW_MILLIS
        }
    }

    suspend fun snoozeSmartTransfer(nowEpochMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            val pending = prefs[PreferenceKeys.smartPendingEmergencyCents] ?: 0L
            val pendingInv = prefs[PreferenceKeys.smartPendingInvestmentCents] ?: 0L
            val awaitingEmergency = prefs[PreferenceKeys.smartAwaitingEmergencyCents] ?: 0L
            val awaitingInvest = prefs[PreferenceKeys.smartAwaitingInvestmentCents] ?: 0L
            val awaitingCount = prefs[PreferenceKeys.smartAwaitingExpenseCount] ?: 0
            val pendingCount = prefs[PreferenceKeys.smartPendingExpenseCount] ?: 0
            if (pending + pendingInv + awaitingEmergency + awaitingInvest <= 0L) return@edit
            prefs[PreferenceKeys.smartHoldUntilEpoch] = nowEpochMillis + SmartTransferDefaults.BATCH_WINDOW_MILLIS
            prefs[PreferenceKeys.smartPendingExpenseCount] = pendingCount
            prefs[PreferenceKeys.smartAwaitingExpenseCount] = awaitingCount
        }
    }

    suspend fun clearSmartTransferState() {
        context.dataStore.edit { prefs ->
            prefs.remove(PreferenceKeys.smartPendingEmergencyCents)
            prefs.remove(PreferenceKeys.smartPendingInvestmentCents)
            prefs.remove(PreferenceKeys.smartPendingExpenseCount)
            prefs.remove(PreferenceKeys.smartHoldUntilEpoch)
            prefs.remove(PreferenceKeys.smartLastExpenseEpoch)
            prefs.remove(PreferenceKeys.smartAwaitingEmergencyCents)
            prefs.remove(PreferenceKeys.smartAwaitingInvestmentCents)
            prefs.remove(PreferenceKeys.smartAwaitingExpenseCount)
            prefs.remove(PreferenceKeys.smartConfirmationStartedEpoch)
        }
    }

    suspend fun getSmartTransferSnapshot(): SmartTransferSnapshot = smartTransferFlow.first()

    suspend fun beginSmartTransferConfirmation(nowEpochMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            val pendingEmergency = prefs[PreferenceKeys.smartPendingEmergencyCents] ?: 0L
            val pendingInvestment = prefs[PreferenceKeys.smartPendingInvestmentCents] ?: 0L
            val pendingCount = prefs[PreferenceKeys.smartPendingExpenseCount] ?: 0
            if (pendingEmergency + pendingInvestment <= 0L) return@edit

            val existingAwaitingEmergency = prefs[PreferenceKeys.smartAwaitingEmergencyCents] ?: 0L
            val existingAwaitingInvestment = prefs[PreferenceKeys.smartAwaitingInvestmentCents] ?: 0L
            val existingAwaitingCount = prefs[PreferenceKeys.smartAwaitingExpenseCount] ?: 0

            prefs[PreferenceKeys.smartAwaitingEmergencyCents] = existingAwaitingEmergency + pendingEmergency
            prefs[PreferenceKeys.smartAwaitingInvestmentCents] = existingAwaitingInvestment + pendingInvestment
            prefs[PreferenceKeys.smartAwaitingExpenseCount] = existingAwaitingCount + pendingCount.coerceAtLeast(1)
            prefs[PreferenceKeys.smartPendingEmergencyCents] = 0L
            prefs[PreferenceKeys.smartPendingInvestmentCents] = 0L
            prefs[PreferenceKeys.smartPendingExpenseCount] = 0
            prefs.remove(PreferenceKeys.smartHoldUntilEpoch)
            prefs[PreferenceKeys.smartConfirmationStartedEpoch] = nowEpochMillis
        }
    }

    suspend fun completeSmartTransferConfirmation(): Pair<Double, Double> {
        var emergencyAmount = 0.0
        var investmentAmount = 0.0
        context.dataStore.edit { prefs ->
            val emergencyCents = prefs[PreferenceKeys.smartAwaitingEmergencyCents] ?: 0L
            val investmentCents = prefs[PreferenceKeys.smartAwaitingInvestmentCents] ?: 0L
            val awaitingCount = prefs[PreferenceKeys.smartAwaitingExpenseCount] ?: 0
            emergencyAmount = emergencyCents.toCurrency()
            investmentAmount = investmentCents.toCurrency()
            prefs[PreferenceKeys.smartAwaitingEmergencyCents] = 0L
            prefs[PreferenceKeys.smartAwaitingInvestmentCents] = 0L
            prefs[PreferenceKeys.smartAwaitingExpenseCount] = 0
            prefs.remove(PreferenceKeys.smartConfirmationStartedEpoch)
            if (awaitingCount > 0) {
                prefs[PreferenceKeys.smartPendingExpenseCount] = 0
            }
        }
        return emergencyAmount to investmentAmount
    }

    suspend fun cancelSmartTransferConfirmation(returnToPending: Boolean = true) {
        context.dataStore.edit { prefs ->
            val awaitingEmergency = prefs[PreferenceKeys.smartAwaitingEmergencyCents] ?: 0L
            val awaitingInvestment = prefs[PreferenceKeys.smartAwaitingInvestmentCents] ?: 0L
            val awaitingCount = prefs[PreferenceKeys.smartAwaitingExpenseCount] ?: 0
            val hasAwaitingAmounts = awaitingEmergency + awaitingInvestment > 0L
            if (returnToPending && hasAwaitingAmounts) {
                val pendingEmergency = prefs[PreferenceKeys.smartPendingEmergencyCents] ?: 0L
                val pendingInvestment = prefs[PreferenceKeys.smartPendingInvestmentCents] ?: 0L
                val pendingCount = prefs[PreferenceKeys.smartPendingExpenseCount] ?: 0
                prefs[PreferenceKeys.smartPendingEmergencyCents] = pendingEmergency + awaitingEmergency
                prefs[PreferenceKeys.smartPendingInvestmentCents] = pendingInvestment + awaitingInvestment
                prefs[PreferenceKeys.smartPendingExpenseCount] = pendingCount + awaitingCount.coerceAtLeast(1)
            }
            prefs[PreferenceKeys.smartAwaitingEmergencyCents] = 0L
            prefs[PreferenceKeys.smartAwaitingInvestmentCents] = 0L
            prefs[PreferenceKeys.smartAwaitingExpenseCount] = 0
            prefs.remove(PreferenceKeys.smartConfirmationStartedEpoch)
        }
    }

    private fun Preferences.toSettings(): SparelySettings {
        val defaults = SparelySettings()
        val scheduleDefaults = defaults.paySchedule

        val emergency = this[PreferenceKeys.emergencyPercent] ?: defaults.defaultPercentages.emergency
        val invest = this[PreferenceKeys.investPercent] ?: defaults.defaultPercentages.invest
        val funPercent = this[PreferenceKeys.funPercent] ?: defaults.defaultPercentages.`fun`
        val safeSplit = this[PreferenceKeys.safeSplit] ?: defaults.defaultPercentages.safeInvestmentSplit
        val riskLevel = this[PreferenceKeys.riskLevel]?.let { runCatching { RiskLevel.valueOf(it) }.getOrNull() }
            ?: defaults.riskLevel
        val autoRecommend = this[PreferenceKeys.autoRecommend] ?: defaults.autoRecommendationsEnabled
        val includeTax = this[PreferenceKeys.includeTax] ?: defaults.includeTaxByDefault
        val income = this[PreferenceKeys.monthlyIncome] ?: defaults.monthlyIncome
        val remindersEnabled = this[PreferenceKeys.remindersEnabled] ?: defaults.remindersEnabled
        val reminderHour = this[PreferenceKeys.reminderHour] ?: defaults.reminderHour
        val reminderFrequency = this[PreferenceKeys.reminderFrequencyDays] ?: defaults.reminderFrequencyDays
    val paydayReminderEnabled = this[PreferenceKeys.paydayReminderEnabled] ?: defaults.paydayReminderEnabled
    val paydayReminderHour = this[PreferenceKeys.paydayReminderHour] ?: defaults.paydayReminderHour
    val paydayReminderMinute = this[PreferenceKeys.paydayReminderMinute] ?: defaults.paydayReminderMinute
    val paydaySuggestAverage = this[PreferenceKeys.paydaySuggestAverage] ?: defaults.paydaySuggestAverageIncome
        val age = this[PreferenceKeys.age] ?: defaults.age
        val joinedDate = this[PreferenceKeys.joinedDateEpochDay]?.let { LocalDate.ofEpochDay(it) }
        val education = this[PreferenceKeys.educationStatus]?.let { runCatching { EducationStatus.valueOf(it) }.getOrNull() }
            ?: defaults.educationStatus
        val employment = this[PreferenceKeys.employmentStatus]?.let { runCatching { EmploymentStatus.valueOf(it) }.getOrNull() }
            ?: defaults.employmentStatus
        val livingSituation = this[PreferenceKeys.livingSituation]?.let { runCatching { LivingSituation.valueOf(it) }.getOrNull() }
            ?: defaults.livingSituation
        val occupation = this[PreferenceKeys.occupation]
        val mainAccountBalance = this[PreferenceKeys.mainAccountBalance] ?: defaults.mainAccountBalance
        val savingsAccountBalance = this[PreferenceKeys.savingsAccountBalance] ?: defaults.savingsAccountBalance
        val vaultsBalance = this[PreferenceKeys.vaultsBalance] ?: defaults.vaultsBalance
        val subscriptionTotal = this[PreferenceKeys.subscriptionTotal] ?: defaults.subscriptionTotal
        val hasDebts = this[PreferenceKeys.hasDebts] ?: defaults.hasDebts
        val emergencyFund = this[PreferenceKeys.emergencyFund] ?: defaults.currentEmergencyFund
        val primaryGoal = this[PreferenceKeys.primaryGoal]
        val displayName = this[PreferenceKeys.displayName]
        val birthday = this[PreferenceKeys.birthdayEpochDay]?.let { LocalDate.ofEpochDay(it) }
        val smartAllocationMode = this[PreferenceKeys.smartAllocationMode]?.let { runCatching { SmartAllocationMode.valueOf(it) }.getOrNull() }
            ?: defaults.smartAllocationMode
        val targetSavingsRate = this[PreferenceKeys.targetSavingsRate] ?: defaults.targetSavingsRate
        val savingTaxRate = this[PreferenceKeys.savingTaxRate] ?: defaults.savingTaxRate
        val vaultAllocationMode = this[PreferenceKeys.vaultAllocationMode]?.let { runCatching { VaultAllocationMode.valueOf(it) }.getOrNull() }
            ?: defaults.vaultAllocationMode
        val dynamicSavingTaxEnabled = this[PreferenceKeys.dynamicSavingTaxEnabled] ?: defaults.dynamicSavingTaxEnabled
        val lastComputedSavingTaxRate = this[PreferenceKeys.lastComputedSavingTaxRate] ?: defaults.lastComputedSavingTaxRate
        val payHistoryCount = this[PreferenceKeys.payHistoryCount] ?: defaults.payHistoryCount
        val payHistoryTotal = this[PreferenceKeys.payHistoryTotal] ?: (defaults.payHistoryAverage * defaults.payHistoryCount)
        val payHistoryAverage = if (payHistoryCount > 0) payHistoryTotal / payHistoryCount else 0.0

        val trackingMode = this[PreferenceKeys.incomeTrackingMode]?.let { runCatching { IncomeTrackingMode.valueOf(it) }.getOrNull() }
            ?: scheduleDefaults.trackingMode
        val payInterval = this[PreferenceKeys.payInterval]?.let { runCatching { PayInterval.valueOf(it) }.getOrNull() }
            ?: scheduleDefaults.interval
        val defaultNetPay = this[PreferenceKeys.payDefaultNet] ?: scheduleDefaults.defaultNetPay
        val defaultSaveRate = this[PreferenceKeys.payDefaultSaveRate] ?: scheduleDefaults.defaultSaveRate
        val weeklyDayRaw = this[PreferenceKeys.payWeeklyDay] ?: scheduleDefaults.weeklyDayOfWeek.value
        val weeklyDay = runCatching { DayOfWeek.of(weeklyDayRaw.coerceIn(1, 7)) }.getOrDefault(scheduleDefaults.weeklyDayOfWeek)
        val semiDay1 = this[PreferenceKeys.paySemiDay1]?.coerceIn(1, 28) ?: scheduleDefaults.semiMonthlyDay1
        val semiDay2 = this[PreferenceKeys.paySemiDay2]?.coerceIn(1, 28) ?: scheduleDefaults.semiMonthlyDay2
        val monthlyDay = this[PreferenceKeys.payMonthlyDay]?.coerceIn(1, 28) ?: scheduleDefaults.monthlyDay
        val customDays = this[PreferenceKeys.payCustomDays]?.coerceAtLeast(1)
        val nextPayDate = this[PreferenceKeys.payNextDate]?.let { LocalDate.ofEpochDay(it) }
        val lastPayDate = this[PreferenceKeys.payLastDate]?.let { LocalDate.ofEpochDay(it) }
        val lastPayAmount = this[PreferenceKeys.payLastAmount] ?: scheduleDefaults.lastPayAmount
        val autoDistribute = this[PreferenceKeys.payAutoDistribute] ?: scheduleDefaults.autoDistributeToVaults
        val autoPending = this[PreferenceKeys.payAutoPending] ?: scheduleDefaults.autoCreatePendingTransfers
    val dynamicSaveEnabled = this[PreferenceKeys.payDynamicSaveEnabled] ?: scheduleDefaults.dynamicSaveRateEnabled
    val lastComputedSaveRate = this[PreferenceKeys.payLastComputedSaveRate] ?: scheduleDefaults.lastComputedSaveRate

        val resolvedAge = birthday?.let {
            ChronoUnit.YEARS.between(it, LocalDate.now()).coerceAtLeast(0).toInt()
        } ?: age
        
        // Read regional settings from preferences
        val regionalDefaults = RegionalSettings()
        val regionalSettings = RegionalSettings(
            countryCode = this[PreferenceKeys.countryCode] ?: regionalDefaults.countryCode,
            languageCode = this[PreferenceKeys.languageCode] ?: regionalDefaults.languageCode,
            currencyCode = this[PreferenceKeys.currencyCode] ?: regionalDefaults.currencyCode,
            customIncomeTaxRate = this[PreferenceKeys.customIncomeTaxRate]
        )

        return SparelySettings(
            defaultPercentages = SavingsPercentages(
                emergency = emergency,
                invest = invest,
                `fun` = funPercent,
                safeInvestmentSplit = safeSplit
            ),
            age = resolvedAge,
            educationStatus = education,
            employmentStatus = employment,
            livingSituation = livingSituation,
            occupation = occupation?.takeIf { it.isNotBlank() },
            mainAccountBalance = mainAccountBalance,
            savingsAccountBalance = savingsAccountBalance,
            vaultsBalance = vaultsBalance,
            subscriptionTotal = subscriptionTotal,
            riskLevel = riskLevel,
            autoRecommendationsEnabled = autoRecommend,
            includeTaxByDefault = includeTax,
            monthlyIncome = income,
            paySchedule = PayScheduleSettings(
                trackingMode = trackingMode,
                interval = payInterval,
                defaultNetPay = defaultNetPay,
                defaultSaveRate = defaultSaveRate,
                weeklyDayOfWeek = weeklyDay,
                semiMonthlyDay1 = semiDay1,
                semiMonthlyDay2 = semiDay2,
                monthlyDay = monthlyDay,
                customDaysBetween = customDays,
                nextPayDate = nextPayDate,
                lastPayDate = lastPayDate,
                lastPayAmount = lastPayAmount,
                autoDistributeToVaults = autoDistribute,
                autoCreatePendingTransfers = autoPending,
                dynamicSaveRateEnabled = dynamicSaveEnabled,
                lastComputedSaveRate = lastComputedSaveRate
            ),
            remindersEnabled = remindersEnabled,
            reminderHour = reminderHour,
            reminderFrequencyDays = reminderFrequency,
            paydayReminderEnabled = paydayReminderEnabled,
            paydayReminderHour = paydayReminderHour,
            paydayReminderMinute = paydayReminderMinute,
            paydaySuggestAverageIncome = paydaySuggestAverage,
            payHistoryCount = payHistoryCount,
            payHistoryAverage = payHistoryAverage,
            joinedDate = joinedDate,
            hasDebts = hasDebts,
            currentEmergencyFund = emergencyFund,
            primaryGoal = primaryGoal,
            displayName = displayName,
            birthday = birthday,
            smartAllocationMode = smartAllocationMode,
            targetSavingsRate = targetSavingsRate,
            savingTaxRate = savingTaxRate,
            vaultAllocationMode = vaultAllocationMode,
            dynamicSavingTaxEnabled = dynamicSavingTaxEnabled,
            lastComputedSavingTaxRate = lastComputedSavingTaxRate,
            regionalSettings = regionalSettings
        )
    }

    private fun Preferences.toSmartTransferSnapshot(): SmartTransferSnapshot {
        val emergencyCents = this[PreferenceKeys.smartPendingEmergencyCents] ?: 0L
        val investmentCents = this[PreferenceKeys.smartPendingInvestmentCents] ?: 0L
        val count = this[PreferenceKeys.smartPendingExpenseCount] ?: 0
        val holdUntil = this[PreferenceKeys.smartHoldUntilEpoch]
        val lastExpense = this[PreferenceKeys.smartLastExpenseEpoch]
        val awaitingEmergency = this[PreferenceKeys.smartAwaitingEmergencyCents] ?: 0L
        val awaitingInvestment = this[PreferenceKeys.smartAwaitingInvestmentCents] ?: 0L
        val awaitingCount = this[PreferenceKeys.smartAwaitingExpenseCount] ?: 0
        val confirmationStarted = this[PreferenceKeys.smartConfirmationStartedEpoch]

        return SmartTransferSnapshot(
            pendingEmergencyCents = emergencyCents,
            pendingInvestmentCents = investmentCents,
            pendingExpenseCount = count,
            lastExpenseEpochMillis = lastExpense,
            holdUntilEpochMillis = holdUntil,
            awaitingConfirmationEmergencyCents = awaitingEmergency,
            awaitingConfirmationInvestmentCents = awaitingInvestment,
            awaitingConfirmationExpenseCount = awaitingCount,
            confirmationStartedEpochMillis = confirmationStarted
        )
    }

    private object PreferenceKeys {
        val emergencyPercent = doublePreferencesKey("percent_emergency")
        val investPercent = doublePreferencesKey("percent_invest")
        val funPercent = doublePreferencesKey("percent_fun")
        val safeSplit = doublePreferencesKey("percent_safe_split")
        val riskLevel = stringPreferencesKey("risk_level")
        val autoRecommend = booleanPreferencesKey("auto_recommend")
        val includeTax = booleanPreferencesKey("include_tax_default")
        val monthlyIncome = doublePreferencesKey("monthly_income")
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderFrequencyDays = intPreferencesKey("reminder_frequency_days")
        val paydayReminderEnabled = booleanPreferencesKey("payday_reminder_enabled")
        val paydayReminderHour = intPreferencesKey("payday_reminder_hour")
        val paydayReminderMinute = intPreferencesKey("payday_reminder_minute")
        val paydaySuggestAverage = booleanPreferencesKey("payday_reminder_suggest_average")
        val age = intPreferencesKey("profile_age")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val joinedDateEpochDay = longPreferencesKey("joined_date_epoch_day")
        val educationStatus = stringPreferencesKey("profile_education_status")
        val employmentStatus = stringPreferencesKey("profile_employment_status")
        val livingSituation = stringPreferencesKey("profile_living_situation")
        val occupation = stringPreferencesKey("profile_occupation")
        val mainAccountBalance = doublePreferencesKey("profile_main_account_balance")
        val savingsAccountBalance = doublePreferencesKey("profile_savings_account_balance")
        val vaultsBalance = doublePreferencesKey("profile_vaults_balance")
        val subscriptionTotal = doublePreferencesKey("profile_subscription_total")
        val hasDebts = booleanPreferencesKey("profile_has_debts")
        val emergencyFund = doublePreferencesKey("profile_emergency_fund")
        val primaryGoal = stringPreferencesKey("profile_primary_goal")
        val displayName = stringPreferencesKey("profile_display_name")
        val birthdayEpochDay = longPreferencesKey("profile_birthday_epoch_day")
        val smartPendingEmergencyCents = longPreferencesKey("smart_pending_emergency_cents")
        val smartPendingInvestmentCents = longPreferencesKey("smart_pending_investment_cents")
        val smartPendingExpenseCount = intPreferencesKey("smart_pending_expense_count")
        val smartHoldUntilEpoch = longPreferencesKey("smart_hold_until_epoch")
        val smartLastExpenseEpoch = longPreferencesKey("smart_last_expense_epoch")
        val smartAwaitingEmergencyCents = longPreferencesKey("smart_awaiting_emergency_cents")
        val smartAwaitingInvestmentCents = longPreferencesKey("smart_awaiting_investment_cents")
        val smartAwaitingExpenseCount = intPreferencesKey("smart_awaiting_expense_count")
        val smartConfirmationStartedEpoch = longPreferencesKey("smart_confirmation_started_epoch")
        val smartAllocationMode = stringPreferencesKey("smart_allocation_mode")
        val vaultAllocationMode = stringPreferencesKey("vault_allocation_mode")
        val targetSavingsRate = doublePreferencesKey("target_savings_rate")
        val savingTaxRate = doublePreferencesKey("saving_tax_rate")
        val dynamicSavingTaxEnabled = booleanPreferencesKey("dynamic_saving_tax_enabled")
        val lastComputedSavingTaxRate = doublePreferencesKey("last_dynamic_saving_tax_rate")
        val autoDepositsEnabled = booleanPreferencesKey("auto_deposits_enabled")
        val autoDepositCheckHour = intPreferencesKey("auto_deposit_check_hour")
        val incomeTrackingMode = stringPreferencesKey("income_tracking_mode")
        val payInterval = stringPreferencesKey("pay_interval")
        val payDefaultNet = doublePreferencesKey("pay_default_net")
        val payDefaultSaveRate = doublePreferencesKey("pay_default_save_rate")
        val payWeeklyDay = intPreferencesKey("pay_weekly_day")
        val paySemiDay1 = intPreferencesKey("pay_semi_day_1")
        val paySemiDay2 = intPreferencesKey("pay_semi_day_2")
        val payMonthlyDay = intPreferencesKey("pay_monthly_day")
        val payCustomDays = intPreferencesKey("pay_custom_days")
        val payNextDate = longPreferencesKey("pay_next_date")
        val payLastDate = longPreferencesKey("pay_last_date")
        val payLastAmount = doublePreferencesKey("pay_last_amount")
        val payAutoDistribute = booleanPreferencesKey("pay_auto_distribute")
        val payAutoPending = booleanPreferencesKey("pay_auto_pending")
        val payDynamicSaveEnabled = booleanPreferencesKey("pay_dynamic_save_enabled")
        val payLastComputedSaveRate = doublePreferencesKey("pay_last_dynamic_save_rate")
        val payHistoryCount = intPreferencesKey("pay_history_count")
        val payHistoryTotal = doublePreferencesKey("pay_history_total")
        
        // Regional Settings
        val countryCode = stringPreferencesKey("regional_country_code")
        val languageCode = stringPreferencesKey("regional_language_code")
        val currencyCode = stringPreferencesKey("regional_currency_code")
        val customIncomeTaxRate = doublePreferencesKey("regional_custom_income_tax_rate")
    }
}

private fun Long.toCurrency(): Double = this / 100.0

data class PayHistoryStats(
    val count: Int,
    val total: Double
) {
    val average: Double
        get() = if (count > 0) total / count else 0.0
}
