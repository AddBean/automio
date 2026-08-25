// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.graphics.Point
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

open class ScriptSpanBaseEditView(context: Context?, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatEditText(context!!, attrs) {

    var touchPoint = Point()

    init {
        isClickable = true
        movementMethod = LinkMovementMethod.getInstance()
    }

    fun setSpans(
        content: String,
        spans: List<ScriptSpanHelper.ClickSpan>?,
        onClicked: (ScriptSpanHelper.ClickSpan, View) -> Unit
    ) {
        setText(ScriptSpanHelper.getSpans(content, spans, onClicked))
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        ScriptSpanHelper.onTouchEvent(touchPoint, event)
        return super.onTouchEvent(event)
    }

}