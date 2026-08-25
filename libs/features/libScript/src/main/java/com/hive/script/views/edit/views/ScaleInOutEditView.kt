// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdPinchZoom
import com.hive.script.views.widgets.ScriptFloatView
import com.hive.script.views.widgets.ScriptScaleInOutView
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.views.widgets.FloatOptView
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class ScaleInOutEditView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    var cmd: CmdPinchZoom? = null

    private var number_duration: ScriptFloatView? = null
    private var scale_view: ScriptScaleInOutView? = null
    private var type_selector: ScriptTabSelectorView? = null

    override fun initView(view: View?) {
        number_duration = findViewById(R.id.number_duration)
        scale_view = findViewById(R.id.scale_view)
        type_selector = findViewById(R.id.type_selector)
        type_selector?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {

                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd?.action = p?.second ?: CmdPinchZoom.ACTION_SCALE_OUT
                    cmd?.run {
                        bindCommand(this)
                    }
                }
            }
        number_duration?.changedListener = object : FloatOptView.OnValueChangedListener {
            override fun onValueChanged(value: Float) {
                cmd?.duration = value.toLong()
            }
        }
    }

    fun bindCommand(command: ScriptCommand) {
        cmd = command as CmdPinchZoom
        type_selector?.setValue(cmd?.action ?: CmdPinchZoom.ACTION_SCALE_OUT)
        number_duration?.setNumber(cmd?.duration?.toFloat() ?: 1f)
        scale_view?.enableTouch = false
        cmd?.run {
            scale_view?.loadCmd2(this, 200 * DP, 150 * DP)
        }
    }

    override fun getLayoutId() = R.layout.spot_scale_in_out_edit

}