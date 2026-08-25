// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.utils.extends.dp
import com.hive.views.widgets.RoundLinearLayout

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/21/21
 */

class ScriptValueView(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private var btn_mask: View? = null

    private var iv_action: ImageView? = null

    private var rl_value: RoundLinearLayout? = null

    private var tv_value: ScriptSpanParamTextView? = null

    private var tv_title: TextView? = null

    var onValueChangedListener: OnValueChangedListener? = null

    var onMaskClickListener: OnClickListener? = null
        set(value) {
            field = value
            btn_mask?.setOnClickListener(value)
        }

    var onActionClickListener: OnClickListener? = null
        set(value) {
            field = value
            iv_action?.setOnClickListener(value)
        }

    val view = LayoutInflater.from(context).inflate(R.layout.script_value_view, this).apply {
        btn_mask = findViewById(R.id.btn_mask)
        iv_action = findViewById(R.id.iv_action)
        rl_value = findViewById(R.id.rl_value)
        tv_value = findViewById(R.id.tv_value)
        tv_title = findViewById(R.id.tv_title)
    }

    init {
        initAttrs(attrs)
        tv_value?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                post {
                    onValueChangedListener?.onValueChanged(s.toString())
                }
            }
        })
    }

    private fun initAttrs(attrs: AttributeSet?) {
        attrs?.run {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.ScriptValueView
            )

            val count = a.indexCount
            for (i in 0 until count) {
                val attr = a.getIndex(i)
                if (attr == R.styleable.ScriptValueView_scriptValueName) {
                    setName(a.getString(attr).toString())
                } else if (attr == R.styleable.ScriptValueView_scriptValueEditEnable) {
                    setEditEnable(a.getBoolean(attr, false))
                } else if (attr == R.styleable.ScriptValueView_scriptValueTextColor) {
                    setTextColor(a.getColor(attr, Color.BLACK))
                } else if (attr == R.styleable.ScriptValueView_scriptValueEditWidth) {
                    var width = a.getDimension(attr, -1f)
                    if (width > 0) {
                        setEditWidth(width)
                    }
                } else if (attr == R.styleable.ScriptValueView_scriptActionEnable) {
                    setActionEnable(a.getBoolean(attr, false))
                }

            }
            a.recycle()
        }
    }

    fun setValue(value: String): ScriptValueView {
        tv_value?.setSpanText(value)
        return this
    }

    fun setTextColor(color: Int): ScriptValueView {
        tv_value?.setTextColor(color)
        return this
    }

    fun setActionEnable(enable: Boolean): ScriptValueView {
        iv_action?.visibility = if (enable) VISIBLE else GONE
        return this
    }

    fun setEditEnable(enable: Boolean): ScriptValueView {
        tv_value?.isEnabled = enable
        return this
    }

    fun setEditWidth(width: Float): ScriptValueView {
        val lp = rl_value?.layoutParams
        lp?.width = width.toInt()
        rl_value?.layoutParams = lp
        return this
    }

    fun setName(name: String): ScriptValueView {
        tv_title?.text = name
        return this
    }

    fun setInputFullWidth(full: Boolean): ScriptValueView {
        val lp = rl_value?.layoutParams
        lp?.width = if (full) LayoutParams.MATCH_PARENT else 80.dp
        tv_title?.visibleOrGone(!full)
        rl_value?.layoutParams = lp
        return this
    }


    fun getTextView(): ScriptSpanParamTextView {
        return tv_value!!
    }

    fun setTextBackgroundColor(color: Int): ScriptValueView {
        tv_value?.setBackgroundColor(color)
        return this
    }

    interface OnValueChangedListener {
        fun onValueChanged(value: String)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        tv_value?.isEnabled = enabled
    }

}