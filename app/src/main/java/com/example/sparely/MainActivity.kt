package com.example.sparely

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.example.sparely.ui.theme.SparelyTheme
import com.example.sparely.ui.SparelyApp
import com.example.sparely.ui.SparelyViewModel
import com.example.sparely.ui.SparelyViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: SparelyViewModel by viewModels {
        SparelyViewModelFactory((application as SparelyApplication).container)
    }

    private val requestNotificationsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    val deepLinkDestination = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply locale before setting content - it's already done in Application.onCreate
        // and will be applied when settings change via UserPreferencesRepository
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationsPermission.launch(permission)
            }
        }
        
        // Handle notification navigation
        handleDeepLink(intent)
        
        enableEdgeToEdge()
        setContent {
            SparelyTheme {
                SparelyApp(
                    viewModel = viewModel,
                    deepLinkDestination = deepLinkDestination.value,
                    onDeepLinkHandled = { deepLinkDestination.value = null }
                )
            }
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: android.content.Intent?) {
        intent?.getStringExtra("navigate_to")?.let { destination ->
            deepLinkDestination.value = destination
        }
    }
}