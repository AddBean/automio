// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.scope

import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.model.SkillSpec
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.base.ScriptConst
import com.hive.script.utils.bundle.WorkflowBundleManifest
import com.hive.utils.GlobalApp
import com.hive.utils.utils.GsonHelper
import java.io.File

/**
 * 纯计算：根据 authorId、isPublic 实时判断哪些 custom tool/skill 对主 Agent 可见。
 * 无持久化，无合并逻辑。
 *
 * 规则：
 * - 市场显式下载（custom tool、无 sources 的 skill）→ public
 * - workflow 依赖：作者本机 → public；非作者 → 看 spec.isPublic
 * - 本地创建（无 manifest）→ public
 */
object ScriptVisibilityRegistry {

    /** 获取当前 public 的 custom tool id 集合（实时计算） */
    fun getPublicToolIds(): Set<String> {
        val result = mutableSetOf<String>()

        // 1) Custom tools（Save_Tool_Path）→ 市场显式下载，全部 public
        CustomStorage.readCustomTools().forEach { result.add(it.scriptId) }

        // 2) Workflow 依赖的 tools
        LocalResourceListRepository.listWorkflows().forEach { workflowDir ->
            val snapshot = runCatching { ScriptScopeRepository.load(workflowDir, validate = false) }.getOrNull() ?: return@forEach
            val isAuthor = computeIsAuthor(workflowDir)
            snapshot.tools.forEach { spec ->
                if (isAuthor || spec.isPublic) result.add(spec.functionName)
            }
        }
        return result
    }

    /** 获取当前 public 的 skill id 集合（实时计算） */
    fun getPublicSkillIds(): Set<String> {
        val result = mutableSetOf<String>()
        val provider = ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider ?: return result

        val skills = runCatching { provider.listSkills() }.getOrDefault(emptyList())
        skills.forEach { spec ->
            if (isSkillPublic(spec)) result.add(spec.id)
        }
        return result
    }

    private fun isSkillPublic(spec: SkillSpec): Boolean {
        val sources = spec.sources.orEmpty()
        if (sources.isEmpty()) return true // 无 sources = 本地/custom skill → public

        sources.forEach { scriptUid ->
            val workflowDir = findWorkflowDirByScriptUid(scriptUid) ?: return true
            val snapshot = runCatching { ScriptScopeRepository.load(workflowDir, validate = false) }.getOrNull() ?: return@forEach
            val skillSpec = snapshot.skills.firstOrNull { it.id == spec.id } ?: return@forEach
            val isAuthor = computeIsAuthor(workflowDir)
            if (isAuthor || skillSpec.isPublic) return true
        }
        return false
    }

    private fun computeIsAuthor(workflowDir: File): Boolean {
        val manifestFile = File(workflowDir, WorkflowBundleManifest.DEFAULT_MANIFEST_NAME)
        if (!manifestFile.exists() || !manifestFile.isFile) return true // 无 manifest = 本地创建 → 作者
        val manifest = runCatching {
            GsonHelper.getInstance().fromJson(manifestFile.readText(), WorkflowBundleManifest::class.java)
        }.getOrNull() ?: return true
        return manifest.authorId.isNullOrBlank()
    }

    private fun findWorkflowDirByScriptUid(scriptUid: String): File? {
        return ScriptScopeRepository.findLocalScriptDirsByUid(scriptUid).firstOrNull()
    }
}
