// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.text.TextUtils
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.system.CommonUtils
import com.hive.views.widgets.CommonToast
import com.hive.script.utils.ScriptHelper
/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdOpenUrl, name = "openUrl")
class CmdOpenUrl : ScriptCommand(), ScriptRegularInterface {
    var targetScheme: String? = null

    override fun onExecute() : CmdExecuteResult {
        var isSuccess = false
        ScriptHelper.runInMain {
            val cxt = ScriptProvider.getViewContext()
            if (!TextUtils.isEmpty(targetScheme)) {
                try {
                    CommonUtils.startDefaultBrowser(cxt, targetScheme)
                    isSuccess= true
                } catch (e: Exception) {
                    e.printStackTrace()
                    CommonToast.show(com.hive.i8n.R.string.sc_open_scheme_failed)
                }
            }
        }
        ScriptThreadManager.delay(getCommandDuration())
        return if (isSuccess) {
            CmdExecuteResult.maySuccess()
        } else {
            CmdExecuteResult.failure()
        }
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_OpenScheme

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_openscheme)

    override fun getCommandDescribe() =
        GlobalApp.getString(com.hive.i8n.R.string.cmd_des_openscheme, targetScheme)

    override fun getCommand() = "${cmdPrefix()} url=\"${targetScheme?.encode()}\""

    override fun getCommandIcon() = R.drawable.sc_icon_link

    

    override fun parseCmd(cmd: String) {
        targetScheme = ScriptLineTokenizer.parseKeyValueParams(cmd)["url"]?.decode()
    }

    companion object {
        fun createCommand(targetScheme: String?) = CmdOpenUrl().apply {
            this.targetScheme = targetScheme
        }
    }
}