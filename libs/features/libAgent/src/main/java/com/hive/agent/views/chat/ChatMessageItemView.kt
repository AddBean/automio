// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.hive.agent.R
import com.hive.utils.GlobalApp
import com.hive.agent.utils.MessageStatusHelper
import com.hive.net.image.ImageLoader
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.MessageStatus
import com.hive.utils.GlobalApp.getColor
import com.hive.utils.GlobalApp.getString
import com.hive.utils.system.ClipboardUtil
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.widgets.CommonToast

/**
 * 聊天消息ItemView
 * 根据消息角色和状态显示不同的样式
 */
class ChatMessageItemView(context: Context) : ListRecyclerItemView(context) {

    private lateinit var messageTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var statusIndicator: ImageView
    private lateinit var statusTextView: TextView
    private lateinit var statusLayout: View
    private lateinit var messageImageView: ImageView

    init {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.chat_message_item, this)
        messageTextView = findViewById(R.id.messageTextView)
        timeTextView = findViewById(R.id.tvName)
        statusLayout = findViewById(R.id.statusLayout)
        statusIndicator = findViewById(R.id.statusIndicator)
        statusTextView = findViewById(R.id.statusTextView)
        messageImageView = findViewById(R.id.messageImageView)
        val bubbleMaxWidth = (resources.displayMetrics.widthPixels * 0.85f).toInt()
        messageTextView.maxWidth = bubbleMaxWidth
        val onLongCopy = View.OnLongClickListener {
            (itemData as? ChatMessage)?.let { msg ->
                val text = msg.content?.trim()
                if (!text.isNullOrEmpty()) {
                    ClipboardUtil.getInstance(context).copyText("agent_message", text)
                    CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.agent_message_copied))
                }
            }
            true
        }
        setOnLongClickListener(onLongCopy)
        messageTextView.setOnLongClickListener(onLongCopy)
    }

    override fun bindData(data: Any?) {
        if (data !is ChatMessage) return

        val message = data

        // 设置消息内容
        messageTextView.text = (message.content ?: "-").trim()

        // 设置图片（如有）：从 attachments 中的 IMAGE 类型读取
        val imageAttachment = message.attachments.firstOrNull { !it.url.isNullOrEmpty() }
        if (imageAttachment != null) {
            messageImageView.visibility = View.VISIBLE
            ImageLoader.getInstance().loadImage(context, messageImageView, imageAttachment.url)
        } else {
            messageImageView.visibility = View.GONE
        }

        // 根据消息角色设置不同的样式
        setupMessageRole(message.role)

        // 根据消息状态设置不同的样式
        setupMessageStatus(message.status)
    }

    private fun setupMessageRole(role: MessageRole) {
        val msgLp = messageTextView.layoutParams as LinearLayout.LayoutParams
        val imgLp = messageImageView.layoutParams as LinearLayout.LayoutParams
        val timeLp = timeTextView.layoutParams as LinearLayout.LayoutParams
        when (role) {
            MessageRole.USER -> {
                messageTextView.setBackgroundResource(R.drawable.chat_message_user_bg)
                messageTextView.setTextColor(getColor(com.hive.i8n.R.color.white))
                timeTextView.text = getString(com.hive.i8n.R.string.agent_message_user)
                messageTextView.maxLines = 100
                msgLp.gravity = Gravity.END
                imgLp.gravity = Gravity.END
                timeLp.gravity = Gravity.END
            }


            MessageRole.SYSTEM -> {
                messageTextView.setBackgroundResource(R.drawable.chat_message_system_bg)
                messageTextView.setTextColor(getColor(com.hive.i8n.R.color.system_message_text))
                messageTextView.maxLines = 20
                timeTextView.text = getString(com.hive.i8n.R.string.agent_message_system)
                msgLp.gravity = Gravity.CENTER_HORIZONTAL
                imgLp.gravity = Gravity.CENTER_HORIZONTAL
                timeLp.gravity = Gravity.CENTER_HORIZONTAL
            }

            else -> {
                // 背景样式会根据状态在setupMessageStatus中设置
                messageTextView.setTextColor(getColor(com.hive.i8n.R.color.design_text_slate_300))
                timeTextView.text = getString(com.hive.i8n.R.string.agent_message_ai)
                msgLp.gravity = Gravity.START
                imgLp.gravity = Gravity.START
                timeLp.gravity = Gravity.START
            }

        }
        messageTextView.layoutParams = msgLp
        messageImageView.layoutParams = imgLp
        timeTextView.layoutParams = timeLp
    }

    private fun setupMessageStatus(status: MessageStatus) {
        // 只有ASSISTANT角色的消息才根据状态设置背景
        if (messageTextView.background == null) {
            messageTextView.setBackgroundResource(MessageStatusHelper.getBackgroundResource(status))
        }

        // 设置状态指示器
        if (MessageStatusHelper.shouldShowStatusIndicator(status)) {
            statusTextView.text = MessageStatusHelper.getStatusText(status)
            statusLayout.visibility = View.VISIBLE
            val iconResource = MessageStatusHelper.getStatusIconResource(status)
            if (iconResource != 0) {
                statusIndicator.setImageResource(iconResource)
            }
        } else {
            statusLayout.visibility = View.GONE
        }
    }
} 
