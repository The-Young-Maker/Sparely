package com.example.sparely.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sparely.data.preferences.PayHistoryStats
import com.example.sparely.data.preferences.UserPreferencesRepository
import com.example.sparely.domain.logic.PayScheduleCalculator
import java.time.LocalDate

class PaydayReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val preferencesRepository = UserPreferencesRepository(applicationContext)
        val settings = preferencesRepository.getSettingsSnapshot()
        if (!settings.paydayReminderEnabled) {
            NotificationScheduler(applicationContext).cancelPaydayReminder()
            return Result.success()
        }

        val expectedEpoch = inputData.getLong(KEY_EXPECTED_PAYDAY_EPOCH_DAY, Long.MIN_VALUE)
        val expectedDate = if (expectedEpoch != Long.MIN_VALUE) {
            LocalDate.ofEpochDay(expectedEpoch)
        } else {
            PayScheduleCalculator.resolveUpcomingPayDate(settings.paySchedule) ?: return Result.success()
        }

        val lastPayDate = settings.paySchedule.lastPayDate
        if (lastPayDate != null && !lastPayDate.isBefore(expectedDate)) {
            NotificationScheduler(applicationContext).schedulePaydayReminder(settings)
            return Result.success()
        }

        val stats: PayHistoryStats = preferencesRepository.getPayHistoryStats()
        val suggestion = if (settings.paydaySuggestAverageIncome && stats.count >= MIN_HISTORY_FOR_SUGGESTION) {
            stats.average
        } else {
            null
        }

        NotificationHelper.showPaydayReminder(applicationContext, expectedDate, suggestion)

        NotificationScheduler(applicationContext).schedulePaydayReminder(settings)
        return Result.success()
    }

    companion object {
        const val KEY_EXPECTED_PAYDAY_EPOCH_DAY = "expected_payday_epoch_day"
        private const val MIN_HISTORY_FOR_SUGGESTION = 2
    }
}
