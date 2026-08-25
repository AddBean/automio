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

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdClick, name = "click")
class CmdClick : ScriptCommand(), ScriptRegularInterface {
    private var x = 0f

    private var y = 0f

    var random = 0

    var actualX = 0

    var actualY = 0

    override fun onExecute() : CmdExecuteResult {
        actualX = ScriptCoordinateAdapter.get().toRealX(x)
        actualY = ScriptCoordinateAdapter.get().toRealY(y)
        actualX = ScriptCommonUtils.getRandomValue(actualX, random)
        actualY = ScriptCommonUtils.getRandomValue(actualY, random)
        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this)
        ScriptEventHelper.get().performClick(actualX, actualY)
        ScriptThreadManager.delay(ScriptConst.Cmd_Click_Default)
        return CmdExecuteResult.success()
    }

    override fun getCommand() = "${cmdPrefix()} x=$x y=$y random=$random"

    override fun getCommandName(): String = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_click)

    override fun getCommandDescribe(): String = GlobalApp.getString(
        com.hive.i8n.R.string.cmd_des_click,
        (x * ScriptCoordinateAdapter.getScreenWidthByOrientation()).toInt(),
        (y * ScriptCoordinateAdapter.getScreenHeightByOrientation()).toInt()
    )

    override fun getCommandIcon() = R.drawable.ic_click

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        x = p["x"]?.toFloatOrNull() ?: 0f
        y = p["y"]?.toFloatOrNull() ?: 0f
        random = p["random"]?.toIntOrNull() ?: 0
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
        return ScriptCommonUtils.convertToNormalization(
            r,
            ScriptCoordinateAdapter.getScreenWidthByOrientation(),
            ScriptCoordinateAdapter.getScreenHeightByOrientation()
        )
    }

    companion object {
        fun createCommand(x: Int, y: Int) = CmdClick().apply {
            this.x = ScriptCoordinateAdapter.get().toNormalizedX(x)
            this.y = ScriptCoordinateAdapter.get().toNormalizedY(y)
            this.actualX = x
            this.actualY = y
            this.random =
                if (ScriptSetting.script_setting_anti_detect) ScriptConst.Default_Anti_Detect_Radius_Value else 0
        }
    }
}