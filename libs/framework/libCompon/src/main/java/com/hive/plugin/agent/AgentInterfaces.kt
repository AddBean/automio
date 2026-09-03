// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent

import com.hive.plugin.agent.model.AIRequest
import com.hive.plugin.agent.model.AIResult
import com.hive.plugin.agent.model.AgentRequest
import com.hive.plugin.agent.model.AgentResult
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.ReasoningEffort
import com.hive.plugin.agent.model.TaskResult
import com.hive.plugin.agent.model.ToolDefinition

/**
 * Agent工具接口
 */
interface AgentToolClient {
    val id: String
    val name: String
    val description: String
    val supportedMethods: MutableList<String>
    /**
     * tool 的来源 workflow（scriptUid）列表。默认空，表示系统/内置或未标注来源。
     */
    val sources: List<String>
        get() = emptyList()

    /**
     * 可选：semver，用于导入冲突比较（阶段一先铺字段）。
     */
    val version: String?
        get() = null

    suspend fun initTools() {
    }

    suspend fun execute(request: AgentRequest): AgentResult<*>?

    fun stopExecute()

    fun onDestroy()

    fun toToolDefinitions(): List<ToolDefinition>
}

/**
 * Agent管理器接口，管理所有工具和工作流执行
 */
interface AgentManager {

    val agentContext: IAgentContext

    /**
     * 注册工具
     */
    fun registerTool(tool: AgentToolClient)

    /**
     * 注销工具
     */
    fun unregisterTool(toolId: String)

    /**
     * 分发请求给特定工具
     */
    suspend fun <T> dispatchRequest(request: AgentRequest): AgentResult<*>

    /**
     * 执行复杂工作流
     * 支持流式和非流式两种模式
     *
     * @param goal 工作流目标
     * @param useStream 是否使用流式推理，默认为 false
     * @return 工作流执行结果，流式模式下返回 Flow<TaskResult>，非流式模式下返回 TaskResult
     */
    suspend fun executeAgentTask(goal: AgentTaskGoal, useStream: Boolean = false): TaskResult?

    /**
     * 获取所有已注册的工具
     */
    fun getRegisteredTools(): List<AgentToolClient>

    /**
     * 将工具标记为主循环可见的全局工具。
     */
    fun markToolAsGlobal(toolId: String)

    /**
     * 取消全局工具标记。
     */
    fun unmarkToolAsGlobal(toolId: String)

    /**
     * 获取主循环可见的全局工具ID集合。
     */
    fun getGlobalToolIds(): Set<String>

    /**
     * 检查工具是否可用
     */
    fun isToolAvailable(toolId: String): Boolean

    fun stopExecution()

}


interface AIServiceProvider {

    fun getProviderInfo(): ProviderInfo

    suspend fun <T> inference(request: AIRequest): AIResult<T>

    suspend fun <T> streamInference(
        request: AIRequest,
        onChunkResponse: ((ChatCompletionResponse) -> Unit)? = null
    ): AIResult<T>

    fun getPerformanceScore(): Float

    fun stopInference(request: AIRequest? = null)

    fun isProviderReady(): Boolean

    fun isModelReady(modelId: String): Boolean

    fun getApiKey(): String?

    fun getTags(): List<String>

    suspend fun getModels(): List<ModelInfo>

    fun hasValidApiKey(): Boolean

    fun updateCustomModels(models: MutableList<ModelInfo>)


}

interface AIServiceManager {
    fun initAIServiceProviders()

    fun getAvailableProvider(): AIServiceProvider?

    fun registerProvider(provider: AIServiceProvider)

    fun unregisterProvider(providerId: String)

    fun clearProviders()

    fun enableProvider(providerId: String)

    fun disableProvider(providerId: String)

    fun getProviderList(): List<AIServiceProvider>

    fun getEnabledProviders(): List<AIServiceProvider>

    fun getProvider(providerId: String): AIServiceProvider?

    fun isProviderEnabled(providerId: String): Boolean


    suspend fun <T> inference(request: AIRequest): AIResult<T>

