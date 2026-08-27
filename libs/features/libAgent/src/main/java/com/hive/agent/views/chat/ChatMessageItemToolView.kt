// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.hive.agent.R
import com.hive.plugin.agent.model.ChatMessage
import com.hive.utils.GlobalApp
import com.hive.utils.system.ClipboardUtil
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.widgets.CommonToast

/**
 * 工具调用 chip：仅展示工具名与状态，点击打开详情 BottomSheet。
 */
class ChatMessageItemToolView(context: Context) : ListRecyclerItemView(context) {

    companion object {
        const val EVENT_TOOL_DETAIL = "tool_detail"
    }

    private lateinit var toolHeaderContainer: LinearLayout
    private lateinit var toolTitleTextView: TextView
    private lateinit var toolStatusImageView: ImageView

    init {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.chat_message_tool_item, this)

        toolHeaderContainer = findViewById(R.id.toolHeaderContainer)
        toolTitleTextView = findViewById(R.id.toolTitleTextView)
        toolStatusImageView = findViewById(R.id.toolStatusImageView)

        toolHeaderContainer.setOnClickListener {
            postEvent(EVENT_TOOL_DETAIL)
        }
        setOnLongClickListener {
            (itemData as? ChatMessage)?.let { msg ->
                val parts = mutableListOf<String>()
                msg.content?.trim()?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
                msg.toolCallResult?.trim()?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
                val text = parts.joinToString("\n\n")
                if (text.isNotEmpty()) {
                    ClipboardUtil.getInstance(context).copyText("agent_message", text)
                    CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.agent_message_copied))
                }
            }
            true
        }
    }

    override fun bindData(data: Any?) {
        if (data !is ChatMessage) return
        val toolCall = data.toolCalls?.firstOrNull()
        toolTitleTextView.text = if (toolCall != null) {
            AgentToolDisplayNames.resolve(toolCall)
        } else {
            GlobalApp.getString(com.hive.i8n.R.string.agent_tool_call)
        }
        updateToolStatus(data)
    }

    private fun updateToolStatus(message: ChatMessage) {
        when {
            message.toolCallResult != null -> {
                if (message.toolCallResultSuccess) {
                    toolStatusImageView.setImageResource(R.drawable.ic_status_check)
                } else {
                    toolStatusImageView.setImageResource(R.drawable.ic_status_close)
                }
            }
            else -> toolStatusImageView.setImageResource(R.drawable.ic_status_running)
        }
    }
}
