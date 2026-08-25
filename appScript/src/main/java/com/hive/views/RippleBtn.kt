// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.hive.framework.ext.dp

class RippleBtn : View {

    var panPaint = Paint()
    private var width = 100.dp
    private var rippleWidth = 10.dp

    constructor(context: Context) : this(context,null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        width = getWidth()
        rippleWidth = width/10
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawRipple(canvas)
        drawMainbg(canvas)
    }

    private fun drawRipple(canvas: Canvas){

    }

    private fun drawMainbg(canvas: Canvas){

    }
}