    suspend fun <T> streamInference(
        request: AIRequest,
        onChunkResponse: ((ChatCompletionResponse) -> Unit)? = null
    ): AIResult<T>

    fun stopInference(request: AIRequest? = null)


    /**
     * 启用Provider的模型
     */
    fun enableProviderModel(providerId: String, modelId: String)

    /**
     * 禁用Provider的模型
     */
    fun disableProviderModel(providerId: String, modelId: String)

    /**
     * 检查Provider的模型是否启用
     */
    fun isProviderModelEnabled(providerId: String, modelId: String): Boolean

    /**
     * 获取Provider的自定义模型列表
     */
    fun getProviderCustomModels(providerId: String): List<ModelInfo>

    /**
     * 添加Provider的自定义模型
     */
    fun addProviderCustomModel(providerId: String, modelInfo: ModelInfo)

    /**
     * 删除Provider的自定义模型
     */
    fun removeProviderCustomModel(providerId: String, modelId: String)

    /**
     * 检查Provider是否有自定义模型
     */
    fun hasProviderCustomModels(providerId: String): Boolean

    /**
     * 清空Provider的所有自定义模型
     */
    fun clearProviderCustomModels(providerId: String)


    fun setInferenceModel(type: InferenceType, model: ModelInfo?)

    /**
     * 获取普通推理模型信息
     */
    fun getInferenceModel(type: InferenceType): ModelInfo?

    /**
     * 获取Provider的默认模型
     */
    suspend fun loadDefaultInferenceModelIfNeeded(providerId: String)


}

enum class InferenceType(var type: Int) {
    TEXT(1001), IMAGE(1002);

    companion object {
        fun parserType(type: Int): InferenceType {
            return when (type) {
                1001 -> TEXT
                1002 -> IMAGE
                else -> TEXT
            }
        }
    }
}

/**
 * Provider信息
 */
data class ProviderInfo(
    val name: String,
    val displayName: String,
    val description: String,
    val isEnabled: Boolean,
    val defaultModelId: String?,
    val defaultMultiModelId: String?,
    val tags: List<String>? = null,
    val promptPrefix: String? = "",
    val promptSuffix: String? = "",
    val apiKeyPrefix: String? = "",
    val apiKeyValidateMsg: String = com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.compon_api_key_format_error),
    val apikeyEnabled: Boolean = true,
    val apiUrl: String = "",
    val apiKey: String? = null,
    val sortIndex: Int = 0
) : java.io.Serializable

/**
 * 模型信息
 */
data class ModelInfo(
    val modelId: String,
    val displayName: String,
    val providerId: String,
    val buildIn: Boolean,
    val capabilities: ModelCapabilities,
) : java.io.Serializable

/**
 * 模型能力配置
 */
data class ModelCapabilities(
    val supportsFunctionCall: Boolean = false,
    val supportsVision: Boolean = false,
    val contextWindow: Int = 0, // 上下文窗口大小，如128K
    val modelType: ModelType = ModelType.LLM,
    /** null 表示旧缓存或 Provider 未声明；通过 [reasoningCapabilitiesOrUnknown] 安全读取。 */
    val reasoning: ReasoningCapabilities? = null
) : java.io.Serializable

/** 模型可用的思考模式能力；默认未知，绝不主动猜测。 */
data class ReasoningCapabilities(
    val availability: ReasoningAvailability = ReasoningAvailability.UNKNOWN,
    val supportedEfforts: Set<ReasoningEffort> = emptySet(),
    val defaultEffort: ReasoningEffort? = null
) : java.io.Serializable

enum class ReasoningAvailability {
    UNKNOWN,
    UNSUPPORTED,
    OPTIONAL,
    REQUIRED
}

/**
 * 兼容 Gson 反序列化的旧 [ModelCapabilities]：旧数据的 reasoning 字段可能为 null。
 */
fun ModelCapabilities?.reasoningCapabilitiesOrUnknown(): ReasoningCapabilities =
    this?.reasoning ?: ReasoningCapabilities()

/**
 * 模型类型枚举
 */
enum class ModelType {
    LLM,           // 大语言模型
    CHAT           // 聊天模型
}
