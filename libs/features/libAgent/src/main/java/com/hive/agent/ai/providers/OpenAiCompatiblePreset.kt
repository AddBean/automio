// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import androidx.annotation.StringRes
import com.hive.plugin.agent.ModelCapabilities
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ModelType
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string

/**
 * OpenAI Chat Completions 兼容厂商预设（配置驱动，共用 [OpenAiCompatibleProvider]）。
 */
data class OpenAiCompatibleModelPreset(
    val modelId: String,
    val displayName: String,
    val supportsVision: Boolean = false,
    val supportsFunctionCall: Boolean = true,
    val contextWindow: Int = 128_000,
)

data class OpenAiCompatiblePreset(
    val id: String,
    @StringRes val displayNameRes: Int,
    @StringRes val descriptionRes: Int,
    /** 建议已含 /v1 或 /compatible-mode/v1 等版本前缀 */
    val apiUrl: String,
    /** 空字符串表示不校验前缀，仅要求 Key 非空 */
    val apiKeyPrefix: String = "",
    @StringRes val apiKeyValidateMsgRes: Int,
    val defaultModelId: String?,
    val defaultMultiModelId: String? = null,
    val sortIndex: Int,
    val tags: List<String> = listOf("LLM", "OpenAI-Compatible"),
    val models: List<OpenAiCompatibleModelPreset> = emptyList(),
) {
    fun displayName(): String = GlobalApp.getString(displayNameRes)
    fun description(): String = GlobalApp.getString(descriptionRes)
    fun apiKeyValidateMsg(): String = apiKeyValidateMsgRes.string()

    fun toModelInfoList(): List<ModelInfo> = models.map { m ->
        ModelInfo(
            modelId = m.modelId,
            displayName = m.displayName,
            providerId = id,
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = m.supportsFunctionCall,
                supportsVision = m.supportsVision,
                contextWindow = m.contextWindow,
                modelType = ModelType.CHAT
            )
        )
    }
}

object OpenAiCompatiblePresets {

    val BAILIAN = OpenAiCompatiblePreset(
        id = "bailian",
        displayNameRes = com.hive.i8n.R.string.ai_provider_bailian,
        descriptionRes = com.hive.i8n.R.string.ai_provider_bailian_desc,
        apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        apiKeyValidateMsgRes = com.hive.i8n.R.string.api_key_validation_bailian,
        defaultModelId = "qwen-plus",
        defaultMultiModelId = "qwen-vl-plus",
        sortIndex = 860,
        models = listOf(
            OpenAiCompatibleModelPreset("qwen-plus", "Qwen Plus", contextWindow = 131_072),
            OpenAiCompatibleModelPreset("qwen-max", "Qwen Max", contextWindow = 131_072),
            OpenAiCompatibleModelPreset("qwen-turbo", "Qwen Turbo", contextWindow = 131_072),
            OpenAiCompatibleModelPreset(
                "qwen-vl-plus", "Qwen VL Plus",
                supportsVision = true, contextWindow = 131_072
            ),
            OpenAiCompatibleModelPreset(
                "qwen-vl-max", "Qwen VL Max",
                supportsVision = true, contextWindow = 131_072
            ),
            OpenAiCompatibleModelPreset(
                "qwen3.5-plus", "Qwen 3.5 Plus",
                supportsVision = true, contextWindow = 1_000_000
            ),
        )
    )

    val KIMI = OpenAiCompatiblePreset(
        id = "kimi",
        displayNameRes = com.hive.i8n.R.string.ai_provider_kimi,
        descriptionRes = com.hive.i8n.R.string.ai_provider_kimi_desc,
        apiUrl = "https://api.moonshot.cn/v1",
        apiKeyValidateMsgRes = com.hive.i8n.R.string.api_key_validation_kimi,
        defaultModelId = "kimi-k2.5",
        defaultMultiModelId = "kimi-k2.5",
        sortIndex = 870,
        models = listOf(
            OpenAiCompatibleModelPreset(
                "kimi-k2.5", "Kimi K2.5",
                supportsVision = true, contextWindow = 256_000
            ),
            OpenAiCompatibleModelPreset(
                "kimi-k2.6", "Kimi K2.6",
                supportsVision = true, contextWindow = 256_000
            ),
            OpenAiCompatibleModelPreset(
                "moonshot-v1-128k-vision-preview", "Moonshot V1 128K Vision",
                supportsVision = true, contextWindow = 128_000
            ),
            OpenAiCompatibleModelPreset(
                "moonshot-v1-auto", "Moonshot V1 Auto", contextWindow = 128_000
            ),
        )
    )

