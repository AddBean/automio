// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View.OnClickListener
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.params.ScriptParam
import com.hive.script.cmd.CmdReadViewText
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.views.dialog.DialogCommonSelector
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.extends.string
import com.hive.utils.extends.stringArray
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdReadViewTextEditView(context: Context) : BaseCommandEditCard(context),
    OnClickListener {

    var cmd: CmdReadViewText? = null

    private var editId: ScriptValueView? = null
    private var editParamIs: ScriptValueView? = null
    private var editScope: ScriptTabSelectorView? = null
    private var editSelector: ScriptTabSelectorView? = null
    private var edit_direct: ScriptValueView? = null

    override fun initView() {
        editId = findViewById(R.id.editId)
        editParamIs = findViewById(R.id.editParamIs)
        editScope = findViewById(R.id.editScope)
        editSelector = findViewById(R.id.editSelector)
        edit_direct = findViewById(R.id.edit_direct)


        editId?.onMaskClickListener = OnClickListener {
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

        editParamIs?.onMaskClickListener = OnClickListener {
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
        editSelector?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd?.readType = CmdReadViewText.ReadType.of(p?.second ?: "TEXT")
                }
            }

        editScope?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd?.readScope = CmdReadViewText.ReadScope.of(p?.second ?: "SINGLE")
                }
            }
        edit_direct?.onMaskClickListener = OnClickListener {

            DialogCommonSelector(context).setTitle(com.hive.i8n.R.string.sc_cmd_direct_menu_title.string())
                .setDataSet(
                    com.hive.i8n.R.array.sc_cmd_direct_menu_array.stringArray().mapIndexed { index, s ->
                        index to s
                    }.toMutableList()
                ).setSelectListener(object : DialogCommonSelector.OnSelectListener {
                    override fun onSelected(
                        dialog: DialogCommonSelector, pos: Int, pair: Pair<Int, String>
                    ) {
                        cmd?.findDirection = pair.first
                        cmd?.let { onBindCommand(it) }
                        dialog.dismiss()
                    }

                    override fun onCancel() {
                    }

                }).show()
        }
        editId?.setActionEnable(!ScriptInterpreter.getDefault().isRecording())
        editId?.onActionClickListener = OnClickListener {
            ScriptInsertManager.startSelectViewId(false) {
                cmd?.targetId = it ?: ""
                cmd?.let {
                    onBindCommand(it)
                }
            }
        }
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdReadViewText
        editId?.setValue(cmd?.targetId ?: "")
        editSelector?.setValue(cmd?.readType?.value ?: "")
        editScope?.setValue(cmd?.readScope?.value ?: "")
        editParamIs?.setValue(ScriptCommandHelper.paramFormat.format(cmd?.targetParamId ?: ""))
        edit_direct?.setValue(
            com.hive.i8n.R.array.sc_cmd_direct_menu_array.stringArray()[cmd?.findDirection ?: 0]
        )
    }


    override fun getEditContentId() = R.layout.cmd_read_view_text_card

}