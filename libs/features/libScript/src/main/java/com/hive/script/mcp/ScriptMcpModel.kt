// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.extensions.executeSync

abstract class McpToolBuilder {
    @Volatile
    private var lastExecutedCommand: ScriptCommand? = null

    open fun getLastExecutedCommand(): ScriptCommand? = lastExecutedCommand

    open fun matchAction(actionName: String): Boolean {
        return getAction()?.action?.equals(actionName, ignoreCase = true) ?: false
    }

    open fun getActionName(): String? {
        return getAction()?.action
    }

    open fun getToolType(): McpToolType {
        return McpToolType.SCRIPT_COMMAND
    }

    fun getToolAction(): McpAction? {
        return getAction().apply {
            this?.paramInfo?.add(
                    McpActionParameters(
                        name = "description",
                        type = "string",
                        description = com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.mcp_tool_param_desc),
                        required = true,
                        examples = listOf(
                            com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.mcp_tool_param_example1),
                            com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.mcp_tool_param_example2),
                        ),
                    )
            )
            if (supportDelay()) {
                this?.paramInfo?.add(
                    McpActionParameters(
                        name = "delay",
                        type = "number",
                        description = com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.mcp_tool_param_delay_desc),
                        required = true,
                        examples = listOf("500", "1000"),
                    )
                )
            }
        }
    }

    open fun supportDelay(): Boolean = true

    protected abstract fun getAction(): McpAction?

    fun checkAction(action: McpAction): CheckActionResult {
        return onCheckAction(action)
    }

    fun checkPermission(action: McpAction): CheckActionResult {
        if (!ScriptProvider.isServiceReady()) {
            return CheckActionResult(
                false,
                com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.mcp_tool_accessibility_required),
                null
            )
        }
        return onCheckPermission(action)
    }

    /**
     * 执行工具：创建命令 + 执行命令的完整流程
     */
    open suspend fun executeAction(params: Map<String, String>): ActionResult {
        ScriptThreadManager.delay(ScriptConst.Cmd_Delay_Default)
        val cmd = buildCommand(params)
        return executeCommand(cmd)
    }

    /**
     * 安全构建命令对象（不执行）。
     * 可直接调用，用于从历史记录提取命令。
     */
    open fun buildCommand(params: Map<String, String>): ScriptCommand? {
        val created = onCreateCommand(params)
        val cmd = created ?: getCommand()
        if (cmd != null) {
            onCommandPrepare(cmd, params)
        }
        return cmd
    }

    /**
     * 创建命令对象（子类必须实现）。
     * 只负责根据参数创建对应的命令对象，不设置注释/延迟，不执行。
     */
    abstract fun onCreateCommand(params: Map<String, String>): ScriptCommand?

    /**
     * 执行已构建的命令。
     */
    open fun executeCommand(cmd: ScriptCommand?): ActionResult {
        if (cmd == null) {
            return ActionResult.failure(message = "Command not created")
        }
        lastExecutedCommand = cmd
        val result = cmd.executeSync()
        onCommandExecuted(cmd, result)
        return if (result.success) {
            ActionResult.success(message = result.message, data = result.data)
        } else {
            ActionResult.failure(message = result.message, data = result.data)
        }
    }

    abstract fun getCommand(): ScriptCommand?

    open fun requiredModelVision() = false

    open fun withScreenLayout() = true

    open fun withScreenShot() = withScreenLayout()

    open fun onCheckPermission(action: McpAction): CheckActionResult {
        return CheckActionResult(true, null, null)
    }

    open fun onCheckAction(action: McpAction): CheckActionResult {
        return CheckActionResult(true, null, null)
    }

    /**
     * 准备命令：设置注释、延迟等元数据（在构建时调用）。
     */
    protected fun onCommandPrepare(cmd: ScriptCommand?, params: Map<String, String>) {
        params["description"]?.let {
            cmd?.comment = it
        }
        if (supportDelay()) {
            val delay = params["delay"].toLongCompat(100L)
            cmd?.startDelay = delay
            cmd?.endDelay = delay
        }
    }

    /**
     * 命令执行后的回调（可用于清理、统计等）。
     */
    protected fun onCommandExecuted(cmd: ScriptCommand?, result: ScriptCommand.CmdExecuteResult?) {
        // 默认空实现，子类可覆盖
    }

}

data class CheckActionResult(
    val success: Boolean = false,
    val message: String? = null,
    val data: Any? = null
)

enum class McpToolType { SCRIPT_COMMAND }
