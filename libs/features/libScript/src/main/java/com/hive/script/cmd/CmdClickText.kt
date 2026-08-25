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
import com.hive.script.utils.ScriptHelper
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdClickText, name = "clickText")
class CmdClickText : ScriptCommand(), ScriptRegularInterface {

    var targetText = ""

    var action = ScriptClickActionHelper.ACTION_CLICK

    var findType = TEXT_FIND_CONTAINS

    var random = 0

    var fastCount = 1

    var fastGap = 200L

    var pressDuration = ScriptConst.Cmd_Long_Click_Default

    var targetRect: Rect? = null

    var findDirection = 0//0 优先左上角、1 优先右上角、2 优先左下角、3 优先右下角

    var ocrType = 1 // 0:控件 1:ocr

    override fun onExecute(): CmdExecuteResult {
        try {
            val _targetText = parseParamText(targetText)
            targetRect = if (ocrType == 1) {
                findRectByOcr(_targetText ?: "")
            } else {
                findRectByView(_targetText ?: "")
            }
            targetRect?.let {
                val actualPoint = Point(it.centerX(), it.centerY())
                ScriptClickActionHelper.performAction(actualPoint, action, this)
                return CmdExecuteResult.maySuccess(actualPoint)
            } ?: run {
                return CmdExecuteResult.failure(
                    GlobalApp.getString(com.hive.i8n.R.string.sc_cmd_spot_text_not_found, _targetText ?: "")
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            return CmdExecuteResult.failure(e.message)
        }
    }

    private fun findRectByOcr(text: String): Rect? {
        return ScriptEventHelper.get().tryFindOcrTextInSync(
            text,
            findDirection,
            when (findType) {
                TEXT_FIND_CONTAINS -> 0
                TEXT_FIND_EQUALS -> 1
                else -> 0
            },
            limitRect
        )
    }

    private fun findRectByView(text: String): Rect? {
        val node = ScriptEventHelper.get()
            .performFindLayout(
                null, text, null, when (findType) {
                    TEXT_FIND_CONTAINS -> 0
                    TEXT_FIND_EQUALS -> 1
                    else -> 0
                }, findDirection, limitRect, null
            )
        return node?.let {
            val outBounds = Rect()
            it.getBoundsInScreen(outBounds)
            outBounds
        }
    }

    fun getCommandDuration() = when (action) {
        ScriptClickActionHelper.ACTION_CLICK -> ScriptConst.Cmd_Default_Spot_Text
        ScriptClickActionHelper.ACTION_PRESS -> ScriptConst.Cmd_Default_Spot_Text + pressDuration
        ScriptClickActionHelper.ACTION_FAST_CLICK -> ScriptConst.Cmd_Default_Spot_Text + fastCount * fastGap
        ScriptClickActionHelper.ACTION_BREAK -> ScriptConst.Cmd_Default_Spot_Text
        else -> ScriptConst.Cmd_Default_Spot_Text
    }

    override fun getCommandName() = when (action) {
        else -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_click_text1)
    }

    override fun getCommandDescribe() = when (action) {
        ScriptClickActionHelper.ACTION_CLICK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_text1)
        ScriptClickActionHelper.ACTION_PRESS -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_text2)
        ScriptClickActionHelper.ACTION_FAST_CLICK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_text3)
        ScriptClickActionHelper.ACTION_BREAK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_text4)
        else -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot_text1)
    }

    override fun getCommandIcon() = R.drawable.ic_text_setting

    override fun getCommand(): String {
        val textStr = if (targetText.contains(" ") || targetText.contains(",")) "\"$targetText\"" else targetText
        return "${cmdPrefix()} text=$textStr action=$action findType=$findType direction=$findDirection random=$random fastCount=$fastCount fastGap=$fastGap pressDuration=$pressDuration ocrType=$ocrType"
    }

    override fun isSupportOffset() = true

    override fun isSupportRect(): Boolean = true

    override fun isSupportDrag(): Boolean = true

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        targetText = p["text"] ?: ""
        action = p["action"] ?: ScriptClickActionHelper.ACTION_CLICK
        findType = p["findType"] ?: TEXT_FIND_CONTAINS
        findDirection = p["direction"]?.toIntOrNull() ?: 0
        random = p["random"]?.toIntOrNull() ?: 0
        fastCount = p["fastCount"]?.toIntOrNull() ?: 1
        fastGap = p["fastGap"]?.toLongOrNull() ?: 200L
        pressDuration = p["pressDuration"]?.toLongOrNull() ?: ScriptConst.Cmd_Long_Click_Default
        ocrType = p["ocrType"]?.toIntOrNull() ?: 1
    }

    override fun getNormalizedActiveArea() = RectF(0f, 0f, 1f, 1f)

    override fun getPermissionRequest() = mutableListOf(ScriptHelper.PERMISSION_CAPTURE)

    companion object {

        const val TEXT_FIND_CONTAINS = "contains"

        const val TEXT_FIND_EQUALS = "equals"

        fun createCommand(
            action: String,
            fastCount: Int,
            fastGap: Long,
            pressDuration: Long,
            targetText: String,
            findType: String
        ) = CmdClickText().apply {
            this.findType = findType
            this.targetText = targetText
            this.action = action
            this.random =
                if (ScriptSetting.script_setting_anti_detect) ScriptConst.Default_Anti_Detect_Radius_Value else 0
            this.fastCount = fastCount
            this.pressDuration = pressDuration
            this.fastGap = fastGap
        }

        fun createCommand(
            action: String,
            fastCount: Int,
            fastGap: Long,
            pressDuration: Long,
            targetText: String,
            findType: String,
            findDirection: Int,
            random: Int,
            ocrType: Int
        ) = CmdClickText().apply {
            this.findType = findType
            this.targetText = targetText
            this.action = action
            this.random = random
            this.fastCount = fastCount
            this.pressDuration = pressDuration
            this.fastGap = fastGap
            this.findDirection = findDirection
            this.ocrType = ocrType
        }
    }
}