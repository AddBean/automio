// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.agent

import android.view.View
import androidx.fragment.app.FragmentContainerView
import com.hive.agent.views.AgentChatFragment
import com.hive.app.script.R
import com.hive.base.BaseFragment
import com.hive.event.AgentEvent
import com.hive.event.AgentEventType
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.IAgentStateObserver
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.TaskResult
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.views.event.ScriptMenuEvent
import com.hive.utils.GlobalApp
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * 智能体主页面
 * 仅展示 AI 对话（能力管理已迁移至工作流 Tab）
 */
class AgentMainFragment : BaseFragment(), IAgentStateObserver {

    private var chatFragment: AgentChatFragment? = null

    override fun getLayoutId(): Int = R.layout.fragment_agent_main

    override fun initView() {
        // 注册 EventBus
        EventBus.getDefault().register(this)

        val container = mView?.findViewById<FragmentContainerView>(R.id.chat_container)
        chatFragment = AgentChatFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.chat_container, chatFragment!!)
            .commitNow()

        // 注册 Agent 状态观察者
        val agentProvider = ComponentManager.getInstance()
            .getProvider(IAgentProvider::class.java) as? IAgentProvider
        agentProvider?.registerAgentStateObserver(this)

        chatFragment?.setupAgentObservers()
    }

    // ========== IAgentStateObserver 实现 ==========

    override fun onAgentExecuteStart(taskId: String) {
        // Agent 任务启动时保持在聊天 Tab
    }

    override fun onAgentExecuteEnd(taskId: String, taskResult: TaskResult?) {
        // 任务结束时保持在聊天界面，让用户查看结果
    }

    override fun onAgentStateChanged(taskId: String, status: ExecutionStatus) {
        // 状态变化处理（可选）
    }

    // ========== EventBus 事件处理 ==========

    @Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    fun onAgentEvent(event: AgentEvent) {
        when (event.type) {
            AgentEventType.AGENT_SERVICE_MCP_REGISTERED -> {
                val agentProvider = ComponentManager.getInstance()
                    .getProvider(IAgentProvider::class.java) as? IAgentProvider
                agentProvider?.registerAgentStateObserver(this)
                chatFragment?.setupAgentObservers()
            }

            AgentEventType.AGENT_CHAT_FRAGMENT_READY -> {
                // 聊天 Fragment 已准备就绪
            }

            AgentEventType.AGENT_NEW_TASK_VIEW -> {
                // 新任务视图请求
            }

            else -> {}
        }
    }

    @Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    fun onScriptMenuEvent(@Suppress("UNUSED_PARAMETER") event: ScriptMenuEvent) {
        // 脚本菜单事件（可选处理）
    }

    // ==========生命周期 ==========

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    fun startToolbarNewTask() {
        chatFragment?.startNewConversation()
    }

    fun showToolbarHistory() {
        chatFragment?.showSessionHistory()
    }

    fun refreshToolbarActions() {
        chatFragment?.syncToolbarActions()
    }
}
