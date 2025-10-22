package com.example.sparely

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationsPermission.launch(permission)
            }
        }
        enableEdgeToEdge()
        setContent {
            SparelyTheme {
                SparelyApp(viewModel = viewModel)
            }
        }
    }
}