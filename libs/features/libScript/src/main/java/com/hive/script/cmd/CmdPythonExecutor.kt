// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.text.TextUtils
import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IPythonProvider
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.base.params.ScriptSystemParam
import com.hive.script.utils.ScriptHelper
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.views.widgets.CommonToast
import com.hive.utils.utils.GsonHelper
import com.hive.utils.utils.StringUtils
import org.json.JSONObject

/**
 * Python 脚本执行器
 *
 * 职责分离：
 * - CmdPythonExecutor：预解析（${main.param1} 变量替换）、构建可运行代码块（含输出包装）、负责 pythonCode 的编码/解码链路
 * - PythonProvider：仅执行传入的可运行代码块，返回结果
 *
 * pythonCode 存储为 StringUtils.encoding 后的字符串（与其它命令一致），用于安全落盘/序列化到单行脚本文本；
 * 展示与执行时通过 StringUtils.decoding 还原
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2024-06-24
 */
@AutoCmdRegister(type = IDS.CmdPythonExecutor, name = "python")
class CmdPythonExecutor : ScriptCommand(), ScriptRegularInterface {

    /**
     * Python 代码内容（已编码）
     * 注意：执行前会在 parseParamText 内部先 decoding，再做变量替换
     */
    var pythonCode: String? = null

    // 输出变量ID
    var outputParam: String = ScriptSystemParam.OUTPUT1.paramId

    // 执行超时时间（毫秒）
    var timeoutMs: Long = 30000L

    // 执行结果回调
    interface PythonExecutionCallback {
        fun onSuccess(result: String)
        fun onError(error: String)
        fun onTimeout()
    }

    private var callback: PythonExecutionCallback? = null

    override fun onExecute(): CmdExecuteResult {
        ScriptThreadManager.checkInterrupted()

        val provider = ComponentManager.getInstance()
            .getProvider(IPythonProvider::class.java) as? IPythonProvider
        if (provider == null || !provider.isAvailable()) {
            val msg = GlobalApp.getString(com.hive.i8n.R.string.script_python_provider_unavailable)
            ScriptHelper.runInMain {
                CommonToast.show(msg)
                handleExecutionError(msg)
            }
            ScriptThreadManager.delay(getCommandDuration())
            return CmdExecuteResult.failure(msg)
        }

        try {
            if (TextUtils.isEmpty(pythonCode)) {
                return CmdExecuteResult.failure("failed")
            }
            val parsedCode = parseParamText(pythonCode) ?: ""
            val result = if (parsedCode.isNotBlank()) {
                val executableCode = buildExecutableCode(parsedCode)
                provider.executeCode(executableCode)
            } else {
                IPythonProvider.Result(
                    -1,
                    "",
                    GlobalApp.getString(com.hive.i8n.R.string.script_python_not_specified)
                )
            }
            ScriptThreadManager.delay(getCommandDuration())
            return if (result.isSuccess) {
                extractAndSaveOutputVariables(result.output)
                CmdExecuteResult.success(data = result.output, message = result.output)
            } else {
                extractAndSaveOutputVariables(result.error)
                CmdExecuteResult.failure(result.error.ifEmpty {
                    GlobalApp.getString(
                        com.hive.i8n.R.string.script_python_exec_failed_code,
                        result.exitCode
                    )
                })
            }
        } catch (e: Exception) {
            val errMsg =
                e.message ?: GlobalApp.getString(com.hive.i8n.R.string.script_python_unknown_error)
            ScriptHelper.runInMain {
                handleExecutionError(errMsg)
            }
            extractAndSaveOutputVariables(e.message ?: "failed")
            ScriptThreadManager.delay(getCommandDuration())
            return CmdExecuteResult.failure(errMsg)
        }
    }

    private fun buildExecutableCode(parsedCode: String): String = parsedCode


    /**
     * 提取并保存输出变量
     */
    private fun extractAndSaveOutputVariables(output: String) {
        writeParam(outputParam, output)
    }

    /**
     * 处理执行错误
     */
    private fun handleExecutionError(error: String) {
        callback?.onError(error)
        logError(error)
    }

    private fun logSuccess(message: String) {
        // 成功日志由 callback 处理
    }

    private fun logError(message: String) {
        com.hive.utils.debug.DLog.w("CmdPythonExecutor", "Python执行失败: $message")
    }

    fun setCallback(callback: PythonExecutionCallback) {
        this.callback = callback
    }

    fun setTimeout(timeoutMs: Long) {
        this.timeoutMs = timeoutMs
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommandName() =
        GlobalApp.getString(com.hive.i8n.R.string.script_python_executor_name)

    override fun getCommandDescribe(): String {
        return if (!pythonCode.isNullOrEmpty()) {
            GlobalApp.getString(
                com.hive.i8n.R.string.script_python_exec_code,
                pythonCode?.take(50) ?: ""
            )
        } else {
            GlobalApp.getString(com.hive.i8n.R.string.script_python_executor_name)
        }
    }

    override fun getCommand(): String {
        val parts = mutableListOf<String>()
        if (!pythonCode.isNullOrEmpty()) {
            parts.add("code=\"${pythonCode!!.encode()}\"")
        } else {
            parts.add("code=\"\"")
        }
        outputParam.takeIf { it.isNotBlank() }?.let { parts.add("output=$it") }
        return "${cmdPrefix()} ${parts.joinToString(" ")}"
    }

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        // 这里存储的就是编码串（展示/执行时统一 decoding）
        pythonCode = p["code"]?.decode() ?: ""
        outputParam = p["output"]?.takeIf { it.isNotBlank() } ?: ScriptSystemParam.OUTPUT1.paramId
    }

    override fun getPermissionRequest() = null

    override fun getCommandIcon() = R.drawable.ic_sc_root

    companion object {
        fun createCodeCommand(code: String, outputParam: String? = null): CmdPythonExecutor {
            return CmdPythonExecutor().apply {
                // 统一编码存储，避免多行/特殊字符破坏脚本文本
                this.pythonCode = code
                // MCP 调用一般不需要写入脚本变量：传 null 即关闭输出包装
                this.outputParam = outputParam ?: ""
            }
        }

        fun createCommand(): CmdPythonExecutor {
            return CmdPythonExecutor().apply {
                this.pythonCode = ""
            }
        }
    }
}
