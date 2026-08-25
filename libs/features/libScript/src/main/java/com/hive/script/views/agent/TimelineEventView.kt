// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.agent

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.MessageStatus
import com.hive.script.R
import com.hive.utils.system.ClipboardUtil
import com.hive.views.widgets.CommonToast
import com.hive.utils.GlobalApp
import com.hive.utils.extends.dp
import com.hive.utils.utils.StringUtils
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory

data class TimelineEventData(
    val event: String,
    val time: String,
    val detail: String,
    val rawText: String? = null, // md 原文本，用于长按复制
    val isCompleted: Boolean = false,
    val isCurrent: Boolean = false,
    val isThinking: Boolean = false,
    val isExecuting: Boolean = false,
    val isToolSuccess: Boolean? = null,
    val index: Int = 0,
    val isFirst: Boolean = false,
    val isLast: Boolean = false,
    val totalCount: Int = 0
)

class TimelineEventView(context: Context) : ListRecyclerItemView(context) {

    private val view = LayoutInflater.from(context).inflate(R.layout.item_timeline_event, this)
    private lateinit var tvEventName: TextView
    private lateinit var vEventDot: View
    private lateinit var tvEventTime: TextView
    private lateinit var tvEventDetail: TextView

    private val statusManager = StatusManager()

