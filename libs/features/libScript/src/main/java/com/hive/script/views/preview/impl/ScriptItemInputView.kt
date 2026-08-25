// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.preview.impl

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdInput
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.views.preview.ScriptItemView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/8/21
 */
class ScriptItemInputView : ScriptItemView() {

    private var mActualPoint: Point? = null
    private var targetRect: Rect? = null

    private var mCurrentValue: Float = 0f

    var mAnimRunning = false

    override fun onDraw(canvas: Canvas) {
        if (mAnimRunning) {
            targetRect?.run {
                ScriptCommonDrawer.drawRect(canvas, this, (mCurrentValue * 255).toInt())
            }
            mActualPoint?.run {
                ScriptCommonDrawer.drawClickDot(canvas, this)
            }
        }
    }

    override fun onExecuteUpdate(type: Int, cmd: ScriptCommand, obj: Any?) {
        mAnimRunning = true
        mActualPoint = obj as Point?
        val cmd = command as CmdInput
        targetRect = cmd.resultRect
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = 200L
        anim.addUpdateListener {
            mCurrentValue = it.animatedValue as Float
            parentView?.postInvalidate()
        }
        anim.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator) {

            }

            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                mAnimRunning = false
            }

            override fun onAnimationCancel(animation: Animator) {
                mAnimRunning = false
            }

        })
        anim.start()
    }
}