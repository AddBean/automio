// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.AiRequestHelper
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.logger.ScriptLoggerView
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 */
@AutoCmdRegister(type = IDS.CmdAiRequest, name = "aiRequest")
class CmdAiRequest : ScriptCommand(), ScriptRegularInterface {
    var prompt: String = ""

    var failureMsg: String = ""

    var targetParamId = ScriptParamEnv.getDefaultParam()?.getFullId()

    override fun onExecute() : CmdExecuteResult {
//        if (ScriptRightCountHelper.isOverLimit(ScriptRightCountHelper.CountKey.DAILY_AI_COUNT)) {
//            CommonToast.getInstance().showToast(getString(com.hive.i8n.R.string.ai_request_limit))
//            ScriptInterpreterObserver.notifyLogger(
//                this, ScriptLoggerView.LogType.ERROR, getString(com.hive.i8n.R.string.ai_request_limit)
//            )
//            throw ScriptInterruptedException()
//        }
        val content = parseParamText(prompt)
        ScriptInterpreterObserver.notifyLogger(
            this, ScriptLoggerView.LogType.DEBUG, content
        )
        val result = AiRequestHelper.requestSync(content ?: "")
        if (result?.isNotEmpty() == true) {
            ScriptInterpreterObserver.notifyLogger(
                this, ScriptLoggerView.LogType.DEBUG, result
            )
            writeParam(targetParamId, result)
            return CmdExecuteResult.success(result)
        } else {
            val msg = AiRequestHelper.lastErrorMessage ?: failureMsg
            ScriptInterpreterObserver.notifyLogger(
                this, ScriptLoggerView.LogType.DEBUG, msg
            )
            writeParam(targetParamId, msg)
            return CmdExecuteResult.failure(msg)
        }
    }

    override fun getCommandName() = getString(com.hive.i8n.R.string.cmd_ai_request_name)

    override fun getCommandDescribe() = getString(com.hive.i8n.R.string.cmd_ai_request_des)

    override fun getCommandIcon() = R.drawable.sc_ai_request

    override fun getCommand(): String {
        return "${cmdPrefix()} output=$targetParamId failure=\"${failureMsg.encode()}\" prompt=\"${prompt.encode()}\""
    }

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        targetParamId = p["output"] ?: ScriptParamEnv.getDefaultParam()?.getFullId()
        failureMsg = p["failure"]?.decode() ?: failureMsg
        prompt = p["prompt"]?.decode() ?: prompt
    }

    override fun getPermissionRequest(): List<String> {
        return mutableListOf(ScriptHelper.PERMISSION_NETWORK)
    }

    companion object {

        fun createCommand(
            prompt: String
        ): CmdAiRequest {
            val cmd = CmdAiRequest()
            cmd.prompt = prompt
            cmd.targetParamId = ScriptParamEnv.getDefaultParam()?.getFullId()
            return cmd
        }

    }
}
