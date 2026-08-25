// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import android.text.TextUtils
import android.view.Gravity
import android.view.View
import com.hive.agent.R
import com.hive.agent.views.provider.ActivityAgentSetting
import com.hive.anim.AnimUtils
import com.hive.utils.extends.visibleOrGone
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.ScriptProvider
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.script.views.manager.ScriptManager
import com.hive.utils.GlobalApp


object AgentCheckHelper {

    fun checkAgentEnv(): Boolean {
        return isAccessServiceReady() && isModelAvailable()
    }

    private fun isAccessServiceReady(): Boolean {
        // Avoid IScriptProvider from ComponentManager; it may be null during early resume.
        return try {
            ScriptManager.checkServerEnable()
        } catch (_: Throwable) {
            ScriptProvider.isServiceReady()
        }
    }

    private fun isModelAvailable(): Boolean {
        val agentProvider = ComponentManager.getInstance()
            .getProvider(IAgentProvider::class.java) as? IAgentProvider ?: return false
        val manager = agentProvider.getAIServiceManager() ?: return false
        val selectedModel = manager.getInferenceModel(InferenceType.TEXT) ?: return false
        if (TextUtils.isEmpty(selectedModel.displayName)) return false

        val provider = manager.getProvider(selectedModel.providerId)
        if (provider == null ||
            !manager.isProviderEnabled(selectedModel.providerId) ||
            !provider.isProviderReady()
        ) {
            // Cached model exists but provider is not really ready (e.g. missing API key).
            manager.setInferenceModel(InferenceType.TEXT, null)
            return false
        }
        return true
    }

    fun showAgentEnvDialog() {
        val context = try {
            ScriptProvider.getViewContext()
        } catch (_: Throwable) {
            null
        } ?: GlobalApp.getContext() ?: return

        DialogScriptAlert(context)
            .setTitle(GlobalApp.getString(com.hive.i8n.R.string.agent_env_config_required))
            .setContent(
                GlobalApp.getString(
                    com.hive.i8n.R.string.agent_env_config_required_content
                )
            )
            .setContentLayout(R.layout.agent_dialog_check_content)
            .setContentGravity(Gravity.START)
            .setConfirmEnable(false)
            .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                    dialog.dismiss()
                }
            }).show().run {
                val tvBtnModel = this.findViewById<View>(R.id.tv_btn_model)
                val tvBtnAccess = this.findViewById<View>(R.id.tv_btn_access)
                tvBtnModel?.visibleOrGone(isModelAvailable().not())
                tvBtnAccess?.visibleOrGone(isAccessServiceReady().not())
                tvBtnModel?.setOnClickListener {
                    AnimUtils.animatePress(this) {
                        this.dismiss()
                        ActivityAgentSetting.start(context)
                    }
                }
                tvBtnAccess?.setOnClickListener {
                    AnimUtils.animatePress(this) {
                        this.dismiss()
                        ScriptProvider.startToAccessibilitySetting()
                    }
                }
            }
    }
}
