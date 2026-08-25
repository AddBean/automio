// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.setting

import android.app.AlertDialog
import android.graphics.Typeface
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.hive.agent.config.AIAgentConfig
import com.hive.app.script.R
import com.hive.base.BaseFragment
import com.hive.config.SpeechCredentialHelper
import com.hive.extension.visibleOrGone
import com.hive.framework.coper.ScriptManagerImpl
import com.hive.i8n.R as i8nR
import com.hive.net.engineer.EngineerConfig
import com.hive.script.views.dialog.DialogImageManager
import com.hive.utils.system.ClipboardUtil
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
class FragmentMoreSetting : BaseFragment() {

    private var btn_image_setting: View? = null
    private var btn_script_screenlock: View? = null
    private var btn_script_setting: View? = null
    private var btn_speech_setting: View? = null
    private var btn_agent_memory_view: View? = null
    private var btn_agent_prompt: View? = null
    private var btn_skill_prompt: View? = null

    override fun getLayoutId() = R.layout.fragment_setting_more

    override fun initView() {
        btn_image_setting = view?.findViewById(R.id.btn_image_setting)
        btn_script_screenlock = view?.findViewById(R.id.btn_script_screenlock)
        btn_script_setting = view?.findViewById(R.id.btn_script_setting)

        btn_speech_setting = view?.findViewById(R.id.btn_speech_setting)
        btn_agent_memory_view = view?.findViewById(R.id.btn_agent_memory_view)
        btn_agent_prompt = view?.findViewById(R.id.btn_agent_prompt)
        btn_skill_prompt = view?.findViewById(R.id.btn_skill_prompt)

        btn_script_screenlock?.setOnClickListener {
            ActivityUnlockSetting.start(requireContext())
        }
        btn_script_setting?.setOnClickListener {
            ScriptManagerImpl.openSetting()
        }
        btn_image_setting?.setOnClickListener {
            DialogImageManager(requireContext())
                .setEditorMode()
                .show()
        }

        btn_speech_setting?.setOnClickListener {
            showSpeechCredentialDialog()
        }
        btn_agent_memory_view?.setOnClickListener {
            com.hive.agent.views.memory.ActivityAgentMemoryView.start(requireContext())
        }

        btn_agent_prompt?.setOnClickListener {
            showPromptDialog(
                title = getString(i8nR.string.setting_agent_prompt),
                content = AIAgentConfig.PromptDefaults.getAutoSystemPrompt(supportsVision = null)
            )
        }
        btn_skill_prompt?.setOnClickListener {
            showPromptDialog(
                title = getString(i8nR.string.setting_skill_prompt),
                content = AIAgentConfig.PromptDefaults.getSkillBaseSystemPrompt(supportsVision = null)
            )
        }

        updateEngineerOnlyItems()
    }

    private fun showSpeechCredentialDialog() {
        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val gap = (8 * density).toInt()
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val etKey = EditText(requireContext()).apply {
            hint = getString(i8nR.string.setting_speech_ms_key)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(SpeechCredentialHelper.getMsSpeechKey().orEmpty())
        }
        val etRegion = EditText(requireContext()).apply {
            hint = getString(i8nR.string.setting_speech_ms_region)
            inputType = InputType.TYPE_CLASS_TEXT
            setText(SpeechCredentialHelper.getMsSpeechRegion().orEmpty())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = gap }
        }
        container.addView(etKey)
        container.addView(etRegion)

        AlertDialog.Builder(requireContext())
            .setTitle(i8nR.string.setting_speech_service)
            .setView(container)
            .setPositiveButton(i8nR.string.ai_save) { _, _ ->
                val key = etKey.text?.toString()?.trim().orEmpty()
                val region = etRegion.text?.toString()?.trim().orEmpty()
                if (key.isEmpty() || region.isEmpty()) {
                    CommonToast.getInstance().showToast(i8nR.string.setting_speech_key_required)
                    return@setPositiveButton
                }
                SpeechCredentialHelper.saveMsSpeech(key, region)
                CommonToast.getInstance().showToast(i8nR.string.setting_speech_saved)
            }
            .setNeutralButton(i8nR.string.ai_clear) { _, _ ->
                SpeechCredentialHelper.clear()
                CommonToast.getInstance().showToast(i8nR.string.setting_speech_cleared)
            }
            .setNegativeButton(i8nR.string.sc_close, null)
            .show()
    }

    private fun updateEngineerOnlyItems() {
        val show = EngineerConfig.read().engineerOn
        btn_agent_prompt?.visibleOrGone(show)
        btn_skill_prompt?.visibleOrGone(show)
    }

    private fun showPromptDialog(title: String, content: String) {
        val tv = TextView(requireContext()).apply {
            text = content
            setTextIsSelectable(true)
            typeface = Typeface.MONOSPACE
            setPadding(32, 24, 32, 24)
        }
        val scroll = ScrollView(requireContext()).apply {
            addView(tv)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(scroll)
            .setPositiveButton(i8nR.string.sc_copy) { _, _ ->
                ClipboardUtil.getInstance(requireContext()).copyText(title, content)
                CommonToast.getInstance().showToast(i8nR.string.sc_copy_success)
            }
            .setNegativeButton(i8nR.string.sc_close, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateEngineerOnlyItems()
    }
}
