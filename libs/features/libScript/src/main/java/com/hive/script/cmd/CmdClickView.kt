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
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.setting.ScriptSetting
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.utils.ScriptCommandHelper
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdClickView, name = "clickView")
class CmdClickView : ScriptCommand(), ScriptRegularInterface {

    var targetId = ScriptConst.NONE_CHAR

    var targetText = ScriptConst.NONE_CHAR

    var targetTag = ScriptConst.NONE_CHAR

    var action = ScriptClickActionHelper.ACTION_CLICK

    var random = 0

    var fastCount = 1

    var fastGap = 200L

    var pressDuration = ScriptConst.Cmd_Long_Click_Default

    var resultRect = Rect()

    var findDirection = 0//0 左上角、1 右上角、2 左下角、3 右下角

    override fun onExecute(): CmdExecuteResult {
        try {
            val finalId = parseParamText(targetId)
            val finalText = parseParamText(targetText)
            val finalTag = parseParamText(targetTag)
            ScriptEventHelper.get().performFindLayout(
                finalId, finalText, finalTag, 0, findDirection, limitRect
            ) { targetNode, _, _ ->
                targetNode?.getBoundsInScreen(resultRect)
                ScriptClickActionHelper.performAction(
                    Point(resultRect.centerX(), resultRect.centerY()), action, this
                )

            }?.run {
                return CmdExecuteResult.success()
            } ?: run {
                return CmdExecuteResult.failure(
                    GlobalApp.getString(
                        com.hive.i8n.R.string.sc_cmd_click_view_not_found,
                        finalId,
                        finalText,
                        finalTag
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return CmdExecuteResult.failure(e.message)
        }
    }


    override fun getCommand(): String {
        val idStr = targetId.encode()
        val textStr = if (targetText.contains(" ") || targetText.contains(",")) "\"${targetText.encode()}\"" else targetText.encode()
        val tagStr = if (targetTag.contains(" ") || targetTag.contains(",")) "\"${targetTag.encode()}\"" else targetTag.encode()
        return "${cmdPrefix()} id=$idStr text=$textStr tag=$tagStr action=$action direction=$findDirection random=$random fastCount=$fastCount fastGap=$fastGap pressDuration=$pressDuration"
    }

    override fun getCommandName() = when (action) {
        ScriptClickActionHelper.ACTION_CLICK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_name_click_layout1)
        ScriptClickActionHelper.ACTION_PRESS -> GlobalApp.getString(com.hive.i8n.R.string.cmd_name_click_layout2)
        ScriptClickActionHelper.ACTION_FAST_CLICK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_name_click_layout3)
        ScriptClickActionHelper.ACTION_BREAK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_name_click_layout4)
        else -> GlobalApp.getString(com.hive.i8n.R.string.cmd_name_click_layout1)
    }


    override fun getCommandDescribe(): String {
        var targetTxt = ""
        if (targetText != ScriptConst.NONE_CHAR) targetTxt += "$targetText "
        if (targetId != ScriptConst.NONE_CHAR) targetTxt += "$targetId "
        if (targetTag != ScriptConst.NONE_CHAR) targetTxt += "$targetTag "
        return when (action) {
            ScriptClickActionHelper.ACTION_CLICK -> GlobalApp.getString(
                com.hive.i8n.R.string.cmd_des_click_layout1, targetTxt
            )

            ScriptClickActionHelper.ACTION_PRESS -> GlobalApp.getString(
                com.hive.i8n.R.string.cmd_des_click_layout2, targetTxt
            )

            ScriptClickActionHelper.ACTION_FAST_CLICK -> GlobalApp.getString(
                com.hive.i8n.R.string.cmd_des_click_layout3, targetTxt
            )

            ScriptClickActionHelper.ACTION_BREAK -> GlobalApp.getString(
                com.hive.i8n.R.string.cmd_des_click_layout4, targetTxt
            )

            else -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_click_layout1, targetTxt)
        }
    }

    override fun getCommandIcon() = R.drawable.ic_layout

    override fun isSupportOffset() = true

    override fun isSupportRect(): Boolean = true

    override fun isSupportDrag(): Boolean = true

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        targetId = p["id"]?.decode() ?: ScriptConst.NONE_CHAR
        targetText = (p["text"] ?: ScriptConst.NONE_CHAR).decode()
        targetTag = (p["tag"] ?: ScriptConst.NONE_CHAR).decode()
        action = p["action"] ?: ScriptClickActionHelper.ACTION_CLICK
        findDirection = p["direction"]?.toIntOrNull() ?: 0
        random = p["random"]?.toIntOrNull() ?: 0
        fastCount = p["fastCount"]?.toIntOrNull() ?: 1
        fastGap = p["fastGap"]?.toLongOrNull() ?: 200L
        pressDuration = p["pressDuration"]?.toLongOrNull() ?: ScriptConst.Cmd_Long_Click_Default
    }

    override fun getNormalizedActiveArea() = RectF(0f, 0f, 1f, 1f)

    companion object {

        fun createCommand(
            action: String,
            fastCount: Int,
            fastGap: Long,
            pressDuration: Long,
            targetId: String?,
            targetText: String?,
            targetTag: String?
        ) = CmdClickView().apply {
            this.action = action
            this.random =
                if (ScriptSetting.script_setting_anti_detect) ScriptConst.Default_Anti_Detect_Radius_Value else 0
            this.fastCount = fastCount
            this.pressDuration = pressDuration
            this.fastGap = fastGap

            this.targetId = targetId ?: ScriptConst.NONE_CHAR
            this.targetText = targetText ?: ScriptConst.NONE_CHAR
            this.targetTag = targetTag ?: ScriptConst.NONE_CHAR
        }
    }
}