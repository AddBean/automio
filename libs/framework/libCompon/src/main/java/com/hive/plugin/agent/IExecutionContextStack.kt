// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent

/**
 * 统一的执行上下文栈，用于追踪 script/agent/skill 的调用链。
 *
 * 约定：
 * - push/pop 必须成对调用（建议 try/finally）。
 * - pop(expectedId) 用于在异常/并发场景下做防御（不匹配则不 pop）。
 */
interface IExecutionContextStack {

    fun push(frame: ExecutionContextFrame)

    fun pop(expectedId: String? = null): ExecutionContextFrame?

    fun peek(): ExecutionContextFrame?

    fun snapshot(): List<ExecutionContextFrame>

    fun depth(): Int

    fun registerObserver(observer: IExecutionContextObserver)

    fun unregisterObserver(observer: IExecutionContextObserver)
}

