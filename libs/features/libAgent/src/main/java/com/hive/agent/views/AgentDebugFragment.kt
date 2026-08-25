// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.hive.agent.R
import com.hive.agent.XAgent
import com.hive.agent.config.AIAgentConfig
import com.hive.base.BaseFragment
import com.hive.plugin.agent.IAgentTaskObserver
import com.hive.plugin.agent.model.AgentResult
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.TaskPriority
import com.hive.script.utils.ScriptHelper.PERMISSION_BIND_ACCESSIBILITY_SERVICE
import com.hive.script.utils.ScriptHelper.PERMISSION_CAPTURE
import com.hive.script.utils.ScriptPermissionManager
import com.hive.utils.debug.DLog
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgentDebugFragment : BaseFragment() {

    private val xAgent = XAgent.getInstance()

    private lateinit var logTextView: TextView
    private lateinit var systemInfoText: TextView

    private var isAgentInitialized = false
    private val logBuilder = StringBuilder()

    override fun initView() {
        isAgentInitialized = xAgent.getRegisteredTools().isNotEmpty() == true

        xAgent.registerAgentTaskObserver(object : IAgentTaskObserver {
            override fun onTaskInfoUpdated(message: String) {
                logMessage("🔔 工作流更新: $message")
            }

            override fun onTaskMessageUpdated(goal: AgentTaskGoal) {
                // 可以在这里更新聊天界面
            }

            override fun onTaskMessageStreamUpdated(goal: AgentTaskGoal) {

            }

        })

        initializeViews()
        setupClickListeners()
        logMessage("🎯 调试界面已加载，等待用户操作")
        refreshSystemInfo()
    }

    private fun initializeViews() {
        logTextView = view?.findViewById(R.id.logTextView) ?: return
        systemInfoText = view?.findViewById(R.id.systemInfoText) ?: return
    }

    private fun setupClickListeners() {
        view?.findViewById<View>(R.id.complexTaskBtn)?.setOnClickListener {
            testComplexTask()
        }

        view?.findViewById<View>(R.id.mcpTestBtn)?.setOnClickListener {
            testMcpTools()
        }

        view?.findViewById<View>(R.id.clearLogsBtn)?.setOnClickListener {
            clearLogs()
        }

        view?.findViewById<View>(R.id.listToolsBtn)?.setOnClickListener {
            listRegisteredTools()
        }

        view?.findViewById<View>(R.id.taskReportBtn)?.setOnClickListener {
            showTaskReports()
        }

        view?.findViewById<View>(R.id.refreshSystemInfoBtn)?.setOnClickListener {
            refreshSystemInfo()
        }

        view?.findViewById<View>(R.id.exportLogsBtn)?.setOnClickListener {
            exportLogs()
        }



        view?.findViewById<View>(R.id.runMcpCallTestsBtn)?.setOnClickListener {
            testMcpCall()
        }

        view?.findViewById<View>(R.id.pauseTaskBtn)?.setOnClickListener {
            xAgent.getRunningTasks()?.forEach {
                xAgent.pauseTask(it)
            }
        }

        view?.findViewById<View>(R.id.resumeTaskBtn)?.setOnClickListener {
            xAgent.getTasksByState()?.forEach {
                xAgent.resumeTask(it)
            }
        }

        view?.findViewById<View>(R.id.stopTaskBtn)?.setOnClickListener {
            xAgent.getTasksByState()?.forEach {
                xAgent.stopTask(it)
            }
        }
    }


    private fun refreshSystemInfo() {
        lifecycleScope.launch {
            try {
                val aiProvider = if (isAgentInitialized) "Agent (已连接)" else "未连接"
                val toolCount =
                    if (isAgentInitialized) xAgent.getRegisteredTools().size.toString() else "0"
                val mcpStatus =
                    if (isAgentInitialized) "已连接 (${AIAgentConfig.BaseConfig.McpToolName})" else "未连接"

                val info = "AI提供器: $aiProvider\n已注册工具: $toolCount\nMCP服务器: $mcpStatus"

                activity?.runOnUiThread {
                    systemInfoText.text = info
                }

                logMessage("🔄 系统信息已刷新")
            } catch (e: Exception) {
                logMessage("❌ 刷新系统信息失败: ${e.message}")
            }
        }
    }

    private fun listRegisteredTools() {
        if (!isAgentInitialized) {
            logMessage("❌ 请先初始化Agent系统")
            return
        }

        lifecycleScope.launch {
            try {
                logMessage("\n📋 正在获取已注册工具列表...")
                val tools = xAgent.getRegisteredTools()

                if (tools.isEmpty()) {
                    logMessage("⚠️ 没有已注册的工具")
                } else {
                    logMessage("✅ 已注册工具 (${tools.size}个):")
                    tools.forEachIndexed { index, tool ->
                        logMessage("  ${index + 1}. ${tool.name} (${tool.id})")
                        logMessage("     支持操作: ${tool.supportedMethods.joinToString(", ")}")
                    }
                }
            } catch (e: Exception) {
                logMessage("❌ 获取工具列表失败: ${e.message}")
            }
        }
    }

    private fun showTaskReports() {
        if (!isAgentInitialized) {
            logMessage("❌ 请先初始化Agent系统")
            return
        }

        lifecycleScope.launch {
            try {
                logMessage("\n📊 正在获取工作流报告...")
                val taskIds = xAgent.getAllTaskIds()

                if (taskIds.isEmpty()) {
                    logMessage("ℹ️ 暂无工作流执行记录")
                } else {
                    logMessage("✅ 找到 ${taskIds.size} 个工作流记录:")
                    taskIds.take(5).forEach { taskId ->
                        val report = xAgent.getTaskReport(taskId)
                        logMessage("📋 工作流ID: $taskId")
                        logMessage(report)
                        logMessage("---")
                    }

                    if (taskIds.size > 5) {
                        logMessage("⚠️ 仅显示最近5个工作流，共${taskIds.size}个")
                    }
                }
            } catch (e: Exception) {
                logMessage("❌ 获取工作流报告失败: ${e.message}")
            }
        }
    }


    private fun exportLogs() {
        lifecycleScope.launch {
            try {
                logMessage("\n📤 正在导出日志...")

                val timestamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "agent_debug_log_$timestamp.txt"

                val externalDir = context?.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val file = File(externalDir, fileName)

                FileWriter(file).use { writer ->
                    writer.write("=== Agent调试日志导出 ===\n")
                    writer.write(
                        "导出时间: ${
                            SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            ).format(Date())
                        }\n"
                    )
                    writer.write("系统信息:\n${systemInfoText.text}\n")
                    writer.write("\n=== 执行日志 ===\n")
                    writer.write(logBuilder.toString())
                }

                logMessage("✅ 日志已导出至: ${file.absolutePath}")

                val clipboardManager =
                    context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("文件路径", file.absolutePath)
                clipboardManager?.setPrimaryClip(clip)

                Toast.makeText(context, "日志已导出，路径已复制到剪贴板", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                logMessage("❌ 导出日志失败: ${e.message}")
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ScriptPermissionManager.checkMissedPermissions(
            arrayListOf(
                PERMISSION_BIND_ACCESSIBILITY_SERVICE,
                PERMISSION_CAPTURE
            )
        ).isEmpty()
    }

    private fun testComplexTask() {
        if (!checkAgentReady()) return

        try {
            logMessage("\n📋 测试复杂工作流执行...")

            val goal = AgentTaskGoal(
                id = "mobile_automation_${System.currentTimeMillis()}",
                userInput = "发送一条消息'你好呀'到微信给豆夫子",
                requiredCapabilities = listOf(
                    AIAgentConfig.BaseConfig.AIAssistantToolName,
                    AIAgentConfig.BaseConfig.McpToolName
                ),
                priority = TaskPriority.NORMAL
            )

            logMessage("🎯 工作流目标: ${goal.userInputOptimized}")
            logMessage("🔧 所需能力: ${goal.requiredCapabilities.joinToString(", ")}")
            logMessage("⏳ 开始执行...")

            xAgent.executeTask(goal) { result ->
                if (result.isSuccess()) {
                    logMessage("✅ 复杂工作流执行成功!")
                    logMessage("⏱️ 执行时长: ${result.getDuration()}ms")
                    logMessage("📊 工作流结果: ${result.data}")

                    val report = xAgent.getTaskReport(goal.id)
                    logMessage("📋 执行报告:")
                    logMessage(report)
                } else {
                    logMessage("❌ 复杂工作流执行失败: ${result.message}")
                    logMessage("🔍 建议检查工作流配置和系统状态")
                }
            }


        } catch (e: Exception) {
            logMessage("❌ 复杂工作流执行异常: ${e.message}")
        }
    }

    private fun testMcpCall() {
        if (!checkAgentReady()) return
        lifecycleScope.launch {
//            val result: AgentResult<*> = xAgent.callMcpTool(
//                serverId = AIAgentConfig.AgentDefaults.McpToolName,
//                toolName = "openApp",
//                arguments = mapOf(
//                    "packageName" to "com.tencent.mm",
//                    "className" to "-",
//                    "appName" to "微信",
//                    "action" to "reopen"
//                )
//            )

//            val result: AgentResult<*> = xAgent.callMcpTool(
//                serverId = AIAgentConfig.AgentDefaults.McpToolName,
//                toolName = "dialogConfirm",
//                arguments = mutableMapOf(
//                    "title" to "警告",
//                    "message" to "是否继续？",
//                    "cancelBtn" to "取消",
//                    "submitBtn" to "继续",
//                    "countdown" to "10"
//                )
//            )
//                        val result: AgentResult<*> = xAgent.callMcpTool(
//                            serverId = AIAgentConfig.AgentDefaults.McpToolName,
//                            toolName = "dialogSelector",
//                            arguments = mutableMapOf(
//                                "title" to "选择性别",
//                                "items" to "男|女|未知",
//                                "multiSelect" to "false",
//                            )
//                        )


//dialogInput title="",="",hints="",="",="|0|0|"

//            val result: AgentResult<*> = xAgent.callMcpTool(
//                serverId = AIAgentConfig.AgentDefaults.McpToolName,
//                toolName = "dialogInput",
//                arguments = mutableMapOf(
//                    "title" to "商品信息",
//                    "inputs" to "商品名称|价格|库存|描述",
//                    "hints" to "请输入商品名称|请输入价格|请输入库存数量|请输入商品描述",
//                    "requires" to "true|true|true|false",
//                    "defaults" to "手机|111|20|手机描述"
//                )
//            )

//            val result: AgentResult<*> = xAgent.callMcpTool(
//                serverId = AIAgentConfig.AgentDefaults.McpToolName,
//                toolName = "waitUserOperate",
//                arguments = mutableMapOf(
//                    "title" to "警告",
//                    "message" to "是否继续？",
//                    "cancelBtn" to "取消",
//                    "submitBtn" to "继续",
//                    "countdown" to "10"
//                )
//            )
//            val result: AgentResult<*> = xAgent.callMcpTool(
//                serverId = AIAgentConfig.AgentDefaults.McpToolName,
//                toolName = "getInstalledAppList",
//                arguments = mutableMapOf(  )
//            )
            val result: AgentResult<*> = xAgent.callMcpTool(
                serverId = AIAgentConfig.BaseConfig.McpToolName,
                toolName = "requestPermission",
                arguments = mutableMapOf(
                    "permission" to "android.permission.CALL_PHONE"
                )
            )
            when {
                result.success -> {
                    logMessage("✅ MCP工具调用成功: ${result.data}")
                }

                else -> {
                    logMessage("❌ MCP工具调用失败: ${result.getErrorOrNull()}")
                    logMessage("🔍 错误代码: ${result.getErrorOrNull()}")
                }
            }
        }
    }

    private fun testMcpTools() {
        if (!checkAgentReady()) return

        lifecycleScope.launch {
            try {
                logMessage("🌐 连接到MCP服务器...")

                xAgent.refreshMcpServer(AIAgentConfig.BaseConfig.McpToolName)

                val result: AgentResult<*> = xAgent.callMcpTool(
                    serverId = AIAgentConfig.BaseConfig.McpToolName,
                    toolName = com.hive.plugin.mcp.McpConst.Tool_Name_Prefix_BuildIn + "readScreenLayout",
                    arguments = emptyMap()
                )

                when {
                    result.success -> {
                        logMessage("${GsonHelper.getInstance().toFormatJson(result.data)}")
                    }

                    else -> {
                        logMessage("❌ 工具调用失败: ${result.getErrorOrNull()}")
                        logMessage("🔍 错误代码: ${result.getErrorOrNull()?.code}")
                        logMessage("💡 建议检查MCP服务器状态")
                    }
                }
            } catch (e: Exception) {
                logMessage("❌ MCP工具列表获取异常: ${e.message}")
                logMessage("🔍 建议检查服务器连接和配置")
            }
        }
    }

    private fun checkAgentReady(): Boolean {
        if (!isAgentInitialized) {
            logMessage("❌ 请先初始化Agent系统")
            return false
        }
        return true
    }

    private fun clearLogs() {
        logBuilder.clear()
        activity?.runOnUiThread {
            logTextView.text = "=== 🤖 Agent测试日志 ===\n等待用户操作...\n"
        }
        logMessage("🧹 日志已清空")
    }

    private fun logMessage(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $message\n"

        logBuilder.append(logEntry)

        activity?.runOnUiThread {
            logTextView.append(logEntry)
            DLog.e(message)
            view?.findViewById<View>(R.id.logScrollView)?.let { scrollView ->
                scrollView.post {
                    (scrollView as? android.widget.ScrollView)?.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }


    override fun getLayoutId() = R.layout.fragment_agent_debug

    override fun onDestroy() {
        super.onDestroy()
        if (isAgentInitialized) {
            try {
                xAgent.cleanup()
                logMessage("🧹 Agent系统已清理")
            } catch (e: Exception) {
                // 忽略清理时的错误
            }
        }
    }
} 
