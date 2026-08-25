// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdCurl
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolCurl)
class ScriptToolBuilder_CmdCurl : McpToolBuilder() {

    private var cmd = CmdCurl.createCommand()

    override fun matchAction(actionName: String): Boolean {
        return "curl" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "curl",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_curl_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_curl_description),
        paramInfo = mutableListOf(
            McpActionParameters(
                name = "url",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_curl_url_desc),
                required = true,
                examples = listOf("https://api.example.com/data"),
            ),
            McpActionParameters(
                name = "method",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_curl_method_desc),
                required = true,
                examples = listOf("GET", "POST", "PUT", "DELETE"),
            ),
            McpActionParameters(
                name = "headers",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_curl_headers_desc),
                required = false,
                examples = listOf("Content-Type:application/json"),
            ),
            McpActionParameters(
                name = "form",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_curl_form_data_desc),
                required = false,
                examples = listOf("username:john,password:secret"),
            ),
            McpActionParameters(
                name = "body",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_curl_body_desc),
                required = false,
                examples = listOf("{\"name\":\"John\",\"age\":30}"),
            ),
            McpActionParameters(
                name = "paramId",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_curl_response_id_desc),
                required = false,
                examples = listOf("response"),
            )
        ),
        paramValues = emptyMap(),
    )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val url = action.paramValues["url"]

        if (url.isNullOrBlank()) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_curl_error_url_empty)
            )
        }

        val method = action.paramValues["method"]
        if (method.isNullOrBlank() || !listOf("GET", "POST", "PUT", "DELETE").contains(method)) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_curl_error_method_invalid)
            )
        }

        return CheckActionResult(true, null)
    }

    override fun getCommand(): ScriptCommand? {
        return cmd
    }

    override fun withScreenLayout() = false


    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val url = params["url"] ?: ""
        val methodStr = params["method"] ?: "GET"
        val method = CmdCurl.Method.valueOf(methodStr)

        val headers = params["headers"]?.let { headerStr ->
            if (headerStr.isNotBlank()) {
                headerStr.split(",").associate {
                    val parts = it.split(":")
                    if (parts.size >= 2) {
                        parts[0] to parts.subList(1, parts.size).joinToString(":")
                    } else {
                        parts[0] to ""
                    }
                }
            } else {
                mapOf()
            }
        } ?: mapOf()

        val form = params["form"]?.let { formStr ->
            if (formStr.isNotBlank()) {
                formStr.split(",").associate {
                    val parts = it.split(":")
                    if (parts.size >= 2) {
                        parts[0] to parts.subList(1, parts.size).joinToString(":")
                    } else {
                        parts[0] to ""
                    }
                }
            } else {
                mapOf()
            }
        } ?: mapOf()

        val body = params["body"] ?: ""
        val paramId = params["paramId"] ?: ""

        cmd = CmdCurl.createCommand(url, method, headers, form, body, paramId)
        return cmd
    }
}
