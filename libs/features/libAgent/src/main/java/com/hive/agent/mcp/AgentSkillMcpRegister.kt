// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.mcp

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.hive.agent.R
import com.hive.agent.XAgent
import com.hive.agent.skill.SkillIdGenerator
import com.hive.agent.skill.SkillPersistence
import com.hive.agent.skill.SkillToolLogger
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.ExecutionContexts
import com.hive.plugin.agent.ExecutionContextType
import com.hive.plugin.mcp.McpConst
import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpTool
import com.hive.plugin.agent.model.RunSkillOptions
import com.hive.plugin.agent.model.RunSkillRequest
import com.hive.plugin.provider.IAgentProvider
import com.hive.plugin.provider.IMcpProvider
import com.hive.script.scope.ScriptVisibilityRegistry
import com.hive.plugin.agent.model.SkillSpec
import com.hive.utils.GlobalApp
import com.hive.utils.utils.GsonHelper

/** -1 = unlimited */
private const val DEFAULT_MAX_ROUNDS = -1
private const val DEFAULT_TIMEOUT_MS = -1L

/**
 * 将 Skill 的“入口能力”注册到本地 MCP 服务（ToolRegistry）。
 * 这样它们会自然出现在 tools/list，并可通过 tools/call 调用。
 */
object AgentSkillMcpRegister {

    fun register(mcpProvider: IMcpProvider) {
        mcpProvider.registerTool(buildSkillTool())
    }

