package com.example.sparely.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sparely.domain.model.SmartTransferRecommendation
import com.example.sparely.domain.model.SparelySettings
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class NotificationScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    fun schedule(settings: SparelySettings) {
        if (!settings.remindersEnabled) {
            cancel()
            return
        }
        NotificationHelper.ensureChannels(appContext)
        val initialDelay = computeInitialDelay(settings.reminderHour)
        val frequencyDays = settings.reminderFrequencyDays.toLong().coerceAtLeast(1L)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(frequencyDays, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
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

    fun showSmartTransferTracker(
        recommendation: SmartTransferRecommendation,
        vaultBreakdown: List<Pair<String, Double>>
    ) {
        NotificationHelper.showSmartTransferTracker(appContext, recommendation, vaultBreakdown)
    }

    fun dismissSmartTransferTracker() {
        NotificationHelper.dismissSmartTransferTracker(appContext)
    }

    suspend fun showVaultTransferWorkflow(container: com.example.sparely.AppContainer) {
        val pendingContributions = container.savingsRepository.getPendingVaultContributions()
        if (pendingContributions.isEmpty()) {
            NotificationHelper.dismissVaultTransferNotification(appContext)
            return
        }
        
        // Reset progress counter
        val prefs = appContext.getSharedPreferences("vault_transfer_workflow", android.content.Context.MODE_PRIVATE)
        prefs.edit().putInt("completed_count", 0).apply()
        
        val groupedByVault = pendingContributions.groupBy { it.vaultId }
        if (groupedByVault.isEmpty()) return
        
        // Show notification for first vault
        val firstVaultId = groupedByVault.keys.first()
        val firstContributions = groupedByVault[firstVaultId]!!
        
        // Get vault name from flow
        val allVaults = container.savingsRepository.observeSmartVaults().first()
        val vault = allVaults.find { it.id == firstVaultId }
        
        NotificationHelper.showVaultTransferNotification(
            context = appContext,
            vaultId = firstVaultId,
            vaultName = vault?.name ?: "Vault",
            contributions = firstContributions,
            currentIndex = 0,
            totalVaultCount = groupedByVault.size
        )
    }

    fun dismissVaultTransferWorkflow() {
        NotificationHelper.dismissVaultTransferNotification(appContext)
    }

    private fun computeInitialDelay(targetHour: Int): Long {
        val now = ZonedDateTime.now()
        val targetToday = now.withHour(targetHour.coerceIn(0, 23))
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        var firstRun = if (targetToday.isBefore(now)) targetToday.plusDays(1) else targetToday
        var delay = Duration.between(now, firstRun)
        val minimumDelay = Duration.ofMinutes(15)
        if (delay < minimumDelay) {
            firstRun = firstRun.plusDays(1)
            delay = Duration.between(now, firstRun)
        }
        return delay.toMillis()
    }

    companion object {
        private const val WORK_NAME = "sparely-daily-reminder"
    }
}
