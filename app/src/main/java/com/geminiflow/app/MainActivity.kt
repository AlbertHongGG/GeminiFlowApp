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
import com.geminiflow.app.presentation.components.AnimatedFloatingTabBar
import com.geminiflow.app.presentation.components.AppTab
import com.geminiflow.app.presentation.navigation.components.CupertinoSwipeBackContainer
import com.geminiflow.app.presentation.navigation.components.LocalCupertinoNavigator
import com.geminiflow.app.presentation.navigation.model.AppRoute
import com.geminiflow.app.presentation.notification.GlobalNotificationOverlay
import com.geminiflow.app.presentation.theme.GeminiFlowTheme
import com.geminiflow.app.presentation.ui.AiLogViewerScreen
import com.geminiflow.app.presentation.ui.ApiLogDetailScreen
import com.geminiflow.app.presentation.ui.BatteryGuideBottomSheet
import com.geminiflow.app.presentation.ui.GoogleAuthScreen
import com.geminiflow.app.presentation.ui.PlaygroundScreen
import com.geminiflow.app.presentation.ui.ServerHubScreen
import com.geminiflow.app.presentation.ui.SettingsPage
import com.geminiflow.app.presentation.ui.SystemNotificationLogScreen
import com.geminiflow.app.presentation.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()

        setContent {
            GeminiFlowTheme {
                GlobalNotificationOverlay {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(viewModel = viewModel, activity = this)
                    }
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

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    activity: ComponentActivity
) {
    val app = GeminiFlowApplication.instance
    val uiState by viewModel.uiState.collectAsState()

    CupertinoSwipeBackContainer(
        backStack = uiState.backStack,
        onPushRoute = { route -> viewModel.pushRoute(route) },
        onPopRoute = { viewModel.popRoute() }
    ) { route ->
        val navigator = LocalCupertinoNavigator.current
        when (route) {
            is AppRoute.Main -> {
                MainContainerScreen(
                    viewModel = viewModel,
                    activity = activity,
                    onNavigateToLogin = { navigator.push(AppRoute.GoogleAuth) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen(
    viewModel: MainViewModel,
    activity: ComponentActivity,
    onNavigateToLogin: () -> Unit,
    onNavigateToAiLogs: () -> Unit,
    onNavigateToNotificationLogs: () -> Unit
) {
    var showBatteryBottomSheet by rememberSaveable { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    if (showBatteryBottomSheet) {
        BatteryGuideBottomSheet(
            isUnrestricted = uiState.isBatteryUnrestricted,
            onRequestUnrestricted = {
                viewModel.requestIgnoreBatteryOptimizations(activity)
            },
            onDismiss = {
                showBatteryBottomSheet = false
                viewModel.refreshBatteryStatus()
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 主畫面視圖：於三大核心頁面間切換 (SSOT: uiState.activeTab)
        AnimatedContent(
            targetState = uiState.activeTab,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(animationSpec = tween(150)))
            },
            label = "threeTabsContent"
        ) { tab ->
            when (tab) {
                AppTab.DASHBOARD -> {
                    ServerHubScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = onNavigateToLogin,
                        onOpenBatteryGuide = { showBatteryBottomSheet = true },
                        onNavigateToSettings = { viewModel.selectTab(AppTab.SETTINGS) }
                    )
                }
                AppTab.SANDBOX -> {
                    PlaygroundScreen(
                        viewModel = viewModel
                    )
                }
                AppTab.SETTINGS -> {
                    SettingsPage(
                        viewModel = viewModel,
                        onNavigateToLogin = onNavigateToLogin,
                        onOpenBatteryGuide = { showBatteryBottomSheet = true },
                        onNavigateToAiLogs = onNavigateToAiLogs,
                        onNavigateToNotificationLogs = onNavigateToNotificationLogs
                    )
                }
            }
        }

        // 底部浮動導航欄 (SSOT: uiState.activeTab)
        AnimatedFloatingTabBar(
            selectedTab = uiState.activeTab,
            onTabSelected = { viewModel.selectTab(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
