// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdClick
import com.hive.script.cmd.CmdClickView
import com.hive.script.cmd.CmdPress
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.script.mcp.toIntCompat
import com.hive.script.mcp.toLongCompat
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.utils.GlobalApp

/**
 * 统一交互工具：优先使用控件 targetId/targetTag；为空时使用坐标 x/y（归一化 0–1）。
 * - action=click|press|fastClick
 * - 坐标模式下仅支持 click/press（fastClick 建议使用 target 模式或旧工具）
 */
@AutoMcpToolsRegister(MCP_IDS.ToolInteract)
class ScriptToolBuilder_CmdInteract : McpToolBuilder() {

    private var cmd: ScriptCommand? = null

    override fun matchAction(actionName: String): Boolean {
        return "interact" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "interact",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_interact_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_interact_description),
        paramInfo = mutableListOf(
            McpActionParameters(
                name = "action",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_interact_action_desc),
                required = false,
                examples = listOf("click", "press", "fastClick"),
            ),
            McpActionParameters(
                name = "targetId",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_interact_target_id_desc),
                required = false,
                examples = listOf("button_login", "-"),
            ),
            McpActionParameters(
                name = "targetTag",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_interact_target_tag_desc),
                required = false,
                examples = listOf("tag_button", "-"),
            ),
            McpActionParameters(
                name = "x",
                type = "number",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_interact_x_desc),
                required = false,
                examples = listOf("0.5"),
                format = "number",
            ),
            McpActionParameters(
                name = "y",
                type = "number",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_interact_y_desc),
                required = false,
                examples = listOf("0.5"),
                format = "number",
            ),
            McpActionParameters(
                name = "fastCount",
                type = "number",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_interact_fast_count_desc),
                required = false,
                examples = listOf("5"),
                format = "number",
            ),
            McpActionParameters(
                name = "fastGap",
                type = "number",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_interact_fast_gap_desc),
                required = false,
                examples = listOf("200"),
                format = "number",
            ),
            McpActionParameters(
                name = "pressDuration",
                type = "number",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_interact_press_duration_desc),
                required = false,
                examples = listOf("1000"),
                format = "number",
            ),
        ),
        paramValues = emptyMap(),
    )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val actionType = (action.paramValues["action"] ?: "click").trim()
        if (actionType !in listOf("click", "press", "fastClick")) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_interact_error_invalid_action))
        }

        val targetId = action.paramValues["targetId"]?.takeIf { it.isNotBlank() && it != ScriptConst.NONE_CHAR }
        val targetTag = action.paramValues["targetTag"]?.takeIf { it.isNotBlank() && it != ScriptConst.NONE_CHAR }
        val hasTarget = !targetId.isNullOrBlank() || !targetTag.isNullOrBlank()

        val x = action.paramValues["x"]?.toFloatOrNull()
        val y = action.paramValues["y"]?.toFloatOrNull()
        val hasXY = x != null && y != null

        if (!hasTarget && !hasXY) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_interact_error_missing_target))
        }

        if (hasXY) {
            if (x!! < 0 || x > 1) return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_interact_error_x_range))
            if (y!! < 0 || y > 1) return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_interact_error_y_range))
            if (actionType == "fastClick") {
                return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_interact_error_fast_click_coord))
            }
        }

        val fastCount = action.paramValues["fastCount"].toIntCompat(1)
        val fastGap = action.paramValues["fastGap"].toLongCompat(200L)
        val pressDuration = action.paramValues["pressDuration"].toLongCompat(1000L)

        if (fastCount <= 0) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_interact_error_fast_count_positive))
        }
        if (fastGap < 0) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_interact_error_fast_gap_negative))
        }
        if (pressDuration < 0) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_interact_error_press_duration_negative))
        }

        return CheckActionResult(true, null)
    }

    override fun getCommand(): ScriptCommand? = cmd

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val actionType = (params["action"] ?: "click").trim()

        val targetId = params["targetId"]?.takeIf { it.isNotBlank() } ?: ScriptConst.NONE_CHAR
        val targetTag = params["targetTag"]?.takeIf { it.isNotBlank() } ?: ScriptConst.NONE_CHAR
        val hasTarget = (targetId != ScriptConst.NONE_CHAR) || (targetTag != ScriptConst.NONE_CHAR)

        val fastCount = params["fastCount"].toIntCompat(1)
        val fastGap = params["fastGap"].toLongCompat(200L)
        val pressDuration = params["pressDuration"].toLongCompat(1000L)

        cmd = if (hasTarget) {
            val realAction = when (actionType) {
                "press" -> ScriptClickActionHelper.ACTION_PRESS
                "fastClick" -> ScriptClickActionHelper.ACTION_FAST_CLICK
                else -> ScriptClickActionHelper.ACTION_CLICK
            }
            CmdClickView.createCommand(
                realAction,
                fastCount,
                fastGap,
                pressDuration,
                targetId,
                ScriptConst.NONE_CHAR,
                targetTag
            )
        } else {
            val px = params["x"]?.toFloatOrNull() ?: 0f
            val py = params["y"]?.toFloatOrNull() ?: 0f
            val x = ScriptCoordinateAdapter.get().toRealX(px)
            val y = ScriptCoordinateAdapter.get().toRealY(py)
            when (actionType) {
                "press" -> CmdPress.createCommand(x, y, pressDuration)
                else -> CmdClick.createCommand(x, y)
            }
        }

        return cmd
    }
}

