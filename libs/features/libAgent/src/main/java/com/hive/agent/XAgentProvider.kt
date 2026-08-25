// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent

import android.content.Context
import android.text.TextUtils
import com.hive.agent.config.ConfigAgentModels
import com.hive.agent.mcp.AgentSkillMcpRegister
import com.hive.agent.mcp.McpToolClient
import com.hive.plugin.agent.model.SkillSpec
import com.hive.annotation.NotProguard
import com.hive.event.AgentEvent
import com.hive.event.AgentEventType
import com.hive.plugin.agent.AIServiceManager
import com.hive.plugin.agent.AgentToolClient
import com.hive.plugin.agent.IAgentStateObserver
import com.hive.plugin.agent.IAgentTaskObserver
import com.hive.plugin.agent.ISkillStateObserver
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentErrorCode
import com.hive.plugin.agent.model.AgentResult
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.RunSkillRequest
import com.hive.plugin.agent.model.SkillResult
import com.hive.plugin.provider.IAgentProvider
import com.hive.plugin.provider.IMcpProvider
import com.hive.plugin.ComponentManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

@NotProguard
class XAgentProvider : IAgentProvider {
    private var context: Context? = null

    /**
     * 是否正在运行
     */
    private var isServiceRunning: Boolean = false

    private val xAgent: XAgent by lazy {
        XAgent.getInstance()
    }

    override fun init(context: Context?) {
        this.context = context
    }

