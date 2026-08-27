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
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.ListRecyclerView

/**
 * Agent聊天视图
 */
class AgentChatView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs),
    IAgentChatView {

    private lateinit var listRecyclerView: ListRecyclerView
    private lateinit var chatMessageViewFactory: ChatMessageViewFactory

    private var messages = mutableListOf<ChatMessage>()
    private var displayedDataList: List<Pair<Int, Any?>> = emptyList()
    private var isCompressingMemory = false
    private var onVisibleMessageStateChanged: ((Boolean) -> Unit)? = null
    private var onToolMessageClick: ((ChatMessage) -> Unit)? = null

    private data class MessageFingerprint(
        val role: MessageRole,
        val content: String?,
        val reasoningContent: String?,
        val toolCallId: String?,
        val toolCallResult: String?,
        val toolCallsSize: Int,
        val status: MessageStatus,
        val attachmentsSize: Int,
        val placeholder: String? = null,
    )

    override fun initView(view: View?) {
        listRecyclerView = findViewById(R.id.listRecyclerView)

        chatMessageViewFactory = ChatMessageViewFactory(context)

        listRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listRecyclerView.setItemViewFactory(chatMessageViewFactory)
        listRecyclerView.setEnableDrag(false)
        listRecyclerView.itemAnimator = null
        listRecyclerView.setOnItemEventListener(object : ListRecyclerItemView.OnItemEventListener {
            override fun onItemEvent(itemData: Any?, eventData: Any?) {
                if (eventData == ChatMessageItemToolView.EVENT_TOOL_DETAIL && itemData is ChatMessage) {
                    onToolMessageClick?.invoke(itemData)
                }
            }
        })
    }

    override fun updateMessages(goal: AgentTaskGoal) {
        val aiInput = goal.input
        aiInput ?: run {
            updateChatList()
            return
        }
        messages.clear()
        messages.addAll(aiInput.messages)
        updateChatList()
    }

    override fun updateTaskStatus(taskId: String, status: ExecutionStatus) {
    }

    fun setCompressingMemory(compressing: Boolean) {
        if (isCompressingMemory == compressing) return
        isCompressingMemory = compressing
        updateChatList()
    }

    fun setOnToolMessageClickListener(listener: ((ChatMessage) -> Unit)?) {
        onToolMessageClick = listener
    }

    override fun getLayoutId() = R.layout.agent_chat_view

    override fun getChatView() = this

    private fun isAtBottom(): Boolean = !listRecyclerView.canScrollVertically(1)

    private fun updateChatList() {
        val previousHasVisibleMessages = displayedDataList.isNotEmpty()
        val shouldScrollToBottom = isAtBottom()

        val newDataList = AgentChatDisplayHelper.buildDisplayData(messages, isCompressingMemory)
        val oldDataList = displayedDataList

        val oldFp = oldDataList.map { toFingerprint(it) }
        val newFp = newDataList.map { toFingerprint(it) }

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
            oldFp == newFp -> {
                if (shouldScrollToBottom && newDataList.isNotEmpty()) {
                    listRecyclerView.scrollToPosition(newDataList.lastIndex)
                }
                return
            }

            isPureAppend -> {
                for (pos in oldFp.size until newFp.size) {
                    listRecyclerView.notifyItemInserted(pos)
                }
                if (shouldScrollToBottom && newDataList.isNotEmpty()) {
                    listRecyclerView.post { listRecyclerView.scrollToPosition(newDataList.lastIndex) }
                }
            }

            isSameSizeSingleTailChange -> {
                val pos = if (firstDiffIndex == -1) oldFp.lastIndex else firstDiffIndex
                if (pos >= 0) {
                    listRecyclerView.notifyItemChanged(pos)
                } else {
                    listRecyclerView.notifyDataSetChanged()
                }
                if (shouldScrollToBottom && newDataList.isNotEmpty()) {
                    listRecyclerView.post { listRecyclerView.scrollToPosition(newDataList.lastIndex) }
                }
            }

            else -> {
                listRecyclerView.notifyDataSetChanged()
                if (shouldScrollToBottom && newDataList.isNotEmpty()) {
                    listRecyclerView.post { listRecyclerView.scrollToPosition(newDataList.lastIndex) }
                }
            }
        }
    }

    private fun toFingerprint(pair: Pair<Int, Any?>): MessageFingerprint {
        return when (pair.first) {
            ChatMessageViewFactory.TYPE_COMPRESSING_MEMORY -> MessageFingerprint(
                role = MessageRole.ASSISTANT,
                content = null,
                reasoningContent = null,
                toolCallId = null,
                toolCallResult = null,
                toolCallsSize = 0,
                status = MessageStatus.WAITING,
                attachmentsSize = 0,
                placeholder = "compressing",
            )
            ChatMessageViewFactory.TYPE_THINKING_LOADING -> MessageFingerprint(
                role = MessageRole.ASSISTANT,
                content = null,
                reasoningContent = null,
                toolCallId = null,
                toolCallResult = null,
                toolCallsSize = 0,
                status = MessageStatus.WAITING,
                attachmentsSize = 0,
                placeholder = "thinking",
            )
            else -> (pair.second as ChatMessage).toFingerprint()
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

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        updateChatList()
    }

    fun clearMessages() {
        messages.clear()
        updateChatList()
    }

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
