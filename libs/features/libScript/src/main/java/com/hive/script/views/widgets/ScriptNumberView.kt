// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.hive.script.R
import com.hive.views.widgets.NumberOptView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/21/21
 */

class ScriptNumberView(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs) {

    private var type: Int = 0

    private var number_view: NumberOptView? = null

    private var tv_title: TextView? = null

    val view = LayoutInflater.from(context).inflate(R.layout.script_number_view, this).apply {
        number_view = findViewById(R.id.number_view)
        tv_title = findViewById(R.id.tv_title)
    }

    var name = ""

    var changedListener: NumberOptView.OnValueChangedListener? = null
        set(value) {
            field = value
            number_view?.onValueChangedListener = value
        }

    init {
        initAttrs1(attrs)
        initAttrs2(attrs)
        updateUi()
    }

    fun setViewEnabled(enable: Boolean) {
        number_view?.isEnabled = enable
    }

    private fun initAttrs1(attrs: AttributeSet?) {
        attrs?.run {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.ScriptNumberView
            )
            val count = a.indexCount
            for (i in 0 until count) {
                val attr = a.getIndex(i)
                if (attr == R.styleable.ScriptNumberView_scriptNumberName) {
                    name = a.getString(attr).toString()
                }
            }
            a.recycle()
        }
    }

    private fun initAttrs2(attrs: AttributeSet?) {
        attrs?.run {
            number_view?.initAttrs(attrs)
        }
    }

    fun setMaxNumber(number: Int) {
        number_view?.maxValue = number
    }

    fun setMinNumber(number: Int) {
        number_view?.minValue = number
    }


    fun setNumber(number: Int) {
        number_view?.setValue(number)
    }

    fun getNumberView() = number_view

    fun updateUi() {
        tv_title?.text = name
        number_view?.setEditEnable(number_view?.inputEnable == true)
        post {
            number_view?.number?.run {
                number_view?.number = this
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        number_view?.isEnabled = enabled
        number_view?.inputEnable = enabled
        number_view?.setEditEnable(number_view?.inputEnable == true)
    }
}