    // 连接线绘制相关
    private val linePaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary)
        strokeWidth = 1.dp().toFloat()
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private var currentData: TimelineEventData? = null

    init {
        initView()
    }

    private fun initView() {
        tvEventName = view.findViewById(R.id.tv_event_name)
        vEventDot = view.findViewById(R.id.v_event_dot)
        tvEventTime = view.findViewById(R.id.tv_event_time)
        tvEventDetail = view.findViewById(R.id.tv_event_detail)
        setOnLongClickListener {
            (itemData as? TimelineEventData)?.rawText?.takeIf { it.isNotEmpty() }?.let { text ->
                ClipboardUtil.getInstance(context).copyText("agent_timeline", text)
                CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.agent_message_copied))
            }
            true
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        drawConnectionLines(canvas)
        super.dispatchDraw(canvas)
    }

    private fun drawConnectionLines(canvas: Canvas) {
        currentData?.let { data ->
            // 确保视图已经布局完成
            if (vEventDot.width == 0 || vEventDot.height == 0) {
                return
            }

            val dotCenterX = vEventDot.x + vEventDot.width / 2f
            val dotCenterY = vEventDot.y + vEventDot.height / 2f
            val lineLength = measuredWidth/2

            // 根据状态设置连接线颜色
            val lineColor = GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary)
            linePaint.color = lineColor

            // 绘制连接线
            when {
                data.isFirst -> {
                    // 第一个项目：只画右边横线
                    canvas.drawLine(
                        dotCenterX,
                        dotCenterY,
                        dotCenterX + lineLength,
                        dotCenterY,
                        linePaint
                    )
                }

                data.isLast -> {
                    // 最后一个项目：只画左边横线
                    canvas.drawLine(
                        dotCenterX - lineLength,
                        dotCenterY,
                        dotCenterX,
                        dotCenterY,
                        linePaint
                    )
                }

                else -> {
                    // 中间项目：画左右两条横线
                    canvas.drawLine(
                        dotCenterX - lineLength,
                        dotCenterY,
                        dotCenterX,
                        dotCenterY,
                        linePaint
                    )
                    canvas.drawLine(
                        dotCenterX,
                        dotCenterY,
                        dotCenterX + lineLength,
                        dotCenterY,
                        linePaint
                    )
                }
            }
        }
    }

    override fun bindData(data: Any?) {
        if (data is TimelineEventData) {
            currentData = data
            itemData = data
            statusManager.updateStatus(data)
            // 强制重绘以显示连接线
            post { invalidate() }
        }
    }

    // 状态管理器
    private inner class StatusManager {
        fun updateStatus(data: TimelineEventData) {
            updateEventName(data)
            updateEventTime(data)
            updateEventDetail(data)
            updateEventDot(data)
        }

        private fun updateEventName(data: TimelineEventData) {
            tvEventName.text = data.event
            // 设置事件名称颜色
            tvEventName.setTextColor(
                when {
                    data.isToolSuccess == false -> GlobalApp.getColor(com.hive.i8n.R.color.colorRed)
                    data.isThinking -> GlobalApp.getColor(com.hive.i8n.R.color.color_orange)
                    data.isExecuting -> GlobalApp.getColor(com.hive.i8n.R.color.colorPurple)
                    data.isCurrent -> GlobalApp.getColor(com.hive.i8n.R.color.colorAccent)
                    data.isCompleted -> GlobalApp.getColor(com.hive.i8n.R.color.colorTextGreen)
                    else -> GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary)
                }
            )
        }

        private fun updateEventTime(data: TimelineEventData) {
            tvEventTime.text = data.time
        }

        private fun updateEventDetail(data: TimelineEventData) {
            val detailText = when {
                data.isToolSuccess == true -> "✓ Success"
                data.isToolSuccess == false -> "✗ Failed"
                data.isThinking -> "Thinking..."
                data.isExecuting -> "Executing..."
                data.isCompleted -> "Completed"
                else -> data.detail
            }
            tvEventDetail.text = detailText

            // 设置详情文本颜色
            tvEventDetail.setTextColor(
                when {
                    data.isToolSuccess == false -> GlobalApp.getColor(com.hive.i8n.R.color.colorRed)
                    data.isThinking -> GlobalApp.getColor(com.hive.i8n.R.color.color_orange)
                    data.isExecuting -> GlobalApp.getColor(com.hive.i8n.R.color.colorPurple)
                    data.isCompleted -> GlobalApp.getColor(com.hive.i8n.R.color.colorTextGreen)
                    else -> GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary)
                }
            )
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        private fun updateEventDot(data: TimelineEventData) {
            // 设置圆点样式
            vEventDot.background = context.getDrawable(
                when {
                    data.isToolSuccess == false -> R.drawable.xml_timeline_dot_failed
                    data.isThinking -> R.drawable.xml_timeline_dot_thinking
                    data.isExecuting -> R.drawable.xml_timeline_dot_executing
                    data.isCurrent -> R.drawable.xml_timeline_dot_current
                    data.isCompleted -> R.drawable.xml_timeline_dot_completed
                    else -> R.drawable.xml_timeline_dot_waiting
                }
            )
        }
    }

    companion object {
        fun createFromMessage(
            message: ChatMessage,
            index: Int,
            totalCount: Int = 0
        ): TimelineEventData {
            val event = when (message.role) {
                MessageRole.USER -> "User"
                MessageRole.ASSISTANT -> "Thinking"
                MessageRole.TOOL -> (message.toolCalls?.firstOrNull()?.function?.name)
                        ?.let { if (it.contains(".")) it.substringAfter(".") else it }
                        ?.uppercase() ?: "Tool"

                MessageRole.SYSTEM -> "System"
            }

            val detail = when (message.status) {
                MessageStatus.WAITING -> "Waiting..."
                MessageStatus.TOOL_RUNNING -> "Executing..."
                MessageStatus.FINISH -> "Completed"
            }

            val time = formatTime(message.timestamp)

            val isCompleted = message.status == MessageStatus.FINISH
            val isCurrent = message.status == MessageStatus.TOOL_RUNNING
            val isThinking =
                message.role == MessageRole.ASSISTANT && message.status == MessageStatus.WAITING
            val isExecuting = message.status == MessageStatus.TOOL_RUNNING

            // 处理工具调用结果
            var isToolSuccess = true

            if (message.role == MessageRole.TOOL) {
                isToolSuccess = message.toolCallResultSuccess
            }

            val isFirst = index == 0
            val isLast = totalCount > 0 && index == totalCount - 1

            // md 原文本，用于长按复制
            val rawText = buildString {
                when (message.role) {
                    MessageRole.USER -> message.content?.trim()?.let { append(it) }
                    MessageRole.ASSISTANT -> {
                        message.reasoningContent?.trim()?.takeIf { it.isNotEmpty() }?.let { append(it).append("\n\n") }
                        message.content?.trim()?.takeIf { it.isNotEmpty() }?.let { append(it) }
                    }
                    MessageRole.TOOL -> {
                        message.content?.trim()?.takeIf { it.isNotEmpty() }?.let { append(it).append("\n\n") }
                        message.toolCallResult?.trim()?.takeIf { it.isNotEmpty() }?.let { append(it) }
                    }
                    MessageRole.SYSTEM -> message.content?.trim()?.let { append(it) }
                }
            }.takeIf { it.isNotEmpty() }

            return TimelineEventData(
                event = event,
                time = time,
                detail = detail,
                rawText = rawText,
                isCompleted = isCompleted,
                isCurrent = isCurrent,
                isThinking = isThinking,
                isExecuting = isExecuting,
                isToolSuccess = isToolSuccess,
                index = index,
                isFirst = isFirst,
                isLast = isLast,
                totalCount = totalCount
            )
        }


        @SuppressLint("DefaultLocale")
        private fun formatTime(timestamp: Long): String {
            return StringUtils.dateFormat(timestamp, "mm:ss")
        }
    }
}

class TimelineEventFactory(private val context: Context) : IListRecyclerViewFactory {
    override fun createItemView(viewType: Int): ListRecyclerItemView {
        return TimelineEventView(context)
    }
} 