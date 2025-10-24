package com.example.sparely

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.example.sparely.notifications.NotificationHelper
import com.example.sparely.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale

class SparelyApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        container = DefaultAppContainer(this)
        
        // Apply saved locale
        applicationScope.launch {
            applyStoredLocale()
        }
    }
    
    private suspend fun applyStoredLocale() {
        val preferencesRepository = UserPreferencesRepository(this)
        val settings = preferencesRepository.settingsFlow.firstOrNull()
        
        settings?.regionalSettings?.languageCode?.let { code ->
            setAppLocale(code)
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applicationScope.launch {
            applyStoredLocale()
        }
    }
}

fun Context.setAppLocale(languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        createConfigurationContext(config)
    }
    
    @Suppress("DEPRECATION")
    resources.updateConfiguration(config, resources.displayMetrics)
}
