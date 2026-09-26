package com.geminiflow.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.geminiflow.app.data.auth.CookieManagerHelper
import com.geminiflow.app.data.media.AndroidMediaExporter
import com.geminiflow.app.data.network.TrafficLoggingInterceptor
import com.geminiflow.app.data.repository.AuthRepositoryImpl
import com.geminiflow.app.data.repository.ChatRepositoryImpl
import com.geminiflow.app.data.repository.ImageRepositoryImpl
import com.geminiflow.app.data.repository.SessionRepositoryImpl
import com.geminiflow.app.data.storage.ApiLogManager
import com.geminiflow.app.data.storage.NotificationLogManager
import com.geminiflow.app.data.storage.TrafficLogManager
import com.geminiflow.app.domain.media.MediaExporter
import com.geminiflow.app.domain.repository.AuthRepository
import com.geminiflow.app.domain.repository.ChatRepository
import com.geminiflow.app.domain.repository.ImageRepository
import com.geminiflow.app.domain.repository.SessionRepository
import com.geminiflow.app.domain.usecase.EnsureAuthUseCase
import com.geminiflow.app.domain.usecase.StreamChatUseCase
import com.geminiflow.app.presentation.notification.NotificationController
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

    lateinit var okHttpClient: OkHttpClient
        private set
    lateinit var cookieHelper: CookieManagerHelper
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var sessionRepository: SessionRepository
        private set
    lateinit var imageRepository: ImageRepository
        private set
    lateinit var mediaExporter: MediaExporter
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var streamChatUseCase: StreamChatUseCase
        private set
    lateinit var ensureAuthUseCase: EnsureAuthUseCase
        private set
    lateinit var trafficLogManager: TrafficLogManager
        private set
    lateinit var notificationLogManager: NotificationLogManager
        private set
    lateinit var apiLogManager: ApiLogManager
        private set
    lateinit var notificationController: NotificationController
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
        trafficLogManager = TrafficLogManager()
        val loggingInterceptor = TrafficLoggingInterceptor(trafficLogManager)

        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        cookieHelper = CookieManagerHelper()
        authRepository = AuthRepositoryImpl(this, okHttpClient, cookieHelper)
        sessionRepository = SessionRepositoryImpl(this)
        imageRepository = ImageRepositoryImpl(this)
        mediaExporter = AndroidMediaExporter(this)
        chatRepository = ChatRepositoryImpl(okHttpClient)

        notificationLogManager = NotificationLogManager(this)
        apiLogManager = ApiLogManager(this)
        notificationController = NotificationController(notificationLogManager)
        NotificationController.init(notificationController)

        ensureAuthUseCase = EnsureAuthUseCase(authRepository)
        streamChatUseCase = StreamChatUseCase(
            authRepository = authRepository,
            chatRepository = chatRepository,
            sessionRepository = sessionRepository,
            imageRepository = imageRepository
        )

        ktorServer = KtorLocalServer(
            streamChatUseCase = streamChatUseCase,
            imageRepository = imageRepository,
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
