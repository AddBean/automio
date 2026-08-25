// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.model

import androidx.annotation.Keep
import com.hive.plugin.agent.model.JsonSchemaObject
import com.hive.plugin.agent.model.JsonSchemaProperty

/** AI 助手工具参数 schema（含 i18n 描述，不直接序列化） */
@Keep
data class AIAssistantParametersSchema(
    val actionDescription: String,
    val contentDescription: String,
    val parametersDescription: String,
    val targetLanguageDescription: String,
    val analysisTypeDescription: String,
    val maxLengthDescription: String
) {
    fun toJsonSchemaObject(): JsonSchemaObject = JsonSchemaObject(
        type = "object",
        properties = mapOf(
            "action" to JsonSchemaProperty(
                type = "string",
                description = actionDescription,
                enum = listOf("chat", "generate_text", "translate", "summarize", "analyze")
            ),
            "content" to JsonSchemaProperty(
                type = "string",
                description = contentDescription
            ),
            "parameters" to JsonSchemaProperty(
                type = "object",
                description = parametersDescription,
                properties = mapOf(
                    "target_language" to JsonSchemaProperty(
                        type = "string",
                        description = targetLanguageDescription
                    ),
                    "analysis_type" to JsonSchemaProperty(
                        type = "string",
                        description = analysisTypeDescription
                    ),
                    "max_length" to JsonSchemaProperty(
                        type = "integer",
                        description = maxLengthDescription
                    )
                )
            )
        ),
        required = listOf("action", "content")
    )
}
