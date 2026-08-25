// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.graphics.RectF
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.ScriptHelper
import com.hive.script.utils.ScriptLayoutReader
import com.hive.script.views.logger.ScriptLoggerView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import com.hive.utils.extends.toJson

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdReadScreenLayout, name = "readScreenLayout")
class CmdReadScreenLayout : ScriptCommand(), ScriptRegularInterface {

    var targetParamId = ScriptParamEnv.getDefaultParam()?.getFullId()

    var resultJson: String? = null

    override fun onExecute(): CmdExecuteResult {
        val layoutResult = ScriptLayoutReader.getCurrentLayout()
        resultJson = layoutResult.toJson()
        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this)
        ScriptInterpreterObserver.notifyLogger(
            this,
            ScriptLoggerView.LogType.DEBUG,
            com.hive.i8n.R.string.sc_read_text.string(resultJson ?: "")
        )
        writeParam(targetParamId, resultJson)
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Base)
        return CmdExecuteResult.success(layoutResult)
    }

    override fun doExecute(): CmdExecuteResult {
        return super.doExecute()
    }


    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommand() = "${cmdPrefix()} output=$targetParamId"

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_read_screen_layout)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_read_screen_layout)

    override fun getCommandIcon() = R.drawable.sc_ic_ocr_read

    override fun parseCmd(cmd: String) {
        targetParamId = ScriptLineTokenizer.parseKeyValueParams(cmd)["output"] ?: targetParamId
    }

    override fun getNormalizedActiveArea() = RectF(0f, 0f, 1f, 1f)

    override fun getPermissionRequest() =
        mutableListOf(ScriptHelper.PERMISSION_BIND_ACCESSIBILITY_SERVICE)

    companion object {
        fun createCommand(paramFullId: String?) = CmdReadScreenLayout().apply {
            targetParamId = paramFullId ?: "main.param0"
        }
    }
}