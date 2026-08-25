// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.graphics.Point
import android.graphics.PointF
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
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdScroll, name = "scroll")
class CmdScroll : ScriptCommand(), ScriptRegularInterface {

    private var pointFs = mutableListOf<PointF>()

    var times = mutableListOf<Long>()

    var duration = 1000L

    var actualPoints = mutableListOf<Point>()

    override fun onExecute(): CmdExecuteResult {
        ScriptThreadManager.delay(ScriptConst.Cmd_Click_Default)
        updateActualPoints()
        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this)
        ScriptEventHelper.get().performScroll(actualPoints, times, duration)
        ScriptThreadManager.delay(ScriptConst.Cmd_Click_Default)
        return CmdExecuteResult.success()
    }

    fun updateActualPoints() {
        actualPoints = ScriptCoordinateAdapter.get().toRealPoints(pointFs)
    }

    private fun getCommandDuration() = duration + ScriptConst.Cmd_Click_Default

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_scroll)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_scroll)

    override fun getCommand() = "${cmdPrefix()} points=\"${getPointString()}\" times=\"${getTimeString()}\""

    private fun getPointString(): String {
        return pointFs.joinToString(",") { "${it.x},${it.y}" }
    }

    private fun getTimeString(): String {
        return times.joinToString(",")
    }

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        pointFs.clear()
        times.clear()
        p["points"]?.split(",")?.let { ps ->
            for (i in ps.indices step 2) {
                if (i + 1 < ps.size) {
                    pointFs.add(PointF(ps[i].trim().toFloat(), ps[i + 1].trim().toFloat()))
                }
            }
        }
        duration = 0L
        p["times"]?.split(",")?.let { ts ->
            for (t in ts) {
                val v = t.trim().toLongOrNull() ?: 0L
                times.add(v)
                duration += v
            }
        }
        updateActualPoints()
    }

    override fun getNormalizedActiveArea(): RectF {
        val minLeft = ScriptCoordinateAdapter.get().toRealX(pointFs.minOf { it.x })
        val minTop = ScriptCoordinateAdapter.get().toRealY(pointFs.minOf { it.y })
        val maxRight = ScriptCoordinateAdapter.get().toRealX(pointFs.maxOf { it.x })
        val maxBottom = ScriptCoordinateAdapter.get().toRealY(pointFs.maxOf { it.y })
        val r = Rect()
        r.left = minLeft
        r.top = minTop
        r.right = maxRight
        r.bottom = maxBottom
        return ScriptCoordinateAdapter.get().toNormalizedRect(
            r
        )
    }

    override fun getCommandIcon() = R.drawable.sc_ic_scroll

    companion object {

        fun createCommand(points: List<Point>, times: List<Long>) = CmdScroll().apply {
            this.pointFs = mutableListOf()
            this.actualPoints = mutableListOf()

            this.times = mutableListOf()
            points.forEach {
                this.pointFs.add(ScriptCoordinateAdapter.get().toNormalizedPoint(it.x, it.y))
                this.actualPoints.add(it)
            }
            var dur = 0L
            times.forEach {
                this.times.add(it)
                dur += it
            }
            this.updateActualPoints()
            this.duration = dur
        }
    }
}