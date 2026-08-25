// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import com.hive.utils.system.UIUtils
import com.hive.views.R

class ProgressCircleView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private var mSweepMode: Boolean = true
    private var mAnimSweepAngle: Float = 0f
    private var mAnimPercent: Float = 0f
    private var mAnimPercentStep: Float = 0.007f //决定动画速度
    private var mProgressType: Int = 0
    private var mPadding: Float
    private var mLineWidth: Float
    private var rectF: RectF = RectF();
    private var mTargetPercent = 0.5f
    private var mCurrentPercent = 0f
    private var isAnimRunning = true
    private var mPercent = 0f
    private var mBackgroundColor = 0x5f36ADFA.toInt()
    private var mColor = 0xff36ADFA.toInt()
    private var paint: Paint = Paint();
    private var dp = UIUtils.dp2px(context!!, 1).toFloat()

    init {
        val ta = getContext().obtainStyledAttributes(attrs, R.styleable.ProgressView)
        mColor = ta.getColor(R.styleable.ProgressView_progressFrontColor, 0x5f36ADFA.toInt())
        mBackgroundColor = ta.getColor(R.styleable.ProgressView_progressBackColor, 0xff36ADFA.toInt())
        mLineWidth = ta.getDimension(R.styleable.ProgressView_progressLineWidth, 2 * dp)
        mPadding = ta.getDimension(R.styleable.ProgressView_progressPadding, 1 * dp)
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
        paint.alpha = 100
        paint.strokeWidth = mLineWidth
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        canvas?.let {
            //进度条模式
            if (mProgressType == 0) {
                drawCircle(it)
            }
            //循环动画模式
            if (mProgressType == 1) {
                drawCircleAnim(it)
                postInvalidate()
            }
            if (mProgressType == -1) {
                drawCircleBg(canvas)
            }
        }


    }

    private fun drawCircleBg(canvas: Canvas) {
        paint.color = mBackgroundColor
        canvas.drawOval(rectF, paint)
    }

    private fun drawCircleAnim(canvas: Canvas) {
        paint.color = mBackgroundColor
        canvas.drawOval(rectF, paint)
        paint.color = mColor

        mAnimSweepAngle = 60f
        canvas.drawArc(rectF, mAnimPercent * 360, mAnimSweepAngle, false, paint)
        mAnimPercent += mAnimPercentStep
//        if (mSweepMode) {
//            mAnimSweepAngle = mAnimPercent * 360
//        } else {
//            mAnimSweepAngle = (1 - mAnimPercent) * 360
//        }
//        if (mAnimSweepAngle >= 360 || mAnimSweepAngle <= 0) {
//            mSweepMode = !mSweepMode
//        }
        if (mAnimPercent > 1) mAnimPercent = 0f

    }

    private fun drawCircle(canvas: Canvas) {
        paint.color = mBackgroundColor
        canvas.drawOval(rectF, paint)
        paint.color = mColor
        canvas.drawArc(rectF, -90f, mPercent * 360, false, paint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rectF.set(0f, 0f, w.toFloat(), h.toFloat())
        rectF.inset(mPadding, mPadding)
        invalidate()
    }


    fun setPercent(percent: Float) {
        setProgress(percent);
    }

    fun setProgressType(type: Int) {
        mProgressType = type
        invalidate()
    }

    fun setProgress(percent: Float) {
        mPercent = percent
        isAnimRunning = false
        mTargetPercent = mPercent
        mCurrentPercent = mPercent
        invalidate()
    }
}