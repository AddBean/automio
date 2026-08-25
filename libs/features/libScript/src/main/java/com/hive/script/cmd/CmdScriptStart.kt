// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.text.TextUtils
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.ScriptStartRememberHelper
import com.hive.script.views.dialog.DialogCmdDialogInput
import com.hive.script.views.manager.ScriptManager
import com.hive.utils.GlobalApp
import com.hive.utils.thread.UIHandlerUtils
import com.hive.script.utils.ScriptHelper
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.views.dialog.DialogCmdDialogInput.InputItem
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdScriptStart, name = "scriptStart")
class CmdScriptStart : ScriptCommand(), ScriptRegularInterface {

    var dialogTitle: String? = null
    var dialogInputs: String? = null
    var dialogHints: String? = null
    var dialogRequires: String? = null
    var dialogDefaults: String? = null
    var dialogParams: String? = null

    private var dialogShowing: AtomicBoolean = AtomicBoolean(false)

    override fun onExecute(): CmdExecuteResult {
        if (executeDisable) {
            executeDisable = false
            return CmdExecuteResult.success()
        }
        if (TextUtils.isEmpty(dialogParams)) {
            return CmdExecuteResult.success()
        }
        var inputValues: List<InputItem>? = mutableListOf()
        dialogShowing.set(true)
        UIHandlerUtils.getInstance().post {
            showInputDialog { values ->
                inputValues = values
                dialogShowing.set(false)
            }
        }
        while (dialogShowing.get()) {
            ScriptThreadManager.delay(1000)
        }

        return if (inputValues?.isNotEmpty() == true) {
            CmdExecuteResult.success(
                "confirmed",
                GlobalApp.getString(com.hive.i8n.R.string.script_command_execute_success)
            )
        } else {
            ScriptHelper.runInMain {
                ScriptManager.stopPlay()
            }
            CmdExecuteResult.success("canceled")

        }
    }

    /**
     * 根据名称（标签或参数 id）查找对应的完整参数 id，用于 writeParam。
     * 先按标签在 dialogInputs 中查找下标，再取 dialogParams 同下标的参数 id；
     * 若未找到则把 key 当作参数 id 在 dialogParams 中查找。
     * @param key 输入项标签（如 dialogInputs 中的某一项）或参数 id（如 dialogParams 中的某一项）
     * @return 对应的完整参数 id，未找到时返回 null
     */
    fun findFullParamIdByName(key: String): String? {
        if (TextUtils.isEmpty(key)) return null
        val paramIds = dialogParams?.split("|")?.map { it.trim() } ?: return null
        val labels = dialogInputs?.split("|")?.map { it.trim() } ?: emptyList()
        val index = labels.indexOf(key)
        return if (index >= 0 && index < paramIds.size) {
            ScriptParamEnv.parseParamsId(paramIds[index])
        } else {
            paramIds.find { it == key }?.let { ScriptParamEnv.parseParamsId(it) }
        }
    }


    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun isSupportDelay() = false

    override fun isSupportDrag() = false

    override fun isSupportOffset() = false

    override fun isSupportRect() = false

    override fun isGroupCommand() = false

