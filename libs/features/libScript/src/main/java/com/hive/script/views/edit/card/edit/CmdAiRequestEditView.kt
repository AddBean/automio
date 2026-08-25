// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import android.widget.TextView
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst.AIPromptActions
import com.hive.script.base.params.ScriptParam
import com.hive.script.cmd.CmdAiRequest
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.extends.string

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdAiRequestEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdAiRequest? = null

    private var editError: ScriptValueView? = null
    private var editParamId: ScriptValueView? = null
    private var editPrompt: ScriptValueView? = null
    private var tvMessage: TextView? = null

    override fun initView() {
        editError = findViewById(R.id.editError)
        editParamId = findViewById(R.id.editParamId)
        editPrompt = findViewById(R.id.editPrompt)
        tvMessage = findViewById(R.id.tvMessage)

        editPrompt?.onMaskClickListener = OnClickListener {
            DialogCommonTextInput(context)
                .setTitle(com.hive.i8n.R.string.sc_curl_ai_prompt_edit_text_input_title.string())
                .setHint(com.hive.i8n.R.string.sc_curl_ai_prompt_edit_text_input_hint.string())
                .setText(cmd?.prompt ?: "")
                .setSingleLine(false)
                .setQuickAction(com.hive.i8n.R.string.cmd_ai_request_quick_name.string(), AIPromptActions)
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        cmd?.prompt = content
                        cmd?.let { onBindCommand(it) }
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }

        editError?.onMaskClickListener = OnClickListener {
            DialogCommonTextInput(context).setSingleLine(true).setTitle(
                com.hive.i8n.R.string.sc_curl_ai_error_edit_text_input_title.string()
            ).setHint(com.hive.i8n.R.string.sc_curl_ai_error_edit_text_input_hint.string())
                .setText(cmd?.failureMsg ?: "")
                .setSingleLine(false)
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        cmd?.failureMsg = content
                        cmd?.let { onBindCommand(it) }
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }


        editParamId?.onMaskClickListener = OnClickListener {
            DialogParamsManager(context)
                .setWritable(true)
                .setParamListener(object :
                    DialogParamsManager.OnParamListener {
                    override fun onParamSelected(param: ScriptParam?) {
                        cmd?.targetParamId = param?.getFullId() ?: ""
                        cmd?.let { onBindCommand(it) }
                    }
                }).show()

        }

        tvMessage?.visibility = View.GONE
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdAiRequest
        editPrompt?.setValue(ScriptCommandHelper.getValueDisplayName(cmd?.prompt))
        editError?.setValue(cmd?.failureMsg ?: "")
        editParamId?.setValue(ScriptCommandHelper.paramFormat.format(cmd?.targetParamId ?: ""))
    }


    override fun getEditContentId() = R.layout.cmd_read_view_ai_request

}
