// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.graphics.RectF
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdLongClick, name = "press")
class CmdPress : ScriptCommand(), ScriptRegularInterface {
    private var x: Float = 0f
    private var y: Float = 0f
    var actualX: Int = 0
    var actualY: Int = 0
    var duration = ScriptConst.Cmd_Long_Click_Default

    override fun onExecute(): CmdExecuteResult {
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Bias)
        actualX = ScriptCoordinateAdapter.get().toRealX(x)
        actualY = ScriptCoordinateAdapter.get().toRealY(y)
        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this)
        ScriptEventHelper.get().performPress(actualX, actualY, duration)
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Bias)
        return CmdExecuteResult.success()
    }

    private fun getCommandDuration() = duration + ScriptConst.Cmd_Default_Bias

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_longclick)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_longclick, x, y)

    override fun getCommand() = "${cmdPrefix()} x=$x y=$y duration=$duration"


    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        x = p["x"]?.toFloatOrNull() ?: 0f
        y = p["y"]?.toFloatOrNull() ?: 0f
        duration = p["duration"]?.toLongOrNull() ?: ScriptConst.Cmd_Long_Click_Default
    }

    override fun getCommandIcon() = R.drawable.ic_sc_press

    override fun getNormalizedActiveArea(): RectF {
        val r = RectF()
        val actualX = ScriptCoordinateAdapter.get().toRealX(x)
        val actualY = ScriptCoordinateAdapter.get().toRealY(y)
        val size = GlobalApp.DP * 10f
        r.left = actualX - size
        r.top = actualY - size
        r.right = actualX + size
        r.bottom = actualY + size
        return ScriptCommonUtils.convertToNormalization(r)
    }

    companion object {
        fun createCommand(x: Int, y: Int, duration: Long) = CmdPress().apply {
            this.x = ScriptCoordinateAdapter.get().toNormalizedX(x)
            this.y = ScriptCoordinateAdapter.get().toNormalizedY(y)
            this.duration = duration
        }
    }
}