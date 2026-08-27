// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import com.hive.agent.config.AIAgentConfig
import com.hive.utils.GlobalApp
import com.hive.agent.config.AIAgentConfig.BaseConfig.MAX_ATTACHMENT_KEEP_REQUEST
import com.hive.agent.config.AIAgentConfig.BaseConfig.MAX_HISTORY_COUNT
import com.hive.agent.config.AIAgentConfig.BaseConfig.MAX_NOT_SIMPLIFY_COUNT
import com.hive.agent.config.AIAgentConfig.BaseConfig.MAX_TEXT_SIZE_SIMPLIFY_TEXT
import com.google.gson.reflect.TypeToken
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.model.AttachmentType
import com.hive.plugin.agent.model.ChatAttachment
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.utils.debug.DLog
import com.hive.utils.utils.GsonHelper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.Proxy

object AgentMessageUtils {

    private const val TAG = "AgentMessageUtils"

    private val blobHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .build()
    }

    fun processSystemMessage(messages: List<ChatMessage>, selectedModel: ModelInfo? = null): List<ChatMessage> {
        messages.firstOrNull { it.role == MessageRole.SYSTEM }?.run {
            this.content = AIAgentConfig.PromptDefaults.getAutoSystemPrompt(
                supportsVision = selectedModel?.capabilities?.supportsVision
            )
        }
        return messages
    }


    /** 处理并复制消息 */
    suspend fun processAndCopyMessages(
        taskId: String,
        messages: List<ChatMessage>,
        onMemoryCompressing: ((Boolean) -> Unit)? = null
    ): List<ChatMessage> {
        var chatMessages = trimMessages(taskId, messages, onMemoryCompressing)
        chatMessages = ensureMessagesLegality(chatMessages)
        chatMessages = simplifyTextMessages(chatMessages)
        chatMessages = simplifyToolMessages(chatMessages)
        chatMessages = simplifyImageMessages(chatMessages)
        return chatMessages
    }

    /** 清理消息，保留系统消息，保留用户消息，保留最新的ASSISTANT/TOOL消息 */
    private suspend fun trimMessages(
        taskId: String,
        messages: List<ChatMessage>,
        onMemoryCompressing: ((Boolean) -> Unit)? = null
    ): MutableList<ChatMessage> {
        // 创建消息的深度副本，确保线程安全
        val chatMessages = messages.map { it.copy() }.toMutableList()

        if (chatMessages.size > MAX_HISTORY_COUNT) {
            // 保留所有SYSTEM消息
            val systemMsgs = chatMessages.filter { it.role == MessageRole.SYSTEM }

            // 获取所有ASSISTANT和TOOL消息
            val assistantToolMsgs = chatMessages.filter {
                it.role == MessageRole.ASSISTANT || it.role == MessageRole.TOOL
            }

            // ========== 摘要处理（仅当记忆开关开启且有待删除消息时） ==========
            val removedMessages = assistantToolMsgs.dropLast(MAX_HISTORY_COUNT).takeLast(1)
            val summaryText = if (AIAgentConfig.MemoryConfig.isTaskMemoryEnabled() && removedMessages.isNotEmpty()) {
                try {
                    onMemoryCompressing?.invoke(true)
                    MessageSummaryProcessor.processSummary(taskId, removedMessages)
                } finally {
                    onMemoryCompressing?.invoke(false)
                }
            } else {
                ""
            }

            // 注入摘要到系统消息（仅当有内容时，避免空标签）
            if (summaryText.isNotEmpty()) {
                systemMsgs.firstOrNull()?.let { systemMsg ->
                    // 不强制覆盖 system prompt：主循环/skill 子循环都可能自定义 SYSTEM
                    // 这里采用“追加摘要”的方式，避免把 skill 的 systemPrompt 覆盖成 agent.md
                    val base = systemMsg.content?.takeIf { it.isNotBlank() }
                        ?: AIAgentConfig.PromptDefaults.getAutoSystemPrompt()
                    systemMsg.content = "$base\n\n$summaryText"
                }
            }
            // ========== 摘要处理结束 ==========

            // 保留最新的ASSISTANT/TOOL消息
            val latestAssistantToolMsgs = assistantToolMsgs.takeLast(MAX_HISTORY_COUNT)

            // 重新构建消息列表，按时间戳排序
            val newMessages = mutableListOf<ChatMessage>()

            // 1. 首先添加所有SYSTEM消息
            newMessages.addAll(systemMsgs)

            // 2. 按时间戳顺序处理其他消息
            val nonSystemMsgs = chatMessages.filter { it.role != MessageRole.SYSTEM }
            val sortedNonSystemMsgs = nonSystemMsgs.sortedBy { it.timestamp }

            for (msg in sortedNonSystemMsgs) {
                when (msg.role) {
                    MessageRole.USER -> {
                        // 保留所有USER消息
                        newMessages.add(msg)
                    }

                    MessageRole.ASSISTANT, MessageRole.TOOL -> {
                        // 只保留在最新列表中的ASSISTANT/TOOL消息
                        if (latestAssistantToolMsgs.any { it.timestamp == msg.timestamp }) {
                            newMessages.add(msg)
                        }
                    }

                    else -> {
                        // 保留其他类型的消息
                        newMessages.add(msg)
                    }
                }
            }

            chatMessages.clear()
            chatMessages.addAll(newMessages)
        }

        return chatMessages
    }


    /**
     * 清理非法 tool 序列，满足 OpenAI 兼容接口约束：
     * assistant.tool_calls 后面必须紧邻等量 tool 消息，且每个 tool_call_id 都有对应响应。
     * 中间插入 user/其它 role、缺响应、或 id 对不上时，剥离 tool_calls 并丢弃孤儿 tool。
     */
    private fun ensureMessagesLegality(messages: List<ChatMessage>): MutableList<ChatMessage> {
        if (messages.isEmpty()) return messages.toMutableList()

        val result = mutableListOf<ChatMessage>()
        var index = 0
        while (index < messages.size) {
            val message = messages[index]
            if (message.role == MessageRole.ASSISTANT && !message.toolCalls.isNullOrEmpty()) {
                val expectedIds = message.toolCalls!!
                    .map { it.id }
                    .filter { it.isNotEmpty() }
                    .toSet()
                val toolResponses = mutableListOf<ChatMessage>()
                var cursor = index + 1
                while (cursor < messages.size && messages[cursor].role == MessageRole.TOOL) {
                    toolResponses.add(messages[cursor])
                    cursor++
                }
                val responseIds = toolResponses.mapNotNull { it.toolCallId?.takeIf { id -> id.isNotEmpty() } }.toSet()
                val isComplete = expectedIds.isNotEmpty() && expectedIds == responseIds
                if (isComplete) {
                    result.add(message)
                    result.addAll(toolResponses)
                } else {
                    result.add(message.copy(toolCalls = null))
                }
                index = cursor
                continue
            }

            if (message.role == MessageRole.TOOL) {
                // 无前置完整 assistant.tool_calls 的孤儿 tool
                index++
                continue
            }

            result.add(message)
            index++
        }
        return result
    }


    /** 简化工具调用消息 */
    private fun simplifyToolMessages(chatMessages: MutableList<ChatMessage>): MutableList<ChatMessage> {
        // 简化工具调用消息 - 使用索引遍历避免并发修改
        val toolMessages = chatMessages.filter { it.role == MessageRole.TOOL }
        for (idx in toolMessages.indices) {
            val msg = toolMessages[idx]
            if (msg.role == MessageRole.TOOL && idx < toolMessages.size - MAX_NOT_SIMPLIFY_COUNT) {
                if ((msg.toolCallResult?.length ?: 0) > MAX_TEXT_SIZE_SIMPLIFY_TEXT) {
                    // 找到对应的消息在chatMessages中的位置并更新
                    val messageIndex = chatMessages.indexOf(msg)
                    if (messageIndex != -1) {
                        val simplifiedMsg = GlobalApp.getString(com.hive.i8n.R.string.agent_tool_result_simplified)
                        chatMessages[messageIndex] = msg.copy(
                            content = simplifiedMsg,
                            toolCallResult = simplifiedMsg
                        )
                    }
                }
            }
        }
        return chatMessages
    }

    /** 简化多模态消息：仅保留最近 MAX_ATTACHMENT_KEEP_REQUEST 条带附件的消息，其余清空 attachments 以节省 token */
    private fun simplifyImageMessages(chatMessages: MutableList<ChatMessage>): MutableList<ChatMessage> {
        val imageMessages = chatMessages.filter { it.attachments.isNotEmpty() }
        for (idx in imageMessages.indices) {
            if (idx >= imageMessages.size - MAX_ATTACHMENT_KEEP_REQUEST) continue
            val msg = imageMessages[idx]
            val messageIndex = chatMessages.indexOf(msg)
            if (messageIndex != -1) {
                chatMessages[messageIndex] = msg.copy(attachments = mutableListOf())
            }
        }
        return chatMessages
    }

    /** 简化text调用消息 */
    private fun simplifyTextMessages(chatMessages: MutableList<ChatMessage>): MutableList<ChatMessage> {
        // 简化工具调用消息 - 使用索引遍历避免并发修改
        val messages = chatMessages.toMutableList()
        for (idx in messages.indices) {
            val msg = messages[idx]
            msg.reasoningContent = "-"
        }
        return messages
    }

    /**
     * 将 blob 信息转成 base64，requestMessage先深拷贝一份（可用 gson），再修改返回
     */
    fun processAttachMessage(requestMessage: List<ChatMessage>): List<ChatMessage> {
        val gson = GsonHelper.getInstance().getGson()
        val type = object : TypeToken<List<ChatMessage>>() {}.type
        val copiedMessages: List<ChatMessage> = try {
            gson.fromJson(gson.toJson(requestMessage), type) ?: emptyList()
        } catch (e: Exception) {
            DLog.w(TAG, "processAttachMessage: deep copy failed, fallback to shallow copy: ${e.message}")
            requestMessage.map { it.copy() }
        }

        copiedMessages.forEach { msg ->
            if (msg.attachments.isEmpty()) return@forEach
            msg.attachments.forEach { att ->
                if (att.type != AttachmentType.IMAGE) return@forEach
                if (!att.base64.isNullOrEmpty()) return@forEach
                val url = att.url ?: return@forEach
                if (!isLocalBlobUrl(url)) return@forEach
                tryFillBase64FromBlobUrl(att, url)
            }
            // blob 服务未启动或已过期时，移除无法获取的图片附件
            msg.attachments.removeAll { att ->
                att.type == AttachmentType.IMAGE &&
                    att.base64.isNullOrEmpty() &&
                    att.url != null && isLocalBlobUrl(att.url!!)
            }
        }

        return copiedMessages
    }

    private fun isLocalBlobUrl(url: String): Boolean {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) return false
        if (!url.contains("/blob/")) return false
        val host = try {
            url.removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
                .substringBefore(":")
        } catch (_: Exception) {
            null
        } ?: return false
        return try {
            InetAddress.getByName(host).isLoopbackAddress
        } catch (_: Exception) {
            false
        }
    }

    private fun tryFillBase64FromBlobUrl(att: ChatAttachment, url: String) {
        val req = Request.Builder().url(url).get().build()
        val resp = blobHttpClient.newCall(req).execute()
        resp.use { r ->
            if (!r.isSuccessful) {
                DLog.w(TAG, "blob fetch failed: http=${r.code}, url=$url")
                return
            }
            val bytes = r.body?.bytes()
            if (bytes == null || bytes.isEmpty()) {
                DLog.w(TAG, "blob fetch empty body: url=$url")
                return
            }
            val mimeFromHeader = r.header("Content-Type")?.substringBefore(';')?.trim()
            val mime = att.mimeType ?: mimeFromHeader ?: "image/jpeg"

            // 统一成 data URL（与 FileUtils.convertLocalFileToBase64 返回格式一致）
            val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            att.base64 = "data:$mime;base64,$b64"
        }
    }
}
