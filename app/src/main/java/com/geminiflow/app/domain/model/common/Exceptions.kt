package com.geminiflow.app.domain.model.common

sealed class GeminiFlowException(message: String, cause: Throwable? = null) : Exception(message, cause)

class AuthenticationRequiredException(message: String = "Google 登入憑證失效或遺失，請先登入 Google 帳號") :
    GeminiFlowException(message)

class TokenExpiredException(message: String = "Gemini Token 已過期，需要重新整理") :
    GeminiFlowException(message)

class NetworkException(message: String, cause: Throwable? = null) :
    GeminiFlowException(message, cause)

class PayloadException(message: String) :
    GeminiFlowException(message)

class ImageDownloadException(message: String, cause: Throwable? = null) :
    GeminiFlowException(message, cause)
