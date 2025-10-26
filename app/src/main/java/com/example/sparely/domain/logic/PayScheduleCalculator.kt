package com.example.sparely.domain.logic

import com.example.sparely.domain.model.PayInterval
import com.example.sparely.domain.model.PayScheduleSettings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object PayScheduleCalculator {
    fun computeNextPayDate(schedule: PayScheduleSettings, lastPayDate: LocalDate): LocalDate? {
        return when (schedule.interval) {
            PayInterval.WEEKLY -> nextScheduledWeekday(lastPayDate, schedule.weeklyDayOfWeek, 7)
            PayInterval.BIWEEKLY -> nextScheduledWeekday(lastPayDate, schedule.weeklyDayOfWeek, 14)
            PayInterval.SEMI_MONTHLY -> nextSemiMonthlyDate(lastPayDate, schedule.semiMonthlyDay1, schedule.semiMonthlyDay2)
            PayInterval.MONTHLY -> nextMonthlyDate(lastPayDate, schedule.monthlyDay)
            PayInterval.CUSTOM -> schedule.customDaysBetween?.let { lastPayDate.plusDays(it.toLong()) }
        }
    }

    fun resolveUpcomingPayDate(schedule: PayScheduleSettings, referenceDate: LocalDate = LocalDate.now()): LocalDate? {
        schedule.nextPayDate?.let { explicit ->
            if (!explicit.isBefore(referenceDate)) return explicit
        }
        val lastPayDate = schedule.lastPayDate ?: return schedule.nextPayDate
        val next = computeNextPayDate(schedule, lastPayDate) ?: return null
        return if (next.isBefore(referenceDate)) computeNextPayDate(schedule, referenceDate) else next
    }

    private fun nextScheduledWeekday(baseDate: LocalDate, target: DayOfWeek, minimumDays: Long): LocalDate {
        var candidate = baseDate.plusDays(minimumDays.coerceAtLeast(1))
        repeat(7) {
            if (candidate.dayOfWeek == target) return candidate
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
}
