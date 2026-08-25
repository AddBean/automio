// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views

import com.hive.agent.XAgent
import com.hive.plugin.agent.ErrorContext
import com.hive.plugin.agent.IAgentStateObserver
import com.hive.plugin.agent.IAgentTaskObserver
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.TaskResult
import com.hive.utils.thread.UIHandlerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AgentChatTaskBridge(
    private val xAgent: XAgent,
) {

    interface Callbacks {
        fun lifecycleScope(): CoroutineScope
        fun onTaskMessagesUpdated(goal: AgentTaskGoal)
        fun onTaskMessagesStreamUpdated(goal: AgentTaskGoal)
        fun onTaskStatusChanged(taskId: String, status: ExecutionStatus)
        fun onTaskMemoryCompressing(isCompressing: Boolean)
        fun onTaskError(error: AgentError, context: ErrorContext)
        fun onPrepareFreshSessionForExternalTask()
        fun onRequestSessionSave(goal: AgentTaskGoal? = null, delayMs: Long)
    }

    var currentTaskId: String? = null
        private set

    fun syncCurrentTaskId(taskId: String?) {
        currentTaskId = taskId
    }

    private var pendingChatLaunchTaskId: String? = null
    private var lastStartedTaskId: String? = null
    private var taskObserver: IAgentTaskObserver? = null
    private var stateObserver: IAgentStateObserver? = null
    private var streamRenderJob: Job? = null
    private var pendingStreamGoal: AgentTaskGoal? = null

    fun markPendingChatLaunch(taskId: String) {
        pendingChatLaunchTaskId = taskId
    }

    fun register(callbacks: Callbacks) {
        unregister()

        taskObserver = object : IAgentTaskObserver {
            override fun onTaskInfoUpdated(message: String) = Unit

            override fun onTaskMessageUpdated(goal: AgentTaskGoal) {
                UIHandlerUtils.getInstance().executeInMainThread {
                    callbacks.onTaskMessagesUpdated(goal)
                    callbacks.onRequestSessionSave(goal, SAVE_DEBOUNCE_MS)
                }
            }

            override fun onTaskMessageStreamUpdated(goal: AgentTaskGoal) {
                UIHandlerUtils.getInstance().executeInMainThread {
                    scheduleStreamRender(goal, callbacks)
                    callbacks.onRequestSessionSave(goal, SAVE_DEBOUNCE_MS)
                }
            }

            override fun onMemoryCompressing(taskId: String, isCompressing: Boolean) {
                UIHandlerUtils.getInstance().executeInMainThread {
                    if (taskId == currentTaskId) {
                        callbacks.onTaskMemoryCompressing(isCompressing)
                    }
                }
            }

            override fun onTaskError(taskId: String, error: AgentError, context: ErrorContext) {
                UIHandlerUtils.getInstance().executeInMainThread {
                    if (taskId == currentTaskId) {
                        callbacks.onTaskError(error, context)
                    }
                }
            }
        }

        stateObserver = object : IAgentStateObserver {
            override fun onAgentStateChanged(taskId: String, status: ExecutionStatus) {
                UIHandlerUtils.getInstance().executeInMainThread {
                    if (isTerminalStatus(status) && currentTaskId == taskId) {
                        currentTaskId = null
                    }
                    callbacks.onTaskStatusChanged(taskId, status)
                }
            }

            override fun onAgentExecuteStart(taskId: String) {
                val launchedFromChatInput = pendingChatLaunchTaskId == taskId
                val isContinueAction = !launchedFromChatInput && lastStartedTaskId == taskId
                if (!launchedFromChatInput && !isContinueAction) {
                    callbacks.onPrepareFreshSessionForExternalTask()
                }
                currentTaskId = taskId
                pendingChatLaunchTaskId = null
                lastStartedTaskId = taskId
            }

            override fun onAgentExecuteEnd(taskId: String, taskResult: TaskResult?) {
                UIHandlerUtils.getInstance().executeInMainThread {
                    if (taskId == currentTaskId) {
                        xAgent.getCurrentAgentGoal()?.let { callbacks.onRequestSessionSave(it, 0) }
                    }
                }
            }
        }

        xAgent.registerAgentTaskObserver(taskObserver!!)
        xAgent.registerAgentStateObserver(stateObserver!!)
    }

    fun unregister() {
        taskObserver?.let { xAgent.unregisterAgentTaskObserver(it) }
        stateObserver?.let { xAgent.unregisterAgentStateObserver(it) }
        taskObserver = null
        stateObserver = null
        streamRenderJob?.cancel()
        streamRenderJob = null
        pendingStreamGoal = null
    }

    fun refreshCurrentTask(callbacks: Callbacks) {
        xAgent.getCurrentAgentGoal()?.let { goal ->
            callbacks.onTaskMessagesUpdated(goal)
            callbacks.onRequestSessionSave(goal, 0)
        }
    }

    private fun scheduleStreamRender(goal: AgentTaskGoal, callbacks: Callbacks) {
        pendingStreamGoal = goal
        if (streamRenderJob?.isActive == true) return
        streamRenderJob = callbacks.lifecycleScope().launch {
            delay(STREAM_RENDER_DEBOUNCE_MS)
            streamRenderJob = null
            val latest = pendingStreamGoal
            pendingStreamGoal = null
            if (latest != null) {
                callbacks.onTaskMessagesStreamUpdated(latest)
            }
        }
    }

    private companion object {
        private const val SAVE_DEBOUNCE_MS = 400L
        private const val STREAM_RENDER_DEBOUNCE_MS = 66L

        private fun isTerminalStatus(status: ExecutionStatus): Boolean {
            return status == ExecutionStatus.SUCCESS ||
                status == ExecutionStatus.FAILED ||
                status == ExecutionStatus.STOPPED ||
                status == ExecutionStatus.TIMEOUT
        }
    }
}
