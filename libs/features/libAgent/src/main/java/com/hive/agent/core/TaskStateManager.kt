// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.core

import com.hive.plugin.agent.IAgentStateObserver
import com.hive.plugin.agent.ITaskStateManager
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.TaskResult
import kotlinx.coroutines.delay

/**
 * 任务状态管理器
 * 负责管理Agent任务的执行状态，包括互斥执行、暂停、恢复、停止等功能
 *
 */
class TaskStateManager : ITaskStateManager {
    companion object {
        private const val TAG = "TaskStateManager"
    }

    // 存储任务状态的Map
    private val taskStates = mutableMapOf<String, ExecutionStatus>()

    // 状态变化监听器
    private var stateChangeObservers: List<IAgentStateObserver> =
        mutableListOf()

    override fun startTask(taskId: String): Boolean {
        // 如果已有任务在运行，则先停止，并等待停止成功
        getTasksByState(
            mutableListOf(
                ExecutionStatus.PAUSED,
                ExecutionStatus.RUNNING
            )
        ).forEach {
            if (it != taskId) {
                stopTask(it)
            }
        }
        // 设置任务状态为运行中
        taskStates[taskId] = ExecutionStatus.RUNNING
        // 触发状态变化回调
        notifyAgentStateChanged(taskId, ExecutionStatus.RUNNING)
        return true
    }

    override fun pauseTask(taskId: String): Boolean {
        val currentState = taskStates[taskId] ?: return false

        // 只有正在运行的任务才能暂停
        if (currentState == ExecutionStatus.RUNNING) {
            taskStates[taskId] = ExecutionStatus.PAUSED
            // 触发状态变化回调
            notifyAgentStateChanged(taskId, ExecutionStatus.PAUSED)
            return true
        }
        return false
    }

    override fun resumeTask(taskId: String): Boolean {
        val currentState = taskStates[taskId] ?: return false

        // 只有已暂停的任务才能恢复
        if (currentState == ExecutionStatus.PAUSED) {
            taskStates[taskId] = ExecutionStatus.RUNNING
            // 触发状态变化回调
            notifyAgentStateChanged(taskId, ExecutionStatus.RUNNING)
            return true
        }
        return false
    }

    override fun stopTask(taskId: String): Boolean {
        // 任何状态的任务都可以停止
        taskStates[taskId] = ExecutionStatus.STOPPED
        // 触发状态变化回调
        notifyAgentStateChanged(taskId, ExecutionStatus.STOPPED)
        return true
    }

    override fun getCurrentState(taskId: String): ExecutionStatus {
        return taskStates[taskId] ?: ExecutionStatus.UNKNOWN
    }

    override fun setCurrentState(taskId: String, status: ExecutionStatus) {
        val oldStatus = taskStates[taskId]
        taskStates[taskId] = status
        // 如果状态发生变化，触发回调
        if (oldStatus != status) {
            notifyAgentStateChanged(taskId, status)
        }
    }

    override suspend fun checkPausedAndWait(taskId: String) {
        // 检查任务是否处于暂停状态
        val state = taskStates[taskId] ?: return
        if (state == ExecutionStatus.PAUSED) {
            // 如果任务处于暂停状态，等待恢复
            while (taskStates[taskId] == ExecutionStatus.PAUSED) {
                delay(50)
            }
        }
    }

    override fun checkPaused(taskId: String): Boolean {
        val state = taskStates[taskId] ?: return true
        return state == ExecutionStatus.PAUSED
    }

    override fun checkStopped(taskId: String): Boolean {
        val state = taskStates[taskId] ?: return true
        return state == ExecutionStatus.STOPPED || state == ExecutionStatus.SUCCESS || state == ExecutionStatus.FAILED || state == ExecutionStatus.TIMEOUT
    }

    /**
     * 获取当前正在运行的任务ID
     */
    override fun getRunningTaskIds(): List<String> {
        return taskStates.entries.filter { it.value == ExecutionStatus.RUNNING }
            .map { it.key }
    }

    override fun getTasksByState(states: List<ExecutionStatus>?): List<String> {
        return taskStates.filter { states?.contains(it.value) ?: true }
            .map { it.key }
    }

    /**
     * 清理任务状态
     */
    override fun clearTask(taskId: String) {
        taskStates.remove(taskId)
    }

    /**
     * 获取当前任务状态
     */
    override fun getTaskState(taskId: String): ExecutionStatus {
        return taskStates[taskId] ?: ExecutionStatus.UNKNOWN
    }

    /**
     * 获取所有任务状态
     */
    override fun getAllTaskState(): Map<String, ExecutionStatus> {
        return taskStates.toMap()
    }

    override fun registerAgentStateListener(listener: IAgentStateObserver) {
        if (!stateChangeObservers.contains(listener)) {
            this.stateChangeObservers += listener
        }
    }

    override fun unregisterAgentStateListener(listener: IAgentStateObserver) {
        this.stateChangeObservers = stateChangeObservers.filter { it != listener }
    }

    override fun notifyAgentExecuteStart(taskId: String) {
        stateChangeObservers.forEach { it.onAgentExecuteStart(taskId) }
    }

    override fun notifyAgentExecuteEnd(taskId: String, taskResult: TaskResult?) {
        stateChangeObservers.forEach { it.onAgentExecuteEnd(taskId, taskResult) }
    }


    /**
     * 通知状态变化
     * @param taskId 任务ID
     * @param status 新状态
     */
    private fun notifyAgentStateChanged(taskId: String, status: ExecutionStatus) {
        stateChangeObservers.forEach { it.onAgentStateChanged(taskId, status) }
    }

    /**
     * 设置任务为成功状态
     * @param taskId 任务ID
     */
    fun setTaskSuccess(taskId: String) {
        taskStates[taskId] = ExecutionStatus.SUCCESS
        notifyAgentStateChanged(taskId, ExecutionStatus.SUCCESS)
    }

    /**
     * 设置任务为失败状态
     * @param taskId 任务ID
     */
    fun setTaskFailed(taskId: String) {
        taskStates[taskId] = ExecutionStatus.FAILED
        notifyAgentStateChanged(taskId, ExecutionStatus.FAILED)
    }

    /**
     * 设置任务为超时状态
     * @param taskId 任务ID
     */
    fun setTaskTimeout(taskId: String) {
        taskStates[taskId] = ExecutionStatus.TIMEOUT
        notifyAgentStateChanged(taskId, ExecutionStatus.TIMEOUT)
    }
} 