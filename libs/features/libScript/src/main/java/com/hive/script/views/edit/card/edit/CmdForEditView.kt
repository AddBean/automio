// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdFor
import com.hive.script.views.edit.editor.ListScriptEditView
import com.hive.script.views.widgets.ScriptNumberView
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.views.widgets.NumberOptView
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class CmdForEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdFor? = null

    private var edit_view: ListScriptEditView? = null
    private var number_for: ScriptNumberView? = null
    private var play_type: ScriptTabSelectorView? = null
    override fun initView() {
        edit_view = findViewById(R.id.edit_view)
        number_for = findViewById(R.id.number_for)
        play_type = findViewById(R.id.play_type)
        number_for?.changedListener =
            NumberOptView.OnValueChangedListener { value -> cmd?.loopCount = value }
        play_type?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd?.loopCount = if (((p?.second ?: "0") == "1")) 0 else 1
                    updateNumberEnable()
                    bindCommand(cmd!!)
                }
            }
    }

    private fun updateNumberEnable() {
        number_for?.isEnabled = (cmd?.loopCount ?: 1) > 0
        number_for?.alpha = if ((cmd?.loopCount ?: 1) > 0) 1f else 0.4f
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdFor
        number_for?.setNumber(cmd?.loopCount ?: 1)
        play_type?.setValue(if (cmd?.loopCount == 0) "1" else "0")
        cmd?.run {
            edit_view?.submitData(this)
        }
        updateNumberEnable()
    }

    override fun getEditContentId() = R.layout.cmd_for_card

}