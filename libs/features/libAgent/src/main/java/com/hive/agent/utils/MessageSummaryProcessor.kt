// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import android.text.TextUtils
import com.hive.agent.XAgent
import com.hive.agent.config.AIAgentConfig
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.agent.model.AIRequest
import com.hive.plugin.agent.model.AIRequestType
import com.hive.plugin.agent.model.AgentInput
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.AIResult
import com.hive.plugin.agent.model.FunctionCall
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import java.util.concurrent.ConcurrentHashMap

/**
 * 消息摘要处理器
 * 负责生成和管理任务执行历史的精简摘要
 *
 * 策略：
 * - 规则快速摘要：从整个 pending 生成工具链，作为桥接层（0 token 成本）
 * - AI 深度总结：待总结缓存满 16 条时触发，通过 AIServiceManager 实现
 * - 双层展示：AI历史摘要 + 最近快速摘要
 */
object MessageSummaryProcessor {

    private const val TAG = "MessageSummaryProcessor"

    // 待总结的消息队列：taskId -> List<ChatMessage>
    private val pendingSummaryMessages = ConcurrentHashMap<String, MutableList<ChatMessage>>()

    // AI 生成的历史摘要缓存：taskId -> String
    private val taskAISummaries = ConcurrentHashMap<String, String>()


    /**
     * 处理消息摘要（主入口）
     * @param taskId 任务ID
     * @param removedMessages 被删除的消息列表
     * @return 摘要文本（可为空）
     */
    suspend fun processSummary(
        taskId: String,
        removedMessages: List<ChatMessage>
    ): String {
        if (removedMessages.isEmpty()) return ""

        // 1. 先加入 pending
        pendingSummaryMessages
            .getOrPut(taskId) { mutableListOf() }
            .addAll(removedMessages)
        val pending = pendingSummaryMessages[taskId] ?: emptyList()

        // 2. quickSummary 从整个 pending 生成（桥接层，覆盖待总结部分）
        val quickSummary = buildQuickSummary(pending)

        // 3. 当 pending >= max 时触发 AI 深度总结
        if (pending.size >= AIAgentConfig.MemoryConfig.AI_SUMMARY_TRIGGER_COUNT) {
            val aiSummary = generateAISummary(taskId)
            if (aiSummary.isNotEmpty()) {
                // 有上次摘要时，AI 会输出完整合并摘要，直接替换
                taskAISummaries[taskId] = aiSummary
                pendingSummaryMessages[taskId]?.clear()
            }
        }

        // 4. 组合双层摘要
        return combineSummaries(
            aiSummary = taskAISummaries[taskId],
            quickSummary = quickSummary
        )
    }

    /**
     * 生成快速工具链摘要（规则提取，不调用AI）
     * 优先使用工具调用时的 description 参数（ScriptMcpModel 强制要求附带），否则回退到工具名
     * 格式：点击确认键→返回主页→搜索联系人
     */
    private fun buildQuickSummary(messages: List<ChatMessage>): String {
        return GlobalApp.getString(
            com.hive.i8n.R.string.task_memory_recent_label
        ) + "\n" + messages.joinToString("\n") { msg ->
            val role = when (msg.role) {
                MessageRole.TOOL -> GlobalApp.getString(com.hive.i8n.R.string.task_memory_tool_label)
                MessageRole.ASSISTANT -> "AI"
                MessageRole.USER -> GlobalApp.getString(com.hive.i8n.R.string.task_memory_user_label)
                else -> msg.role.name
            }
            val toolName = msg.toolCalls?.firstOrNull()?.function?.name ?: ""
            val toolDes = ("-" + msg.toolCalls?.firstOrNull()?.function?.getCallDescription()) ?: ""
            val msg = if (!TextUtils.isEmpty(msg.content)) {
                msg.content
            } else if (!TextUtils.isEmpty(msg.reasoningContent)) {
                msg.reasoningContent
            } else {
                ""
            }

            "$role: ${msg}\n[$toolName${toolDes}]"
        }.take(1000)

    }

