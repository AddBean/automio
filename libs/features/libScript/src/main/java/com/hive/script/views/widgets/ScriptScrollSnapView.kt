// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdScroll
import com.hive.script.cmd.CmdScrollMultiple
import com.hive.script.extensions.copyPoints
import com.hive.script.extensions.scalePointInRect
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/23/21
 */
class ScriptScrollSnapView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var mPointsMap: MutableMap<Int, List<Point>>? = mutableMapOf()

    private var mPathMap: MutableMap<Int, Path>? = mutableMapOf()

    private var mCmdScroll: CmdScroll? = null

    private var mCmdScrollMultiple: CmdScrollMultiple? = null

    private val canvasRect = Rect()

    private var mPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = GlobalApp.dp2px(2)
        color = Color.BLACK
    }

    fun loadCmdScroll(cmd: CmdScroll) {
        mPointsMap?.clear()
        mPathMap?.clear()
        mCmdScroll = cmd
        mPointsMap?.set(0, mCmdScroll!!.actualPoints.copyPoints())
        updatePath()
        invalidate()
    }

    fun loadCmdScrollMultiple(cmd: CmdScrollMultiple) {
        mPointsMap?.clear()
        mPathMap?.clear()
        mCmdScrollMultiple = cmd
        mCmdScrollMultiple?.actualPoints?.forEach {
            mPointsMap?.set(it.key, it.value.copyPoints())
        }
        updatePath()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasRect.set(0, 0, w, h)
        mCmdScrollMultiple?.actualPoints?.forEach {
            mPointsMap?.set(it.key, it.value.copyPoints())
        }
        mCmdScroll?.run {
            mPointsMap?.set(0, actualPoints.copyPoints())
        }
        updatePath()
    }

    private fun updatePath() {
        if (width == 0 || height == 0) return
        val list2 = mutableListOf<List<Point>>()
        val listMap = mutableListOf<Pair<Int, Int>>()
        var index = 0
        mPointsMap?.forEach {
            list2.add(it.value)
            listMap.add(index to it.key)
            index += it.value.size
        }
        var key = 0
        var currentIndex = 0
        list2.flatten().scalePointInRect(canvasRect).forEachIndexed { index, it ->
            if (listMap.size > currentIndex && listMap[currentIndex].first == index) {
                key = listMap[currentIndex].second
                mPathMap?.set(key, Path())
                mPathMap?.get(key)?.reset()
                mPathMap?.get(key)?.moveTo(
                    it.x.toFloat(),
                    it.y.toFloat()
                )
                currentIndex++
            } else {
                mPathMap?.get(key)?.lineTo(
                    it.x.toFloat(),
                    it.y.toFloat()
                )
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvasRect.set(0, 0, measuredWidth, measuredHeight)
        mPathMap?.forEach {
            mPaint.color = ScriptConst.colorPickArray[it.key % ScriptConst.colorPickArray.size]
            canvas.drawPath(it.value, mPaint)
        }

    }
}