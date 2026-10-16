package com.geminiflow.app.domain.model.common

sealed class GeminiFlowException(message: String, cause: Throwable? = null) : Exception(message, cause)

class AuthenticationRequiredException(message: String = "Google 登入憑證失效或遺失，請先登入 Google 帳號") :
    GeminiFlowException(message)

class TokenExpiredException(message: String = "Gemini Token 已過期，需要重新整理") :
    GeminiFlowException(message)

class TokenExtractionException(message: String, cause: Throwable? = null) :
    GeminiFlowException(message, cause)

class NetworkException(message: String, cause: Throwable? = null) :
    GeminiFlowException(message, cause)

class PayloadException(message: String) :
    GeminiFlowException(message)

class ImageDownloadException(message: String, cause: Throwable? = null) :
    GeminiFlowException(message, cause)

class RateLimitException(message: String = "Google 邊緣網關觸發頻率限制 (HTTP 429)") :
    GeminiFlowException(message)

open class AttestationException(message: String, cause: Throwable? = null) :
    GeminiFlowException(message, cause)

class ChallengeFetchException(message: String, cause: Throwable? = null) :
    AttestationException(message, cause)

class VmExecutionException(message: String, cause: Throwable? = null) :
    AttestationException(message, cause)
