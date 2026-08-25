// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.mcp.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.hive.compon.R
import com.hive.utils.GlobalApp

/** MCP 动作 - 输入: LLM/工具 → Agent */
@Keep
data class McpAction(
    @SerializedName("action") val action: String,
    @SerializedName("paramInfo") val paramInfo: MutableList<McpActionParameters> = mutableListOf(),
    @SerializedName("paramValues") val paramValues: Map<String, String> = emptyMap(),
    @SerializedName("description") val description: String? = null,
    @SerializedName("extraName") val extraName: String? = null,
    @SerializedName("extraType") val extraType: String? = "buildin"
)

/** MCP 动作参数 - 用于描述动作的输入参数 */
@Keep
data class McpActionParameters(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("format") val format: String? = null,
    @SerializedName("required") val required: Boolean = true,
    @SerializedName("description") val description: String? = null,
    @SerializedName("examples") val examples: List<String>? = null,
    @SerializedName("localParam") val localParam: String? = null
)

/** 动作执行结果 */
@Keep
data class ActionResult(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") var data: Any? = null,
    @SerializedName("extra") var extra: Any? = null,
    @SerializedName("files") var files: List<McpResultFile>? = null
) {
    companion object {

        fun success(
            message: String? = null,
            data: Any? = null,
            files: List<McpResultFile>? = null,
            extra: Any? = null
        ): ActionResult {
            return ActionResult(
                success = true,
                message = message
                    ?: GlobalApp.getString(com.hive.i8n.R.string.mcp_operation_success),
                data = data,
                files = files,
                extra = extra
            )
        }

        fun failure(
            message: String? = null,
            data: Any? = null,
            files: List<McpResultFile>? = null
        ): ActionResult {
            return ActionResult(
                false,
                message ?: GlobalApp.getString(com.hive.i8n.R.string.mcp_operation_failed),
                data,
                files
            )
        }
    }
}

/** MCP 文件项（ActionResult.files、tools/call 响应等通用） */
@Keep
data class McpResultFile(
    @SerializedName("name") val name: String? = null,
    @SerializedName("mimeType") val mimeType: String? = null,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("base64") val base64: String? = null
)

/** 实现信息 */
@Keep
data class ImplementationInfo(
    @SerializedName("name") val name: String,
    @SerializedName("version") val version: String,
    @SerializedName("description") val description: String
)

/** 服务状态 */
@Keep
data class ServiceStatus(
    @SerializedName("isRunning") val isRunning: Boolean,
    @SerializedName("implementationsCount") val implementationsCount: Int,
    @SerializedName("supportedActions") val supportedActions: List<String>
)
