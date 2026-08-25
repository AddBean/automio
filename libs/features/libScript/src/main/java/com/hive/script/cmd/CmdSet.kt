// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.base.params.ScriptSystemParam
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.logger.ScriptLoggerView
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.extends.string
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 *
 * @author jiadou
 *
 * 格式：set param=paramId action=xxx value=xxx
 *
 * 1,读取系统参数写入到变量：${sys.clipboard}:剪切板，${sys.timestamp}:时间戳，${sys.random}:0-100随机数，${sys.device}:设备信息
 *      set param=paramId action=system value=${sys.clipboard}
 * 2,字符串写入到变量：
 *      set param=paramId action=content value="xxxxxx"
 * 3,正则匹配 targetParamId 后写入到变量：
 *      set param=paramId action=regular value=reg("pattern",targetParamId)
 * 4,公式运算结果写入到变量：
 *      set param=paramId action=expression value=exp("xxxxxx")
 */
@AutoCmdRegister(type = IDS.CmdSet, name = "set")
class CmdSet : ScriptCommand(), ScriptRegularInterface {
    private val matchAction3 = """reg\("(.*)",(.*)\)"""

    private val matchAction4 = """exp\("(.*)"\)"""

    var paramId = ScriptParamEnv.getDefaultParam()?.getFullId()

    var action: SetAction = SetAction.SYSTEM

    var action1system: ScriptSystemParam = ScriptSystemParam.CLIPBOARD

    var action2content = ""

    var action3Regular = ""

    var action3ParamId = ScriptParamEnv.getDefaultSysParam()?.getFullId()

    var action4expression: String? = null

    override fun onExecute(): CmdExecuteResult {
        when (action) {
            SetAction.SYSTEM -> {
                writeParamInner(paramId, readParam(action1system.paramId))
            }

            SetAction.CONTENT -> {
                writeParamInner(paramId, parseParamText(action2content))
            }

            SetAction.REGULAR -> {
                val value = readParam(action3ParamId) ?: ""
                val regex = action3Regular
                val r: Pattern = Pattern.compile(regex)
                val m: Matcher = r.matcher(value)
                if (m.find()) {
                    if (m.groupCount() >= 1) {
                        writeParamInner(paramId, m.group(1))
                    }
                }
            }

            SetAction.EXPRESSION -> {
                val result = action4expression?.replace("+", "-ADD-")
                var exp = parseParamText(result)
                exp = exp?.replace("-ADD-", "+")
                writeParamInner(paramId, ScriptCommandHelper.calculation(exp))
            }
        }
        return CmdExecuteResult.success()
    }

    private fun writeParamInner(paramId: String?, value: String?) {
        paramId ?: return
        value ?: return
        ScriptInterpreterObserver.notifyLogger(
            this,
            ScriptLoggerView.LogType.DEBUG,
            com.hive.i8n.R.string.sc_set_param_text.string(ScriptParamEnv.getParam(paramId)?.name ?: "", value)
        )
        writeParam(paramId, value)
    }

    override fun getCommandName() = getString(com.hive.i8n.R.string.cmd_set_name)

    override fun getCommandDescribe() =
        getString(com.hive.i8n.R.string.cmd_set_des, ScriptParamEnv.getParam(paramId)?.name)

    override fun getCommandIcon() = R.drawable.sc_icon_param

    override fun getCommand(): String {
        val paramPart = "param=${ScriptCommandHelper.paramFormat.format(paramId ?: "")}"
        val actionPart = "action=${action.value}"
        return when (action) {
            SetAction.SYSTEM -> {
                val valuePart = "value=${ScriptCommandHelper.paramFormat.format(action1system.paramId)}"
                "${cmdPrefix()} $paramPart $actionPart $valuePart"
            }

            SetAction.CONTENT -> {
                val valuePart = "value=\"${action2content.encode()}\""
                "${cmdPrefix()} $paramPart $actionPart $valuePart"
            }

            SetAction.REGULAR -> {
                val valuePart = "value=reg(\"${action3Regular.encode()}\",${ScriptCommandHelper.paramFormat.format(action3ParamId)})"
                "${cmdPrefix()} $paramPart $actionPart $valuePart"
            }

            SetAction.EXPRESSION -> {
                val valuePart = "value=exp(\"${action4expression?.encode()}\")"
                "${cmdPrefix()} $paramPart $actionPart $valuePart"
            }
        }
    }

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        paramId = ScriptCommandHelper.parseParamsId(p["param"] ?: "")
        val actionStr = p["action"] ?: ""
        val value = p["value"]?.trim() ?: ""
        action = SetAction.entries.find { it.value == actionStr } ?: SetAction.SYSTEM
        parseValueByAction(action, value)
    }

    private fun parseValueByAction(action: SetAction, value: String) {
        when (action) {
            SetAction.SYSTEM -> {
                action1system = ScriptSystemParam.fromValue(ScriptCommandHelper.parseParamsId(value))
            }
            SetAction.CONTENT -> {
                action2content = value.decode()
            }
            SetAction.REGULAR -> {
                val m1 = Pattern.compile(matchAction3).matcher(value)
                if (m1.find()) {
                    action3Regular = m1.group(1)?.decode() ?: ""
                    action3ParamId = ScriptCommandHelper.parseParamsId(m1.group(2)?.trim() ?: "")
                }
            }
            SetAction.EXPRESSION -> {
                val m1 = Pattern.compile(matchAction4).matcher(value)
                if (m1.find()) {
                    action4expression = m1.group(1)?.decode() ?: ""
                }
            }
        }
    }

    override fun getPermissionRequest(): List<String>? {
        return if (action1system == ScriptSystemParam.LOCATION) {
            mutableListOf(ScriptHelper.PERMISSION_LOCATION)
        } else {
            null
        }
    }

    enum class SetAction(val value: String) {
        SYSTEM("system"), CONTENT("content"), REGULAR("regular"), EXPRESSION("expression");

        override fun toString(): String {
            return value
        }
    }

    companion object {

        fun createAction1(paramId: String, action1system: ScriptSystemParam): CmdSet {
            val cmd = CmdSet()
            cmd.paramId = paramId
            cmd.action = SetAction.SYSTEM
            cmd.action1system = action1system
            return cmd
        }

        fun createAction2(paramId: String, action2content: String): CmdSet {
            val cmd = CmdSet()
            cmd.paramId = paramId
            cmd.action = SetAction.CONTENT
            cmd.action2content = action2content
            return cmd
        }

        fun createAction3(paramId: String, action3Regular: String, action3ParamId: String): CmdSet {
            val cmd = CmdSet()
            cmd.paramId = paramId
            cmd.action = SetAction.REGULAR
            cmd.action3Regular = action3Regular
            cmd.action3ParamId = action3ParamId
            return cmd
        }

        fun createAction4(paramId: String, action4expression: String): CmdSet {
            val cmd = CmdSet()
            cmd.paramId = paramId
            cmd.action = SetAction.EXPRESSION
            cmd.action4expression = action4expression
            return cmd
        }

    }
}