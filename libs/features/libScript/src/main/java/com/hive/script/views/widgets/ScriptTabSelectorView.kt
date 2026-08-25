// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.hive.script.R
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/1/21
 */
class ScriptTabSelectorView(context: Context?, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {

    var selector_view: SelectorTabView? = null

    var tv_title: TextView? = null

    val view = LayoutInflater.from(context).inflate(R.layout.script_tab_selector_view, this).apply {
        selector_view=findViewById(R.id.selector_view)
        tv_title=findViewById(R.id.tv_title)
    }

    var name = ""

    var curValue = ""

    var onTabSelectedChangedListener: SelectorTabView.OnTabSelectedChangedListener? = null
        set(value) {
            field = value
            selector_view?.onTabSelectedChangedListener = field
        }

    init {
        initAttrs1(attrs)
        initAttrs2(attrs)
        updateUI()
    }

    @SuppressLint("CustomViewStyleable")
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
            selector_view?.initAttrs(attrs)
        }
    }


    fun setValue(value: String) {
        curValue = value
        selector_view?.setValue(value)
    }

    private fun updateUI() {
        tv_title?.text = name
    }

}