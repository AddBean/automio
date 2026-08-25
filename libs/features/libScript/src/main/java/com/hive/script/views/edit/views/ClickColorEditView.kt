// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.cmd.CmdClickColor
import com.hive.script.utils.ScriptColorHelper
import com.hive.script.views.beans.PointVectorFloat
import com.hive.script.views.dialog.DialogColorPicker
import com.hive.script.views.widgets.ScriptNumberView
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.extends.string
import com.hive.views.widgets.NumberOptView
import com.hive.views.widgets.SelectorTabView
import java.math.BigInteger

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/4/21
 */
class ClickColorEditView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    private lateinit var cmd: CmdClickColor
    private var find_selector: ScriptTabSelectorView? = null
    private var number_color: ScriptValueView? = null
    private var number_threshold: ScriptNumberView? = null
    private var operate_edit_view: OperateCommonEditView? = null

    override fun initView(view: View?) {
        find_selector = findViewById(R.id.find_selector)
        number_color = findViewById(R.id.number_color)
        number_threshold = findViewById(R.id.number_threshold)
        operate_edit_view = findViewById(R.id.operate_edit_view)

        find_selector?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd.findType = p!!.second!!
                }
            }

        number_color?.onValueChangedListener = object : ScriptValueView.OnValueChangedListener {

            override fun onValueChanged(hex: String) {
                val bi = BigInteger(hex, 16)
                cmd.targetColor = bi.toInt()
            }
        }

        number_threshold?.changedListener = object : NumberOptView.OnValueChangedListener {
            override fun onValueChanged(value: Int) {
                cmd.threshold = value
            }
        }
        number_color?.onMaskClickListener = OnClickListener {
            DialogColorPicker(context).loadColor(cmd.targetColor)
                .setOnColorPickListener(object : DialogColorPicker.OnColorPickListener {
                    override fun onColorPicked(dialog: DialogColorPicker, color: Int) {
                        dialog.dismiss()
                        ScriptColorHelper.addColorToFirst(color)
                        cmd.targetColor = color
                        loadCmdSpot(cmd)
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

    fun loadCmdSpot(data: CmdClickColor) {
        cmd = data
        number_color?.setTextBackgroundColor(cmd.targetColor)
        number_color?.setValue(Integer.toHexString(cmd.targetColor))
        number_threshold?.setNumber((cmd.threshold))
        find_selector?.setValue(cmd.findType)
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


    override fun getLayoutId() = R.layout.spot_color_edit_view

}