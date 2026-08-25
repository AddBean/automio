// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.params.ScriptParam
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.cmd.CmdDownload
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.dialog.DialogParamsManager
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
class CmdDownloadEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdDownload? = null

    private var downloadUrl: ScriptValueView? = null
    private var gallerySelector: ScriptTabSelectorView? = null
    private var paramTarget: ScriptValueView? = null

    override fun initView() {

        downloadUrl = findViewById(R.id.downloadUrl)
        gallerySelector = findViewById(R.id.gallerySelector)
        paramTarget = findViewById(R.id.paramTarget)

        gallerySelector?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd?.saveToGallery = p!!.second!!.toString().toBoolean()
                    onBindCommand(cmd!!)
                }
            }

        downloadUrl?.onMaskClickListener = OnClickListener {
            DialogCommonTextInput(context)
                .setSingleLine(true)
                .setTitle(
                    com.hive.i8n.R.string.sc_curl_dowload_edit_text_input_title.string()
                ).setHint(com.hive.i8n.R.string.sc_download_url_edit_text_input_hint.string())
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
        paramTarget?.onMaskClickListener = OnClickListener {
            DialogParamsManager(context)
                .setWritable(true)
                .setParamListener(object :
                    DialogParamsManager.OnParamListener {
                    override fun onParamSelected(param: ScriptParam?) {
                        cmd?.saveParamId =
                            ScriptParamEnv.getParam(param?.getFullId() ?: "")?.getFullId()
                        cmd?.let { onBindCommand(it) }
                    }
                }).show()
        }


    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdDownload?
        cmd?.run {
            downloadUrl?.setValue(ScriptCommandHelper.getValueDisplayName(url))
            paramTarget?.setValue(ScriptCommandHelper.paramFormat.format(saveParamId))
            gallerySelector?.setValue(saveToGallery.toString())
        }
    }

    override fun checkCommandOrThrowError() {
        cmd?.run {
            if (url.isEmpty()) {
                throw IllegalArgumentException(com.hive.i8n.R.string.sc_download_url_edit_text_empty.string())
            }
        }
    }


    override fun getEditContentId() = R.layout.cmd_download_edit_card

}