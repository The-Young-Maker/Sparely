package com.example.sparely.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sparely.MainActivity
import com.example.sparely.R
import java.text.NumberFormat

object NotificationHelper {
    const val REMINDER_CHANNEL_ID = "sparely_reminders"
    const val AUTO_DEPOSIT_CHANNEL_ID = "sparely_auto_deposits"
    const val VAULT_TRANSFER_CHANNEL_ID = "sparely_vault_transfers"
    const val PAYDAY_CHANNEL_ID = "sparely_payday_reminders"
    private const val REMINDER_NOTIFICATION_ID = 1001
    private const val AUTO_DEPOSIT_NOTIFICATION_ID = 3001
    private const val VAULT_TRANSFER_NOTIFICATION_ID = 4001
    private const val PAYDAY_NOTIFICATION_ID = 4002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                context.getString(R.string.app_name) + " reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Savings and goal nudges from Sparely"
            }
            val autoDepositChannel = NotificationChannel(
                AUTO_DEPOSIT_CHANNEL_ID,
                "Auto Deposits",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for scheduled vault auto-deposits"
            }
            val paydayChannel = NotificationChannel(
                PAYDAY_CHANNEL_ID,
                "Payday Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to log your paycheck"
                setShowBadge(false)
            }
            val vaultTransferChannel = NotificationChannel(
                VAULT_TRANSFER_CHANNEL_ID,
                "Vault Transfers",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Step-by-step vault transfer workflow"
                setShowBadge(false)
            }
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(autoDepositChannel)
            manager.createNotificationChannel(paydayChannel)
            manager.createNotificationChannel(vaultTransferChannel)
        }
    }

    fun showReminder(context: Context, message: String) {
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setContentTitle("Sparely reminder")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
    }
    
    fun showAutoDepositReminder(context: Context, depositCount: Int, totalAmount: Double) {
        val formattedAmount = formatAmount(totalAmount)
        val title = if (depositCount == 1) {
            "Vault Auto-Deposit Due"
        } else {
            "$depositCount Vault Auto-Deposits Due"
        }
        val message = "Transfer $formattedAmount to your savings vaults and mark them as complete in the app."
        
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "vaultTransfers")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, AUTO_DEPOSIT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(AUTO_DEPOSIT_NOTIFICATION_ID, notification)
    }

    fun showVaultTransferNotification(
        context: Context,
        vaultId: Long,
        vaultName: String,
        contributions: List<com.example.sparely.domain.model.VaultContribution>,
        currentIndex: Int,
        totalVaultCount: Int
    ) {
        ensureChannels(context)
        
        val totalAmount = contributions.sumOf { it.amount }
        val formattedTotal = formatAmount(totalAmount)
        
        // Build source breakdown
        val sourceBreakdown = contributions.groupBy { it.source }
            .map { (source, contribs) ->
                val sourceAmount = contribs.sumOf { it.amount }
                val sourceName = when (source) {
                    com.example.sparely.domain.model.VaultContributionSource.INCOME -> "Income"
                    com.example.sparely.domain.model.VaultContributionSource.SAVING_TAX -> "Saving tax"
                    com.example.sparely.domain.model.VaultContributionSource.AUTO_DEPOSIT -> "Auto deposit"
                    com.example.sparely.domain.model.VaultContributionSource.MANUAL -> "Manual"
                    com.example.sparely.domain.model.VaultContributionSource.TRANSFER -> "Transfer"
                }
                "$sourceName: ${formatAmount(sourceAmount)}"
            }
            .joinToString("\n")
        
        val progressText = if (totalVaultCount > 1) {
            "Vault ${currentIndex + 1} of $totalVaultCount"
        } else {
            ""
        }
        
        val contentText = buildString {
            append("Transfer $formattedTotal")
            if (progressText.isNotEmpty()) {
                append(" • $progressText")
            }
        }
        
        val bigText = buildString {
            append(contentText)
            append("\n\n")
            append(sourceBreakdown)
        }
        
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "vaultTransfers")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val transferredIntent = PendingIntent.getBroadcast(
            context,
            vaultId.toInt(),
            VaultTransferNotificationReceiver.createTransferredIntent(context, vaultId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            vaultId.toInt() + 10000,
            VaultTransferNotificationReceiver.createDismissIntent(context, vaultId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, VAULT_TRANSFER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setContentTitle(vaultName)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Transferred",
                transferredIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Dismiss",
                dismissIntent
            )
            .build()
        
        NotificationManagerCompat.from(context).notify(VAULT_TRANSFER_NOTIFICATION_ID, notification)
    }

    fun dismissVaultTransferNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(VAULT_TRANSFER_NOTIFICATION_ID)
    }

    fun showPaydayReminder(
        context: Context,
        expectedDate: java.time.LocalDate,
        suggestedAmount: Double?
    ) {
        ensureChannels(context)
        val formattedDate = expectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
        val baseMessage = "Enter the amount you received this payday."
        val body = if (suggestedAmount != null && suggestedAmount > 0.0) {
            val formattedAmount = formatAmount(suggestedAmount)
            "$baseMessage\nSuggested amount based on recent paychecks: $formattedAmount"
        } else baseMessage

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "paycheck")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val recordIncomeIntent = PendingIntent.getBroadcast(
            context,
            1,
            PaydayNotificationReceiver.createRecordIncomeIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remindLaterIntent = PendingIntent.getBroadcast(
            context,
            2,
            PaydayNotificationReceiver.createRemindLaterIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PAYDAY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setContentTitle("Payday — $formattedDate")
            .setContentText(baseMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Record Income",
                recordIncomeIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Remind Later",
                remindLaterIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(PAYDAY_NOTIFICATION_ID, notification)
    }

    fun dismissPaydayReminder(context: Context) {
        NotificationManagerCompat.from(context).cancel(PAYDAY_NOTIFICATION_ID)
    }

    private fun formatAmount(amount: Double): String =
        NumberFormat.getCurrencyInstance().format(amount)
}
