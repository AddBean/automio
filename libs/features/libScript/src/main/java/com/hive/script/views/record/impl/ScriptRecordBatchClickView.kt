// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.hive.script.R
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdPatternTap
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.views.record.ScriptRecordBaseView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.record.handler.ScriptBatchClickHandler
import com.hive.script.views.widgets.ScriptNumberView
import com.hive.utils.GlobalApp
import com.hive.views.widgets.NumberOptView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/29/21
 */
class ScriptRecordBatchClickView(context: Context, attributeSet: AttributeSet) :
    ScriptRecordBaseView(context, attributeSet) {

    private var mTouchDownFlag = -1

    private var mRectFDefault = RectF().apply {
        val sh = ScriptCoordinateAdapter.getScreenHeightByOrientation()
        val sw = ScriptCoordinateAdapter.getScreenWidthByOrientation()
        val x = (sw - 100 * dp) / 2f
        val y = (sh - 100 * dp) / 2f
        set(x, y, (x + 100 * dp), (y + 100 * dp))
    }

    private var mRectF = RectF()

    private var originRectF = RectF().apply {
        val sh = ScriptCoordinateAdapter.getScreenHeightByOrientation()
        val sw = ScriptCoordinateAdapter.getScreenWidthByOrientation()
        set(
            mRectFDefault.left / sw,
            mRectFDefault.top / sh,
            mRectFDefault.right / sw,
            mRectFDefault.bottom / sh
        )
    }

    private val mRect1Origin = RectF(0f, 0f, 30f * dp, 30f * dp)

    private val mRect2Origin = RectF(0f, 0f, 30f * dp, 30f * dp)

    private val mRect3Origin = RectF(0f, 0f, 30f * dp, 30f * dp)

    private val mRect4Origin = RectF(0f, 0f, 60f * dp, 30f * dp)

    private var mRect1 = RectF(mRect1Origin)

    private var mRect2 = RectF(mRect2Origin)

    private var mRect3 = RectF(mRect3Origin)

    private var mRect4 = RectF(mRect4Origin)


    private var cmd: CmdPatternTap? = null


    private val mPaint = Paint().apply {
        color = (0x99111111 and GlobalApp.getColor(com.hive.i8n.R.color.colorAccent).toLong()).toInt()
        isAntiAlias = true
        strokeWidth = 2f * dp
    }

    private val mPaintConner = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorAccent)
        isAntiAlias = true
        strokeWidth = 1f * dp
    }


    private val mPaintDot = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorRed)
        alpha = 160
        isAntiAlias = true
    }

    override fun getCtrView(): View? {
        return ControlView(context)
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.run {
            drawSelection(canvas)
            drawConner(canvas)
            drawClickPoints(canvas)
        }
        super.dispatchDraw(canvas)
    }

    override fun getViewTypes() = mutableListOf(ScriptRecordViewManager.RecordViewType.BATCH_CLICK)

    override fun getEventHandler() = ScriptBatchClickHandler(this)

    private fun drawSelection(canvas: Canvas) {
        canvas.drawRect(mRectF, mPaint)
    }

    private fun drawConner(canvas: Canvas) {
        canvas.drawCircle(mRect1.centerX(), mRect1.centerY(), 8f * dp, mPaintConner)
        canvas.drawCircle(mRect2.centerX(), mRect2.centerY(), 8f * dp, mPaintConner)
        canvas.drawCircle(mRect3.centerX(), mRect3.centerY(), 8f * dp, mPaintConner)
        canvas.drawCircle(mRect4.centerX(), mRect4.centerY(), 8f * dp, mPaintConner)
    }

    /**
     * 在canvas中的mRectF区域中均匀绘制横向为cmd.clickHrz个，纵向为cmd.clickVer的点
     */
    private fun drawClickPoints(canvas: Canvas) {
        cmd?.run {
            ScriptCommonUtils.forEachRect(mRectF, clickType, clickHrz, clickVer) { x, y ->
                canvas.drawCircle(
                    x, y, 1f * dp, mPaintDot
                )
            }
        }
    }

    private var mLastPoint = PointF(0f, 0f)

    private var mDownPoint = PointF(0f, 0f)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.run {
            if (MotionEvent.ACTION_DOWN == event.action) {
                mLastPoint.x = event.x
                mLastPoint.y = event.y
                mDownPoint.x = event.x
                mDownPoint.y = event.y
                when {
                    mRect1.contains(event.x, event.y) -> {
                        mTouchDownFlag = 1
                    }

                    mRect2.contains(event.x, event.y) -> {
                        mTouchDownFlag = 2
                    }

                    mRect3.contains(event.x, event.y) -> {
                        mTouchDownFlag = 3
                    }

                    mRect4.contains(event.x, event.y) -> {
                        mTouchDownFlag = 4
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

                1, 2, 3, 4 -> {
                    when (event.action) {
                        MotionEvent.ACTION_MOVE -> {
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
        }

        checkRectIlegel()
        updateConnerRect()
        invalidate()
        return true
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

    private fun updateConnerRect() {
        mRect1.set(mRect1Origin)

        mRect2.set(mRect2Origin)

        mRect3.set(mRect3Origin)

        mRect4.set(mRect4Origin)

        mRect1.offset(
            mRectF.left - mRect1Origin.width() / 2, mRectF.top - mRect1Origin.height() / 2
        )

        mRect2.offset(
            mRectF.right - mRect2Origin.width() / 2, mRectF.top - mRect2Origin.height() / 2
        )

        mRect3.offset(
            mRectF.left - mRect3Origin.width() / 2, mRectF.bottom - mRect3Origin.height() / 2
        )

        mRect4.offset(
            mRectF.right - mRect4Origin.width() / 2, mRectF.bottom - mRect4Origin.height() / 2
        )
    }

    fun reset() {

        mRect1.set(mRect1Origin)

        mRect2.set(mRect2Origin)

        mRect3.set(mRect3Origin)

        mRect4.set(mRect4Origin)

        mRectF.set(mRectFDefault)

        invalidate()
    }


    override fun onShow() {
        super.onShow()
        reset()
        val rect = RectF(0f, 0f, 0f, 0f)
        cmd = CmdPatternTap.createCommand(0, ScriptConst.Cmd_Click_Default, 10, 10, rect)
        if (controlView is ControlView?) {
            (controlView as ControlView).adjustPosition()
        }
        setFloatRect(originRectF)
    }

    private fun setFloatRect(rectF: RectF) {
        post {
            originRectF.set(rectF)
            mRectF.set(ScriptCommonUtils.convertToLocation(originRectF, width, height))
            checkRectIlegel()
            updateConnerRect()
            invalidate()
        }
    }


    override fun getLayoutName(): String {
        return GlobalApp.getString(com.hive.i8n.R.string.sc_spot_layout_name_7)
    }


    inner class ControlView(context: Context) : FrameLayout(context) {

        private var number_hrz: ScriptNumberView? = null

        private var number_ver: ScriptNumberView? = null

        val view = LayoutInflater.from(context)
            .inflate(R.layout.script_batch_click_control_view, this@ControlView)


        override fun onConfigurationChanged(newConfig: Configuration?) {
            super.onConfigurationChanged(newConfig)
            post { adjustPosition() }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            post { adjustPosition() }
        }

        fun adjustPosition() {
            number_hrz = view.findViewById(R.id.number_hrz)
            number_ver = view.findViewById(R.id.number_ver)
            cmd?.run {
                number_hrz?.setNumber(clickHrz)
                number_ver?.setNumber(clickVer)
            }
            number_hrz?.changedListener =
                NumberOptView.OnValueChangedListener { value ->
                    cmd?.clickHrz = value
                    postInvalidate()
                }

            number_ver?.changedListener =
                NumberOptView.OnValueChangedListener { value ->
                    cmd?.clickVer = value
                    postInvalidate()
                }
            view.findViewById<View>(R.id.btn_submit)?.setOnClickListener {
                cmd?.limitRect = ScriptCommonUtils.convertToNormalization(
                    mRectF,
                    this@ScriptRecordBatchClickView.width,
                    this@ScriptRecordBatchClickView.height
                )
                baseEventHandler?.notifyEvent(
                    ScriptRecordEventHandler.RecordResultAction.ACTION_BATCH,
                    cmd
                )
            }
        }

    }

}