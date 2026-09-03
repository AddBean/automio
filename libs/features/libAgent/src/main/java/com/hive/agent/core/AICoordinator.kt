// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.core

import android.content.Context
import com.hive.agent.skill.SkillToolLogger
import com.hive.utils.GlobalApp
import com.hive.agent.config.AIAgentConfig
import com.hive.agent.mcp.McpToolClient
import com.hive.agent.tools.AIAssistantToolClient
import com.hive.agent.utils.AgentMessageUtils
import com.hive.agent.utils.MessageSummaryProcessor
import com.hive.script.scope.ScriptVisibilityRegistry
import com.hive.plugin.agent.AIServiceProvider
import com.hive.plugin.agent.AgentToolClient
import com.hive.plugin.agent.ErrorContext
import com.hive.plugin.agent.IAICoordinator
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.agent.ModelInfo
import com.hive.agent.ai.ReasoningPrivacy
import com.hive.agent.ai.ReasoningRequestFactory
import com.hive.agent.ai.ReasoningRunContext
import com.hive.agent.ai.ReasoningRunContexts
import com.hive.agent.ai.StreamingAssistantSession
import com.hive.plugin.agent.model.AIRequest
import com.hive.plugin.agent.model.AIRequestType
import com.hive.plugin.agent.model.AIResult
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentErrorCode
import com.hive.plugin.agent.model.AgentInput
import com.hive.plugin.agent.model.AgentRequest
import com.hive.plugin.agent.model.AgentResult
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.MessageStatus
import com.hive.plugin.agent.model.TaskResult
import com.hive.plugin.agent.model.ToolCall
import com.hive.plugin.agent.model.ToolDefinition
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.hive.plugin.agent.ExecutionContextFrame
import com.hive.plugin.agent.ExecutionContextType
import com.hive.plugin.agent.ExecutionContexts
import com.hive.utils.debug.DLog
import com.hive.utils.extends.string
import com.hive.utils.thread.UIHandlerUtils
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.delay

/**
 * AI 协调器实现类
 * 负责驱动 AI 进行工具选择、工作流规划和执行
 * 支持任务暂停、停止、恢复功能
 * 支持流式和非流式推理
 */
