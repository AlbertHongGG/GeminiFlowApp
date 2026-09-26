package com.geminiflow.app.server

import android.util.Log
import com.geminiflow.app.data.storage.ApiLogManager
import com.geminiflow.app.data.storage.TrafficLogManager
import com.geminiflow.app.domain.model.server.ServerStatus
import com.geminiflow.app.domain.repository.ImageRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class KtorLocalServer(
    private val streamChatUseCase: StreamChatUseCase,
    private val imageRepository: ImageRepository,
    private val trafficLogManager: TrafficLogManager,
    private val apiLogManager: ApiLogManager
) {
    companion object {
        private const val TAG = "KtorLocalServer"
    }

    private var serverEngine: CIOApplicationEngine? = null

    private val _status = MutableStateFlow(ServerStatus())
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    private val totalRequests = AtomicLong(0)
    private val activeConnections = AtomicInteger(0)

    val isRunning: Boolean
        get() = serverEngine != null && _status.value.isRunning

    @Synchronized
    fun start(host: String = "127.0.0.1", port: Int = 5000) {
        if (serverEngine != null) {
            Log.w(TAG, "Server is already running.")
            return
        }

        try {
            _status.value = _status.value.copy(
                host = host,
                port = port,
                errorMessage = null
            )

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
                            updateStatusCounts()
                        },
                        onRequestEnded = {
                            activeConnections.decrementAndGet()
                            updateStatusCounts()
                        }
                    )
                    streamRoute(
                        streamChatUseCase = streamChatUseCase,
                        trafficLogManager = trafficLogManager,
                        apiLogManager = apiLogManager,
                        onRequestStarted = {
                            totalRequests.incrementAndGet()
                            activeConnections.incrementAndGet()
                            updateStatusCounts()
                        },
                        onRequestEnded = {
                            activeConnections.decrementAndGet()
                            updateStatusCounts()
                        }
                    )
                }
            }

            engine.start(wait = false)
            serverEngine = engine
            _status.value = _status.value.copy(
                isRunning = true,
                host = host,
                port = port,
                errorMessage = null,
                startTime = System.currentTimeMillis()
            )
            Log.i(TAG, "Ktor Embedded Server started on http://$host:$port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Ktor server: ${e.message}", e)
            _status.value = _status.value.copy(
                isRunning = false,
                errorMessage = e.message,
                startTime = null
            )
            throw e
        }
    }

    @Synchronized
    fun stop() {
        serverEngine?.let {
            it.stop(1000, 2000)
            serverEngine = null
            _status.value = _status.value.copy(
                isRunning = false,
                activeConnections = 0,
                startTime = null
            )
            Log.i(TAG, "Ktor Embedded Server stopped.")
        }
    }

    private fun updateStatusCounts() {
        _status.value = _status.value.copy(
            totalRequests = totalRequests.get(),
            activeConnections = activeConnections.get()
        )
    }
}
