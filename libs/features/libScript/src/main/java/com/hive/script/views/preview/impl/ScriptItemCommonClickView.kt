// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.preview.impl

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdClickColor
import com.hive.script.cmd.CmdClickImage
import com.hive.script.cmd.CmdClickText
import com.hive.script.cmd.CmdClickView
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.views.preview.ScriptItemView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/8/21
 */
class ScriptItemCommonClickView : ScriptItemView() {


    private var actualPoint: Point? = null

    private var resultPointArray: Array<Point>? = null

    private var resultRectArray: Array<Rect>? = null

    private var currentValue: Float = 0f

    private var limitRect: Rect? = null

    private var dragRect = Rect()

    private var clickType: String = ScriptClickActionHelper.ACTION_CLICK

    private var animRunning = false

    private var drawPath = Path()

    override fun onDraw(canvas: Canvas) {
        if (animRunning) {
            if (clickType == ScriptClickActionHelper.ACTION_DRAG) {
                ScriptCommonDrawer.drawRect(canvas, dragRect, ((1 - currentValue) * 255).toInt())
            } else {
                var index = 0
                val maxGrad = 4
                limitRect?.run {
                    ScriptCommonDrawer.drawLimitRect(canvas, this, (currentValue * 255).toInt())
                }
                resultRectArray?.forEach {
                    val alpha = (255 - (index / maxGrad.toFloat()) * 230).toInt()
                    ScriptCommonDrawer.drawRect(canvas, it, alpha)
                    index++
                    if (index > maxGrad) index = maxGrad
                }
                resultPointArray?.run {
                    drawPath.reset()
                    resultPointArray?.first()?.run {
                        ScriptCommonDrawer.drawCircle(canvas, x.toFloat(), y.toFloat())
                    }
                }

                actualPoint?.run {
                    ScriptCommonDrawer.drawClickDot(canvas, this)
                }
            }
        }
    }

    override fun onExecuteUpdate(type: Int, cmd: ScriptCommand, obj: Any?) {
        animRunning = true
        actualPoint = obj as Point?
        limitRect = ScriptCommonUtils.covertToScreenRect(cmd.limitRect)
        setValues(cmd)
        actualPoint?.run {
            //中心点移到actualPoint
            dragRect.offset(this.x - dragRect.centerX(), this.y - dragRect.centerY())
        }
        val distanceX = cmd.dragVector.toRealDiffX()
        val distanceY = cmd.dragVector.toRealDiffY()
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = ScriptClickActionHelper.getDurationByCmd(clickType, cmd)
        parentView?.postInvalidate()
        val dragX = dragRect.centerX()
        val dragY = dragRect.centerY()
        var noChangedProgress = 0f
        if (cmd.dragType == 0) {
            noChangedProgress = (ScriptConst.Cmd_Scroll_Click_Default / anim.duration.toFloat())
        }
        val dragRectOriginal = Rect(dragRect)
        anim.addUpdateListener {
            currentValue = it.animatedValue as Float
            val currentProgress = if (currentValue < noChangedProgress) {
                0f
            } else {
                //后半部分从0开始到1f
                (currentValue - noChangedProgress) / (1 - noChangedProgress)
            }
            val moveX = dragX + distanceX * (currentProgress)
            val moveY = dragY + distanceY * (currentProgress)
            dragRect.set(dragRectOriginal)
            dragRect.offset((moveX - dragX).toInt(), (moveY - dragY).toInt())
            parentView?.invalidate()
        }
        anim.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator) {

            }

            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                animRunning = false
            }

            override fun onAnimationCancel(animation: Animator) {
                animRunning = false
            }

        })
        anim.start()
    }


    private fun setValues(cmd: ScriptCommand) {
        when (cmd) {
            is CmdClickImage -> {
                cmd.resultRect?.let {
                    resultRectArray = arrayOf(it)
                    dragRect.set(it)
                }
                clickType = cmd.action
            }

            is CmdClickColor -> {
                resultPointArray = cmd.resultPoints
                resultRectArray = cmd.resultRects
                clickType = cmd.action
                resultRectArray?.firstOrNull()?.run {
                    dragRect.set(this)
                }
            }

            is CmdClickText -> {
                cmd.targetRect?.let {
                    resultRectArray = arrayOf(it)
                    dragRect.set(it)
                }
                clickType = cmd.action
            }

            is CmdClickView -> {
                resultRectArray = arrayOf(cmd.resultRect)
                dragRect.set(cmd.resultRect)
                clickType = cmd.action
            }

            else -> {

            }
        }
    }
}