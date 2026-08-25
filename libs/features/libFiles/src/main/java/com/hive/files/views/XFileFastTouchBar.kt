// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.views

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.hive.libfiles.R
import com.hive.utils.GlobalApp
import com.hive.utils.system.UIUtils

class XFileFastTouchBar(context: Context, atts: AttributeSet) : View(context, atts) {

    private var mTouchState: Boolean = false
    var DP = UIUtils.dp2px(GlobalApp.sContext, 1)
    var mColorNormal = GlobalApp.getColor(com.hive.i8n.R.color.color_gran_trans)
    var mColorPressed = GlobalApp.getColor(com.hive.i8n.R.color.color_blue)
    var mTouchBarBitmap = BitmapFactory.decodeResource(GlobalApp.getResources(), R.drawable.file_scroll_bar)
    var mTouchBarBitmapRect = Rect(0, 0, mTouchBarBitmap.width, mTouchBarBitmap.height)
    var mColor = mColorNormal
    var paint = Paint()
    var mOnFastTouchListener: OnFastTouchListener? = null

    private val rectSize = RectF(-20f * DP, 0f * DP, 50f * DP - 20f * DP, 50f * DP * (mTouchBarBitmap.height.toFloat() / mTouchBarBitmap.width))
    private var targetRect = RectF(rectSize)
    private var enableBitmap = true

    private var mCurrentProgress: Float = 0f
        set(value) {
            field = value
            var height = rectSize.height()
            targetRect.top = value * measuredHeight.toFloat()
            targetRect.bottom = targetRect.top + height
            invalidate()
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (parent != null) {
            (parent as ViewGroup).clipChildren = false
            (parent as ViewGroup).clipToPadding = false
            if (parent.parent != null) {
                (parent.parent as ViewGroup).clipChildren = false
                (parent.parent as ViewGroup).clipToPadding = false
            }
        }
    }

    fun updateProgressOutside(p: Float) {
        if (mTouchState) return
        mCurrentProgress = p
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawSlidBar(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (!enableBitmap) {
            rectSize.right = MeasureSpec.getSize(widthMeasureSpec).toFloat() - 4 * DP
            targetRect.right = MeasureSpec.getSize(widthMeasureSpec).toFloat() - 4 * DP
        }
    }

    private fun drawSlidBar(canvas: Canvas?) {
        paint?.color = mColor
        if (enableBitmap) {
            if (mTouchState) {
                paint?.alpha = 255
            } else {
                paint?.alpha = 140
            }
            canvas?.drawBitmap(mTouchBarBitmap, mTouchBarBitmapRect, targetRect, paint!!)
        } else {
            canvas?.drawRoundRect(targetRect, 4f * DP, 4f * DP, paint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mColor = mColorPressed
                targetRect.inset(-10f * DP, -10f * DP)
                var result = targetRect.contains(event.x, event.y)
                targetRect.inset(10f * DP, 10f * DP)
                postInvalidate()
                mTouchState = result
                return result
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mColor = mColorNormal
                mTouchState = false
            }
        }
        mCurrentProgress = (event?.y ?: 0f) / (measuredHeight.toFloat() - rectSize.height())
        mOnFastTouchListener?.onTouchProgress((event?.y ?: 0f) / measuredHeight.toFloat())
        postInvalidate()
        return true
    }

    interface OnFastTouchListener {
        fun onTouchProgress(progress: Float)
    }
}