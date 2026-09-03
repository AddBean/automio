// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hive.agent.R
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.provider.IEditorProvider
import com.hive.utils.GlobalApp
import com.hive.utils.extends.gone
import com.hive.utils.extends.jsonToListView
import com.hive.utils.extends.visible

/**
 * 工具调用详情 BottomSheet（视觉对齐会话列表 sheet，支持附件图片预览）。
 */
class AgentToolDetailBottomSheet : BottomSheetDialogFragment() {

    var message: ChatMessage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AgentBottomSheetDialog)
    }

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
        dialog?.window?.navigationBarColor =
            requireContext().getColor(com.hive.i8n.R.color.design_bg_overlay)
        setupSheetBehavior()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val msg = message ?: run {
            dismissAllowingStateLoss()
            return
        }
        bindMessage(view, msg)
        // 等内容（含 markdown）完成布局后再限制高度，避免与 sheet 下滑关闭抢手势
        scheduleLimitScrollHeight(view)
    }

    private fun setupSheetBehavior() {
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        val behavior = (dialog as? BottomSheetDialog)?.behavior
            ?: BottomSheetBehavior.from(bottomSheet)
        behavior.skipCollapsed = true
        behavior.isFitToContents = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun scheduleLimitScrollHeight(root: View) {
        val scroll = root.findViewById<NestedScrollView>(R.id.scrollToolDetail) ?: return
        scroll.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (!scroll.viewTreeObserver.isAlive) return
                scroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                limitScrollHeight(root)
            }
        })
    }

    private fun limitScrollHeight(root: View) {
        val scroll = root.findViewById<NestedScrollView>(R.id.scrollToolDetail) ?: return
        val content = scroll.getChildAt(0) ?: return
        val maxHeight = (resources.displayMetrics.heightPixels * SCROLL_MAX_HEIGHT_RATIO).toInt()
        val contentHeight = content.measuredHeight.takeIf { it > 0 } ?: content.height
        if (contentHeight <= maxHeight) return
        val lp = scroll.layoutParams
        if (lp.height != maxHeight) {
            lp.height = maxHeight
            scroll.layoutParams = lp
        }
    }

    private fun bindMessage(view: View, msg: ChatMessage) {
        val tvTitle = view.findViewById<TextView>(R.id.tvToolTitle)
        val tvMethod = view.findViewById<TextView>(R.id.tvToolMethod)
        val ivStatus = view.findViewById<ImageView>(R.id.ivToolStatus)
        val tvStatus = view.findViewById<TextView>(R.id.tvToolStatus)
        val tvArguments = view.findViewById<TextView>(R.id.tvToolArguments)
        val tvToolExecAt = view.findViewById<TextView>(R.id.tvToolExecAt)
        val tvResult = view.findViewById<TextView>(R.id.tvToolResult)
        val layoutImagesSection = view.findViewById<LinearLayout>(R.id.layoutImagesSection)
        val layoutToolImages = view.findViewById<LinearLayout>(R.id.layoutToolImages)

        val toolCall = msg.toolCalls?.firstOrNull()
        tvTitle.text = if (toolCall != null) {
            AgentToolDisplayNames.resolve(toolCall)
        } else {
            GlobalApp.getString(com.hive.i8n.R.string.agent_tool_call)
        }

        if (toolCall != null) {
            tvMethod.text = toolCall.function.name
            tvArguments.text = AgentToolDisplayNames.formatArguments(toolCall)
                .ifEmpty { GlobalApp.getString(com.hive.i8n.R.string.agent_tool_none) }
        } else {
            tvMethod.text = GlobalApp.getString(com.hive.i8n.R.string.agent_tool_unknown)
            tvArguments.text = GlobalApp.getString(com.hive.i8n.R.string.agent_tool_none)
        }

        when {
            msg.toolCallResult != null -> {
                if (msg.toolCallResultSuccess) {
                    ivStatus.setImageResource(R.drawable.ic_status_check)
                    tvStatus.text = GlobalApp.getString(com.hive.i8n.R.string.agent_tool_execute_success)
                } else {
                    ivStatus.setImageResource(R.drawable.ic_status_close)
                    tvStatus.text = GlobalApp.getString(com.hive.i8n.R.string.agent_tool_execute_failed)
                }
            }
            else -> {
                ivStatus.setImageResource(R.drawable.ic_status_running)
                tvStatus.text = GlobalApp.getString(com.hive.i8n.R.string.agent_tool_executing_status)
            }
        }

        val execAt = resolveExecAt(msg)
        if (execAt.isNullOrBlank()) {
            tvToolExecAt.gone()
        } else {
            tvToolExecAt.visible()
            tvToolExecAt.text = execAt
        }

        val result = msg.toolCallResult
        val resultText = if (result.isNullOrEmpty()) {
            GlobalApp.getString(com.hive.i8n.R.string.agent_tool_executing)
        } else {
            stripExecAtLine(result.jsonToListView())
        }
        renderResultMarkdown(tvResult, resultText)

        bindImages(layoutImagesSection, layoutToolImages, msg)
    }

    private fun resolveExecAt(msg: ChatMessage): String? {
        msg.execAt?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val result = msg.toolCallResult ?: return null
        val line = result.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("exec_at=") }
        return line?.substringAfter("exec_at=")?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun stripExecAtLine(text: String): String {
        return text.lineSequence()
            .filterNot { it.trim().startsWith("exec_at=") }
            .joinToString("\n")
            .trim()
    }

    private fun renderResultMarkdown(textView: TextView, markdown: String) {
        val editorProvider = ComponentManager.getInstance()
            .getProvider(IEditorProvider::class.java) as? IEditorProvider
        if (editorProvider != null) {
            editorProvider.renderMarkdown(textView, markdown)
        } else {
            textView.text = markdown
        }
    }

    private fun bindImages(
        section: LinearLayout,
        container: LinearLayout,
        message: ChatMessage
    ) {
        container.removeAllViews()
        val images = AgentAttachmentImageLoader.imageAttachments(message.attachments)
        if (images.isEmpty()) {
            section.gone()
            return
        }
        section.visible()
        val inflater = layoutInflater
        val ctx = requireContext()
        images.forEach { attachment ->
            val item = inflater.inflate(R.layout.item_agent_tool_attachment_image, container, false)
            val imageView = item.findViewById<ImageView>(R.id.ivToolAttachment)
            AgentAttachmentImageLoader.loadInto(ctx, imageView, attachment)
            container.addView(item)
        }
    }

    companion object {
        private const val TAG = "AgentToolDetailBottomSheet"
        private const val SCROLL_MAX_HEIGHT_RATIO = 0.72f

        fun show(fragmentManager: FragmentManager, message: ChatMessage) {
            AgentToolDetailBottomSheet().apply {
                this.message = message
            }.show(fragmentManager, TAG)
        }
    }
}