    val SILICONFLOW = OpenAiCompatiblePreset(
        id = "siliconflow",
        displayNameRes = com.hive.i8n.R.string.ai_provider_siliconflow,
        descriptionRes = com.hive.i8n.R.string.ai_provider_siliconflow_desc,
        apiUrl = "https://api.siliconflow.cn/v1",
        apiKeyValidateMsgRes = com.hive.i8n.R.string.api_key_validation_siliconflow,
        defaultModelId = "deepseek-ai/DeepSeek-V3",
        defaultMultiModelId = "Qwen/Qwen2.5-VL-72B-Instruct",
        sortIndex = 840,
        models = listOf(
            OpenAiCompatibleModelPreset(
                "deepseek-ai/DeepSeek-V3", "DeepSeek V3", contextWindow = 64_000
            ),
            OpenAiCompatibleModelPreset(
                "deepseek-ai/DeepSeek-R1", "DeepSeek R1", contextWindow = 64_000
            ),
            OpenAiCompatibleModelPreset(
                "Qwen/Qwen2.5-72B-Instruct", "Qwen2.5 72B", contextWindow = 32_000
            ),
            OpenAiCompatibleModelPreset(
                "Qwen/Qwen2.5-VL-72B-Instruct", "Qwen2.5 VL 72B",
                supportsVision = true, contextWindow = 32_000
            ),
        )
    )

    val MIMO = OpenAiCompatiblePreset(
        id = "mimo",
        displayNameRes = com.hive.i8n.R.string.ai_provider_mimo,
        descriptionRes = com.hive.i8n.R.string.ai_provider_mimo_desc,
        apiUrl = "https://api.xiaomimimo.com/v1",
        apiKeyValidateMsgRes = com.hive.i8n.R.string.api_key_validation_mimo,
        defaultModelId = "mimo-v2.5-pro",
        defaultMultiModelId = "mimo-v2.5",
        sortIndex = 830,
        models = listOf(
            OpenAiCompatibleModelPreset(
                "mimo-v2.5-pro", "MiMo V2.5 Pro", contextWindow = 1_000_000
            ),
            OpenAiCompatibleModelPreset(
                "mimo-v2.5", "MiMo V2.5",
                supportsVision = true, contextWindow = 1_000_000
            ),
        )
    )

    val MINIMAX = OpenAiCompatiblePreset(
        id = "minimax",
        displayNameRes = com.hive.i8n.R.string.ai_provider_minimax,
        descriptionRes = com.hive.i8n.R.string.ai_provider_minimax_desc,
        apiUrl = "https://api.minimaxi.com/v1",
        apiKeyValidateMsgRes = com.hive.i8n.R.string.api_key_validation_minimax,
        defaultModelId = "MiniMax-M2.5",
        defaultMultiModelId = "MiniMax-Text-01",
        sortIndex = 820,
        models = listOf(
            OpenAiCompatibleModelPreset(
                "MiniMax-M2.5", "MiniMax M2.5", contextWindow = 1_000_000
            ),
            OpenAiCompatibleModelPreset(
                "MiniMax-Text-01", "MiniMax Text 01",
                supportsVision = true, contextWindow = 1_000_000
            ),
            OpenAiCompatibleModelPreset(
                "MiniMax-M3", "MiniMax M3",
                supportsVision = true, contextWindow = 1_000_000
            ),
        )
    )

    val STEPFUN = OpenAiCompatiblePreset(
        id = "stepfun",
        displayNameRes = com.hive.i8n.R.string.ai_provider_stepfun,
        descriptionRes = com.hive.i8n.R.string.ai_provider_stepfun_desc,
        apiUrl = "https://api.stepfun.com/v1",
        apiKeyValidateMsgRes = com.hive.i8n.R.string.api_key_validation_stepfun,
        defaultModelId = "step-2-mini",
        defaultMultiModelId = "step-1.5v-mini",
        sortIndex = 810,
        models = listOf(
            OpenAiCompatibleModelPreset("step-2-mini", "Step 2 Mini", contextWindow = 256_000),
            OpenAiCompatibleModelPreset(
                "step-1.5v-mini", "Step 1.5V Mini",
                supportsVision = true, contextWindow = 256_000
            ),
            OpenAiCompatibleModelPreset(
                "step-3.7-flash", "Step 3.7 Flash",
                supportsVision = true, contextWindow = 256_000
            ),
        )
    )

