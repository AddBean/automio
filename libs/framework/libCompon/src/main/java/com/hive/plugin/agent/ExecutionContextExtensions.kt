// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent

/**
 * 从执行上下文栈中取最近 SCRIPT 帧的 scopeId。
 * 用于 skill/tool 的 scope 感知查找。
 */
fun IExecutionContextStack.currentScopeId(): String? =
    snapshot().asReversed().firstOrNull { it.type == ExecutionContextType.SCRIPT }?.scopeId
