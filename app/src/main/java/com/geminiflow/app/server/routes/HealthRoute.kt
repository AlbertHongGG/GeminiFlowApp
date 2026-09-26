package com.geminiflow.app.server.routes

import com.geminiflow.app.data.storage.TrafficLogManager
import com.geminiflow.app.domain.model.log.TrafficLog
import com.geminiflow.app.server.dto.HealthResponseDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoute(trafficLogManager: TrafficLogManager) {
    get("/health") {
        val t0 = System.currentTimeMillis()
        val clientIp = call.request.local.remoteHost
        call.respond(HttpStatusCode.OK, HealthResponseDto(ok = true))
        trafficLogManager.record(
            TrafficLog(
                method = "GET",
                path = "/health",
                statusCode = 200,
                durationMs = System.currentTimeMillis() - t0,
                clientIp = clientIp,
                responseSummary = "ok: true"
            )
        )
    }
}
