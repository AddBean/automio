// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.carlos.ui.ext.shakeBottomLeftAndRight
import com.hive.views.R

/**
 *
 * @author jiadou
 * @date 5/6/21
 */
class FloatOptView(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs),
    View.OnClickListener {

    var targetColor = Color.BLACK
    private var iv_minus: View? = null
    private var iv_plus: View? = null
    private var tv_unit: TextView? = null
    private var tv_value: TextView? = null
    var floatValue: Float = 0f
        set(value) {
            field = value
            onValueChangedListener?.onValueChanged(field)
            if (field <= minValue) {
                iv_minus?.alpha = 0.4f
                iv_minus?.isEnabled = false
            } else {
                iv_minus?.alpha = 1f
                iv_minus?.isEnabled = true
            }
            if (field >= maxValue) {
                iv_plus?.alpha = 0.4f
                iv_plus?.isEnabled = false
            } else {
                iv_plus?.alpha = 1f
                iv_plus?.isEnabled = true
            }
        }
    var maxValue = Int.MAX_VALUE.toFloat()
    var minValue = Int.MIN_VALUE.toFloat()
    var onValueChangedListener: OnValueChangedListener? = null
    var view: View = LayoutInflater.from(context).inflate(R.layout.float_opt_view, this)
    var stepSize = 0.1f
    var numberType = 0
    var numberUnit = ""
    var inputEnable: Boolean = true
    var maxDigits = 1

    fun setViewEnabled(enabled: Boolean) {
        setEditEnable(enabled)
        inputEnable = enabled
    }

    init {
        initAttrs(attrs)
        updateNumberType()
        iv_minus = view.findViewById(R.id.iv_minus)
        iv_plus = view.findViewById(R.id.iv_plus)
        tv_unit = view.findViewById(R.id.tv_unit)
        tv_value = view.findViewById(R.id.tv_value)
        iv_minus?.setOnClickListener(this)
        iv_plus?.setOnClickListener(this)
        tv_value?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                setTextValue(floatValue)
            }
        }
        tv_value?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.toString()?.toFloatOrNull()?.let {
                    if (!checkLegel(it) && floatValue != it) {
                        tv_value?.shakeBottomLeftAndRight()
//                        setTextValueColor(Color.RED)
                    } else {
                        tv_value?.clearAnimation()
                        floatValue = it
                    }
                }
            }
        })

    }

    fun updateUiStatus() {
        tv_unit?.text = numberUnit
        setTextValue(floatValue)
        setEditEnable(inputEnable)
    }


    fun setEditEnable(enable: Boolean) {
        tv_value?.isEnabled = enable
    }

    private fun getColor(resId: Int): Int {
        return context.resources.getColor(resId, null)
    }

    fun updateNumberType() {
        if (numberType == 1) {
            iv_plus?.visibility = View.VISIBLE
            iv_minus?.visibility = View.VISIBLE
        } else if (numberType == 2) {
            iv_plus?.visibility = View.GONE
            iv_minus?.visibility = View.GONE
        }
    }

    fun initAttrs(attrs: AttributeSet?) {
        attrs?.run {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.FloatOptView
            )
            val count = a.indexCount
            for (i in 0 until count) {
                val attr = a.getIndex(i)
                when (attr) {
                    R.styleable.FloatOptView_optFloatMax -> {
                        maxValue = a.getFloat(attr, Int.MAX_VALUE.toFloat())
                    }

                    R.styleable.FloatOptView_optFloatMin -> {
                        minValue = a.getFloat(attr, 0f)
                    }

                    R.styleable.FloatOptView_optFloatDefault -> {
                        floatValue = a.getFloat(attr, 1f)
                    }

                    R.styleable.FloatOptView_optFloatInput -> {
                        inputEnable = a.getBoolean(attr, true)
                    }

                    R.styleable.FloatOptView_optFloatMaxDigits -> {
                        maxDigits = a.getInteger(attr, 1)
                    }

                    R.styleable.FloatOptView_optFloatStep -> {
                        stepSize = a.getFloat(attr, 1f)
                    }

                    R.styleable.FloatOptView_optFloatType -> {
                        numberType = a.getInt(attr, 1)
                    }

                    R.styleable.FloatOptView_optFloatUnit -> {
                        numberUnit = a.getString(attr) ?: ""
                    }

                }
            }
            a.recycle()
        }
        updateUiStatus()
    }

    fun setUnitText(unit: String?) {
        unit?.run {
            numberUnit = unit
            tv_unit?.text = numberUnit
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_minus -> {
                if (isEnabled) {
                    minusValue()
                }
            }

            R.id.iv_plus -> {
                if (isEnabled) {
                    addValue()
                }
            }

            R.id.tv_value -> {
//                InputDialog(context).show()
            }
        }
    }

    fun addValue() {
        if (!checkLegel(floatValue + stepSize)) return
        floatValue += stepSize
        setTextValue(floatValue)
    }

    fun minusValue() {
        if (!checkLegel(floatValue - stepSize)) return
        floatValue -= stepSize
        setTextValue(floatValue)
    }

    private fun checkLegel(v: Float): Boolean {
        if (v > maxValue) return false
        if (v < minValue) return false
        return true
    }

    fun setValue(value: Float) {
        if (!checkLegel(value)) return
        this.floatValue = value
        setTextValue(this.floatValue)
    }


    fun setTextValue(value: Float) {
        tv_value?.setText(String.format("%.${maxDigits}f", floatValue))
    }

    fun interface OnValueChangedListener {
        fun onValueChanged(value: Float)
    }


}