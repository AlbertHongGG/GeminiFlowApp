package com.geminiflow.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.geminiflow.app.presentation.theme.GeminiFlowTheme
import com.geminiflow.app.presentation.ui.DashboardScreen
import com.geminiflow.app.presentation.ui.GoogleAuthScreen
import com.geminiflow.app.presentation.ui.SettingsScreen
import com.geminiflow.app.presentation.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Notification permission result handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()

        setContent {
            GeminiFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshBatteryStatus()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }
    }
}

object Destinations {
    const val DASHBOARD = "dashboard"
    const val GOOGLE_AUTH = "google_auth"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.DASHBOARD
    ) {
        composable(Destinations.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.navigate(Destinations.GOOGLE_AUTH) },
                onNavigateToSettings = { navController.navigate(Destinations.SETTINGS) }
            )
        }
        composable(Destinations.GOOGLE_AUTH) {
            GoogleAuthScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
