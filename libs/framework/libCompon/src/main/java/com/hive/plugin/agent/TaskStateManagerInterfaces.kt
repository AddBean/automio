// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent

import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.TaskResult


interface ITaskStateManager {

    /**
     * 设置状态变化回调
     */
    fun startTask(taskId: String): Boolean

    /**
     * 暂停任务
     * @param taskId 任务ID
     * @return true 如果成功暂停，false 如果任务不存在或不是当前任务
     */
    fun pauseTask(taskId: String): Boolean

    /**
     * 恢复任务
     * @param taskId 任务ID
     * @return true 如果成功恢复，false 如果任务不存在或不是当前任务
     */
    fun resumeTask(taskId: String): Boolean

    /**
     * 停止任务
     * @param taskId 任务ID
     * @return true 如果成功停止，false 如果任务不存在或不是当前任务
     */
    fun stopTask(taskId: String): Boolean

    /**
     * 获取当前任务状态
     */
    fun getCurrentState(taskId: String): ExecutionStatus

    /**
     * 设置当前任务状态
     */
    fun setCurrentState(taskId: String, status: ExecutionStatus)


    /**
     * 获取当前任务ID
     */
    suspend fun checkPausedAndWait(taskId: String)


    /**
     * 检查当前任务是否暂停
     */
    fun checkPaused(taskId: String): Boolean

    /**
     * 检查当前任务是否正在执行
     */
    fun checkStopped(taskId: String): Boolean

    /**
     * 获取当前正在运行的任务ID
     * @return 当前任务ID，如果没有任务在运行则返回null
     */
    fun getRunningTaskIds(): List<String>?

    /**
     * 获取当前正在运行的任务ID
     * @return 当前任务ID，如果没有任务在运行则返回null
     */
    fun getTasksByState(states: List<ExecutionStatus>?): List<String>?


    /**
     * 清除任务状态
     * @param taskId 任务ID
     */
    fun clearTask(taskId: String)

    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 当前任务状态，如果任务不存在则返回null
     */
    fun getTaskState(taskId: String): ExecutionStatus


    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 当前任务状态，如果任务不存在则返回null
     */
    fun getAllTaskState(): Map<String, ExecutionStatus>


    /**
     * 设置任务状态变化监听器
     * @param listener 任务状态变化监听器
     */
    fun registerAgentStateListener(listener: IAgentStateObserver)

    /**
     * 注销任务状态变化监听器
     * @param listener 任务状态变化监听器
     */
    fun unregisterAgentStateListener(listener: IAgentStateObserver)

    fun notifyAgentExecuteStart(taskId: String)

    fun notifyAgentExecuteEnd(taskId: String, taskResult: TaskResult?)

}

interface IAgentStateObserver {
    /**
     * 任务状态变化回调
     * @param taskId 任务ID
     * @param status 新的状态
     */
    fun onAgentExecuteStart(taskId: String)

    fun onAgentExecuteEnd(taskId: String, taskResult: TaskResult?)

    fun onAgentStateChanged(taskId: String, status: ExecutionStatus)

}