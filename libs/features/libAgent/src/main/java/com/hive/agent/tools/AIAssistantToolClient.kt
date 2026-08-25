// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.tools

import com.google.gson.JsonParser
import com.hive.agent.config.AIAgentConfig
import com.hive.agent.model.AIAssistantParametersSchema
import com.hive.plugin.agent.AgentToolClient
import com.hive.plugin.agent.model.AgentRequest
import com.hive.plugin.agent.model.AgentResult
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.FunctionDefinition
import com.hive.plugin.agent.model.ToolDefinition
import com.hive.utils.GlobalApp
import com.hive.utils.utils.GsonHelper

/**
 * AI助手工具
 * 提供AI对话、文本生成等功能的Agent工具实现
 * 注意：此工具现在主要作为AI可调用的功能描述，实际AI交互由AICoordinator处理
 */
class AIAssistantToolClient : AgentToolClient {

    override val id = AIAgentConfig.BaseConfig.AIAssistantToolName
    override val name = GlobalApp.getString(com.hive.i8n.R.string.agent_ai_assistant)
    override val description = GlobalApp.getString(com.hive.i8n.R.string.agent_ai_assistant_description)
    override val supportedMethods = mutableListOf(
        "chat",           // 对话
        "generate_text",  // 文本生成
        "translate",      // 翻译
        "summarize",      // 摘要
        "analyze"         // 分析
    )

    override suspend fun execute(request: AgentRequest): AgentResult<*> {
        // 在新架构中，AI助手的功能主要通过AICoordinator处理
        // 这里提供基本的响应以保持兼容性
        return AgentResult.Success(
            when (request.action) {
                "chat" -> handleBasicChat(request.params as Map<String, Any>)
                "generate_text" -> handleBasicGenerate(request.params as Map<String, Any>)
                "translate" -> handleBasicTranslate(request.params as Map<String, Any>)
                "summarize" -> handleBasicSummarize(request.params as Map<String, Any>)
                "analyze" -> handleBasicAnalyze(request.params as Map<String, Any>)
                else -> throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.agent_unsupported_ai_operation, request.action))
            }
        )
    }

    override fun stopExecute() {

    }

    /**
     * 基本对话处理（回退实现）
     */
    private fun handleBasicChat(params: Map<String, Any>): ChatCompletionResponse {
        val messages = params["messages"] as? List<Map<String, String>>
            ?: throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.agent_messages_param_required))

        val lastMessage = messages.lastOrNull()?.get("content") ?: ""

        val response = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_response, lastMessage)


        return ChatCompletionResponse(
            content = response,
            model = params["model"] as? String 
        )
    }

    /**
     * 基本文本生成处理（回退实现）
     */
    private fun handleBasicGenerate(params: Map<String, Any>): ChatCompletionResponse {
        val prompt = params["prompt"] as? String
            ?: throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.agent_prompt_param_required))

        val response = GlobalApp.getString(com.hive.i8n.R.string.agent_generate_response, prompt)

        return ChatCompletionResponse(
            content = response,
            model = params["model"] as? String 
        )
    }

    /**
     * 基本翻译处理（回退实现）
     */
    private fun handleBasicTranslate(params: Map<String, Any>): ChatCompletionResponse {
        val text = params["text"] as? String
            ?: throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.agent_text_param_required))
        val targetLanguage = params["targetLanguage"] as? String ?: GlobalApp.getString(com.hive.i8n.R.string.agent_chinese)

        val response = GlobalApp.getString(com.hive.i8n.R.string.agent_translate_response, text, targetLanguage)

        return ChatCompletionResponse(
            content = response,
            model = params["model"] as? String 
        )
    }

    /**
     * 基本摘要处理（回退实现）
     */
    private fun handleBasicSummarize(params: Map<String, Any>): ChatCompletionResponse {
        val text = params["text"] as? String
            ?: throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.agent_text_param_required))

        val response = GlobalApp.getString(com.hive.i8n.R.string.agent_summarize_response, text.length)

        return ChatCompletionResponse(
            content = response,
            model = params["model"] as? String 
        )
    }

    /**
     * 基本分析处理（回退实现）
     */
    private fun handleBasicAnalyze(params: Map<String, Any>): ChatCompletionResponse {
        val text = params["text"] as? String
            ?: throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.agent_text_param_required))
        val analysisType = params["type"] as? String ?: "general"

        val response = GlobalApp.getString(com.hive.i8n.R.string.agent_analyze_response, analysisType, text.length)

        return ChatCompletionResponse(
            content = response,
            model = params["model"] as? String 
        )
    }

    override fun onDestroy() {
        // AI助手工具无需特殊清理
    }

    override fun toToolDefinitions(): List<ToolDefinition> {
        val parametersSchema = JsonParser().parse(
            GsonHelper.getInstance().toJson(
                AIAssistantParametersSchema(
                    actionDescription = GlobalApp.getString(com.hive.i8n.R.string.agent_ai_operation_type),
                    contentDescription = GlobalApp.getString(com.hive.i8n.R.string.agent_text_content_or_messages),
                    parametersDescription = GlobalApp.getString(com.hive.i8n.R.string.agent_extra_parameters),
                    targetLanguageDescription = GlobalApp.getString(com.hive.i8n.R.string.agent_target_language),
                    analysisTypeDescription = GlobalApp.getString(com.hive.i8n.R.string.agent_analysis_type),
                    maxLengthDescription = GlobalApp.getString(com.hive.i8n.R.string.agent_max_content_length)
                ).toJsonSchemaObject()
            )
        ).asJsonObject

        return listOf(
            ToolDefinition(
                type = "function",
                function = FunctionDefinition(
                    name = AIAgentConfig.BaseConfig.AIAssistantToolName,
                    description = this.description,
                    parameters = parametersSchema
                )
            )
        )
    }
}
