// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.hive.agent.R
import com.hive.agent.XAgent
import com.hive.agent.mcp.McpToolClient
import com.hive.plugin.agent.model.AttachmentType
import com.hive.plugin.agent.model.ChatAttachment
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.ToolCall
import com.hive.plugin.mcp.McpConst
import com.hive.utils.GlobalApp
import com.hive.utils.GlobalApp.getString
import com.hive.utils.debug.DLog
import com.hive.utils.extends.jsonToListView
import com.hive.utils.system.ClipboardUtil
import com.hive.utils.extends.gone
import com.hive.utils.extends.visible
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.widgets.CommonToast

/**
 * 工具调用消息ItemView
 * 显示工具调用的详细信息，包括方法名、参数和执行结果
 */
class ChatMessageItemToolView(context: Context) : ListRecyclerItemView(context) {

    private lateinit var tvName: TextView
    private lateinit var toolHeaderContainer: LinearLayout
    private lateinit var toolDetailContainer: LinearLayout
    private lateinit var toolTitleTextView: TextView
    private lateinit var toolStatusImageView: ImageView
    private lateinit var toolMethodTextView: TextView
    private lateinit var toolArgumentsTextView: TextView
    private lateinit var toolResultExpandIcon: ImageView
    private lateinit var toolResultTextView: TextView
    private lateinit var toolMessageImageView: ImageView

    // 使用静态 map 记录每个消息的展开状态，避免复用 card 时状态混乱
    companion object {
        private const val TAG = "ChatMessageItemToolView"
        private val expandedStates = mutableMapOf<Int, Boolean>()
    }
    
    private var currentPosition: Int = -1

    init {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.chat_message_tool_item, this)

        tvName = findViewById(R.id.tvName)
        toolHeaderContainer = findViewById(R.id.toolHeaderContainer)
        toolDetailContainer = findViewById(R.id.toolDetailContainer)
        toolTitleTextView = findViewById(R.id.toolTitleTextView)
        toolStatusImageView = findViewById(R.id.toolStatusImageView)
        toolMethodTextView = findViewById(R.id.toolMethodTextView)
        toolArgumentsTextView = findViewById(R.id.toolArgumentsTextView)
        toolResultExpandIcon = findViewById(R.id.toolResultExpandIcon)
        toolResultTextView = findViewById(R.id.toolResultTextView)
        toolMessageImageView = findViewById(R.id.toolMessageImageView)

        // 设置顶部点击事件：控制整个详情区域展开/折叠
        toolHeaderContainer.setOnClickListener {
            toggleResultExpansion()
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

        currentPosition = itemPosition
        
        // 设置时间显示
        tvName.text = getString(com.hive.i8n.R.string.agent_tool_call)

        bindToolAttachment(data)
        
        // 设置工具调用信息
        if (data.toolCalls != null && data.toolCalls!!.isNotEmpty()) {
            val toolCall = data.toolCalls!!.first()
            setupToolCallInfo(toolCall, data)
        } else {
            // 如果没有工具调用信息，显示基本信息
            toolTitleTextView.text = getString(com.hive.i8n.R.string.agent_tool_call)
            toolMethodTextView.text = getString(com.hive.i8n.R.string.agent_tool_unknown)
            toolArgumentsTextView.text = getString(com.hive.i8n.R.string.agent_tool_none)
        }

        // 设置执行状态
        updateToolStatus(data)
        
        // 根据消息ID恢复展开状态
        updateResultExpansionState()
    }

