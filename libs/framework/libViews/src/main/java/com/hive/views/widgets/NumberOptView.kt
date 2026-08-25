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
import com.hive.utils.utils.StringUtils
import com.hive.views.R

/**
 *
 * @author jiadou
 * @date 5/6/21
 */
class NumberOptView(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs),
    View.OnClickListener {

    var targetColor = Color.BLACK

private var iv_minus:View?=null
private var iv_plus:View?=null
private var tv_unit:TextView?=null
private var tv_value:TextView?=null

    var number: Int = 0
        set(value) {
            field = value
            onValueChangedListener?.onValueChanged(field)
            if (field == minValue) {
                iv_minus?.alpha = 0.4f
                iv_minus?.isEnabled = false
            } else {
                iv_minus?.alpha = 1f
                iv_minus?.isEnabled = true
            }
            if (field == maxValue) {
                iv_plus?.alpha = 0.4f
                iv_plus?.isEnabled = false
            } else {
                iv_plus?.alpha = 1f
                iv_plus?.isEnabled = true
            }
        }
    var maxValue = Int.MAX_VALUE
    var minValue = Int.MIN_VALUE
    var onValueChangedListener: OnValueChangedListener? = null
    var view: View = LayoutInflater.from(context).inflate(R.layout.number_opt_view, this)
    var stepSize = 1
    var numberType = 0
    var numberUnit = ""
    var inputEnable: Boolean = true

    fun setViewEnabled(enabled: Boolean) {
        setEditEnable(enabled)
        inputEnable = enabled
    }

    init {
        initAttrs(attrs)
        iv_minus = view.findViewById(R.id.iv_minus)
        iv_plus = view.findViewById(R.id.iv_plus)
        tv_unit = view.findViewById(R.id.tv_unit)
        tv_value = view.findViewById(R.id.tv_value)
        updateNumberType()
        iv_minus?.setOnClickListener(this)
        iv_plus?.setOnClickListener(this)
        tv_value?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                tv_value?.setText("$number")
            }
        }
        tv_value?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.toString()?.takeIf { StringUtils.isNumber(it) }?.toIntOrNull()?.let {
                    if (!checkLegel(it) && number != it) {
                        tv_value?.shakeBottomLeftAndRight()
//                        tv_value?.setTextColor(Color.RED)
                    } else {
                        number = it
                    }
                }
            }
        })

    }

    fun updateUiStatus() {
        tv_unit?.text = numberUnit
        tv_value?.setText("$number")
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
                R.styleable.NumberOptView
            )
            val count = a.indexCount
            for (i in 0 until count) {
                val attr = a.getIndex(i)
                when (attr) {
                    R.styleable.NumberOptView_optNumberMax -> {
                        maxValue = a.getInt(attr, Int.MAX_VALUE)
                    }

                    R.styleable.NumberOptView_optNumberMin -> {
                        minValue = a.getInt(attr, 0)
                    }

                    R.styleable.NumberOptView_optNumberDefault -> {
                        number = a.getInt(attr, 1)
                    }

                    R.styleable.NumberOptView_optNumberInput -> {
                        inputEnable = a.getBoolean(attr, true)
                    }

                    R.styleable.NumberOptView_optNumberStep -> {
                        stepSize = a.getInt(attr, 1)
                    }

                    R.styleable.NumberOptView_optNumberType -> {
                        numberType = a.getInt(attr, 1)
                    }

                    R.styleable.NumberOptView_optNumberUnit -> {
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
        if (!checkLegel(number + stepSize)) return
        number += stepSize
        tv_value?.setText(number.toString())
    }

    fun minusValue() {
        if (!checkLegel(number - stepSize)) return
        number -= stepSize
        tv_value?.setText(number.toString())
    }

    private fun checkLegel(v: Int): Boolean {
        if (v > maxValue) return false
        if (v < minValue) return false
        return true
    }

    fun setValue(value: Int) {
        if (!checkLegel(value)) return
        this.number = value
        tv_value?.setText(this.number.toString())
    }


    fun interface OnValueChangedListener {
        fun onValueChanged(value: Int)
    }


}