// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.condition

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
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
import com.hive.utils.utils.StringUtils
import java.util.regex.Matcher
import java.util.regex.Pattern

@AutoConditionRegister(type = ConditionIDS.ConditionIdView)
class ConditionView(cmd: ScriptCommand) : ScriptCommandCondition(cmd) {

    private var matchPattern = """checkView\((.*)\)"""

    private var meetNode: AccessibilityNodeInfo? = null

    private var actualX = -1

    private var actualY = -1

    var action: String? = "contains"

    var text: String? = GlobalApp.getString(com.hive.i8n.R.string.sc_edit_condition_view_edit_text_default)

    var timeGap = 300L

    var timeCurrent = 0L

    var ocrType = 1//0:控件 1:ocr

    override fun isMeet(cmd: ScriptCommand?): Boolean {
        val event = ScriptEventHelper.get().accessibilityViewEvent
        event ?: return false
        if (ocrType == 1) {
            findRectByOcr(cmd, text ?: "")
        } else {
            findRectByView(cmd, text ?: "")
        }

        return actualX != -1 && actualY != -1
    }

    private fun findRectByOcr(cmd: ScriptCommand?, text: String) {
        val paramText = cmd?.parseParamText(text) ?: text
        ScriptEventHelper.get().tryFindOcrTextInSync(paramText, 0, 0, cmd?.limitRect)?.run {
            actualX = centerX()
            actualY = centerY()
        } ?: run {
            actualX = -1
            actualY = -1
        }
    }

    private fun findRectByView(cmd: ScriptCommand?, text: String) {
        meetNode =
            ScriptEventHelper.get().performFindLayout(null, text, null, 0,0, cmd?.limitRect, null)
        meetNode?.run {
            val outBounds = Rect()
            meetNode?.getBoundsInScreen(outBounds)
            actualX = outBounds.centerX()
            actualY = outBounds.centerY()
        } ?: run {
            actualX = -1
            actualY = -1
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
                ScriptEventHelper.get().performPress(
                    ScriptCommonUtils.getRandomValue(actualX, 0) + command.offsetVector.toRealDiffX(),
                    ScriptCommonUtils.getRandomValue(actualY, 0) + command.offsetVector.toRealDiffY(),
                    ScriptConst.Cmd_Long_Click_Default
                )
            }

            else -> {

            }
        }
    }

    override fun getCondition() = "checkView($action,\"${StringUtils.encoding(text)}\",$ocrType)"

    override fun getConditionName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_view_name)

    override fun getConditionDesc() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_view_des, text)

    fun getActionName(): String {
        return actionMap[action] ?: ""
    }

    fun getTextName(): String {
        return text ?: ""
    }

    fun getOcrName(): String? {
        return when (ocrType) {
            0 -> GlobalApp.getString(com.hive.i8n.R.string.sc_edit_condition_view_ocr_type_0)
            1 -> GlobalApp.getString(com.hive.i8n.R.string.sc_edit_condition_view_ocr_type_1)
            else -> ""
        }
    }


    override fun parseCondition(cmd: String) {
        val r: Pattern = Pattern.compile(matchPattern)
        val m: Matcher = r.matcher(cmd)
        if (m.find()) {
            val params = ScriptCommandHelper.splitParams(m.group(1)?.toString())
            action = params.getOrNull(0)
            text = StringUtils.decoding(ScriptCommandHelper.parseParamString(params.getOrNull(1)))
            ocrType = params.getOrNull(2)?.toIntOrNull() ?: 1
        }
    }

    override fun matchCondition(condition: String) = Regex(matchPattern).matches(condition)

    override fun getPermissionRequest() =
        mutableListOf(
            ScriptHelper.PERMISSION_BIND_ACCESSIBILITY_SERVICE,
            ScriptHelper.PERMISSION_CAPTURE
        )
}