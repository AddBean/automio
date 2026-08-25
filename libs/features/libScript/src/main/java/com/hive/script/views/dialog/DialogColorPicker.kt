// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.graphics.Color
import android.view.View
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptColorRecentView
import com.hive.views.widgets.colorpicker.ColorPickerView

/**
 *
 * @author jiadou
 * @date 7/15/21
 */
class DialogColorPicker(context: Context) : BaseScriptDialog(context) {
    private var mColor: Int = Color.BLACK
    var onColorPickListener: OnColorPickListener? = null
    private var btn_submit: View? = null
    private var color_picker_view: ColorPickerView? = null
    private var color_recent_view: ScriptColorRecentView? = null
    private var iv_close: View? = null
    private var view_color: View? = null
    override fun initWindow() {
        iv_close = findViewById(R.id.iv_close)
        btn_submit = findViewById(R.id.btn_submit)
        color_picker_view = findViewById(R.id.color_picker_view)
        color_recent_view = findViewById(R.id.color_recent_view)
        view_color = findViewById(R.id.view_color)

        iv_close?.setOnClickListener {
            dismiss()
        }
        color_picker_view?.color = mColor
        color_picker_view?.setOnColorChangedListener {
            mColor = it
            updateColorInfo()
        }
        btn_submit?.setOnClickListener {
            onColorPickListener?.onColorPicked(this, mColor)
        }
        color_recent_view?.setOnColorSelectedListener(object :
            ScriptColorRecentView.OnColorSelectedListener {
            override fun onSelected(color: Int) {
                mColor = color
                updateColorInfo()
            }
        })
        updateColorInfo()
    }

    private fun updateColorInfo() {
        view_color?.setBackgroundColor(mColor)
    }

    fun loadColor(color: Int): DialogColorPicker {
        mColor = color
        color_picker_view?.color = mColor
        updateColorInfo()
        return this
    }

    fun setOnColorPickListener(ls: OnColorPickListener): DialogColorPicker {
        onColorPickListener = ls
        return this
    }


    override fun enableFadeAnimation() = true

    override fun isTouchOutsideDismissed() = false

    override fun getWindowLayoutId() = R.layout.dialog_color_picker

    interface OnColorPickListener {
        fun onColorPicked(dialog: DialogColorPicker, color: Int)
    }
}