// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.condition

import android.graphics.Rect
import android.graphics.RectF
import android.text.TextUtils
import com.hive.script.R
import com.hive.script.base.IScriptFileInterface
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
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

@AutoConditionRegister(type = ConditionIDS.ConditionIdImage)
class ConditionImage(cmd: ScriptCommand) : ScriptCommandCondition(cmd), IScriptFileInterface {

    private var matchPattern = """checkImage\((.*)\)"""

    private var actualX = 0

    private var actualY = 0

    var actionValue: String? = "75"

    var attachmentFiles: MutableList<String>? = mutableListOf()

    var timeGap = 300L

    var timeCurrent = 0L

    private var resultRect: Rect? = null

    override fun isMeet(cmd: ScriptCommand?): Boolean {
        return try {
            resultRect =
                ScriptEventHelper.get()
                    .tryRecogniseImage(
                        getAttachmentFullPaths(),
                        cmd?.limitRect ?: RectF(0f, 0f, 1f, 1f),
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

    private fun getAccuracy(value: String?): Double {
        return when (value) {
            "accurate" -> ScriptConst.Cmd_Spot_Accuracy
            "normal" -> ScriptConst.Cmd_Spot_Normal
            "obscure" -> ScriptConst.Cmd_Spot_Obscure
            else -> ((value?.toIntOrNull() ?: 75) / 100.0)
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

    override fun getCondition() = "checkImage($actionValue,\"${
        attachmentFiles?.map { StringUtils.encoding(it) }?.joinToString(",")
    }\")"

    override fun getConditionName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_image_name)

    override fun getConditionDesc() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_image_des)

    fun getActionName(): String {
        return actionMap2[actionValue] ?: actionMapImage[actionValue] ?: ("${actionValue ?: ""}%")
    }

    override fun parseCondition(cmd: String) {
        val r: Pattern = Pattern.compile(matchPattern)
        val m: Matcher = r.matcher(cmd)
        if (m.find()) {
            val params = ScriptCommandHelper.splitParams(m.group(1)?.toString())
            actionValue = params.getOrNull(0)
            val strings = ScriptCommandHelper.parseParamString(params.getOrNull(1))
            attachmentFiles = strings.split(",").map { StringUtils.decoding(it) }.toMutableList()
        }
    }

    override fun matchCondition(condition: String) = Regex(matchPattern).matches(condition)

    override fun getAttachmentFullPaths(): List<String> {
        return attachmentFiles?.map { ScriptCommandHelper.getFileByRelativePath(command, it) }
            ?: mutableListOf()
    }

    override fun getAttachmentRelativePaths(): List<String>? {
        return attachmentFiles
    }

    override fun setAttachmentFilePaths(path: List<String>?) {
        attachmentFiles = path?.toMutableList()
    }

    override fun getAttachFiles(): List<File>? {
        return this.getAttachmentFullPaths().map { File(it) }
    }

    private fun getScriptBasePath(): String {
        var filePath = command.getRootScript()?.getScriptBasePath()
        if (TextUtils.isEmpty(filePath)) {
            filePath = ScriptConst.Save_Script_Temp_Path
        }
        return filePath!!
    }

    override fun getPermissionRequest() =
        mutableListOf(
            ScriptHelper.PERMISSION_BIND_ACCESSIBILITY_SERVICE,
            ScriptHelper.PERMISSION_CAPTURE
        )

}