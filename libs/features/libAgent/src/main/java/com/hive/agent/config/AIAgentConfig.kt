// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.config

import com.hive.agent.R
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.ScriptCommandHelper
import com.hive.plugin.agent.model.ReasoningEffort
import com.hive.plugin.agent.model.ReasoningOptions
import com.hive.utils.GlobalApp
import com.hive.utils.global.MMKVTools

/**
 * A task-local copy of the user setting. It deliberately has no dependency on MMKV so a
 * running request cannot be affected by a settings change made after the task starts.
 */
data class ReasoningRunPolicy(
    val enabled: Boolean,
    val effort: ReasoningEffort = ReasoningEffort.MEDIUM
) {
    fun asOptions(): ReasoningOptions = ReasoningOptions(enabled = enabled, effort = effort)

    companion object {
        fun from(options: ReasoningOptions): ReasoningRunPolicy =
            ReasoningRunPolicy(enabled = options.enabled, effort = options.effort)
    }
}

object AIAgentConfig {
    object BaseConfig {

        const val MAX_HISTORY_COUNT = 18

        const val MAX_NOT_SIMPLIFY_COUNT = 2

        const val MAX_TEXT_SIZE_SIMPLIFY_TEXT = 200

        const val MAX_ATTACHMENT_KEEP_REQUEST = 1 //请求时最多保持的附件数量

        const val McpToolName = "mcp"

        const val AIAssistantToolName = "ai_assistant"

    }


    /**
     * Agent 任务记忆摘要配置
     * 使用 MMKV 持久化全局开关
     */
    object MemoryConfig {

        // 触发 AI 总结的阈值（累计删除消息数）
        const val AI_SUMMARY_TRIGGER_COUNT = 20

        private const val MMKV_KEY_TASK_MEMORY_ENABLED = "agent_task_memory_enabled"

        private const val DEFAULT_TASK_MEMORY_ENABLED = true

        /**
         * 是否启用任务记忆摘要功能
         */
        @JvmStatic
        fun isTaskMemoryEnabled(): Boolean {
            return MMKVTools.getInstance().getBoolean(MMKV_KEY_TASK_MEMORY_ENABLED, DEFAULT_TASK_MEMORY_ENABLED)
        }

        /**
         * 设置任务记忆摘要开关
         */
        @JvmStatic
        fun setTaskMemoryEnabled(enabled: Boolean) {
            MMKVTools.getInstance().putBoolean(MMKV_KEY_TASK_MEMORY_ENABLED, enabled)
        }
    }

    /**
     * 视觉识别开关（独立于已选视觉模型；关闭后不走多模态链路，以降低 token 消耗）
     */
    object VisionConfig {

        private const val MMKV_KEY_VISION_RECOGNITION_ENABLED = "agent_vision_recognition_enabled"

        private const val DEFAULT_VISION_RECOGNITION_ENABLED = true

        @JvmStatic
        fun isVisionRecognitionEnabled(): Boolean {
            return MMKVTools.getInstance()
                .getBoolean(MMKV_KEY_VISION_RECOGNITION_ENABLED, DEFAULT_VISION_RECOGNITION_ENABLED)
        }

        @JvmStatic
        fun setVisionRecognitionEnabled(enabled: Boolean) {
            MMKVTools.getInstance().putBoolean(MMKV_KEY_VISION_RECOGNITION_ENABLED, enabled)
        }

        /** 用户开关开启，且当前模型具备视觉能力时，才视为可用视觉。 */
        @JvmStatic
        fun effectiveSupportsVision(modelSupportsVision: Boolean): Boolean {
            return isVisionRecognitionEnabled() && modelSupportsVision
        }

        /**
         * 视觉链路是否真正可用：开关开启且已配置视觉模型。
         * 用于选模、system prompt、以及是否向 API 发送图片附件。
         */
        @JvmStatic
        fun isVisionPipelineActive(hasConfiguredVisionModel: Boolean): Boolean {
            return isVisionRecognitionEnabled() && hasConfiguredVisionModel
        }
    }

    /**
     * 全局思考模式设置。每个任务须先调用 [snapshot] 固化为 [ReasoningRunPolicy]。
     */
    object ReasoningConfig {

