package com.example.sparely.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sparely.MainActivity
import com.example.sparely.data.local.SparelyDatabase
import com.example.sparely.data.local.ExpenseEntity
import com.example.sparely.data.preferences.UserPreferencesRepository
import com.example.sparely.data.local.toDomain
import com.example.sparely.domain.model.SmartTransferRecommendation
import com.example.sparely.domain.logic.BudgetEngine
import com.example.sparely.domain.logic.SmartTransferEngine
import com.example.sparely.domain.model.SmartTransferStatus
import com.example.sparely.domain.model.AllocationBreakdown
import com.example.sparely.domain.model.BudgetOverrunPrompt
import com.example.sparely.domain.model.BudgetPromptReason
import com.example.sparely.domain.model.EducationStatus
import com.example.sparely.domain.model.EmploymentStatus
import com.example.sparely.domain.model.Expense
import com.example.sparely.domain.model.ExpenseCategory
import com.example.sparely.domain.model.RecurringExpense
import com.example.sparely.domain.model.SavingsPercentages
import com.example.sparely.ui.theme.AzureTertiary
import com.example.sparely.ui.theme.AzureTertiaryDark
import com.example.sparely.domain.model.SparelySettings
import com.example.sparely.domain.model.SuggestionConfidence
import com.example.sparely.domain.model.UpcomingRecurringExpense
import com.example.sparely.ui.theme.DeepCurrentSurfaceVariant
import com.example.sparely.ui.theme.DeepNavy
import com.example.sparely.ui.theme.MidnightSurface
import com.example.sparely.ui.theme.MistyWhite
import com.example.sparely.ui.theme.TealPrimary
import com.example.sparely.ui.theme.TealPrimaryDark
import com.example.sparely.ui.theme.TideOutline
import com.example.sparely.ui.theme.WhisperSurface
import com.example.sparely.ui.theme.PearlOnVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import java.time.format.DateTimeFormatter

import java.time.temporal.ChronoUnit
class SavingsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = SavingsWidgetDataRepository(context)
        val snapshot = repository.loadSnapshot()
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppAction = actionStartActivity(openAppIntent)

        provideContent {
            SavingsWidgetContent(snapshot = snapshot, openApp = openAppAction)
        }
    }
}

private fun EducationStatus.displayLabel(): String = when (this) {
    EducationStatus.HIGH_SCHOOL -> "High school"
    EducationStatus.UNIVERSITY -> "University/college"
    EducationStatus.GRADUATED -> "Graduated"
    EducationStatus.OTHER -> "Other"
}

private fun EmploymentStatus.displayLabel(): String = when (this) {
    EmploymentStatus.STUDENT -> "Student"
    EmploymentStatus.EMPLOYED -> "Employed"
    EmploymentStatus.SELF_EMPLOYED -> "Self-employed"
    EmploymentStatus.UNEMPLOYED -> "Unemployed"
    EmploymentStatus.RETIRED -> "Retired"
}

private fun ExpenseCategory.displayLabel(): String {
    val locale = Locale.getDefault()
    return name.lowercase(locale).replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
    }
}

private fun BudgetPromptReason.describe(): String = when (this) {
    BudgetPromptReason.POTENTIAL_ONE_OFF -> "Looks like a one-off spike. Confirm if it won't repeat."
    BudgetPromptReason.TRENDING_HIGH -> "Spending trend is running hot versus your usual pattern."
    BudgetPromptReason.UNPLANNED_CATEGORY -> "No budget set yet—consider adding one so Sparely can help."
}

private fun SuggestionConfidence.displayLabel(): String = when (this) {
    SuggestionConfidence.HIGH -> "high confidence"
    SuggestionConfidence.MEDIUM -> "medium confidence"
    SuggestionConfidence.LOW -> "early insight"
}

private fun formatCountdown(daysUntil: Int): String = when {
    daysUntil < 0 -> "overdue by ${-daysUntil}d"
    daysUntil == 0 -> "due today"
    daysUntil == 1 -> "in 1 day"
    else -> "in ${daysUntil}d"
}

