// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.skill

import com.hive.plugin.agent.model.SkillSpec
import com.hive.script.net.data.ScriptCustomSkill
import com.hive.script.scope.CustomStorage
import com.hive.script.scope.SkillFileHelper
import com.hive.utils.debug.DLog

/**
 * Custom/public skill 持久化。
 * 方案 A：SP 仅存基本信息（ScriptCustomSkill 路径引用），具体内容在 .skill 文件中。
 * Private skill 仍内联在 workflow 的 dependence/skills.json。
 */
object SkillPersistence {

    private const val TAG = "SkillPersistence"

    fun loadCustomSkills(): List<SkillSpec> {
        val refs = readSkillRefs()
        return refs.mapNotNull { ref ->
            SkillFileHelper.readSkillFile(ref.skillPath)
        }.filter { it.id.isNotBlank() }.distinctBy { it.id }
    }

    fun addOrUpdateSkill(spec: SkillSpec) {
        if (spec.id.isBlank()) return
        val path = SkillFileHelper.writeSkillFile(spec)
        if (path == null) {
            DLog.e(TAG, "addOrUpdateSkill failed to write file: ${spec.id}")
            return
        }
        val refs = readSkillRefs().toMutableList()
        refs.removeAll { it.skillId == spec.id }
        refs.add(ScriptCustomSkill(skillId = spec.id, skillName = spec.name, skillPath = path))
        saveSkillRefs(refs)
    }

    fun removeSkill(skillId: String) {
        if (skillId.isBlank()) return
        val refs = readSkillRefs().toMutableList()
        refs.find { it.skillId == skillId }?.let { SkillFileHelper.deleteSkillFile(it.skillPath) }
        refs.removeAll { it.skillId == skillId }
        saveSkillRefs(refs)
        // 同时删除本地 skill 目录，避免持久化引用残留
        SkillFileHelper.getSkillPackageDir(skillId).let { dir ->
            if (dir.exists()) dir.deleteRecursively()
        }
    }

    /** 供 ScriptScopeBundleV2Exporter、GlobalScriptRegistry 等读取 custom skill 引用列表 */
    fun loadCustomSkillRefs(): List<ScriptCustomSkill> {
        return readSkillRefs()
    }

    /** 供 ScriptScopeBundleV2Installer 等写入 custom skill 引用（同时写入 .skill 文件由调用方负责） */
    fun addOrUpdateSkillRef(ref: ScriptCustomSkill) {
        if (ref.skillId.isBlank() || ref.skillPath.isBlank()) return
        val refs = readSkillRefs().toMutableList()
        refs.removeAll { it.skillId == ref.skillId }
        refs.add(ref)
        saveSkillRefs(refs)
    }

    private fun readSkillRefs(): List<ScriptCustomSkill> {
        return CustomStorage.readCustomSkillRefs()
    }

    private fun saveSkillRefs(refs: List<ScriptCustomSkill>) {
        CustomStorage.saveCustomSkillRefs(refs)
    }
}