    override fun initAgentService() {
        xAgent.initialize()
        // 将 Skill 入口注册到本地 MCP 服务（tools/list 可见、tools/call 可调用）
        (ComponentManager.getInstance().getProvider(IMcpProvider::class.java) as? IMcpProvider)?.let {
            AgentSkillMcpRegister.register(it)
        }
        EventBus.getDefault().post(AgentEvent(AgentEventType.AGENT_SERVICE_START))
        loadDefaultModel()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadDefaultModel() {
        GlobalScope.launch(Dispatchers.IO) {
            val defaultProviderName = when {
                aiServiceManager?.getProvider("default") != null -> "default"
                else -> ConfigAgentModels.read()
                    ?.find { it.isEnabled && ((!it.apikeyEnabled) || !TextUtils.isEmpty(it.apiKey)) }
                    ?.name
                    ?: aiServiceManager?.getProviderList()?.firstOrNull()?.getProviderInfo()?.name
                    ?: "ollama"
            }
            aiServiceManager?.getProvider(defaultProviderName)?.getModels()
            // 立即刷新列表
            aiServiceManager?.loadDefaultInferenceModelIfNeeded(defaultProviderName)
        }
    }

    override fun registerGlobalMcpServer(
        toolName: String,
        serverUrl: String
    ) {
        GlobalScope.launch {
            xAgent.registerMcpServer(
                toolName,
                serverUrl
            )
            xAgent.markGlobalTools(toolName)
            isServiceRunning = true
            EventBus.getDefault()
                .post(AgentEvent(AgentEventType.AGENT_SERVICE_MCP_REGISTERED, toolName))
        }
    }

    override fun registerSkillSpec(
        skillSpec: SkillSpec,
    ) {
        xAgent.registerSkillSpec(skillSpec)
    }

    override fun unregisterSkillSpec(skillId: String) {
        xAgent.unregisterSkill(skillId)
    }

    override fun registerTool(tool: AgentToolClient) {
        xAgent.registerTool(tool)
    }

    override fun unregisterTool(toolId: String) {
        xAgent.unregisterTool(toolId)
    }

    override fun listSkills(): List<SkillSpec> {
        return xAgent.listSkills()
    }

    override fun runSkillSync(request: RunSkillRequest): SkillResult {
        return runBlocking {
            xAgent.runSkill(request)
        }
    }

    override fun isAgentServiceReady(): Boolean {
        return isServiceRunning
    }

    override fun getAIServiceManager(): AIServiceManager? {
        return xAgent.getAIServiceManager()
    }

    override fun startAgentService() {
        // 服务已经在registerMcpServer中启动
    }

    override fun stopAgentService() {
        xAgent.cleanup()
        isServiceRunning = false
    }

    override fun isAgentServiceRunning(): Boolean {
        return isServiceRunning
    }

    /**
     * 注销MCP服务器
     */
    override fun unregisterMcpServer(serverId: String) {
        xAgent.unregisterMcpServer(serverId)
    }


    override fun executeAgentToolSync(
        serverId: String?,
        toolName: String?,
        arguments: MutableMap<String, Any>?
    ): AgentResult<Any> {
        return runBlocking {
            try {
                val result = xAgent.callMcpTool(
                    serverId ?: "",
                    toolName ?: "",
                    arguments ?: mutableMapOf()
                )
                result as AgentResult<Any>
            } catch (e: Exception) {
                AgentResult.Failure(
                    AgentError(
                        AgentErrorCode.UNKNOWN_ERROR,
                        "执行工具时发生异常: ${e.message}",
                        e
                    )
                ) as AgentResult<Any>
            }
        }
    }

    override fun executeLocalToolSync(
        toolId: String?,
        arguments: Map<String, Any>?
    ): AgentResult<Any> {
        if (toolId.isNullOrBlank()) {
            return AgentResult.Failure(
                AgentError(AgentErrorCode.INVALID_PARAMS, "toolId is blank", null)
            ) as AgentResult<Any>
        }
        return runBlocking {
            try {
                val result = xAgent.dispatchLocalToolRequest(
                    toolId = toolId!!,
                    arguments = arguments ?: emptyMap<String, Any>()
                )
                result as AgentResult<Any>
            } catch (e: Exception) {
                AgentResult.Failure(
                    AgentError(
                        AgentErrorCode.UNKNOWN_ERROR,
                        "执行本地工具时发生异常: ${e.message}",
                        e
                    )
                ) as AgentResult<Any>
            }
        }
    }

    override fun registerAgentStateObserver(agentObserver: IAgentStateObserver) {
        xAgent.registerAgentStateObserver(agentObserver)
    }

    override fun unregisterAgentStateObserver(agentObserver: IAgentStateObserver) {
        xAgent.unregisterAgentStateObserver(agentObserver)
    }


    /**
     * 注册Agent观察者
     */
    override fun registerAgentTaskObserver(agentObserver: IAgentTaskObserver) {
        xAgent.registerAgentTaskObserver(agentObserver)
    }

    /**
     * 注销Agent观察者
     */
    override fun unregisterAgentTaskObserver(agentObserver: IAgentTaskObserver) {
        xAgent.unregisterAgentTaskObserver(agentObserver)
    }

    override fun registerSkillStateObserver(observer: ISkillStateObserver) {
        xAgent.registerSkillStateObserver(observer)
    }

    override fun unregisterSkillStateObserver(observer: ISkillStateObserver) {
        xAgent.unregisterSkillStateObserver(observer)
    }

    override fun registerSkillTaskObserver(observer: IAgentTaskObserver) {
        xAgent.registerSkillTaskObserver(observer)
    }

    override fun unregisterSkillTaskObserver(observer: IAgentTaskObserver) {
        xAgent.unregisterSkillTaskObserver(observer)
    }

    /**
     * 执行任务
     */
    override fun executeAgentTask(
        goal: AgentTaskGoal,
        onResult: IAgentProvider.TaskResultCallback?
    ) {
        xAgent.executeTask(goal) { result ->
            onResult?.onResult(result)
        }
    }


    /**
     * 获取已注册的工具列表
     */
    override fun getRegisteredTools(): List<AgentToolClient> {
        return xAgent.getRegisteredTools()
    }

    /**
     * 获取工作流执行报告
     */
    override fun getTaskReport(taskId: String): String {
        return xAgent.getTaskReport(taskId)
    }

    /**
     * 获取所有工作流ID
     */
    override fun getAllTaskIds(): List<String> {
        return xAgent.getAllTaskIds()
    }

    /**
     * 暂停任务
     */
    override fun pauseTask(taskId: String) {
        xAgent.pauseTask(taskId)
    }

    /**
     * 恢复任务
     */
    override fun resumeTask(taskId: String) {
        xAgent.resumeTask(taskId)
    }

    /**
     * 停止任务
     */
    override fun stopTask(taskId: String) {
        xAgent.stopTask(taskId)
    }

    override fun getCurrentAgentGoal(): AgentTaskGoal? {
        return xAgent.getCurrentAgentGoal();
    }

    /**
     * 获取运行中的任务
     */
    override fun getRunningTasks(): List<String>? {
        return xAgent.getRunningTasks()
    }

    /**
     * 根据状态获取任务
     */
    override fun getTasksByState(states: List<ExecutionStatus>?): List<String>? {
        return xAgent.getTasksByState(states)
    }

    /**
     * 刷新MCP服务器
     */
    override fun refreshMcpServer(serverId: String) {
        GlobalScope.launch {
            xAgent.refreshMcpServer(serverId)
        }
    }

    override fun refreshAllMcpServer(callback: IAgentProvider.OnRefreshMcpServerCallback) {
        GlobalScope.launch {
            xAgent.getRegisteredTools().filterIsInstance<McpToolClient>().forEach {
                xAgent.refreshMcpServer(it.serverId)
            }
            withContext(Dispatchers.Main) {
                callback.onRefreshed()
            }
        }
    }

    /**
     * 刷新MCP服务器（异步版本，使用回调）
     */
    fun refreshMcpServerAsync(
        serverId: String,
        callback: (Boolean) -> Unit
    ) {
        GlobalScope.launch {
            try {
                xAgent.refreshMcpServer(serverId)
                callback(true)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }
}
