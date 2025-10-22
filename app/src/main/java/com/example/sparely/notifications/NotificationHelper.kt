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
import com.example.sparely.domain.model.SmartTransferRecommendation
import java.text.NumberFormat

object NotificationHelper {
    const val REMINDER_CHANNEL_ID = "sparely_reminders"
    const val SMART_TRANSFER_CHANNEL_ID = "sparely_smart_transfer"
    const val AUTO_DEPOSIT_CHANNEL_ID = "sparely_auto_deposits"
    const val VAULT_TRANSFER_CHANNEL_ID = "sparely_vault_transfers"
    private const val REMINDER_NOTIFICATION_ID = 1001
    private const val SMART_TRANSFER_NOTIFICATION_ID = 2001
    private const val AUTO_DEPOSIT_NOTIFICATION_ID = 3001
    private const val VAULT_TRANSFER_NOTIFICATION_ID = 4001

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
            val smartTransferChannel = NotificationChannel(
                SMART_TRANSFER_CHANNEL_ID,
                context.getString(R.string.smart_transfer_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.smart_transfer_channel_description)
                setShowBadge(false)
            }
            val autoDepositChannel = NotificationChannel(
                AUTO_DEPOSIT_CHANNEL_ID,
                "Auto Deposits",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for scheduled vault auto-deposits"
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
            manager.createNotificationChannel(smartTransferChannel)
            manager.createNotificationChannel(autoDepositChannel)
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

    fun showSmartTransferTracker(
        context: Context,
        recommendation: SmartTransferRecommendation,
        vaultBreakdown: List<Pair<String, Double>>
    ) {
        ensureChannels(context)
        val total = formatAmount(recommendation.awaitingConfirmationAmount)
        val emergency = formatAmount(recommendation.awaitingEmergencyAmount)
        val invest = formatAmount(recommendation.awaitingInvestmentAmount)
        val baseContent = context.getString(
            R.string.smart_transfer_notification_content,
            total,
            emergency,
            invest
        )
        val breakdownText = if (vaultBreakdown.isNotEmpty()) {
            val bulletList = vaultBreakdown.joinToString(separator = "\n") { (name, amount) ->
                "- ${name}: ${formatAmount(amount)}"
            }
            baseContent + "\n" + context.getString(R.string.smart_transfer_notification_vault_breakdown, bulletList)
        } else {
            baseContent
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = PendingIntent.getBroadcast(
            context,
            1,
            SmartTransferNotificationReceiver.createIntent(context, SmartTransferNotificationReceiver.ACTION_COMPLETE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = PendingIntent.getBroadcast(
            context,
            2,
            SmartTransferNotificationReceiver.createIntent(context, SmartTransferNotificationReceiver.ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SMART_TRANSFER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setContentTitle(context.getString(R.string.smart_transfer_notification_title, total))
            .setContentText(breakdownText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(breakdownText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.smart_transfer_notification_mark_done),
                completeIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.smart_transfer_notification_do_later),
                cancelIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(SMART_TRANSFER_NOTIFICATION_ID, notification)
    }

    fun dismissSmartTransferTracker(context: Context) {
        NotificationManagerCompat.from(context).cancel(SMART_TRANSFER_NOTIFICATION_ID)
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

    private fun formatAmount(amount: Double): String =
        NumberFormat.getCurrencyInstance().format(amount)
}
