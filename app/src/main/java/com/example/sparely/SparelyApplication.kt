package com.example.sparely

import android.app.Application
import com.example.sparely.notifications.NotificationHelper

class SparelyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        container = DefaultAppContainer(this)
    }
}