private fun computeBirthdayMessage(settings: SparelySettings, today: LocalDate = LocalDate.now()): String? {
    val birthday = settings.birthday ?: return null
    val next = nextBirthdayDate(birthday, today)
    val days = ChronoUnit.DAYS.between(today, next).toInt()
    val nameSuffix = settings.displayName?.let { " $it" } ?: ""
    return when {
        days == 0 -> "Happy birthday$nameSuffix!"
        days in 1..30 -> "Birthday in $days day${if (days == 1) "" else "s"} (${next.format(widgetDateFormatter)})"
        else -> null
    }
}

private fun nextBirthdayDate(birthday: LocalDate, today: LocalDate): LocalDate {
    val thisYear = birthdayInYear(birthday, today.year)
    return if (!thisYear.isBefore(today)) thisYear else birthdayInYear(birthday, today.year + 1)
}

private fun birthdayInYear(birthday: LocalDate, year: Int): LocalDate {
    val month = birthday.monthValue
    val length = YearMonth.of(year, month).lengthOfMonth()
    val day = birthday.dayOfMonth.coerceAtMost(length)
    return LocalDate.of(year, month, day)
}

class SavingsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SavingsWidget()
}

private data class SavingsWidgetSnapshot(
    val totalSaved: Double,
    val monthSaved: Double,
    val monthSpent: Double,
    val recentExpense: ExpenseSummary?,
    val smartTransfer: SmartTransferRecommendation?,
    val settings: SparelySettings,
    val budgetPrompt: BudgetPromptSummary?,
    val nextRecurring: NextRecurringSummary?
)

private data class ExpenseSummary(
    val description: String,
    val amount: Double
)

private data class BudgetPromptSummary(
    val category: ExpenseCategory,
    val month: YearMonth,
    val spent: Double,
    val limit: Double,
    val overspend: Double,
    val reason: BudgetPromptReason,
    val suggestionLimit: Double?,
    val suggestionConfidence: SuggestionConfidence?
)

private data class NextRecurringSummary(
    val description: String,
    val amount: Double,
    val dueDate: LocalDate,
    val daysUntil: Int,
    val autoLog: Boolean,
    val reminderDays: Int
)

private val widgetDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

private class SavingsWidgetDataRepository(context: Context) {
    private val database = SparelyDatabase.getInstance(context)
    private val transferDao = database.transferDao()
    private val expenseDao = database.expenseDao()
    private val budgetDao = database.budgetDao()
    private val recurringDao = database.recurringExpenseDao()
    private val accountsDao = database.savingsAccountDao()
    private val preferences = UserPreferencesRepository(context)

