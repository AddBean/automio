// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent.model

data class SkillSpec(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val allowedToolNames: List<String>,
    val maxRounds: Int? = null,
    val timeoutMs: Long? = null,
    val fallbackSkillId: String? = null,
    val memoryGroup: String? = null,
    /**
     * 来源 workflow 的 scriptUid 列表。多源合并规则由上层 registry 实现：已存在则追加并去重。
     */
    val sources: List<String>? = null,
    /**
     * 可选：scriptUid -> 脚本名，用于 UI 展示。
     */
    val sourceScriptNames: Map<String, String>? = null,
    /**
     * 可选：semver，用于导入冲突比较（阶段一先铺字段）。
     */
    val version: String? = null
)

data class RunSkillRequest(
    val skillId: String,
    val userPrompt: String,
    val options: RunSkillOptions? = null
)

data class RunSkillOptions(
    val timeoutMs: Long? = null,
    val maxRounds: Int? = null,
    val memoryGroup: String? = null,
    val depth: Int? = null,
    val attachments: List<String>? = null
)

/** 单个工具执行错误，用于 Skill 执行过程中发生的工具失败 */
data class SkillToolError(
    val toolName: String,
    val errorMessage: String
)

data class SkillResult(
    val status: String,
    val summary: String,
    val message: String? = null,
    val error: SkillError? = null,
    val extra: String? = null,
    /** 执行过程中发生的工具错误，工具失败后继续执行时由上层感知 */
    val toolErrors: List<SkillToolError>? = null
) {
    companion object {
        const val STATUS_SUCCESS = "success"
        const val STATUS_PARTIAL = "partial"
        const val STATUS_FAILURE = "failure"
    }
}

data class SkillError(
    val code: String,
    val message: String,
    val details: Map<String, Any?>? = null,
    /**
     * AI 错误详情，用于在 runSkill 等场景中传递详细的 AI 推理错误（如 402 token 不足）
     * 以便调用方能够根据错误类型显示友好的错误弹窗
     */
    val aiErrorDetail: AIErrorDetail? = null
)
