// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent

import com.hive.plugin.agent.model.SkillResult

/**
 * Skill 执行状态观察者
 * 与 IAgentStateObserver 分离，skill 与 agent 互不影响
 */
interface ISkillStateObserver {
    fun onSkillExecuteStart(taskId: String)
    fun onSkillExecuteEnd(taskId: String, result: SkillResult?)
}