package com.geminiflow.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.geminiflow.app.presentation.components.GfFloatingNavBar
import com.geminiflow.app.presentation.components.NavigationTab
import com.geminiflow.app.presentation.theme.GeminiFlowTheme
import com.geminiflow.app.presentation.ui.BatteryGuideBottomSheet
import com.geminiflow.app.presentation.ui.GoogleAuthScreen
import com.geminiflow.app.presentation.ui.PlaygroundScreen
import com.geminiflow.app.presentation.ui.ServerHubScreen
import com.geminiflow.app.presentation.ui.SettingsScreen
import com.geminiflow.app.presentation.ui.TrafficScreen
import com.geminiflow.app.presentation.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Notification permission handled
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
                    AppNavigation(viewModel = viewModel, activity = this)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshBatteryStatus()
        viewModel.refreshCacheStats()
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
    const val MAIN = "main"
    const val GOOGLE_AUTH = "google_auth"
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    activity: ComponentActivity
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.MAIN
    ) {
        composable(
            route = Destinations.MAIN,
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) }
        ) {
            MainContainerScreen(
                viewModel = viewModel,
                activity = activity,
                onNavigateToLogin = { navController.navigate(Destinations.GOOGLE_AUTH) }
            )
        }

        composable(
            route = Destinations.GOOGLE_AUTH,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            GoogleAuthScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen(
    viewModel: MainViewModel,
    activity: ComponentActivity,
    onNavigateToLogin: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(NavigationTab.HUB) }
    var showBatteryBottomSheet by rememberSaveable { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    if (showBatteryBottomSheet) {
        BatteryGuideBottomSheet(
            isUnrestricted = uiState.isBatteryUnrestricted,
            oemTips = uiState.oemTips,
            onRequestUnrestricted = {
                viewModel.requestIgnoreBatteryOptimizations(activity)
            },
            onOpenOemSettings = {
                viewModel.openOemAutoStartSettings(activity)
            },
            onDismiss = {
                showBatteryBottomSheet = false
                viewModel.refreshBatteryStatus()
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Viewport with animated transition
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(animationSpec = tween(150)))
            },
            label = "tabContent"
        ) { tab ->
            when (tab) {
                NavigationTab.HUB -> {
                    ServerHubScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = onNavigateToLogin,
                        onOpenBatteryGuide = { showBatteryBottomSheet = true },
                        onNavigateToSettings = { selectedTab = NavigationTab.SETTINGS }
                    )
                }
                NavigationTab.PLAYGROUND -> {
                    PlaygroundScreen(
                        viewModel = viewModel
                    )
                }
                NavigationTab.TRAFFIC -> {
                    TrafficScreen(
                        viewModel = viewModel
                    )
                }
                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = onNavigateToLogin,
                        onOpenBatteryGuide = { showBatteryBottomSheet = true }
                    )
                }
            }
        }

        // Floating Pill Dock at bottom
        GfFloatingNavBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
