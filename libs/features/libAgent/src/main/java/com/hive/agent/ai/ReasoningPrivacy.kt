// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

/**
 * 思考内容隐私：状态摘要、工具 reason 日志、异常信息不得包含 reasoning 正文。
 * 聊天 UI 仍可通过消息字段展示思考过程。
 */
object ReasoningPrivacy {

    /**
     * @param reasoningContent 故意忽略，避免调用方把思考正文拼进摘要/日志。
     */
    fun publicAssistantText(
        content: String?,
        @Suppress("UNUSED_PARAMETER") reasoningContent: String? = null,
        emptyFallback: String = "思考完成"
    ): String = content?.trim()?.takeIf { it.isNotEmpty() } ?: emptyFallback

    fun toolReasonLog(publicAssistantText: String): String = publicAssistantText

    fun safeMetaLog(
        providerId: String,
        modelId: String,
        resolved: ResolvedReasoning
    ): String {
        val opts = resolved.effectiveOptions
        return "provider=$providerId model=$modelId" +
            " availability=${resolved.capabilities.availability}" +
            " enabled=${opts?.enabled}" +
            " effort=${opts?.effort}"
    }
}