    /**
     * 火山方舟 Agent Plan（勿与 Coding Plan `/api/coding/v3` 混用 Key/URL）。
     * 由 [ArkAgentPlanProvider] 注册，不走 [createProviders]。
     */
    val ARK_AGENT_PLAN = OpenAiCompatiblePreset(
        id = "ark_agent_plan",
        displayNameRes = com.hive.i8n.R.string.ai_provider_ark_agent_plan,
        descriptionRes = com.hive.i8n.R.string.ai_provider_ark_agent_plan_desc,
        apiUrl = "https://ark.cn-beijing.volces.com/api/plan/v3",
        apiKeyValidateMsgRes = com.hive.i8n.R.string.api_key_validation_ark_agent_plan,
        defaultModelId = "ark-code-latest",
        defaultMultiModelId = "ark-code-latest",
        sortIndex = 865,
        tags = listOf("LLM", "OpenAI-Compatible", "AgentPlan"),
        models = listOf(
            OpenAiCompatibleModelPreset(
                "ark-code-latest", "Ark Code Latest",
                supportsVision = true, contextWindow = 256_000
            ),
            OpenAiCompatibleModelPreset(
                "kimi-k2.5", "Kimi K2.5",
                supportsVision = true, contextWindow = 256_000
            ),
            OpenAiCompatibleModelPreset(
                "kimi-k2.6", "Kimi K2.6",
                supportsVision = true, contextWindow = 256_000
            ),
            OpenAiCompatibleModelPreset(
                "deepseek-v4-pro", "DeepSeek V4 Pro", contextWindow = 256_000
            ),
            OpenAiCompatibleModelPreset(
                "glm-5.2", "GLM 5.2", contextWindow = 256_000
            ),
            OpenAiCompatibleModelPreset(
                "doubao-seed-code", "Doubao Seed Code", contextWindow = 256_000
            ),
        )
    )

    /**
     * 火山方舟 Coding Plan（勿与 Agent Plan `/api/plan/v3` 混用 Key/URL）。
     * 由 [ArkCodingPlanProvider] 注册。
     */
    val ARK_CODING_PLAN = OpenAiCompatiblePreset(
        id = "ark_coding_plan",
        displayNameRes = com.hive.i8n.R.string.ai_provider_ark_coding_plan,
        descriptionRes = com.hive.i8n.R.string.ai_provider_ark_coding_plan_desc,
        apiUrl = "https://ark.cn-beijing.volces.com/api/coding/v3",
        apiKeyValidateMsgRes = com.hive.i8n.R.string.api_key_validation_ark_coding_plan,
        defaultModelId = "ark-code-latest",
        defaultMultiModelId = "ark-code-latest",
        sortIndex = 864,
        tags = listOf("LLM", "OpenAI-Compatible", "CodePlan"),
        models = listOf(
            OpenAiCompatibleModelPreset(
                "ark-code-latest", "Ark Code Latest",
                supportsVision = true, contextWindow = 256_000
            ),
            OpenAiCompatibleModelPreset(
                "doubao-seed-code", "Doubao Seed Code", contextWindow = 256_000
            ),
            OpenAiCompatibleModelPreset(
                "kimi-k2.5", "Kimi K2.5",
                supportsVision = true, contextWindow = 256_000
            ),
            OpenAiCompatibleModelPreset(
                "deepseek-v3.2", "DeepSeek V3.2", contextWindow = 256_000
            ),
            OpenAiCompatibleModelPreset(
                "glm-4.7", "GLM 4.7", contextWindow = 256_000
            ),
        )
    )

    /** 内置 OpenAI 兼容预设（不含 openai / openrouter / deepseek / 方舟 Plan 等独立实现） */
    val ALL: List<OpenAiCompatiblePreset> = listOf(
        KIMI,
        BAILIAN,
        SILICONFLOW,
        MIMO,
        MINIMAX,
        STEPFUN,
    )

    fun createProviders(): List<OpenAiCompatibleProvider> =
        ALL.map { OpenAiCompatibleProvider(it) }
}
