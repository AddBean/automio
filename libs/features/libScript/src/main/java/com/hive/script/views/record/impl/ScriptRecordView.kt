// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.impl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.os.Vibrator
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptRecordHelper
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.cmd.CmdClick
import com.hive.script.cmd.CmdDelay
import com.hive.script.cmd.CmdPress
import com.hive.script.cmd.CmdScroll
import com.hive.script.cmd.CmdScrollMultiple
import com.hive.script.setting.ScriptSetting
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.ScriptItemViewFactory
import com.hive.script.views.ScriptTouchEventHelper
import com.hive.script.views.ScriptTouchEventListener
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.preview.ScriptItemView
import com.hive.script.views.record.IScriptRecordView
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.record.handler.ScriptRecordHandler
import com.hive.script.views.widgets.ScriptAutoAdjustLayout
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/9/21
 */

class ScriptRecordView(context: Context?, attrs: AttributeSet?) :
    ScriptAutoAdjustLayout(context!!, attrs), ScriptInterpreterObserver.CommandExecuteObserver,
    ScriptInterpreterObserver.InterpreterExecuteObserver,
    ScriptInterpreterObserver.CommandRecordObserver, IScriptRecordView {

    private var currentViewState: ScriptRecordViewManager.ViewState? = null

    private var isPlayRunning = false

    private var dp = GlobalApp.DP

    private var childViews = mutableListOf<ScriptItemView>()

    private var mPointPath = Path()

    private var mRect = Rect()

    private var mPaint = Paint()

    private var tipText = ""

    private var pointsMap: MutableMap<Int, MutableList<Point>> = mutableMapOf()

    private var pointsTimeMap: MutableMap<Int, MutableList<Long>> = mutableMapOf()

    private var pointsLastTimeMap: MutableMap<Int, Long> = mutableMapOf()

    private var pointsStartTimeMap: MutableMap<Int, Long> = mutableMapOf()

    private var isScrolling = false

    private val textPaint = TextPaint().apply {
        textSize = 40f
        color = 0x2fffffff
    }

    private var startTouchTime = 0L

    private var enableTouchable: Boolean = true
        set(value) {
            field = value
            if (value) isPlayRunning = false
            ScriptRecordManager.setRecordTouchable(value)
            invalidate()
        }

    private var enableBackgroundDraw: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    private var enableChildDraw: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    private var mScriptTouchEventHelper = ScriptTouchEventHelper(context!!).apply {
        registerEventListener(object : ScriptTouchEventListener() {

            var isLongPress = false

            var downPoint = Point()

            override fun onTouchEvent(e: MotionEvent) {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        isScrolling = false
                        downPoint.x = e.rawX.toInt()
                        downPoint.y = e.rawY.toInt()
                        isLongPress = false
                        startTouchTime = System.currentTimeMillis()
                        resetRecordData()
                        recordDownEvent(e)
                    }

                    MotionEvent.ACTION_POINTER_DOWN -> {
                        recordDownEvent(e)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        for (i in 0 until e.pointerCount) {
                            pointsMap[e.getPointerId(i)]?.add(
                                if (i == 0) {
                                    Point(
                                        e.rawX.toInt(),
                                        e.rawY.toInt()
                                    )
                                } else {
                                    Point(
                                        e.getX(i).toInt(),
                                        e.getY(i).toInt()
                                    )
                                }

                            )
                            pointsTimeMap[e.getPointerId(i)]?.run {
                                add(System.currentTimeMillis() - pointsLastTimeMap[e.getPointerId(i)]!!)
                                pointsLastTimeMap[e.getPointerId(i)] = System.currentTimeMillis()
                            }
                        }
                    }

                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                        if (isLongPress) {
                            isLongPress = false
                            val cmd = CmdPress.createCommand(
                                downPoint.x,
                                downPoint.y,
                                System.currentTimeMillis() - startTouchTime
                            )
                            addAndExecuteCommand(cmd)
                        }
                    }
                }
            }


            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val cmd = CmdClick.createCommand(e.rawX.toInt(), e.rawY.toInt())
                addAndExecuteCommand(cmd)
                return false
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                val vibrator = getContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(50L)
                isLongPress = true
            }


            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
            ): Boolean {
                isScrolling = true
                invalidate()
                return true
            }

        })
    }

    override fun getViewState(): ScriptRecordViewManager.ViewState {
        return currentViewState ?: ScriptRecordViewManager.ViewState.default()
    }

    override fun setViewState(state: ScriptRecordViewManager.ViewState) {
        this.currentViewState = state
        enableTouchable = state.isEnable(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
        enableChildDraw = state.isEnable(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
    }

    override fun getViewTypes(): List<ScriptRecordViewManager.RecordViewType> {
        return listOf(
            ScriptRecordViewManager.RecordViewType.TOUCHABLE,
            ScriptRecordViewManager.RecordViewType.FAST_CLICK,
            ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW
        )
    }

    override fun getEventHandler() = ScriptRecordHandler(this)

    private fun resetRecordData() {
        pointsMap.clear()
        pointsTimeMap.clear()
        pointsLastTimeMap.clear()
        pointsStartTimeMap.clear()
    }

    private fun recordDownEvent(e: MotionEvent) {
        pointsMap[e.getPointerId(e.actionIndex)] = mutableListOf()
        pointsTimeMap[e.getPointerId(e.actionIndex)] = mutableListOf()
        pointsLastTimeMap[e.getPointerId(e.actionIndex)] =
            System.currentTimeMillis()
        pointsStartTimeMap[e.getPointerId(e.actionIndex)] =
            System.currentTimeMillis() - startTouchTime
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ScriptInterpreterObserver.registerInterpreterObserver(this)
        ScriptInterpreterObserver.registerCommandObserver(this)
        ScriptInterpreterObserver.registerCommandRecordObserver(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ScriptInterpreterObserver.unRegisterInterpreterObserver(this)
        ScriptInterpreterObserver.unRegisterCommandObserver(this)
        ScriptInterpreterObserver.unRegisterCommandRecordObserver(this)
    }


    override fun dispatchDraw(canvas: Canvas) {
        canvas.run {
            if (enableBackgroundDraw) {
                drawBackground(canvas)
            }
            if (enableChildDraw) {
                dispatchDrawChild(canvas)
            }
            if (enableTouchable) {
                drawTouchPath(canvas)
            } else {
                if (!isPlayRunning || ScriptSetting.script_setting_show_tracks) {
                    val cmd = ScriptRecordHelper.instance.getRunningCommand()
                    childViews.find { it.command == cmd }?.dispatchDraw(canvas)
                }

            }
        }
        super.dispatchDraw(canvas)
    }


    private fun drawTipText(canvas: Canvas) {
        canvas.save()
        val textWidth = tipText.length * textPaint.textSize
        val margin = 8 * dp
        var dy = 0
        var id = 0
        while (dy < height) {
            var dx = id
            id -= 20 * dp
            while (dx < width) {
                canvas.drawText(tipText, dx.toFloat(), dy.toFloat(), textPaint)
                dx = (dx + textWidth + margin).toInt()
            }
            dy = (dy + (textPaint.textSize + 3f * margin)).toInt()
        }
        canvas.rotate(45f)
        canvas.restore()
    }

    private fun drawTouchPath(canvas: Canvas) {
        canvas.save()
        pointsMap.forEach { entry ->
            val points = entry.value
            mPointPath.reset()
            for (i in points.indices) {
                if (i == 0) mPointPath.moveTo(points[i].x.toFloat(), points[i].y.toFloat())
                else mPointPath.quadTo(
                    points[i - 1].x.toFloat(),
                    points[i - 1].y.toFloat(),
                    points[i].x.toFloat(),
                    points[i].y.toFloat()
                )
            }
            ScriptCommonDrawer.drawPath(canvas, mPointPath, 100)
        }
        canvas.restore()
    }

    private fun drawBackground(canvas: Canvas) {
        if (ScriptSetting.script_setting_frame_running) {
            if (enableTouchable) {
                mPaint.style = Paint.Style.STROKE
                mPaint.strokeWidth = 20f
                mPaint.color = Color.GREEN
                mRect.set(0, 0, canvas.width, canvas.height)
//            canvas.drawColor(0x3f000000)
                canvas.drawRect(mRect, mPaint)
            }
            if (isPlayRunning) {
                mPaint.style = Paint.Style.STROKE
                mPaint.strokeWidth = 20f
                mPaint.color = Color.RED
                mRect.set(0, 0, canvas.width, canvas.height)
                canvas.drawRect(mRect, mPaint)
            }
        }
    }

    private fun dispatchDrawChild(canvas: Canvas) {
        childViews.forEach {
            if (it.isRunning()) {
                it.dispatchDraw(canvas)
            }
        }
    }

    fun addCommandView(cmd: ScriptCommand) {
        ScriptItemViewFactory.createItemViewByCommand(this, cmd)?.run {
            this.index = 0
            this.totalCount = childViews.size
            addView(this)
        }
    }

    fun clearTrackView() {
        removeAllView()
        resetRecordData()
        invalidate()
    }

    fun resetDataView() {
        removeAllView()
        var curIndex = 0
        val totalCount = ScriptRecordHelper.instance.getRealCommandCount()
        ScriptRecordHelper.instance.traverseScript {
            ScriptItemViewFactory.createItemViewByCommand(this, it)?.run {
                this.index = curIndex
                this.totalCount = totalCount
                addView(this)
                if (it !is CmdDelay) {
                    curIndex++
                }
            }
        }
        invalidate()
    }

    override fun onCommandRecordRemoved(script: ScriptCommand) {
        clearTrackView()
    }

    private fun removeView(view: ScriptItemView) {
        childViews.remove(view)
        view.onViewRemoved()
        invalidate()
    }

    private fun removeAllView() {
        childViews.clear()
        childViews.forEach {
            it.onViewRemoved()
        }
        postInvalidate()
    }

    private fun addView(view: ScriptItemView) {
        childViews.add(view)
        view.onViewAdded()
        postInvalidate()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!enableTouchable || isPlayRunning) return false
        event?.run {
            mScriptTouchEventHelper.onTouchEvent(event)
        }
        if (event?.action == MotionEvent.ACTION_UP || event?.action == MotionEvent.ACTION_CANCEL) {
            checkScrollState()
        }

        return true
    }

    private fun checkScrollState() {
        if (isScrolling) {
            isScrolling = false
            if (pointsMap.size == 1) {
                val points = pointsMap[0] ?: mutableListOf()
                val cmd = CmdScroll.createCommand(points, pointsTimeMap[0] ?: mutableListOf())
                addAndExecuteCommand(cmd)
            } else {
                val cmd =
                    CmdScrollMultiple.createCommand(pointsMap, pointsTimeMap, pointsStartTimeMap)
                addAndExecuteCommand(cmd)
            }
        }

    }

    fun addAndExecuteCommand(cmd: ScriptCommand) {
        if (!isPlayRunning) {
            ScriptRecordHelper.instance.addCommand(cmd)
            resetDataView()
            ScriptInterpreter.getDefault().executeCommand(cmd, true)
            invalidate()
        }
    }

    override fun onInterpreterStart(cmd: ScriptCommand) {
        ScriptRecordManager.getRecordView()?.run {
            ScriptHelper.blockUntilViewReady(this) {
                resetToRunning()
                invalidate()
            }
        }
        ScriptThreadManager.delay(60)
    }

    override fun onInterpreterEnd(cmd: ScriptCommand) {
        ScriptRecordManager.getRecordView()?.run {
            ScriptHelper.blockUntilViewReady(this) {
                if (cmd is ScriptCommandRoot) {
                    resetToStop()
                } else {
                    resetToRecoding()
                }
                invalidate()
            }
        }
    }

    override fun onCommandExecuteBefore(cmd: ScriptCommand) {
        postInvalidate()
        if (ScriptManager.isNeedUpdate()) {
            ScriptHelper.runInMain {
                ScriptManager.updateViewLayout()
            }
        }
    }

    override fun onCommandExecuteAfter(cmd: ScriptCommand) {
        postInvalidate()
    }

    private fun resetToRunning() {
        isPlayRunning = true
        enableChildDraw = false
        enableTouchable = false
    }

    private fun resetToRecoding() {
        isPlayRunning = false
        enableTouchable = true
        enableChildDraw = true
    }

    private fun resetToStop() {
        isPlayRunning = false
        enableTouchable = false
        enableChildDraw = false
    }
}