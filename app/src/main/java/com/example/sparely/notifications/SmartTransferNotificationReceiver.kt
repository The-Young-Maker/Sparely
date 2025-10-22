package com.example.sparely.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sparely.SparelyApplication
import com.example.sparely.data.local.SavingsTransferEntity
import com.example.sparely.domain.model.SavingsCategory
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.round

class SmartTransferNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as SparelyApplication
                val container = app.container
                when (action) {
                    ACTION_COMPLETE -> handleComplete(context, container)
                    ACTION_CANCEL -> handleCancel(context, container)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleComplete(context: Context, container: com.example.sparely.AppContainer) {
        val (emergencyAmount, investmentAmount) = container.preferencesRepository.completeSmartTransferConfirmation()
        if (emergencyAmount > 0.0) {
            val rounded = emergencyAmount.toCurrencyPrecision()
            container.savingsRepository.logTransfer(
                SavingsTransferEntity(
                    category = SavingsCategory.EMERGENCY,
                    amount = rounded,
                    date = LocalDate.now()
                )
            )
        }
        if (investmentAmount > 0.0) {
            val rounded = investmentAmount.toCurrencyPrecision()
            container.savingsRepository.logTransfer(
                SavingsTransferEntity(
                    category = SavingsCategory.INVESTMENT,
                    amount = rounded,
                    date = LocalDate.now()
                )
            )
        }
        NotificationHelper.dismissSmartTransferTracker(context)
    }

    private suspend fun handleCancel(context: Context, container: com.example.sparely.AppContainer) {
        container.preferencesRepository.cancelSmartTransferConfirmation(returnToPending = true)
        NotificationHelper.dismissSmartTransferTracker(context)
    }

    companion object {
        const val ACTION_COMPLETE = "com.example.sparely.SMART_TRANSFER_COMPLETE"
        const val ACTION_CANCEL = "com.example.sparely.SMART_TRANSFER_CANCEL"

        fun createIntent(context: Context, action: String): Intent =
            Intent(context, SmartTransferNotificationReceiver::class.java).apply {
                this.action = action
            }
    }
}

private fun Double.toCurrencyPrecision(): Double = round(this * 100) / 100.0
