package com.geminiflow.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.geminiflow.app.data.attestation.AndroidV8TokenSigner
import com.geminiflow.app.data.attestation.WaaClient
import com.geminiflow.app.data.auth.CookieManagerHelper
import com.geminiflow.app.data.auth.keepalive.SessionKeepAliveManager
import com.geminiflow.app.data.media.AndroidMediaExporter
import com.geminiflow.app.data.network.TrafficLoggingInterceptor
import com.geminiflow.app.data.network.export.GeminiImageExportService
import com.geminiflow.app.data.network.upload.GeminiImageUploader
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
import com.geminiflow.app.domain.repository.ImageUploader
import com.geminiflow.app.domain.repository.SessionRepository
import com.geminiflow.app.domain.repository.GalleryRepository
import com.geminiflow.app.data.datasource.gallery.MediaStoreGalleryDataSource
import com.geminiflow.app.data.repository.GalleryRepositoryImpl
import com.geminiflow.app.data.media.ImageCompressor
import com.geminiflow.app.domain.usecase.ProcessSelectedImagesUseCase
import com.geminiflow.app.domain.usecase.EnsureAuthUseCase
import com.geminiflow.app.domain.usecase.StreamChatUseCase
import com.geminiflow.app.presentation.notification.NotificationController
import com.geminiflow.app.domain.server.ServerManager
import com.geminiflow.app.domain.server.ServerController
import com.geminiflow.app.service.AndroidServerController
import com.geminiflow.app.server.KtorServerManager
import com.geminiflow.app.domain.telemetry.notifier.ServerRequestStatusNotifier
import com.geminiflow.app.service.notification.AndroidSystemStatusNotifierImpl
import com.geminiflow.app.service.BatteryOptimizationHelper
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.geminiflow.app.data.media.MediaStoreThumbnailFetcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class GeminiFlowApplication : Application(), ImageLoaderFactory {

    companion object {
        const val CHANNEL_ID = "gemini_flow_server_channel"
        lateinit var instance: GeminiFlowApplication
            private set
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(MediaStoreThumbnailFetcher.Factory(this@GeminiFlowApplication))
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .crossfade(false)
            .respectCacheHeaders(false)
            .build()
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
    lateinit var imageUploader: ImageUploader
        private set
    lateinit var mediaExporter: MediaExporter
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var galleryRepository: GalleryRepository
        private set
    lateinit var processSelectedImagesUseCase: ProcessSelectedImagesUseCase
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
    lateinit var serverRequestStatusNotifier: ServerRequestStatusNotifier
        private set
    lateinit var serverManager: ServerManager
        private set
    lateinit var serverController: ServerController
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

        val credentialStore = com.geminiflow.app.data.auth.WebKitCredentialStore()
        val tokenExtractor = com.geminiflow.app.data.auth.token.HttpGeminiTokenExtractor(okHttpClient)
        val tokenManager = com.geminiflow.app.data.auth.token.GeminiTokenManager(credentialStore, tokenExtractor)
        authRepository = AuthRepositoryImpl(credentialStore, tokenManager)
        cookieHelper = CookieManagerHelper()
        sessionRepository = SessionRepositoryImpl(this)
        val imageExportService = GeminiImageExportService(okHttpClient)
        imageRepository = ImageRepositoryImpl(this, exportService = imageExportService, authRepository = authRepository)
        imageUploader = GeminiImageUploader(okHttpClient)
        mediaExporter = AndroidMediaExporter(this)
        val waaClient = WaaClient(okHttpClient)
        val tokenSigner = AndroidV8TokenSigner(this, waaClient)
        chatRepository = ChatRepositoryImpl(
            client = okHttpClient,
            authRepository = authRepository,
            tokenSigner = tokenSigner,
            imageUploader = imageUploader
        )

        val galleryDataSource = MediaStoreGalleryDataSource(this)
        galleryRepository = GalleryRepositoryImpl(galleryDataSource)
        val imageCompressor = ImageCompressor(this)
        processSelectedImagesUseCase = ProcessSelectedImagesUseCase(imageCompressor)

        notificationLogManager = NotificationLogManager(this)
        apiLogManager = ApiLogManager(this)
        notificationController = NotificationController(notificationLogManager)
        NotificationController.init(notificationController)

        val settingsPrefs = getSharedPreferences(
            AndroidSystemStatusNotifierImpl.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        serverRequestStatusNotifier = AndroidSystemStatusNotifierImpl(
            context = this,
            isEnabledProvider = {
                settingsPrefs.getBoolean(
                    AndroidSystemStatusNotifierImpl.PREF_KEY_SYSTEM_REQUEST_STATUS_NOTIFICATION_ENABLED,
                    true
                )
            }
        )

        ensureAuthUseCase = EnsureAuthUseCase(authRepository)
        streamChatUseCase = StreamChatUseCase(
            chatRepository = chatRepository,
            sessionRepository = sessionRepository,
            imageRepository = imageRepository
        )

        val sessionKeepAliveManager = SessionKeepAliveManager(authRepository)
        serverManager = KtorServerManager(
            streamChatUseCase = streamChatUseCase,
            imageRepository = imageRepository,
            trafficLogManager = trafficLogManager,
            apiLogManager = apiLogManager,
            serverRequestStatusNotifier = serverRequestStatusNotifier,
            sessionKeepAliveManager = sessionKeepAliveManager
        )

        batteryOptimizationHelper = BatteryOptimizationHelper(this)
        serverController = AndroidServerController(this, serverManager)
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
