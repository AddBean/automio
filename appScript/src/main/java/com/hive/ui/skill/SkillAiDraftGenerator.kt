// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.skill

import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.AIServiceManager
import com.hive.plugin.agent.AIServiceProvider
import com.hive.plugin.agent.AgentToolClient
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.agent.model.AIRequest
import com.hive.plugin.agent.model.AIRequestType
import com.hive.plugin.agent.model.AIResult
import com.hive.plugin.agent.model.AgentInput
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.ToolDefinition
import com.hive.plugin.provider.IAgentProvider
import com.hive.plugin.provider.IMcpProvider
import com.hive.script.scope.ScriptScopeRepository
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.utils.GsonHelper
import java.io.File

object SkillAiDraftGenerator {

    private const val TAG = "SkillAiDraftGenerator"

    data class ToolOption(
        val functionName: String,
        val displayName: String,
        val description: String
    )

    data class Result(
        val draft: SkillDraft?,
        val errorMessage: String? = null
    )

    suspend fun generate(requirement: String, scopeScriptPath: String? = null): Result {
        val manager = getAIServiceManager()
            ?: return Result(
                draft = null,
                errorMessage = GlobalApp.getString(com.hive.i8n.R.string.error_ai_no_provider)
            )

        val provider = getTargetProvider(manager)
            ?: return Result(
                draft = null,
                errorMessage = GlobalApp.getString(com.hive.i8n.R.string.error_ai_no_provider)
            )

        val model = resolveModel(manager, provider)
            ?: return Result(
                draft = null,
                errorMessage = GlobalApp.getString(com.hive.i8n.R.string.error_ai_no_provider)
            )

        val toolOptions = loadToolOptions(scopeScriptPath)
        val prompt = buildPrompt(requirement.trim(), toolOptions)
        val request = AIRequest(
            model = model,
            requestType = AIRequestType.CHAT_COMPLETION,
            input = AgentInput(
                listOf(
                    ChatMessage(
                        role = MessageRole.USER,
                        content = prompt
                    )
                )
            ),
            inputOrigin = AgentInput(
                listOf(
                    ChatMessage(
                        role = MessageRole.USER,
                        content = prompt
                    )
                )
            )
        )

        return try {
            when (val result = provider.inference<ChatCompletionResponse>(request)) {
                is AIResult.Success -> {
                    val raw = result.data.content.orEmpty()
                    val draft = parseDraft(raw, toolOptions)
                    if (draft == null) {
                        Result(
                            draft = null,
                            errorMessage = GlobalApp.getString(com.hive.i8n.R.string.skill_ai_create_parse_failed)
                        )
                    } else {
                        Result(draft = draft)
                    }
                }

                is AIResult.Failure -> {
                    Result(draft = null, errorMessage = result.error.getInfo())
                }
            }
        } catch (e: Exception) {
            DLog.e(TAG, "generate failed: ${e.message}")
            Result(draft = null, errorMessage = e.message ?: e.toString())
        }
    }

    fun loadToolOptions(scopeScriptPath: String? = null): List<ToolOption> {
        val agentProvider =
            ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider
        val mcpProvider =
            ComponentManager.getInstance().getProvider(IMcpProvider::class.java) as? IMcpProvider

        val tools: List<AgentToolClient> = agentProvider?.getRegisteredTools().orEmpty()
        val definitions: List<ToolDefinition> = tools
            .flatMap { it.toToolDefinitions() }
            .distinctBy { it.function.name }

        val filtered = definitions.filter { def ->
            val name = def.function.name
            !name.endsWith(".tools_list") && !name.endsWith(".resources_list")
        }

        val displayNameMap = linkedMapOf<String, String>().apply {
            (mcpProvider?.getRegisteredTools()?.toList() ?: emptyList()).forEach { tool ->
                val displayName = tool.extraName.takeIf { it.isNotBlank() } ?: tool.name
                if (tool.name.isNotBlank()) put(tool.name, displayName)
                if (tool.extraName.isNotBlank()) put(tool.extraName, displayName)
            }
        }

        val scopeItems = scopeScriptPath?.let { scriptPath ->
            runCatching {
                ScriptScopeRepository.load(File(scriptPath), validate = false)
            }.getOrNull()?.tools.orEmpty().map { tool ->
                ToolOption(
                    functionName = tool.functionName,
                    displayName = tool.name.ifBlank { tool.functionName },
                    description = tool.description
                )
            }
        }.orEmpty()

        return (filtered.sortedBy { it.function.name }.map { def ->
            ToolOption(
                functionName = def.function.name,
                displayName = displayNameMap[def.function.name] ?: def.function.name,
                description = def.function.description ?: ""
            )
        } + scopeItems).distinctBy { it.functionName }
    }

