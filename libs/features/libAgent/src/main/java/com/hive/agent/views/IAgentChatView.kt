// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views

import android.view.View
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.AgentTaskGoal

interface IAgentChatView {

    fun updateMessages(goal: AgentTaskGoal)

    fun updateTaskStatus(taskId: String, status: ExecutionStatus)

    fun getChatView(): View
}