// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.provider;

import com.hive.plugin.IComponentProvider;
import com.hive.plugin.agent.AIServiceManager;
import com.hive.plugin.agent.IAgentTaskObserver;
import com.hive.plugin.agent.IAgentStateObserver;
import com.hive.plugin.agent.ISkillStateObserver;
import com.hive.plugin.agent.model.AgentResult;
import com.hive.plugin.agent.AgentToolClient;
import com.hive.plugin.agent.model.ExecutionStatus;
import com.hive.plugin.agent.model.AgentTaskGoal;
import com.hive.plugin.agent.model.RunSkillRequest;
import com.hive.plugin.agent.model.SkillResult;
import com.hive.plugin.agent.model.SkillSpec;
import com.hive.plugin.agent.model.TaskResult;

import java.util.List;
import java.util.Map;

public interface IAgentProvider extends IComponentProvider {

    /**
     * 初始化 Agent 服务
     * <p>
     * 该方法用于初始化 Agent 服务，通常在应用启动时调用。
     */
    void initAgentService();

    /**
     * 注册 MCP 服务器
     *
     * @param toolName  工具名称
     * @param serverUrl 服务器 URL
     */
    void registerGlobalMcpServer(String toolName, String serverUrl);

    /**
     * 注册 skill spec
     *
     */
    void registerSkillSpec(SkillSpec skillSpec);

    /**
     * 注销 skill spec
     */
    void unregisterSkillSpec(String skillId);

    /**
     * 注册全局 tool（不按 scope 管理，导入/保存时持久注册）。
     */
    void registerTool(AgentToolClient tool);

    /**
     * 注销全局 tool。
     */
    void unregisterTool(String toolId);

    /**
     * 获取已注册的技能列表
     *
     * @return 技能列表，无实现时返回空列表
     */
    List<SkillSpec> listSkills();

    /**
     * 同步执行技能（脚本命令用）
     *
     * @param request 执行请求
     * @return 执行结果，无实现时返回 status=failure
     */
    SkillResult runSkillSync(RunSkillRequest request);

    boolean isAgentServiceReady();

    /**
     * 获取 AI 服务管理器
     *
     * @return AIServiceManager 实例
     */
    AIServiceManager getAIServiceManager();

    /**
     * 启动 Agent 服务
     *
     * @return 是否启动成功
     */
    void startAgentService();

    /**
     * 停止 Agent 服务
     *
     * @return 是否停止成功
     */
    void stopAgentService();

    /**
     * 检查 Agent 服务是否正在运行
     *
     * @return 是否正在运行
     */
    boolean isAgentServiceRunning();

    /**
     * 注销 MCP 服务器
     *
     * @param serverId 服务器ID
     */
    void unregisterMcpServer(String serverId);


    /**
     * 注册 Agent state观察者
     *
     * @param agentObserver Agent观察者
     */
    void registerAgentStateObserver(IAgentStateObserver agentObserver);

    /**
     * 注销 Agent 观察者
     *
     * @param agentObserver Agent观察者
     */
    void unregisterAgentStateObserver(IAgentStateObserver agentObserver);

    /**
     * 注册 Agent 观察者
     *
     * @param agentObserver Agent观察者
     */
    void registerAgentTaskObserver(IAgentTaskObserver agentObserver);

    /**
     * 注销 Agent 观察者
     *
     * @param agentObserver Agent观察者
     */
    void unregisterAgentTaskObserver(IAgentTaskObserver agentObserver);

    /**
     * 注册 Skill 状态观察者
     */
    void registerSkillStateObserver(ISkillStateObserver observer);

    /**
     * 注销 Skill 状态观察者
     */
    void unregisterSkillStateObserver(ISkillStateObserver observer);

    /**
     * 注册 Skill 任务观察者
     */
    void registerSkillTaskObserver(IAgentTaskObserver observer);

    /**
     * 注销 Skill 任务观察者
     */
    void unregisterSkillTaskObserver(IAgentTaskObserver observer);

    /**
     * 执行任务
     *
     * @param goal     任务目标
     * @param onResult 结果回调
     */
    void executeAgentTask(AgentTaskGoal goal, TaskResultCallback onResult);

    /**
     * 直接执行 tools
     *
     * @param serverId
     * @param toolName
     * @param arguments
     */
    AgentResult<Object> executeAgentToolSync(String serverId, String toolName, Map<String, Object> arguments);

    /**
     * 同步执行本地已注册的 tool（如 buildin.dialog / custom.4a3d9c12），用于 MCP 转发。
     *
     * @param toolId    tool id，如 custom.4a3d9c12
     * @param arguments 调用参数
     * @return 执行结果
     */
    AgentResult<Object> executeLocalToolSync(String toolId, Map<String, Object> arguments);

    /**
     * 获取已注册的工具列表
     *
     * @return 工具列表
     */
    List<AgentToolClient> getRegisteredTools();

    /**
     * 获取工作流执行报告
     *
     * @param taskId 任务ID
     * @return 执行报告
     */
    String getTaskReport(String taskId);

    /**
     * 获取所有工作流ID
     *
     * @return 工作流ID列表
     */
    List<String> getAllTaskIds();

    /**
     * 暂停任务
     *
     * @param taskId 任务ID
     */
    void pauseTask(String taskId);

    /**
     * 恢复任务
     *
     * @param taskId 任务ID
     */
    void resumeTask(String taskId);

    /**
     * 停止任务
     *
     * @param taskId 任务ID
     */
    void stopTask(String taskId);

    /**
     * 获取当前 Goal
     */
    AgentTaskGoal getCurrentAgentGoal();

    /**
     * 获取运行中的任务
     *
     * @return 运行中的任务ID列表
     */
    List<String> getRunningTasks();

    /**
     * 根据状态获取任务
     *
     * @param states 状态列表
     * @return 任务状态映射
     */
    List<String> getTasksByState(List<ExecutionStatus> states);

    /**
     * 刷新 MCP 服务器
     *
     * @param serverId 服务器ID
     */
    void refreshMcpServer(String serverId);

    /**
     * 刷新 MCP 服务器
     */
    void refreshAllMcpServer(OnRefreshMcpServerCallback callback);

    /**
     * 任务结果回调接口
     */
    interface TaskResultCallback {
        void onResult(TaskResult result);
    }

    interface OnRefreshMcpServerCallback {
        void onRefreshed();
    }
}
