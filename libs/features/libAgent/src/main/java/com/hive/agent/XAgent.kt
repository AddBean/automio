// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent

import com.hive.agent.ai.DefaultAIServiceManager
import com.hive.agent.config.AIAgentConfig
import com.hive.agent.core.AICoordinator
import com.hive.agent.core.AgentContext
import com.hive.agent.core.TaskStateManager
import com.hive.agent.debug.SimpleDebugger
import com.hive.agent.mcp.McpToolManager
import com.hive.agent.skill.SkillRegistry
import com.hive.agent.skill.SkillRunner
import com.hive.agent.skill.SkillToolLogger
import com.hive.plugin.agent.ExecutionContextFrame
import com.hive.plugin.agent.ExecutionContexts
import com.hive.plugin.agent.ExecutionContextType
import com.hive.plugin.agent.IExecutionContextObserver
import com.hive.plugin.agent.model.RunSkillRequest
import com.hive.plugin.agent.model.SkillResult
import com.hive.plugin.agent.model.SkillSpec
import com.hive.agent.utils.AgentCheckHelper
import com.hive.i8n.R
import com.hive.plugin.agent.AIServiceManager
import com.hive.plugin.agent.AgentToolClient
import com.hive.plugin.agent.IAICoordinator
import com.hive.plugin.agent.IAgentStateObserver
import com.hive.plugin.agent.IAgentTaskObserver
import com.hive.plugin.agent.ISkillStateObserver
import com.hive.plugin.agent.ITaskStateManager
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentErrorCode
import com.hive.plugin.agent.model.AgentRequest
import com.hive.plugin.agent.model.AgentResult
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.utils.GlobalApp
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.TaskResult
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * XAgent - 智能Agent系统的主入口
 * 提供完整的Agent功能，包括工作流规划、AI集成、MCP支持等
 */
class XAgent private constructor() {
    private val globalSkillIds = ConcurrentHashMap.newKeySet<String>()

