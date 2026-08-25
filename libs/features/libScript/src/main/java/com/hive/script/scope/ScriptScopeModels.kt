// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.scope

import com.hive.plugin.agent.model.SkillSpec

/**
 * Scope 依赖模型。
 *
 * 依赖资源有三种：Workflow、Skill、Tool
 * - Workflow 依赖：callScript 引用，存于 dependence/scripts/
 * - Skill 依赖：runSkill 引用，存于 dependence/skills/
 * - Tool 依赖：skill.allowedToolNames 或 scope，存于 dependence/tools/
 */
data class ScriptScopeToolsFile(
    val version: Int = ScriptScopeRepository.SCOPE_VERSION,
    val scopeId: String,
    val scriptUid: String,
    val tools: List<ScopedToolSpec> = emptyList()
)

data class ScriptScopeSkillsFile(
    val version: Int = ScriptScopeRepository.SCOPE_VERSION,
    val scopeId: String,
    val scriptUid: String,
    val skills: List<ScopedSkillRef> = emptyList()
)

/** scripts.json：Workflow 依赖引用，callScript 引用的子工作流放在 dependence/scripts/{scriptDir} */
data class ScriptScopeScriptsFile(
    val version: Int = ScriptScopeRepository.SCOPE_VERSION,
    val scopeId: String,
    val scriptUid: String,
    val scripts: List<ScopedDependencyScriptRef> = emptyList()
)

/** Workflow 依赖引用：localId 用于 scope:// 解析，scriptDir 为 dependence/scripts 下的子目录名 */
data class ScopedDependencyScriptRef(
    val scriptUid: String,
    val localId: String,
    val scriptDir: String,
    val name: String = ""
)

/** skills.json 中的简介引用，详情在 dependence/skills/{skillDir} 文件中 */
data class ScopedSkillRef(
    val id: String,
    val name: String,
    val description: String,
    val skillDir: String = "",
    val isPublic: Boolean = false,
    val version: String? = null
)

data class ScopedToolSpec(
    val toolUid: String,
    val localId: String,
    val functionName: String,
    val scriptDir: String,
    val name: String,
    val description: String,
    val inputSchemaDigest: String? = null,
    val version: String? = null,
    val sourceGlobalToolId: String? = null,
    val sourceScriptPath: String? = null,
    val sourceScriptUid: String? = null,
    /** 是否对主 Agent 可见；默认 false（private） */
    val isPublic: Boolean = false
)

data class ScopedSkillSpec(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val allowedToolNames: List<String> = emptyList(),
    val maxRounds: Int? = null,
    val timeoutMs: Long? = null,
    val fallbackSkillId: String? = null,
    val memoryGroup: String? = null,
    val version: String? = null,
    /** 是否对主 Agent 可见；默认 false（private） */
    val isPublic: Boolean = false,
    /** Skill 文件目录名（dependence/skills/{skillDir}） */
    val skillDir: String = ""
) {
    fun toSkillSpec(): SkillSpec {
        return SkillSpec(
            id = id,
            name = name,
            description = description,
            systemPrompt = systemPrompt,
            allowedToolNames = allowedToolNames,
            maxRounds = maxRounds,
            timeoutMs = timeoutMs,
            fallbackSkillId = fallbackSkillId,
            memoryGroup = memoryGroup,
            version = version
        )
    }

    companion object {
        fun from(spec: SkillSpec): ScopedSkillSpec {
            return ScopedSkillSpec(
                id = spec.id,
                name = spec.name,
                description = spec.description,
                systemPrompt = spec.systemPrompt,
                allowedToolNames = spec.allowedToolNames,
                maxRounds = spec.maxRounds,
                timeoutMs = spec.timeoutMs,
                fallbackSkillId = spec.fallbackSkillId,
                memoryGroup = spec.memoryGroup,
                version = spec.version
            )
        }

        fun fromRefAndSpec(ref: ScopedSkillRef, spec: SkillSpec): ScopedSkillSpec {
            return ScopedSkillSpec(
                id = ref.id,
                name = ref.name,
                description = ref.description,
                systemPrompt = spec.systemPrompt,
                allowedToolNames = spec.allowedToolNames,
                maxRounds = spec.maxRounds,
                timeoutMs = spec.timeoutMs,
                fallbackSkillId = spec.fallbackSkillId,
                memoryGroup = spec.memoryGroup,
                version = ref.version,
                isPublic = ref.isPublic,
                skillDir = ref.skillDir
            )
        }
    }
}

data class ScriptScopeSnapshot(
    val scopeId: String,
    val scriptUid: String,
    val tools: List<ScopedToolSpec>,
    val skills: List<ScopedSkillSpec>,
    /** Workflow 依赖列表（callScript 引用），存于 dependence/scripts/ */
    val scripts: List<ScopedDependencyScriptRef> = emptyList()
)
