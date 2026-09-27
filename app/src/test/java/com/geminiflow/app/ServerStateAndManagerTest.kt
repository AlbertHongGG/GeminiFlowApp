package com.geminiflow.app

import com.geminiflow.app.domain.model.server.ServerState
import com.geminiflow.app.domain.server.ServerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerStateAndManagerTest {

    @Test
    fun testServerStateTransitionsAndPropertyFlags() {
        val stopped = ServerState.Stopped
        assertFalse(stopped.isRunning)
        assertFalse(stopped.isTransitioning)
        assertEquals("127.0.0.1", stopped.host)
        assertEquals(5000, stopped.port)

        val starting = ServerState.Starting("0.0.0.0", 8080)
        assertFalse(starting.isRunning)
        assertTrue(starting.isTransitioning)
        assertEquals("0.0.0.0", starting.host)
        assertEquals(8080, starting.port)

        val running = ServerState.Running("0.0.0.0", 8080, startTime = 12345L, totalRequests = 10, activeConnections = 2)
        assertTrue(running.isRunning)
        assertFalse(running.isTransitioning)
        assertEquals(12345L, running.startTime)
        assertEquals(10L, running.totalRequests)
        assertEquals(2, running.activeConnections)

        val stopping = ServerState.Stopping
        assertFalse(stopping.isRunning)
        assertTrue(stopping.isTransitioning)

        val failed = ServerState.Failed(error = "Address already in use", host = "127.0.0.1", port = 5000)
        assertFalse(failed.isRunning)
        assertFalse(failed.isTransitioning)
        assertEquals("Address already in use", failed.error)
    }

    @Test
    fun testFakeServerManagerStateFlow() = runBlocking {
        val fakeManager = FakeServerManager()

        assertEquals(ServerState.Stopped, fakeManager.state.value)

        val startResult = fakeManager.startServer("127.0.0.1", 5000)
        assertTrue(startResult.isSuccess)
        assertTrue(fakeManager.state.value.isRunning)

        val stopResult = fakeManager.stopServer()
        assertTrue(stopResult.isSuccess)
        assertEquals(ServerState.Stopped, fakeManager.state.value)
    }

    @Test
    fun testStateTransitionsSequence() = runBlocking {
        val fakeManager = FakeServerManager()
        val emittedStates = mutableListOf<ServerState>()

        val job = launch {
            fakeManager.state.collect { emittedStates.add(it) }
        }
        kotlinx.coroutines.delay(50)

        fakeManager.startServer("127.0.0.1", 5000)
        kotlinx.coroutines.delay(50)
        fakeManager.stopServer()
        kotlinx.coroutines.delay(50)

        job.cancel()

        // 驗證狀態轉移序列：絕不可跳躍，絕不可倒置
        assertEquals(5, emittedStates.size)
        assertTrue("Initial state must be Stopped", emittedStates[0] is ServerState.Stopped)
        assertTrue("Second state must be Starting", emittedStates[1] is ServerState.Starting)
        assertTrue("Third state must be Running", emittedStates[2] is ServerState.Running)
        assertTrue("Fourth state must be Stopping (NOT Starting)", emittedStates[3] is ServerState.Stopping)
        assertTrue("Final state must be Stopped", emittedStates[4] is ServerState.Stopped)
    }

    @Test
    fun testServerStateUiMappingExhaustive() {
        fun resolveStatusText(state: ServerState): String = when (state) {
            is ServerState.Starting -> "ENGINE STARTING"
            is ServerState.Running -> "ENGINE ACTIVE"
            is ServerState.Stopping -> "ENGINE STOPPING"
            is ServerState.Stopped -> "ENGINE STANDBY"
            is ServerState.Failed -> "ENGINE FAILED"
        }

        assertEquals("ENGINE STANDBY", resolveStatusText(ServerState.Stopped))
        assertEquals("ENGINE STARTING", resolveStatusText(ServerState.Starting("127.0.0.1", 5000)))
        assertEquals("ENGINE ACTIVE", resolveStatusText(ServerState.Running("127.0.0.1", 5000, 0L)))
        assertEquals("ENGINE STOPPING", resolveStatusText(ServerState.Stopping))
        assertEquals("ENGINE FAILED", resolveStatusText(ServerState.Failed("error", "127.0.0.1", 5000)))
    }

    private class FakeServerManager : ServerManager {
        private val _state = MutableStateFlow<ServerState>(ServerState.Stopped)
        override val state: StateFlow<ServerState> = _state.asStateFlow()

        override suspend fun startServer(host: String, port: Int): Result<Unit> {
            _state.value = ServerState.Starting(host, port)
            kotlinx.coroutines.delay(50)
            _state.value = ServerState.Running(
                host = host,
                port = port,
                startTime = System.currentTimeMillis()
            )
            kotlinx.coroutines.delay(50)
            return Result.success(Unit)
        }

        override suspend fun stopServer(): Result<Unit> {
            _state.value = ServerState.Stopping
            kotlinx.coroutines.delay(50)
            _state.value = ServerState.Stopped
            kotlinx.coroutines.delay(50)
            return Result.success(Unit)
        }
    }
}