    private fun buildSkillTool(): McpTool {
        val inputSchema = JsonObject().apply {
            addProperty("\$schema", "http://json-schema.org/draft-07/schema#")
            addProperty("type", "object")
            addProperty("title", "skill - Unified Skill MCP Tool")
            add(
                "properties",
                JsonObject().apply {
                    add("action", JsonObject().apply {
                        addProperty("type", "string")
                        addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_action_desc))
                        add(
                            "enum",
                            JsonArray().apply {
                                add(JsonPrimitive("help"))
                                add(JsonPrimitive("list"))
                                add(JsonPrimitive("run"))
                                add(JsonPrimitive("create"))
                                add(JsonPrimitive("update"))
                                add(JsonPrimitive("delete"))
                            }
                        )
                    })
                    add("userPrompt", JsonObject().apply {
                        addProperty("type", "string")
                        addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_user_prompt_desc))
                    })
                    add("options", JsonObject().apply {
                        addProperty("type", "object")
                        addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_options_desc))
                        add(
                            "properties",
                            JsonObject().apply {
                                add("timeoutMs", JsonObject().apply {
                                    addProperty("type", "integer")
                                    addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_options_timeout_ms_desc))
                                })
                                add("maxRounds", JsonObject().apply {
                                    addProperty("type", "integer")
                                    addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_options_max_rounds_desc))
                                })
                                add("memoryGroup", JsonObject().apply {
                                    addProperty("type", "string")
                                    addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_options_memory_group_desc))
                                })
                                add("depth", JsonObject().apply {
                                    addProperty("type", "integer")
                                    addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_options_depth_desc))
                                })
                                add("attachments", JsonObject().apply {
                                    addProperty("type", "array")
                                    add("items", JsonObject().apply { addProperty("type", "string") })
                                    addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_options_attachments_desc))
                                })
                            }
                        )
                    })
                    add("id", JsonObject().apply {
                        addProperty("type", "string")
                        addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_id_desc))
                    })
                    add("name", JsonObject().apply {
                        addProperty("type", "string")
                        addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_name_desc))
                    })
                    add("description", JsonObject().apply {
                        addProperty("type", "string")
                        addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_description_desc))
                    })
                    add("systemPrompt", JsonObject().apply {
                        addProperty("type", "string")
                        addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_system_prompt_desc))
                    })
                    add("allowedToolNames", JsonObject().apply {
                        addProperty("type", "array")
                        add("items", JsonObject().apply { addProperty("type", "string") })
                        addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_allowed_tools_desc))
                    })
                    add("maxRounds", JsonObject().apply {
                        addProperty("type", "integer")
                        addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_max_rounds_desc))
                    })
                    add("timeoutMs", JsonObject().apply {
                        addProperty("type", "integer")
                        addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_timeout_ms_desc))
                    })
                    add("fallbackSkillId", JsonObject().apply {
                        addProperty("type", "string")
                        addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_fallback_skill_id_desc))
                    })
                    add("memoryGroup", JsonObject().apply {
                        addProperty("type", "string")
                        addProperty("description", GlobalApp.getString(com.hive.i8n.R.string.agent_skill_schema_memory_group_desc))
                    })
                }
            )
            add(
                "required",
                JsonArray().apply {
                    add(JsonPrimitive("action"))
                }
            )
        }

        return McpTool(
            name = McpConst.Tool_Name_Prefix_BuildIn + "skill",
            description = GlobalApp.getString(com.hive.i8n.R.string.agent_skill_mcp_tool_desc),
            extraName = GlobalApp.getString(com.hive.i8n.R.string.agent_skill_mcp_tool_extra_name),
            extraType = McpConst.Tool_Type_BuildIn,
            inputSchema = inputSchema,
            handler = { params ->
                try {
                    val map = GsonHelper.getInstance().fromJson<Map<String, Any?>>(
                        params.toString(),
                        object : com.google.gson.reflect.TypeToken<Map<String, Any?>>() {}.type
                    )
                    val action = map["action"]?.toString()?.trim()?.lowercase()
                        ?: return@McpTool ActionResult.failure(GlobalApp.getString(com.hive.i8n.R.string.agent_skill_error_missing_action))

                    SkillToolLogger.d("action=$action")

                    when (action) {
                        "help" -> ActionResult.success(data = loadHelpText())
                        "list" -> {
                            val allSkills = XAgent.getInstance().listSkills()
                            val publicIds = ScriptVisibilityRegistry.getPublicSkillIds()
                            val skills = allSkills.filter { it.id in publicIds }
                            val list = skills.map { spec ->
                                mutableMapOf(
                                    "id" to spec.id,
                                    "name" to spec.name,
                                    "description" to spec.description,
                                    "allowedToolNames" to spec.allowedToolNames
                                )
                            }
                            SkillToolLogger.d("list count=${list.size}")
                            ActionResult.success(data = list)
                        }

                        "run" -> handleRun(params, map)
                        "create" -> handleCreate(params, map)
                        "update" -> handleUpdate(params, map)
                        "delete" -> handleDelete(map)
                        else -> ActionResult.failure(GlobalApp.getString(com.hive.i8n.R.string.agent_skill_error_unsupported_action, action))
                    }
                } catch (e: Exception) {
                    ActionResult.failure(message = e.message ?: GlobalApp.getString(com.hive.i8n.R.string.agent_skill_error_execute))
                }
            }
        )
    }

    private fun handleRun(params: JsonObject, map: Map<String, Any?>): ActionResult {
        val userPrompt = map["userPrompt"]?.toString()
            ?: return ActionResult.failure(GlobalApp.getString(com.hive.i8n.R.string.agent_skill_error_missing_user_prompt))

        val rawId = map["id"]?.toString()?.trim().orEmpty()
        val options = map["options"]
        val runOptions = options?.let {
            GsonHelper.getInstance().fromJson(
                GsonHelper.getInstance().toJson(it),
                RunSkillOptions::class.java
            )
        }

        if (rawId.isNotBlank()) {
            // 常规 run：使用 local id，由 SkillRunner 按 scope 解析
            val skillId = rawId
            SkillToolLogger.d("run skillId=$skillId userPrompt=$userPrompt")
            val request = RunSkillRequest(skillId = skillId, userPrompt = userPrompt, options = runOptions)
            val result = kotlinx.coroutines.runBlocking { XAgent.getInstance().runSkill(request) }
            SkillToolLogger.d("run result status=${result.status} summary=${result.summary}")
            return ActionResult.success(data = result)
        }

        // Inline run：无 id，用 create 参数临时注册并执行
        val inlineSpec = parseInlineSpec(params, map)
            ?: return ActionResult.failure(GlobalApp.getString(com.hive.i8n.R.string.agent_skill_error_inline_run_missing_params))
        val tempId = "skill.inline.${System.currentTimeMillis()}"
        val spec = inlineSpec.copy(id = tempId)

        SkillToolLogger.d("run inline skillId=$tempId userPrompt=$userPrompt")
        val agentProvider = ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider
        try {
            agentProvider?.registerSkillSpec(spec)
            val request = RunSkillRequest(skillId = tempId, userPrompt = userPrompt, options = runOptions)
            val result = kotlinx.coroutines.runBlocking { XAgent.getInstance().runSkill(request) }
            SkillToolLogger.d("run inline result status=${result.status} summary=${result.summary}")
            return ActionResult.success(data = result)
        } finally {
            agentProvider?.unregisterSkillSpec(tempId)
        }
    }

    /** 解析 inline run 的 create 参数，不持久化。maxRounds/timeoutMs 默认 -1，memoryGroup 可省略 */
    private fun parseInlineSpec(params: JsonObject, map: Map<String, Any?>): SkillSpec? {
        val name = map["name"]?.toString()?.trim().orEmpty()
        val description = map["description"]?.toString()?.trim().orEmpty()
        val systemPrompt = map["systemPrompt"]?.toString()?.trim().orEmpty()
        if (name.isBlank() || description.isBlank() || systemPrompt.isBlank()) return null
        if (!params.has("allowedToolNames")) return null

        val allowedToolNames = parseStringList(params["allowedToolNames"])
        if (allowedToolNames.isEmpty()) return null

        val maxRounds = (map["maxRounds"] as? Number)?.toInt() ?: DEFAULT_MAX_ROUNDS
        val timeoutMs = (map["timeoutMs"] as? Number)?.toLong() ?: DEFAULT_TIMEOUT_MS
        val fallbackSkillId = map["fallbackSkillId"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val memoryGroup = map["memoryGroup"]?.toString()?.trim()?.takeIf { it.isNotBlank() }

        return SkillSpec(
            id = "",
            name = name,
            description = description,
            systemPrompt = systemPrompt,
            allowedToolNames = allowedToolNames,
            maxRounds = maxRounds,
            timeoutMs = timeoutMs,
            fallbackSkillId = fallbackSkillId,
            memoryGroup = memoryGroup
        )
    }

    private fun handleCreate(params: JsonObject, map: Map<String, Any?>): ActionResult {
        // id 由本地生成以保证唯一性，create 不接受 id 参数
        val id = SkillIdGenerator.generate()

        val name = map["name"]?.toString()?.trim().orEmpty()
        val description = map["description"]?.toString()?.trim().orEmpty()
        val systemPrompt = map["systemPrompt"]?.toString()?.trim().orEmpty()

        if (name.isBlank()) return ActionResult.failure(GlobalApp.getString(com.hive.i8n.R.string.agent_skill_create_missing_name))
        if (description.isBlank()) return ActionResult.failure(GlobalApp.getString(com.hive.i8n.R.string.agent_skill_create_missing_description))
        if (systemPrompt.isBlank()) return ActionResult.failure(GlobalApp.getString(com.hive.i8n.R.string.agent_skill_create_missing_system_prompt))
        if (!params.has("allowedToolNames")) return ActionResult.failure(GlobalApp.getString(com.hive.i8n.R.string.agent_skill_create_missing_allowed_tool_names))

        SkillToolLogger.d("create id=$id")
        val allowedToolNames = parseStringList(params["allowedToolNames"])
        val maxRounds = (map["maxRounds"] as? Number)?.toInt() ?: DEFAULT_MAX_ROUNDS
        val timeoutMs = (map["timeoutMs"] as? Number)?.toLong() ?: DEFAULT_TIMEOUT_MS
        val fallbackSkillId = map["fallbackSkillId"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val memoryGroup = map["memoryGroup"]?.toString()?.trim()?.takeIf { it.isNotBlank() }

        val spec = SkillSpec(
            id = id,
            name = name,
            description = description,
            systemPrompt = systemPrompt,
            allowedToolNames = allowedToolNames,
            maxRounds = maxRounds,
            timeoutMs = timeoutMs,
            fallbackSkillId = fallbackSkillId,
            memoryGroup = memoryGroup
        )
        persistAndRegister(spec)
        return ActionResult.success(
            data = mapOf(
                "id" to spec.id,
                "name" to spec.name,
                "description" to spec.description,
                "allowedToolNames" to spec.allowedToolNames
            )
        )
    }

    private fun handleUpdate(params: JsonObject, map: Map<String, Any?>): ActionResult {
        val id = map["id"]?.toString()?.trim().orEmpty()
        if (id.isBlank()) return ActionResult.failure(GlobalApp.getString(com.hive.i8n.R.string.agent_skill_update_missing_id))
        SkillToolLogger.d("update id=$id")

        val base = SkillPersistence.loadCustomSkills().firstOrNull { it.id == id }
            ?: return ActionResult.failure(GlobalApp.getString(com.hive.i8n.R.string.agent_skill_update_not_found, id))

        val name = map["name"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val description = map["description"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val systemPrompt = map["systemPrompt"]?.toString()?.trim()?.takeIf { it.isNotBlank() }

        val hasAllowedToolNames = params.has("allowedToolNames")
        val allowedToolNames = if (hasAllowedToolNames) parseStringList(params["allowedToolNames"]) else base.allowedToolNames

        val hasMaxRounds = map.containsKey("maxRounds")
        val maxRounds = if (hasMaxRounds) (map["maxRounds"] as? Number)?.toInt() else base.maxRounds

        val hasTimeoutMs = map.containsKey("timeoutMs")
        val timeoutMs = if (hasTimeoutMs) (map["timeoutMs"] as? Number)?.toLong() else base.timeoutMs

        val hasFallback = map.containsKey("fallbackSkillId")
        val fallbackSkillId = if (hasFallback) {
            map["fallbackSkillId"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
        } else {
            base.fallbackSkillId
        }

        val hasMemoryGroup = map.containsKey("memoryGroup")
        val memoryGroup = if (hasMemoryGroup) {
            map["memoryGroup"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
        } else {
            base.memoryGroup
        }

        val merged = base.copy(
            name = name ?: base.name,
            description = description ?: base.description,
            systemPrompt = systemPrompt ?: base.systemPrompt,
            allowedToolNames = allowedToolNames,
            maxRounds = maxRounds,
            timeoutMs = timeoutMs,
            fallbackSkillId = fallbackSkillId,
            memoryGroup = memoryGroup
        )
        persistAndRegister(merged)
        return ActionResult.success(
            data = mapOf(
                "id" to merged.id,
                "name" to merged.name,
                "description" to merged.description,
                "allowedToolNames" to merged.allowedToolNames
            )
        )
    }

    private fun handleDelete(map: Map<String, Any?>): ActionResult {
        val id = map["id"]?.toString()?.trim().orEmpty()
        if (id.isBlank()) return ActionResult.failure(GlobalApp.getString(com.hive.i8n.R.string.agent_skill_delete_missing_id))
        SkillToolLogger.d("delete id=$id")

        val exists = SkillPersistence.loadCustomSkills().any { it.id == id }
        if (!exists) return ActionResult.failure(GlobalApp.getString(com.hive.i8n.R.string.agent_skill_delete_not_found, id))

        SkillPersistence.removeSkill(id)
        val agentProvider =
            ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider
        agentProvider?.unregisterSkillSpec(id)

        return ActionResult.success(data = mapOf("id" to id, "deleted" to true))
    }

    private fun persistAndRegister(spec: SkillSpec) {
        SkillPersistence.addOrUpdateSkill(spec)
        val agentProvider =
            ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider
        agentProvider?.registerSkillSpec(spec)
    }

    private fun parseStringList(element: JsonElement?): List<String> {
        if (element == null || !element.isJsonArray) return emptyList()
        val arr = element.asJsonArray
        return arr.mapNotNull { it.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotBlank() } }
    }

    private fun loadHelpText(): String {
        val baseHelp = runCatching {
            GlobalApp.getContext().resources.openRawResource(R.raw.skill_help)
                .bufferedReader().use { it.readText() }
        }.getOrElse { "" }

        val allSkills = XAgent.getInstance().listSkills()
        val skills = allSkills.filter { it.id in ScriptVisibilityRegistry.getPublicSkillIds() }
        val skillListSection = buildSkillListSection(skills)
        return if (skillListSection.isNotBlank()) {
            "$baseHelp\n\n$skillListSection"
        } else {
            baseHelp
        }
    }

    private fun buildSkillListSection(skills: List<SkillSpec>): String {
        if (skills.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("## Available Skills (current)\n\n")
        sb.append("| id | name | description | allowedToolNames |\n")
        sb.append("|----|------|-------------|------------------|\n")
        for (spec in skills) {
            val id = spec.id.replace("|", " ")
            val name = spec.name.replace("|", " ").replace("\n", " ")
            val desc = spec.description.replace("|", " ").replace("\n", " ")
            val tools = spec.allowedToolNames.joinToString(", ")
            sb.append("| $id | $name | $desc | $tools |\n")
        }
        sb.append("\nUse `buildin.skill(action=run, id=\"<id>\", userPrompt=\"...\")` directly. Same list via `buildin.skill(action=list)`.")
        return sb.toString()
    }
}
