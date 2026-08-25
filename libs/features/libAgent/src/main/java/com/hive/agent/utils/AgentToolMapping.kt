// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import com.google.gson.JsonObject
import com.hive.plugin.agent.model.ToolDefinition

object AgentToolMapping {
    fun <TDef, TFunc> map(
        tools: List<ToolDefinition>?,
        createDef: (String, TFunc) -> TDef,
        createFunc: (String, String, JsonObject) -> TFunc
    ): List<TDef>? {
        return tools?.map { toolDef ->
            createDef(
                toolDef.type,
                createFunc(toolDef.function.name, toolDef.function.description, toolDef.function.parameters)
            )
        }
    }
}


