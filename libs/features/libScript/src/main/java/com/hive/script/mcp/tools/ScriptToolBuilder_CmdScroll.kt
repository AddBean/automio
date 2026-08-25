// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import android.graphics.Point
import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdScroll
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.script.mcp.toLongCompat
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolScroll)
class ScriptToolBuilder_CmdScroll : McpToolBuilder() {

    private var cmd = CmdScroll.createCommand(emptyList(), emptyList())

    override fun matchAction(actionName: String): Boolean {
        return "scroll" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "scroll",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_scroll_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_scroll_description),
        paramInfo = mutableListOf(
            McpActionParameters(
                name = "points",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_scroll_points_desc),
                required = true,
                examples = listOf("0.5,0.8,0.5,0.2"),
            ),
            McpActionParameters(
                name = "times",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_scroll_times_desc),
                required = true,
                examples = listOf("500"),
            )
        ),
        paramValues = emptyMap(),
    )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val points = action.paramValues["points"]
        val times = action.paramValues["times"]

        if (points.isNullOrBlank()) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_scroll_error_points_empty))
        }
        if (times.isNullOrBlank()) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_scroll_error_times_empty))
        }

        try {
            val pointValues = points.split(",").map { it.trim().toFloat() }
            val timeValues = times.split(",").map { it.trim().toLongCompat(0L) }

            if (pointValues.size % 2 != 0) {
                return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_scroll_error_points_format))
            }

            if (timeValues.isEmpty()) {
                return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_scroll_error_times_min))
            }

            pointValues.forEach { value ->
                if (value < 0 || value > 1) {
                    return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_scroll_error_coord_range))
                }
            }

        } catch (e: Exception) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_scroll_error_format, e.message))
        }

        return CheckActionResult(true, null)
    }

    override fun getCommand(): ScriptCommand? {
        return cmd
    }

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val pointsStr = params["points"] ?: ""
        val timesStr = params["times"] ?: ""

        val pointValues = pointsStr.split(",").map { it.trim().toFloat() }
        val timeValues = timesStr.split(",").map { it.trim().toLongCompat(0L) }

        val points = mutableListOf<Point>()
        for (i in pointValues.indices step 2) {
            val x = ScriptCoordinateAdapter.get().toRealX(pointValues[i])
            val y = ScriptCoordinateAdapter.get().toRealY(pointValues[i + 1])
            points.add(Point(x, y))
        }

        cmd = CmdScroll.createCommand(points, timeValues)
        return cmd
    }
} 