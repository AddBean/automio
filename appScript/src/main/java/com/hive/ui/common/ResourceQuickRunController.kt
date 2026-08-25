// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.common

import android.content.Context
import android.text.InputType
import com.google.gson.JsonObject
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.model.RunSkillRequest
import com.hive.plugin.agent.model.SkillResult
import com.hive.plugin.agent.model.SkillSpec
import com.hive.plugin.mcp.model.McpTool
import com.hive.plugin.provider.IAgentProvider
import com.hive.plugin.provider.IMcpProvider
import com.hive.plugin.provider.IScriptProvider
import com.hive.script.ScriptProvider
import com.hive.script.driver.ServiceAccessibility
import com.hive.script.net.data.ScriptCustomMcpTool
import com.hive.script.scope.PackageRuntimeResolver
import com.hive.script.utils.ScriptHelper
import com.hive.script.utils.ScriptPermissionManager
import com.hive.script.views.dialog.DialogCmdDialogInput
import com.hive.script.views.dialog.DialogInputMessage
import com.hive.script.views.dialog.DialogPermissionAggregate
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.utils.system.CommonUtils
import com.hive.views.widgets.CommonToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ResourceQuickRunController {

    data class ToolRunTarget(
        val toolName: String,
        val displayName: String,
        val inputSchema: JsonObject,
        val customTool: ScriptCustomMcpTool?,
        val tool: McpTool? = null
    )

    fun runSkill(context: Context, skill: SkillSpec) {
        ResourceRunStateStore.ensureRegistered()
        if (ResourceRunStateStore.isSkillRunning(skill.id)) {
            ResourceRunStateStore.stopSkill(skill.id)
            return
        }
        // 预检测基础权限（无障碍服务）
        if (!checkBasicPermissions(context)) {
            return
        }
        checkSkillPermissionAndRights(skill) {
            DialogInputMessage(
                context,
                context.getString(com.hive.i8n.R.string.skill_run_title, skill.name),
                context.getString(com.hive.i8n.R.string.skill_run_prompt_hint),
                inputType = InputType.TYPE_CLASS_TEXT,
                checkInputFun = null
            ) { _, input ->
                executeSkill(context, skill, input)
            }.setMultiLine().show()
        }
    }

    fun runTool(context: Context, target: ToolRunTarget) {
        ResourceRunStateStore.ensureRegistered()
        if (ResourceRunStateStore.isToolRunning(target.toolName)) {
            ResourceRunStateStore.stopTool(target.toolName)
            return
        }
        // 预检测基础权限（无障碍服务）
        if (!checkBasicPermissions(context)) {
            return
        }
        val scriptPath = target.customTool?.scriptPath
        if (!scriptPath.isNullOrBlank()) {
            runCustomToolScript(target.toolName, scriptPath)
            return
        }
        val tool = target.tool ?: findRegisteredTool(target.toolName)
        if (tool == null) {
            CommonToast.show(com.hive.i8n.R.string.mcp_tool_run_service_unavailable)
            return
        }
        val inputItems = buildToolInputItems(target.inputSchema)
        if (inputItems.isEmpty()) {
            executeMcpTool(context, tool, JsonObject())
            return
        }
        DialogCmdDialogInput(context)
            .setTitle(context.getString(com.hive.i8n.R.string.mcp_tool_run_title, target.displayName))
            .setInputItems(inputItems)
            .setInputListener(object : DialogCmdDialogInput.OnInputListener {
                override fun onConfirmed(
                    dialog: DialogCmdDialogInput,
                    inputs: List<DialogCmdDialogInput.InputItem>
                ) {
                    executeMcpTool(context, tool, buildArguments(target.inputSchema, inputs))
                }

                override fun onCancel() = Unit
            })
            .show()
    }

    fun buildToolTarget(tool: McpTool, customTool: ScriptCustomMcpTool?): ToolRunTarget {
        return ToolRunTarget(
            toolName = tool.name,
            displayName = tool.extraName.ifBlank { tool.name },
            inputSchema = tool.inputSchema,
            customTool = customTool,
            tool = tool
        )
    }

    fun buildToolTarget(
        toolName: String,
        displayName: String,
        schema: JsonObject,
        customTool: ScriptCustomMcpTool?
    ): ToolRunTarget {
        return ToolRunTarget(
            toolName = toolName,
            displayName = displayName.ifBlank { toolName },
            inputSchema = schema,
            customTool = customTool,
            tool = findRegisteredTool(toolName)
        )
    }

    private fun executeSkill(context: Context, skill: SkillSpec, input: String) {
        val provider = ComponentManager.getInstance()
            .getProvider(IAgentProvider::class.java) as? IAgentProvider
        if (provider == null) {
            CommonToast.show(com.hive.i8n.R.string.cmd_run_skill_no_provider)
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val result = provider.runSkillSync(
                    RunSkillRequest(skillId = skill.id, userPrompt = input)
                )
                withContext(Dispatchers.Main) {
                    showSkillResult(context, result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    CommonToast.show(e.message ?: context.getString(com.hive.i8n.R.string.agent_skill_error_execute))
                }
            }
        }
    }

    private fun runCustomToolScript(toolName: String, scriptPath: String) {
        val provider = ComponentManager.getInstance()
            .getProvider(IScriptProvider::class.java) as? IScriptProvider
        if (provider == null) {
            CommonToast.show(com.hive.i8n.R.string.mcp_tool_run_service_unavailable)
            return
        }
        provider.checkPermissionAndRights(scriptPath) {
            ResourceRunStateStore.rememberCustomToolScript(toolName, scriptPath)
            provider.executeScript(scriptPath, false)
        }
    }

    private fun executeMcpTool(context: Context, tool: McpTool, args: JsonObject) {
        var runningJob: Job? = null
        val job = GlobalScope.launch(Dispatchers.IO) {
            try {
                val result = tool.handler(args)
                withContext(Dispatchers.Main) {
                    CommonToast.show(
                        result.message ?: context.getString(
                            if (result.success) com.hive.i8n.R.string.agent_tool_execute_success
                            else com.hive.i8n.R.string.agent_tool_execute_failed
                        )
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    CommonToast.show(e.message ?: context.getString(com.hive.i8n.R.string.agent_tool_execute_failed))
                }
            } finally {
                ResourceRunStateStore.finishToolJob(tool.name, runningJob)
            }
        }
        runningJob = job
        ResourceRunStateStore.startToolJob(tool.name, job)
    }

    private fun showSkillResult(context: Context, result: SkillResult) {
        when (result.status) {
            SkillResult.STATUS_SUCCESS -> {
                CommonToast.show(com.hive.i8n.R.string.skill_run_status_success)
            }

            SkillResult.STATUS_PARTIAL -> {
                val msg = result.summary + result.toolErrors
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { errs ->
                        "\n[${context.getString(com.hive.i8n.R.string.skill_run_tool_errors)}: ${
                            errs.joinToString("；") { it.toolName + ": " + it.errorMessage }
                        }]"
                    }
                    .orEmpty()
                CommonToast.show(msg)
            }

            else -> {
                CommonToast.show(result.message ?: result.summary)
            }
        }
    }

    private fun checkSkillPermissionAndRights(skill: SkillSpec, permissionCallback: () -> Unit) {
        val permissions = getRequiredPermissionsMerged(skill)
        if (permissions.isEmpty()) {
            permissionCallback.invoke()
            return
        }
        val missed = ScriptPermissionManager.checkMissedPermissions(permissions)
        if (missed.isEmpty()) {
            permissionCallback.invoke()
            return
        }
        DialogPermissionAggregate(ScriptProvider.getViewContext(), missed).show()
    }

    private fun getRequiredPermissionsMerged(skill: SkillSpec): List<String> {
        val permissionKeys = ScriptHelper.mPermissionMap.keys
        val runtimePackage = PackageRuntimeResolver.resolveByPrimarySkillId(skill.id) ?: return emptyList()
        val fromManifest = runtimePackage.manifest?.permissions
            ?.filter { it in permissionKeys }
            ?.distinct()
            ?: emptyList()
        if (fromManifest.isEmpty()) return emptyList()
        return if (skill.allowedToolNames.isEmpty()) {
            fromManifest
        } else {
            (fromManifest + ScriptHelper.PERMISSION_BIND_ACCESSIBILITY_SERVICE).distinct()
        }
    }

    private fun buildToolInputItems(schema: JsonObject): List<DialogCmdDialogInput.InputItem> {
        val properties = schema.getAsJsonObject("properties") ?: return emptyList()
        val required = schema.getAsJsonArray("required")?.mapNotNull {
            runCatching { it.asString }.getOrNull()
        }?.toSet() ?: emptySet()
        return properties.entrySet()
            .filter { it.key in required }
            .map { entry ->
                val itemSchema = entry.value.takeIf { it.isJsonObject }?.asJsonObject
                val type = itemSchema?.get("type")?.asString ?: "string"
                val desc = itemSchema?.get("description")?.asString.orEmpty()
                DialogCmdDialogInput.InputItem(
                    id = entry.key,
                    label = entry.key,
                    hint = desc.ifBlank { entry.key },
                    required = true,
                    inputType = when (type) {
                        "boolean" -> DialogCmdDialogInput.TYPE_SWITCH
                        "integer", "number" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                        else -> InputType.TYPE_CLASS_TEXT
                    }
                )
            }
    }

    private fun buildArguments(
        schema: JsonObject,
        inputs: List<DialogCmdDialogInput.InputItem>
    ): JsonObject {
        val properties = schema.getAsJsonObject("properties")
        return JsonObject().apply {
            inputs.forEach { input ->
                val id = input.id ?: return@forEach
                val type = properties?.getAsJsonObject(id)?.get("type")?.asString ?: "string"
                when (type) {
                    "boolean" -> addProperty(id, input.value.toBooleanStrictOrNull() ?: false)
                    "integer" -> addProperty(id, input.value.toIntOrNull() ?: 0)
                    "number" -> addProperty(id, input.value.toDoubleOrNull() ?: 0.0)
                    else -> addProperty(id, input.value)
                }
            }
        }
    }

    private fun findRegisteredTool(toolName: String): McpTool? {
        val provider = ComponentManager.getInstance()
            .getProvider(IMcpProvider::class.java) as? IMcpProvider
        return provider?.getRegisteredTools()?.firstOrNull { it.name == toolName }
    }

    /**
     * 预检测基础权限（无障碍服务等）
     * @return true 表示权限已授予，false 表示权限缺失已提示用户
     */
    private fun checkBasicPermissions(context: Context): Boolean {
        // 检查无障碍服务是否开启
        val isAccessibilityOn = CommonUtils.isAccessibilitySettingsOn(
            context,
            ServiceAccessibility::class.java.name
        )
        if (!isAccessibilityOn) {
            DialogScriptAlert(context)
                .setTitle(com.hive.i8n.R.string.sc_need_acc_title)
                .setContent(context.getString(com.hive.i8n.R.string.sc_need_acc_content))
                .setContentGravity(android.view.Gravity.LEFT)
                .setConfirmText(com.hive.i8n.R.string.sc_need_acc_confirm)
                .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                    override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                        dialog.dismiss()
                        if (!isCancel) {
                            ScriptProvider.startToAccessibilitySetting()
                        }
                    }
                })
                .show()
            return false
        }

        // 检查服务是否就绪
        if (!ScriptProvider.isServiceReady()) {
            CommonToast.show(com.hive.i8n.R.string.mcp_tool_run_service_unavailable)
            return false
        }

        return true
    }
}
