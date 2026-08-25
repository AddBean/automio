// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.preview.impl

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Rect
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdReadScreenText
import com.hive.script.cmd.CmdReadViewText
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.views.preview.ScriptItemView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/8/21
 */
class ScriptItemReadView : ScriptItemView() {

    private var targetRectList: List<Rect>? = null

    private var mCurrentValue: Float = 0f

    var mAnimRunning = false

    override fun onDraw(canvas: Canvas) {
        if (mAnimRunning) {
            targetRectList?.forEach {
                ScriptCommonDrawer.drawRect(canvas, it, (mCurrentValue * 255).toInt())
            }
        }
    }

    override fun onExecuteUpdate(type: Int, cmd: ScriptCommand, obj: Any?) {
        mAnimRunning = true
        setValues(command)
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = ScriptConst.Cmd_Default_Base
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

    private fun setValues(command: ScriptCommand?) {
        when (command) {
            is CmdReadViewText -> {
                targetRectList = command.targetNodeList.map {
                    val rect = Rect()
                    it?.getBoundsInScreen(rect)
                    rect
                }
            }

            is CmdReadScreenText -> {
                val rect = ScriptCommonUtils.covertToScreenRect(command.limitRect)
                targetRectList = mutableListOf(rect)
            }
        }


    }
}