// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.params.ScriptParamEnv
import com.hive.utils.GlobalApp
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.extends.encode
import com.hive.utils.extends.decode

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 *
 * 脚本结束指令，可以同时设置多个变量
 * 格式：
 * 1. scriptEnd
 * 2. scriptEnd main.param0="value1" main.param1="value2"
 */
@AutoCmdRegister(type = IDS.CmdScriptEnd, name = "scriptEnd")
class CmdScriptEnd : ScriptCommand() , ScriptRegularInterface {

    // 存储变量设置信息的列表
    var paramSettings = mutableListOf<ParamSetting>()


    override fun onExecute() : CmdExecuteResult {
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Spot)

        // 执行所有变量设置
        paramSettings.forEach { setting ->
            writeParam(setting.paramId, parseParamText(setting.content))
        }

        return CmdExecuteResult.success()
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommand(): String {
        if (paramSettings.isEmpty()) {
            return cmdPrefix()
        }
        fun q(s: String) = s.encode()
        val params = paramSettings.joinToString(" ") { setting ->
            "${setting.paramId}=${q(setting.content)}"
        }
        return "${cmdPrefix()} $params"
    }

    override fun getCommandName(): String = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_script_done)

    override fun getCommandDescribe(): String {
        if (paramSettings.isEmpty()) {
            return GlobalApp.getString(com.hive.i8n.R.string.cmd_name_script_done_describe)
        }

        val paramNames = paramSettings.joinToString(", ") { setting ->
            ScriptParamEnv.getParam(setting.paramId)?.name ?: setting.paramId
        }

        return GlobalApp.getString(com.hive.i8n.R.string.cmd_name_script_done_describe) +
                GlobalApp.getString(com.hive.i8n.R.string.cmd_script_done_set_vars, paramNames)
    }

    override fun getCommandIcon() = R.drawable.sc_icon_exit

    override fun isSupportDelay() = false

    override fun isSupportDrag() = false

    override fun isSupportOffset() = false

    override fun isSupportRect() = false

    override fun isGroupCommand() = false

    override fun parseCmd(cmd: String) {
        paramSettings.clear()
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        p.forEach { (paramId, value) ->
            paramSettings.add(ParamSetting(paramId, content = value.decode()))
        }
    }

    override fun getPermissionRequest() = null
    
    // 参数设置数据类
    data class ParamSetting(
        val paramId: String,
        val content: String = ""
    )

    companion object {
        fun createCommand() = CmdScriptEnd()
    }
}