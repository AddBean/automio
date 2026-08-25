// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.hive.script.R
import com.hive.views.widgets.FloatOptView
import androidx.core.content.withStyledAttributes

/**
 *
 * @author jiadou
 * @date 6/21/21
 */

class ScriptFloatView(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs) {

    private var type: Int = 0

    val view = LayoutInflater.from(context).inflate(R.layout.script_float_view, this)

    var name = ""

    var scriptFloatRatio = 1f

    var scriptFloatDigits = 1

    var scriptFloatEnable = false

    var changedListener: FloatOptView.OnValueChangedListener? = null
        set(value) {
            field = value
            float_view.onValueChangedListener =
                FloatOptView.OnValueChangedListener { v -> field?.onValueChanged(v * scriptFloatRatio) }
        }

    private var float_view: FloatOptView = view.findViewById(R.id.float_view)
    private var tv_title: TextView? = null

    init {
        initAttrs1(attrs)
        initAttrs2(attrs)
        float_view= view.findViewById(R.id.float_view)
        tv_title = view.findViewById(R.id.tv_title)
        float_view.maxDigits = scriptFloatDigits
        updateUi()
    }

    fun setViewEnabled(enable: Boolean) {
        float_view.isEnabled = enable
    }

    private fun initAttrs1(attrs: AttributeSet?) {
        attrs?.run {
            context.withStyledAttributes(
                attrs,
                R.styleable.ScriptFloatView
            ) {
                val count = indexCount
                for (i in 0 until count) {
                    val attr = getIndex(i)
                    if (attr == R.styleable.ScriptFloatView_scriptFloatName) {
                        name = getString(attr).toString()
                    }
                    if (attr == R.styleable.ScriptFloatView_scriptFloatEnable) {
                        scriptFloatEnable = getBoolean(attr, false)
                    }
                    if (attr == R.styleable.ScriptFloatView_scriptFloatMaxDigits) {
                        scriptFloatDigits = getInteger(attr, 1)
                    }
                    if (attr == R.styleable.ScriptFloatView_scriptFloatRatio) {
                        scriptFloatRatio = if (scriptFloatEnable) {
                            getFloat(attr, 1f)
                        } else {
                            1f
                        }
                    }
                }
            }
        }
    }

    private fun initAttrs2(attrs: AttributeSet?) {
        attrs?.run {
            float_view.initAttrs(attrs)
        }
    }

    fun setMaxNumber(number: Float) {
        float_view?.maxValue = number / scriptFloatRatio
    }

    fun setMinNumber(number: Float) {
        float_view?.minValue = number / scriptFloatRatio
    }


    fun setNumber(number: Float) {
        float_view.setValue(number / scriptFloatRatio)
    }


    fun getNumber(): Float = float_view?.floatValue ?: 0f

    fun getNumberView() = float_view

    fun updateUi() {
        tv_title?.text = name
        float_view.setEditEnable(float_view.inputEnable)
        post {
            float_view?.floatValue?.run {
                float_view?.floatValue = this
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        float_view.isEnabled = enabled
        float_view.inputEnable = enabled
        float_view.setEditEnable(float_view.inputEnable)
    }
}