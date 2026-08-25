// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.AIServiceManager
import com.hive.plugin.agent.AIServiceProvider
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.agent.model.AIRequest
import com.hive.plugin.agent.model.AIRequestType
import com.hive.plugin.agent.model.AIResult
import com.hive.plugin.agent.model.AgentInput
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.utils.AiRequestHelper.lastErrorMessage
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object AiRequestHelper {

    private val TAG = AiRequestHelper::class.java.name

    /** 最后一次请求失败时的错误信息，供 CmdAiRequest 等使用 */
    @Volatile
    var lastErrorMessage: String? = null
        private set

    private fun getAIServiceManager(): AIServiceManager? {
        val provider = ComponentManager.getInstance()
            .getProvider(IAgentProvider::class.java) as? IAgentProvider
        return provider?.getAIServiceManager()
    }

    /**
     * 使用 libAgent 的 AIServiceManager 进行同步 AI 请求。
     * 无可用 Provider 或未配置模型时返回 null，并将原因写入 [lastErrorMessage]。
     */
    fun requestSync(srcContent: String): String? {
        lastErrorMessage = null
        val prompt = srcContent.substring(0, minOf(srcContent.length, 1500))

        val manager = getAIServiceManager()
        if (manager == null) {
            lastErrorMessage = GlobalApp.getString(com.hive.i8n.R.string.error_ai_no_provider)
            DLog.e(TAG, "requestSync: AIServiceManager not available")
            return null
        }

        // 使用与「用户选择的文本模型」对应的 Provider，避免选 OpenRouter 的模型却走到 DeepSeek
        val modelInfo = manager.getInferenceModel(InferenceType.TEXT)
        val targetProvider: AIServiceProvider? = if (modelInfo != null) {
            val p = manager.getProvider(modelInfo.providerId)
            if (p != null && manager.isProviderEnabled(modelInfo.providerId)) p else null
        } else null
        val provider = targetProvider ?: manager.getAvailableProvider()
        if (provider == null) {
            lastErrorMessage = GlobalApp.getString(com.hive.i8n.R.string.error_ai_no_provider)
            DLog.e(TAG, "requestSync: no available AI provider")
            return null
        }

        val model = modelInfo?.modelId?.takeIf { it.isNotEmpty() }
            ?: provider.getProviderInfo().defaultModelId
        if (model.isNullOrEmpty()) {
            lastErrorMessage = GlobalApp.getString(com.hive.i8n.R.string.error_ai_no_provider)
            DLog.e(TAG, "requestSync: no text model configured")
            return null
        }

        val messages = listOf(ChatMessage(MessageRole.USER, content = prompt))
        val input = AgentInput(messages)
        val request = AIRequest(
            model = model,
            requestType = AIRequestType.CHAT_COMPLETION,
            input = input,
            inputOrigin = input
        )

        return runBlocking(Dispatchers.IO) {
            try {
                when (val result = provider.inference<ChatCompletionResponse>(request)) {
                    is AIResult.Success -> result.data.content?.takeIf { it.isNotEmpty() } ?: ""
                    is AIResult.Failure -> {
                        lastErrorMessage = result.error.getInfo()
                        DLog.e(TAG, "requestSync: inference failed ${result.error.getInfo()}")
                        null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                lastErrorMessage = e.message ?: e.toString()
                DLog.e(TAG, "requestSync: exception:${e.message}")
                null
            }
        }
    }

    /**
     * 根据用户目标和命令列表，用 AI 生成简短的脚本名（10 字内）。
     * 失败时返回 null，调用方需用默认 tag 兜底。
     */
    fun generateScriptName(commands: List<String>, userGoal: String?): String? {
        val goalPart = userGoal?.trim()?.take(200) ?: ""
        val cmdSummary = commands.take(15).joinToString("\n") { it.take(80) }
        val prompt = GlobalApp.getString(com.hive.i8n.R.string.ai_name_prompt_script)
            .format(goalPart, cmdSummary)
        val raw = requestSync(prompt) ?: return null
        val sanitized = sanitizeForFileName(raw.trim().take(20))
        return sanitized.takeIf { it.isNotEmpty() }
    }

    /**
     * 去除文件名非法字符，避免保存失败。
     */
    private fun sanitizeForFileName(s: String): String {
        return s.replace(Regex("""[/\\:*?"<>|]"""), "").trim()
    }

}
