// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.skill

import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.hive.agent.utils.AgentMessageUtils
import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IAgentProvider
import com.hive.agent.core.AgentContext
import com.hive.agent.mcp.McpToolClient
import com.hive.agent.config.AIAgentConfig
import com.hive.agent.ai.StreamingAssistantSession
import com.hive.plugin.agent.model.RunSkillRequest
import com.hive.plugin.agent.model.SkillError
import com.hive.plugin.agent.model.SkillResult
import com.hive.plugin.agent.model.SkillToolError
import com.hive.plugin.agent.AgentToolClient
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.model.AIRequest
import com.hive.plugin.agent.model.AIRequestType
import com.hive.plugin.agent.model.AIResult
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentErrorCode
import com.hive.plugin.agent.model.AgentInput
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.AgentRequest
import com.hive.plugin.agent.model.AgentResult
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.MessageStatus
import com.hive.plugin.agent.model.ToolCall
import com.hive.plugin.agent.model.ToolDefinition
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.ExecutionContextFrame
import com.hive.plugin.agent.ExecutionContextType
import com.hive.plugin.agent.ExecutionContexts
import com.hive.plugin.agent.currentScopeId
import com.hive.agent.utils.MessageSummaryProcessor
import com.hive.script.scope.PackageRuntimeResolver
import com.hive.script.record.AgentCommandRecorder
import com.hive.plugin.mcp.model.McpTool as McpToolModel
import com.hive.utils.thread.UIHandlerUtils
import com.hive.utils.utils.GsonHelper
import android.os.Handler
import android.os.Looper
import com.hive.views.widgets.CommonToast

