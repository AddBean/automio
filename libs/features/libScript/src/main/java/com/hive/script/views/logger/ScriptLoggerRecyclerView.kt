// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.logger

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.hive.views.widgets.AbsWindowFloatView

class ScriptLoggerRecyclerView(context: Context, attrs: AttributeSet?) :
    RecyclerView(context, attrs) {

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        super.onTouchEvent(e)
        return true
    }

    //禁止上层视图拦截事件
    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        if (e?.action == MotionEvent.ACTION_DOWN) {
            findFloatView()?.requestNoInterceptTouchEvent()
            return true
        }
        return false
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