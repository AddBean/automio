// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.hive.utils.GlobalApp.dp2px

/**
 *
 * @author jiadou
 * @date 5/24/21
 */
class ColorPreviewCircleView(context: Context?) : View(context) {

    private val dp = dp2px(1)

    private var mRect = RectF()

    private var mInnerRect = RectF()

    private var mPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = 20 * dp
    }

    private var mPaintLine = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.GRAY
        strokeWidth = 0.5f * dp
    }

    var color = Color.BLACK
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.let {
            drawCircle(it)
        }
    }

    private fun drawCircle(canvas: Canvas) {
        mPaint.strokeWidth = 2f * dp
        mPaint.color = Color.WHITE
        canvas.drawCircle(mRect.centerY(), mRect.centerY(), 5f * dp, mPaint)

        mInnerRect.set(mRect)

        mPaint.strokeWidth = 20f * dp
        mInnerRect.inset(mPaint.strokeWidth / 2f, mPaint.strokeWidth / 2f)

        mPaint.color = color
        canvas.drawCircle(mInnerRect.centerX(), mInnerRect.centerY(), mInnerRect.width() / 2, mPaint)

        canvas.drawCircle(mInnerRect.centerX(), mInnerRect.centerY(), mInnerRect.width() / 2 + mPaint.strokeWidth / 2, mPaintLine)

        canvas.drawCircle(mInnerRect.centerX(), mInnerRect.centerY(), mInnerRect.width() / 2 - mPaint.strokeWidth / 2, mPaintLine)


    }

    fun getCurrentSelectedColor() = color

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(MeasureSpec.makeMeasureSpec((160 * dp).toInt(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((160 * dp).toInt(), MeasureSpec.EXACTLY))
        mRect.set(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mRect.set(0f, 0f, w.toFloat(), h.toFloat())
        invalidate()
    }


}