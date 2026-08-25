// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.hive.agent.R
import com.hive.extension.visibleOrGone
import com.hive.plugin.agent.model.AgentErrorCode
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AIErrorDetail
import com.hive.plugin.agent.model.ApiKeyErrorReason
import com.hive.plugin.agent.model.AuthErrorType
import com.hive.plugin.agent.model.NetworkErrorType
import com.hive.plugin.agent.model.ServiceErrorType
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.GlobalApp

/**
 * Agent AI Error Dialog - Simplified user-friendly error display
 *
 * Features:
 * - Clean, minimal UI showing only essential information
 * - User-friendly error messages with actionable hints
 * - Smart action buttons based on error type:
 *   - "Go to Settings" for API key errors
 *   - "Retry" for recoverable errors (network, rate limits, timeouts)
 *   - "Dismiss" for all errors
 */
class DialogAgentError(context: Context?) : BaseScriptDialog(context) {

    private var tvTitle: TextView? = null
    private var tvErrorMessage: TextView? = null
    private var tvTroubleshooting: TextView? = null
    private var btnRetry: View? = null
    private var btnSettings: View? = null
    private var btnDismiss: TextView? = null

    // Hidden views for internal tracking
    private var tvErrorCode: TextView? = null
    private var tvErrorType: TextView? = null
    private var btnToggleDetails: TextView? = null
    private var layoutTechnical: View? = null

    private var onRetryListener: (() -> Unit)? = null
    private var onSettingsListener: (() -> Unit)? = null
    private var onDismissListener: (() -> Unit)? = null

    override fun getWindowLayoutId(): Int = R.layout.dialog_agent_error

    override fun initWindow() {
        // Main UI elements
        tvTitle = findViewById(R.id.tv_title)
        tvErrorMessage = findViewById(R.id.tv_error_message)
        tvTroubleshooting = findViewById(R.id.tv_troubleshooting)
        btnRetry = findViewById(R.id.btn_retry)
        btnSettings = findViewById(R.id.btn_settings)
        btnDismiss = findViewById(R.id.tv_btn_cancel)

        // Hidden elements (not shown to user)
        tvErrorCode = findViewById(R.id.tv_error_code)
        tvErrorType = findViewById(R.id.tv_error_type)
        btnToggleDetails = findViewById(R.id.btn_toggle_details)
        layoutTechnical = findViewById(R.id.layout_technical)

        // Setup click listeners
        btnDismiss?.setOnClickListener {
            onDismissListener?.invoke()
            dismiss()
        }

        btnRetry?.setOnClickListener {
            onRetryListener?.invoke()
            dismiss()
        }

        btnSettings?.setOnClickListener {
            onSettingsListener?.invoke()
            dismiss()
        }
    }

    /**
     * Set error information to display
     */
    fun setError(error: AgentError): DialogAgentError {
        // Set user-friendly error message
        val message = getUserFriendlyMessage(error)
        tvErrorMessage?.text = message

        // Set troubleshooting hint if available
        val hint = getTroubleshootingHint(error)
        if (hint != null) {
            tvTroubleshooting?.text = hint
            tvTroubleshooting?.visibility = View.VISIBLE
        } else {
            tvTroubleshooting?.visibility = View.GONE
        }

        // Setup smart action buttons
        setupActionButtons(error)

        // Store error code internally (not shown to user)
        tvErrorCode?.text = "Error Code: ${error.code.code}"

        return this
    }

