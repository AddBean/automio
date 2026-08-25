// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/** JSON Schema 属性定义（用于工具参数 schema） */
@Keep
data class JsonSchemaProperty(
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("enum") val enum: List<String>? = null,
    @SerializedName("properties") val properties: Map<String, JsonSchemaProperty>? = null,
    @SerializedName("items") val items: JsonSchemaProperty? = null
)

/** JSON Schema 对象定义 */
@Keep
data class JsonSchemaObject(
    @SerializedName("type") val type: String = "object",
    @SerializedName("properties") val properties: Map<String, JsonSchemaProperty>,
    @SerializedName("required") val required: List<String>
)
