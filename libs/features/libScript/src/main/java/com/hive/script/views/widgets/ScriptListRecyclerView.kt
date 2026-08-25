// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.widgets.AbsWindowFloatView

class ScriptListRecyclerView(context: Context, attrs: AttributeSet?) :
    ListRecyclerView(context, attrs) {

    private var interceptTouchEvent = true
    fun setInterceptTouchEvent(enable: Boolean) {
        interceptTouchEvent = enable
    }


    //禁止上层视图拦截事件
    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        val result = super.onInterceptTouchEvent(e)
        if (e?.action == MotionEvent.ACTION_DOWN && !interceptTouchEvent) {
            findFloatView()?.requestNoInterceptTouchEvent()
            return result
        }
        return result
    }

    private fun findFloatView(): AbsWindowFloatView? {
        var parent = parent
        var maxLevel = 10
        while (parent != null && maxLevel > 0) {
            if (parent is AbsWindowFloatView) {
                return parent
            }
            maxLevel--
            parent = parent.parent
        }
        return null
    }
}