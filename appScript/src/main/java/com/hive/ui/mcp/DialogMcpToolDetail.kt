// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.mcp

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.gson.JsonObject
import com.hive.app.script.R
import com.hive.plugin.mcp.McpConst
import com.hive.plugin.mcp.model.McpTool
import com.hive.script.base.ScriptConst
import com.hive.script.mcp.ScriptMcpRegister
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.dialog.DialogScriptListSelector
import com.hive.script.views.edit.DialogScriptEdit
import java.io.File

/**
 * MCP 工具详情弹框
 * 展示工具名称、描述、参数等信息，风格与 App 整体一致
 * 自定义工具支持编辑脚本地址
 *
 * @author jiadou
 */
class DialogMcpToolDetail : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_mcp_tool_detail, null)
        dialog.setContentView(view)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvToolName = view.findViewById<TextView>(R.id.tv_tool_name)
        val tvToolType = view.findViewById<TextView>(R.id.tv_tool_type)
        val tvDescription = view.findViewById<TextView>(R.id.tv_description)
        val tvParamsLabel = view.findViewById<TextView>(R.id.tv_params_label)
        val scrollParams = view.findViewById<ScrollView>(R.id.scroll_params)
        val tvParams = view.findViewById<TextView>(R.id.tv_params)
        val ivClose = view.findViewById<ImageView>(R.id.iv_close)
        val tvBtnOk = view.findViewById<TextView>(R.id.tv_btn_ok)
        val layoutScriptPath = view.findViewById<View>(R.id.layout_script_path)
        val tvScriptPath = view.findViewById<TextView>(R.id.tv_script_path)
        val tvBtnChangeScript = view.findViewById<TextView>(R.id.tv_btn_change_script)
        val tvBtnEditScript = view.findViewById<TextView>(R.id.tv_btn_edit_script)

        val name = arguments?.getString(ARG_TOOL_NAME) ?: ""
        val desc = arguments?.getString(ARG_TOOL_DESC) ?: ""
        val schemaStr = arguments?.getString(ARG_TOOL_SCHEMA) ?: "{}"
        val scriptId = arguments?.getString(ARG_SCRIPT_ID)
        val scriptName = arguments?.getString(ARG_SCRIPT_NAME)
        val scriptDesc = arguments?.getString(ARG_SCRIPT_DESC)
        val scriptPath = arguments?.getString(ARG_SCRIPT_PATH)
        val toolType = arguments?.getString(ARG_TOOL_TYPE)

        tvToolName.text = name
        val isCustomTool = toolType != McpConst.Tool_Type_BuildIn
        tvToolType.text = getString(
            if (isCustomTool) com.hive.i8n.R.string.mcp_tool_type_custom else com.hive.i8n.R.string.mcp_tool_type_builtin
        )
        tvToolType.setBackgroundResource(
            if (isCustomTool) R.drawable.bg_filter_pill_amber else R.drawable.bg_filter_pill_sky
        )
        tvDescription.text = desc.ifEmpty { "-" }
        tvDescription.visibility = View.VISIBLE

        val schema = try {
            com.google.gson.JsonParser().parse(schemaStr).asJsonObject
        } catch (_: Exception) {
            JsonObject()
        }
        val paramsText = formatInputSchema(schema)
        if (paramsText.isNotEmpty()) {
            tvParamsLabel.visibility = View.VISIBLE
            scrollParams.visibility = View.VISIBLE
            tvParams.text = paramsText
        } else {
            tvParamsLabel.visibility = View.GONE
            scrollParams.visibility = View.GONE
        }

        // 自定义工具：显示脚本路径、更换脚本和编辑按钮
        if (scriptId != null && scriptPath != null && scriptName != null && scriptDesc != null) {
            layoutScriptPath.visibility = View.VISIBLE
            tvScriptPath.text = scriptPath
            tvBtnChangeScript.setOnClickListener {
                showScriptSelectorAndUpdate(scriptId, scriptName, scriptDesc, layoutScriptPath, tvScriptPath)
            }
            tvBtnEditScript.setOnClickListener {
                openScriptEditor(scriptPath)
            }
        } else {
            layoutScriptPath.visibility = View.GONE
        }

        fun dismissDialog() {
            dismissAllowingStateLoss()
        }

        ivClose.setOnClickListener { dismissDialog() }
        tvBtnOk.setOnClickListener { dismissDialog() }

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private fun showScriptSelectorAndUpdate(
        scriptId: String,
        scriptName: String,
        scriptDesc: String,
        layoutScriptPath: View,
        tvScriptPath: TextView
    ) {
        val ctx = requireContext()
        DialogScriptListSelector(ctx, true)
            .setTitle(ctx.getString(com.hive.i8n.R.string.script_provider_select_script))
            .setOnScriptSelectListener(object : DialogScriptListSelector.OnScriptSelectListener {
                override fun onSelected(
                    dialog: DialogScriptListSelector,
                    model: com.hive.script.views.beans.ScriptInfoModel
                ) {
                    dialog.dismiss()
                    val newPath = model.scriptPath ?: return
                    val scriptDir = java.io.File(newPath)
                    if (!scriptDir.exists() || !scriptDir.isDirectory) return
                    ScriptMcpRegister.registerCustomTool(
                        scriptName = scriptName,
                        scriptDesc = scriptDesc,
                        scriptPath = newPath,
                        toolId = scriptId,
                        overwriteIfExists = true,
                        persistToSp = true
                    )
                    tvScriptPath.text = newPath
                    (targetFragment as? FragmentMcpToolList)?.loadData()
                    (targetFragment as? FragmentToolCustomList)?.loadData()
                }

                override fun onDismissed() {}
            })
            .show()
    }

    private fun openScriptEditor(scriptPath: String) {
        val scriptDir = File(scriptPath)
        if (!scriptDir.exists() || !scriptDir.isDirectory) return
        dismissAllowingStateLoss()
        val infoModel = ScriptHelper.getScriptInfoModelByPath(scriptPath)
        DialogScriptEdit.create(infoModel.scriptMate)
            ?.setScriptPath(scriptPath)
            ?.setTitleName(scriptDir.name)
            ?.setFromSource(ScriptConst.From.FROM_SCRIPT_UNKNOWN)
            ?.show()
    }

    /**
     * 将 inputSchema 格式化为可读的参数字符串
     */
    private fun formatInputSchema(schema: JsonObject): String {
        val sb = StringBuilder()
        try {
            val properties = schema.getAsJsonObject("properties") ?: return ""
            val required = schema.getAsJsonArray("required")?.let { arr ->
                (0 until arr.size()).map { arr.get(it).asString }.toSet()
            } ?: emptySet()

            properties.entrySet().forEach { (name, prop) ->
                if (prop.isJsonObject) {
                    val obj = prop.asJsonObject
                    val propDesc = obj.get("description")?.asString
                    val isReq = name in required
                    sb.append("• $name")
                    if (isReq) sb.append(" (required)")
                    sb.append("\n")
                    if (!propDesc.isNullOrEmpty()) {
                        sb.append("  $propDesc\n")
                    }
                }
            }
        } catch (_: Exception) {
            // 解析失败时返回空
        }
        return sb.toString().trim()
    }

    companion object {
        private const val ARG_TOOL_NAME = "tool_name"
        private const val ARG_TOOL_DESC = "tool_desc"
        private const val ARG_TOOL_SCHEMA = "tool_schema"
        private const val ARG_TOOL_TYPE = "tool_type"
        private const val ARG_SCRIPT_ID = "script_id"
        private const val ARG_SCRIPT_NAME = "script_name"
        private const val ARG_SCRIPT_DESC = "script_desc"
        private const val ARG_SCRIPT_PATH = "script_path"

        /**
         * 显示工具详情弹框
         * @param fragment 宿主 Fragment
         * @param tool MCP 工具
         * @param customTool 自定义工具配置（可选，用于编辑脚本地址）
         */
        fun show(fragment: Fragment, tool: McpTool, customTool: com.hive.script.net.data.ScriptCustomMcpTool? = null) {
            DialogMcpToolDetail().apply {
                setTargetFragment(fragment, 0)
                arguments = Bundle().apply {
                    putString(ARG_TOOL_NAME, tool.name)
                    putString(ARG_TOOL_DESC, tool.description)
                    putString(ARG_TOOL_SCHEMA, tool.inputSchema.toString())
                    putString(ARG_TOOL_TYPE, tool.extraType)
                    customTool?.let {
                        putString(ARG_SCRIPT_ID, it.scriptId)
                        putString(ARG_SCRIPT_NAME, it.scriptName)
                        putString(ARG_SCRIPT_DESC, it.scriptDesc)
                        putString(ARG_SCRIPT_PATH, it.scriptPath)
                    }
                }
            }.show(fragment.parentFragmentManager, "DialogMcpToolDetail")
        }
    }
}
