// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.views.beans.PointVectorInt
import com.hive.utils.GlobalApp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdPinch, name = "pinch")
class CmdPinch : ScriptCommand(), ScriptRegularInterface {
    var duration = ScriptConst.Cmd_Click_Multiple_Default

    private var x1 = 0f

    private var y1 = 0f

    private var x2 = 0f

    private var y2 = 0f

    var actualX1 = 0

    var actualY1 = 0

    var actualX2 = 0

    var actualY2 = 0

    var fingerDistance = 0

    var fingerCount = 0

    val stepCount = 40

    var mActualVector = mutableListOf<PointVectorInt>()

    override fun onExecute(): CmdExecuteResult {
        updateActualXY()
        calculationVectors()
        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this)
        val pLs = mutableListOf<MutableList<Point>>()
        mActualVector.forEach {
            val list = mutableListOf<Point>()
            addPoints(
                list,
                Point(it.fromX, it.fromY),
                Point(it.toX, it.toY)
            )
            pLs.add(list)
        }
        ScriptEventHelper.get().performMultipleScroll(pLs, duration)
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Bias)
        return CmdExecuteResult.success()
    }

    fun updateActualXY() {
        actualX1 = ScriptCoordinateAdapter.get().toRealX(x1)
        actualY1 = ScriptCoordinateAdapter.get().toRealY(y1)
        actualX2 = ScriptCoordinateAdapter.get().toRealX(x2)
        actualY2 = ScriptCoordinateAdapter.get().toRealY(y2)
    }

    fun updateNormalizedXY() {
        x1 = ScriptCoordinateAdapter.get().toNormalizedX(actualX1)
        y1 = ScriptCoordinateAdapter.get().toNormalizedY(actualY1)
        x2 = ScriptCoordinateAdapter.get().toNormalizedX(actualX2)
        y2 = ScriptCoordinateAdapter.get().toNormalizedY(actualY2)
    }

    private fun addPoints(points: MutableList<Point>, startP: Point, endP: Point) {
        points.run {
            add(startP)
            for (i in 1 until stepCount) {
                val x = startP.x + (endP.x - startP.x) * i / stepCount.toFloat()
                val y = startP.y + (endP.y - startP.y) * i / stepCount.toFloat()
                add(Point(x.toInt(), y.toInt()))
            }
            add(endP)
        }
    }

    private fun getCommandDuration() = duration

    fun getExecuteDuration(): Long {
        return duration + ScriptConst.Cmd_Default_Bias
    }

    override fun getCommand() = "${cmdPrefix()} fingers=$fingerCount gap=$fingerDistance duration=$duration from=\"$x1,$y1\" to=\"$x2,$y2\""

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_multiple)

    override fun getCommandDescribe() = getCommandName()

    override fun getCommandIcon() = R.drawable.ic_fingger


    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        fingerCount = p["fingers"]?.toIntOrNull() ?: 2
        fingerDistance = p["gap"]?.toIntOrNull() ?: 0
        duration = p["duration"]?.toLongOrNull() ?: ScriptConst.Cmd_Click_Multiple_Default
        p["from"]?.split(",")?.let { parts ->
            if (parts.size >= 2) {
                x1 = parts[0].toFloatOrNull() ?: x1
                y1 = parts[1].toFloatOrNull() ?: y1
            }
        }
        p["to"]?.split(",")?.let { parts ->
            if (parts.size >= 2) {
                x2 = parts[0].toFloatOrNull() ?: x2
                y2 = parts[1].toFloatOrNull() ?: y2
            }
        }
        calculationVectors()
        updateActualXY()
    }

    fun calculationVectors(): List<PointVectorInt> {
        mActualVector.clear()
        val xr1 = ScriptCoordinateAdapter.get().toRealX(x1)
        val yr1 = ScriptCoordinateAdapter.get().toRealY(y1)
        val xr2 = ScriptCoordinateAdapter.get().toRealX(x2)
        val yr2 = ScriptCoordinateAdapter.get().toRealY(y2)
        val degree = atan2((xr2 - xr1).toDouble(), (yr2 - yr1).toDouble())
        val leftDistance = fingerDistance * (fingerCount - 1) / 2
        for (i in 0 until fingerCount) {
            val vector = PointVectorInt()
            val dx = ((leftDistance - fingerDistance * i) * cos(degree))
            val dy = ((leftDistance - fingerDistance * i) * sin(degree))
            vector.fromX = (xr1 - dx).toInt()
            vector.fromY = (yr1 + dy).toInt()
            vector.toX = (xr2 - dx).toInt()
            vector.toY = (yr2 + dy).toInt()
            mActualVector.add(vector)
        }
        return mActualVector
    }

    fun getActualVector() = mActualVector

    fun updateActualPoints() {
        val pLs = mutableListOf<MutableList<Point>>()
        mActualVector.forEach {
            val list = mutableListOf<Point>()
            addPoints(
                list,
                Point(it.fromX, it.fromY),
                Point(it.toX, it.toY)
            )
            pLs.add(list)
        }
    }


    override fun getNormalizedActiveArea(): RectF {
        val points = mActualVector.map {
            mutableListOf(
                Point(it.fromX, it.fromY), Point(it.toX, it.toY)
            )
        }.toList().flatten()
        val minLeft = points.minOf { it.x }
        val minTop = points.minOf { it.y }
        val maxRight = points.maxOf { it.x }
        val maxBottom = points.maxOf { it.y }
        val r = Rect()
        r.left = minLeft
        r.top = minTop
        r.right = maxRight
        r.bottom = maxBottom
        return ScriptCoordinateAdapter.get().toNormalizedRect(
            r
        )
    }

    companion object {

        fun createCommand(
            fingerCount: Int,
            fingerDistance: Int,
            duration: Long,
            x1: Int,
            y1: Int,
            x2: Int,
            y2: Int
        ) = CmdPinch().apply {
            this.fingerDistance = fingerDistance
            this.fingerCount = fingerCount
            this.duration = duration
            this.x1 = ScriptCoordinateAdapter.get().toNormalizedX(x1)
            this.y1 = ScriptCoordinateAdapter.get().toNormalizedY(y1)
            this.x2 = ScriptCoordinateAdapter.get().toNormalizedX(x2)
            this.y2 = ScriptCoordinateAdapter.get().toNormalizedY(y2)
            this.actualX1 = x1
            this.actualY1 = y1
            this.actualX2 = x2
            this.actualY2 = y2
            this.calculationVectors()
        }
    }
}