class AICoordinator(
    override val agentContext: AgentContext
) : IAICoordinator {

    private val context: Context = GlobalApp.getContext()

    companion object {
        const val TAG = "AICoordinatorImpl"
        private const val TOOL_TRACE_TAG = "AgentToolTrace"
        private const val MAX_TOOL_CALL_ROUNDS = 200 // 限制工具调用轮次，避免无限循环
    }

    override suspend fun coordinateTask(goal: AgentTaskGoal, useStream: Boolean): TaskResult {
        val frame = ExecutionContextFrame(
            type = ExecutionContextType.AGENT,
            id = goal.id,
            name = goal.userInputOptimized.ifEmpty { goal.id },
            rootTaskId = goal.id
        )
        ExecutionContexts.stack.push(frame)
        val reasoningRunContext = ReasoningRunContext.snapshotNow()
        ReasoningRunContexts.bind(goal.id, reasoningRunContext)
        var result: TaskResult? = null
        try {
            agentContext.taskStateManager.notifyAgentExecuteStart(goal.id)
            result = coordinateTaskInternal(goal, useStream, reasoningRunContext)
            agentContext.taskStateManager.notifyAgentExecuteEnd(goal.id, result)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return TaskResult.failure(
                taskId = goal.id,
                error = AgentError.create(
                    code = AgentErrorCode.EXECUTION_FAILED,
                    message = e.message
                )
            )
        } finally {
            // 兜底：异常时也通知结束，避免 UI/状态悬挂
            if (result == null) {
                try {
                    agentContext.taskStateManager.notifyAgentExecuteEnd(
                        goal.id,
                        TaskResult.failure(
                            taskId = goal.id,
                            error = AgentError.create(
                                AgentErrorCode.EXECUTION_FAILED,
                                context.getString(com.hive.i8n.R.string.agent_execution_failed)
                            ),
                            startTime = System.currentTimeMillis()
                        )
                    )
                } catch (_: Throwable) {
                }
            }
            ReasoningRunContexts.unbind(goal.id)
            ExecutionContexts.stack.pop(expectedId = goal.id)
        }
    }


    /**
     * 内部协调任务实现，统一处理流式和非流式逻辑
     */
    private suspend fun coordinateTaskInternal(
        agentGoal: AgentTaskGoal,
        useStream: Boolean,
        reasoningRunContext: ReasoningRunContext
    ): TaskResult {
        val streamType =
            if (useStream) context.getString(com.hive.i8n.R.string.ai_coordinator_streaming) else ""
        agentContext.notifyTaskInfoUpdated(
            context.getString(
                com.hive.i8n.R.string.ai_coordinator_start,
                streamType,
                agentGoal.userInputOptimized
            )
        )
        var currentToolCallRound = 0
        val startTime = System.currentTimeMillis()
        val mainMessages = mutableListOf<ChatMessage>()

        if (agentGoal.input is AgentInput) {
            // 创建输入消息的副本，避免并发修改
            val inputMessages = agentGoal.input?.messages ?: mutableListOf()
            mainMessages.addAll(inputMessages.map { it.copy() })
        }

        if (mainMessages.find { it.role == MessageRole.SYSTEM } == null) {
            mainMessages.add(
                0, ChatMessage(
                    MessageRole.SYSTEM, AIAgentConfig.PromptDefaults.getAutoSystemPrompt()
                )
            )
        }
        if (mainMessages.find { it.role == MessageRole.USER } == null) {
            mainMessages.add(ChatMessage(MessageRole.USER, agentGoal.userInputOptimized))
        }

        val agentInput = AgentInput(mainMessages)

        try {
            while (currentToolCallRound < MAX_TOOL_CALL_ROUNDS) {
                try {
                    checkTaskState(agentGoal.id)

                    // 工具定义收集
                    val globalToolIds = agentContext.agentManager.getGlobalToolIds()
                    val availableTools = agentContext.agentManager.getRegisteredTools()
                        .filter { it.id in globalToolIds }
                    val rawToolDefinitions = availableTools.flatMap { it.toToolDefinitions() }
                    val toolDefinitions = filterScriptToolsByVisibility(rawToolDefinitions)

                    // AI 推理
                    val inferenceType =
                        if (useStream) context.getString(com.hive.i8n.R.string.ai_coordinator_streaming) else ""
                    agentContext.notifyTaskInfoUpdated(
                        context.getString(
                            com.hive.i8n.R.string.ai_coordinator_thinking,
                            inferenceType
                        )
                    )

                    // 选择 AI Provider 和模型
                    val (selectedProvider, selectedModel) = selectAIProviderAndModel(agentInput)
                        ?: return TaskResult.failure(
                            taskId = agentGoal.id,
                            error = AgentError.create(
                                AgentErrorCode.AI_SERVICE_UNAVAILABLE,
                                context.getString(com.hive.i8n.R.string.ai_coordinator_no_ai_model)
                            ),
                            startTime = startTime
                        )

                    val visionPipelineActive = isVisionPipelineActive()
                    agentInput.messages = AgentMessageUtils.processSystemMessage(
                        agentInput.messages,
                        selectedModel,
                        visionActive = visionPipelineActive
                    )

                    val resolvedReasoning = reasoningRunContext.resolve(
                        selectedModel.providerId,
                        selectedModel.modelId,
                        ReasoningRequestFactory.dynamicMetadataFrom(selectedModel)
                    )
                    DLog.d(
                        TAG,
                        ReasoningPrivacy.safeMetaLog(
                            selectedModel.providerId,
                            selectedModel.modelId,
                            resolvedReasoning
                        )
                    )
                    val aiRequest = AIRequest(
                        model = selectedModel.modelId,
                        requestType = AIRequestType.FUNCTION_CALL,
                        input = AgentInput(
                            AgentMessageUtils.processAndCopyMessages(
                                agentGoal.id,
                                agentInput.messages,
                                onMemoryCompressing = { isCompressing ->
                                    UIHandlerUtils.getInstance().executeInMainThread {
                                        agentContext.notifyMemoryCompressing(
                                            agentGoal.id,
                                            isCompressing
                                        )
                                    }
                                },
                                allowImageAttachments = visionPipelineActive
                            )
                        ),
                        inputOrigin = agentInput,
                        tools = toolDefinitions,
                        reasoning = resolvedReasoning.effectiveOptions,
                    )
                    agentGoal.updateNormal(agentContext, agentInput)
                    val streaming = StreamingAssistantSession.start(
                        messages = mainMessages,
                        normalNotify = { agentGoal.updateNormal(agentContext, agentInput) },
                        streamNotify = { agentGoal.updateStream(agentContext, agentInput) },
                        throttleMs = 0L
                    )
                    val replyMessage = streaming.message()
                    var aiResponse: ChatCompletionResponse? = null
                    try {
                        // 根据是否流式选择不同的推理方式
                        val aiResult = if (useStream) {
                            selectedProvider.streamInference<ChatCompletionResponse>(
                                aiRequest, onChunkResponse = { chunk ->
                                    // 实时更新流式消息
                                    streaming.onChunk(chunk)
                                })

                        } else {
                            selectedProvider.inference(
                                aiRequest
                            )
                        }
                        aiResponse = when (aiResult) {
                            is AIResult.Success -> aiResult.data
                            is AIResult.Failure -> {
                                checkTaskState(agentGoal.id)
                                DLog.e(TAG, "AI 推理失败: ${aiResult.error.getInfo()}")

                                // NEW: Notify observers with error details
                                agentContext.notifyTaskError(
                                    agentGoal.id,
                                    aiResult.error,
                                    ErrorContext.MODEL_SELECTION
                                )

                                if (aiResult.error.code == AgentErrorCode.AI_REQUEST_CANCEL) {
                                    return TaskResult.failure(
                                        taskId = agentGoal.id,
                                        error = aiResult.error,
                                        startTime = startTime
                                    )
                                } else {
                                    return TaskResult.failure(
                                        taskId = agentGoal.id,
                                        error = AgentError.create(
                                            AgentErrorCode.AI_INVALID_REQUEST,
                                            context.getString(
                                                com.hive.i8n.R.string.ai_coordinator_inference_failed,
                                                (aiResult.error.e as Exception)?.message
                                            ),
                                            aiErrorDetail = aiResult.error.aiErrorDetail  // Preserve detail
                                        ),
                                        startTime = startTime,
                                    )
                                }

                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        checkTaskState(agentGoal.id)
                    }
                    aiResponse ?: return TaskResult.failure(
                        taskId = agentGoal.id,
                        error = AgentError.create(
                            AgentErrorCode.AI_REQUEST_ERROR,
                            context.getString(com.hive.i8n.R.string.ai_coordinator_request_failed)
                        ),
                        startTime = startTime,
                    )
                    val toolCallSize = aiResponse.toolCalls?.size ?: 0
                    val hasMessage =
                        !aiResponse.content.isNullOrEmpty() || !aiResponse.reasoningContent.isNullOrEmpty()
                    if (toolCallSize == 0 && !hasMessage) {
                        DLog.w(TAG, "AI 返回空响应或不明确的响应")
                        return TaskResult.failure(
                            taskId = agentGoal.id,
                            error = AgentError.create(
                                AgentErrorCode.AI_REQUEST_ERROR,
                                context.getString(com.hive.i8n.R.string.ai_coordinator_empty_response)
                            ),
                            startTime = startTime,
                        )
                    }

                    // 状态摘要 / 工具 reason 仅用最终 content，不含 reasoning 正文
                    val messageContent = if (hasMessage) {
                        ReasoningPrivacy.publicAssistantText(
                            content = aiResponse.content,
                            reasoningContent = aiResponse.reasoningContent,
                            emptyFallback = ""
                        )
                    } else {
                        ""
                    }

                    // 设置回复消息内容
                    replyMessage.role = MessageRole.ASSISTANT
                    streaming.finalizeWith(aiResponse)

                    val thinkingStatus = if (useStream) "AI 思考完成" else "AI 思考中"
                    agentContext.notifyTaskInfoUpdated("$thinkingStatus: $messageContent")
                    agentGoal.updateNormal(agentContext, agentInput)
                    checkTaskState(agentGoal.id)

                    if (toolCallSize > 0) {
                        agentContext.notifyTaskInfoUpdated(
                            context.getString(
                                com.hive.i8n.R.string.ai_coordinator_tool_calling,
                                aiResponse.toolCalls?.firstOrNull()?.function?.name ?: ""
                            )
                        )
                        for (toolCall in aiResponse.toolCalls!!) {
                            checkTaskState(agentGoal.id)
                            val toolName = toolCall.function.name
                            SkillToolLogger.d(
                                "toolCall toolName=$toolName reason=${ReasoningPrivacy.toolReasonLog(messageContent)}"
                            )
                            val toolMessage = ChatMessage(
                                role = MessageRole.TOOL,
                                content = "",
                                toolCallId = toolCall.id,
                                toolCalls = listOf(toolCall),
                                toolCallResult = ""
                            )
                            mainMessages.add(toolMessage)
                            agentGoal.updateNormal(agentContext, agentInput)
                            val toolResult = executeToolCall(toolCall)
                            val execAt = AIAgentConfig.PromptDefaults.formatExecAt()
                            val resultFormatAi = if (toolResult.success) {
                                val dataStr = when (val d = toolResult.data) {
                                    is String -> d
                                    null -> null
                                    else -> GsonHelper.getInstance().toJson(d)
                                }
                                AIAgentConfig.PromptDefaults.getToolResult(
                                    dataStr, toolResult.extra, execAt
                                )
                            } else {
                                AIAgentConfig.PromptDefaults.withExecAt(
                                    "执行失败: ${toolResult.error?.getInfo()}",
                                    execAt
                                )
                            }
                            toolMessage.content = resultFormatAi
                            toolMessage.toolCallResult = resultFormatAi
                            toolMessage.toolCallResultSuccess = toolResult.success
                            toolMessage.execAt = execAt
                            toolMessage.status = MessageStatus.FINISH
                            toolMessage.attachments.addAll(toolResult.files ?: mutableListOf())
                            agentContext.notifyTaskInfoUpdated("执行结果: $toolResult")
                            agentGoal.updateNormal(agentContext, agentInput)
                            //如果存在多个工具，则需要延时执行多个命令
                            if (toolCallSize > 1) {
                                delay(2000)
                            }
                        }
                        currentToolCallRound++
                    } else {
                        delay(1000)
                        agentContext.notifyTaskInfoUpdated(context.getString(com.hive.i8n.R.string.ai_coordinator_workflow_completed))
                        agentGoal.updateNormal(agentContext, agentInput)
                        return TaskResult.success(
                            taskId = agentGoal.id,
                            data = messageContent,
                            startTime = startTime,
                        )
                    }

                } catch (e: AgentError) {
                    when (e.code) {
                        AgentErrorCode.TASK_STATE_PAUSE -> {
                            currentToolCallRound--
                            agentContext.taskStateManager.checkPausedAndWait(agentGoal.id)
                        }

                        else -> throw e
                    }
                } finally {
                    mainMessages.forEach {
                        it.status = MessageStatus.FINISH
                    }
                    agentGoal.updateNormal(agentContext, agentInput)
                }
            }
            DLog.e(TAG, "达到最大工具调用轮次限制，工作流可能未完成。")
            return TaskResult.failure(
                taskId = agentGoal.id,
                error = AgentError.create(
                    AgentErrorCode.TASK_STATE_ERROR,
                    com.hive.i8n.R.string.ai_coordinator_max_tool_calls.string(
                        MAX_TOOL_CALL_ROUNDS
                    )
                ),
                startTime = startTime,
            )
        } catch (e: AgentError) {
            DLog.e(TAG, "任务已停止: ${e.getInfo()}")
            return TaskResult.failure(
                taskId = agentGoal.id,
                error = e,
                startTime = startTime,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            DLog.e(TAG, "工作流协调过程中发生错误: ${e.message}")
            return TaskResult.failure(
                taskId = agentGoal.id,
                error = AgentError.create(
                    AgentErrorCode.UNKNOWN_ERROR,
                    com.hive.i8n.R.string.ai_coordinator_unknown_error.string(e.message),
                    cause = e
                ),
                startTime = startTime,
            )
        } finally {
            // 清理任务状态
            agentContext.taskStateManager.clearTask(agentGoal.id)
            // 清理任务摘要缓存
            MessageSummaryProcessor.clearTaskSummary(agentGoal.id)
        }
    }

    private fun checkTaskState(taskId: String) {
        // 检查任务是否被停止
        if (agentContext.taskStateManager.checkPaused(taskId)) {
            throw AgentError(
                code = AgentErrorCode.TASK_STATE_PAUSE,
                context.getString(com.hive.i8n.R.string.ai_coordinator_task_paused)
            )
        }
        // 检查任务是否被停止
        if (agentContext.taskStateManager.checkStopped(taskId)) {
            throw AgentError(
                code = AgentErrorCode.TASK_STATE_STOP,
                context.getString(com.hive.i8n.R.string.ai_coordinator_task_stopped)
            )
        }
    }

    /**
     * 选择 AI Provider 和模型
     * 根据输入内容智能选择合适的 AI Provider 和模型
     */
    private fun selectAIProviderAndModel(agentInput: AgentInput): Pair<AIServiceProvider, ModelInfo>? {
        // 获取 AI Service Manager
        val aiServiceManager = agentContext.aiServiceProvider

        // 检查是否包含图片（多模态请求）；视觉链路未激活时强制走对话模型
        val hasImages = checkIfHasImages(agentInput)
        val visionPipelineActive = isVisionPipelineActive()

        val normalModel = aiServiceManager.getInferenceModel(InferenceType.TEXT)

        val multimodalModel = aiServiceManager.getInferenceModel(InferenceType.IMAGE)

        var selectedModel =
            if (hasImages && visionPipelineActive) multimodalModel else normalModel
        //兜底normalModel
        if (selectedModel == null) {
            selectedModel = normalModel
        }
        selectedModel ?: return null
        val selectedProvider = aiServiceManager.getProvider(selectedModel.providerId)

        selectedProvider ?: return null

        DLog.d(
            TAG,
            "选择 AI Provider: ${selectedProvider.javaClass.simpleName}, 模型: ${selectedProvider.getProviderInfo()}, 多模态: ${hasImages && visionPipelineActive}"
        )

        return Pair(selectedProvider, selectedModel)
    }

    private fun isVisionPipelineActive(): Boolean {
        val hasVisionModel =
            agentContext.aiServiceProvider.getInferenceModel(InferenceType.IMAGE) != null
        return AIAgentConfig.VisionConfig.isVisionPipelineActive(hasVisionModel)
    }

    /**
     * 检查输入是否包含图片
     * 这里可以根据实际需要实现图片检测逻辑
     */
    private fun checkIfHasImages(agentInput: AgentInput): Boolean {
        return agentInput.messages.find { it.attachments.isNotEmpty() } != null
    }

    /**
     * 执行 AI 返回的工具调用
     * 优化版本：智能处理不同类型的工具调用
     */
    private suspend fun executeToolCall(toolCall: ToolCall): AgentResult<*> {
        val functionName = toolCall.function.name
        val arguments = toolCall.function.arguments // 这是一个 JsonObject

        agentContext.notifyTaskInfoUpdated("准备执行工具调用: $functionName")
        val tools = findToolByMethod(functionName)
        return when (tools) {
            is AIAssistantToolClient -> {
                agentContext.notifyTaskInfoUpdated("执行 AI 助手工具: $functionName")
                executeAIAssistant(tools, functionName, arguments, toolCall.id)
            }

            is McpToolClient -> {
                agentContext.notifyTaskInfoUpdated("执行工具: $functionName arguments: $arguments")
                executeMcpAssistant(tools, functionName, arguments, toolCall.id)
            }

            else -> {
                agentContext.notifyTaskInfoUpdated("执行工具: $functionName arguments: $arguments")
                executeCommonTool(tools, functionName, arguments, toolCall.id)
            }
        }
    }

    private fun findToolByMethod(functionName: String): AgentToolClient? {
        val globalToolIds = agentContext.agentManager.getGlobalToolIds()
        return agentContext.agentManager.getRegisteredTools()
            .filter { it.id in globalToolIds }
            .find { it.supportedMethods.contains(functionName) }
    }

    /**
     * 执行 AI 助手操作
     */
    private suspend fun executeAIAssistant(
        aiTool: AgentToolClient, functionName: String, arguments: JsonObject, taskId: String
    ): AgentResult<*> {
        val p = GsonHelper.getInstance()
            .fromJson(arguments.toString(), AIAssistantCallParams::class.java)
        val content = p.content ?: ""
        val parameters = p.parameters?.let {
            GsonHelper.getInstance().fromJson<Map<String, Any>>(
                it.toString(),
                object : TypeToken<Map<String, Any>>() {}.type
            )
        } ?: emptyMap<String, Any>()

        val agentRequest = AgentRequest(
            toolId = aiTool.id, action = functionName, params = mapOf(
                "content" to content, "parameters" to parameters
            ), taskId = taskId
        )

        return agentContext.agentManager.dispatchRequest<Any>(agentRequest)
    }

    /**
     * 执行 Mcp 助手操作
     */
    private suspend fun executeMcpAssistant(
        tools: AgentToolClient, functionName: String, arguments: JsonObject, taskId: String
    ): AgentResult<*> {
        val p = GsonHelper.getInstance()
            .fromJson(arguments.toString(), McpAssistantCallParams::class.java)
        val toolArguments = p.arguments ?: p.params ?: arguments
        val argumentMap = GsonHelper.getInstance().fromJson<Map<String, Any>>(
            toolArguments.toString(),
            object : TypeToken<Map<String, Any>>() {}.type
        )

        val rawName = if (tools is McpToolClient) tools.resolveRawToolName(functionName) else functionName
        val agentRequest = when (rawName) {
            // MCP 协议方法桥接：让模型把它们当作“工具”调用
            "tools_list" -> AgentRequest(
                toolId = tools.id,
                action = "tools_list",
                params = argumentMap,
                taskId = taskId
            )

            "resources_list" -> AgentRequest(
                toolId = tools.id,
                action = "resources_list",
                params = argumentMap,
                taskId = taskId
            )

            else -> AgentRequest(
                toolId = tools.id,
                action = "tools_call",
                params = mapOf("name" to rawName, "arguments" to argumentMap),
                taskId = taskId
            )
        }

        return agentContext.agentManager.dispatchRequest<Any>(agentRequest)
    }

    private suspend fun executeCommonTool(
        tool: AgentToolClient?,
        functionName: String,
        arguments: JsonObject,
        taskId: String
    ): AgentResult<*> {
        if (tool != null) {
            val argumentMap = GsonHelper.getInstance().fromJson<Map<String, Any>>(
                arguments.toString(),
                object : TypeToken<Map<String, Any>>() {}.type
            ) ?: emptyMap()
            val request = AgentRequest(
                toolId = tool.id,
                action = functionName,
                params = argumentMap,
                taskId = taskId
            )
            return agentContext.agentManager.dispatchRequest<Any>(request)
        } else {
            return AgentResult.Failure(AgentError.create(AgentErrorCode.TOOL_NOT_FOUND))
        }
    }

    private data class AIAssistantCallParams(
        val content: String? = null,
        val parameters: JsonObject? = null
    )

    private data class McpAssistantCallParams(
        val arguments: JsonObject? = null,
        val params: JsonObject? = null
    )

    /**
     * 主 Agent 仅使用 public 的 custom 工具。
     * 对 function.name 以 custom. 开头的，仅保留 ScriptVisibilityRegistry 中标记为 public 的项；
     * 非 custom 工具（如 buildin.dialog、buildin.skill）不做过滤。
     */
    private fun filterScriptToolsByVisibility(definitions: List<ToolDefinition>): List<ToolDefinition> {
        val publicToolIds = ScriptVisibilityRegistry.getPublicToolIds()
        return definitions.filter { def ->
            val name = def.function.name
            if (name.startsWith(com.hive.plugin.mcp.McpConst.Tool_Name_Prefix_Custom)) name in publicToolIds else true
        }
    }
}
