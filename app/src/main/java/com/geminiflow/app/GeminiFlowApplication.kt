package com.geminiflow.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.geminiflow.app.data.api.GeminiApiClient
import com.geminiflow.app.data.auth.CookieManagerHelper
import com.geminiflow.app.data.auth.GeminiAuthRepositoryImpl
import com.geminiflow.app.data.storage.ImageStorageManager
import com.geminiflow.app.data.storage.SessionPreferencesRepository
import com.geminiflow.app.domain.usecase.EnsureAuthUseCase
import com.geminiflow.app.domain.usecase.StreamChatUseCase
import com.geminiflow.app.server.KtorLocalServer
import com.geminiflow.app.service.BatteryOptimizationHelper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class GeminiFlowApplication : Application() {

    companion object {
        const val CHANNEL_ID = "gemini_flow_server_channel"
        lateinit var instance: GeminiFlowApplication
            private set
    }

    // 核心依賴元件（全域單例容器）
    lateinit var okHttpClient: OkHttpClient
        private set
    lateinit var cookieHelper: CookieManagerHelper
        private set
    lateinit var authRepository: GeminiAuthRepositoryImpl
        private set
    lateinit var sessionRepository: SessionPreferencesRepository
        private set
    lateinit var imageStorageManager: ImageStorageManager
        private set
    lateinit var geminiApiClient: GeminiApiClient
        private set
    lateinit var streamChatUseCase: StreamChatUseCase
        private set
    lateinit var ensureAuthUseCase: EnsureAuthUseCase
        private set
    lateinit var trafficLogManager: com.geminiflow.app.data.storage.TrafficLogManager
        private set
    lateinit var notificationLogManager: com.geminiflow.app.data.storage.NotificationLogManager
        private set
    lateinit var apiLogManager: com.geminiflow.app.data.storage.ApiLogManager
        private set
    lateinit var notificationController: com.geminiflow.app.presentation.notification.NotificationController
        private set
    lateinit var ktorServer: KtorLocalServer
        private set
    lateinit var batteryOptimizationHelper: BatteryOptimizationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannel()
        initDependencies()
    }

    private fun initDependencies() {
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        cookieHelper = CookieManagerHelper()
        authRepository = GeminiAuthRepositoryImpl(this, okHttpClient, cookieHelper)
        sessionRepository = SessionPreferencesRepository(this)
        imageStorageManager = ImageStorageManager(this, okHttpClient, cookieHelper)
        trafficLogManager = com.geminiflow.app.data.storage.TrafficLogManager()
        notificationLogManager = com.geminiflow.app.data.storage.NotificationLogManager(this)
        apiLogManager = com.geminiflow.app.data.storage.ApiLogManager(this)
        notificationController = com.geminiflow.app.presentation.notification.NotificationController(notificationLogManager)
        com.geminiflow.app.presentation.notification.NotificationController.init(notificationController)
        geminiApiClient = GeminiApiClient(okHttpClient)

        ensureAuthUseCase = EnsureAuthUseCase(authRepository)
        streamChatUseCase = StreamChatUseCase(
            authRepository = authRepository,
            chatRepository = geminiApiClient,
            sessionRepository = sessionRepository,
            imageRepository = imageStorageManager
        )

        ktorServer = KtorLocalServer(
            streamChatUseCase = streamChatUseCase,
            imageStorageManager = imageStorageManager,
            trafficLogManager = trafficLogManager,
            apiLogManager = apiLogManager
        )

        batteryOptimizationHelper = BatteryOptimizationHelper(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
