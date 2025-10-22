package com.example.sparely.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sparely.data.local.SparelyDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        NotificationHelper.ensureChannels(applicationContext)
        val database = SparelyDatabase.getInstance(applicationContext)
        val expenses = database.expenseDao().observeExpenses().first()
        val message = if (expenses.isEmpty()) {
            "Log your latest purchase in Sparely to start automating savings."
        } else {
            val sevenDaysAgo = LocalDate.now().minusDays(7)
            val recent = expenses.filter { !it.date.isBefore(sevenDaysAgo) }
            val reserved = recent.sumOf { it.emergencyAmount + it.investmentAmount + it.funAmount }
            if (recent.isEmpty()) {
                "You haven't logged a purchase this week. Add one to keep your savings plan on track."
            } else {
                "You set aside $${"%.2f".format(reserved)} in the last 7 days. Review goals to stay ahead."
            }
        }
        NotificationHelper.showReminder(applicationContext, message)
        return Result.success()
    }
}
