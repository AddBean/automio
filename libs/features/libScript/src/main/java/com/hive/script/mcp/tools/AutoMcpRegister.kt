// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import androidx.annotation.IntDef
import com.hive.script.utils.ScriptCommonUtils
import com.hive.utils.GlobalApp

/**
 * MCP 工具 ID 定义（仅保留仍注册使用的内置工具）
 */
object MCP_IDS {
    const val ToolGetCurrentLayout = 7//获取当前布局
    const val ToolScroll = 9//滚动
    const val ToolReadScreenText = 11//读取屏幕文本
    const val ToolOpenApp = 12//打开应用/链接(合并)
    const val ToolInput = 15//输入
    const val ToolDelay = 17//延迟
    const val ToolCopy = 18//复制
    const val ToolDialog = 20//弹窗
    const val ToolRequestPermission = 24//请求手机权限
    const val ToolGetInstalledAppList = 25//获取已安装应用
    const val ToolCaptureScreen = 26//截取屏幕
    const val ToolCurl = 27//curl
    const val ToolCaptureCamera = 28//摄像头拍照
    const val ToolVoiceInteract = 29//语音交互（TTS/ASR）
    const val ToolMemoryNote = 30//记忆读写（get/set）
    const val ToolActionSystem = 31//系统动作（back/home/recent/notifications）
    const val ToolInteract = 32//交互（合并：click/clickView）
    const val ToolPythonExecutor = 33//Python 执行器
    const val ToolDownload = 34//下载文件
}

@IntDef(
    MCP_IDS.ToolGetCurrentLayout,
    MCP_IDS.ToolScroll,
    MCP_IDS.ToolReadScreenText,
    MCP_IDS.ToolOpenApp,
    MCP_IDS.ToolInput,
    MCP_IDS.ToolDelay,
    MCP_IDS.ToolCopy,
    MCP_IDS.ToolDialog,
    MCP_IDS.ToolRequestPermission,
    MCP_IDS.ToolGetInstalledAppList,
    MCP_IDS.ToolCaptureScreen,
    MCP_IDS.ToolCurl,
    MCP_IDS.ToolCaptureCamera,
    MCP_IDS.ToolVoiceInteract,
    MCP_IDS.ToolMemoryNote,
    MCP_IDS.ToolActionSystem,
    MCP_IDS.ToolInteract,
    MCP_IDS.ToolPythonExecutor,
    MCP_IDS.ToolDownload,
)
@Retention(AnnotationRetention.SOURCE)
annotation class McpToolType

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoMcpToolsRegister(@McpToolType val type: Int = 0)

val Mcp_Tools_Register_Set = mutableSetOf<Class<*>>()

fun autoRegisterAllMcpTools() {
    // 要扫描的包
    ScriptCommonUtils.scanClass(GlobalApp.getContext(), AutoMcpToolsRegister::class)
        .forEach {
            val annotation = it.getAnnotation(AutoMcpToolsRegister::class.java)
            if (annotation != null) {
                Mcp_Tools_Register_Set.add(it)
            }
        }
}
