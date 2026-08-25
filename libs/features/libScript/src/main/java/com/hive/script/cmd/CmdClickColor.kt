// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import com.hive.script.R
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.setting.ScriptSetting
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.utils.ScriptHelper
import com.hive.utils.GlobalApp
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdClickColor, name = "clickColor")
class CmdClickColor : ScriptCommand(), ScriptRegularInterface {

    var targetColor = 0

    var action = ScriptClickActionHelper.ACTION_CLICK

    var threshold = ScriptConst.Cmd_Spot_Color_Threshold

    var findType = COLOR_FIND_BLOCK

    var random = 0

    var fastCount = 1

    var fastGap = 200L

    var pressDuration = ScriptConst.Cmd_Long_Click_Default

    var resultPoints: Array<Point>? = null

    var resultRects: Array<Rect>? = null


    override fun onExecute() : CmdExecuteResult {
        try {
            when (findType) {
                COLOR_FIND_BLOCK -> {
                    resultRects =
                        ScriptEventHelper.get().tryFindColorRect(targetColor, limitRect, threshold)
                    if (resultRects != null && resultRects?.isNotEmpty() == true) {
                        val maxRect = resultRects!![0]
                        CommonToast.show(com.hive.i8n.R.string.sc_cmd_spot_color_success)
                        ScriptClickActionHelper.performAction(
                            Point(maxRect.centerX(), maxRect.centerY()),
                            action!!,
                            this
                        )
                        return CmdExecuteResult.success(
                            Point(maxRect.centerX(), maxRect.centerY())
                        )
                    } else {
                        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Spot_Color)
//                        CommonToast.show(com.hive.i8n.R.string.sc_cmd_spot_color_failure)
                        return CmdExecuteResult.failure(GlobalApp.getString(com.hive.i8n.R.string.sc_cmd_spot_color_failure))
                    }
                }

                COLOR_FIND_ACCURATE -> {
                    resultPoints = ScriptEventHelper.get().tryFindColors(
                        targetColor,
                        limitRect,
                        threshold
                    )
                    if (resultPoints != null && resultPoints?.isNotEmpty() == true) {
                        CommonToast.show(com.hive.i8n.R.string.sc_cmd_spot_color_success)
                        ScriptClickActionHelper.performAction(resultPoints!![0], action!!, this)
                        return CmdExecuteResult.success(resultPoints!![0])
                    } else {
                        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Spot_Color)
//                        CommonToast.show(com.hive.i8n.R.string.sc_cmd_spot_color_failure)
                        return CmdExecuteResult.failure(GlobalApp.getString(com.hive.i8n.R.string.sc_cmd_spot_color_failure))
                    }
                }
            }
            return CmdExecuteResult.failure(GlobalApp.getString(com.hive.i8n.R.string.sc_cmd_spot_color_failure))
        } catch (e: Throwable) {
            e.printStackTrace()
            return CmdExecuteResult.failure(e.message)
//            CommonToast.show(com.hive.i8n.R.string.sc_cmd_spot_color_failure)
        }
    }

    fun getCommandDuration() = when (action) {
        ScriptClickActionHelper.ACTION_CLICK -> ScriptConst.Cmd_Default_Spot_Color
        ScriptClickActionHelper.ACTION_PRESS -> ScriptConst.Cmd_Default_Spot_Color + pressDuration
        ScriptClickActionHelper.ACTION_FAST_CLICK -> ScriptConst.Cmd_Default_Spot_Color + fastCount * fastGap
        ScriptClickActionHelper.ACTION_BREAK -> ScriptConst.Cmd_Default_Spot_Color
        else -> ScriptConst.Cmd_Default_Spot_Color
    }

    override fun getCommandName() = when (action) {
        ScriptClickActionHelper.ACTION_CLICK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_color1)
        ScriptClickActionHelper.ACTION_PRESS -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_color2)
        ScriptClickActionHelper.ACTION_FAST_CLICK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_color3)
        ScriptClickActionHelper.ACTION_BREAK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_color4)
        else -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_color1)
    }

    override fun getCommandDescribe() = when (action) {
        ScriptClickActionHelper.ACTION_CLICK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_color1)
        ScriptClickActionHelper.ACTION_PRESS -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_color2)
        ScriptClickActionHelper.ACTION_FAST_CLICK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_color3)
        ScriptClickActionHelper.ACTION_BREAK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_color4)
        else -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_color1)
    }

    override fun getCommandIcon() = R.drawable.ic_color_setting

    override fun getCommand() =
        "${cmdPrefix()} action=$action color=$targetColor threshold=$threshold findType=$findType random=$random fastCount=$fastCount fastGap=$fastGap pressDuration=$pressDuration"

    override fun isSupportOffset() = true

    override fun isSupportRect(): Boolean = true

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        action = p["action"] ?: ScriptClickActionHelper.ACTION_CLICK
        targetColor = p["color"]?.toIntOrNull() ?: 0
        threshold = p["threshold"]?.toIntOrNull() ?: ScriptConst.Cmd_Spot_Color_Threshold
        findType = p["findType"] ?: COLOR_FIND_BLOCK
        random = p["random"]?.toIntOrNull() ?: 0
        fastCount = p["fastCount"]?.toIntOrNull() ?: 1
        fastGap = p["fastGap"]?.toLongOrNull() ?: 200L
        pressDuration = p["pressDuration"]?.toLongOrNull() ?: ScriptConst.Cmd_Long_Click_Default
    }

    override fun getNormalizedActiveArea() = RectF(0f, 0f, 1f, 1f)

    override fun getPermissionRequest() = mutableListOf(ScriptHelper.PERMISSION_CAPTURE)

    companion object {
        const val COLOR_FIND_BLOCK = "block"
        const val COLOR_FIND_ACCURATE = "accurate"


        fun createCommand(
            action: String,
            threshold: Int,
            fastCount: Int,
            fastGap: Long,
            pressDuration: Long,
            targetColor: Int,
            findType: String
        ) = CmdClickColor().apply {
            this.findType = findType
            this.targetColor = targetColor
            this.action = action
            this.threshold = threshold
            this.random =
                if (ScriptSetting.script_setting_anti_detect) ScriptConst.Default_Anti_Detect_Radius_Value else 0
            this.fastCount = fastCount
            this.pressDuration = pressDuration
            this.fastGap = fastGap
        }
    }
}