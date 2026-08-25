// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.scope

import com.hive.script.base.ScriptConst
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.utils.file.FileUtils
import com.hive.utils.utils.GsonHelper
import java.io.File
import java.util.Locale

/**
 * Scope 仓库：管理 workflow 的依赖元数据。
 *
 * 依赖资源有三种，对应 dependence/ 下子目录：
 * - Workflow：dependence/scripts/（callScript）
 * - Skill：dependence/skills/（runSkill）
 * - Tool：dependence/tools/（skill.allowedToolNames）
 */
object ScriptScopeRepository {
    const val SCOPE_VERSION: Int = 1
    /** 依赖元数据目录名 */
    const val DEPENDENCE_DIR_NAME: String = "dependence"
    const val TOOLS_DIR_NAME: String = "tools"
    const val SKILLS_DIR_NAME: String = "skills"
    const val SCRIPTS_DIR_NAME: String = "scripts"
    const val TOOLS_FILE_NAME: String = "tools.json"
    const val SKILLS_FILE_NAME: String = "skills.json"
    const val SCRIPTS_FILE_NAME: String = "scripts.json"
    const val SCOPED_SCRIPT_PATH_PREFIX: String = "scope://"

    /** 获取 workflow 的依赖元数据目录（dependence） */
    fun getDependenceDir(scriptDir: File): File = File(scriptDir, DEPENDENCE_DIR_NAME)

    fun getToolsDir(scriptDir: File): File = File(getDependenceDir(scriptDir), TOOLS_DIR_NAME)

    fun getSkillsDir(scriptDir: File): File = File(getDependenceDir(scriptDir), SKILLS_DIR_NAME)

    fun getScriptsDir(scriptDir: File): File = File(getDependenceDir(scriptDir), SCRIPTS_DIR_NAME)

    fun getToolsFile(scriptDir: File): File = File(getDependenceDir(scriptDir), TOOLS_FILE_NAME)

    fun getSkillsFile(scriptDir: File): File = File(getDependenceDir(scriptDir), SKILLS_FILE_NAME)

    fun getScriptsFile(scriptDir: File): File = File(getDependenceDir(scriptDir), SCRIPTS_FILE_NAME)

    fun scopeId(scriptUid: String): String = scriptUid.trim()

    /**
     * 从脚本目录解析 scriptUid（info 或 main 的 mate）。
     * 供 Exporter/Installer 等复用，避免重复实现。
     */
    fun getScriptUid(scriptDir: File): String? {
        return runCatching {
            ScriptInfoModel().parseInfoFile(scriptDir).scriptMate?.scriptUid
                ?: ScriptInfoModel().parseMainFile(scriptDir).scriptMate?.scriptUid
        }.getOrNull()
    }

    /**
     * 按 scriptUid 查找本地脚本目录。
     * 供 WorkflowBundleInstaller/ScriptScopeBundleV2Installer 复用。
     */
    fun findLocalScriptDirsByUid(scriptUid: String): List<File> {
        if (scriptUid.isBlank()) return emptyList()
        val root = File(ScriptConst.Save_Script_Path)
        val dirs = root.listFiles()?.filter { it.isDirectory } ?: return emptyList()
        return dirs.filter { dir ->
            getScriptUid(dir) == scriptUid
        }
    }

    /** Local tool functionName，与全局 custom tool 一致：custom.<toolUid> */
    fun localToolFunctionName(toolUid: String): String {
        return ScriptConst.SCRIPT_TOOL_ID_PREFIX + toolUid
    }

    fun normalizeId(raw: String): String {
        if (raw.isBlank()) return "unknown"
        val normalized = raw.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
        return normalized.ifBlank { "unknown" }
    }

    fun toScopedScriptRef(localId: String): String {
        return SCOPED_SCRIPT_PATH_PREFIX + normalizeId(localId)
    }

    fun isScopedScriptRef(path: String?): Boolean {
        return path?.startsWith(SCOPED_SCRIPT_PATH_PREFIX) == true
    }

    fun resolveScopedScriptPath(currentScriptDir: File, ref: String): String? {
        val ownerDir = findOwningScriptDir(currentScriptDir) ?: currentScriptDir
        val snapshot = load(ownerDir, validate = true) ?: return null
        val localId = when {
            isScopedScriptRef(ref) -> ref.removePrefix(SCOPED_SCRIPT_PATH_PREFIX)
            else -> null
        }
        if (localId != null) {
            // 优先从 dependence/scripts 解析（Workflow 依赖）
            val depScript = snapshot.scripts.firstOrNull { it.localId == localId }
            if (depScript != null) {
                return File(getScriptsDir(ownerDir), depScript.scriptDir).absolutePath
            }
            // 其次从 dependence/tools 解析（Tool 依赖）
            val tool = snapshot.tools.firstOrNull { it.localId == localId }
            if (tool != null) {
                return File(getToolsDir(ownerDir), tool.scriptDir).absolutePath
            }
        } else {
            val tool = snapshot.tools.firstOrNull { it.sourceScriptPath == ref }
            if (tool != null) return File(getToolsDir(ownerDir), tool.scriptDir).absolutePath
        }
        return null
    }

