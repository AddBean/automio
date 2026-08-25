// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent.model

import android.text.TextUtils
import com.hive.plugin.agent.IAgentContext
import java.io.Serializable

/**
 * 工作流目标定义
 */
data class AgentTaskGoal(
    val id: String,
    val userInput: String,
    var userInputOptimized: String = "",
    val requiredCapabilities: List<String> = emptyList(),
    val priority: TaskPriority = TaskPriority.NORMAL,
    val deadline: Long? = null,
    val constraints: Map<String, Any> = emptyMap(),
    val metadata: Map<String, Any> = emptyMap(),
    var input: AgentInput? = null
) : Serializable {

    fun updateNormal(agentContext: IAgentContext, input: AgentInput) {
        this.input = input.copy()
        agentContext.notifyTaskMessageUpdated(this)
    }

    fun updateStream(agentContext: IAgentContext, input: AgentInput) {
        this.input = input.copy()
        agentContext.notifyTaskMessageStreamUpdated(this)
    }
}

/**
 * 工作流优先级
 */
enum class TaskPriority(val level: Int) {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4)
}

/**
 * 工作流执行结果
 */
data class TaskResult private constructor(
    val taskId: String,
    val status: ExecutionStatus?,
    val data: Any? = null,
    val error: AgentError? = null,
    val msg: String? = null,
    val startTime: Long,
    val endTime: Long = System.currentTimeMillis(),
) : Serializable {

    /**
     * 获取信息
     */
    val message: String?
        get() = if (TextUtils.isEmpty(msg)) {
            error?.getInfo()
        } else {
            msg
        }

    /**
     * 获取执行时长
     */
    fun getDuration(): Long = endTime - startTime


    /**
     * 是否成功
     */
    fun isSuccess(): Boolean = status == ExecutionStatus.SUCCESS


    companion object {
        fun success(
            taskId: String,
            data: Any? = null,
            startTime: Long = System.currentTimeMillis()
        ): TaskResult {
            return TaskResult(
                taskId = taskId,
                data = data,
                startTime = startTime,
                msg = "",
                status = ExecutionStatus.SUCCESS
            )
        }

        fun failure(
            taskId: String,
            error: AgentError,
            data: Any? = null,
            startTime: Long = System.currentTimeMillis()
        ): TaskResult {
            return TaskResult(
                taskId = taskId,
                data = data,
                error = error,
                startTime = startTime,
                status = ExecutionStatus.FAILED
            )
        }

        fun stopped(
            taskId: String,
            message: String,
            startTime: Long = System.currentTimeMillis()
        ): TaskResult {
            return TaskResult(
                taskId = taskId,
                startTime = startTime,
                msg = message,
                status = ExecutionStatus.STOPPED
            )
        }

        fun timeout(
            taskId: String,
            message: String,
            startTime: Long = System.currentTimeMillis()
        ): TaskResult {
            return TaskResult(
                taskId = taskId,
                startTime = startTime,
                msg = message,
                status = ExecutionStatus.TIMEOUT
            )
        }

        fun unknown(
            taskId: String,
            message: String,
            startTime: Long = System.currentTimeMillis()
        ): TaskResult {
            return TaskResult(
                taskId = taskId,
                startTime = startTime,
                msg = message,
                status = ExecutionStatus.UNKNOWN
            )
        }
    }
}