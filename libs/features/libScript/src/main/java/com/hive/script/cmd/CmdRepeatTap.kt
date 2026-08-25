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
import com.hive.script.setting.ScriptSetting
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.GlobalApp

@AutoCmdRegister(type = IDS.CmdRepeatTap, name = "repeatTap")
class CmdRepeatTap : ScriptCommand(), ScriptRegularInterface {
    private var x = 0f

    private var y = 0f

    var random = 0

    var actualX = 0

    var actualY = 0

    var count = 1

    var gap = ScriptConst.Cmd_Fast_Click_Gap_Default

    override fun onExecute(): CmdExecuteResult {
        for (i in 0 until count) {

            actualX = ScriptCoordinateAdapter.get().toRealX(x)
            actualY = ScriptCoordinateAdapter.get().toRealY(y)
            actualX = ScriptCommonUtils.getRandomValue(actualX, random)
            actualY = ScriptCommonUtils.getRandomValue(actualY, random)
            ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this, i)
            ScriptEventHelper.get().performClick(actualX, actualY)
            ScriptThreadManager.delay(gap - ScriptConst.Cmd_Click_Default - 10L)
        }
        return CmdExecuteResult.success()
    }

    fun getExecuteDuration(): Long {
        return gap * count
    }


    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_fastclick)

    override fun getCommandDescribe() =
        GlobalApp.getString(com.hive.i8n.R.string.cmd_des_fastclick, count)

    override fun getCommandIcon() = R.drawable.ic_fast_click

    override fun getCommand() = "${cmdPrefix()} x=$x y=$y random=$random count=$count gap=$gap"

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        x = p["x"]?.toFloatOrNull() ?: 0f
        y = p["y"]?.toFloatOrNull() ?: 0f
        random = p["random"]?.toIntOrNull() ?: 0
        count = p["count"]?.toIntOrNull() ?: 1
        gap = p["gap"]?.toLongOrNull() ?: ScriptConst.Cmd_Fast_Click_Gap_Default
        actualX = ScriptCoordinateAdapter.get().toRealX(x)
        actualY = ScriptCoordinateAdapter.get().toRealY(y)
    }

    override fun getNormalizedActiveArea(): RectF {
        val r = RectF()
        val actualX = ScriptCoordinateAdapter.get().toRealX(x)
        val actualY = ScriptCoordinateAdapter.get().toRealY(y)
        val size = GlobalApp.DP * random.toFloat()
        r.left = actualX - size
        r.top = actualY - size
        r.right = actualX + size
        r.bottom = actualY + size
        return ScriptCommonUtils.convertToNormalization(r)
    }

    companion object {
        fun createCommand(x: Int, y: Int, count: Int, gap: Long) = CmdRepeatTap().apply {
            this.x = ScriptCoordinateAdapter.get().toNormalizedX(x)
            this.y = ScriptCoordinateAdapter.get().toNormalizedY(y)
            this.count = count
            this.gap = gap
            this.random =
                if (ScriptSetting.script_setting_anti_detect) ScriptConst.Default_Anti_Detect_Radius_Value else 0
        }
    }
}