    suspend fun loadSnapshot(): SavingsWidgetSnapshot {
        return runCatching {
            withContext(Dispatchers.IO) {
                val now = LocalDate.now()
                val month = YearMonth.from(now)
                val monthStart = month.atDay(1)
                val monthEnd = month.atEndOfMonth()

                val totalSaved = accountsDao.getTotalBalance()
                val monthSaved = transferDao.getTotalSavedBetween(monthStart, monthEnd)
                val monthSpent = expenseDao.getTotalSpentBetween(monthStart, monthEnd)
                val recentExpense = expenseDao.getMostRecentExpense()?.toSummary()
                val settings = preferences.getSettingsSnapshot()
                val monthlyExpenses = expenseDao.getExpensesBetween(monthStart, monthEnd)
                val domainExpenses = monthlyExpenses.map { it.toDomain() }
                val currentBudgets = budgetDao.getBudgetsForMonth(month.year, month.monthValue)
                    .map { it.toDomain() }
                val budgetSummary = currentBudgets.takeIf { it.isNotEmpty() }?.let {
                    BudgetEngine.generateBudgetSummary(it, domainExpenses, month)
                }
                val budgetSuggestions = if (budgetSummary != null) {
                    BudgetEngine.suggestBudgetAdjustments(currentBudgets, domainExpenses, settings)
                } else {
                    emptyList()
                }
                val budgetPrompt = if (budgetSummary != null) {
                    BudgetEngine.detectBudgetPrompts(budgetSummary, domainExpenses, budgetSuggestions, settings)
                        .sortedByDescending { prompt -> prompt.overspendAmount }
                        .firstOrNull()
                        ?.toWidgetSummary()
                } else {
                    null
                }
                val upcomingRecurring = recurringDao.getAll()
                    .map { it.toDomain() }
                    .let { computeUpcomingRecurring(it, now) }
                    .firstOrNull()
                    ?.toWidgetSummary()
                val smartSnapshot = preferences.getSmartTransferSnapshot()
                val smartRecommendation = SmartTransferEngine.evaluate(smartSnapshot)

                SavingsWidgetSnapshot(
                    totalSaved = totalSaved,
                    monthSaved = monthSaved,
                    monthSpent = monthSpent,
                    recentExpense = recentExpense,
                    smartTransfer = smartRecommendation,
                    settings = settings,
                    budgetPrompt = budgetPrompt,
                    nextRecurring = upcomingRecurring
                )
            }
        }.getOrElse {
            SavingsWidgetSnapshot(
                totalSaved = 0.0,
                monthSaved = 0.0,
                monthSpent = 0.0,
                recentExpense = null,
                smartTransfer = null,
                settings = SparelySettings(),
                budgetPrompt = null,
                nextRecurring = null
            )
        }
    }

    private fun ExpenseEntity.toSummary(): ExpenseSummary = ExpenseSummary(
        description = description,
        amount = amount
    )

    private fun ExpenseEntity.toDomain(): Expense {
        val appliedPercentages = SavingsPercentages(
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
            appliedPercentages = appliedPercentages,
            autoRecommended = autoRecommended,
            riskLevelUsed = riskLevelUsed
        )
    }
}

private fun BudgetOverrunPrompt.toWidgetSummary(): BudgetPromptSummary = BudgetPromptSummary(
    category = category,
    month = month,
    spent = status.spent,
    limit = status.limit,
    overspend = overspendAmount,
    reason = reason,
    suggestionLimit = suggestion?.suggestedLimit,
    suggestionConfidence = suggestion?.confidence
)

private fun computeUpcomingRecurring(
    recurring: List<RecurringExpense>,
    today: LocalDate = LocalDate.now(),
    windowDays: Int = 30
): List<UpcomingRecurringExpense> {
    return recurring
        .filter { it.isActive }
        .mapNotNull { expense ->
            val intervalDays = expense.frequency.daysInterval.toLong().coerceAtLeast(1)
            var nextDue = expense.lastProcessedDate?.plusDays(intervalDays) ?: expense.startDate
            if (nextDue.isBefore(today)) {
                val diff = ChronoUnit.DAYS.between(nextDue, today)
                val steps = (diff / intervalDays) + 1
                nextDue = nextDue.plusDays(steps * intervalDays)
            }
            expense.endDate?.let { end ->
                if (nextDue.isAfter(end)) return@mapNotNull null
            }
            val daysUntil = ChronoUnit.DAYS.between(today, nextDue).toInt()
            if (daysUntil < 0 || daysUntil > windowDays) return@mapNotNull null
            UpcomingRecurringExpense(expense, nextDue, daysUntil)
        }
        .sortedBy { it.dueDate }
}

private fun UpcomingRecurringExpense.toWidgetSummary(): NextRecurringSummary = NextRecurringSummary(
    description = recurringExpense.description,
    amount = recurringExpense.amount,
    dueDate = dueDate,
    daysUntil = daysUntilDue,
    autoLog = recurringExpense.autoLog,
    reminderDays = recurringExpense.reminderDaysBefore
)

