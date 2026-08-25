// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.graphics.Point
import android.graphics.RectF
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdPinchZoom, name = "pinchZoom")
class CmdPinchZoom : ScriptCommand(), ScriptRegularInterface {

    var action = ACTION_SCALE_OUT

    var duration = ScriptConst.Cmd_Click_Scale_Default

    private var normalizedX1 = 0f

    private var normalizedY1 = 0f

    private var normalizedX2 = 0f

    private var normalizedY2 = 0f

    private var normalizedCX = 0f

    private var normalizedCY = 0f

    var actualX1 = 0

    var actualY1 = 0

    var actualX2 = 0

    var actualY2 = 0

    var actualCX = 0

    var actualCY = 0

    var actualPoint1 = Point()

    var actualPoint2 = Point()

    var actualPointC = Point()

    val stepCount = 40

    override fun onExecute(): CmdExecuteResult {
        updateActualPoints()
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Bias)
        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this)
        ScriptEventHelper.get().performScale(mutableListOf<Point>().apply {
            addPoints(this, actualPointC, actualPoint1)
        }, mutableListOf<Point>().apply {
            addPoints(this, actualPointC, actualPoint2)
        }, duration)
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Bias)
        return CmdExecuteResult.success()
    }

    fun getExecuteDuration(): Long {
        return duration + ScriptConst.Cmd_Default_Bias * 2
    }

    fun updateActualPoints() {
        actualX1 = ScriptCoordinateAdapter.get().toRealX(normalizedX1)
        actualY1 = ScriptCoordinateAdapter.get().toRealY(normalizedY1)
        actualX2 = ScriptCoordinateAdapter.get().toRealX(normalizedX2)
        actualY2 = ScriptCoordinateAdapter.get().toRealY(normalizedY2)
        actualCX = ScriptCoordinateAdapter.get().toRealX(normalizedCX)
        actualCY = ScriptCoordinateAdapter.get().toRealY(normalizedCY)
        actualPoint1 = Point(actualX1, actualY1)
        actualPoint2 = Point(actualX2, actualY2)
        actualPointC = Point(actualCX, actualCY)
    }


    private fun addPoints(points: MutableList<Point>, pc: Point, p: Point) {
        points.run {
            if (action == ACTION_SCALE_IN) {
                add(p)
                for (i in 1 until stepCount) {
                    val x = p.x + (pc.x - p.x) * i / stepCount.toFloat()
                    val y = p.y + (pc.y - p.y) * i / stepCount.toFloat()
                    add(Point(x.toInt(), y.toInt()))
                }
                add(pc)
            } else {
                add(pc)
                for (i in 1 until stepCount) {
                    val x = pc.x + (p.x - pc.x) * i / stepCount.toFloat()
                    val y = pc.y + (p.y - pc.y) * i / stepCount.toFloat()
                    add(Point(x.toInt(), y.toInt()))
                }
                add(p)
            }
        }
    }

    private fun getCommandDuration() = duration


    override fun getCommand() =
        "${cmdPrefix()} action=$action duration=$duration from=\"$normalizedX1,$normalizedY1\" to=\"$normalizedX2,$normalizedY2\" center=\"$normalizedCX,$normalizedCY\""


    override fun getCommandName() = when (action) {
        ACTION_SCALE_OUT -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_scale_out)
        ACTION_SCALE_IN -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_scale_in)
        else -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_scale_in)
    }


    override fun getCommandDescribe() = getCommandName()

    override fun getCommandIcon() = R.drawable.ic_touch_small

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        action = p["action"] ?: ACTION_SCALE_OUT
        duration = p["duration"]?.toLongOrNull() ?: 500L
        p["from"]?.split(",")?.let { parts ->
            if (parts.size >= 2) {
                normalizedX1 = parts[0].trim().toFloatOrNull() ?: normalizedX1
                normalizedY1 = parts[1].trim().toFloatOrNull() ?: normalizedY1
            }
        }
        p["to"]?.split(",")?.let { parts ->
            if (parts.size >= 2) {
                normalizedX2 = parts[0].trim().toFloatOrNull() ?: normalizedX2
                normalizedY2 = parts[1].trim().toFloatOrNull() ?: normalizedY2
            }
        }
        p["center"]?.split(",")?.let { parts ->
            if (parts.size >= 2) {
                normalizedCX = parts[0].trim().toFloatOrNull() ?: normalizedCX
                normalizedCY = parts[1].trim().toFloatOrNull() ?: normalizedCY
            }
        }
        updateActualPoints()
    }

    override fun getNormalizedActiveArea(): RectF {
        val r = RectF()
        val minLeft = minOf(normalizedX1, normalizedX2, normalizedCX)
        val maxRight = maxOf(normalizedX1, normalizedX2, normalizedCX)
        val minTop = minOf(normalizedY1, normalizedY2, normalizedCY)
        val maxBottom = maxOf(normalizedY1, normalizedY2, normalizedCY)
        val size = GlobalApp.DP * 20f
        r.left = ScriptCoordinateAdapter.get().toRealX(minLeft) - size
        r.top = ScriptCoordinateAdapter.get().toRealY(minTop) - size
        r.right = ScriptCoordinateAdapter.get().toRealX(maxRight) + size
        r.bottom = ScriptCoordinateAdapter.get().toRealY(maxBottom) + size
        return ScriptCommonUtils.convertToNormalization(r)
    }


    fun updateNormalizedXY() {
        normalizedX1 = ScriptCoordinateAdapter.get().toNormalizedX(actualX1)
        normalizedY1 = ScriptCoordinateAdapter.get().toNormalizedY(actualY1)
        normalizedX2 = ScriptCoordinateAdapter.get().toNormalizedX(actualX2)
        normalizedY2 = ScriptCoordinateAdapter.get().toNormalizedY(actualY2)
        normalizedCX = ScriptCoordinateAdapter.get().toNormalizedX(actualCX)
        normalizedCY = ScriptCoordinateAdapter.get().toNormalizedY(actualCY)
    }

    companion object {

        val ACTION_SCALE_OUT = "out"

        val ACTION_SCALE_IN = "in"

        fun createCommand(
            action: String,
            duration: Long,
            x1: Int,
            y1: Int,
            x2: Int,
            y2: Int,
            cx: Int,
            cy: Int
        ) = CmdPinchZoom().apply {
            this.action = action
            this.duration = duration
            this.normalizedX1 = ScriptCoordinateAdapter.get().toNormalizedX(x1)
            this.normalizedY1 = ScriptCoordinateAdapter.get().toNormalizedY(y1)
            this.normalizedX2 = ScriptCoordinateAdapter.get().toNormalizedX(x2)
            this.normalizedY2 = ScriptCoordinateAdapter.get().toNormalizedY(y2)
            this.normalizedCX = ScriptCoordinateAdapter.get().toNormalizedX(cx)
            this.normalizedCY = ScriptCoordinateAdapter.get().toNormalizedY(cy)
            this.actualCX = cx
            this.actualCY = cy
            this.actualX1 = x1
            this.actualY1 = y1
            this.actualX2 = x2
            this.actualY2 = y2
            updateActualPoints()
        }
    }
}