// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.hive.script.R
import com.hive.script.base.ScriptConst
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.views.record.ScriptRecordEventHandler.RecordResultAction
import com.hive.utils.GlobalApp
import kotlin.math.pow

/**
 *
 * @author jiadou
 * @date 6/29/21
 */
abstract class ScriptRecordSelectRectView(context: Context, attributeSet: AttributeSet) :
    ScriptRecordBaseView(context, attributeSet) {

    private val mTextPaint = Paint().apply {
        textSize = 13f * dp
        isAntiAlias = true
        color = GlobalApp.getColor(com.hive.i8n.R.color.textColorPrimary)
        //加粗
        isFakeBoldText = true
    }

    private val mTextBias =
        (mTextPaint.fontMetrics.descent - mTextPaint.fontMetrics.ascent) / 2 - mTextPaint.fontMetrics.descent


    private val btnText = GlobalApp.getString(com.hive.i8n.R.string.ok)

    private val btnTextLength = btnText.length * mTextPaint.textSize

    private var mRectFDefault = RectF().apply {
        val sh = ScriptCoordinateAdapter.getScreenHeightByOrientation()
        val sw = ScriptCoordinateAdapter.getScreenWidthByOrientation()
        val x = (sw - 100 * dp) / 2f
        val y = (sh - 100 * dp) / 2f
        set(x, y, (x + 100 * dp), (y + 100 * dp))
    }

    private var mTouchDownFlag = -1

    private var mRectTempF = RectF()

    private val mRect1Origin = RectF(0f, 0f, 30f * dp, 30f * dp)

    private val mRect2Origin = RectF(0f, 0f, 30f * dp, 30f * dp)

    private val mRect3Origin = RectF(0f, 0f, 30f * dp, 30f * dp)

    private val mRect4Origin = RectF(0f, 0f, 30f * dp, 30f * dp)

    private val mRectBtnOrigin = RectF(0f, 0f, 60f * dp, 30f * dp)

    private var mRect1 = RectF(mRect1Origin)

    private var mRect2 = RectF(mRect2Origin)

    private var mRect3 = RectF(mRect3Origin)

    private var mRect4 = RectF(mRect4Origin)

    private var originRectF = RectF()

    private var mRectConfirm = RectF(mRectBtnOrigin)

    private var mRectF = RectF().apply {
        set(mRectFDefault)
    }

    private val mPaint = Paint().apply {
        color = (0x99111111 and GlobalApp.getColor(com.hive.i8n.R.color.colorAccent).toLong()).toInt()
        isAntiAlias = true
        strokeWidth = 2f * dp
    }

    private val mPaintDashLine = Paint().apply {
        color = (0x69999999).toInt()
        isAntiAlias = true
        strokeWidth = 1f * dp
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(2f * dp, 2f * dp), 0f)
    }

    private val mPaintConner = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorAccent)
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE
    }


    private val mPaintConnerLine = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorSplitLine)
        isAntiAlias = true
        strokeWidth = 0.7f * dp
    }



    override fun dispatchDraw(canvas: Canvas) {
        canvas.run {
            drawSelection(canvas)
            drawSelectionDashLines(canvas, 12 * dp)
            drawConner(canvas)
            drawBtn(canvas)
        }
        super.dispatchDraw(canvas)
    }

    private fun drawSelection(canvas: Canvas) {
        mRectTempF.set(0f, 0f, canvas.width.toFloat(), mRectF.top)
        canvas.drawRect(mRectTempF, mPaint)

        mRectTempF.set(0f, mRectF.top, mRectF.left, mRectF.bottom)
        canvas.drawRect(mRectTempF, mPaint)

        mRectTempF.set(0f, mRectF.bottom, canvas.width.toFloat(), canvas.height.toFloat())
        canvas.drawRect(mRectTempF, mPaint)

        mRectTempF.set(mRectF.right, mRectF.top, canvas.width.toFloat(), mRectF.bottom)
        canvas.drawRect(mRectTempF, mPaint)

    }

    private fun drawSelectionDashLines(canvas: Canvas, lineMargin: Int) {
        val lineCount = (mRectF.width() / lineMargin).toInt()
        for (i in 0..lineCount) {
            val x = mRectF.left + i * lineMargin
            canvas.drawLine(x, mRectF.top, x, mRectF.bottom, mPaintDashLine)
        }

        val lineCount2 = (mRectF.height() / lineMargin).toInt()
        for (i in 0..lineCount2) {
            val y = mRectF.top + i * lineMargin
            canvas.drawLine(mRectF.left, y, mRectF.right, y, mPaintDashLine)
        }
    }

    private val connerRectF = RectF()
    private fun drawConner(canvas: Canvas) {
        connerRectF.set(mRect1)
        connerRectF.inset(7f * dp, 7f * dp)
        canvas.drawArc(connerRectF, 90f, 270f, true, mPaintConner)
        connerRectF.set(mRect2)
        connerRectF.inset(7f * dp, 7f * dp)
        canvas.drawArc(connerRectF, 180f, 270f, true, mPaintConner)
        connerRectF.set(mRect3)
        connerRectF.inset(7f * dp, 7f * dp)
        canvas.drawArc(connerRectF, 0f, 270f, true, mPaintConner)
        connerRectF.set(mRect4)
        connerRectF.inset(7f * dp, 7f * dp)
        canvas.drawArc(connerRectF, 270f, 270f, true, mPaintConner)
    }

    private fun drawBtn(canvas: Canvas) {
        canvas.drawRoundRect(mRectConfirm, 20f * dp, 20f * dp, mPaintConner)

        canvas.drawRoundRect(mRectConfirm, 20f * dp, 20f * dp, mPaintConnerLine)

        canvas.drawText(
            btnText,
            mRectConfirm.centerX() - (btnTextLength) / if (ScriptConst.compatWideScreen()) 4f else 2f,
            mRectConfirm.centerY() + mTextBias,
            mTextPaint
        )
    }

    private var mLastPoint = PointF(0f, 0f)

    private var mDownPoint = PointF(0f, 0f)

    private var mClickFlag = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.run {
            if (MotionEvent.ACTION_DOWN == event.action) {
                mLastPoint.x = event.x
                mLastPoint.y = event.y
                mDownPoint.x = event.x
                mDownPoint.y = event.y
                when {
                    getTouchRect(mRect1).contains(event.x, event.y) -> {
                        mTouchDownFlag = 1
                    }

                    getTouchRect(mRect2).contains(event.x, event.y) -> {
                        mTouchDownFlag = 2
                    }

                    getTouchRect(mRect3).contains(event.x, event.y) -> {
                        mTouchDownFlag = 3
                    }

                    getTouchRect(mRect4).contains(event.x, event.y) -> {
                        mTouchDownFlag = 4
                    }

                    mRectConfirm.contains(event.x, event.y) -> {
                        mTouchDownFlag = 5
                    }

                    mRectF.contains(event.x, event.y) -> {
                        mTouchDownFlag = 0
                    }

                    else -> {
                        mTouchDownFlag = -1
                    }
                }
            }
            when (mTouchDownFlag) {
                0 -> {
                    when (event.action) {
                        MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            mRectF.offset(event.x - mLastPoint.x, event.y - mLastPoint.y)
                            mLastPoint.x = event.x
                            mLastPoint.y = event.y
                        }
                    }
                }

                1, 2, 3, 4, 5 -> {
                    when (event.action) {
                        MotionEvent.ACTION_MOVE -> {
                            val touchSlop =
                                ViewConfiguration.get(context).scaledTouchSlop.toDouble()
                            val d1 = (event.x - mDownPoint.x).toDouble()
                                .pow(2.0) + (event.y - mDownPoint.y).toDouble().pow(2.0)
                            if (d1 > touchSlop.pow(2.0)) {
                                mClickFlag = false
                            }
                            if (!mClickFlag) {
                                when (mTouchDownFlag) {
                                    1 -> {
                                        mRectF.left = event.x
                                        mRectF.top = event.y
                                    }

                                    2 -> {
                                        mRectF.right = event.x
                                        mRectF.top = event.y
                                    }

                                    3 -> {
                                        mRectF.left = event.x
                                        mRectF.bottom = event.y
                                    }

                                    4 -> {
                                        mRectF.right = event.x
                                        mRectF.bottom = event.y
                                    }

                                    5 -> {
                                        mRectF.offset(
                                            event.x - mLastPoint.x,
                                            event.y - mLastPoint.y
                                        )
                                        mLastPoint.x = event.x
                                        mLastPoint.y = event.y
                                    }
                                }
                            }
                        }

                        MotionEvent.ACTION_UP -> {
                            if (mClickFlag) {
                                onConfirmClick(mTouchDownFlag)
                            }
                        }
                    }

                }

                else -> {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            mRectF.left = event.x
                            mRectF.top = event.y
                            mRectF.right = event.x
                            mRectF.bottom = event.y
                        }

                        MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            mRectF.right = event.x
                            mRectF.bottom = event.y
                        }
                    }
                }
            }
        }
        if (event?.action == MotionEvent.ACTION_UP || event?.action == MotionEvent.ACTION_CANCEL) {
            mTouchDownFlag = -1
            mClickFlag = true
        }

        checkRectIlegel()
        updateConnerRect()
        updateBtnRect()
        invalidate()
        return true
    }

    /**
     * 获取触摸有效区域
     */
    private fun getTouchRect(r: RectF): RectF {
        val touchRect = RectF(r)
        touchRect.inset(-2f * dp, -2f * dp)
        return touchRect
    }

    /**
     * 矩形不能超出屏幕，宽高不能小于10dp
     */
    private fun checkRectIlegel() {
        val screenW = ScriptCoordinateAdapter.getScreenWidthByOrientation()
        val screenH = ScriptCoordinateAdapter.getScreenHeightByOrientation()
        if (mRectF.left < 0) {
            mRectF.left = 0f
        }
        if (mRectF.top < 0) {
            mRectF.top = 0f
        }
        if (mRectF.right > screenW) {
            mRectF.right = screenW.toFloat()
        }
        if (mRectF.bottom > screenH) {
            mRectF.bottom = screenH.toFloat()
        }
        if (mRectF.width() < 10 * dp) {
            mRectF.right = mRectF.left + 10 * dp
        }
        if (mRectF.height() < 10 * dp) {
            mRectF.bottom = mRectF.top + 10 * dp
        }
    }

    open fun isUseNormalization() = true

    abstract fun getActionType(): RecordResultAction

    private fun onConfirmClick(clickType: Int) {
        if (isUseNormalization()) {
            baseEventHandler?.notifyEvent(
                getActionType(),
                clickType to ScriptCommonUtils.convertToNormalization(mRectF, width, height)
            )
        } else {
            baseEventHandler?.notifyEvent(getActionType(), clickType to mRectF)
        }
    }


    fun setNormalizedRect(rectF: RectF) {
        post {
            originRectF.set(rectF)
            mRectF.set(ScriptCommonUtils.convertToLocation(originRectF, width, height))
            checkRectIlegel()
            updateConnerRect()
            updateBtnRect()
            invalidate()
        }
    }

    private fun updateConnerRect() {
        mRect1.set(mRect1Origin)

        mRect2.set(mRect2Origin)

        mRect3.set(mRect3Origin)

        mRect4.set(mRect4Origin)

        mRect1.offset(
            mRectF.left - mRect1Origin.width() / 2,
            mRectF.top - mRect1Origin.height() / 2
        )

        mRect2.offset(
            mRectF.right - mRect2Origin.width() / 2,
            mRectF.top - mRect2Origin.height() / 2
        )

        mRect3.offset(
            mRectF.left - mRect3Origin.width() / 2,
            mRectF.bottom - mRect3Origin.height() / 2
        )

        mRect4.offset(
            mRectF.right - mRect4Origin.width() / 2,
            mRectF.bottom - mRect4Origin.height() / 2
        )
    }

    private fun getConfirmRect(fitType: Int): RectF {
        val mRectConfirm = RectF(mRectBtnOrigin)
        mRectConfirm.set(mRectBtnOrigin)
        when (fitType) {
            0 -> {//上
                mRectConfirm.offset(
                    mRectF.centerX() - mRectBtnOrigin.width() / 2,
                    mRectF.top - mRectBtnOrigin.height() - 8 * dp
                )
            }

            1 -> {//中
                mRectConfirm.offset(
                    mRectF.centerX() - mRectBtnOrigin.width() / 2,
                    mRectF.centerY() - mRectBtnOrigin.height() / 2
                )
            }

            2 -> {//下
                mRectConfirm.offset(
                    mRectF.centerX() - mRectBtnOrigin.width() / 2,
                    mRectF.bottom + 8 * dp
                )
            }
        }

        return mRectConfirm
    }

    private fun updateBtnRect() {
        mRectConfirm.set(mRectBtnOrigin)
        var btnRect = getConfirmRect(2)
        if (!isRectOutOfScreenInVert(btnRect)) {
            mRectConfirm.set(btnRect)
            return
        }
        btnRect = getConfirmRect(0)
        if (!isRectOutOfScreenInVert(btnRect)) {
            mRectConfirm.set(btnRect)
            return
        }

        btnRect = getConfirmRect(1)
        mRectConfirm.set(btnRect)
    }

    private fun isRectOutOfScreenInVert(rect: RectF): Boolean {
//        val screenW = ScriptCoordinateAdapter.getScreenWidthByOrientation()
        val screenH = ScriptCoordinateAdapter.getScreenHeightByOrientation()
        return rect.top < 0 || rect.bottom > screenH
    }


    fun reset() {

        mRect1.set(mRect1Origin)

        mRect2.set(mRect2Origin)

        mRect3.set(mRect3Origin)

        mRect4.set(mRect4Origin)

        mRectBtnOrigin.set(mRectBtnOrigin)

        updateConnerRect()

        updateBtnRect()

        invalidate()
    }

    override fun onShow() {
        super.onShow()
        reset()
        if (isUseNormalization()) {
            setNormalizedRect(originRectF)
        }
    }

}