    override fun getCommand(): String {
        fun q(s: String?) = when {
            s.isNullOrBlank() -> ""
            s.contains(" ") || s.contains("\"") || s.contains("|") -> "\"${
                (s ?: "").replace(
                    "\"",
                    "\\\""
                )
            }\""

            else -> s ?: ""
        }
        return buildString {
            append(cmdPrefix())
            if (!dialogParams.isNullOrEmpty()) append(" params=${q(dialogParams)}")
            if (!dialogTitle.isNullOrEmpty()) append(" title=${q(dialogTitle)}")
            if (!dialogInputs.isNullOrEmpty()) append(" inputs=${q(dialogInputs)}")
            if (!dialogHints.isNullOrEmpty()) append(" hints=${q(dialogHints)}")
            if (!dialogRequires.isNullOrEmpty()) append(" requires=${q(dialogRequires)}")
            if (!dialogDefaults.isNullOrEmpty()) append(" defaults=${q(dialogDefaults)}")
        }
    }

    override fun getCommandName(): String =
        GlobalApp.getString(com.hive.i8n.R.string.cmd_name_script_start)

    override fun getCommandDescribe(): String =
        GlobalApp.getString(com.hive.i8n.R.string.cmd_name_script_start_describe)

    override fun getCommandIcon() = R.drawable.sc_icon_record

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        dialogTitle = p["title"]?.takeIf { it.isNotBlank() }
        dialogInputs = p["inputs"]?.takeIf { it.isNotBlank() }
        dialogHints = p["hints"]?.takeIf { it.isNotBlank() }
        dialogRequires = p["requires"]?.takeIf { it.isNotBlank() }
        dialogDefaults = p["defaults"]?.takeIf { it.isNotBlank() }
        dialogParams = p["params"]?.takeIf { it.isNotBlank() }
    }

    override fun getPermissionRequest() = null

    private fun showInputDialog(onInput: (values: List<InputItem>) -> Unit) {
        val rememberKey =
            ScriptStartRememberHelper.buildKey(getScriptBasePath(), dialogParams.orEmpty())
        val savedValues = ScriptStartRememberHelper.load(rememberKey)
        val inputItems = parseInputItems(this)
        if (!savedValues.isNullOrEmpty()) {
            savedValues.forEach { (pId, value) ->
                writeParam(pId, value)
                inputItems.find { ScriptParamEnv.parseParamsId(it.id?:"")== pId }?.run {
                    this.defaultValue = value
                    this.value = value
                }
            }
        }


        val dialog = DialogCmdDialogInput(ScriptProvider.getViewContext())
        dialog.setTitle(
            dialogTitle ?: GlobalApp.getString(com.hive.i8n.R.string.cmd_name_script_start)
        )
        dialog.setInputItems(inputItems)
        dialog.setRememberOption(rememberKey) { key, values ->
            ScriptStartRememberHelper.save(key, values)
        }
        dialog.setInputListener(object : DialogCmdDialogInput.OnInputListener {
            override fun onConfirmed(dialog: DialogCmdDialogInput, inputs: List<InputItem>) {
                inputs.forEach { it ->
                    if (!it.id.isNullOrEmpty() && it.value.isNotEmpty()) {
                        val pId = ScriptParamEnv.parseParamsId(it.id)
                        writeParam(pId, it.value)
                    }
                }
                onInput.invoke(inputs)
            }

            override fun onCancel() {
                onInput.invoke(mutableListOf())
            }
        })
        dialog.show()
    }


    companion object {

        private var executeDisable = false

        fun ignoreExecuteOnce() {
            executeDisable = true
        }

        fun cleanIgnoreFlag() {
            executeDisable = false
        }

        fun createCommand(
            params: String? = null,
            title: String? = null,
            inputs: String? = null,
            hints: String? = null,
            requires: String? = null,
            defaults: String? = null,
        ) = CmdScriptStart().apply {
            this.dialogParams = params
            this.dialogTitle = title
            this.dialogInputs = inputs
            this.dialogHints = hints
            this.dialogRequires = requires
            this.dialogDefaults = defaults
        }

        /**
         * 解析输入项配置
         * 格式：分别解析 inputs、hints、requires、defaults、params
         * inputs: 输入项标签列表，用|分隔
         * hints: 提示文本列表，用|分隔
         * requires: 是否必填列表，用|分隔
         * defaults: 默认值列表，用|分隔
         * params: 自定义参数列表，用|分隔
         */
        fun parseInputItems(cmd: CmdScriptStart): List<InputItem> {
            val params = cmd.dialogParams?.split("|") ?: emptyList()
            val inputs = cmd.dialogInputs?.split("|") ?: emptyList()
            val hints = cmd.dialogHints?.split("|") ?: emptyList()
            val requires = cmd.dialogRequires?.split("|") ?: emptyList()
            val defaults = cmd.dialogDefaults?.split("|") ?: emptyList()

            if (inputs.isEmpty()) {
                return emptyList()
            }

            return inputs.mapIndexed { index, label ->
                val param = if (index < params.size) params[index] else ""
                val hint =
                    if (index < hints.size) hints[index] else GlobalApp.getString(com.hive.i8n.R.string.ui_dialog_input_hint)
                val required = if (index < requires.size) requires[index].toBoolean() else false
                val defaultValue = if (index < defaults.size) defaults[index] else ""

                InputItem(
                    id = param,
                    label = label,
                    hint = hint,
                    required = required,
                    inputType = DialogCmdDialogInput.TYPE_TEXT,
                    defaultValue = defaultValue,
                )
            }
        }
    }
}