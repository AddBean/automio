// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.ScriptHelper
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.utils.StringUtils
import com.hive.views.widgets.CommonToast

@AutoCmdRegister(type = IDS.CmdToast, name = "toast")
class CmdToast : ScriptCommand(), ScriptRegularInterface {
    var content: String? = null

    override fun onExecute(): CmdExecuteResult {
        val msg = parseParamText(content)
        ScriptHelper.runInMain {
            CommonToast.getInstance().showToastLong(msg)
        }
        ScriptThreadManager.delay(getCommandDuration())
        return CmdExecuteResult.success()
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_toast)

    override fun getCommandDescribe() =
        if (ScriptParamEnv.isParamText(content)) GlobalApp.getString(com.hive.i8n.R.string.cmd_name_toast) else StringUtils.decoding(content)

    override fun getCommand() = "${cmdPrefix()} text=\"${content?.encode()}\""

    override fun parseCmd(cmd: String) {
        content = ScriptLineTokenizer.parseKeyValueParams(cmd)["text"]?.decode()
    }

    override fun getPermissionRequest() = null

    override fun getCommandIcon() = R.drawable.sc_icon_toast

    companion object {
        fun createCommand() = CmdToast().apply {
            this.content = ""
        }
    }
}