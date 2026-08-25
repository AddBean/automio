// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.condition

import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandCondition
import com.hive.script.base.ScriptConst
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.setting.ScriptSetting
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.utils.ScriptHelper
import com.hive.utils.GlobalApp
import java.util.regex.Matcher
import java.util.regex.Pattern

@AutoConditionRegister(type = ConditionIDS.ConditionIdColor)
class ConditionColor(cmd: ScriptCommand) : ScriptCommandCondition(cmd) {

    private var matchPattern = """checkColor\((.*)\)"""

    private var actualX = 0

    private var actualY = 0

    var actionValue: String? = "10"

    var color: Int? = Color.parseColor("#000000")

    var timeGap = 300L

    var timeCurrent = 0L

    private var resultRect: Rect? = null

    override fun isMeet(cmd: ScriptCommand?): Boolean {
        return try {
            color ?: return false
            resultRect =
                ScriptEventHelper.get().tryFindColorRect(
                    color!!, cmd?.limitRect ?: RectF(0f, 0f, 1f, 1f),
                    getAccuracy(actionValue)
                )?.firstOrNull()
            actualX = resultRect?.centerX() ?: 0
            actualY = resultRect?.centerY() ?: 0
            resultRect != null
        } catch (e: Throwable) {
            e.printStackTrace()
            resultRect = null
            false
        }
    }

    private fun getAccuracy(value: String?): Int {
        return when (value) {
            "accurate" -> ScriptConst.Cmd_Spot_Color_Threshold_Accuracy
            "normal" -> ScriptConst.Cmd_Spot_Color_Threshold_Normal
            "obscure" -> ScriptConst.Cmd_Spot_Color_Threshold_Obscure
            else -> (value?.toIntOrNull() ?: 10)
        }
    }

    override fun doPostAction(action: String) {
        if (timeCurrent + timeGap > System.currentTimeMillis()) {
            return
        }
        timeCurrent = System.currentTimeMillis()
        when (action) {
            Post_Action_Click -> {

                val random =
                    if (ScriptSetting.script_setting_anti_detect) ScriptConst.Default_Anti_Detect_Radius_Value else 0
                ScriptEventHelper.get().performClick(
                    ScriptCommonUtils.getRandomValue(actualX, random) + command.offsetVector.toRealDiffX(),
                    ScriptCommonUtils.getRandomValue(actualY, random) + command.offsetVector.toRealDiffY()
                )
            }

            Post_Action_Long_Click -> {
                val random =
                    if (ScriptSetting.script_setting_anti_detect) ScriptConst.Default_Anti_Detect_Radius_Value else 0
                ScriptEventHelper.get().performPress(
                    ScriptCommonUtils.getRandomValue(actualX, random) + command.offsetVector.toRealDiffX(),
                    ScriptCommonUtils.getRandomValue(actualY, random) + command.offsetVector.toRealDiffY(),
                    1000
                )
            }

            else -> {

            }
        }
    }

    override fun getCondition() = "checkColor($actionValue,$color)"

    override fun getConditionName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_color_name)

    override fun getConditionDesc() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_color_des)

    fun getActionName(): String {
        return actionMap2[actionValue] ?: actionMapColor[getAccuracy(actionValue).toString()]
        ?: ("${getAccuracy(actionValue)}%")
    }


    override fun parseCondition(cmd: String) {
        val r: Pattern = Pattern.compile(matchPattern)
        val m: Matcher = r.matcher(cmd)
        if (m.find()) {
            val params = ScriptCommandHelper.splitParams(m.group(1)?.toString())
            actionValue = params.getOrNull(0)
            color = params.getOrNull(1)?.toIntOrNull()
        }
    }

    override fun matchCondition(condition: String) = Regex(matchPattern).matches(condition)

    override fun getPermissionRequest() =
        mutableListOf(
            ScriptHelper.PERMISSION_BIND_ACCESSIBILITY_SERVICE,
            ScriptHelper.PERMISSION_CAPTURE
        )

}