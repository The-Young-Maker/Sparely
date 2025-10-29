package com.example.sparely.workers

import android.content.Context
import androidx.work.*
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules monthly runs of the MonthlyAllocationWorker.
 * Uses a 30-day periodic work request and computes an initial delay so the
 * first run lands at the start of next month (local timezone) near midnight.
 */
class MonthlyAllocationScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun schedule(enabled: Boolean) {
        if (!enabled) {
            cancel()
            return
        }

        val initialDelayMillis = computeInitialDelayToNextMonth()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = PeriodicWorkRequestBuilder<MonthlyAllocationWorker>(30, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    fun runImmediate() {
        val request = OneTimeWorkRequestBuilder<MonthlyAllocationWorker>()
            .addTag(WORK_TAG)
            .build()
        workManager.enqueue(request)
    }

    private fun computeInitialDelayToNextMonth(): Long {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        // target at start of next month at 00:05 to avoid exact midnight boundary
        val firstOfNextMonth = now.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(5).withSecond(0).withNano(0)

        val delay = Duration.between(now, firstOfNextMonth)

        // ensure at least a small delay
        val minimum = Duration.ofMinutes(1)
        val finalDelay = if (delay < minimum) delay.plusDays(1) else delay

        return finalDelay.toMillis()
    }

    companion object {
        private const val WORK_NAME = "sparely-monthly-allocations"
        private const val WORK_TAG = "monthly-allocation"
    }
}
