// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.scope

import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IAgentProvider
import com.hive.plugin.provider.IMcpProvider
import com.hive.script.mcp.ScriptMcpRegister
import com.hive.utils.debug.DLog
import java.io.File

/**
 * 全局注册入口（V1）：
 * - 不再把 workflow dependence 自动升全局；
 * - 仅恢复已持久化的全局资源（custom tools / primary skills 等）。
 */
object GlobalScriptRegistry {

    private const val TAG = "GlobalScriptRegistry"

    class RegisterOptions

    fun registerFromWorkflow(workflowDir: File) {
        registerFromWorkflow(workflowDir, RegisterOptions())
    }

    fun registerFromWorkflow(workflowDir: File, options: RegisterOptions) {
        // V1: dependence 仅运行时可见，不在安装/保存阶段提升为全局资源。
        return
    }

    fun registerAllInstalled() {
        getAgentProvider() ?: return

        // 注册 SP 中的 custom tools（custom.<uid>）到 MCP
        CustomStorage.readCustomTools().forEach { tool ->
            registerRuntimeTool(
                scriptName = tool.scriptName,
                scriptDesc = tool.scriptDesc,
                scriptDir = File(tool.scriptPath),
                toolId = tool.scriptId
            )
        }
    }

    private fun registerRuntimeTool(
        scriptName: String,
        scriptDesc: String,
        scriptDir: File,
        toolId: String
    ) {
        if (!scriptDir.exists() || !scriptDir.isDirectory) {
            DLog.w(TAG, "skip missing toolDir=${scriptDir.absolutePath}")
            return
        }
        ScriptMcpRegister.registerCustomTool(
            scriptName = scriptName,
            scriptDesc = scriptDesc,
            scriptPath = scriptDir.absolutePath,
            toolId = toolId,
            overwriteIfExists = true,
            persistToSp = false
        )
    }

    private fun getAgentProvider(): IAgentProvider? {
        return ComponentManager.getInstance()
            .getProvider(IAgentProvider::class.java) as? IAgentProvider
    }

    /**
     * 孤儿：sources 中所有 scriptUid 指向的脚本目录均已不存在。
     * 清理孤儿 skill/tool 并返回清理数量。
     */
    data class CleanupResult(
        val skillsRemoved: Int = 0,
        val toolsRemoved: Int = 0,
        val customToolsRemovedFromSp: Int = 0,
        val customSkillsRemovedFromSp: Int = 0
    )

    fun cleanupOrphans(): CleanupResult {
        val provider = getAgentProvider() ?: return CleanupResult()
        var skillsRemoved = 0
        var toolsRemoved = 0
        var customSkillsRemoved = 0
        val mcpProvider = ComponentManager.getInstance()
            .getProvider(IMcpProvider::class.java) as? IMcpProvider

        // 1) 孤儿 skill（workflow 依赖，有 sources）
        val skills = runCatching { provider.listSkills() }.getOrDefault(emptyList())
        skills.forEach { spec ->
            val sources = spec.sources.orEmpty()
            if (sources.isEmpty()) return@forEach
            val allMissing = sources.all { scriptUid -> !isScriptDirExists(scriptUid) }
            if (allMissing) {
                runCatching { provider.unregisterSkillSpec(spec.id) }
                skillsRemoved++
            }
        }

        // 2) 孤儿 tool（Agent 中注册的：历史遗留/非 script.* 场景）
        val tools = runCatching { provider.getRegisteredTools() }.getOrDefault(emptyList())
        tools.forEach { tool ->
            val sources = tool.sources
            if (sources.isEmpty()) return@forEach
            val allMissing = sources.all { scriptUid -> !isScriptDirExists(scriptUid) }
            if (allMissing) {
                runCatching { provider.unregisterTool(tool.id) }
                toolsRemoved++
            }
        }

        // 3) SP 中 custom tools：scriptPath 不存在则移除
        val customTools = CustomStorage.readCustomTools()
        val validCustomTools = customTools.filter {
            LocalResourceListRepository.toolExists(it.scriptId)
        }
        val customRemoved = customTools.size - validCustomTools.size
        val validIds = validCustomTools.map { it.scriptId }.toSet()
        customTools.filter { it.scriptId !in validIds }.forEach { dead ->
            val toolId = dead.scriptId
            runCatching { mcpProvider?.unregisterTool(toolId) }
            toolsRemoved++
        }
        if (customRemoved > 0) {
            CustomStorage.saveCustomTools(validCustomTools)
        }

        // 4) SP 中 custom skills：skillPath 不存在则移除
        val customSkillRefs = CustomStorage.readCustomSkillRefs()
        val validSkillRefs = customSkillRefs.filter {
            LocalResourceListRepository.skillExists(it.skillId)
        }
        customSkillsRemoved = customSkillRefs.size - validSkillRefs.size
        customSkillRefs.filter { it !in validSkillRefs }.forEach { dead ->
            runCatching { provider.unregisterSkillSpec(dead.skillId) }
            skillsRemoved++
        }
        if (customSkillsRemoved > 0) {
            CustomStorage.saveCustomSkillRefs(validSkillRefs)
        }

        // 刷新 Agent 的 MCP 工具列表
        if (customRemoved > 0) {
            runCatching { provider.refreshAllMcpServer { } }
        }

        return CleanupResult(
            skillsRemoved = skillsRemoved,
            toolsRemoved = toolsRemoved,
            customToolsRemovedFromSp = customRemoved,
            customSkillsRemovedFromSp = customSkillsRemoved
        )
    }

    private fun isScriptDirExists(scriptUid: String): Boolean {
        return ScriptScopeRepository.findLocalScriptDirsByUid(scriptUid).isNotEmpty()
    }
}