    /**
     * 解析 scope skill id，按 id 匹配。
     * 返回 id 供 SkillRegistry.get(scopeId, id) 查找。
     */
    fun resolveScopedSkillId(currentScriptDir: File, rawSkillId: String): String? {
        val ownerDir = findOwningScriptDir(currentScriptDir) ?: currentScriptDir
        val snapshot = load(ownerDir, validate = true) ?: return null
        val skill = snapshot.skills.firstOrNull { it.id == rawSkillId } ?: return null
        return skill.id
    }

    private fun findOwningScriptDir(currentScriptDir: File): File? {
        var cursor: File? = currentScriptDir
        while (cursor != null) {
            val depDir = getDependenceDir(cursor)
            if (depDir.exists() && depDir.isDirectory) return cursor
            cursor = cursor.parentFile
        }
        return null
    }

    fun load(scriptDir: File, validate: Boolean = true): ScriptScopeSnapshot? {
        if (!scriptDir.exists() || !scriptDir.isDirectory) return null
        val toolsFile = getToolsFile(scriptDir)
        val skillsFile = getSkillsFile(scriptDir)
        val scriptsFile = getScriptsFile(scriptDir)
        if (!toolsFile.exists() && !skillsFile.exists() && !scriptsFile.exists()) return null

        val toolsData = if (toolsFile.exists()) {
            GsonHelper.getInstance().fromJson(toolsFile.readText(), ScriptScopeToolsFile::class.java)
        } else null
        val skillsData = if (skillsFile.exists()) {
            GsonHelper.getInstance().fromJson(skillsFile.readText(), ScriptScopeSkillsFile::class.java)
        } else null
        val scriptsData = if (scriptsFile.exists()) {
            GsonHelper.getInstance().fromJson(scriptsFile.readText(), ScriptScopeScriptsFile::class.java)
        } else null

        val scopeId = toolsData?.scopeId?.takeIf { it.isNotBlank() }
            ?: skillsData?.scopeId?.takeIf { it.isNotBlank() }
            ?: scriptsData?.scopeId?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing scopeId in dependence metadata")
        val scriptUid = toolsData?.scriptUid?.takeIf { it.isNotBlank() }
            ?: skillsData?.scriptUid?.takeIf { it.isNotBlank() }
            ?: scriptsData?.scriptUid?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing scriptUid in dependence metadata")

        val tools = toolsData?.tools.orEmpty()
        val skillsRefs = skillsData?.skills.orEmpty().filter { it.skillDir.isNotBlank() }
        val skillsDir = getSkillsDir(scriptDir)
        val skills = skillsRefs.mapNotNull { ref ->
            val spec = SkillFileHelper.readSkillFileFromDir(skillsDir, ref.skillDir)
                ?: return@mapNotNull null
            ScopedSkillSpec.fromRefAndSpec(ref, spec)
        }
        val scripts = scriptsData?.scripts.orEmpty()
        val snapshot = ScriptScopeSnapshot(
            scopeId = scopeId,
            scriptUid = scriptUid,
            tools = tools,
            skills = skills,
            scripts = scripts
        )
        if (validate) validate(scriptDir, snapshot)
        return snapshot
    }

    fun save(scriptDir: File, snapshot: ScriptScopeSnapshot) {
        validate(scriptDir, snapshot)
        val depDir = getDependenceDir(scriptDir)
        FileUtils.makeDirs(depDir.absolutePath)
        FileUtils.makeDirs(getToolsDir(scriptDir).absolutePath)
        FileUtils.makeDirs(getScriptsDir(scriptDir).absolutePath)
        val skillsDir = getSkillsDir(scriptDir)
        if (!skillsDir.exists()) skillsDir.mkdirs()
        val skillsRefs = snapshot.skills.map { scoped ->
            val spec = scoped.toSkillSpec()
            val skillDir = SkillFileHelper.writeSkillFileToDir(skillsDir, spec)
                ?: throw IllegalArgumentException("Failed to write skill file: ${scoped.id}")
            ScopedSkillRef(
                id = scoped.id,
                name = scoped.name,
                description = scoped.description,
                skillDir = skillDir,
                isPublic = scoped.isPublic,
                version = scoped.version
            )
        }
        val toolsFile = ScriptScopeToolsFile(
            scopeId = snapshot.scopeId,
            scriptUid = snapshot.scriptUid,
            tools = snapshot.tools
        )
        val skillsFile = ScriptScopeSkillsFile(
            scopeId = snapshot.scopeId,
            scriptUid = snapshot.scriptUid,
            skills = skillsRefs
        )
        getToolsFile(scriptDir).writeText(GsonHelper.getInstance().toJson(toolsFile))
        getSkillsFile(scriptDir).writeText(GsonHelper.getInstance().toJson(skillsFile))
        val scriptsFile = ScriptScopeScriptsFile(
            scopeId = snapshot.scopeId,
            scriptUid = snapshot.scriptUid,
            scripts = snapshot.scripts
        )
        getScriptsFile(scriptDir).writeText(GsonHelper.getInstance().toJson(scriptsFile))
    }

