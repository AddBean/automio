// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.cmd.CmdClickView
import com.hive.script.views.beans.PointVectorFloat
import com.hive.script.views.dialog.DialogCommonSelector
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.extends.string
import com.hive.utils.extends.stringArray

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/6/21
 */
class ClickViewEditView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    private lateinit var cmd: CmdClickView

    private var edit_direct: ScriptValueView? = null
    private var edit_id: ScriptValueView? = null
    private var edit_tag: ScriptValueView? = null
    private var edit_text: ScriptValueView? = null
    private var operate_edit_view: OperateCommonEditView? = null

    override fun initView(view: View?) {
        edit_direct = findViewById(R.id.edit_direct)
        edit_id = findViewById(R.id.edit_id)
        edit_tag = findViewById(R.id.edit_tag)
        edit_text = findViewById(R.id.edit_text)
        operate_edit_view = findViewById(R.id.operate_edit_view)
        edit_text?.onMaskClickListener = OnClickListener {
            DialogCommonTextInput(context)
                .setSingleLine(false)
                .setEnableInputEmpty(true)
                .setTitle(
                    com.hive.i8n.R.string.sc_curl_body_click_text_input_title.string()
                ).setHint(com.hive.i8n.R.string.sc_curl_body_click_text_input_hint.string())
                .setText(cmd.targetText)
                .setSingleLine(false)
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        cmd.targetText = content
                        bindData(cmd)
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }

        edit_id?.onMaskClickListener = OnClickListener {
            DialogCommonTextInput(context)
                .setSingleLine(true)
                .setEnableInputEmpty(true)
                .setTitle(
                    com.hive.i8n.R.string.sc_curl_body_click_id_input_title.string()
                ).setHint(com.hive.i8n.R.string.sc_curl_body_click_id_input_hint.string())
                .setText(cmd.targetId)
                .setSingleLine(false)
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        cmd.targetId = content
                        bindData(cmd)
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }


        edit_tag?.onMaskClickListener = OnClickListener {
            DialogCommonTextInput(context)
                .setSingleLine(true)
                .setEnableInputEmpty(true)
                .setTitle(
                    com.hive.i8n.R.string.sc_curl_body_click_tag_input_title.string()
                ).setHint(com.hive.i8n.R.string.sc_curl_body_click_tag_input_hint.string())
                .setText(cmd.targetTag)
                .setSingleLine(false)
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        cmd.targetTag = content
                        bindData(cmd)
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
                        bindData(cmd)
                        dialog.dismiss()
                    }

                    override fun onCancel() {
                    }

                }).show()
        }
        edit_text?.onValueChangedListener = object : ScriptValueView.OnValueChangedListener {
            override fun onValueChanged(value: String) {
                cmd.targetText = maskValue(value)
            }
        }
        edit_id?.onValueChangedListener = object : ScriptValueView.OnValueChangedListener {
            override fun onValueChanged(value: String) {
                cmd.targetId = maskValue(value)
            }
        }
        edit_tag?.onValueChangedListener = object : ScriptValueView.OnValueChangedListener {
            override fun onValueChanged(value: String) {
                cmd.targetTag = maskValue(value)
            }
        }
        edit_id?.setActionEnable(!ScriptInterpreter.getDefault().isRecording())
        edit_id?.onActionClickListener = OnClickListener {
            ScriptInsertManager.startSelectViewId(false) {
                cmd.targetId = it ?: ""
                bindData(cmd)
            }
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

    private fun maskValue(v: String) = if (v == "") ScriptConst.NONE_CHAR else v

    fun bindData(data: CmdClickView) {
        cmd = data
        edit_text?.setValue(cmd.targetText)
        edit_id?.setValue(cmd.targetId)
        edit_tag?.setValue(cmd.targetTag)
        edit_direct?.setValue(com.hive.i8n.R.array.sc_cmd_direct_menu_array.stringArray()[cmd.findDirection])
        operate_edit_view?.bindData(OperateCommonEditView.OperateData().apply {
            pressDuration = cmd.pressDuration
            fastCount = cmd.fastCount
            fastGap = cmd.fastGap
            random = cmd.random
            action = cmd.action
            dragPressDuration = cmd.dragPressDuration
            dragDuration = cmd.dragDuration
            dragType = cmd.dragType
            dragData = cmd.dragVector
        }, object : OperateCommonEditView.OnOperateListener {

            override fun onOperateChangedData(data: OperateCommonEditView.OperateData) {
                cmd.dragPressDuration = data.dragPressDuration
                cmd.dragDuration = data.dragDuration
                cmd.dragType = data.dragType
                cmd.dragVector = data.dragData ?: PointVectorFloat()
                cmd.pressDuration = data.pressDuration
                cmd.fastCount = data.fastCount
                cmd.fastGap = data.fastGap
                cmd.random = data.random
                cmd.action = data.action
            }
        })
    }


    override fun getLayoutId() = R.layout.spot_layout_edit_view
}