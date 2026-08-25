// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.base.ScriptCommand
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptThreadManager
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolMemoryNote)
class ScriptToolBuilder_MemoryNote : McpToolBuilder() {

    private val defaultKey = "default"
    private val keyRegex = Regex("^[A-Za-z_][A-Za-z0-9_]{0,63}$")

    override fun matchAction(actionName: String): Boolean {
        return "memoryNote" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "memoryNote",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_description),
        paramInfo = mutableListOf(
            McpActionParameters(
                name = "op",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_param_op_desc),
                required = true,
                examples = listOf("get", "set", "listKey"),
            ),
            McpActionParameters(
                name = "group",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_param_group_desc),
                required = false,
                examples = listOf("memory"),
            ),
            McpActionParameters(
                name = "key",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_param_key_desc),
                required = false,
                examples = listOf("state", "counter"),
            ),
            McpActionParameters(
                name = "value",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_param_value_desc),
                required = false,
                examples = listOf("3", "step:fill_form"),
            ),
            McpActionParameters(
                name = "default",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_param_default_desc),
                required = false,
                examples = listOf("0", ""),
            ),
            McpActionParameters(
                name = "persist",
                type = "boolean",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_param_persist_desc),
                required = true,
                examples = listOf("true", "false"),
            ),
        ),
        paramValues = emptyMap(),
    )

    override fun supportDelay(): Boolean = false

    override fun withScreenLayout(): Boolean = false

    override fun getCommand(): ScriptCommand? = null

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val op = action.paramValues["op"]?.trim()?.lowercase()
        if (op !in listOf("get", "set", "listkey")) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_error_invalid_op)
            )
        }

        val group = resolveGroup(action.paramValues["group"])
        if (!group.matches(Regex("^\\w+$"))) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_error_invalid_group)
            )
        }

        val key = action.paramValues["key"]?.trim().orEmpty()
        if (key.isNotEmpty() && !keyRegex.matches(key)) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_error_invalid_key)
            )
        }

        if (op == "set" && action.paramValues["value"] == null) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_error_missing_value)
            )
        }

        return CheckActionResult(true, null)
    }

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        // MemoryNote 不创建命令，直接返回 null
        return null
    }

    override suspend fun executeAction(params: Map<String, String>): ActionResult {
        // MemoryNote 是纯数据工具，不走 ScriptCommand 执行链路
        ScriptThreadManager.delay(ScriptConst.Cmd_Delay_Default)
        val env = ScriptParamEnv.getParamEnv()
        val op = params["op"]?.trim()?.lowercase().orEmpty()
        val group = resolveGroup(params["group"])
        return when (op) {
            "set" -> executeSet(env, group, params)
            "get" -> executeGet(env, group, params)
            "listkey" -> executeListKey(env, group)
            else -> ActionResult.failure(
                message = GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_error_invalid_op)
            )
        }
    }

    private fun executeSet(
        env: ScriptParamEnv,
        group: String,
        params: Map<String, String>
    ): ActionResult {
        val key = resolveKey(params["key"])
        val value = params["value"] ?: ""
        val persist = params["persist"]?.trim()?.lowercase() in listOf("true", "1", "yes")
        ensureGroup(env, group)
        val fullId = "$group.$key"
        if (ScriptParamEnv.getParam(fullId) == null) {
            env.addParam(fullId, key, value)
        }
        env.writeParam(fullId, value, persist = persist)
        return ActionResult.success(
            message = GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_set_success),
            data = mapOf(
                "op" to "set",
                "group" to group,
                "key" to key,
                "value" to value,
                "persisted" to persist
            )
        )
    }

    private fun executeGet(
        env: ScriptParamEnv,
        group: String,
        params: Map<String, String>
    ): ActionResult {
        val key = resolveKey(params["key"])
        val fullId = "$group.$key"
        // 恢复：param 不存在时从 MMKV 加载并创建，否则 get 永远读不到持久化数据
        if (ScriptParamEnv.getParam(fullId) == null) {
            ensureGroup(env, group)
            env.addParam(fullId, key, "")
        }
        val value = env.readParam(fullId) ?: params["default"].orEmpty()
        return ActionResult.success(
            message = GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_get_success),
            data = mapOf(
                "op" to "get",
                "group" to group,
                "key" to key,
                "value" to value
            )
        )
    }

    private fun executeListKey(
        env: ScriptParamEnv,
        group: String
    ): ActionResult {
        val groupData = env.readGroup(group)
        val items = groupData?.params.orEmpty().map { item ->
            mapOf(
                "key" to item.id,
                "fullId" to item.getFullId(),
                "name" to item.name
            )
        }
        return ActionResult.success(
            message = GlobalApp.getString(com.hive.i8n.R.string.tool_memory_note_list_success),
            data = mapOf(
                "op" to "listKey",
                "group" to group,
                "count" to items.size,
                "items" to items
            )
        )
    }

    private fun ensureGroup(env: ScriptParamEnv, group: String) {
        if (env.hasGroup(group)) return
        env.parseParam("def group $group #$group")
    }

    private fun resolveGroup(input: String?): String {
        val group = input?.trim().orEmpty()
        return if (group.isEmpty()) "memory" else group
    }

    private fun resolveKey(input: String?): String {
        val key = input?.trim().orEmpty()
        return if (key.isEmpty()) defaultKey else key
    }
}
