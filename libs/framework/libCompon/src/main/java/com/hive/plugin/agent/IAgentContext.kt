// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent

import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentResult
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.SkillResult
import com.hive.plugin.agent.model.TaskResult
import kotlinx.coroutines.CoroutineScope

interface IAgentContext {

    fun initialize()

    fun registerAgentStateObserver(observer: IAgentStateObserver)

    fun unregisterAgentStateObserver(observer: IAgentStateObserver)

    fun registerTaskObserver(observer: IAgentTaskObserver)

    fun unregisterTaskObserver(observer: IAgentTaskObserver)

    fun registerSkillStateObserver(observer: ISkillStateObserver)

    fun unregisterSkillStateObserver(observer: ISkillStateObserver)

    fun registerSkillTaskObserver(observer: IAgentTaskObserver)

    fun unregisterSkillTaskObserver(observer: IAgentTaskObserver)

    fun notifyTaskInfoUpdated(data: String)

    fun notifyTaskMessageUpdated(goal: AgentTaskGoal)

    fun notifyTaskMessageStreamUpdated(goal: AgentTaskGoal)

    fun notifyMemoryCompressing(taskId: String, isCompressing: Boolean)

    fun notifySkillExecuteStart(taskId: String)

    fun notifySkillExecuteEnd(taskId: String, result: SkillResult?)

    fun notifySkillMessageUpdated(goal: AgentTaskGoal)

    fun notifySkillMessageStreamUpdated(goal: AgentTaskGoal)

    fun notifyTaskError(
        taskId: String,
        error: AgentError,
        context: ErrorContext = ErrorContext.AGENT_CHAT
    )

    suspend fun executeAgentTask(goal: AgentTaskGoal, useStream: Boolean = false): TaskResult?

    suspend fun registerMcpServer(serverId: String, serverUrl: String): Boolean

    fun unregisterMcpServer(serverId: String)

    suspend fun callMcpTool(
        serverId: String,
        toolName: String,
        arguments: Map<String, Any>
    ): AgentResult<*>

    suspend fun refreshMcpTools(serverId: String)

    fun getStateManager(): ITaskStateManager

    fun getScope(): CoroutineScope

    fun getAllTools(): List<AgentToolClient>

    fun checkPermission(permission: String): Boolean

    fun getSystemProperty(key: String): String?

    fun isNetworkAvailable(): Boolean

    fun cleanup()

}