        private const val MMKV_KEY_REASONING_ENABLED = "agent_reasoning_enabled"
        private const val MMKV_KEY_REASONING_EFFORT = "agent_reasoning_effort"
        private const val DEFAULT_REASONING_ENABLED = false
        private const val DEFAULT_REASONING_EFFORT = "MEDIUM"

        @JvmStatic
        fun isEnabled(): Boolean = MMKVTools.getInstance().getBoolean(
            MMKV_KEY_REASONING_ENABLED,
            DEFAULT_REASONING_ENABLED
        )

        @JvmStatic
        fun setEnabled(enabled: Boolean) {
            MMKVTools.getInstance().putBoolean(MMKV_KEY_REASONING_ENABLED, enabled)
        }

        @JvmStatic
        fun effort(): ReasoningEffort = runCatching {
            ReasoningEffort.valueOf(
                MMKVTools.getInstance().getString(MMKV_KEY_REASONING_EFFORT, DEFAULT_REASONING_EFFORT)
                    ?: DEFAULT_REASONING_EFFORT
            )
        }.getOrDefault(ReasoningEffort.MEDIUM)

        @JvmStatic
        fun setEffort(effort: ReasoningEffort) {
            MMKVTools.getInstance().putString(MMKV_KEY_REASONING_EFFORT, effort.name)
        }

        @JvmStatic
        fun snapshot(): ReasoningRunPolicy = ReasoningRunPolicy(isEnabled(), effort())
    }

    object PromptDefaults {

        suspend fun getOptimizedUserPrompt(userInput: String): String {
            return GlobalApp.getString(com.hive.i8n.R.string.agent_config_optimized_user_prompt, userInput)
        }

        suspend fun getToolResult(data: String?, extra: String?): String {
            val dataText = data ?: GlobalApp.getString(com.hive.i8n.R.string.agent_config_tool_result_no_data)
            val extraText = extra?.trim()
            return if (extraText.isNullOrBlank()) {
                GlobalApp.getString(com.hive.i8n.R.string.agent_config_tool_result_simple, dataText)
            } else {
                GlobalApp.getString(com.hive.i8n.R.string.agent_config_tool_result_with_extra, dataText, extraText)
            }
        }

        private val agentPromptTemplate: String by lazy {
            runCatching {
                GlobalApp.getContext().resources.openRawResource(R.raw.agent)
                    .bufferedReader().use { it.readText() }
            }.getOrElse { "" }
        }

        private val skillPromptTemplate: String by lazy {
            runCatching {
                GlobalApp.getContext().resources.openRawResource(R.raw.skill)
                    .bufferedReader().use { it.readText() }
            }.getOrElse { "" }
        }

        fun getAutoSystemPrompt(supportsVision: Boolean? = null): String {
            val base = ScriptCommandHelper.parseParamValue(
                ScriptParamEnv.getParamEnv(),
                agentPromptTemplate
            ) ?: agentPromptTemplate
            val capabilitySection = when (supportsVision) {
                true -> """
                    |<model_capabilities>
                    |You have vision capabilities. You can understand and analyze images from user attachments. Encourage using screenshot tools (captureScreen etc..)
                    |</model_capabilities>""".trimMargin()
                false -> """
                    |<model_capabilities>
                    |You do NOT have vision capabilities. You cannot process images. 
                    |</model_capabilities>""".trimMargin()
                null -> ""
            }
            return base + if (capabilitySection.isNotEmpty()) "\n\n$capabilitySection" else ""
        }

        fun getSkillBaseSystemPrompt(supportsVision: Boolean? = null): String {
            val base = ScriptCommandHelper.parseParamValue(
                ScriptParamEnv.getParamEnv(),
                skillPromptTemplate
            ) ?: skillPromptTemplate
            val capabilitySection = when (supportsVision) {
                true -> """
                    |<model_capabilities>
                    |You have vision capabilities. You can understand and analyze images from user attachments. Encourage using screenshot tools (captureScreen etc..)
                    |</model_capabilities>""".trimMargin()
                false -> """
                    |<model_capabilities>
                    |You do NOT have vision capabilities.You cannot process images. 
                    |</model_capabilities>""".trimMargin()
                null -> ""
            }
            return base + if (capabilitySection.isNotEmpty()) "\n\n$capabilitySection" else ""
        }
    }

}
