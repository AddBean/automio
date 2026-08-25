// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.hive.script.R

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/1/21
 */
class ScriptRandomSizeView(context: Context?, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {

    private var type: Int = 0

    private var seek_view: ScriptSizeSeekbarView? = null

    private var tv_title: TextView? = null

    val view = LayoutInflater.from(context).inflate(R.layout.script_random_size_view, this).apply {
        seek_view = findViewById(R.id.seek_view)
        tv_title = findViewById(R.id.tv_title)
    }

    var name = ""

    var curValue = 0

    var mOnProgressChanged: ScriptSizeSeekbarView.OnSizeChangedListener? = null
        set(value) {
            field = value
            seek_view?.mOnProgressChanged = field
        }

    init {
        initAttrs1(attrs)
        initAttrs2(attrs)
        updateUI()
    }

    private fun initAttrs1(attrs: AttributeSet?) {
        attrs?.run {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.ScriptCommonView
            )
            val count = a.indexCount
            for (i in 0 until count) {
                val attr = a.getIndex(i)
                if (attr == R.styleable.ScriptCommonView_scriptName) {
                    name = a.getString(attr).toString()
                }
            }
            a.recycle()
        }
    }

    private fun initAttrs2(attrs: AttributeSet?) {
        attrs?.run {
            seek_view?.initAttrs(attrs)
        }
    }


    fun setValue(value: Int) {
        curValue = value
        seek_view?.setCurrentSize(value)
    }

    private fun updateUI() {
        tv_title?.text = name
    }

}