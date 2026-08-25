// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * AI error detail information - supplements AgentError with detailed diagnostic data
 * Provides fine-grained error classification for troubleshooting
 */
@Keep
sealed class AIErrorDetail : Serializable {
    abstract val httpStatusCode: Int?
    abstract val responseBody: String?
    abstract val originalException: Throwable?
    abstract val troubleshootingHint: String?

    /**
     * API Key not configured or invalid
     */
    @Keep
    data class ApiKeyError(
        @SerializedName("providerId") val providerId: String,
        @SerializedName("reason") val reason: ApiKeyErrorReason,
        override val httpStatusCode: Int? = null,
        override val responseBody: String? = null,
        override val originalException: Throwable? = null,
        override val troubleshootingHint: String? = null
    ) : AIErrorDetail()

    /**
     * Authentication/Authorization failure (401/403)
     */
    @Keep
    data class AuthenticationError(
        @SerializedName("httpCode") override val httpStatusCode: Int,
        @SerializedName("errorType") val errorType: AuthErrorType,
        override val responseBody: String? = null,
        override val originalException: Throwable? = null,
        override val troubleshootingHint: String? = null
    ) : AIErrorDetail()

    /**
     * Network connectivity errors
     */
    @Keep
    data class NetworkError(
        @SerializedName("networkType") val networkType: NetworkErrorType,
        override val httpStatusCode: Int? = null,
        override val responseBody: String? = null,
        override val originalException: Throwable? = null,
        override val troubleshootingHint: String? = null
    ) : AIErrorDetail()

    /**
     * Service-level errors (500, 502, 503, 504, rate limits)
     */
    @Keep
    data class ServiceError(
        @SerializedName("httpCode") override val httpStatusCode: Int,
        @SerializedName("serviceType") val serviceType: ServiceErrorType,
        override val responseBody: String? = null,
        override val originalException: Throwable? = null,
        override val troubleshootingHint: String? = null
    ) : AIErrorDetail()

    /**
     * Response parsing/format errors
     */
    @Keep
    data class ParseError(
        @SerializedName("parseType") val parseType: ParseErrorType,
        @SerializedName("invalidData") val invalidData: String? = null,
        override val httpStatusCode: Int? = null,
        override val responseBody: String? = null,
        override val originalException: Throwable? = null,
        override val troubleshootingHint: String? = null
    ) : AIErrorDetail()

    /**
     * Model availability errors
     */
    @Keep
    data class ModelError(
        @SerializedName("modelId") val modelId: String,
        @SerializedName("providerId") val providerId: String,
        @SerializedName("reason") val reason: ModelErrorReason,
        override val httpStatusCode: Int? = null,
        override val responseBody: String? = null,
        override val originalException: Throwable? = null,
        override val troubleshootingHint: String? = null
    ) : AIErrorDetail()

    /**
     * Insufficient balance/credits error (HTTP 402)
     */
    @Keep
    data class InsufficientBalanceError(
        @SerializedName("currentBalance") val currentBalance: Int,
        @SerializedName("requiredBalance") val requiredBalance: Int,
        @SerializedName("diffBalance") val diffBalance: Int,
        override val httpStatusCode: Int = 402,
        override val responseBody: String? = null,
        override val originalException: Throwable? = null,
        override val troubleshootingHint: String? = null
    ) : AIErrorDetail()
}

/**
 * API key error reasons
 */
@Keep
enum class ApiKeyErrorReason {
    NOT_CONFIGURED,      // API key not set in config
    INVALID_FORMAT,      // API key format validation failed (prefix check)
    EMPTY_KEY,           // API key is empty string
    REVOKED              // API key revoked by provider
}

/**
 * Authentication error types
 */
@Keep
enum class AuthErrorType {
    INVALID_API_KEY,     // 401 - wrong key
    PERMISSION_DENIED,   // 403 - access forbidden
    INSUFFICIENT_SCOPE   // 403 - missing permissions
}

/**
 * Network error types
 */
@Keep
enum class NetworkErrorType {
    CONNECTION_TIMEOUT,       // ConnectException
    READ_TIMEOUT,            // SocketTimeoutException (read)
    DNS_RESOLUTION_FAILED,   // UnknownHostException
    SSL_HANDSHAKE_FAILED,    // SSLException
    CONNECTION_REFUSED,      // ConnectException (refused)
    NETWORK_UNREACHABLE      // General network issue
}

/**
 * Service error types
 */
@Keep
enum class ServiceErrorType {
    INTERNAL_ERROR,          // 500
    BAD_GATEWAY,             // 502
    SERVICE_UNAVAILABLE,     // 503
    GATEWAY_TIMEOUT,         // 504
    RATE_LIMIT_EXCEEDED,     // 429
    OVERLOADED,              // Provider overloaded
    MODEL_OVERLOADED         // Specific model overloaded
}

/**
 * Parse error types
 */
@Keep
enum class ParseErrorType {
    JSON_PARSE_ERROR,        // Gson/JSON parsing failed
    INVALID_RESPONSE_FORMAT, // Response structure unexpected
    MISSING_REQUIRED_FIELD,  // Required field missing
    TYPE_MISMATCH,           // Data type doesn't match expected
    STREAM_PARSE_ERROR       // SSE stream parsing failed
}

/**
 * Model error reasons
 */
@Keep
enum class ModelErrorReason {
    NOT_AVAILABLE,           // Model not in provider's catalog
    DISABLED_BY_PROVIDER,    // Provider disabled this model
    DEPRECATED,              // Model deprecated
    CAPABILITY_MISMATCH      // Model lacks required capability (e.g. no vision)
}