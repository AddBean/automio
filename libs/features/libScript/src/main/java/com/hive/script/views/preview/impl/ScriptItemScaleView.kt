// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.preview.impl

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.PointF
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdPinchZoom
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.views.preview.ScriptItemView

/**
 *
 * @author jiadou
 * @date 6/9/21
 */
class ScriptItemScaleView : ScriptItemView() {

    private var mAnimRunning = false
    private var mAnimProgress = 0f
    private var point1 = PointF()
    private var point2 = PointF()

    override fun onDraw(canvas: Canvas) {
        command as CmdPinchZoom
        if (mAnimRunning) {
            ScriptCommonDrawer.drawCircle(canvas, point1.x, point1.y, 255)
            ScriptCommonDrawer.drawCircle(canvas, point2.x, point2.y, 255)
        }
    }

    override fun onExecuteUpdate(type: Int, cmd: ScriptCommand, obj: Any?) {
        super.onExecuteUpdate(type, cmd, obj)
        mAnimRunning = true
        parentView?.postInvalidate()
        val cmd = command as CmdPinchZoom
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = cmd.duration
        mAnimProgress = 0f
        anim.addUpdateListener {
            mAnimRunning = true
            mAnimProgress = it.animatedValue as Float
            if (cmd.action == CmdPinchZoom.ACTION_SCALE_IN) {
                point1.x = (cmd.actualPointC.x - cmd.actualPoint1.x) * mAnimProgress + cmd.actualPoint1.x
                point1.y = (cmd.actualPointC.y - cmd.actualPoint1.y) * mAnimProgress + cmd.actualPoint1.y
                point2.x = (cmd.actualPointC.x - cmd.actualPoint2.x) * mAnimProgress + cmd.actualPoint2.x
                point2.y = (cmd.actualPointC.y - cmd.actualPoint2.y) * mAnimProgress + cmd.actualPoint2.y
            } else {
                point1.x = (cmd.actualPoint1.x - cmd.actualPointC.x) * mAnimProgress + cmd.actualPointC.x
                point1.y = (cmd.actualPoint1.y - cmd.actualPointC.y) * mAnimProgress + cmd.actualPointC.y
                point2.x = (cmd.actualPoint2.x - cmd.actualPointC.x) * mAnimProgress + cmd.actualPointC.x
                point2.y = (cmd.actualPoint2.y - cmd.actualPointC.y) * mAnimProgress + cmd.actualPointC.y
            }
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