// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent

/**
 * 执行上下文类型。
 *
 * - SCRIPT: 脚本命令执行触发的上下文（如 CmdRunSkill）
 * - AGENT: Agent 主任务上下文
 * - SKILL: Skill 子任务上下文（可嵌套）
 */
enum class ExecutionContextType {
    SCRIPT,
    AGENT,
    SKILL
}

/**
 * 执行上下文栈帧。
 *
 * @param id 建议全局唯一（至少在同一进程生命周期内），用于精确 pop
 * @param rootTaskId 用于绑定同一条任务链（例如独立 skill 的 nested skill 共享同一个 rootTaskId）
 * @param scopeId 脚本 scope 标识（scriptUid），用于 resolve skill/tool 时按 scope 分区查找；null 表示全局
 */
data class ExecutionContextFrame(
    val type: ExecutionContextType,
    val id: String,
    val name: String,
    val rootTaskId: String? = null,
    val startTimeMs: Long = System.currentTimeMillis(),
    val scopeId: String? = null
)

interface IExecutionContextObserver {
    fun onExecutionContextStackChanged(snapshot: List<ExecutionContextFrame>)
}

