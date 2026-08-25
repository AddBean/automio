// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.event

class AgentEvent {
    /**
     * 事件类型
     */
    var type: AgentEventType = AgentEventType.AGENT_SERVICE_START

    /**
     * 事件数据
     */
    var data: Any? = null

    constructor(type: AgentEventType) {
        this.type = type
    }

    constructor(type: AgentEventType, data: Any?) {
        this.type = type
        this.data = data
    }
}

enum class AgentEventType(val type: Int) {
    /**
     * 代理服务启动
     */
    AGENT_SERVICE_START(1),

    /**
     * 代理服务停止
     */
    AGENT_SERVICE_STOP(2),

    /**
     * 代理任务开始
     */
    AGENT_TASK_START(3),

    /**
     * 代理任务结束
     */
    AGENT_TASK_END(4),

    /**
     * 代理任务状态更新
     */
    AGENT_TASK_STATUS_UPDATE(5),

    /**
     * 代理错误发生
     */
    AGENT_ERROR(6),

    /**
     * MCP服务注册成功
     */
    AGENT_SERVICE_MCP_REGISTERED(7),
    /**
     * 展示新的任务视图
     */
    AGENT_NEW_TASK_VIEW(8),
    
    /**
     * 聊天Fragment已准备就绪
     */
    AGENT_CHAT_FRAGMENT_READY(9),

    /**
     * 聊天页顶部快捷入口显隐
     */
    AGENT_CHAT_TOOLBAR_ACTIONS_VISIBILITY(10),

    /**
     * 导航到资源页指定类型
     */
    AGENT_NAVIGATE_TO_RESOURCE_TYPE(11);
}
