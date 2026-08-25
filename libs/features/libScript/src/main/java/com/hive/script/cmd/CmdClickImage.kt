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
import com.hive.utils.utils.StringUtils

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdClickImage, name = "clickImage")
class CmdClickImage : ScriptCommand(), ScriptRegularInterface {

    var action = ScriptClickActionHelper.ACTION_CLICK

    var accuracy = ScriptConst.Cmd_Spot_Accuracy

    var random = 0

    var fastCount = 1

    var fastGap = 200L

    var pressDuration = ScriptConst.Cmd_Long_Click_Default

    var resultRect: Rect? = null

    override fun onExecute(): CmdExecuteResult {
        try {
            resultRect =
                ScriptEventHelper.get()
                    .tryRecogniseImage(
                        getAttachmentFullPaths(),
                        limitRect,
                        accuracy
                    )?.firstOrNull()
            if (resultRect != null) {
                ScriptClickActionHelper.performAction(
                    Point(
                        resultRect!!.centerX(),
                        resultRect!!.centerY()
                    ), action, this
                )
                return CmdExecuteResult.success(
                    Point(
                        resultRect!!.centerX(),
                        resultRect!!.centerY()
                    )
                )
            } else {
                ScriptThreadManager.delay(ScriptConst.Cmd_Default_Spot)
                return CmdExecuteResult.failure(
                    GlobalApp.getString(com.hive.i8n.R.string.sc_cmd_spot_image_not_found)
                )
            }

        } catch (e: Throwable) {
            e.printStackTrace()
            return CmdExecuteResult.failure(e.message)
        }
    }


    override fun getCommandName() = when (action) {
        ScriptClickActionHelper.ACTION_CLICK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot1)
        ScriptClickActionHelper.ACTION_PRESS -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot2)
        ScriptClickActionHelper.ACTION_FAST_CLICK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot3)
        ScriptClickActionHelper.ACTION_BREAK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot4)
        else -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot1)
    }

    override fun getCommandDescribe() = when (action) {
        ScriptClickActionHelper.ACTION_CLICK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot1)
        ScriptClickActionHelper.ACTION_PRESS -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot2)
        ScriptClickActionHelper.ACTION_FAST_CLICK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot3)
        ScriptClickActionHelper.ACTION_BREAK -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot4)
        else -> GlobalApp.getString(com.hive.i8n.R.string.cmd_des_spot1)
    }

    override fun getCommandIcon() = R.drawable.ic_check_pic

    override fun getCommand(): String {
        val imagesVal = attachmentFiles?.map { StringUtils.encoding(it) }?.joinToString(",") ?: ""
        val imagesStr = if (imagesVal.contains(",") || imagesVal.contains(" ")) "\"$imagesVal\"" else imagesVal
        return "${cmdPrefix()} action=$action accuracy=$accuracy random=$random fastCount=$fastCount fastGap=$fastGap pressDuration=$pressDuration images=$imagesStr"
    }

    override fun isSupportOffset() = true

    override fun isSupportRect(): Boolean = true

    override fun isSupportDrag(): Boolean = true

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        action = p["action"] ?: ScriptClickActionHelper.ACTION_CLICK
        accuracy = p["accuracy"]?.toDoubleOrNull() ?: ScriptConst.Cmd_Spot_Accuracy
        random = p["random"]?.toIntOrNull() ?: 0
        fastCount = p["fastCount"]?.toIntOrNull() ?: 1
        fastGap = p["fastGap"]?.toLongOrNull() ?: 200L
        pressDuration = p["pressDuration"]?.toLongOrNull() ?: ScriptConst.Cmd_Long_Click_Default
        p["images"]?.takeIf { it.isNotBlank() }?.let { str ->
            attachmentFiles = str.split(",").map { StringUtils.decoding(it.trim()) }.filter { it.isNotBlank() }.toMutableList()
        }
    }

    override fun getAttachmentFullPaths(): List<String> {
        return attachmentFiles?.map { ScriptCommandHelper.getFileByRelativePath(this, it) }
            ?: mutableListOf()
    }

    override fun getAttachmentRelativePaths(): List<String>? {
        return attachmentFiles
    }

    override fun getNormalizedActiveArea() = RectF(0f, 0f, 1f, 1f)

    override fun getPermissionRequest() = mutableListOf(ScriptHelper.PERMISSION_CAPTURE)

    companion object {

        fun createCommand(
            action: String,
            accuracy: Double,
            fastCount: Int,
            fastGap: Long,
            pressDuration: Long,
            imagePath: String
        ) = CmdClickImage().apply {
            this.attachmentFiles = mutableListOf(imagePath)
            this.action = action
            this.accuracy = accuracy
            this.random =
                if (ScriptSetting.script_setting_anti_detect) ScriptConst.Default_Anti_Detect_Radius_Value else 0
            this.fastCount = fastCount
            this.pressDuration = pressDuration
            this.fastGap = fastGap
        }
    }
}