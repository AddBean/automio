// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.mcp.model

import androidx.annotation.Keep
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/** 可注册的 MCP 功能的通用接口 */
interface McpFeature {
    val name: String
    val description: String
}

/** MCP 工具的定义 */
@Keep
data class McpTool(
    @SerializedName("name") override val name: String,
    @SerializedName("description") override val description: String,
    @SerializedName("extraName") val extraName: String,
    @SerializedName("extraType") val extraType: String,
    @SerializedName("inputSchema") val inputSchema: JsonObject,
    val handler: suspend (JsonObject) -> ActionResult
) : McpFeature

/** MCP 资源的定义 */
@Keep
data class McpResource(
    @SerializedName("uri") val uri: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("mimeType") val mimeType: String,
    val handler: suspend () -> ActionResult
)

/** MCP 提示的定义 */
@Keep
data class McpPrompt(
    @SerializedName("name") override val name: String,
    @SerializedName("description") override val description: String,
    @SerializedName("arguments") val arguments: JsonObject,
    val handler: suspend (JsonObject) -> ActionResult
) : McpFeature