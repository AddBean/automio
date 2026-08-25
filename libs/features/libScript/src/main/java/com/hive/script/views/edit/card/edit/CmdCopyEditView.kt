// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdCopyToClipboard
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.utils.utils.StringUtils

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdCopyEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdCopyToClipboard? = null

    override fun initView() {
        findViewById<ScriptSpanParamLayout>(R.id.edit_content)?.addTextChangedListener(object :
            ScriptSpanParamLayout.ScriptTextWatcher {
            override fun afterTextChanged(s: String?) {
                if (s == null || s == "") {
                    cmd?.content = ScriptConst.NONE_CHAR
                } else {
                    cmd?.content = StringUtils.encoding(s.toString())
                }
            }

        })
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdCopyToClipboard
        findViewById<ScriptSpanParamLayout>(R.id.edit_content)?.setText(
            StringUtils.decoding(
                cmd?.content ?: ""
            )
        )
    }


    override fun getEditContentId() = R.layout.cmd_copy_card

}