    fun validate(scriptDir: File, snapshot: ScriptScopeSnapshot) {
        val errors = mutableListOf<String>()
        if (snapshot.scopeId.isBlank()) errors.add("scopeId is blank")
        if (snapshot.scriptUid.isBlank()) errors.add("scriptUid is blank")
        if (snapshot.scopeId != scopeId(snapshot.scriptUid)) {
            errors.add("scopeId must equal scriptUid")
        }

        val toolNames = LinkedHashSet<String>()
        val toolLocalIds = LinkedHashSet<String>()
        snapshot.tools.forEach { tool ->
            if (tool.toolUid.isBlank()) errors.add("toolUid is blank")
            if (tool.localId.isBlank()) errors.add("tool.localId is blank: ${tool.toolUid}")
            if (tool.functionName.isBlank()) errors.add("tool.functionName is blank: ${tool.toolUid}")
            if (tool.scriptDir.isBlank()) errors.add("tool.scriptDir is blank: ${tool.toolUid}")
            if (!toolNames.add(tool.functionName)) errors.add("duplicate tool functionName: ${tool.functionName}")
            if (!toolLocalIds.add(tool.localId)) errors.add("duplicate tool localId: ${tool.localId}")
            val expectedFn = localToolFunctionName(tool.toolUid)
            if (tool.functionName != expectedFn) {
                errors.add("invalid tool functionName: ${tool.functionName}, expected: $expectedFn")
            }
            runCatching {
                val toolsRoot = getToolsDir(scriptDir).canonicalFile
                val resolved = File(toolsRoot, tool.scriptDir).canonicalFile
                if (!resolved.path.startsWith(toolsRoot.path + File.separator) && resolved != toolsRoot) {
                    errors.add("tool scriptDir escapes dependence/tools: ${tool.scriptDir}")
                }
            }.onFailure {
                errors.add("tool scriptDir invalid: ${tool.scriptDir}")
            }
        }

        val skillIds = LinkedHashSet<String>()
        snapshot.skills.forEach { skill ->
            if (skill.id.isBlank()) errors.add("skill.id is blank")
            if (skill.name.isBlank()) errors.add("skill.name is blank: ${skill.id}")
            if (skill.description.isBlank()) errors.add("skill.description is blank: ${skill.id}")
            if (skill.systemPrompt.isBlank()) errors.add("skill.systemPrompt is blank: ${skill.id}")
            if (!skillIds.add(skill.id)) errors.add("duplicate skillId: ${skill.id}")
            // skill.id 使用 local 形式，不再要求 skill.scope. 前缀
            if (skill.id.startsWith("skill.scope.")) {
                errors.add("skill id should use local format, not scoped: ${skill.id}")
            }
            // 仅校验 scope 内 tool 引用；全局 tool（如 buildin.dialog、buildin.skill）运行时可用，不在此校验
            val invalidAllowed = skill.allowedToolNames.filter { toolName ->
                if (toolName in toolNames) return@filter false
                toolName.startsWith(ScriptConst.SCRIPT_TOOL_ID_PREFIX)  // 引用 scope 内 custom tool 但不在 toolNames 中 -> 非法
            }
            if (invalidAllowed.isNotEmpty()) {
                errors.add("skill ${skill.id} references unknown tools: ${invalidAllowed.joinToString()}")
            }
        }

        val scriptLocalIds = LinkedHashSet<String>()
        snapshot.scripts.forEach { ref ->
            if (ref.scriptUid.isBlank()) errors.add("workflow dependency scriptUid is blank")
            if (ref.localId.isBlank()) errors.add("workflow dependency localId is blank: ${ref.scriptUid}")
            if (ref.scriptDir.isBlank()) errors.add("workflow dependency scriptDir is blank: ${ref.scriptUid}")
            if (!scriptLocalIds.add(ref.localId)) errors.add("duplicate workflow dependency localId: ${ref.localId}")
        }

        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(errors.joinToString("; "))
        }
    }
}