class SkillRunner(
    private val agentContext: AgentContext,
    private val skillRegistry: SkillRegistry,
    private val messageProcessor: suspend (String, List<ChatMessage>, ModelInfo) -> List<ChatMessage> =
        { taskId, messages, _ ->
            val hasVisionModel =
                agentContext.aiServiceProvider.getInferenceModel(InferenceType.IMAGE) != null
            val visionActive =
                AIAgentConfig.VisionConfig.isVisionPipelineActive(hasVisionModel)
            AgentMessageUtils.processAndCopyMessages(
                taskId,
                messages,
                onMemoryCompressing = { isCompressing ->
                    UIHandlerUtils.getInstance().executeInMainThread {
                        agentContext.notifyMemoryCompressing(taskId, isCompressing)
                    }
                },
                allowImageAttachments = visionActive
            )
        }
) {
    companion object {
        private const val TAG = "SkillRunner"
        const val SKILL_NOT_FOUND = "SKILL_NOT_FOUND"
        const val SKILL_DEPTH_EXCEEDED = "SKILL_DEPTH_EXCEEDED"
        const val SKILL_TOOL_FORBIDDEN = "SKILL_TOOL_FORBIDDEN"
        const val SKILL_GLOBAL_FALLBACK_FORBIDDEN = "SKILL_GLOBAL_FALLBACK_FORBIDDEN"
        const val SKILL_INFERENCE_FAILED = "SKILL_INFERENCE_FAILED"
        const val SKILL_TOOL_EXECUTION_FAILED = "SKILL_TOOL_EXECUTION_FAILED"
        /** -1 = unlimited */
        private const val UNLIMITED_MS = -1L
        private const val UNLIMITED_ROUNDS = -1
        private const val DEFAULT_TIMEOUT_MS = UNLIMITED_MS
        private const val DEFAULT_MAX_ROUNDS = UNLIMITED_ROUNDS
        private const val DEFAULT_DEPTH = 0
        private const val MAX_DEPTH = 5
        private const val STREAM_THROTTLE_MS = 80L
    }

    suspend fun runSkill(request: RunSkillRequest, taskId: String? = null): SkillResult {
        val scopeId = ExecutionContexts.stack.currentScopeId()
        val scopeRuntimePackage = PackageRuntimeResolver.resolveByScopeId(scopeId)
        val localScopedSkill = scopeRuntimePackage?.resolveLocalSkill(request.skillId)

        val spec = when {
            localScopedSkill != null -> localScopedSkill.toSkillSpec()
            else -> {
                if (scopeRuntimePackage != null && !scopeRuntimePackage.isGlobalSkillAllowed(request.skillId)) {
                    return failure(
                        code = SKILL_GLOBAL_FALLBACK_FORBIDDEN,
                        summary = "Skill 不在 package 白名单中",
                        message = "skillId=${request.skillId} 未在 allowedGlobalSkillIds 中。"
                    )
                }
                skillRegistry.get(null, request.skillId)
                    ?: run {
                        CommonToast.getInstance().showToast("Skill 不存在: ${request.skillId}")
                        return failure(
                            code = SKILL_NOT_FOUND,
                            summary = "Skill 不存在: ${request.skillId}",
                            message = "请先通过 buildin.skill(action=list) 或 buildin.skill(action=help) 获取可用 skillId。"
                        )
                    }
            }
        }

        val toolRuntimePackage = when {
            localScopedSkill != null -> scopeRuntimePackage
            else -> PackageRuntimeResolver.resolveByPrimarySkillId(spec.id)
        }
        val localToolBindings = toolRuntimePackage
            ?.resolveLocalToolBindings(spec.allowedToolNames)
            .orEmpty()

        val options = request.options
        val depth = options?.depth ?: DEFAULT_DEPTH
        if (depth !in 0..MAX_DEPTH) {
            CommonToast.getInstance().showToast("Skill 调用深度超限: ${request.skillId}")
            return failure(
                code = SKILL_DEPTH_EXCEEDED,
                summary = "Skill 调用深度超限",
                message = "当前 depth=$depth，允许范围 0..$MAX_DEPTH。"
            )
        }

        val parentRootTaskIdFromStack: String? = ExecutionContexts.stack.snapshot()
            .asReversed()
            .firstOrNull { it.type == ExecutionContextType.SKILL || it.type == ExecutionContextType.AGENT }
            ?.rootTaskId
        val effectiveRootTaskId: String? = taskId ?: parentRootTaskIdFromStack
        val isStandalone = effectiveRootTaskId == null
        var skillResult: SkillResult? = null
        var taskKeyForEnd: String? = null
        var skillTaskKeyForEnd: String? = null

        return try {
            val effectiveTimeoutMs = options?.timeoutMs ?: spec.timeoutMs ?: DEFAULT_TIMEOUT_MS
            val effectiveMaxRounds = options?.maxRounds ?: spec.maxRounds ?: DEFAULT_MAX_ROUNDS
            val startTime = System.currentTimeMillis()
            val deadline = if (effectiveTimeoutMs >= 0) startTime + effectiveTimeoutMs else Long.MAX_VALUE
            val taskKey = effectiveRootTaskId ?: "skill-${spec.id}-${startTime}"
            val skillTaskKey = "skill-${spec.id}-${startTime}"
            taskKeyForEnd = taskKey
            skillTaskKeyForEnd = skillTaskKey
            val effectiveMemoryGroup = options?.memoryGroup ?: spec.memoryGroup ?: "skill-${spec.id}"
            SkillToolLogger.d("runSkill skillId=${spec.id} taskKey=$taskKey memoryGroup=$effectiveMemoryGroup maxRounds=$effectiveMaxRounds")
            ExecutionContexts.stack.push(
                ExecutionContextFrame(
                    type = ExecutionContextType.SKILL,
                    id = skillTaskKey,
                    name = spec.name,
                    rootTaskId = taskKey
                )
            )
            AgentCommandRecorder.pushSkillContext(spec.name)
            val stateBeforeStart = agentContext.taskStateManager.getCurrentState(taskKey)
            if (stateBeforeStart == ExecutionStatus.UNKNOWN) {
                agentContext.taskStateManager.startTask(taskKey)
            } else {
                // 在 Agent / 父 Skill 链路下复用同一个 taskKey 时，不应反复 startTask() 覆盖 PAUSED 等状态
                SkillToolLogger.d("skip startTask taskKey=$taskKey state=$stateBeforeStart")
            }
            agentContext.notifySkillExecuteStart(skillTaskKey)
            val hasVisionModel =
                agentContext.aiServiceProvider.getInferenceModel(InferenceType.IMAGE) != null
            val skillBasePrompt = AIAgentConfig.PromptDefaults.getSkillBaseSystemPrompt(
                supportsVision = AIAgentConfig.VisionConfig.isVisionPipelineActive(hasVisionModel)
            )
            val combinedSystemPrompt = listOf(skillBasePrompt, spec.systemPrompt)
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
            val messages = mutableListOf(
                ChatMessage(role = MessageRole.SYSTEM, content = combinedSystemPrompt),
                ChatMessage(role = MessageRole.USER, content = request.userPrompt)
            )

            val skillGoal = AgentTaskGoal(
                id = taskKey,
                userInput = request.userPrompt,
                input = AgentInput(messages.map { it.copy() })
            )
            // 立即通知一次；再延迟 post 到主线程，确保 ScriptAgentTopView 完成 initWindow 注册后再收到
            notifySkillProgress(skillGoal, messages)
            Handler(Looper.getMainLooper()).postDelayed({
                notifySkillProgress(skillGoal, messages)
            }, 150)

            var currentRound = 0
            var latestAssistantMessage = ""
            val toolErrors = mutableListOf<SkillToolError>()

            while (effectiveMaxRounds !in 0..currentRound) {
                ensureNotTimeout(deadline)
                ensureTaskRunning(taskKey)

                SkillToolLogger.d("round=${currentRound + 1}/$effectiveMaxRounds")
                val scopedTools = buildScopedTools(
                    allowedToolNames = spec.allowedToolNames,
                    localToolBindings = localToolBindings
                )
                // 先基于当前历史构建本轮推理请求；占位 assistant 仅用于 UI 流式展示，不应进入模型输入
                val requestMessagesSnapshot = messages.toList()
                val streaming = StreamingAssistantSession.start(
                    messages = messages,
                    normalNotify = { notifySkillProgress(skillGoal, messages) },
                    streamNotify = { notifySkillProgressStream(skillGoal, messages) },
                    throttleMs = STREAM_THROTTLE_MS
                )
                val chatResponse = inferOnce(
                    skillTaskKey,
                    requestMessagesSnapshot,
                    scopedTools.definitions,
                    onChunk = { chunk ->
                        streaming.onChunk(chunk)
                    }
                )
                    ?: run {
                        CommonToast.getInstance().showToast("Skill 推理失败: ${spec.id}")
                        skillResult = failure(
                            code = SKILL_INFERENCE_FAILED,
                            summary = "Skill 推理返回空结果",
                            message = "请稍后重试。"
                        )
                        return skillResult
                    }

                // 收尾：将最终响应写回占位消息，并以 normal 更新一次（保证最后一次必达 + timeline 完整）
                val assistant = streaming.message()
                streaming.finalizeWith(chatResponse)
                notifySkillProgress(skillGoal, messages)

                latestAssistantMessage = listOfNotNull(chatResponse.reasoningContent, chatResponse.content)
                    .joinToString(" ")
                    .trim()

                val toolCalls = chatResponse.toolCalls.orEmpty()
                if (toolCalls.isEmpty()) {
                    val summary = latestAssistantMessage.ifEmpty { "Skill 执行完成" }
                    skillResult = SkillResult(
                        status = SkillResult.STATUS_SUCCESS,
                        summary = summary,
                        toolErrors = toolErrors.takeIf { it.isNotEmpty() }
                    )
                    SkillToolLogger.d("runSkill done status=${skillResult.status} summary=$summary")
                    return skillResult
                }

                for (toolCall in toolCalls) {
                    ensureNotTimeout(deadline)
                    ensureTaskRunning(taskKey)

                    val toolName = toolCall.function.name
                    val argsStr = toolCall.function.arguments?.toString() ?: "{}"
                    SkillToolLogger.d("toolCall toolName=$toolName args=$argsStr reason=$latestAssistantMessage")

                    if (!spec.allowedToolNames.contains(toolName)) {
                        CommonToast.getInstance().showToast("Skill 调用未授权工具: ${spec.id} → $toolName")
                        skillResult = failure(
                            code = SKILL_TOOL_FORBIDDEN,
                            summary = "Skill 试图调用未授权工具，执行被拒绝",
                            message = "toolName=$toolName 不在 allowedToolNames 内。",
                            details = mapOf(
                                "skillId" to spec.id,
                                "toolName" to toolName
                            )
                        )
                        return skillResult
                    }

                    val toolResult = executeScopedToolCall(
                        toolCall = toolCall,
                        functionToTool = scopedTools.functionToTool,
                        localToolBindings = scopedTools.localToolBindings,
                        taskId = taskKey,
                        memoryGroup = effectiveMemoryGroup
                    )
                    SkillToolLogger.d("toolCall result toolName=$toolName success=${toolResult.success} info=${toolResult.error?.getInfo()}")
                    val resultText = if (toolResult.success) {
                        val dataText = toolResult.data?.toString().orEmpty()
                        val extraText = toolResult.extra
                        if (!extraText.isNullOrEmpty()) "$dataText\n$extraText" else dataText
                    } else {
                        "执行失败: ${toolResult.error?.getInfo()}"
                    }
                    val toolAttachments = (toolResult.files ?: emptyList()).toMutableList()
                    messages.add(
                        ChatMessage(
                            role = MessageRole.TOOL,
                            content = resultText,
                            toolCallId = toolCall.id,
                            toolCalls = listOf(toolCall),
                            toolCallResult = resultText,
                            toolCallResultSuccess = toolResult.success,
                            status = MessageStatus.FINISH,
                            attachments = toolAttachments
                        )
                    )
                    notifySkillProgress(skillGoal, messages)
                }
                assistant.status = MessageStatus.FINISH
                notifySkillProgress(skillGoal, messages)

//                    if (!toolResult.success) {
//                        skillResult = failure(
//                            code = SKILL_TOOL_EXECUTION_FAILED,
//                            summary = "Skill 执行工具失败",
//                            message = toolResult.error?.getInfo(),
//                            details = mapOf(
//                                "skillId" to spec.id,
//                                "toolName" to toolCall.function.name
//                            )
//                        )
//                        return skillResult!!
//                    }

                currentRound++
            }

            skillResult = SkillResult(
                status = SkillResult.STATUS_PARTIAL,
                summary = "达到最大轮次限制，Skill 提前结束",
                message = latestAssistantMessage.ifEmpty { "maxRounds=$effectiveMaxRounds" },
                toolErrors = toolErrors.takeIf { it.isNotEmpty() }
            )
            SkillToolLogger.d("runSkill done status=PARTIAL summary=${skillResult.summary}")
            skillResult
        } catch (e: AgentError) {
            SkillToolLogger.e("runSkill catch AgentError code=${e.code} msg=${e.msg}", e)
            skillResult = SkillResult(
                status = SkillResult.STATUS_FAILURE,
                summary = "Skill 执行失败",
                message = e.msg ?: e.code.name,
                error = SkillError(
                    code = e.code.name,
                    message = e.msg ?: e.code.name,
                    aiErrorDetail = e.aiErrorDetail  // 传递 AI 错误详情（如 402 token 不足）
                )
            )
            skillResult
        } catch (e: Exception) {
            SkillToolLogger.e("runSkill catch Exception skillId=${spec.id} msg=${e.message}", e)
            CommonToast.getInstance().showToast("Skill 执行异常: ${spec.id} - ${e.message}")
            skillResult = failure(
                code = SKILL_INFERENCE_FAILED,
                summary = "Skill 执行异常",
                message = e.message
            )
            skillResult
        } finally {
            AgentCommandRecorder.popSkillContext()
            ExecutionContexts.stack.pop(expectedId = skillTaskKeyForEnd)
            skillTaskKeyForEnd?.let { MessageSummaryProcessor.clearTaskSummary(it) }
            if (isStandalone && taskKeyForEnd != null) {
                val result = skillResult ?: failure(
                    code = SKILL_INFERENCE_FAILED,
                    summary = "Skill 执行异常",
                    message = "未知错误"
                )
                val status = when (result.status) {
                    SkillResult.STATUS_SUCCESS, SkillResult.STATUS_PARTIAL -> ExecutionStatus.SUCCESS
                    else -> ExecutionStatus.FAILED
                }
                agentContext.taskStateManager.setCurrentState(taskKeyForEnd, status)
                agentContext.notifySkillExecuteEnd(skillTaskKeyForEnd ?: "", result)
                agentContext.taskStateManager.clearTask(taskKeyForEnd)
            } else if (taskKeyForEnd != null) {
                val result = skillResult ?: failure(
                    code = SKILL_INFERENCE_FAILED,
                    summary = "Skill 执行异常",
                    message = "未知错误"
                )
                agentContext.notifySkillExecuteEnd(skillTaskKeyForEnd ?: "", result)
            }
        }
    }

    private suspend fun inferOnce(
        taskId: String,
        messages: List<ChatMessage>,
        scopedToolDefinitions: List<ToolDefinition>,
        onChunk: ((ChatCompletionResponse) -> Unit)? = null
    ): ChatCompletionResponse? {
        val (provider, model) = selectAIProviderAndModel(AgentInput(messages))
            ?: return null

        val input = AgentInput(messageProcessor(taskId, messages, model))
        val request = AIRequest(
            model = model.modelId,
            requestType = AIRequestType.FUNCTION_CALL,
            input = input,
            inputOrigin = AgentInput(messages),
            tools = scopedToolDefinitions
        )
        val aiResult = if (onChunk != null) {
            provider.streamInference<ChatCompletionResponse>(request, onChunkResponse = onChunk)
        } else {
            provider.inference<ChatCompletionResponse>(request)
        }
        return when (aiResult) {
            is AIResult.Success -> aiResult.data
            is AIResult.Failure -> throw AgentError.create(
                code = aiResult.error.code,
                message = aiResult.error.getInfo()
            )
        }
    }

    private fun buildScopedTools(
        allowedToolNames: List<String>,
        localToolBindings: Map<String, McpToolModel>
    ): ScopedTools {
        if (allowedToolNames.isEmpty()) {
            return ScopedTools(emptyList(), emptyMap(), emptyMap())
        }

        val provider = ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider
        val tools = provider?.getRegisteredTools() ?: agentContext.agentManager.getRegisteredTools()

        val definitions = mutableListOf<ToolDefinition>()
        val functionToTool = LinkedHashMap<String, AgentToolClient>()
        val localByName = LinkedHashMap<String, McpToolModel>()
        val allowedSet = allowedToolNames.toHashSet()

        localToolBindings.forEach { (functionName, mcpTool) ->
            if (functionName !in allowedSet) return@forEach
            definitions.add(
                ToolDefinition(
                    function = com.hive.plugin.agent.model.FunctionDefinition(
                        name = mcpTool.name,
                        description = mcpTool.description,
                        parameters = mcpTool.inputSchema
                    )
                )
            )
            localByName[functionName] = mcpTool
        }

        tools.forEach { tool ->
            tool.toToolDefinitions().forEach { definition ->
                val functionName = definition.function.name
                if (functionName in allowedSet
                    && !localByName.containsKey(functionName)
                    && !functionToTool.containsKey(functionName)
                ) {
                    definitions.add(definition)
                    functionToTool[functionName] = tool
                }
            }
        }
        return ScopedTools(definitions, functionToTool, localByName)
    }

    private suspend fun executeScopedToolCall(
        toolCall: ToolCall,
        functionToTool: Map<String, AgentToolClient>,
        localToolBindings: Map<String, McpToolModel>,
        taskId: String?,
        memoryGroup: String? = null
    ): AgentResult<*> {
        val functionName = toolCall.function.name

        // 优先执行本地作用域工具
        localToolBindings[functionName]?.let { mcpTool ->
            val arguments = toolCall.function.arguments ?: JsonObject()
            return runCatching {
                val actionResult = mcpTool.handler(arguments)
                if (actionResult.success) {
                    AgentResult.Success(
                        data = actionResult.data,
                        message = actionResult.message
                    )
                } else {
                    AgentResult.Failure(
                        AgentError.create(
                            AgentErrorCode.EXECUTION_FAILED,
                            actionResult.message ?: "Local scope tool execution failed: $functionName"
                        )
                    )
                }
            }.getOrElse { e ->
                AgentResult.Failure(
                    AgentError.create(
                        AgentErrorCode.EXECUTION_FAILED,
                        e.message ?: "Local scope tool execution exception: $functionName"
                    )
                )
            }
        }

        // 否则执行全局工具
        val tool = functionToTool[functionName]
            ?: return AgentResult.Failure(
                AgentError.create(
                    AgentErrorCode.TOOL_NOT_FOUND,
                    "未找到工具: $functionName"
                )
            )

        var arguments = jsonToMap(toolCall.function.arguments)
        if (functionName == com.hive.plugin.mcp.McpConst.Tool_Name_Prefix_BuildIn + "memoryNote" && !memoryGroup.isNullOrBlank()) {
            arguments = arguments.toMutableMap().apply { put("group", memoryGroup) }
        }
        val request = if (tool is McpToolClient) {
            AgentRequest(
                toolId = tool.id,
                action = "tools_call",
                params = mapOf(
                    "name" to tool.resolveRawToolName(functionName),
                    "arguments" to arguments
                ),
                taskId = taskId
            )
        } else {
            AgentRequest(
                toolId = tool.id,
                action = functionName,
                params = arguments,
                taskId = taskId
            )
        }
        return agentContext.agentManager.dispatchRequest<Any>(request)
    }

    private fun selectAIProviderAndModel(agentInput: AgentInput): Pair<com.hive.plugin.agent.AIServiceProvider, ModelInfo>? {
        val aiServiceManager = agentContext.aiServiceProvider
        val hasImages = agentInput.messages.any { it.attachments.isNotEmpty() }
        val hasVisionModel = aiServiceManager.getInferenceModel(InferenceType.IMAGE) != null
        val visionPipelineActive =
            AIAgentConfig.VisionConfig.isVisionPipelineActive(hasVisionModel)
        val selectedModel = if (hasImages && visionPipelineActive) {
            aiServiceManager.getInferenceModel(InferenceType.IMAGE)
                ?: aiServiceManager.getInferenceModel(InferenceType.TEXT)
        } else {
            aiServiceManager.getInferenceModel(InferenceType.TEXT)
        } ?: return null
        val selectedProvider = aiServiceManager.getProvider(selectedModel.providerId) ?: return null
        return Pair(selectedProvider, selectedModel)
    }

    private suspend fun ensureTaskRunning(taskId: String?) {
        if (taskId.isNullOrBlank()) return
        if (agentContext.taskStateManager.checkPaused(taskId)) {
            SkillToolLogger.w("ensureTaskRunning paused taskId=$taskId state=${agentContext.taskStateManager.getCurrentState(taskId)}")
            agentContext.taskStateManager.checkPausedAndWait(taskId)
        }
        if (agentContext.taskStateManager.checkStopped(taskId)) {
            SkillToolLogger.w("ensureTaskRunning stopped taskId=$taskId state=${agentContext.taskStateManager.getCurrentState(taskId)}")
            throw AgentError.create(AgentErrorCode.TASK_STATE_STOP, "任务已停止")
        }
    }

    private fun ensureNotTimeout(deadline: Long) {
        if (System.currentTimeMillis() > deadline) {
            throw AgentError.create(AgentErrorCode.TIMEOUT, "Skill 执行超时")
        }
    }

    private fun jsonToMap(jsonObject: JsonObject): Map<String, Any> {
        return GsonHelper.getInstance().fromJson(
            jsonObject.toString(),
            object : TypeToken<Map<String, Any>>() {}.type
        ) ?: emptyMap()
    }

    private fun notifySkillProgress(goal: AgentTaskGoal, messages: List<ChatMessage>) {
        goal.input = AgentInput(messages.map { it.copy() })
        agentContext.notifySkillMessageUpdated(goal)
    }

    private fun notifySkillProgressStream(goal: AgentTaskGoal, messages: List<ChatMessage>) {
        goal.input = AgentInput(messages.map { it.copy() })
        agentContext.notifySkillMessageStreamUpdated(goal)
    }

    private fun failure(
        code: String,
        summary: String,
        message: String? = null,
        details: Map<String, Any?>? = null
    ): SkillResult {
        return SkillResult(
            status = SkillResult.STATUS_FAILURE,
            summary = summary,
            message = message,
            error = SkillError(code = code, message = message ?: summary, details = details)
        )
    }

    private data class ScopedTools(
        val definitions: List<ToolDefinition>,
        val functionToTool: Map<String, AgentToolClient>,
        val localToolBindings: Map<String, McpToolModel>
    )
}
