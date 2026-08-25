// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.hive.agent.R
import com.hive.agent.views.IAgentChatView
import com.hive.base.BaseLayout
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.MessageStatus
import com.hive.script.extensions.submitDataSetsWithType
import com.hive.views.list_view.ListRecyclerView

/**
 * Agent聊天视图
 *
 * 使用示例：
 * ```kotlin
 * // 在Fragment或Activity中
 * val chatView = AgentChatView(context)
 *
 * // 添加测试消息
 * chatView.addTestMessages()
 *
 * // 或者添加自定义消息
 * chatView.addMessage(ChatMessage(
 *     role = MessageRole.USER,
 *     content = "用户消息"
 * ))
 *
 * // 更新任务状态
 * chatView.updateTaskStatus("taskId", ExecutionStatus.RUNNING)
 * ```
 */
class AgentChatView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs),
    IAgentChatView {

    private lateinit var listRecyclerView: ListRecyclerView
    private lateinit var chatMessageViewFactory: ChatMessageViewFactory

    private var messages = mutableListOf<ChatMessage>()
    private var displayedDataList: List<Pair<Int, Any?>> = emptyList()
    private var isCompressingMemory = false
    private var onVisibleMessageStateChanged: ((Boolean) -> Unit)? = null

    private data class MessageFingerprint(
        val role: MessageRole,
        val content: String?,
        val reasoningContent: String?,
        val toolCallId: String?,
        val toolCallResult: String?,
        val toolCallsSize: Int,
        val status: MessageStatus,
        val attachmentsSize: Int,
    )

    override fun initView(view: View?) {
        listRecyclerView = findViewById(R.id.listRecyclerView)

        // 初始化聊天消息工厂
        chatMessageViewFactory = ChatMessageViewFactory(context)

        // 设置RecyclerView
        listRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listRecyclerView.setItemViewFactory(chatMessageViewFactory)
        listRecyclerView.setEnableDrag(false) // 聊天消息不需要拖拽功能
        listRecyclerView.itemAnimator = null
    }

    override fun updateMessages(goal: AgentTaskGoal) {
        val aiInput = goal.input
        aiInput ?: run {
            // 更新列表数据
            updateChatList()
            return
        }
        messages.clear()
        messages.addAll(aiInput.messages)
        // 更新列表数据
        updateChatList()
    }

    override fun updateTaskStatus(taskId: String, status: ExecutionStatus) {

    }

    /**
     * 设置记忆压缩状态，压缩中时在列表末尾展示「压缩记忆中」占位
     */
    fun setCompressingMemory(compressing: Boolean) {
        if (isCompressingMemory == compressing) return
        isCompressingMemory = compressing
        updateChatList()
    }

    override fun getLayoutId() = R.layout.agent_chat_view

    override fun getChatView() = this

    /**
     * 是否处于列表底部（无法再向下滚动即视为在底部）
     * 覆盖不足一屏、满屏、超出底部等场景，符合常见聊天应用交互
     */
    private fun isAtBottom(): Boolean = !listRecyclerView.canScrollVertically(1)

    /**
     * 更新聊天列表
     * 仅在用户已在底部时自动平滑滚动到底部，用户上滑查看历史时不打扰
     */
    private fun updateChatList() {
        val previousHasVisibleMessages = displayedDataList.isNotEmpty()
        val shouldScrollToBottom = isAtBottom()

        val newDataList = buildDisplayData(messages)
        val oldDataList = displayedDataList

        val oldFp = oldDataList.map { toFingerprint(it) }
        val newFp = newDataList.map { toFingerprint(it) }

        // 计算第一个变化的位置（用于“只刷新最后一条/某一条”的场景）
        var firstDiffIndex = -1
        val minSize = minOf(oldFp.size, newFp.size)
        for (i in 0 until minSize) {
            if (oldFp[i] != newFp[i]) {
                firstDiffIndex = i
                break
            }
        }

        val isPureAppend = oldFp.size <= newFp.size && (firstDiffIndex == -1)
        val isSameSizeSingleTailChange =
            oldFp.size == newFp.size && (firstDiffIndex == -1 || firstDiffIndex == oldFp.lastIndex)

        displayedDataList = newDataList
        listRecyclerView.submitDataSetsWithType(newDataList.map { it.first to it.second })
        val hasVisibleMessages = newDataList.isNotEmpty()
        if (previousHasVisibleMessages != hasVisibleMessages) {
            onVisibleMessageStateChanged?.invoke(hasVisibleMessages)
        }

        when {
            // 无变化：不触发刷新，避免无意义重绘
            oldFp == newFp -> {
                if (shouldScrollToBottom && newDataList.isNotEmpty()) {
                    listRecyclerView.scrollToPosition(newDataList.lastIndex)
                }
                return
            }

            // 纯追加：只 notify 插入，避免全量闪烁
            isPureAppend -> {
                for (pos in oldFp.size until newFp.size) {
                    listRecyclerView.notifyItemInserted(pos)
                }
                if (shouldScrollToBottom && newDataList.isNotEmpty()) {
                    listRecyclerView.post { listRecyclerView.scrollToPosition(newDataList.lastIndex) }
                }
            }

            // 同大小，且仅尾部变化：只刷新最后一个 item（流式输出典型场景）
            isSameSizeSingleTailChange -> {
                val pos = if (firstDiffIndex == -1) oldFp.lastIndex else firstDiffIndex
                if (pos >= 0) {
                    listRecyclerView.notifyItemChanged(pos)
                } else {
                    listRecyclerView.notifyDataSetChanged()
                }
                if (shouldScrollToBottom && newDataList.isNotEmpty()) {
                    // 内容增长时用 scrollToPosition（无动画）更稳，不抖动
                    listRecyclerView.scrollToPosition(newDataList.lastIndex)
                }
            }

            // 兜底：结构性变化（切换会话、插入/删除中间消息等）
            else -> {
                listRecyclerView.notifyDataSetChanged()
                if (shouldScrollToBottom && newDataList.isNotEmpty()) {
                    listRecyclerView.post { listRecyclerView.scrollToPosition(newDataList.lastIndex) }
                }
            }
        }
    }

    private fun buildDisplayData(source: List<ChatMessage>): List<Pair<Int, Any?>> {
        val list = source.mapNotNull { message ->
            val viewType = when (message.role) {
                MessageRole.USER -> ChatMessageViewFactory.TYPE_USER_MESSAGE
                MessageRole.ASSISTANT -> ChatMessageViewFactory.TYPE_ASSISTANT_MESSAGE
                MessageRole.SYSTEM -> ChatMessageViewFactory.TYPE_SYSTEM_MESSAGE
                MessageRole.TOOL -> ChatMessageViewFactory.TYPE_TOOL_MESSAGE
            }
            if (viewType == ChatMessageViewFactory.TYPE_SYSTEM_MESSAGE) return@mapNotNull null
            viewType to message as Any?
        }.toMutableList()
        if (isCompressingMemory) {
            list.add(ChatMessageViewFactory.TYPE_COMPRESSING_MEMORY to null)
        }
        return list
    }

    private fun toFingerprint(pair: Pair<Int, Any?>): MessageFingerprint {
        return if (pair.first == ChatMessageViewFactory.TYPE_COMPRESSING_MEMORY) {
            MessageFingerprint(
                role = MessageRole.ASSISTANT,
                content = "___COMPRESSING___",
                reasoningContent = null,
                toolCallId = null,
                toolCallResult = null,
                toolCallsSize = 0,
                status = MessageStatus.WAITING,
                attachmentsSize = 0,
            )
        } else {
            (pair.second as ChatMessage).toFingerprint()
        }
    }

    private fun ChatMessage.toFingerprint(): MessageFingerprint {
        return MessageFingerprint(
            role = role,
            content = content,
            reasoningContent = reasoningContent,
            toolCallId = toolCallId,
            toolCallResult = toolCallResult,
            toolCallsSize = toolCalls?.size ?: 0,
            status = status,
            attachmentsSize = attachments.size,
        )
    }

    /**
     * 添加新消息
     */
    fun addMessage(message: ChatMessage) {
        messages.add(message)
        updateChatList()
    }

    /**
     * 清空消息列表
     */
    fun clearMessages() {
        messages.clear()
        updateChatList()
    }

    /**
     * 一次性设置全部消息（用于切换会话时恢复记录）
     */
    fun setMessages(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        updateChatList()
    }

    fun setOnVisibleMessageStateChangedListener(listener: ((Boolean) -> Unit)?) {
        onVisibleMessageStateChanged = listener
        listener?.invoke(displayedDataList.isNotEmpty())
    }

    fun hasVisibleMessages(): Boolean = displayedDataList.isNotEmpty()
}
