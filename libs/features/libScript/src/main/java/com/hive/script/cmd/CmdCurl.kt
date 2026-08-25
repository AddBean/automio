// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.text.TextUtils
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.ScriptHelper
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.utils.ScriptNetHelper
import com.hive.script.views.logger.ScriptLoggerView
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.extends.string

/**
 *
 * @author jiadou
 * 格式为：curl url=... method=GET headers=... form=... body=... output=p0
 */
@AutoCmdRegister(type = IDS.CmdCurl, name = "curl")
class CmdCurl : ScriptCommand(), ScriptRegularInterface {
    var method: Method = Method.GET

    var url: String = ""

    var headers: Map<String, String> = mapOf()

    var form: Map<String, String> = mapOf()

    var body: String = ""

    var targetParamId = ScriptParamEnv.getDefaultParam()?.getFullId()

    override fun onExecute(): CmdExecuteResult {
        val urlReal = parseParamText(url) ?: return CmdExecuteResult.failure(
            getString(com.hive.i8n.R.string.sc_curl_error_url)
        )
        ScriptInterpreterObserver.notifyLogger(
            this,
            ScriptLoggerView.LogType.DEBUG,
            com.hive.i8n.R.string.sc_curl_start_request_url.string(urlReal)
        )
        val requestHeaders = convertParamMap(headers)
        val requestFrom = convertParamMap(form)
        val requestBody = parseParamText(body) ?: ""
        val result =
            ScriptNetHelper.curl(urlReal, method.value, requestHeaders, requestFrom, requestBody)
        ScriptInterpreterObserver.notifyLogger(
            this, ScriptLoggerView.LogType.DEBUG, result
        )
        writeParam(targetParamId, result)
        return if (TextUtils.isEmpty(result)) {
            CmdExecuteResult.failure(getString(com.hive.i8n.R.string.sc_curl_error_url))
        } else {
            CmdExecuteResult.success(data = result)
        }
    }

    private fun convertParamMap(map: Map<String, String>?): Map<String, String> {
        return map?.mapNotNull {
            (parseParamText(it.key) ?: "") to (parseParamText(it.value) ?: "")
        }?.toMap() ?: mapOf()
    }

    override fun getCommandName() = getString(com.hive.i8n.R.string.cmd_curl_name)

    override fun getCommandDescribe() = getString(com.hive.i8n.R.string.cmd_curl_des)

    override fun getCommandIcon() = R.drawable.sc_icon_curl

    override fun getCommand(): String {
        val cUrl = url.encode()
        val cHeaders = headers.map { "${it.key}:${it.value}" }.joinToString(",").encode()
        val cForm = form.map { "${it.key}:${it.value}" }.joinToString(",").encode()
        val cBody = body.encode()
        return "${cmdPrefix()} url=$cUrl method=${method.value} headers=$cHeaders form=$cForm body=$cBody output=$targetParamId"
    }

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        url = p["url"]?.decode() ?: url
        method = Method.valueOf(p["method"] ?: "GET")
        targetParamId = p["output"] ?: targetParamId
        body = p["body"]?.decode() ?: body
        p["headers"]?.decode()?.takeIf { it.isNotEmpty() }?.let { cHeaders ->
            headers = cHeaders.split(",").mapNotNull {
                val kv = it.split(":", limit = 2)
                if (kv.size == 2) kv[0] to kv[1] else null
            }.toMap()
        }
        p["form"]?.decode()?.takeIf { it.isNotEmpty() }?.let { cForm ->
            form = cForm.split(",").mapNotNull {
                val kv = it.split(":", limit = 2)
                if (kv.size == 2) kv[0] to kv[1] else null
            }.toMap()
        }
    }

    override fun getPermissionRequest(): List<String> {
        return mutableListOf(ScriptHelper.PERMISSION_NETWORK)
    }

    enum class Method(val value: String) {
        GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE");

        override fun toString(): String {
            return value
        }
    }

    companion object {

        fun createCommand(): CmdCurl {
            return createCommand(
                "",
                Method.GET,
                mapOf(),
                mapOf(),
                "",
                ScriptParamEnv.getDefaultParam()?.getFullId() ?: ""
            )
        }

        fun createCommand(
            url: String,
            method: Method,
            headers: Map<String, String>,
            form: Map<String, String>,
            body: String,
            paramId: String
        ): CmdCurl {
            val cmd = CmdCurl()
            cmd.url = url
            cmd.method = method
            cmd.headers = headers
            cmd.form = form
            cmd.body = body
            cmd.targetParamId = paramId
            return cmd
        }

    }
}