    /**
     * Get user-friendly error message
     */
    private fun getUserFriendlyMessage(error: AgentError): String {
        val detail = error.aiErrorDetail

        return when {
            // API Key errors
            detail is AIErrorDetail.ApiKeyError -> {
                when (detail.reason) {
                    ApiKeyErrorReason.NOT_CONFIGURED ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_apikey_not_configured)
                    ApiKeyErrorReason.INVALID_FORMAT ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_apikey_invalid)
                    else ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_apikey_error)
                }
            }

            // Authentication errors
            detail is AIErrorDetail.AuthenticationError -> {
                when (detail.errorType) {
                    AuthErrorType.INVALID_API_KEY ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_auth_invalid_key)
                    AuthErrorType.PERMISSION_DENIED ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_auth_permission)
                    else ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_auth_error)
                }
            }

            // Network errors
            detail is AIErrorDetail.NetworkError -> {
                when (detail.networkType) {
                    NetworkErrorType.CONNECTION_TIMEOUT,
                    NetworkErrorType.READ_TIMEOUT ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_network_timeout)
                    NetworkErrorType.DNS_RESOLUTION_FAILED ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_network_dns)
                    NetworkErrorType.NETWORK_UNREACHABLE ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_network_unreachable)
                    else ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_network_error)
                }
            }

            // Service errors
            detail is AIErrorDetail.ServiceError -> {
                when (detail.serviceType) {
                    ServiceErrorType.RATE_LIMIT_EXCEEDED ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_rate_limit)
                    ServiceErrorType.SERVICE_UNAVAILABLE ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_service_unavailable)
                    ServiceErrorType.GATEWAY_TIMEOUT ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_service_timeout)
                    else ->
                        GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_service_error)
                }
            }

            // Model errors
            detail is AIErrorDetail.ModelError -> {
                GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_model_unavailable)
            }

            // Provider quota / billing errors
            detail is AIErrorDetail.InsufficientBalanceError -> {
                GlobalApp.getString(
                    com.hive.i8n.R.string.ai_error_msg_insufficient_balance,
                    detail.diffBalance
                )
            }

            // Parse errors
            detail is AIErrorDetail.ParseError -> {
                GlobalApp.getString(com.hive.i8n.R.string.ai_error_msg_parse_error)
            }

            // Default error message
            else -> error.getInfo()
        }
    }

    /**
     * Get troubleshooting hint for the error
     */
    private fun getTroubleshootingHint(error: AgentError): String? {
        return error.aiErrorDetail?.troubleshootingHint
    }

    /**
     * Setup smart action buttons based on error type
     */
    private fun setupActionButtons(error: AgentError) {
        val shouldShowSettings = shouldShowSettingsButton(error)
        val canRetry = canRetryError(error)

        btnSettings?.visibleOrGone(shouldShowSettings)
        btnRetry?.visibleOrGone(canRetry)

        // Adjust dismiss button text
        btnDismiss?.setText(
            if (shouldShowSettings || canRetry) {
                com.hive.i8n.R.string.ai_error_dismiss
            } else {
                com.hive.i8n.R.string.ok
            }
        )
    }

    /**
     * Determine if error is retryable
     */
    private fun canRetryError(error: AgentError): Boolean {
        val detail = error.aiErrorDetail

        return when {
            error.code == AgentErrorCode.AI_REQUEST_CANCEL -> false
            detail is AIErrorDetail.ApiKeyError -> false
            detail is AIErrorDetail.AuthenticationError -> false
            detail is AIErrorDetail.ParseError -> false
            detail is AIErrorDetail.NetworkError -> false
            detail is AIErrorDetail.InsufficientBalanceError -> false
            detail is AIErrorDetail.ServiceError -> {
                detail.serviceType in listOf(
                    ServiceErrorType.RATE_LIMIT_EXCEEDED,
                    ServiceErrorType.GATEWAY_TIMEOUT,
                    ServiceErrorType.SERVICE_UNAVAILABLE
                )
            }
            else -> true
        }
    }

    /**
     * Determine if should show "Go to Settings" button
     */
    private fun shouldShowSettingsButton(error: AgentError): Boolean {
        return error.aiErrorDetail is AIErrorDetail.ApiKeyError
    }

    /**
     * Set retry listener
     */
    fun setOnRetryListener(listener: () -> Unit): DialogAgentError {
        onRetryListener = listener
        return this
    }

    /**
     * Set settings navigation listener
     */
    fun setOnSettingsListener(listener: () -> Unit): DialogAgentError {
        onSettingsListener = listener
        return this
    }

    /**
     * Set dismiss listener
     */
    fun setOnDismissListener(listener: () -> Unit): DialogAgentError {
        onDismissListener = listener
        return this
    }
}
