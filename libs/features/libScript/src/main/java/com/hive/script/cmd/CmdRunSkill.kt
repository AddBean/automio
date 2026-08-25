// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.ExecutionContextFrame
import com.hive.plugin.agent.ExecutionContextType
import com.hive.plugin.agent.ExecutionContexts
import com.hive.plugin.agent.model.RunSkillRequest
import com.hive.plugin.agent.model.SkillResult
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.scope.PackageRuntimeResolver
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.utils.StringUtils
import com.hive.views.widgets.CommonToast
import java.io.File

/**
 * 运行技能命令：通过 IAgentProvider.runSkillSync 执行指定技能。
 */
@AutoCmdRegister(type = IDS.CmdRunSkill, name = "runSkill")
class CmdRunSkill : ScriptCommand(), ScriptRegularInterface {

    var skillId: String? = null
    var skillName: String? = null
    var userPrompt: String? = null

    override fun onExecute(): CmdExecuteResult {
        val ctxId = "script-runSkill-${System.currentTimeMillis()}"
        val ctxName = "runSkill(${skillId ?: ""})"
        val scopeId = getRootScript()?.scriptMate?.scriptUid?.takeIf { it.isNotBlank() }
        ExecutionContexts.stack.push(
            ExecutionContextFrame(
                type = ExecutionContextType.SCRIPT,
                id = ctxId,
                name = ctxName,
                rootTaskId = ctxId,
                scopeId = scopeId
            )
        )
        try {
        ScriptThreadManager.delay(getCommandDuration())
        val sid = resolveSkillId()
        if (sid.isNullOrEmpty()) {
            CommonToast.show(com.hive.i8n.R.string.cmd_run_skill_empty)
            return CmdExecuteResult.failure(GlobalApp.getString(com.hive.i8n.R.string.cmd_run_skill_empty))
        }
        val provider = ComponentManager.getInstance()
            .getProvider(IAgentProvider::class.java) as? IAgentProvider
        if (provider == null) {
            val msg = GlobalApp.getString(com.hive.i8n.R.string.cmd_run_skill_no_provider)
            CommonToast.show(msg)
            return CmdExecuteResult.failure(msg)
        }
        val request = RunSkillRequest(
            skillId = sid,
            userPrompt = parseParamText(userPrompt?.takeIf { it.isNotBlank() } ?: "") ?: "unkown"
        )
        val result: SkillResult = provider.runSkillSync(request)
        return when (result.status) {
            SkillResult.STATUS_SUCCESS, SkillResult.STATUS_PARTIAL -> {
                val toolErrInfo = result.toolErrors?.takeIf { it.isNotEmpty() }?.let { errs ->
                    "\n[工具错误: ${errs.joinToString("; ") { "${it.toolName}: ${it.errorMessage}" }}]"
                }.orEmpty()
                CmdExecuteResult.success(data = result.summary + toolErrInfo)
            }
            else -> {
                // 402 余额不足等错误已通过 EventBus 统一处理，这里只需返回失败结果
                CommonToast.show(result.message ?: result.summary)
                CmdExecuteResult.failure(result.message ?: result.summary)
            }
        }
        } finally {
            ExecutionContexts.stack.pop(expectedId = ctxId)
        }
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    private fun resolveSkillId(): String? {
        val raw = skillId?.takeIf { it.isNotEmpty() } ?: return null
        val resolved = PackageRuntimeResolver.resolveSkillForScript(
            currentScriptDir = File(getScriptBasePath()),
            rawSkillId = raw
        )
        return resolved.localSkillId ?: raw
    }

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_run_skill)

    override fun getCommandDescribe() = GlobalApp.getString(
        com.hive.i8n.R.string.cmd_name_run_skill_des,
        skillName ?: skillId ?: ""
    )

    override fun getCommandIcon() = R.drawable.sc_cmd_run_skill

    override fun getCommand(): String {
        return "${cmdPrefix()} skillId=\"${skillId?.encode() ?: ""}\" name=\"${skillName?.encode() ?: ""}\" prompt=\"${userPrompt?.encode() ?: ""}\""
    }

    override fun parseCmd(cmd: String) {
        val kv = ScriptLineTokenizer.parseKeyValueParams(cmd)
        skillId = kv["skillId"]?.decode()?.takeIf { it.isNotEmpty() }
        skillName = kv["name"]?.decode()?.takeIf { it.isNotEmpty() }
        userPrompt = kv["prompt"]?.decode()?.takeIf { it.isNotEmpty() }
    }

    companion object {
        fun createCommand(
            skillId: String,
            skillName: String,
            userPrompt: String? = null
        ) = CmdRunSkill().apply {
            this.skillId = skillId
            this.skillName = skillName
            this.userPrompt = userPrompt?.takeIf { it.isNotBlank() }
        }
    }
}
