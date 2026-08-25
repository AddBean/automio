// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.params.ScriptParam
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.cmd.CmdPythonExecutor
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.extends.string
import com.hive.utils.utils.StringUtils

/**
 * Python 执行器编辑卡片：代码编辑 + 输出变量选择
 */
class CmdPythonExecutorEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdPythonExecutor? = null

    private var paramCode: ScriptValueView? = null
    private var paramOutput: ScriptValueView? = null

    override fun initView() {
        paramCode = findViewById(R.id.paramCode)
        paramOutput = findViewById(R.id.paramOutput)

        paramCode?.onMaskClickListener = OnClickListener {
            DialogCommonTextInput(context)
                .setSingleLine(false)
                .setTitle(com.hive.i8n.R.string.script_python_code_edit_title.string())
                .setHint(com.hive.i8n.R.string.script_python_code_edit_hint.string())
                .setActionMenuList(
                    mutableListOf(
                        ScriptSpanParamLayout.ActionMenuType.Copy,
                        ScriptSpanParamLayout.ActionMenuType.Paste,
                        ScriptSpanParamLayout.ActionMenuType.Clean,
                        ScriptSpanParamLayout.ActionMenuType.Format
                    )
                )
                .setText(StringUtils.decoding(cmd?.pythonCode ?: ""))
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        cmd?.pythonCode = StringUtils.encoding(content)
                        cmd?.let { onBindCommand(it) }
                    }

                    override fun onCanceled() {}
                }).show()
        }

        paramOutput?.onMaskClickListener = OnClickListener {
            DialogParamsManager(context)
                .setWritable(true)
                .setParamListener(object : DialogParamsManager.OnParamListener {
                    override fun onParamSelected(param: ScriptParam?) {
                        cmd?.outputParam =
                            ScriptParamEnv.getParam(param?.getFullId() ?: "")?.getFullId() ?: ""
                        cmd?.let { onBindCommand(it) }
                    }
                }).show()
        }
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as? CmdPythonExecutor
        cmd?.run {
            paramCode?.setValue(ScriptCommandHelper.getValueDisplayName(pythonCode))
            paramOutput?.setValue(ScriptCommandHelper.paramFormat.format(outputParam))
        }
    }

    override fun checkCommandOrThrowError() {
        if (cmd?.pythonCode.isNullOrEmpty()) {
            throw IllegalArgumentException(com.hive.i8n.R.string.script_python_code_edit_empty.string())
        }
    }

    override fun getEditContentId() = R.layout.cmd_python_executor_edit_card
}