@Composable
private fun SavingsWidgetContent(snapshot: SavingsWidgetSnapshot, openApp: Action) {
    val currencyFormatter = rememberCurrencyFormatter()

    // Corrected ColorProvider definitions
    val surfaceColor =
        androidx.glance.color.ColorProvider(day = WhisperSurface, night = MidnightSurface)
    val onSurfaceColor = androidx.glance.color.ColorProvider(day = DeepNavy, night = MistyWhite)
    val onSurfaceVariantColor =
        androidx.glance.color.ColorProvider(day = TideOutline, night = PearlOnVariant)
    val accentColor = androidx.glance.color.ColorProvider(day = TealPrimary, night = TealPrimaryDark)
    val accentContainerColor = androidx.glance.color.ColorProvider(
        day = TealPrimary.copy(alpha = 0.12f),
        night = TealPrimaryDark.copy(alpha = 0.24f)
    )
    val spentColor =
        androidx.glance.color.ColorProvider(day = AzureTertiary, night = AzureTertiaryDark)
    val spentContainerColor = androidx.glance.color.ColorProvider(
        day = AzureTertiary.copy(alpha = 0.12f),
        night = AzureTertiaryDark.copy(alpha = 0.24f)
    )
    val neutralContainerColor = androidx.glance.color.ColorProvider(
        day = TideOutline.copy(alpha = 0.08f),
        night = DeepCurrentSurfaceVariant.copy(alpha = 0.6f)
    )


    val settings = snapshot.settings
    val educationLabel = settings.educationStatus.displayLabel()
    val employmentLabel = settings.employmentStatus.displayLabel()
    val birthdayMessage = computeBirthdayMessage(settings)

    val monthBadge = remember {
        val month = YearMonth.now()
        month.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase(Locale.getDefault())
    }

    val monthDelta = snapshot.monthSaved - snapshot.monthSpent
    val netLabel = if (monthDelta >= 0) {
        "+${currencyFormatter(monthDelta)}"
    } else {
        currencyFormatter(monthDelta)
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(surfaceColor)
            .cornerRadius(22.dp)
            .padding(16.dp)
            .clickable(openApp)
    ) {
        Text(
            modifier = GlanceModifier
                .background(accentContainerColor)
                .cornerRadius(14.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            text = "$monthBadge · INSIGHTS",
            style = TextStyle(
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.height(10.dp))
        settings.displayName?.takeIf { it.isNotBlank() }?.let { name ->
            Text(
                text = "Hi $name!",
                style = TextStyle(
                    color = onSurfaceVariantColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
        }
        Text(
            text = "Savings snapshot",
            style = TextStyle(
                color = onSurfaceColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = currencyFormatter(snapshot.totalSaved),
            style = TextStyle(
                color = onSurfaceColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "Lifetime saved",
            style = TextStyle(
                color = onSurfaceVariantColor,
                fontSize = 12.sp
            )
        )
        settings.primaryGoal?.takeIf { it.isNotBlank() }?.let { goal ->
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = "Primary goal: $goal",
                style = TextStyle(
                    color = onSurfaceVariantColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "Emergency fund ${currencyFormatter(settings.currentEmergencyFund)} • Debts ${if (settings.hasDebts) "active" else "clear"}",
            style = TextStyle(
                color = onSurfaceVariantColor,
                fontSize = 11.sp
            )
        )
        Text(
            text = "$educationLabel • $employmentLabel",
            style = TextStyle(
                color = onSurfaceVariantColor,
                fontSize = 11.sp
            )
        )
        birthdayMessage?.let { message ->
            Text(
                text = message,
                style = TextStyle(
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(16.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(accentContainerColor)
                    .cornerRadius(18.dp)
                    .padding(14.dp)
            ) {
                Text(
                    text = "Saved this month",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = currencyFormatter(snapshot.monthSaved),
                    style = TextStyle(
                        color = onSurfaceColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "Net $netLabel",
                    style = TextStyle(
                        color = onSurfaceVariantColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
            Spacer(modifier = GlanceModifier.width(12.dp))
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(spentContainerColor)
                    .cornerRadius(18.dp)
                    .padding(14.dp)
            ) {
                Text(
                    text = "Spent this month",
                    style = TextStyle(
                        color = spentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = currencyFormatter(snapshot.monthSpent),
                    style = TextStyle(
                        color = onSurfaceColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "Tap to plan smarter",
                    style = TextStyle(
                        color = onSurfaceVariantColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
        snapshot.budgetPrompt?.let { prompt ->
            Spacer(modifier = GlanceModifier.height(12.dp))
            BudgetPromptWidgetCard(
                prompt = prompt,
                currencyFormatter = currencyFormatter,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                accentColor = accentColor,
                spentColor = spentColor,
                spentContainerColor = spentContainerColor
            )
        }
        snapshot.nextRecurring?.let { summary ->
            Spacer(modifier = GlanceModifier.height(12.dp))
            NextRecurringWidgetCard(
                summary = summary,
                currencyFormatter = currencyFormatter,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                accentColor = accentColor,
                neutralContainerColor = neutralContainerColor
            )
        }
        snapshot.recentExpense?.let { expense ->
            Spacer(modifier = GlanceModifier.height(12.dp))
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(neutralContainerColor)
                    .cornerRadius(18.dp)
                    .padding(14.dp)
            ) {
                Text(
                    text = "Latest expense",
                    style = TextStyle(
                        color = onSurfaceVariantColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = expense.description,
                    style = TextStyle(
                        color = onSurfaceColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = currencyFormatter(expense.amount),
                    style = TextStyle(
                        color = onSurfaceVariantColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
        snapshot.smartTransfer?.let { smart ->
            Spacer(modifier = GlanceModifier.height(12.dp))
            SmartTransferWidgetPanel(
                recommendation = smart,
                currencyFormatter = currencyFormatter,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                accentColor = accentColor,
                accentContainerColor = accentContainerColor,
                spentColor = spentColor,
                spentContainerColor = spentContainerColor,
                neutralContainerColor = neutralContainerColor
            )
        }
        Spacer(modifier = GlanceModifier.height(12.dp))
        Text(
            text = "Tap to open Sparely",
            style = TextStyle(
                color = onSurfaceVariantColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        )
    }
}

@Composable
private fun rememberCurrencyFormatter(): (Double) -> String {
    val formatter = remember { NumberFormat.getCurrencyInstance() }
    return { value -> formatter.format(value) }
}

@Composable
private fun BudgetPromptWidgetCard(
    prompt: BudgetPromptSummary,
    currencyFormatter: (Double) -> String,
    onSurfaceColor: ColorProvider,
    onSurfaceVariantColor: ColorProvider,
    accentColor: ColorProvider,
    spentColor: ColorProvider,
    spentContainerColor: ColorProvider
) {
    val monthLabel = remember(prompt.month) {
        val label = prompt.month.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase(Locale.getDefault())
        "$label budget"
    }
    val categoryLabel = prompt.category.displayLabel()
    val reasonLabel = prompt.reason.describe()
    val suggestionText = prompt.suggestionLimit?.let { limit ->
        val base = "Suggested limit ${currencyFormatter(limit)}"
        prompt.suggestionConfidence?.displayLabel()?.let { confidence ->
            "$base ($confidence)"
        } ?: base
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(spentContainerColor)
            .cornerRadius(18.dp)
            .padding(14.dp)
    ) {
        Text(
            text = "Budget focus",
            style = TextStyle(
                color = spentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "$categoryLabel exceeded by ${currencyFormatter(prompt.overspend)}",
            style = TextStyle(
                color = onSurfaceColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = "$monthLabel · Spent ${currencyFormatter(prompt.spent)} of ${currencyFormatter(prompt.limit)}",
            style = TextStyle(
                color = onSurfaceVariantColor,
                fontSize = 11.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = reasonLabel,
            style = TextStyle(
                color = onSurfaceVariantColor,
                fontSize = 11.sp
            )
        )
        suggestionText?.let { suggestion ->
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = suggestion,
                style = TextStyle(
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun NextRecurringWidgetCard(
    summary: NextRecurringSummary,
    currencyFormatter: (Double) -> String,
    onSurfaceColor: ColorProvider,
    onSurfaceVariantColor: ColorProvider,
    accentColor: ColorProvider,
    neutralContainerColor: ColorProvider
) {
    val dueDateLabel = summary.dueDate.format(widgetDateFormatter)
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(neutralContainerColor)
            .cornerRadius(18.dp)
            .padding(14.dp)
    ) {
        Text(
            text = "Next recurring",
            style = TextStyle(
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = summary.description,
            style = TextStyle(
                color = onSurfaceColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = "Due $dueDateLabel (${formatCountdown(summary.daysUntil)})",
            style = TextStyle(
                color = onSurfaceVariantColor,
                fontSize = 11.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = currencyFormatter(summary.amount),
            style = TextStyle(
                color = onSurfaceColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = if (summary.autoLog) "Auto-log on" else "Manual confirm",
            style = TextStyle(
                color = onSurfaceVariantColor,
                fontSize = 11.sp
            )
        )
        Text(
            text = "Reminder ${summary.reminderDays}d before",
            style = TextStyle(
                color = onSurfaceVariantColor,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun SmartTransferWidgetPanel(
    recommendation: SmartTransferRecommendation,
    currencyFormatter: (Double) -> String,
    onSurfaceColor: ColorProvider,
    onSurfaceVariantColor: ColorProvider,
    accentColor: ColorProvider,
    accentContainerColor: ColorProvider,
    spentColor: ColorProvider,
    spentContainerColor: ColorProvider,
    neutralContainerColor: ColorProvider
) {
    val (containerColor, headlineColor) = when (recommendation.status) {
        SmartTransferStatus.READY -> accentContainerColor to accentColor
        SmartTransferStatus.ACCUMULATING -> spentContainerColor to spentColor
        SmartTransferStatus.STANDBY -> neutralContainerColor to onSurfaceVariantColor
        SmartTransferStatus.AWAITING_CONFIRMATION -> accentContainerColor to accentColor
    }
    val totalText = currencyFormatter(recommendation.totalAmount)
    val countLabel = if (recommendation.pendingExpenseCount == 1) "1 purchase" else "${recommendation.pendingExpenseCount} purchases"
    val statusDetail = when (recommendation.status) {
        SmartTransferStatus.READY -> "Ready to shift across. Tap Sparely after you move it."
        SmartTransferStatus.ACCUMULATING -> {
            val remainingMillis = recommendation.holdUntilEpochMillis?.let { it - System.currentTimeMillis() } ?: 0L
            val remainingMinutes = if (remainingMillis > 0) remainingMillis / 60000.0 else 0.0
            if (remainingMinutes > 0.1) {
                "Holding for another ${String.format("%.1f", remainingMinutes)} min in case you add more."
            } else {
                "Holding briefly for the next expense in your streak."
            }
        }
        SmartTransferStatus.STANDBY -> {
            val shortfall = recommendation.shortfallToThreshold
            if (shortfall > 0.0) {
                "Needs ${currencyFormatter(recommendation.minimumTransferAmount)} total — ${currencyFormatter(shortfall)} to go."
            } else {
                "Below your quick-transfer threshold but available anytime."
            }
        }
    SmartTransferStatus.AWAITING_CONFIRMATION -> "Amounts stay parked until you mark them done."
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(containerColor)
            .cornerRadius(18.dp)
            .padding(14.dp)
    ) {
        Text(
            text = "Smart transfer",
            style = TextStyle(
                color = headlineColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = totalText,
            style = TextStyle(
                color = onSurfaceColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = statusDetail,
            style = TextStyle(
                color = onSurfaceVariantColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = "From $countLabel",
            style = TextStyle(
                color = headlineColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "Emergency ${currencyFormatter(recommendation.emergencyPortion)} · Investing ${currencyFormatter(recommendation.investmentPortion)}",
            style = TextStyle(
                color = onSurfaceVariantColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        )
    }
}
