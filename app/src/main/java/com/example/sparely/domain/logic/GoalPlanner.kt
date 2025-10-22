package com.example.sparely.domain.logic

import com.example.sparely.data.local.GoalEntity
import com.example.sparely.domain.model.Expense
import com.example.sparely.domain.model.Goal
import com.example.sparely.domain.model.SavingsCategory
import com.example.sparely.domain.model.SavingsTransfer
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.min

object GoalPlanner {
    fun toDomainGoals(
        goals: List<GoalEntity>,
        expenses: List<Expense>,
        transfers: List<SavingsTransfer>
    ): List<Goal> {
        if (goals.isEmpty()) return emptyList()
        val contributions = aggregateContributions(expenses, transfers)
        val monthlyAverages = computeMonthlyAverages(expenses, transfers)
        val today = LocalDate.now()

        return goals.map { entity ->
            val contributed = contributions[entity.category] ?: 0.0
            val percent = if (entity.targetAmount <= 0) 1.0 else (contributed / entity.targetAmount).coerceIn(0.0, 1.2)
            val projected = projectCompletion(entity, contributed, monthlyAverages[entity.category] ?: 0.0, today)
            Goal(
                id = entity.id,
                title = entity.title,
                targetAmount = entity.targetAmount,
                category = entity.category,
                targetDate = entity.targetDate,
                createdAt = entity.createdAt,
                notes = entity.notes,
                archived = entity.archived,
                progressAmount = contributed,
                progressPercent = percent,
                projectedCompletion = projected
            )
        }
    }

    private fun aggregateContributions(
        expenses: List<Expense>,
        transfers: List<SavingsTransfer>
    ): Map<SavingsCategory, Double> {
        val totals = mutableMapOf<SavingsCategory, Double>()
        expenses.forEach { expense ->
            totals.merge(SavingsCategory.EMERGENCY, expense.allocation.emergencyAmount, Double::plus)
            totals.merge(SavingsCategory.INVESTMENT, expense.allocation.investmentAmount, Double::plus)
            totals.merge(SavingsCategory.FUN, expense.allocation.funAmount, Double::plus)
        }
        transfers.forEach { transfer ->
            totals.merge(transfer.category, transfer.amount, Double::plus)
        }
        return totals
    }

    private fun computeMonthlyAverages(
        expenses: List<Expense>,
        transfers: List<SavingsTransfer>
    ): Map<SavingsCategory, Double> {
        if (expenses.isEmpty() && transfers.isEmpty()) return emptyMap()
        val monthly = mutableMapOf<Pair<YearMonth, SavingsCategory>, Double>()
        expenses.forEach { expense ->
            val month = YearMonth.from(expense.date)
            monthly.merge(month to SavingsCategory.EMERGENCY, expense.allocation.emergencyAmount, Double::plus)
            monthly.merge(month to SavingsCategory.INVESTMENT, expense.allocation.investmentAmount, Double::plus)
            monthly.merge(month to SavingsCategory.FUN, expense.allocation.funAmount, Double::plus)
        }
        transfers.forEach { transfer ->
            val month = YearMonth.from(transfer.date)
            monthly.merge(month to transfer.category, transfer.amount, Double::plus)
        }

        val groupedByCategory = SavingsCategory.values().associateWith { category ->
            val monthValues = monthly.filterKeys { it.second == category }.values
            if (monthValues.isEmpty()) 0.0 else monthValues.average()
        }
        return groupedByCategory
    }

    private fun projectCompletion(
        goal: GoalEntity,
        contributed: Double,
        monthlyAverage: Double,
        today: LocalDate
    ): LocalDate? {
        val remaining = goal.targetAmount - contributed
        if (remaining <= 0.0) return today
        if (monthlyAverage <= 0.0) return null
        val monthsNeeded = ceil(remaining / monthlyAverage).toLong().coerceAtMost(60)
        return today.plusMonths(min(monthsNeeded, 240))
    }
}
