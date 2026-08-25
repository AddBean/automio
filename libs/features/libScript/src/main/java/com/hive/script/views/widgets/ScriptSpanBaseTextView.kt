// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.graphics.Point
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

open class ScriptSpanBaseTextView(context: Context?, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatTextView(context!!, attrs) {

    var touchPoint = Point()

    var currentText: CharSequence? = null

    init {
        isClickable = true
        addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //处理部分span不显示
                if (!TextUtils.equals(s, (currentText))) {
                    post {
                        text = s
                        currentText = s
                    }
                } else {
                    currentText = s
                }
            }
        })
    }

    fun setSpans(
        content: String,
        spans: List<ScriptSpanHelper.ClickSpan>?,
        onClicked: (ScriptSpanHelper.ClickSpan, View) -> Unit
    ) {
        currentText = ScriptSpanHelper.getSpans(content, spans, onClicked)
        post {
            text = currentText
        }
    }

//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
////        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//
//        val width = MeasureSpec.getSize(widthMeasureSpec)
//        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
//        val minWidth = 100.dp()
//        //保证最小宽度为100dp
//        val newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
//            if (width < minWidth) minWidth else width,
//            widthMode
//        )
//        super.onMeasure(newWidthMeasureSpec, heightMeasureSpec)
//    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        ScriptSpanHelper.onTouchEvent(touchPoint, event)
        return super.onTouchEvent(event)
    }

}