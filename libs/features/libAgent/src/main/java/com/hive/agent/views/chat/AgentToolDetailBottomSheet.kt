// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hive.agent.R
import com.hive.plugin.agent.model.AttachmentType
import com.hive.plugin.agent.model.ChatMessage
import com.hive.utils.GlobalApp
import com.hive.utils.extends.gone
import com.hive.utils.extends.jsonToListView
import com.hive.utils.extends.visible

/**
 * 工具调用详情 BottomSheet（视觉对齐会话列表 sheet）。
 */
class AgentToolDetailBottomSheet : BottomSheetDialogFragment() {

    var message: ChatMessage? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_agent_tool_detail, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val msg = message ?: run {
            dismissAllowingStateLoss()
            return
        }
        bindMessage(view, msg)
    }

    private fun bindMessage(view: View, msg: ChatMessage) {
        val tvTitle = view.findViewById<TextView>(R.id.tvToolTitle)
        val ivStatus = view.findViewById<ImageView>(R.id.ivToolStatus)
        val tvMethod = view.findViewById<TextView>(R.id.tvToolMethod)
        val tvArguments = view.findViewById<TextView>(R.id.tvToolArguments)
        val tvResult = view.findViewById<TextView>(R.id.tvToolResult)
        val ivImage = view.findViewById<ImageView>(R.id.ivToolImage)

        val toolCall = msg.toolCalls?.firstOrNull()
        tvTitle.text = if (toolCall != null) {
            AgentToolDisplayNames.resolve(toolCall)
        } else {
            GlobalApp.getString(com.hive.i8n.R.string.agent_tool_call)
        }

        when {
            msg.toolCallResult != null -> {
                ivStatus.setImageResource(
                    if (msg.toolCallResultSuccess) R.drawable.ic_status_check
                    else R.drawable.ic_status_close
                )
            }
            else -> ivStatus.setImageResource(R.drawable.ic_status_running)
        }

        if (toolCall != null) {
            tvMethod.text = toolCall.function.name
            tvArguments.text = AgentToolDisplayNames.formatArguments(toolCall)
                .ifEmpty { GlobalApp.getString(com.hive.i8n.R.string.agent_tool_none) }
        } else {
            tvMethod.text = GlobalApp.getString(com.hive.i8n.R.string.agent_tool_unknown)
            tvArguments.text = GlobalApp.getString(com.hive.i8n.R.string.agent_tool_none)
        }

        val result = msg.toolCallResult
        tvResult.text = if (result.isNullOrEmpty()) {
            GlobalApp.getString(com.hive.i8n.R.string.agent_tool_executing)
        } else {
            result.jsonToListView()
        }

        bindImage(ivImage, msg)
    }

    private fun bindImage(imageView: ImageView, message: ChatMessage) {
        val imageAttachment = message.attachments.firstOrNull {
            it.type == AttachmentType.IMAGE && (!it.base64.isNullOrEmpty() || !it.url.isNullOrEmpty())
        }
        val displaySource = when {
            imageAttachment == null -> null
            !imageAttachment.base64.isNullOrEmpty() -> imageAttachment.base64
            !imageAttachment.url.isNullOrEmpty() -> imageAttachment.url
            else -> null
        }
        if (displaySource.isNullOrEmpty()) {
            imageView.gone()
            return
        }
        imageView.visible()
        Glide.with(this)
            .load(displaySource)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean = false

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean = false
            })
            .into(imageView)
    }

    companion object {
        private const val TAG = "AgentToolDetailBottomSheet"

        fun show(fragmentManager: FragmentManager, message: ChatMessage) {
            AgentToolDetailBottomSheet().apply {
                this.message = message
            }.show(fragmentManager, TAG)
        }
    }
}
