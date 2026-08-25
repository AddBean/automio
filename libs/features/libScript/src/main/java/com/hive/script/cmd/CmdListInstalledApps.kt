// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.files.utils.XAppInfoParser
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.utils.GlobalApp
import com.hive.utils.extends.toJson
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdListInstalledApps, name = "listInstalledApps")
class CmdListInstalledApps : ScriptCommand(), ScriptRegularInterface {

    private var dialogShowing: AtomicBoolean = AtomicBoolean(false)

    override fun onExecute(): CmdExecuteResult {
        dialogShowing.set(true)
        if ((ScriptProvider.sAppList?.size ?: 0) > 0) {
            return CmdExecuteResult.success(getAppData())
        }
        ScriptProvider.updateApp {
            dialogShowing.set(false)
        }
        while (dialogShowing.get()) {
            ScriptThreadManager.delay(1000)
        }

        if ((ScriptProvider.sAppList?.size ?: 0) > 0) {
            return CmdExecuteResult.success(getAppData())
        } else {
            return CmdExecuteResult.failure()
        }
    }

    private fun getAppData(): String? {
        return ScriptProvider.sAppList?.map { it.cardData as XAppInfoParser.AppInfo }
            ?.map { mutableMapOf(it.appName to it.packageName) }?.toJson()
    }


    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_get_app_list)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_get_app_list)

    override fun getCommand() = cmdPrefix()

    override fun parseCmd(cmd: String) {

    }

    override fun getPermissionRequest() = null

    override fun getCommandIcon() = R.drawable.sc_icon_toast


    companion object {
        fun createCommand() = CmdListInstalledApps()
    }
}