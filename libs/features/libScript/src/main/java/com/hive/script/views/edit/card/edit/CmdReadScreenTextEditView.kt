// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.params.ScriptParam
import com.hive.script.cmd.CmdReadScreenText
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.script.views.widgets.ScriptValueView

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class CmdReadScreenTextEditView(context: Context) : BaseCommandEditCard(context),
    View.OnClickListener {

    var cmd: CmdReadScreenText? = null

    private var target_param: ScriptValueView? = null

    override fun initView() {
        target_param = findViewById(R.id.target_param)
        target_param?.onValueChangedListener = object : ScriptValueView.OnValueChangedListener {

            override fun onValueChanged(text: String) {
                //@{main.param6},去除@{%s}格式,只保留main.param6
                cmd?.targetParamId = ScriptCommandHelper.parseParamsId(text)
            }
        }

        target_param?.onMaskClickListener = OnClickListener {
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
    }

    override fun expandCommonEdit() = true

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdReadScreenText
        target_param?.setValue(ScriptCommandHelper.paramFormat.format(cmd?.targetParamId ?: ""))
    }


    override fun getEditContentId() = R.layout.cmd_read_screen_text_card

}