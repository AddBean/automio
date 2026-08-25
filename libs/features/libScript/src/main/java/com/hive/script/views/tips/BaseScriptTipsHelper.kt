// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.tips

import android.annotation.SuppressLint
import android.text.TextUtils
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.model.AgentInput
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.TaskResult
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.ScriptProvider
import com.hive.script.views.dialog.DialogAgentTextInput
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.widgets.BaseScriptTips
import com.hive.utils.GlobalApp
import com.hive.utils.system.ClipboardUtil
import com.hive.views.widgets.CommonToast

@SuppressLint("StaticFieldLeak")
object BaseScriptTipsHelper {
    private val context = GlobalApp.getContext()

    fun showUnlockTips() {
        BaseScriptTips(ScriptProvider.getViewContext()).setTitleText(context.getString(com.hive.i8n.R.string.script_tips_unlock_title))
            .setMsgText(context.getString(com.hive.i8n.R.string.script_tips_unlock_msg))
            .setCancelClickListener {
                it.dismiss()
            }
            .setSubmitClickListener {
                it.dismiss()
            }
            .startDismissTimer(4000).show()
    }

    fun showUnlockTestSuccess() {
        BaseScriptTips(ScriptProvider.getViewContext()).setTitleText(context.getString(com.hive.i8n.R.string.script_tips_unlock_success_title))
            .setMsgText(context.getString(com.hive.i8n.R.string.script_tips_unlock_success))
            .setCancelClickListener {
                it.dismiss()
            }
            .setSubmitClickListener {
                it.dismiss()
            }
            .startDismissTimer(3000).show()
    }

    fun showAgentFinishTip(
        currentAgentGoal: AgentTaskGoal?,
        taskResult: TaskResult?
    ) {
        var msg = context.getString(com.hive.i8n.R.string.script_tips_execution_completed)
        var chatInput: AgentInput? = null
        if (currentAgentGoal?.input is AgentInput) {
            chatInput = (currentAgentGoal.input as AgentInput)
            msg = chatInput.messages.lastOrNull { it.role == MessageRole.ASSISTANT }?.content
                ?: msg
        }

        if (taskResult != null && !taskResult.isSuccess()) {
            msg = taskResult.message
                ?: context.getString(com.hive.i8n.R.string.script_tips_execution_error)
        }

        val hasContinueTask = currentAgentGoal != null
        BaseScriptTips(ScriptProvider.getViewContext()).setTitleText(context.getString(com.hive.i8n.R.string.script_tips_task_completed))
            .setMsgText(msg)
            .setCancelText(context.getString(com.hive.i8n.R.string.script_tips_i_know))
            .setSubmitText(
                if (hasContinueTask) context.getString(com.hive.i8n.R.string.script_tips_continue_task)
                else context.getString(com.hive.i8n.R.string.script_tips_i_know)
            )
            .setActionButton(context.getString(com.hive.i8n.R.string.sc_copy), { v, content ->
                if (!TextUtils.isEmpty(content)) {
                    ClipboardUtil.getInstance(context).copyText("script_tips", content!!)
                    CommonToast.show(com.hive.i8n.R.string.sc_copy_success)
                }
            })
            .setCancelClickListener {
                it.dismiss()
            }
            .setSubmitClickListener {
                it.dismiss()
                if (!hasContinueTask) return@setSubmitClickListener
                DialogAgentTextInput(ScriptProvider.getViewContext())
                    .setTitle(
                        context.getString(com.hive.i8n.R.string.script_tips_continue_task)
                    ).setHint(context.getString(com.hive.i8n.R.string.script_provider_task_prompt))
                    .setOnCommonListener(object : DialogAgentTextInput.OnCommonListener {
                        override fun onSubmitted(content: String) {
                            val agentProvider = ComponentManager.getInstance()
                                .getProvider(IAgentProvider::class.java) as IAgentProvider
                            chatInput?.run {
                                val lastMessages = mutableListOf<ChatMessage>()
                                lastMessages.addAll(messages)
                                lastMessages.add(ChatMessage(MessageRole.USER, content))
                                chatInput.messages = lastMessages
                                agentProvider.executeAgentTask(currentAgentGoal, null)
                            }
                        }

                        override fun onCanceled() {
                        }
                    })
                    .show()
            }
            .startDismissTimer(30 * 1000).show()
    }
}