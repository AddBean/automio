// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdRunSkill
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.plugin.agent.model.SkillSpec
import com.hive.script.views.dialog.DialogSkillSelector
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import com.hive.utils.utils.StringUtils

/**
 * 运行技能命令编辑卡片：选择技能与可选用户提示。
 */
class CmdRunSkillEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdRunSkill? = null

    override fun initView() {
        findViewById<ScriptValueView>(R.id.skill_name)?.onMaskClickListener =
            View.OnClickListener { showSkillSelector() }
        findViewById<ScriptValueView>(R.id.user_prompt)?.onMaskClickListener =
            View.OnClickListener { showPromptEditor() }
    }

    private fun showSkillSelector() {
        DialogSkillSelector(context)
            .setTitle(GlobalApp.getString(com.hive.i8n.R.string.sc_run_skill_menu_title))
            .setScopeScriptPath(cmd?.getRootScript()?.scriptPath, includeGlobal = true)
            .setOnSkillSelectListener(object : DialogSkillSelector.OnSkillSelectListener {
                override fun onSelected(dialog: DialogSkillSelector, spec: SkillSpec) {
                    cmd?.skillId = spec.id
                    cmd?.skillName = StringUtils.encoding(spec.name)
                    onBindCommand(cmd!!)
                    dialog.dismiss()
                }

                override fun onDismissed() {}
            }).show()
    }

    private fun showPromptEditor() {
        DialogCommonTextInput(context)
            .setTitle(com.hive.i8n.R.string.sc_cmd_run_skill_prompt.string())
            .setHint("")
            .setText(StringUtils.decoding(cmd?.userPrompt ?: ""))
            .setSingleLine(false)
            .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                override fun onSubmitted(content: String) {
                    cmd?.userPrompt = StringUtils.encoding(content.takeIf { it.isNotBlank() })
                    cmd?.let { onBindCommand(it) }
                }

                override fun onCanceled() {}
            }).show()
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdRunSkill
        findViewById<ScriptValueView>(R.id.skill_name)?.setValue(
            GlobalApp.getString(
                com.hive.i8n.R.string.cmd_name_run_skill_des,
                StringUtils.decoding(cmd?.skillName) ?: cmd?.skillId ?: ""
            )
        )
        findViewById<ScriptValueView>(R.id.user_prompt)?.setValue(
            ScriptCommandHelper.getValueDisplayName(StringUtils.decoding(cmd?.userPrompt))
        )
    }

    override fun getEditContentId(): Int = R.layout.cmd_run_skill_card
}
