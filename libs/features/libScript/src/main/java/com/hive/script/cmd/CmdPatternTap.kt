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
import com.hive.script.setting.ScriptSetting
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.GlobalApp

@AutoCmdRegister(type = IDS.CmdPatternTap, name = "patternTap")
class CmdPatternTap : ScriptCommand(), ScriptRegularInterface {
    // 0:自上而下 1:自下而上 2:自左而右 3:自右而左 4:随机
    var clickType = 0

    var clickGap = 300L

    var clickHrz = 30

    var clickVer = 30

    var random = 0


    override fun onExecute(): CmdExecuteResult {
        val mRectF = RectF(ScriptCommonUtils.covertToScreenRect(limitRect))
        ScriptCommonUtils.forEachRect(mRectF, clickType, clickHrz, clickVer) { x, y ->
            if (!ScriptThreadManager.isPaused()) {
                doClick(x.toInt(), y.toInt())
            }
        }
        return CmdExecuteResult.success()
    }

    private fun doClick(x: Int, y: Int) {
        val actualX = ScriptCommonUtils.getRandomValue(x, random)
        val actualY = ScriptCommonUtils.getRandomValue(y, random)
        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this, Point(actualX, actualY))
        ScriptEventHelper.get().performClick(actualX, actualY)
        ScriptThreadManager.delay(clickGap)
    }

    fun getExecuteDuration(): Long {
        return clickHrz * clickVer * clickGap
    }

    override fun getCommand() =
        "${cmdPrefix()} type=$clickType gap=$clickGap hrz=$clickHrz ver=$clickVer random=$random"

    override fun getCommandName(): String =
        GlobalApp.getString(com.hive.i8n.R.string.cmd_name_batch_click)

    override fun getCommandDescribe(): String =
        GlobalApp.getString(com.hive.i8n.R.string.cmd_des_batch_click, clickHrz, clickVer)

    override fun getCommandIcon() = R.drawable.ic_grid

    override fun isSupportDelay() = true

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        clickType = p["type"]?.toIntOrNull() ?: 0
        clickGap = p["gap"]?.toLongOrNull() ?: 300L
        clickHrz = p["hrz"]?.toIntOrNull() ?: 30
        clickVer = p["ver"]?.toIntOrNull() ?: 30
        random = p["random"]?.toIntOrNull() ?: 0
    }

    override fun isSupportRect() = true

    override fun getNormalizedActiveArea() = RectF(limitRect)

    companion object {
        fun createCommand(
            clickType: Int, clickGap: Long, clickHrz: Int, clickVer: Int, rect: RectF
        ) = CmdPatternTap().apply {
            this.clickType = clickType
            this.clickGap = clickGap
            this.clickHrz = clickHrz
            this.clickVer = clickVer
            this.limitRect = rect
            this.random =
                if (ScriptSetting.script_setting_anti_detect) ScriptConst.Default_Anti_Detect_Radius_Value else 0
        }
    }
}