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
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.utils.GlobalApp
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdScrollMultiple, name = "scrollMultiple")
class CmdScrollMultiple : ScriptCommand(), ScriptRegularInterface {

    var matchPattern = """scrollMultiple\s+(.*)"""

    var actualPoints = mutableMapOf<Int, MutableList<Point>>()

    var pointList = mutableListOf<Int>()

    private var pointMap = mutableMapOf<Int, MutableList<PointF>>()

    var timeMap = mutableMapOf<Int, MutableList<Long>>()

    var startTimeMap = mutableMapOf<Int, Long>()

    var duration = 1000L

    override fun onExecute(): CmdExecuteResult {
        ScriptThreadManager.delay(ScriptConst.Cmd_Click_Default)
        updateActualPoints()
        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this)
        ScriptEventHelper.get().performScrollMultiple(actualPoints, timeMap, startTimeMap, duration)
        ScriptThreadManager.delay(ScriptConst.Cmd_Click_Default)
        return CmdExecuteResult.success()
    }

    fun updateActualPoints() {
        actualPoints.clear()
        pointMap.forEach {
            actualPoints[it.key] = ScriptCoordinateAdapter.get().toRealPoints(it.value)
        }
    }

    private fun getCommandDuration() = duration + ScriptConst.Cmd_Click_Default

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_scroll_multiple)!!

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_scroll_multiple)!!

    override fun getCommand() = "${cmdPrefix()} ${getParams()}"

    private fun getParams(): String {
        val keys = pointMap.keys
        keys.map { key ->
            "${getPointString(pointMap[key]!!)} ${getTimeString(timeMap[key]!!)} ${startTimeMap[key]}"
        }.let {
            return pointList.joinToString(",") + " " + it.joinToString(" ")
        }
    }

    private fun getPointString(points: List<PointF>): String {
        return points.joinToString(",") { "${it.x},${it.y}" }
    }

    private fun getTimeString(times: List<Long>): String {
        return times.joinToString(",")
    }


    override fun parseCmd(cmd: String) {
        if (Regex(matchPattern).matches(cmd)) {
            val r: Pattern = Pattern.compile(matchPattern)
            val m: Matcher = r.matcher(cmd)
            if (m.find()) {
                val list = m.group(0)?.split(" ")?.toMutableList() ?: mutableListOf()
                pointList.clear()
                list.removeFirst()
                list[0].split(",").forEach {
                    pointList.add(it.toInt())
                }
                list.removeFirst()
                var index = 0
                for (i in list.indices) {
                    if (i % 3 == 0) {
                        pointMap[pointList[index]] = mutableListOf()
                        val ps = list[i].split(",")
                            .filter { it.isNotBlank() }.map { it.toFloat() }
                            .toMutableList()
                        for (j in ps.indices step 2) {
                            pointMap[pointList[index]]?.add(
                                PointF(
                                    ps[j],
                                    ps.getOrNull(j + 1) ?: 0f
                                )
                            )
                        }
                        index++
                    }

                    if (i % 3 == 1) {
                        timeMap[pointList[index - 1]] = mutableListOf()
                        val ts = list[i].split(",")
                            .filter { it.isNotBlank() }.map { it.toFloat() }
                            .toMutableList()
                        for (j in ts.indices) {
                            timeMap[pointList[index - 1]]?.add(ts[j].toLong())
                        }
                    }

                    if (i % 3 == 2) {
                        startTimeMap[pointList[index - 1]] = list[i].toLong()
                    }

                }
                duration = 0L
                timeMap.forEach {
                    it.value.forEach { t ->
                        duration += t
                    }
                }
                updateActualPoints()
            }
        }
    }

    override fun getCommandIcon() = R.drawable.ic_fingger

    override fun matchCmd(cmd: String) = Regex(matchPattern).matches(cmd)

    override fun getNormalizedActiveArea(): RectF {
        val points = pointMap.flatMap { it.value }
        val minLeft = ScriptCoordinateAdapter.get().toRealX(points.minOf { it.x })
        val minTop = ScriptCoordinateAdapter.get().toRealY(points.minOf { it.y })
        val maxRight = ScriptCoordinateAdapter.get().toRealX(points.maxOf { it.x })
        val maxBottom = ScriptCoordinateAdapter.get().toRealY(points.maxOf { it.y })
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
            pointMap: MutableMap<Int, MutableList<Point>>,
            timeMap: MutableMap<Int, MutableList<Long>>,
            startTimeMap: MutableMap<Int, Long>
        ) = CmdScrollMultiple().apply {
            this.pointMap = mutableMapOf()
            this.timeMap = mutableMapOf()
            this.actualPoints = mutableMapOf()
            this.pointList = pointMap.keys.toMutableList()
            pointMap.forEach { it ->
                this.pointMap[it.key] = mutableListOf()
                it.value.map { ScriptCoordinateAdapter.get().toNormalizedPoint(it.x, it.y) }
                    .forEach { p ->
                        this.pointMap[it.key]?.add(p)
                    }
            }
            pointMap.forEach {
                this.actualPoints[it.key] = mutableListOf()
                it.value.forEach { p ->
                    this.actualPoints[it.key]?.add(p)
                }
            }
            timeMap.forEach {
                this.timeMap[it.key] = mutableListOf()
                it.value.forEach { t ->
                    this.timeMap[it.key]?.add(t)
                }
            }
            startTimeMap.forEach {
                this.startTimeMap[it.key] = it.value
            }

            var dur = 0L
            timeMap.firstNotNullOf {
                it.value.forEach {
                    dur += it
                }
            }
            this.duration = dur
            this.updateActualPoints()
        }
    }
}