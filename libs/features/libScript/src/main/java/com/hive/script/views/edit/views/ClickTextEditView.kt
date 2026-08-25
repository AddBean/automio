// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.cmd.CmdClickText
import com.hive.script.views.beans.PointVectorFloat
import com.hive.script.views.dialog.DialogCommonSelector
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.extends.string
import com.hive.utils.extends.stringArray
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @date 7/4/21
 */
class ClickTextEditView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    private lateinit var cmd: CmdClickText


    private var edit_direct: ScriptValueView? = null

    private var find_selector: ScriptTabSelectorView? = null

    private var target_text: ScriptValueView? = null

    private var operate_edit_view: OperateCommonEditView? = null

    override fun initView(view: View?) {

        find_selector = findViewById(R.id.find_selector)
        target_text = findViewById(R.id.target_text)
        edit_direct = findViewById(R.id.edit_direct)
        operate_edit_view = findViewById(R.id.operate_edit_view)

        find_selector?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd.findType = p!!.second!!
                }
            }
        target_text?.onMaskClickListener = OnClickListener {
            DialogCommonTextInput(context)
                .setText(cmd.targetText)
                .setSingleLine(true)
                .setOnCommonListener(object :
                    DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        cmd.targetText = content
                        onBindCommand(cmd)
                    }

                    override fun onCanceled() {
                    }
                }).show()
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
                        cmd.findDirection = pair.first
                        cmd.let { onBindCommand(it) }
                        dialog.dismiss()
                    }

                    override fun onCancel() {
                    }

                }).show()
        }
    }

    fun checkCommandOrThrowError() {
        if (cmd.action == ScriptClickActionHelper.ACTION_DRAG) {
            if (cmd.dragVector.fromX == 0f && cmd.dragVector.fromY == 0f &&
                cmd.dragVector.toX == 0f && cmd.dragVector.toY == 0f
            ) {
                throw Exception(com.hive.i8n.R.string.sc_drag_not_set_error.string())
            }
        }
    }

    fun onBindCommand(data: CmdClickText) {
        cmd = data
        target_text?.setValue(cmd.targetText)
        if (cmd.targetText.isEmpty()) {
            target_text?.setValue(com.hive.i8n.R.string.cmd_text_default_text.string())
        }
        find_selector?.setValue(cmd.findType)
        edit_direct?.setValue(
            com.hive.i8n.R.array.sc_cmd_direct_menu_array.stringArray()[cmd.findDirection]
        )

        cmd.let { cmd ->
            operate_edit_view?.bindData(OperateCommonEditView.OperateData().apply {
                pressDuration = cmd.pressDuration
                fastCount = cmd.fastCount
                fastGap = cmd.fastGap
                random = cmd.random
                action = cmd.action
                dragDuration = cmd.dragDuration
                dragPressDuration = cmd.dragPressDuration
                dragType = cmd.dragType
                dragData = cmd.dragVector
            }, object : OperateCommonEditView.OnOperateListener {

                override fun onOperateChangedData(data: OperateCommonEditView.OperateData) {
                    cmd.dragDuration = data.dragDuration
                    cmd.dragType = data.dragType
                    cmd.dragVector = data.dragData ?: PointVectorFloat()
                    cmd.dragPressDuration = data.dragPressDuration
                    cmd.pressDuration = data.pressDuration
                    cmd.fastCount = data.fastCount
                    cmd.fastGap = data.fastGap
                    cmd.random = data.random
                    cmd.action = data.action
                }
            })
        }
    }


    override fun getLayoutId() = R.layout.spot_text_edit_view

}