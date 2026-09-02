// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hive.agent.R
import com.hive.agent.XAgent
import com.hive.agent.ai.DefaultAIServiceManager
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.agent.ModelInfo
import com.hive.views.widgets.CommonToast

/**
 * 对话页模型入口 BottomSheet：复用 [AgentAISettingsView]，风格对齐工具详情 sheet。
 */
class AgentModelSettingsBottomSheet : BottomSheetDialogFragment() {

    var onSettingsChanged: (() -> Unit)? = null

    private var settingsView: AgentAISettingsView? = null

    private val selectTextModelLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleModelResult(InferenceType.TEXT, result.resultCode, result.data)
        }

    private val selectImageModelLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleModelResult(InferenceType.IMAGE, result.resultCode, result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AgentBottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_agent_model_settings, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.navigationBarColor =
            requireContext().getColor(com.hive.i8n.R.color.design_bg_overlay)
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindSettingsView(view.findViewById(R.id.aiSettingsView))
    }

    private fun bindSettingsView(settings: AgentAISettingsView) {
        settingsView = settings
        settings.setShowServiceSettingsEntry(true)
        settings.onSelectModelClick = { type ->
            val intent = Intent(requireContext(), ActivityAgentSelector::class.java).apply {
                putExtra("type", type.type)
            }
            when (type) {
                InferenceType.IMAGE -> selectImageModelLauncher.launch(intent)
                else -> selectTextModelLauncher.launch(intent)
            }
        }
        settings.onOpenServiceSettingsClick = {
            dismissAllowingStateLoss()
            ActivityAgentSetting.start(requireContext())
        }
        settings.onSettingsChanged = {
            onSettingsChanged?.invoke()
        }
        settings.updateStatus()
    }

    private fun handleModelResult(type: InferenceType, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val model = data?.getSerializableExtra("data") as? ModelInfo
            val applied = settingsView?.applySelectedModel(type, model) == true
            if (!applied && model != null) {
                // View 可能已销毁，直接落盘到 Manager，避免选模结果丢失
                commitModelToManager(type, model)
            }
        }
        settingsView?.updateStatus()
        onSettingsChanged?.invoke()
    }

    private fun commitModelToManager(type: InferenceType, model: ModelInfo) {
        val manager = XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager
        val provider = manager?.getProvider(model.providerId)
        if (provider?.isProviderReady() == true) {
            manager.setInferenceModel(type, model)
        } else {
            CommonToast.getInstance().showToast(
                getString(com.hive.i8n.R.string.ai_set_api_key_first, model.providerId)
            )
        }
    }

    companion object {
        private const val TAG = "AgentModelSettingsBottomSheet"

        fun show(fragmentManager: FragmentManager, onSettingsChanged: (() -> Unit)? = null) {
            val existing = fragmentManager.findFragmentByTag(TAG) as? AgentModelSettingsBottomSheet
            if (existing != null) {
                existing.onSettingsChanged = onSettingsChanged
                existing.settingsView?.updateStatus()
                return
            }
            AgentModelSettingsBottomSheet().apply {
                this.onSettingsChanged = onSettingsChanged
            }.show(fragmentManager, TAG)
        }
    }
}
