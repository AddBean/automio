// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base

import android.graphics.Point
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.cmd.CmdClickColor
import com.hive.script.cmd.CmdClickImage
import com.hive.script.cmd.CmdClickText
import com.hive.script.cmd.CmdClickView
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.views.manager.ScriptManager
import com.hive.utils.thread.UIHandlerUtils
import com.hive.script.utils.ScriptHelper
/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/15/21
 */
object ScriptClickActionHelper {

    const val ACTION_CLICK = "click"

    const val ACTION_PRESS = "press"

    const val ACTION_FAST_CLICK = "fast_click"

    const val ACTION_BREAK = "break"

    const val ACTION_DRAG = "drag"

    fun performAction(point: Point, action: String, cmd: ScriptCommand) {
        var pressDuration = ScriptConst.Cmd_Long_Click_Default
        var fastCount = 5
        var fastGap = 200L
        var random = 0
        when (cmd) {
            is CmdClickView -> {
                pressDuration = cmd.pressDuration
                fastCount = cmd.fastCount
                fastGap = cmd.fastGap
                random = cmd.random
            }

            is CmdClickImage -> {
                pressDuration = cmd.pressDuration
                fastCount = cmd.fastCount
                fastGap = cmd.fastGap
                random = cmd.random
            }

            is CmdClickColor -> {
                pressDuration = cmd.pressDuration
                fastCount = cmd.fastCount
                fastGap = cmd.fastGap
                random = cmd.random
            }

            is CmdClickText -> {
                pressDuration = cmd.pressDuration
                fastCount = cmd.fastCount
                fastGap = cmd.fastGap
                random = cmd.random
            }
        }

        val clickX =
            ScriptCommonUtils.getRandomValue(point.x, random) + cmd.offsetVector.toRealDiffX()
        val clickY =
            ScriptCommonUtils.getRandomValue(point.y, random) + cmd.offsetVector.toRealDiffY()
        when (action) {
            ACTION_CLICK -> {
                ScriptInterpreterObserver.notifyCommandExecuteEvent(0, cmd, Point(clickX, clickY))
                ScriptEventHelper.get().performClick(
                    clickX,
                    clickY
                )
                ScriptThreadManager.delay(getDurationByCmd(action, cmd))
            }

            ACTION_PRESS -> {
                ScriptInterpreterObserver.notifyCommandExecuteEvent(0, cmd, Point(clickX, clickY))
                ScriptEventHelper.get().performPress(
                    clickX,
                    clickY,
                    pressDuration
                )
                ScriptThreadManager.delay(getDurationByCmd(action, cmd))
            }

            ACTION_FAST_CLICK -> {
                for (i in 0 until fastCount) {
                    val cX = ScriptCommonUtils.getRandomValue(point.x, random)
                    val cY = ScriptCommonUtils.getRandomValue(point.y, random)
                    ScriptInterpreterObserver.notifyCommandExecuteEvent(0, cmd, Point(cX, cY))
                    ScriptEventHelper.get().performClick(
                        cX,
                        cY
                    )
                    ScriptThreadManager.delay(fastGap)
                }
                ScriptThreadManager.delay(getDurationByCmd(action, cmd))
            }

            ACTION_DRAG -> {

                val dragDuration = cmd.dragDuration

                val dragType = cmd.dragType

                val dragVector = cmd.dragVector

                val dragPressDuration = cmd.dragPressDuration

                val startPoint = Point(clickX, clickY)

                val endPoint =
                    Point(clickX + dragVector.toRealDiffX(), clickY + dragVector.toRealDiffY())

                ScriptInterpreterObserver.notifyCommandExecuteEvent(0, cmd, startPoint)

                when (dragType) {
                    0 -> {//长按后滑动
                        ScriptEventHelper.get().performLongPressThenScroll(
                            startPoint,
                            endPoint,
                            dragPressDuration,
                            dragDuration
                        )
                    }

                    1 -> {//直接滑动
                        ScriptEventHelper.get().performScroll(
                            mutableListOf(
                                startPoint,
                                endPoint
                            ), mutableListOf(dragDuration),
                            dragDuration
                        )
                    }
                }
            }

            ACTION_BREAK -> {
                if (!ScriptInterpreter.getDefault().isRecording()) {
                    ScriptHelper.runInMain {
                        ScriptManager.stopPlay()
                    }
                }
                ScriptThreadManager.delay(getDurationByCmd(action, cmd))
            }
        }
    }

    fun getDurationByCmd(clickType: String, cmd: ScriptCommand): Long {
        return when (clickType) {

            ACTION_CLICK -> ScriptConst.Cmd_Click_Default

            ACTION_PRESS -> when (cmd) {
                is CmdClickImage -> cmd.pressDuration
                is CmdClickText -> cmd.pressDuration
                is CmdClickColor -> cmd.pressDuration
                is CmdClickView -> cmd.pressDuration
                else -> ScriptConst.Cmd_Long_Click_Default
            }

            ACTION_FAST_CLICK -> ScriptConst.Cmd_Click_Default

            ACTION_DRAG -> when (cmd.dragType) {
                0 -> cmd.dragDuration + cmd.dragPressDuration
                1 -> cmd.dragDuration
                else -> cmd.dragDuration
            }

            else -> ScriptConst.Cmd_Click_Default
        } + ScriptConst.Cmd_Default_Bias
    }
}