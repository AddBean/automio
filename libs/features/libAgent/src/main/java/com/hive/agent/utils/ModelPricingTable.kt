// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

/**
 * 单模型定价（USD per 1000 tokens）
 */
data class ModelPricing(
    val inputUsdPer1k: Double,
    val outputUsdPer1k: Double
) {
    fun calculate(promptTokens: Int, completionTokens: Int): Double {
        if (promptTokens <= 0 && completionTokens <= 0) return 0.0
        return (promptTokens / 1000.0) * inputUsdPer1k + (completionTokens / 1000.0) * outputUsdPer1k
    }
}

/**
 * 按 (provider, modelId) 提供 token 单价，用于在 API 不返回 cost 时估算费用。
 * 优先级：MMKV 覆盖配置 > 内置表 > null（未知模型不估算）
 */
object ModelPricingTable {

    private val builtIn: Map<String, ModelPricing> = mapOf(
        // DeepSeek (USD per 1k tokens, 参考公开定价)
        "deepseek:deepseek-chat" to ModelPricing(0.00014, 0.00028),
        "deepseek:deepseek-reasoner" to ModelPricing(0.00055, 0.00219),
        "deepseek:*" to ModelPricing(0.00014, 0.00028),
        // Ollama 本地免费
        "ollama:*" to ModelPricing(0.0, 0.0),
        // 百炼 / 通义 (示例，可按实际定价更新)
        "BailianCode:qwen3.5-plus" to ModelPricing(0.0002, 0.0006),
        "BailianCode:*" to ModelPricing(0.0002, 0.0006)
    )

    /**
     * 计算本次调用的估算费用（USD）。
     * @return 估算费用，未知 provider/model 时返回 null
     */
    @JvmStatic
    fun calculate(
        provider: String,
        modelId: String?,
        promptTokens: Int,
        completionTokens: Int
    ): Double? {
        if (promptTokens <= 0 && completionTokens <= 0) return 0.0
        val pricing = resolve(provider, modelId) ?: return null
        return pricing.calculate(promptTokens, completionTokens)
    }

    private fun resolve(provider: String, modelId: String?): ModelPricing? {
        val key = key(provider, modelId)
        // MMKV 覆盖可在后续扩展；当前仅用内置表
        return builtIn[key] ?: builtIn["$provider:*"]
    }

    private fun key(provider: String, modelId: String?): String {
        val m = modelId?.takeIf { it.isNotBlank() } ?: "*"
        return "$provider:$m"
    }
}
