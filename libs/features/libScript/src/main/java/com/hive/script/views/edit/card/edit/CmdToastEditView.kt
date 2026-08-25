// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdToast
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.utils.utils.StringUtils

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdToastEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdToast? = null
    private var edit_content: ScriptSpanParamLayout? = null

    override fun initView() {
        edit_content = findViewById(R.id.edit_content)
        edit_content?.addTextChangedListener(object : ScriptSpanParamLayout.ScriptTextWatcher {
            override fun afterTextChanged(s: String?) {
                if (s == null || s == "") {
                    cmd?.content = ""
                } else {
                    cmd?.content = StringUtils.encoding(s.toString())
                }
            }
        })
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdToast
        edit_content?.setText(StringUtils.decoding(cmd?.content ?: ""))
    }


    override fun getEditContentId() = R.layout.cmd_toast_card

}