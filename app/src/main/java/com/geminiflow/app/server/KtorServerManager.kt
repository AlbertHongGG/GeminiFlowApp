package com.geminiflow.app.server

import android.util.Log
import com.geminiflow.app.data.storage.ApiLogManager
import com.geminiflow.app.data.storage.TrafficLogManager
import com.geminiflow.app.domain.model.server.ServerState
import com.geminiflow.app.domain.repository.ImageRepository
import com.geminiflow.app.domain.server.ServerManager
import com.geminiflow.app.domain.usecase.StreamChatUseCase
import com.geminiflow.app.server.routes.chatRoute
import com.geminiflow.app.server.routes.healthRoute
import com.geminiflow.app.server.routes.imagesRoute
import com.geminiflow.app.server.routes.streamRoute
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 現代化 Ktor 伺服器核心管理器（Ktor Server Manager）
 * 實現 ServerManager 領域介面：
 * 1. 內建五態狀態機（ServerState），嚴格維護單一真實來源（SSOT）。
 * 2. 所有啟動與停止操作皆強制於 Dispatchers.IO 執行，主執行緒 0ms 負擔，徹底消除 UI 卡頓。
 * 3. 透過 Mutex 互斥鎖確保並發安全與防抖，防止過渡期重入。
 * 4. 具備 400ms 視覺過渡鎖存（Visual Latch），保證狀態轉移動畫細膩可見，杜絕瞬跳與倒置。
 */
class KtorServerManager(
    private val streamChatUseCase: StreamChatUseCase,
    private val imageRepository: ImageRepository,
    private val trafficLogManager: TrafficLogManager,
    private val apiLogManager: ApiLogManager
) : ServerManager {

    companion object {
        private const val TAG = "KtorServerManager"
        private const val SHUTDOWN_GRACE_PERIOD_MS = 500L
        private const val SHUTDOWN_TIMEOUT_MS = 1500L
        private const val MIN_VISUAL_LATCH_MS = 400L
    }

    private val _state = MutableStateFlow<ServerState>(ServerState.Stopped)
    override val state: StateFlow<ServerState> = _state.asStateFlow()

    private val mutex = Mutex()
    private var serverEngine: CIOApplicationEngine? = null

    private val totalRequests = AtomicLong(0)
    private val activeConnections = AtomicInteger(0)

    override suspend fun startServer(host: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentState = _state.value
            if (currentState is ServerState.Running) {
                Log.w(TAG, "Server is already running on http://${currentState.host}:${currentState.port}")
                return@withContext Result.success(Unit)
            }
            if (currentState is ServerState.Starting) {
                Log.w(TAG, "Server is already in starting transition.")
                return@withContext Result.success(Unit)
            }

            Log.i(TAG, "Initiating server start on http://$host:$port...")
            val transitionStartTime = System.currentTimeMillis()
            _state.value = ServerState.Starting(host, port)

            try {
                val engine = embeddedServer(CIO, host = host, port = port) {
                    install(ContentNegotiation) {
                        json(Json {
                            prettyPrint = false
                            isLenient = true
                            ignoreUnknownKeys = true
                        })
                    }

                    install(CORS) {
                        anyHost()
                        allowHeader(HttpHeaders.ContentType)
                        allowHeader(HttpHeaders.Authorization)
                        allowMethod(HttpMethod.Options)
                        allowMethod(HttpMethod.Get)
                        allowMethod(HttpMethod.Post)
                    }

                    routing {
                        healthRoute(trafficLogManager)
                        imagesRoute(imageRepository, trafficLogManager)
                        chatRoute(
                            streamChatUseCase = streamChatUseCase,
                            trafficLogManager = trafficLogManager,
                            apiLogManager = apiLogManager,
                            onRequestStarted = {
                                totalRequests.incrementAndGet()
                                activeConnections.incrementAndGet()
                                syncRunningMetrics()
                            },
                            onRequestEnded = {
                                activeConnections.decrementAndGet()
                                syncRunningMetrics()
                            }
                        )
                        streamRoute(
                            streamChatUseCase = streamChatUseCase,
                            trafficLogManager = trafficLogManager,
                            apiLogManager = apiLogManager,
                            onRequestStarted = {
                                totalRequests.incrementAndGet()
                                activeConnections.incrementAndGet()
                                syncRunningMetrics()
                            },
                            onRequestEnded = {
                                activeConnections.decrementAndGet()
                                syncRunningMetrics()
                            }
                        )
                    }
                }

                engine.start(wait = false)
                serverEngine = engine

                // 視覺鎖存保障：確保 Starting 狀態維持至少 MIN_VISUAL_LATCH_MS，提供細膩轉移動畫
                val elapsed = System.currentTimeMillis() - transitionStartTime
                if (elapsed < MIN_VISUAL_LATCH_MS) {
                    delay(MIN_VISUAL_LATCH_MS - elapsed)
                }

                val startTime = System.currentTimeMillis()
                _state.value = ServerState.Running(
                    host = host,
                    port = port,
                    startTime = startTime,
                    totalRequests = totalRequests.get(),
                    activeConnections = activeConnections.get()
                )
                Log.i(TAG, "Ktor Server successfully started and running on http://$host:$port")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Ktor Server on http://$host:$port: ${e.message}", e)
                serverEngine = null
                val elapsed = System.currentTimeMillis() - transitionStartTime
                if (elapsed < MIN_VISUAL_LATCH_MS) {
                    delay(MIN_VISUAL_LATCH_MS - elapsed)
                }
                _state.value = ServerState.Failed(
                    error = e.message ?: "Failed to start Ktor server",
                    host = host,
                    port = port
                )
                Result.failure(e)
            }
        }
    }

    override suspend fun stopServer(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentState = _state.value
            if (currentState is ServerState.Stopped) {
                return@withContext Result.success(Unit)
            }
            if (currentState is ServerState.Stopping) {
                return@withContext Result.success(Unit)
            }

            Log.i(TAG, "Initiating server stop...")
            val transitionStartTime = System.currentTimeMillis()
            _state.value = ServerState.Stopping

            try {
                serverEngine?.let { engine ->
                    engine.stop(
                        gracePeriodMillis = SHUTDOWN_GRACE_PERIOD_MS,
                        timeoutMillis = SHUTDOWN_TIMEOUT_MS
                    )
                }
                serverEngine = null
                activeConnections.set(0)

                // 視覺鎖存保障：確保 Stopping 狀態維持至少 MIN_VISUAL_LATCH_MS，杜絕瞬跳倒置
                val elapsed = System.currentTimeMillis() - transitionStartTime
                if (elapsed < MIN_VISUAL_LATCH_MS) {
                    delay(MIN_VISUAL_LATCH_MS - elapsed)
                }

                _state.value = ServerState.Stopped
                Log.i(TAG, "Ktor Server cleanly stopped.")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error while stopping Ktor Server: ${e.message}", e)
                serverEngine = null
                activeConnections.set(0)
                val elapsed = System.currentTimeMillis() - transitionStartTime
                if (elapsed < MIN_VISUAL_LATCH_MS) {
                    delay(MIN_VISUAL_LATCH_MS - elapsed)
                }
                _state.value = ServerState.Stopped
                Result.failure(e)
            }
        }
    }

    private fun syncRunningMetrics() {
        _state.update { current ->
            if (current is ServerState.Running) {
                current.copy(
                    totalRequests = totalRequests.get(),
                    activeConnections = activeConnections.get().coerceAtLeast(0)
                )
            } else {
                current
            }
        }
    }
}
