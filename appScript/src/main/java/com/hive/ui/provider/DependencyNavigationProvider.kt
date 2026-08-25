// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.provider

import android.content.Context
import com.hive.plugin.agent.model.SkillSpec
import com.hive.plugin.provider.IDependencyNavigationProvider
import com.hive.script.net.data.ScriptCustomMcpTool
import com.hive.ui.mcp.ActivityMcpToolDetail
import com.hive.ui.skill.ActivitySkillDetail

class DependencyNavigationProvider : IDependencyNavigationProvider {

    override fun init(context: Context) = Unit

    override fun openSkillDetail(context: Context, skillSpec: SkillSpec) {
        ActivitySkillDetail.start(context, skillSpec)
    }

    override fun openToolDetail(
        context: Context,
        toolId: String,
        toolDisplayName: String,
        toolDescription: String,
        toolType: String,
        toolSchema: String,
        customScriptPath: String?
    ) {
        val customTool = customScriptPath?.takeIf { it.isNotBlank() }?.let {
            ScriptCustomMcpTool(
                scriptId = toolId,
                scriptName = toolDisplayName,
                scriptDesc = toolDescription,
                scriptPath = it
            )
        }
        ActivityMcpToolDetail.start(
            context = context,
            toolName = toolId,
            toolDisplayName = toolDisplayName,
            toolDescription = toolDescription,
            toolType = toolType,
            toolSchema = toolSchema,
            customTool = customTool
        )
    }
}
