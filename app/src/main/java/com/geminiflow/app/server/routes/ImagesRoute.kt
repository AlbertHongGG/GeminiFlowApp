package com.geminiflow.app.server.routes

import com.geminiflow.app.data.storage.TrafficLogManager
import com.geminiflow.app.domain.model.log.TrafficLog
import com.geminiflow.app.domain.repository.ImageRepository
import com.geminiflow.app.server.dto.ErrorResponseDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.imagesRoute(
    imageRepository: ImageRepository,
    trafficLogManager: TrafficLogManager
) {
    get("/images/{filename}") {
        val t0 = System.currentTimeMillis()
        val clientIp = call.request.local.remoteHost
        val filename = call.parameters["filename"]
        if (filename.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponseDto("缺少圖片名稱"))
            trafficLogManager.record(
                TrafficLog(
                    method = "GET",
                    path = "/images/empty",
                    statusCode = 400,
                    durationMs = System.currentTimeMillis() - t0,
                    clientIp = clientIp,
                    responseSummary = "缺少圖片名稱"
                )
            )
            return@get
        }
        val imageFile = imageRepository.getImageFile(filename)
        if (imageFile != null && imageFile.exists()) {
            call.respondFile(imageFile)
            trafficLogManager.record(
                TrafficLog(
                    method = "GET",
                    path = "/images/$filename",
                    statusCode = 200,
                    durationMs = System.currentTimeMillis() - t0,
                    clientIp = clientIp,
                    responseSummary = "Image sent (${imageFile.length()} bytes)"
                )
            )
        } else {
            call.respond(HttpStatusCode.NotFound, ErrorResponseDto("找不到該圖片"))
            trafficLogManager.record(
                TrafficLog(
                    method = "GET",
                    path = "/images/$filename",
                    statusCode = 404,
                    durationMs = System.currentTimeMillis() - t0,
                    clientIp = clientIp,
                    responseSummary = "找不到該圖片"
                )
            )
        }
    }
}