    private fun getAIServiceManager(): AIServiceManager? {
        val provider = ComponentManager.getInstance()
            .getProvider(IAgentProvider::class.java) as? IAgentProvider
        return provider?.getAIServiceManager()
    }

    private fun getTargetProvider(manager: AIServiceManager): AIServiceProvider? {
        val modelInfo = manager.getInferenceModel(InferenceType.TEXT)
        val targetProvider = if (modelInfo != null) {
            val provider = manager.getProvider(modelInfo.providerId)
            if (provider != null && manager.isProviderEnabled(modelInfo.providerId)) provider else null
        } else {
            null
        }
        return targetProvider ?: manager.getAvailableProvider()
    }

    private fun resolveModel(manager: AIServiceManager, provider: AIServiceProvider): String? {
        val modelInfo = manager.getInferenceModel(InferenceType.TEXT)
        return modelInfo?.modelId?.takeIf { it.isNotEmpty() }
            ?: provider.getProviderInfo().defaultModelId
    }

    private fun buildPrompt(requirement: String, tools: List<ToolOption>): String {
        val toolSection = if (tools.isEmpty()) {
            "No available tools. Set allowedToolNames to an empty array."
        } else {
            tools.joinToString("\n") { tool ->
                "- ${tool.functionName} | ${tool.displayName} | ${tool.description.ifBlank { "No description" }}"
            }
        }
        return """
            You are generating an AI skill draft for a mobile automation app.
            Read the user requirement and return exactly one JSON object.
            Do not output markdown, code fences, or explanation.

            Required JSON fields:
            - name: string
            - description: string
            - systemPrompt: string
            - allowedToolNames: string[]
            - maxRounds: number
            - timeoutMs: number

            Rules:
            - The JSON must be valid and parseable.
            - Only use tool names from the provided list.
            - If no tool is clearly needed, allowedToolNames should be [].
            - maxRounds should default to -1 unless the requirement strongly implies a limit.
            - timeoutMs should default to -1 unless the requirement strongly implies a timeout.
            - name should be concise.
            - description should be one short paragraph.
            - systemPrompt should be directly usable as a skill system prompt.

            Available tools:
            $toolSection

            User requirement:
            $requirement
        """.trimIndent()
    }

    private fun parseDraft(raw: String, tools: List<ToolOption>): SkillDraft? {
        val toolSet = tools.map { it.functionName }.toHashSet()
        val normalized = raw
            .replace("```json", "")
            .replace("```", "")
            .trim()
            .let { content ->
                val start = content.indexOf('{')
                val end = content.lastIndexOf('}')
                if (start >= 0 && end > start) content.substring(start, end + 1) else content
            }

        return runCatching {
            val map = GsonHelper.getInstance().fromJson<Map<String, Any?>>(
                normalized,
                object : com.google.gson.reflect.TypeToken<Map<String, Any?>>() {}.type
            )
            val name = map["name"]?.toString()?.trim().orEmpty()
            val description = map["description"]?.toString()?.trim().orEmpty()
            val systemPrompt = map["systemPrompt"]?.toString()?.trim().orEmpty()
            if (name.isBlank() || description.isBlank() || systemPrompt.isBlank()) return null

            val rawTools = map["allowedToolNames"] as? List<*>
            val allowedToolNames = rawTools.orEmpty()
                .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
                .filter { it in toolSet }
                .distinct()

            val maxRounds = (map["maxRounds"] as? Number)?.toInt() ?: -1
            val timeoutMs = (map["timeoutMs"] as? Number)?.toLong() ?: -1L

            SkillDraft(
                name = name,
                description = description,
                systemPrompt = systemPrompt,
                allowedToolNames = allowedToolNames,
                maxRounds = maxRounds,
                timeoutMs = timeoutMs
            )
        }.getOrNull()
    }
}
