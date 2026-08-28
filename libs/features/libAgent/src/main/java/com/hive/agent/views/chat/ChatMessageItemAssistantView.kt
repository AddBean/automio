// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.hive.agent.R
import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IEditorProvider
import com.hive.extension.visibleOrGone
import com.hive.net.image.ImageLoader
import com.hive.plugin.agent.model.AttachmentType
import com.hive.plugin.agent.model.ChatMessage
import com.hive.utils.GlobalApp
import com.hive.utils.system.ClipboardUtil
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.widgets.CommonToast
import com.wang.avi.AVLoadingIndicatorView

/**
 * AI 助手消息 ItemView。
 * 思考态 loading 由列表尾占位统一展示，气泡本身不再高亮 / 挂 statusIndicator。
 */
class ChatMessageItemAssistantView(context: Context) : ListRecyclerItemView(context) {

    private lateinit var tvMessage: TextView
    private lateinit var messageInfoLayout: LinearLayout
    private lateinit var messageImageView: ImageView
    private lateinit var reasoningLayout: LinearLayout
    private lateinit var reasoningTextView: TextView
    private lateinit var statusIndicator: AVLoadingIndicatorView

    private var lastRenderedMarkdown: String? = null

    init {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.chat_message_assistant_enhanced_item, this)

        tvMessage = findViewById(R.id.tvMessage)
        messageInfoLayout = findViewById(R.id.messageInfoLayout)
        messageImageView = findViewById(R.id.messageImageView)
        reasoningLayout = findViewById(R.id.reasoningLayout)
        reasoningTextView = findViewById(R.id.reasoningTextView)
        statusIndicator = findViewById(R.id.statusIndicator)
        setOnLongClickListener {
            (itemData as? ChatMessage)?.let { msg ->
                val parts = mutableListOf<String>()
                msg.content?.trim()?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
                msg.reasoningContent?.trim()?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
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
        data.let { message ->
            val imageAttachment =
                message.attachments.firstOrNull { it.type == AttachmentType.IMAGE && !it.url.isNullOrEmpty() }
            val raw = message.content?.trim()
            val hasContent = !raw.isNullOrEmpty()
            val hasThinking = !message.reasoningContent.isNullOrEmpty()
            val hasImage = imageAttachment != null
            this.visibleOrGone(hasContent || hasThinking || hasImage)
            tvMessage.visibleOrGone(hasContent)
            reasoningLayout.visibleOrGone(hasThinking)
            messageImageView.visibleOrGone(hasImage)
            if (hasThinking) {
                reasoningTextView.text = message.reasoningContent
            }
            if (hasContent) {
                if (lastRenderedMarkdown != raw) {
                    lastRenderedMarkdown = raw
                    val editorProvider = ComponentManager.getInstance()
                        .getProvider(IEditorProvider::class.java) as? IEditorProvider
                    if (editorProvider != null) {
                        editorProvider.renderMarkdown(tvMessage, raw)
                    } else {
                        tvMessage.text = raw
                    }
                }
                tvMessage.setTextIsSelectable(false)
            } else {
                lastRenderedMarkdown = null
                tvMessage.text = ""
            }
            if (imageAttachment != null) {
                ImageLoader.getInstance().loadImage(context, messageImageView, imageAttachment.url)
            }
            statusIndicator.visibleOrGone(false)
        }
    }
}
