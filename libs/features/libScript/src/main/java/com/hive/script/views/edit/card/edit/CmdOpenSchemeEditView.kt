// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.text.TextUtils
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdOpenUrl
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.extends.string

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class CmdOpenSchemeEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdOpenUrl? = null

    override fun initView() {
        findViewById<ScriptValueView>(R.id.edit_scheme)?.onValueChangedListener =
            object : ScriptValueView.OnValueChangedListener {
                override fun onValueChanged(value: String) {
                    value.takeIf { TextUtils.isEmpty(value) }.run {
                        if (cmd?.targetScheme != value) {
                            cmd?.targetScheme = value
                        }
                    }
                }
            }
        findViewById<ScriptValueView>(R.id.edit_scheme)?.onMaskClickListener =
            object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    DialogCommonTextInput(context).setSingleLine(true).setTitle(
                        com.hive.i8n.R.string.sc_open_scheme_edit_text_input_title.string()
                    ).setHint(com.hive.i8n.R.string.sc_open_scheme_edit_text_input_hint.string())
                        .setText(cmd?.targetScheme ?: "")
                        .setSingleLine(false)
                        .setCheckFunction {
                            if (TextUtils.isEmpty(it)) {
                                throw Exception(com.hive.i8n.R.string.sc_open_scheme_edit_text_input_error_0.string())
                            }
                            if (it?.contains("://") == false) {
                                throw Exception(com.hive.i8n.R.string.sc_open_scheme_edit_text_input_error_1.string())
                            }
                        }
                        .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                            override fun onSubmitted(content: String) {
                                cmd?.targetScheme = content
                                cmd?.let { onBindCommand(it) }
                            }

                            override fun onCanceled() {
                            }
                        }).show()
                }

            }
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdOpenUrl
        findViewById<ScriptValueView>(R.id.edit_scheme)?.setValue(cmd?.targetScheme ?: "")
    }


    override fun getEditContentId() = R.layout.cmd_open_scheme_card

}