// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.params.ScriptParam
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.cmd.CmdCurl
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.views.dialog.DialogCommonPairValueInput
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.extends.string
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdCurlEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdCurl? = null

    private var methodSelector: ScriptTabSelectorView? = null
    private var paramBody: ScriptValueView? = null
    private var paramForm: ScriptValueView? = null
    private var paramHeader: ScriptValueView? = null
    private var paramTarget: ScriptValueView? = null
    private var requestUrl: ScriptValueView? = null

    override fun initView() {
        methodSelector = findViewById(R.id.methodSelector)
        paramBody = findViewById(R.id.paramBody)
        paramForm = findViewById(R.id.paramForm)
        paramHeader = findViewById(R.id.paramHeader)
        paramTarget = findViewById(R.id.paramTarget)
        requestUrl = findViewById(R.id.requestUrl)

        methodSelector?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd?.method = CmdCurl.Method.valueOf(p!!.second!!)
                    onBindCommand(cmd!!)
                }
            }
        requestUrl?.onMaskClickListener = OnClickListener {
            DialogCommonTextInput(context)
                .setSingleLine(true)
                .setTitle(
                    com.hive.i8n.R.string.sc_curl_url_edit_text_input_title.string()
                ).setHint(com.hive.i8n.R.string.sc_curl_url_edit_text_input_hint.string())
                .setText(cmd?.url ?: "")
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        cmd?.url = content
                        onBindCommand(cmd!!)
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }
        paramBody?.onMaskClickListener = OnClickListener {
            DialogCommonTextInput(context)
                .setSingleLine(false)
                .setTitle(
                    com.hive.i8n.R.string.sc_curl_body_edit_text_input_title.string()
                ).setHint(com.hive.i8n.R.string.sc_curl_body_edit_text_input_hint.string())
                .setActionMenuList(
                    mutableListOf(
                        ScriptSpanParamLayout.ActionMenuType.Copy,
                        ScriptSpanParamLayout.ActionMenuType.Paste,
                        ScriptSpanParamLayout.ActionMenuType.Clean,
                        ScriptSpanParamLayout.ActionMenuType.Format
                    )
                )
                .setText(cmd?.body ?: "")
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        cmd?.body = content
                        onBindCommand(cmd!!)
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }
        paramTarget?.onMaskClickListener = OnClickListener {
            DialogParamsManager(context)
                .setWritable(true)
                .setParamListener(object :
                    DialogParamsManager.OnParamListener {
                    override fun onParamSelected(param: ScriptParam?) {
                        cmd?.targetParamId =
                            ScriptParamEnv.getParam(param?.getFullId() ?: "")?.getFullId()
                        cmd?.let { onBindCommand(it) }
                    }
                }).show()
        }

        paramHeader?.onMaskClickListener = OnClickListener {
            DialogCommonPairValueInput(context)
                .setTitle(com.hive.i8n.R.string.sc_curl_header_edit_text_input_title.string())
                .setMapData(cmd?.headers ?: mapOf())
                .setOnCommonListener(object : DialogCommonPairValueInput.OnCommonListener {
                    override fun onSubmitted(map: Map<String, String>) {
                        cmd?.headers = map
                        onBindCommand(cmd!!)
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }

        paramForm?.onMaskClickListener = OnClickListener {
            DialogCommonPairValueInput(context)
                .setTitle(com.hive.i8n.R.string.sc_curl_form_edit_text_input_title.string())
                .setMapData(cmd?.form ?: mapOf())
                .setOnCommonListener(object : DialogCommonPairValueInput.OnCommonListener {
                    override fun onSubmitted(map: Map<String, String>) {
                        cmd?.form = map
                        onBindCommand(cmd!!)
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdCurl?
        cmd?.run {
            methodSelector?.setValue(method.value)
            requestUrl?.setValue(ScriptCommandHelper.getValueDisplayName(url))
            paramForm?.setValue(ScriptCommandHelper.getValueDisplayName(form))
            paramHeader?.setValue(ScriptCommandHelper.getValueDisplayName(headers))
            paramBody?.setValue(ScriptCommandHelper.getValueDisplayName(body))
            paramTarget?.setValue(ScriptCommandHelper.paramFormat.format(targetParamId))
        }
    }

    override fun checkCommandOrThrowError() {
        cmd?.run {
            //检查url格式,带url参数也合法，例如：http://www.baidu.com?name=123
//            val urlRegex =
//                Regex("^(http|https)://([\\w-]+\\.)+[\\w-]+(:\\d+)?(/[\\w- ./?%&=]*)?(\\?[\\w- ./?%&=]*)?(#[\\w- ./?%&=]*)?(@\\{[\\w.-]+\\})?$")
            if (url.isEmpty()) {
                throw IllegalArgumentException(com.hive.i8n.R.string.sc_curl_url_edit_text_empty.string())
            }
//            if (!urlRegex.matches(url)) {
//                throw IllegalArgumentException(com.hive.i8n.R.string.sc_curl_url_edit_text_error.string())
//            }
            if (targetParamId.isNullOrEmpty()) {
                throw IllegalArgumentException(com.hive.i8n.R.string.sc_curl_target_edit_text_empty.string())
            }
        }
    }


    override fun getEditContentId() = R.layout.cmd_curl_edit_card

}