    private fun bindToolAttachment(message: ChatMessage) {
        val imageAttachment = message.attachments.firstOrNull {
            it.type == AttachmentType.IMAGE && (!it.base64.isNullOrEmpty() || !it.url.isNullOrEmpty())
        }
        if (imageAttachment == null) {
            toolMessageImageView.gone()
            if (message.attachments.isNotEmpty()) {
                DLog.i(
                    TAG,
                    "no displayable image attachment, attachments=${message.attachments.joinToString { it.describeForLog() }}"
                )
            }
            return
        }

        val displaySource = when {
            !imageAttachment.base64.isNullOrEmpty() -> imageAttachment.base64
            !imageAttachment.url.isNullOrEmpty() -> imageAttachment.url
            else -> null
        }
        if (displaySource.isNullOrEmpty()) {
            toolMessageImageView.gone()
            DLog.w(TAG, "image attachment has no usable source: ${imageAttachment.describeForLog()}")
            return
        }

        toolMessageImageView.visible()
        DLog.i(
            TAG,
            "load tool image: sourceScheme=${displaySource.toSchemeForLog()}, attachment=${imageAttachment.describeForLog()}, size=${toolMessageImageView.width}x${toolMessageImageView.height}"
        )
        Glide.with(context)
            .load(displaySource)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    val rootCauseSummary = e?.rootCauses?.joinToString {
                        "${it.javaClass.simpleName}:${it.message.orEmpty()}"
                    }
                    DLog.e(
                        TAG,
                        "tool image load failed: model=$model, scheme=${displaySource.toSchemeForLog()}, attachment=${imageAttachment.describeForLog()}, rootCauses=$rootCauseSummary, msg=${e?.message}",
                        e
                    )
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    DLog.i(
                        TAG,
                        "tool image load success: model=$model, dataSource=$dataSource, intrinsic=${resource.intrinsicWidth}x${resource.intrinsicHeight}"
                    )
                    return false
                }
            })
            .into(toolMessageImageView)
    }

    private fun ChatAttachment.describeForLog(): String {
        return "type=$type,mime=$mimeType,urlScheme=${url?.toSchemeForLog()},urlPreview=${url?.take(120)},hasBase64=${!base64.isNullOrEmpty()}"
    }

    private fun String.toSchemeForLog(): String {
        return substringBefore(':', missingDelimiterValue = "plain")
    }

    private fun setupToolCallInfo(toolCall: ToolCall, message: ChatMessage) {
        val toolDisplayName = resolveToolDisplayName(toolCall)
        toolTitleTextView.text = toolDisplayName

        // 设置方法名
        toolMethodTextView.text = toolCall.function.name
        
        // 设置参数
        val argumentsText = try {
            val args = toolCall.function.arguments
            args.entrySet().joinToString(", ") { (k, v) ->
                val str = v?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.getAsString() ?: v?.toString() ?: ""
                "$k=$str"
            }
        } catch (_: Exception) {
            "${toolCall.function.arguments}"
        }
        toolArgumentsTextView.text = argumentsText

        // 设置执行结果
        message.toolCallResult?.let { result ->
            toolResultTextView.text = result.jsonToListView()
        }
    }

    private fun resolveToolDisplayName(toolCall: ToolCall): String {
        val fallbackName = toolCall.function.name
        val displayName = resolveSkillDisplayName(toolCall) ?: XAgent.getInstance()
            .getRegisteredTools()
            .asSequence()
            .filterIsInstance<McpToolClient>()
            .mapNotNull { it.resolveDisplayName(fallbackName) }
            .firstOrNull()
            ?: fallbackName
        return displayName.limitToTitleLength()
    }

    private fun resolveSkillDisplayName(toolCall: ToolCall): String? {
        return when (toolCall.function.name) {
            McpConst.Tool_Name_Prefix_BuildIn + "skill" -> resolveUnifiedSkillActionName(toolCall)
            else -> null
        }
    }

    private fun resolveUnifiedSkillActionName(toolCall: ToolCall): String {
        val action = toolCall.function.arguments
            .get("action")
            ?.takeIf { it.isJsonPrimitive }
            ?.asJsonPrimitive
            ?.asString
            ?.trim()
            ?.lowercase()
            .orEmpty()
        return when (action) {
            "help" -> getString(com.hive.i8n.R.string.agent_skill_action_help)
            "list" -> getString(com.hive.i8n.R.string.agent_skill_action_list)
            "run" -> getString(com.hive.i8n.R.string.agent_skill_action_run)
            "create" -> getString(com.hive.i8n.R.string.agent_skill_action_create)
            "update" -> getString(com.hive.i8n.R.string.agent_skill_action_update)
            "delete" -> getString(com.hive.i8n.R.string.agent_skill_action_delete)
            else -> getString(com.hive.i8n.R.string.agent_skill_action_default)
        }
    }

    private fun String.limitToTitleLength(maxLength: Int = 20): String {
        return if (maxLength !in 4..<length) {
            take(maxLength)
        } else {
            take(maxLength - 3) + "..."
        }
    }

    private fun updateToolStatus(message: ChatMessage) {
        when {
            message.toolCallResult != null -> {
                // 有结果，判断成功或失败
                val isSuccess = message.toolCallResultSuccess
                if (isSuccess) {
                    toolStatusImageView.setImageResource(R.drawable.ic_status_check)
                } else {
                    toolStatusImageView.setImageResource(R.drawable.ic_status_close)
                }
            }
            else -> {
                // 没有结果，显示执行中
                toolStatusImageView.setImageResource(R.drawable.ic_status_running)
            }
        }
    }

    private fun toggleResultExpansion() {
        currentPosition.let { messageId ->
            val currentState = expandedStates[messageId] ?: false
            val newState = !currentState
            expandedStates[messageId] = newState

            updateResultExpansionState()
        }
    }
    
    private fun updateResultExpansionState() {
        val isExpanded = currentPosition.let { expandedStates[it] } ?: false

        if (isExpanded) {
            toolDetailContainer.visibility = View.VISIBLE
            toolResultExpandIcon.rotation = 180f
        } else {
            toolDetailContainer.visibility = View.GONE
            toolResultExpandIcon.rotation = 0f
        }
    }
} 
