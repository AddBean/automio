// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

/**
 * OpenAI Chat Completions 兼容 URL 拼接。
 *
 * 约定：[baseUrl] 可为根域名（如 https://api.openai.com）或已含版本前缀
 * （如 .../v1、方舟 .../api/plan/v3、.../compatible-mode/v1）。
 */
object OpenAiUrlHelper {

    fun normalizeBaseUrl(baseUrl: String): String =
        baseUrl.trim().trimEnd('/')

    fun resolveBaseUrl(override: String?, defaultApiUrl: String): String {
        val raw = override?.takeIf { it.isNotBlank() } ?: defaultApiUrl
        return normalizeBaseUrl(raw)
    }

    fun chatCompletionsUrl(baseUrl: String): String {
        val base = normalizeBaseUrl(baseUrl)
        if (base.isEmpty()) return base
        return when {
            base.endsWith("/chat/completions") -> base
            isVersionedApiRoot(base) -> "$base/chat/completions"
            else -> "$base/v1/chat/completions"
        }
    }

    fun modelsUrl(baseUrl: String): String {
        val base = normalizeBaseUrl(baseUrl)
        if (base.isEmpty()) return base
        return when {
            base.endsWith("/models") -> base
            isVersionedApiRoot(base) -> "$base/models"
            else -> "$base/v1/models"
        }
    }

    private fun isVersionedApiRoot(base: String): Boolean {
        // openai /v1、方舟 /api/plan/v3|/api/coding/v3、百炼 compatible-mode/v1 等
        return Regex(""".*/v\d+$""").containsMatchIn(base) ||
            base.contains("/compatible-mode/v1")
    }
}
