// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdDialog
import com.hive.script.cmd.CmdDialogUserInput
import com.hive.script.cmd.CmdDialogUserSelector
import com.hive.script.cmd.CmdWaitForUser
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.script.mcp.toIntOrNullCompat
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolDialog)
class ScriptToolBuilder_CmdDialog : McpToolBuilder() {

    private var cmd: ScriptCommand? = null

    override fun matchAction(actionName: String): Boolean {
        return "dialog" == actionName
    }

    override fun getAction(): McpAction =
        McpAction(
            action = "dialog",
            extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_name),
            description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_description),
            paramInfo =
            mutableListOf(
                McpActionParameters(
                    name = "type",
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_type_desc),
                    required = false,
                    examples = listOf("confirm", "input", "select", "wait"),
                ),
                McpActionParameters(
                    name = "title",
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_title_desc),
                    required = true,
                    examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_title_examples)),
                ),
                McpActionParameters(
                    name = "message",
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_message_desc),
                    required = false,
                    examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_message_examples)),
                ),
                McpActionParameters(
                    name = "image",
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_image_desc),
                    required = false,
                    examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_image_examples)),
                ),
                McpActionParameters(
                    name = "cancelBtn",
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_cancel_btn_desc),
                    required = false,
                    examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_cancel_btn_examples)),
                ),
                McpActionParameters(
                    name = "submitBtn",
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_submit_btn_desc),
                    required = false,
                    examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_submit_btn_examples)),
                ),
                McpActionParameters(
                    name = "countdown",
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_countdown_desc),
                    required = true,
                    examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_countdown_examples)),
                )
                ,
                // input
                McpActionParameters(
                    name = "inputs",
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_input_inputs_desc),
                    required = false,
                    examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_input_inputs_examples)),
                ),
                McpActionParameters(
                    name = "hints",
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_input_hints_desc),
                    required = false,
                    examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_input_hints_examples)),
                ),
                McpActionParameters(
                    name = "requires",
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_input_requires_desc),
                    required = false,
                    examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_input_requires_examples)),
                ),
                McpActionParameters(
                    name = "defaults",
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_input_defaults_desc),
                    required = false,
                    examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_input_defaults_examples)),
                ),
                // select
                McpActionParameters(
                    name = "items",
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_selector_items_desc),
                    required = false,
                    examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_selector_items_examples)),
                ),
                McpActionParameters(
                    name = "multiSelect",
                    type = "boolean",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_selector_multi_select_desc),
                    required = false,
                    examples = GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_selector_multi_select_examples).split(", "),
                    format = "boolean",
                ),
            ),
            paramValues = emptyMap(),
        )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        if (action.paramValues.isEmpty()) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_error_params_empty))
        }
        if (action.paramValues["title"].isNullOrBlank()) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_error_title_empty))
        }
        if (action.paramValues["countdown"].isNullOrBlank()) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_error_countdown_empty))
        }
        if (action.paramValues["countdown"]?.toIntOrNullCompat() == null) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_error_countdown_invalid))
        }

        val type = (action.paramValues["type"] ?: "confirm").trim()
        if (type !in listOf("confirm", "input", "select", "wait")) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_error_invalid_type))
        }

        when (type) {
            "confirm", "wait" -> {
                if (action.paramValues["message"].isNullOrBlank()) {
                    return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_error_message_required))
                }
            }

            "input" -> {
                if (action.paramValues["inputs"].isNullOrBlank()) {
                    return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_error_inputs_required))
                }
            }

            "select" -> {
                if (action.paramValues["items"].isNullOrBlank()) {
                    return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_error_items_required))
                }
                if (action.paramValues["multiSelect"].isNullOrBlank()) {
                    return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_error_multi_select_required))
                }
            }
        }

        return CheckActionResult(true, null)
    }


    override fun getCommand(): ScriptCommand? {
        return cmd
    }

    override fun supportDelay() = false

    override fun withScreenLayout() = false

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val type = (params["type"] ?: "confirm").trim()
        val title = params["title"]
        val message = params["message"]
        val image = params["image"]
        val cancelBtn = params["cancelBtn"]
        val submitBtn = params["submitBtn"]
        val countdown = params["countdown"]
        val countDownInt = countdown?.toIntOrNullCompat() ?: 15

        cmd = when (type) {
            "wait" -> {
                CmdWaitForUser.createCommand(
                    title,
                    message,
                    submitBtn,
                    cancelBtn,
                    countDownInt
                )
            }

            "input" -> {
                CmdDialogUserInput.createCommand(
                    title,
                    params["inputs"],
                    params["hints"],
                    params["requires"],
                    params["defaults"],
                    countDownInt
                )
            }

            "select" -> {
                val multiSelect = params["multiSelect"]?.lowercase() in listOf("true", "1", "yes")
                CmdDialogUserSelector.createCommand(
                    title,
                    params["items"],
                    multiSelect,
                    countDownInt
                )
            }

            else -> {
                // confirm (default)
                CmdDialog.createCommand(
                    title,
                    message,
                    image,
                    submitBtn,
                    cancelBtn,
                    countDownInt
                )
            }
        }

        return cmd
    }
}
