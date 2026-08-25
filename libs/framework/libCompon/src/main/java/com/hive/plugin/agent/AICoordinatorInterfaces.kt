// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent

import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.TaskResult

/**
 * AI 协调器接口，负责驱动 AI 进行工具选择和工作流执行
 */
interface IAICoordinator {

    val agentContext: IAgentContext

    /**
     * 根据工作流目标，协调 AI 进行工具选择和执行，完成复杂工作流
     * 支持流式和非流式两种模式
     *
     * @param goal 工作流目标
     * @param useStream 是否使用流式推理，默认为 false
     * @return 工作流执行结果，流式模式下返回 Flow<TaskResult>，非流式模式下返回 TaskResult
     */
    suspend fun coordinateTask(goal: AgentTaskGoal, useStream: Boolean = false): TaskResult?

}

/**
 * Error context enum - indicates where error occurred
 */
enum class ErrorContext {
    AGENT_CHAT,       // Main agent chat loop
    SKILL_EXECUTION,  // Skill runner context
    TOOL_CALL,        // Tool execution
    MODEL_SELECTION,  // AI provider/model selection
    NETWORK_CALL      // HTTP/network layer
}

interface IAgentTaskObserver {
    /**
     * 观察者方法，工作流更新时调用
     *
     */
    fun onTaskInfoUpdated(message: String)

    fun onTaskMessageUpdated(goal: AgentTaskGoal)

    fun onTaskMessageStreamUpdated(goal: AgentTaskGoal)

    /**
     * 记忆压缩状态变化时调用（消息摘要处理中/完成）
     */
    fun onMemoryCompressing(taskId: String, isCompressing: Boolean) {}

    /**
     * NEW: Dedicated error callback with full details
     */
    fun onTaskError(
        taskId: String,
        error: AgentError,
        context: ErrorContext = ErrorContext.AGENT_CHAT
    ) {}
}

