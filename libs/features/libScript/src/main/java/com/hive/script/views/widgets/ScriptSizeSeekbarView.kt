// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.hive.script.R
import com.hive.utils.GlobalApp
import com.hive.utils.GlobalApp.DP
import com.hive.utils.extends.dp

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/1/21
 */
class ScriptSizeSeekbarView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    var isTouchProgress = false;
    private var mPercent = 0f
    var maxSize = 11
        set(value) {
            field = value
            mPercent = ((curSize - minSize) / (maxSize - minSize)).toFloat()
            invalidate()
        }
    var minSize = 0
        set(value) {
            field = value
            mPercent = ((curSize - minSize) / (maxSize - minSize)).toFloat()
            invalidate()
        }
    private var curSize = 5
        set(value) {
            field = value
            invalidate()
        }
    private val closeTxt = GlobalApp.getString(com.hive.i8n.R.string.sc_off)
    private val rectF = RectF()
    private val paint = Paint().apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorSplitLine)
        isAntiAlias = true
    }

    private val paintDot = Paint().apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.FILL
        isAntiAlias = true
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorAccent)
    }

    private val mTextPaint = TextPaint().apply {
        isAntiAlias = true
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorAccent)
        textSize = 14f * DP
        typeface = Typeface.DEFAULT_BOLD
    }

    private val mTextBias =
        (mTextPaint.fontMetrics.descent - mTextPaint.fontMetrics.ascent) / 2 - mTextPaint.fontMetrics.descent

    init {
        initAttrs(attrs)
    }

    fun initAttrs(attrs: AttributeSet?) {
        if (attrs != null) {
            val ta = context!!.obtainStyledAttributes(attrs, R.styleable.ScriptSizeSeekbarView)
            maxSize = ta.getInteger(R.styleable.ScriptSizeSeekbarView_sbMaxValue, 1)
            minSize = ta.getInteger(R.styleable.ScriptSizeSeekbarView_sbMinValue, 0)
            curSize = ta.getInteger(R.styleable.ScriptSizeSeekbarView_sbValue, 1)
            mPercent = ((curSize - minSize) / (maxSize - minSize)).toFloat()
            ta.recycle()
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawProgress(canvas)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN)
            parent.requestDisallowInterceptTouchEvent(true)
        super.dispatchTouchEvent(event)
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (rectF.contains(event.x, event.y)) {
                    isTouchProgress = true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_UP -> {
                isTouchProgress = false
            }
        }
        if (isTouchProgress) {
            mPercent = (event.x - rectF.left) / rectF.width()
            if (mPercent > 1f) mPercent = 1f
            if (mPercent < 0f) mPercent = 0f
            curSize = (minSize + mPercent * (maxSize - minSize)).toInt()
            if (mOnProgressChanged != null) mOnProgressChanged?.onSizeChanged(event.action, curSize)
            invalidate()
        }
        return true
    }

    private fun drawProgress(canvas: Canvas) {
        paint.strokeWidth = 4f * DP
        var size = paint.strokeWidth / 2 + (minSize + mPercent * (maxSize - minSize)) * DP
        if (size > height / 2) {
            size = height / 2f
        }
        val txt = if (curSize == 0) closeTxt else "$curSize"
        val tw = mTextPaint.measureText(txt)

        rectF.set(42f * DP, 0f, width - 12f * DP, height.toFloat())

        canvas.drawLine(
            rectF.left - paint.strokeWidth / 2F,
            rectF.centerY(),
            rectF.right - paint.strokeWidth / 2F - 16.dp,
            rectF.centerY(),
            paint
        )

        canvas.drawCircle(
            rectF.left + rectF.width() * mPercent - size,
            rectF.centerY(),
            size,
            paintDot
        )

        canvas.drawText(txt, 18f * DP - tw / 2, rectF.centerY() + mTextBias, mTextPaint)
    }

    var mOnProgressChanged: OnSizeChangedListener? = null

    fun setCurrentSize(size: Int) {
        curSize = size
        mPercent = ((curSize - minSize) / (maxSize - minSize).toFloat())
        invalidate()
    }

    interface OnSizeChangedListener {
        fun onSizeChanged(action: Int, size: Int)
    }
}