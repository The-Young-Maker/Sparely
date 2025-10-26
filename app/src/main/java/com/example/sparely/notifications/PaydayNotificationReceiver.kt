package com.example.sparely.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sparely.MainActivity

class PaydayNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_RECORD_INCOME -> {
                // Dismiss the notification
                NotificationHelper.dismissPaydayReminder(context)
                
                // Open the app to the settings/paycheck section
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "paycheck")
                }
                context.startActivity(mainIntent)
            }
            ACTION_REMIND_LATER -> {
                // Just dismiss the notification - the worker will reschedule
                NotificationHelper.dismissPaydayReminder(context)
            }
        }
    }

    companion object {
        const val ACTION_RECORD_INCOME = "com.example.sparely.ACTION_RECORD_INCOME"
        const val ACTION_REMIND_LATER = "com.example.sparely.ACTION_REMIND_LATER"

        fun createRecordIncomeIntent(context: Context): Intent {
            return Intent(context, PaydayNotificationReceiver::class.java).apply {
                action = ACTION_RECORD_INCOME
            }
        }

        fun createRemindLaterIntent(context: Context): Intent {
            return Intent(context, PaydayNotificationReceiver::class.java).apply {
                action = ACTION_REMIND_LATER
            }
        }
    }
}
