// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.preview.impl

import android.animation.ValueAnimator
import android.graphics.Canvas
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdPinch
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.views.beans.PointVectorInt
import com.hive.script.views.preview.ScriptItemView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/9/21
 */
class ScriptItemMultipleView : ScriptItemView() {

    private var mActualVector: MutableList<PointVectorInt>? = null
    private var mAnimRunning = false
    private var mAnimProgress = 0f

    override fun onDraw(canvas: Canvas) {
        command as CmdPinch
        if (mAnimRunning) {
            mActualVector?.forEach {
                val tx = it.fromX + (it.toX - it.fromX) * mAnimProgress
                val ty = it.fromY + (it.toY - it.fromY) * mAnimProgress
                ScriptCommonDrawer.drawCircle(canvas, tx, ty, 255)
            }
        }
    }

    override fun onCommandUpdate(cmd: ScriptCommand?) {
        val cmdScroll = command as CmdPinch
        cmdScroll.updateActualPoints()
    }

    override fun onExecuteUpdate(type: Int, cmd: ScriptCommand, obj: Any?) {
        super.onExecuteUpdate(type, cmd, obj)
        mAnimRunning = true
        parentView?.postInvalidate()
        val cmd = command as CmdPinch
        cmd.calculationVectors()
        mActualVector = cmd.getActualVector()
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = cmd.duration
        mAnimProgress = 0f
        anim.addUpdateListener {
            mAnimRunning = true
            mAnimProgress = it.animatedValue as Float
            if (mAnimProgress == 1f) {
                mAnimRunning = false
            }
            parentView?.invalidate()

        }
        anim.start()
    }

    override fun onExecuteEnd(cmd: ScriptCommand) {
        mAnimRunning = false
        parentView?.postInvalidate()
    }


}