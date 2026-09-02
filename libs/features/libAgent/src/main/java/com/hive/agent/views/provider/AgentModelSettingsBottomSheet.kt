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
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.agent.ModelInfo

/**
 * 对话页模型入口 BottomSheet：复用 [AgentAISettingsView]，风格对齐工具详情 sheet。
 */
class AgentModelSettingsBottomSheet : BottomSheetDialogFragment() {

    var onSettingsChanged: (() -> Unit)? = null

    private var settingsView: AgentAISettingsView? = null
    private var pendingSelectType: InferenceType = InferenceType.TEXT

    private val selectModelLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val model = result.data?.getSerializableExtra("data") as? ModelInfo
                settingsView?.applySelectedModel(pendingSelectType, model)
            }
            settingsView?.updateStatus()
            onSettingsChanged?.invoke()
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
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val settings = view.findViewById<AgentAISettingsView>(R.id.aiSettingsView)
        settingsView = settings
        settings.setShowServiceSettingsEntry(true)
        settings.onSelectModelClick = { type ->
            pendingSelectType = type
            selectModelLauncher.launch(
                Intent(requireContext(), ActivityAgentSelector::class.java).apply {
                    putExtra("type", type.type)
                }
            )
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

    companion object {
        private const val TAG = "AgentModelSettingsBottomSheet"

        fun show(fragmentManager: FragmentManager, onSettingsChanged: (() -> Unit)? = null) {
            (fragmentManager.findFragmentByTag(TAG) as? AgentModelSettingsBottomSheet)?.dismissAllowingStateLoss()
            AgentModelSettingsBottomSheet().apply {
                this.onSettingsChanged = onSettingsChanged
            }.show(fragmentManager, TAG)
        }
    }
}
