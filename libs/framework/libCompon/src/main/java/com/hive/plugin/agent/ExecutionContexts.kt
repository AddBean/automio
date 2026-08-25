// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent

/**
 * 全局 ExecutionContextStack 单例（进程级）。
 *
 * 用途：
 * - Agent/Skill 侧 push/pop 维护上下文。
 * - Script/UI 侧通过 observer 订阅，按栈顶类型决定 show/dismiss。
 */
object ExecutionContexts {
    @JvmField
    val stack: IExecutionContextStack = ExecutionContextStack()
}

