// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.hive.plugin.agent.ModelCapabilities
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ModelType
import com.hive.plugin.agent.model.AIErrorDetail
import com.hive.plugin.agent.model.AuthErrorType
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 火山方舟 Coding Plan（OpenAI Compatible）。
 *
 * - Base URL: https://ark.cn-beijing.volces.com/api/coding/v3
 * - 须使用 Coding Plan 控制台专属 API Key（勿与 Agent Plan / 普通方舟 Key 混用）
 */
class ArkCodingPlanProvider : OpenAiCompatibleProvider(OpenAiCompatiblePresets.ARK_CODING_PLAN) {

    companion object {
        private const val CACHE_DURATION = 24 * 60 * 60 * 1000L
        private var cachedModels: List<ModelInfo>? = null
        private var cacheTimestamp: Long = 0L
    }

    override fun supportsEditableBaseUrl(): Boolean = false

    override fun resolveEffectiveBaseUrl(): String =
        OpenAiUrlHelper.normalizeBaseUrl(OpenAiCompatiblePresets.ARK_CODING_PLAN.apiUrl)

    override fun getReadTimeout(): Int = 180_000

    override fun classifyHttpError(
        httpCode: Int,
        responseBody: String,
        providerId: String
    ): AIErrorDetail {
        if (httpCode == 401 || httpCode == 403) {
            return AIErrorDetail.AuthenticationError(
                httpStatusCode = httpCode,
                errorType = AuthErrorType.INVALID_API_KEY,
                responseBody = responseBody,
                troubleshootingHint = GlobalApp.getString(
                    com.hive.i8n.R.string.ai_error_ark_coding_plan_auth_hint
                )
            )
        }
        return super.classifyHttpError(httpCode, responseBody, providerId)
    }

    override suspend fun getBuildInModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        try {
            if (isCacheValid()) return@withContext cachedModels!!
            val remote = fetchModelsFromApi()
            if (remote.isNotEmpty()) {
                cacheModels(remote)
                return@withContext remote
            }
        } catch (e: Exception) {
            DLog.e("ArkCodingPlanProvider", "获取模型列表失败: ${e.message}")
        }
        OpenAiCompatiblePresets.ARK_CODING_PLAN.toModelInfoList()
    }

    private fun isCacheValid(): Boolean =
        cachedModels != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION

    private fun cacheModels(models: List<ModelInfo>) {
        cachedModels = models
        cacheTimestamp = System.currentTimeMillis()
    }

    private fun fetchModelsFromApi(): List<ModelInfo> {
        if (getApiKey().isEmpty()) return emptyList()
        val url = OpenAiUrlHelper.modelsUrl(resolveEffectiveBaseUrl())
        if (url.isEmpty()) return emptyList()
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Automio/1.0")
                setRequestProperty("Authorization", "Bearer ${getApiKey()}")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            if (connection.responseCode != 200) {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                DLog.e("ArkCodingPlanProvider", "models API ${connection.responseCode}: $err")
                return emptyList()
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val resp = GsonHelper.getInstance().fromJson(body, ArkModelsResponse::class.java)
            resp.data.orEmpty().mapNotNull { item ->
                val id = item.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                ModelInfo(
                    modelId = id,
                    displayName = id,
                    providerId = OpenAiCompatiblePresets.ARK_CODING_PLAN.id,
                    buildIn = true,
                    capabilities = ModelCapabilities(
                        supportsFunctionCall = true,
                        supportsVision = id.contains("kimi", ignoreCase = true) ||
                            id.contains("vision", ignoreCase = true) ||
                            id.contains("vl", ignoreCase = true),
                        contextWindow = 256_000,
                        modelType = ModelType.CHAT
                    )
                )
            }.sortedBy { it.displayName }
        } catch (e: Exception) {
            DLog.e("ArkCodingPlanProvider", "请求 models 失败: ${e.message}")
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private data class ArkModelsResponse(
        val data: List<ArkModelItem>? = null
    )

    private data class ArkModelItem(
        val id: String? = null
    )
}
