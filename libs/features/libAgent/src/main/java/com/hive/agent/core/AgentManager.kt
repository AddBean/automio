// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.core
import com.hive.agent.skill.SkillToolLogger
import com.hive.plugin.agent.AgentManager
import com.hive.plugin.agent.AgentToolClient
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentErrorCode
import com.hive.plugin.agent.model.AgentRequest
import com.hive.plugin.agent.model.AgentResult
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.TaskResult
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * AgentManager的默认实现
 * 支持任务互斥执行、暂停、停止、恢复功能
 */
class AgentManager(
    override val agentContext: AgentContext,
    coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()
) : AgentManager {

    private val scope = CoroutineScope(coroutineContext)

    private val tools = ConcurrentHashMap<String, AgentToolClient>()
    private val globalToolIds = ConcurrentHashMap.newKeySet<String>()

    companion object {
        const val TAG = "AgentManagerImpl"
    }

    override fun registerTool(tool: AgentToolClient) {
        tools[tool.id] = tool
        DLog.d(
            TAG,
            GlobalApp.getString(com.hive.i8n.R.string.agent_tool_register, tool.name, tool.id)
        )
    }

    override fun unregisterTool(toolId: String) {
        tools.remove(toolId)?.let { tool ->
            try {
                globalToolIds.remove(toolId)
                tool.onDestroy()
                DLog.d(
                    TAG,
                    GlobalApp.getString(
                        com.hive.i8n.R.string.agent_tool_unregister,
                        tool.name,
                        tool.id
                    )
                )
            } catch (e: Exception) {
                DLog.w(
                    TAG,
                    GlobalApp.getString(
                        com.hive.i8n.R.string.agent_tool_destroy_error,
                        tool.name,
                        e.message ?: ""
                    )
                )
            }
        }
    }

    override suspend fun <T> dispatchRequest(request: AgentRequest): AgentResult<*> {
        return withContext(scope.coroutineContext) {
            try {
                // 1. 选择最佳工具
                val selectedTool = selectBestTool(request)
                    ?: return@withContext AgentResult.Failure(
                        AgentError(
                            code = AgentErrorCode.TOOL_NOT_FOUND,
                            msg = GlobalApp.getString(
                                com.hive.i8n.R.string.agent_tool_not_found,
                                request.toolId,
                                request.preferredTools.toString()
                            )
                        )
                    )

                SkillToolLogger.d("dispatch toolId=${request.toolId} action=${request.action}")

                // 3. 执行带超时的操作
                val result = withTimeoutOrNull(request.timeout) {
                    selectedTool.execute(request)
                }

                // 4. 处理结果
                result ?: AgentResult.Failure(
                    AgentError(
                        code = AgentErrorCode.TIMEOUT,
                        msg = GlobalApp.getString(
                            com.hive.i8n.R.string.agent_operation_timeout,
                            request.timeout
                        )
                    )
                )

            } catch (e: CancellationException) {
                AgentResult.Failure(
                    AgentError(
                        code = AgentErrorCode.EXECUTION_FAILED,
                        msg = GlobalApp.getString(com.hive.i8n.R.string.agent_operation_cancelled),
                        e = e
                    )
                )
            } catch (e: Exception) {
                AgentResult.Failure(
                    AgentError(
                        code = AgentErrorCode.EXECUTION_FAILED,
                        msg = e.message
                            ?: GlobalApp.getString(com.hive.i8n.R.string.agent_execution_failed),
                        e = e
                    )
                )
            }
        }
    }

    override suspend fun executeAgentTask(goal: AgentTaskGoal, useStream: Boolean): TaskResult? {
        val startTime = System.currentTimeMillis()
        // 检查是否有其他任务正在执行
        agentContext.taskStateManager.startTask(goal.id)
        return try {
            // 设置当前任务状态为运行中
            agentContext.taskStateManager.setCurrentState(goal.id, ExecutionStatus.RUNNING)
            // 将复杂工作流执行委托给 AICoordinator
            val result = agentContext.aiCoordinator.coordinateTask(goal, useStream)
            agentContext.taskStateManager.setCurrentState(goal.id, ExecutionStatus.SUCCESS)
            return result
        } catch (e: Exception) {
            agentContext.taskStateManager.setCurrentState(goal.id, ExecutionStatus.FAILED)
            TaskResult.failure(
                taskId = goal.id,
                error = AgentError.create(AgentErrorCode.EXECUTION_FAILED, cause = e),
                startTime = startTime
            )
        } finally {
            // 清理当前任务
            agentContext.taskStateManager.clearTask(goal.id)
        }
    }

    override fun getRegisteredTools(): List<AgentToolClient> {
        return tools.values.toList()
    }

    override fun markToolAsGlobal(toolId: String) {
        if (tools.containsKey(toolId)) {
            globalToolIds.add(toolId)
        }
    }

    override fun unmarkToolAsGlobal(toolId: String) {
        globalToolIds.remove(toolId)
    }

    override fun getGlobalToolIds(): Set<String> {
        return globalToolIds.toSet()
    }

    override fun isToolAvailable(toolId: String): Boolean {
        return tools.containsKey(toolId)
    }

    /**
     * 获取任务统计信息
     */
    private fun getTaskStatistics(): Map<String, Any?> {
        val allTaskStates = agentContext.taskStateManager.getAllTaskState()
        return mapOf(
            "totalTasks" to allTaskStates.size,
            "runningTasks" to agentContext.taskStateManager.getRunningTaskIds()?.size!!,
            "taskStates" to allTaskStates.mapValues { it.value.name }
        )
    }

    /**
     * 选择最佳工具
     * 优先级：preferredTools > toolId > 默认工具
     */
    private fun selectBestTool(request: AgentRequest): AgentToolClient? {
        // 1. 如果指定了优先工具列表，按顺序查找
        request.preferredTools?.let { preferredTools ->
            for (toolId in preferredTools) {
                val tool = tools[toolId]
                if (tool != null && request.action in tool.supportedMethods) {
                    return tool
                }
            }
        }

        // 2. 尝试使用指定的 toolId
        val specifiedTool = tools[request.toolId]
        if (specifiedTool != null && request.action in specifiedTool.supportedMethods) {
            return specifiedTool
        }
        return specifiedTool
    }

    override fun stopExecution() {
        tools.forEach {
            it.value.stopExecute()
        }
    }

}
