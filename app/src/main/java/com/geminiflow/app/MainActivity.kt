package com.geminiflow.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.geminiflow.app.presentation.features.auth.GoogleAuthScreen
import com.geminiflow.app.presentation.features.log.AiLogViewerScreen
import com.geminiflow.app.presentation.features.log.ApiLogDetailScreen
import com.geminiflow.app.presentation.features.log.SystemNotificationLogScreen
import com.geminiflow.app.presentation.features.playground.PlaygroundScreen
import com.geminiflow.app.presentation.features.playground.PlaygroundViewModel
import com.geminiflow.app.presentation.features.server.ServerHubScreen
import com.geminiflow.app.presentation.features.server.ServerHubViewModel
import com.geminiflow.app.presentation.features.settings.BatteryGuideBottomSheet
import com.geminiflow.app.presentation.features.settings.SettingsPage
import com.geminiflow.app.presentation.features.settings.SettingsViewModel
import com.geminiflow.app.presentation.features.splash.SplashScreen
import com.geminiflow.app.presentation.navigation.components.CupertinoSwipeBackContainer
import com.geminiflow.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.geminiflow.app.presentation.navigation.model.AppRoute
import com.geminiflow.app.presentation.notification.GlobalNotificationOverlay
import com.geminiflow.app.presentation.theme.GeminiFlowTheme
import com.geminiflow.app.presentation.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val serverHubViewModel: ServerHubViewModel by viewModels()
    private val playgroundViewModel: PlaygroundViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GeminiFlowApp)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()

        setContent {
            GeminiFlowTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) togetherWith
                            fadeOut(animationSpec = tween(400))
                    },
                    label = "splashTransition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            onAnimationFinished = { showSplash = false }
                        )
                    } else {
                        GlobalNotificationOverlay {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                AppNavigation(
                                    mainViewModel = mainViewModel,
                                    serverHubViewModel = serverHubViewModel,
                                    playgroundViewModel = playgroundViewModel,
                                    settingsViewModel = settingsViewModel,
                                    activity = this@MainActivity
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.refreshBatteryStatus()
        settingsViewModel.refreshCacheStats()
        serverHubViewModel.refreshCacheStats()
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

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    serverHubViewModel: ServerHubViewModel,
    playgroundViewModel: PlaygroundViewModel,
    settingsViewModel: SettingsViewModel,
    activity: ComponentActivity
) {
    val app = GeminiFlowApplication.instance
    val navState by mainViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    var showBatteryBottomSheet by rememberSaveable { mutableStateOf(false) }

    if (showBatteryBottomSheet) {
        BatteryGuideBottomSheet(
            isUnrestricted = settingsState.isBatteryUnrestricted,
            onRequestUnrestricted = {
                settingsViewModel.requestIgnoreBatteryOptimizations(activity)
            },
            onDismiss = {
                showBatteryBottomSheet = false
                settingsViewModel.refreshBatteryStatus()
            }
        )
    }

    CupertinoSwipeBackContainer(
        backStack = navState.backStack,
        onPushRoute = { route -> mainViewModel.pushRoute(route) },
        onPopRoute = { mainViewModel.popRoute() }
    ) { route ->
        val navigator = LocalCupertinoNavigator.current
        when (route) {
            is AppRoute.Main -> {
                ServerHubScreen(
                    viewModel = serverHubViewModel,
                    onNavigateToSandbox = { navigator.push(AppRoute.Sandbox) },
                    onNavigateToSettings = { navigator.push(AppRoute.Settings) }
                )
            }
            is AppRoute.Sandbox -> {
                PlaygroundScreen(
                    viewModel = playgroundViewModel
                )
            }
            is AppRoute.Settings -> {
                SettingsPage(
                    viewModel = settingsViewModel,
                    onNavigateToLogin = { navigator.push(AppRoute.GoogleAuth) },
                    onOpenBatteryGuide = { showBatteryBottomSheet = true },
                    onNavigateToAiLogs = { navigator.push(AppRoute.AiLogs) },
                    onNavigateToNotificationLogs = { navigator.push(AppRoute.NotificationLogs) }
                )
            }
            is AppRoute.GoogleAuth -> {
                GoogleAuthScreen(
                    onNavigateBack = { navigator.pop() }
                )
            }
            is AppRoute.NotificationLogs -> {
                SystemNotificationLogScreen(
                    logManager = app.notificationLogManager,
                    onNavigateBack = { navigator.pop() }
                )
            }
            is AppRoute.AiLogs -> {
                AiLogViewerScreen(
                    apiLogManager = app.apiLogManager,
                    onNavigateBack = { navigator.pop() },
                    onNavigateToDetail = { rawJson ->
                        navigator.push(AppRoute.ApiLogDetail(rawJson))
                    }
                )
            }
            is AppRoute.ApiLogDetail -> {
                ApiLogDetailScreen(
                    rawJson = route.rawJson,
                    onNavigateBack = { navigator.pop() }
                )
            }
        }
    }
}
