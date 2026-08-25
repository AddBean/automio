// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.cmd.CmdInput
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import com.hive.utils.utils.StringUtils
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class CmdInputEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdInput? = null
    private var editId: ScriptValueView? = null
    private var editTargetIndex: ScriptValueView? = null
    private var edit_content: ScriptSpanParamLayout? = null
    private var type_selector: ScriptTabSelectorView? = null
    private var switch_anim_input: ScriptTabSelectorView? = null


    override fun initView() {
        editId = findViewById(R.id.editId)
        editTargetIndex = findViewById(R.id.editTargetIndex)
        edit_content = findViewById(R.id.edit_content)
        type_selector = findViewById(R.id.type_selector)
        switch_anim_input = findViewById(R.id.switch_anim_input)
        editId?.onMaskClickListener = View.OnClickListener {
            DialogCommonTextInput(context)
                .setSingleLine(true)
                .setTitle(
                    com.hive.i8n.R.string.sc_curl_body_click_id_input_title.string()
                ).setHint(com.hive.i8n.R.string.sc_curl_body_click_id_input_hint.string())
                .setText(cmd?.targetId ?: "")
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        cmd?.targetId = content
                        cmd?.let {
                            onBindCommand(it)
                        }
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }
        editTargetIndex?.onMaskClickListener = View.OnClickListener {
            DialogCommonTextInput(context)
                .setSingleLine(true)
                .setTitle(com.hive.i8n.R.string.sc_input_target_index_title.string())
                .setHint(com.hive.i8n.R.string.sc_input_target_index_hint.string())
                .setText((cmd?.targetIndex ?: 1).toString())
                .setCheckFunction { content ->
                    val value = content?.toIntOrNull()
                    require(value != null && value > 0) {
                        GlobalApp.getString(com.hive.i8n.R.string.tool_input_error_target_index_invalid)
                    }
                }
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        cmd?.targetIndex = content.toIntOrNull()?.takeIf { it > 0 } ?: 1
                        cmd?.let {
                            onBindCommand(it)
                        }
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }
        edit_content?.addTextChangedListener(object : ScriptSpanParamLayout.ScriptTextWatcher {
            override fun afterTextChanged(s: String?) {
                if (s == null || s == "") {
                    cmd?.content = ""
                } else {
                    cmd?.content = StringUtils.encoding(s.toString())
                }
            }
        })
        type_selector?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {

                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd?.action = p?.second ?: "full"
                }
            }
        switch_anim_input?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd?.animInput = (p?.second == "1")
                }
            }
        editId?.setActionEnable(!ScriptInterpreter.getDefault().isRecording())
        editId?.onActionClickListener = View.OnClickListener {
            ScriptInsertManager.startSelectViewId(true) {
                cmd?.targetId = it ?: ""
                cmd?.let {
                    onBindCommand(it)
                }
            }
        }
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdInput
        editId?.setValue(cmd?.targetId ?: "")
        editTargetIndex?.setValue((cmd?.targetIndex ?: 1).toString())
        type_selector?.setValue(cmd?.action ?: "full")
        edit_content?.setText(StringUtils.decoding(cmd?.content ?: ""))
        switch_anim_input?.setValue(if (cmd?.animInput == true) "1" else "0")
    }


    override fun getEditContentId() = R.layout.cmd_input_card

}