    /** 从 FunctionCall.arguments 中获取调用说明（description 参数，工具调用时强制附带） */
    private fun FunctionCall.getCallDescription(): String =
        arguments.get("description")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.getAsString()
            ?.trim().orEmpty()
            .ifEmpty { name }

    /**
     * 调用 AI 生成深度摘要（通过 AIServiceManager）
     * 要求：精简到 30 字以内，只列关键步骤
     */
    private suspend fun generateAISummary(taskId: String): String {
        val messages = pendingSummaryMessages[taskId] ?: return ""
        if (messages.isEmpty()) return ""

        return try {

            val messagesText = buildQuickSummary(messages)
            val existingSummary = taskAISummaries[taskId]?.trim() ?: ""
            val promptContent = if (existingSummary.isNotEmpty()) {
                val previousLabel = GlobalApp.getString(
                    com.hive.i8n.R.string.task_memory_ai_summary_previous,
                    existingSummary
                )
                val newMessagesLabel =
                    GlobalApp.getString(com.hive.i8n.R.string.task_memory_ai_summary_new_messages)
                "$previousLabel\n\n$newMessagesLabel\n$messagesText"
            } else {
                messagesText
            }
            val summaryPrompt = GlobalApp.getString(
                com.hive.i8n.R.string.task_memory_ai_summary_prompt,
                promptContent
            )

            val manager = XAgent.getInstance().getAIServiceManager() ?: return ""
            val modelInfo = manager.getInferenceModel(InferenceType.TEXT)
            val targetProvider = if (modelInfo != null) {
                val p = manager.getProvider(modelInfo.providerId)
                if (p != null && manager.isProviderEnabled(modelInfo.providerId)) p else null
            } else null
            val provider = targetProvider ?: manager.getAvailableProvider() ?: return ""

            val model = modelInfo?.modelId?.takeIf { it.isNotEmpty() }
                ?: provider.getProviderInfo().defaultModelId
                ?: return ""

            val requestMessages = listOf(ChatMessage(MessageRole.USER, content = summaryPrompt))
            val input = AgentInput(requestMessages)
            val request = AIRequest(
                model = model,
                requestType = AIRequestType.CHAT_COMPLETION,
                input = input,
                inputOrigin = input
            )

            when (val result = provider.inference<ChatCompletionResponse>(request)) {
                is AIResult.Success -> result.data.content?.trim()?.take(200) ?: ""
                is AIResult.Failure -> {
                    DLog.e(
                        TAG, GlobalApp.getString(
                            com.hive.i8n.R.string.task_memory_ai_summary_failed,
                            result.error.getInfo()
                        )
                    )
                    ""
                }
            }
        } catch (e: Exception) {
            DLog.e(
                TAG, GlobalApp.getString(
                    com.hive.i8n.R.string.task_memory_ai_summary_failed,
                    e.message ?: ""
                )
            )
            ""
        }
    }

    /**
     * 组合 AI 历史摘要 + 快速摘要
     * 格式：
     * 【历史】打开微信→搜索联系人→发送消息
     * 最近: click→input→scroll
     */
    private fun combineSummaries(aiSummary: String?, quickSummary: String): String {
        val historyLabel = GlobalApp.getString(com.hive.i8n.R.string.task_memory_history_label)

        return when {
            !aiSummary.isNullOrEmpty() && quickSummary.isNotEmpty() ->
                "$historyLabel$aiSummary\n$quickSummary"

            !aiSummary.isNullOrEmpty() -> "$historyLabel$aiSummary"
            quickSummary.isNotEmpty() -> "$historyLabel$quickSummary"
            else -> ""
        }
    }

    /**
     * 清理任务摘要缓存
     * 建议在任务结束时调用
     */
    fun clearTaskSummary(taskId: String) {
        pendingSummaryMessages.remove(taskId)
        taskAISummaries.remove(taskId)
    }

    /**
     * 获取摘要统计信息（用于调试）
     */
    fun getSummaryStats(): Map<String, Any> {
        return mapOf(
            "taskCount" to taskAISummaries.size,
            "pendingTasks" to pendingSummaryMessages.keys.toList(),
            "aiSummaryCount" to taskAISummaries.size
        )
    }
}
