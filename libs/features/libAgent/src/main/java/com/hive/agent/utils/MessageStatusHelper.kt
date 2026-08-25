// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import com.hive.plugin.agent.model.MessageStatus
import com.hive.utils.GlobalApp

/**
 * 消息状态辅助工具类
 * 用于管理消息状态的变化和样式
 */
object MessageStatusHelper {
    
    /**
     * 获取状态对应的背景资源ID
     */
    fun getBackgroundResource(status: MessageStatus): Int {
        return when (status) {
            MessageStatus.WAITING -> com.hive.agent.R.drawable.chat_message_waiting_bg
            MessageStatus.TOOL_RUNNING -> com.hive.agent.R.drawable.chat_message_tool_running_bg
            MessageStatus.FINISH -> com.hive.agent.R.drawable.chat_message_finish_bg
        }
    }
    
    /**
     * 获取状态对应的文本描述
     */
    fun getStatusText(status: MessageStatus): String {
        return when (status) {
            MessageStatus.WAITING -> GlobalApp.getString(com.hive.i8n.R.string.agent_status_thinking)  // from ft-lib-script
            MessageStatus.TOOL_RUNNING -> GlobalApp.getString(com.hive.i8n.R.string.agent_status_tool_running)
            MessageStatus.FINISH -> ""
        }
    }
    
    /**
     * 获取状态对应的图标资源ID
     */
    fun getStatusIconResource(status: MessageStatus): Int {
        return when (status) {
            MessageStatus.WAITING -> com.hive.agent.R.drawable.loading_dots_static
            MessageStatus.TOOL_RUNNING -> com.hive.agent.R.drawable.ic_tool
            MessageStatus.FINISH -> 0 // 完成状态不需要图标
        }
    }
    
    /**
     * 判断状态是否需要显示状态指示器
     */
    fun shouldShowStatusIndicator(status: MessageStatus): Boolean {
        return status == MessageStatus.WAITING
    }
    
    /**
     * 判断状态是否需要动画
     */
    fun shouldAnimate(status: MessageStatus): Boolean {
        return false // 暂时禁用动画，避免兼容性问题
    }
} 
