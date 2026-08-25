// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.core

import com.hive.agent.core.AgentManager.Companion.TAG
import com.hive.agent.mcp.McpToolManager
import com.hive.agent.skill.SkillRegistry
import com.hive.agent.skill.SkillRunner
import com.hive.plugin.agent.AIServiceManager
import com.hive.plugin.agent.AgentManager
import com.hive.plugin.agent.AgentToolClient
import com.hive.plugin.agent.ErrorContext
import com.hive.plugin.agent.IAICoordinator
import com.hive.plugin.agent.IAgentContext
import com.hive.plugin.agent.IAgentStateObserver
import com.hive.plugin.agent.IAgentTaskObserver
import com.hive.plugin.agent.ISkillStateObserver
import com.hive.plugin.agent.ITaskStateManager
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentResult
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.SkillResult
import com.hive.plugin.agent.model.TaskResult
import com.hive.utils.debug.DLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** Agent上下文接口 */
abstract class AgentContext : IAgentStateObserver, IAgentContext {

    private val lifecycleScope = CoroutineScope(Dispatchers.IO)

    // AI 服务提供者
    abstract val aiServiceProvider: AIServiceManager

    // 任务状态管理器
    abstract val taskStateManager: ITaskStateManager

    // AI 协调器
    abstract val aiCoordinator: IAICoordinator

    // agent 管理器
    abstract val agentManager: AgentManager

    // mcp 管理器
    abstract val mcpToolManager: McpToolManager

    // skill 注册表
    abstract val skillRegistry: SkillRegistry

    // skill 执行器
    abstract val skillRunner: SkillRunner

    private var agentObservers: MutableList<IAgentTaskObserver> = mutableListOf()
    private var skillStateObservers: MutableList<ISkillStateObserver> = mutableListOf()
    private var skillTaskObservers: MutableList<IAgentTaskObserver> = mutableListOf()

    override fun initialize() {
        // 设置任务状态变化监听器
        taskStateManager.registerAgentStateListener(this)
        aiServiceProvider.initAIServiceProviders()
    }

    override fun registerAgentStateObserver(observer: IAgentStateObserver) {
        taskStateManager.registerAgentStateListener(observer)
    }

    override fun unregisterAgentStateObserver(observer: IAgentStateObserver) {
        taskStateManager.unregisterAgentStateListener(observer)
    }

    override fun registerTaskObserver(observer: IAgentTaskObserver) {
        if (!agentObservers.contains(observer)) {
            agentObservers.add(observer)
        }
    }

    override fun unregisterTaskObserver(observer: IAgentTaskObserver) {
        agentObservers.remove(observer)
    }

    override fun registerSkillStateObserver(observer: ISkillStateObserver) {
        if (!skillStateObservers.contains(observer)) {
            skillStateObservers.add(observer)
        }
    }

    override fun unregisterSkillStateObserver(observer: ISkillStateObserver) {
        skillStateObservers.remove(observer)
    }

    override fun registerSkillTaskObserver(observer: IAgentTaskObserver) {
        if (!skillTaskObservers.contains(observer)) {
            skillTaskObservers.add(observer)
        }
    }

    override fun unregisterSkillTaskObserver(observer: IAgentTaskObserver) {
        skillTaskObservers.remove(observer)
    }

    override fun notifyTaskInfoUpdated(data: String) {
        agentObservers.forEach { it.onTaskInfoUpdated(data) }
    }

    override fun notifyTaskMessageUpdated(goal: AgentTaskGoal) {
        agentObservers.forEach { it.onTaskMessageUpdated(goal) }
    }


    override fun notifyTaskMessageStreamUpdated(goal: AgentTaskGoal) {
        agentObservers.forEach { it.onTaskMessageStreamUpdated(goal) }
    }

    override fun notifyMemoryCompressing(taskId: String, isCompressing: Boolean) {
        agentObservers.forEach { it.onMemoryCompressing(taskId, isCompressing) }
        skillTaskObservers.forEach { it.onMemoryCompressing(taskId, isCompressing) }
    }

    override fun notifySkillExecuteStart(taskId: String) {
        skillStateObservers.forEach { it.onSkillExecuteStart(taskId) }
    }

    override fun notifySkillExecuteEnd(taskId: String, result: SkillResult?) {
        skillStateObservers.forEach { it.onSkillExecuteEnd(taskId, result) }
    }

    override fun notifySkillMessageUpdated(goal: AgentTaskGoal) {
        skillTaskObservers.forEach { it.onTaskMessageUpdated(goal) }
    }

    override fun notifySkillMessageStreamUpdated(goal: AgentTaskGoal) {
        skillTaskObservers.forEach { it.onTaskMessageStreamUpdated(goal) }
    }

    override fun notifyTaskError(
        taskId: String,
        error: AgentError,
        context: ErrorContext
    ) {
        agentObservers.forEach { it.onTaskError(taskId, error, context) }
        skillTaskObservers.forEach { it.onTaskError(taskId, error, context) }
    }

    override suspend fun executeAgentTask(goal: AgentTaskGoal, useStream: Boolean): TaskResult? {
        return agentManager.executeAgentTask(goal, useStream)
    }

    override suspend fun registerMcpServer(serverId: String, serverUrl: String): Boolean {
        return mcpToolManager.registerMcpServer(serverId, serverUrl)
    }

    override fun unregisterMcpServer(serverId: String) {
        mcpToolManager.unregisterMcpServer(serverId)
    }

    override suspend fun callMcpTool(
        serverId: String,
        toolName: String,
        arguments: Map<String, Any>
    ): AgentResult<*> {
        return mcpToolManager.callMcpTool(serverId, toolName, arguments)
    }

    override suspend fun refreshMcpTools(serverId: String) {
        mcpToolManager.getMcpServer(serverId)?.refreshTools()
    }

    override fun onAgentExecuteStart(taskId: String) {

    }

    override fun onAgentExecuteEnd(taskId: String,taskResult: TaskResult?) {

    }

    override fun onAgentStateChanged(taskId: String, status: ExecutionStatus) {
        notifyTaskInfoUpdated("任务状态变化: $taskId -> ${status}")
        // 根据状态变化执行相应的处理逻辑
        when (status) {
            ExecutionStatus.PAUSED -> {
                aiServiceProvider.stopInference()
                agentManager.stopExecution()
                DLog.i(TAG, "任务 $taskId 已暂停")
            }

            ExecutionStatus.STOPPED -> {
                DLog.i(TAG, "任务 $taskId 已停止")
                aiServiceProvider.stopInference()
                agentManager.stopExecution()
                DLog.i(TAG, "任务 $taskId 已暂停")
            }

            else -> {}
        }
    }

    override fun getStateManager(): ITaskStateManager {
        return taskStateManager
    }

    override fun getScope() = lifecycleScope

    override fun getAllTools(): List<AgentToolClient> {
        return agentManager.getRegisteredTools()
    }

    override fun cleanup() {
        agentManager.getRegisteredTools().forEach { tool -> agentManager.unregisterTool(tool.id) }
    }
}
