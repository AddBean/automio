// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.scope

import com.hive.script.base.ScriptConst
import java.io.File

/**
 * 本地资源列表统一仓库
 * 统一管理三种本地资源的列表查询和存在性检查：
 * - Workflow: Save_Script_Path
 * - Primary Skill: Save_Skill_Path
 * - Standalone Tool: Save_Tool_Path
 *
 * 文件系统是唯一可信数据源，不依赖 SP 或 Agent 注册表。
 */
object LocalResourceListRepository {

    private fun isValidWorkflowDir(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory || dir.name.startsWith(".")) return false
        return File(dir, ScriptConst.SCRIPT_MAIN_INFO_FILE_NAME).isFile ||
            File(dir, ScriptConst.SCRIPT_MAIN_FILE_NAME).isFile ||
            File(dir, ScriptConst.SCRIPT_MAIN_ENCRYPT_FILE_NAME).isFile
    }

    // ==================== Workflow ====================

    /**
     * 列出所有本地 workflow 目录
     * @return workflow 目录列表（排除以 . 开头的特殊目录）
     */
    fun listWorkflows(): List<File> {
        val root = File(ScriptConst.Save_Script_Path)
        if (!root.exists() || !root.isDirectory) return emptyList()
        return root.listFiles()
            ?.filter(::isValidWorkflowDir)
            ?: emptyList()
    }

    /**
     * 检查 workflow 是否存在
     * @param scriptDir workflow 目录名（如 "my_workflow"）
     */
    fun workflowExists(scriptDir: String): Boolean {
        if (scriptDir.isBlank()) return false
        return isValidWorkflowDir(File(ScriptConst.Save_Script_Path, scriptDir))
    }

    /**
     * 获取 workflow 目录
     * @param scriptDir workflow 目录名
     */
    fun getWorkflowDir(scriptDir: String): File? {
        if (scriptDir.isBlank()) return null
        val dir = File(ScriptConst.Save_Script_Path, scriptDir)
        return dir.takeIf(::isValidWorkflowDir)
    }

    // ==================== Primary Skill ====================

    /**
     * 列出所有本地 primary skill 目录
     * @return skill 目录列表（每个目录对应一个独立的 skill）
     */
    fun listPrimarySkills(): List<File> {
        val root = File(ScriptConst.Save_Skill_Path)
        if (!root.exists() || !root.isDirectory) return emptyList()
        return root.listFiles()
            ?.filter { it.isDirectory }
            ?: emptyList()
    }

    /**
     * 检查 primary skill 是否存在
     * @param skillId skill ID
     */
    fun skillExists(skillId: String): Boolean {
        if (skillId.isBlank()) return false
        return SkillFileHelper.getSkillPackageDir(skillId).exists()
    }

    /**
     * 获取 primary skill 目录
     * @param skillId skill ID
     */
    fun getSkillDir(skillId: String): File? {
        if (skillId.isBlank()) return null
        val dir = SkillFileHelper.getSkillPackageDir(skillId)
        return dir.takeIf { it.exists() && it.isDirectory }
    }

    // ==================== Standalone Tool ====================

    /**
     * 列出所有本地独立 tool 目录
     * @return tool 目录列表（每个目录对应一个独立的 tool）
     */
    fun listStandaloneTools(): List<File> {
        val root = File(ScriptConst.Save_Tool_Path)
        if (!root.exists() || !root.isDirectory) return emptyList()
        return root.listFiles()
            ?.filter { it.isDirectory }
            ?: emptyList()
    }

    /**
     * 检查 standalone tool 是否存在
     * @param toolId tool ID（可能带 "custom." 前缀）
     */
    fun toolExists(toolId: String): Boolean {
        if (toolId.isBlank()) return false
        val toolUid = toolId.removePrefix(ScriptConst.SCRIPT_TOOL_ID_PREFIX)
        return File(ScriptConst.Save_Tool_Path, toolUid).exists()
    }

    /**
     * 获取 standalone tool 目录
     * @param toolId tool ID（可能带 "custom." 前缀）
     */
    fun getToolDir(toolId: String): File? {
        if (toolId.isBlank()) return null
        val toolUid = toolId.removePrefix(ScriptConst.SCRIPT_TOOL_ID_PREFIX)
        val dir = File(ScriptConst.Save_Tool_Path, toolUid)
        return dir.takeIf { it.exists() && it.isDirectory }
    }

    // ==================== 通用工具方法 ====================

    /**
     * 获取资源统计信息
     */
    data class ResourceStats(
        val workflowCount: Int = 0,
        val skillCount: Int = 0,
        val toolCount: Int = 0
    )

    fun getStats(): ResourceStats {
        return ResourceStats(
            workflowCount = listWorkflows().size,
            skillCount = listPrimarySkills().size,
            toolCount = listStandaloneTools().size
        )
    }

    /**
     * 检查任一资源是否存在（兼容旧逻辑）
     */
    fun anyResourceExists(
        scriptDirs: List<String>?,
        skillIds: List<String>?,
        toolIds: List<String>?
    ): Boolean {
        val hasWorkflow = scriptDirs?.any { workflowExists(it) } == true
        val hasSkill = skillIds?.any { skillExists(it) } == true
        val hasTool = toolIds?.any { toolExists(it) } == true
        return hasWorkflow || hasSkill || hasTool
    }
}
