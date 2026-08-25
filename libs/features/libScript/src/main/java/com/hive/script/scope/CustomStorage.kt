// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.scope

import com.hive.plugin.agent.AgentGlobalConfig
import com.hive.script.net.data.ScriptCustomMcpTool
import com.hive.script.net.data.ScriptCustomSkill
import com.hive.utils.GlobalApp
import com.hive.utils.global.SPTools
import com.hive.utils.utils.GsonHelper

/**
 * 统一管理 SP 中的 custom tools 和 custom skills 读写。
 * 供 Exporter/Installer/Registry 等复用，避免重复实现。
 */
object CustomStorage {

    private const val CUSTOM_TOOLS_KEY = "tools"
    private const val CUSTOM_SKILLS_SP_NAME = "agent_custom_skills"
    private const val CUSTOM_SKILL_REFS_KEY = "skill_refs"

    fun readCustomTools(): List<ScriptCustomMcpTool> {
        val sp = SPTools(GlobalApp.getContext(), AgentGlobalConfig.CUSTOM_TOOLS_SP_NAME)
        val json = sp.getString(CUSTOM_TOOLS_KEY, "[]") ?: "[]"
        return runCatching {
            GsonHelper.getInstance().fromListJson(json, ScriptCustomMcpTool::class.java)
                ?.distinctBy { it.scriptId }
                ?: emptyList()
        }.getOrElse { emptyList() }
    }

    fun saveCustomTools(tools: List<ScriptCustomMcpTool>) {
        val sp = SPTools(GlobalApp.getContext(), AgentGlobalConfig.CUSTOM_TOOLS_SP_NAME)
        sp.putString(CUSTOM_TOOLS_KEY, GsonHelper.getInstance().toJson(tools))
    }

    fun readCustomSkillRefs(): List<ScriptCustomSkill> {
        val sp = SPTools(GlobalApp.getContext(), CUSTOM_SKILLS_SP_NAME)
        val json = sp.getString(CUSTOM_SKILL_REFS_KEY, "[]") ?: "[]"
        return runCatching {
            GsonHelper.getInstance().fromListJson(json, ScriptCustomSkill::class.java)
                ?.filter { it.skillId.isNotBlank() && it.skillPath.isNotBlank() }
                ?.distinctBy { it.skillId }
                ?: emptyList()
        }.getOrElse { emptyList() }
    }

    fun saveCustomSkillRefs(refs: List<ScriptCustomSkill>) {
        val sp = SPTools(GlobalApp.getContext(), CUSTOM_SKILLS_SP_NAME)
        val safe = refs.filter { it.skillId.isNotBlank() && it.skillPath.isNotBlank() }.distinctBy { it.skillId }
        sp.putString(CUSTOM_SKILL_REFS_KEY, GsonHelper.getInstance().toJson(safe))
    }
}