    companion object {
        val GLOBAL_MCP_NAME = "mcp"

        @Volatile
        private var INSTANCE: XAgent? = null

        /**
         * 获取XAgent单例实例
         */
        fun getInstance(): XAgent {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: XAgent().also { INSTANCE = it }
            }
        }
    }

    private var agentContext: AgentContext? = null

    private lateinit var debugger: SimpleDebugger

    private var currentAgentTaskGoal: AgentTaskGoal? = null

    /**
     * 创建默认上下文
     */
    private fun createDefaultContext(): AgentContext {
        return object : AgentContext() {

            override val aiServiceProvider: AIServiceManager = DefaultAIServiceManager()

            override val taskStateManager: ITaskStateManager = TaskStateManager()

            override val agentManager: com.hive.plugin.agent.AgentManager =
                com.hive.agent.core.AgentManager(this)

            override val aiCoordinator: IAICoordinator = AICoordinator(this)

            override val mcpToolManager: McpToolManager = McpToolManager(this)

            override val skillRegistry: SkillRegistry = SkillRegistry()

            override val skillRunner: SkillRunner = SkillRunner(this, skillRegistry)

            override fun checkPermission(permission: String): Boolean {
                // 简化实现，实际应该检查Android权限
                return true
            }

            override fun getSystemProperty(key: String): String? {
                return System.getProperty(key)
            }

            override fun isNetworkAvailable(): Boolean {
                // 简化实现，实际应该检查网络状态
                return true
            }
        }
    }


    /**
     * 初始化Agent系统
     */
    fun initialize() {
        agentContext = createDefaultContext()
        agentContext?.initialize()

        // 初始化调试器
        debugger = SimpleDebugger()
        debugger.logExecution(
            "system", null, GlobalApp.getString(com.hive.i8n.R.string.agent_system_init_complete)
        )

        // 注册 ExecutionContext 栈变化观察者，便于 skill 调试时查看帧栈
        ExecutionContexts.stack.registerObserver(object : IExecutionContextObserver {
            override fun onExecutionContextStackChanged(snapshot: List<ExecutionContextFrame>) {
                SkillToolLogger.logFrameStack(snapshot)
            }
        })
    }

    /**
     * 注册SkillSpec
     */
    fun registerSkillSpec(spec: SkillSpec) {
        agentContext?.skillRegistry?.register(spec)
        globalSkillIds.add(spec.id)
    }

    fun registerTool(tool: AgentToolClient) {
        agentContext?.agentManager?.registerTool(tool)
    }

    fun unregisterTool(toolId: String) {
        agentContext?.agentManager?.unregisterTool(toolId)
    }


    /**
     * 将指定 MCP 服务器的工具标记为主循环可见的全局工具
     * @param serverId MCP 服务器 ID（如 mcp），对应 McpTool.id = "mcp:$serverId"
     */
    fun markGlobalTools(serverId: String) {
        val mcpTool = agentContext?.mcpToolManager?.getMcpServer(serverId) ?: return
        agentContext?.agentManager?.markToolAsGlobal(mcpTool.id)
    }

    /**
     * 注册MCP服务器
     */
    suspend fun registerMcpServer(serverId: String, serverUrl: String): Boolean {
        return agentContext?.registerMcpServer(serverId, serverUrl) ?: false
    }

    /**
     * 设置Agent观察者
     */
    fun registerAgentStateObserver(agentObserver: IAgentStateObserver) {
        agentContext?.registerAgentStateObserver(agentObserver)
    }

    /**
     * Agent观察者
     */
    fun unregisterAgentStateObserver(agentObserver: IAgentStateObserver) {
        agentContext?.unregisterAgentStateObserver(agentObserver)
    }

    /**
     * 设置Agent观察者
     */
    fun registerAgentTaskObserver(agentObserver: IAgentTaskObserver) {
        agentContext?.registerTaskObserver(agentObserver)
    }

    /**
     * Agent观察者
     */
    fun unregisterAgentTaskObserver(agentObserver: IAgentTaskObserver) {
        agentContext?.unregisterTaskObserver(agentObserver)
    }

    fun registerSkillStateObserver(observer: ISkillStateObserver) {
        agentContext?.registerSkillStateObserver(observer)
    }

    fun unregisterSkillStateObserver(observer: ISkillStateObserver) {
        agentContext?.unregisterSkillStateObserver(observer)
    }

    fun registerSkillTaskObserver(observer: IAgentTaskObserver) {
        agentContext?.registerSkillTaskObserver(observer)
    }

    fun unregisterSkillTaskObserver(observer: IAgentTaskObserver) {
        agentContext?.unregisterSkillTaskObserver(observer)
    }

    /**
     * 注销MCP服务器
     */
    fun unregisterMcpServer(serverId: String) {
        agentContext?.unregisterMcpServer(serverId)
    }

    /**
     * 同步执行本地已注册的 tool（如 buildin.dialog / custom.4a3d9c12），用于 MCP 转发。
     */
    suspend fun dispatchLocalToolRequest(
        toolId: String,
        arguments: Map<String, Any>
    ): AgentResult<*> {
        val request = AgentRequest(
            toolId = toolId,
            action = toolId,
            params = arguments
        )
        return agentContext?.agentManager?.dispatchRequest<Any>(request)
            ?: AgentResult.Failure(
                AgentError(AgentErrorCode.TOOL_NOT_FOUND, "Agent not ready", null)
            )
    }

    /**
     * 调用MCP工具
     */
    suspend fun callMcpTool(
        serverId: String, toolName: String, arguments: Map<String, Any>
    ): AgentResult<*> {
        return agentContext!!.callMcpTool(serverId, toolName, arguments)
    }


    fun executeTask(
        goal: AgentTaskGoal, onResult: ((result: TaskResult) -> Unit)?
    ) {
        if (!AgentCheckHelper.checkAgentEnv()) {
            AgentCheckHelper.showAgentEnvDialog()
            return
        }


        agentContext?.getScope()?.launch {
            val result = executeTaskInner(goal)
            onResult?.invoke(result)
        }
    }

    /**
     * 执行复杂工作流
     */
    private suspend fun executeTaskInner(
        goal: AgentTaskGoal,
    ): TaskResult {
        goal.userInputOptimized =
            AIAgentConfig.PromptDefaults.getOptimizedUserPrompt(goal.userInput)
        debugger.logExecution(
            goal.id,
            null,
            GlobalApp.getString(com.hive.i8n.R.string.agent_start_complex_workflow),
            mapOf(
                "description" to goal.userInputOptimized,
                "capabilities" to goal.requiredCapabilities.toString()
            )
        )
        currentAgentTaskGoal = goal
        return try {
            val result = agentContext!!.executeAgentTask(goal, useStream = true) as TaskResult
            debugger.logExecution(
                goal.id,
                null,
                if (result.status == ExecutionStatus.SUCCESS) GlobalApp.getString(com.hive.i8n.R.string.agent_workflow_success) else GlobalApp.getString(
                    com.hive.i8n.R.string.agent_workflow_failed
                ),
                mapOf("duration" to (result.endTime - result.startTime).toString())
            )
            result
        } catch (e: Exception) {
            debugger.logExecution(
                goal.id,
                null,
                GlobalApp.getString(com.hive.i8n.R.string.agent_workflow_exception),
                mapOf(
                    "error" to (e.message
                        ?: GlobalApp.getString(com.hive.i8n.R.string.agent_unknown_error))
                )
            )
            TaskResult.failure(
                taskId = goal.id, error = AgentError.create(
                    AgentErrorCode.TASK_STATE_ERROR,
                    e.message ?: GlobalApp.getString(com.hive.i8n.R.string.agent_execution_failed)
                )
            )
        }
    }


    /**
     * 获取已注册的工具列表
     */
    fun getRegisteredTools(): List<AgentToolClient> {
        return agentContext?.getAllTools().orEmpty()
    }

    /**
     * 获取工作流执行报告
     */
    fun getTaskReport(taskId: String): String {
        return debugger.generateSimpleReport(taskId)
    }

    /**
     * 获取所有工作流ID
     */
    fun getAllTaskIds(): List<String> {
        return debugger.getAllTaskIds()
    }

    fun getAIServiceManager(): AIServiceManager? {
        return agentContext?.aiServiceProvider
    }

    fun registerSkill(spec: SkillSpec) {
        agentContext?.skillRegistry?.register(spec)
        globalSkillIds.add(spec.id)
    }

    fun unregisterSkill(skillId: String) {
        agentContext?.skillRegistry?.unregister(skillId)
        globalSkillIds.remove(skillId)
    }

    fun listSkills(): List<SkillSpec> {
        return agentContext?.skillRegistry?.list()?.filter { it.id in globalSkillIds } ?: emptyList()
    }

    suspend fun runSkill(request: RunSkillRequest): SkillResult {
        val context = agentContext ?: return SkillResult(
            status = SkillResult.STATUS_FAILURE,
            summary = GlobalApp.getString(R.string.agent_skill_not_initialized),
            message = GlobalApp.getString(R.string.agent_skill_not_initialized)
        )
        // 脚本直接调用 runSkill 时栈顶为 SCRIPT，应传 null 使用独立 taskKey，避免复用已停止的 Agent taskId
        val taskIdForSkill = if (ExecutionContexts.stack.snapshot().lastOrNull()?.type == ExecutionContextType.SCRIPT) {
            null
        } else {
            currentAgentTaskGoal?.id
        }
        return context.skillRunner.runSkill(request, taskIdForSkill)
    }

    // 新增：工作流控制
    fun pauseTask(taskId: String) = agentContext?.getStateManager()?.pauseTask(taskId)

    fun resumeTask(taskId: String) = agentContext?.getStateManager()?.resumeTask(taskId)

    fun stopTask(taskId: String) = agentContext?.getStateManager()?.stopTask(taskId)

    fun getRunningTasks() = agentContext?.getStateManager()?.getRunningTaskIds()

    fun getCurrentAgentGoal() = currentAgentTaskGoal

    fun getTasksByState(states: List<ExecutionStatus>? = null) =
        agentContext?.getStateManager()?.getTasksByState(states)

    /**
     * 清理资源
     */
    fun cleanup() {
        agentContext?.cleanup()
        debugger.clearOldLogs()
        debugger.logExecution(
            "system", null, GlobalApp.getString(com.hive.i8n.R.string.agent_system_cleanup_complete)
        )
    }

    suspend fun refreshMcpServer(serverId: String) {
        agentContext?.refreshMcpTools(serverId)
    }
}
