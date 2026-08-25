// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst.RegexActions
import com.hive.script.base.params.ScriptParam
import com.hive.script.base.params.ScriptSystemParam
import com.hive.script.cmd.CmdSet
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.views.dialog.DialogCalculator
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.extends.string

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class CmdSetEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdSet? = null

    private var action1: View? = null
    private var action2: View? = null
    private var action3: View? = null
    private var action4: View? = null
    private var paramContent: ScriptSpanParamLayout? = null
    private var paramExpression: ScriptValueView? = null
    private var regexParam: ScriptSpanParamLayout? = null
    private var srcParam: ScriptValueView? = null
    private var systemParam: ScriptValueView? = null
    private var targetParam: ScriptValueView? = null

    override fun initView() {
        action1 = findViewById(R.id.action1)
        action2 = findViewById(R.id.action2)
        action3 = findViewById(R.id.action3)
        action4 = findViewById(R.id.action4)
        paramContent = findViewById(R.id.paramContent)
        paramExpression = findViewById(R.id.paramExpression)
        regexParam = findViewById(R.id.regexParam)
        srcParam = findViewById(R.id.srcParam)
        systemParam = findViewById(R.id.systemParam)
        targetParam = findViewById(R.id.targetParam)


        targetParam?.onValueChangedListener = object : ScriptValueView.OnValueChangedListener {

            override fun onValueChanged(text: String) {
                //@{main.param6},去除@{%s}格式,只保留main.param6
                cmd?.paramId = ScriptCommandHelper.parseParamsId(text)
            }
        }

        targetParam?.onMaskClickListener = OnClickListener {
            DialogParamsManager(context)
                .setWritable(true)
                .setParamListener(object :
                    DialogParamsManager.OnParamListener {
                    override fun onParamSelected(param: ScriptParam?) {
                        cmd?.paramId = param?.getFullId() ?: ""
                        cmd?.let { onBindCommand(it) }
                    }
                }).show()
        }
        systemParam?.onValueChangedListener = object : ScriptValueView.OnValueChangedListener {

            override fun onValueChanged(text: String) {
                cmd?.action1system =
                    ScriptSystemParam.fromValue(ScriptCommandHelper.parseParamsId(text))
            }
        }

        systemParam?.onMaskClickListener = OnClickListener {
            DialogParamsManager(context)
                .setSystemOnly(true)
                .setReadable(true)
                .setParamListener(object :
                    DialogParamsManager.OnParamListener {
                    override fun onParamSelected(param: ScriptParam?) {
                        cmd?.action1system = ScriptSystemParam.fromValue(param?.getFullId() ?: "")
                        cmd?.let { onBindCommand(it) }
                    }
                }).show()
        }

        paramContent?.addTextChangedListener(object : ScriptSpanParamLayout.ScriptTextWatcher {
            override fun afterTextChanged(s: String?) {
                cmd?.action2content = s ?: ""
            }
        })

        srcParam?.onValueChangedListener = object : ScriptValueView.OnValueChangedListener {

            override fun onValueChanged(text: String) {
                //@{main.param6},去除@{%s}格式,只保留main.param6
                cmd?.action3ParamId = ScriptCommandHelper.parseParamsId(text)
            }
        }

        srcParam?.onMaskClickListener = OnClickListener {
            DialogParamsManager(context)
                .setReadable(true)
                .setParamListener(object :
                    DialogParamsManager.OnParamListener {
                    override fun onParamSelected(param: ScriptParam?) {
                        cmd?.action3ParamId = param?.getFullId() ?: ""
                        cmd?.let { onBindCommand(it) }
                    }
                }).show()
        }

        regexParam?.addTextChangedListener(object : ScriptSpanParamLayout.ScriptTextWatcher {
            override fun afterTextChanged(s: String?) {
                cmd?.action3Regular = s ?: ""
            }
        })

        regexParam?.setQuickAction(com.hive.i8n.R.string.sc_quick_action_regex.string(), RegexActions)

        paramExpression?.onMaskClickListener = OnClickListener {
            DialogCalculator
                .show(context, cmd?.action4expression ?: "") {
                    cmd?.action4expression = it
                    cmd?.let { onBindCommand(it) }
                }
        }
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdSet?
        cmd?.run {
            updateActionLayout(this)
            targetParam?.setValue(ScriptCommandHelper.paramFormat.format(paramId))
            when (action) {
                CmdSet.SetAction.SYSTEM -> {
                    systemParam?.setValue(ScriptCommandHelper.paramFormat.format(action1system.paramId))
                }

                CmdSet.SetAction.CONTENT -> {
                    paramContent?.setText(action2content)
                }

                CmdSet.SetAction.REGULAR -> {
                    srcParam?.setValue(ScriptCommandHelper.paramFormat.format(action3ParamId))
                    regexParam?.setText(action3Regular)
                }

                CmdSet.SetAction.EXPRESSION -> {
                    paramExpression?.setValue(action4expression ?: "")
                }
            }
        }
    }

    private fun updateActionLayout(cmd: CmdSet) {
        when (cmd.action) {
            CmdSet.SetAction.SYSTEM -> {
                action1?.visibleOrGone(true)
                action2?.visibleOrGone(false)
                action3?.visibleOrGone(false)
                action4?.visibleOrGone(false)
            }

            CmdSet.SetAction.CONTENT -> {
                action1?.visibleOrGone(false)
                action2?.visibleOrGone(true)
                action3?.visibleOrGone(false)
                action4?.visibleOrGone(false)
            }

            CmdSet.SetAction.REGULAR -> {
                action1?.visibleOrGone(false)
                action2?.visibleOrGone(false)
                action3?.visibleOrGone(true)
                action4?.visibleOrGone(false)
            }

            CmdSet.SetAction.EXPRESSION -> {
                action1?.visibleOrGone(false)
                action2?.visibleOrGone(false)
                action3?.visibleOrGone(false)
                action4?.visibleOrGone(true)
            }
        }
    }


    override fun getEditContentId() = R.layout.cmd_set_edit_card

}