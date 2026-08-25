// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.views.dialog.DialogCmdDialogConfirm
import com.hive.script.views.dialog.DialogCmdDialogConfirm.OnDialogEventListener
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.thread.UIHandlerUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdDialog, name = "dialog")
class CmdDialog : ScriptCommand(), ScriptRegularInterface {

    var dialogTitle: String? = null

    var dialogMessage: String? = null

    var dialogConfirmBtnText: String? = null

    var dialogCancelBtnText: String? = null

    var dialogCountDown: Int = -1

    var dialogImage: String? = null

    var dialogShowing: AtomicBoolean = AtomicBoolean(false)

    override fun onExecute(): CmdExecuteResult {
        var confirmed = false
        var overExceeded = false
        dialogShowing.set(true)
        UIHandlerUtils.getInstance().post {
            showConfirmDialog { isConfirmed, isOverExceeded ->
                confirmed = isConfirmed
                overExceeded = isOverExceeded
                dialogShowing.set(false)
            }
        }
        while (dialogShowing.get()) {
            ScriptThreadManager.delay(1000)
        }

        return if (overExceeded) {
            CmdExecuteResult.success("overExceeded", GlobalApp.getString(com.hive.i8n.R.string.dialog_wait_timeout))
        } else {
            if (confirmed) {
                CmdExecuteResult.success("confirmed", GlobalApp.getString(com.hive.i8n.R.string.dialog_user_confirmed))
            } else {
                CmdExecuteResult.success("canceled", GlobalApp.getString(com.hive.i8n.R.string.dialog_user_canceled))
            }
        }
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommandName() = getString(com.hive.i8n.R.string.cmd_dialog_confirm_name)

    override fun getCommandDescribe() = getString(com.hive.i8n.R.string.cmd_dialog_confirm_des)

    override fun getCommandIcon() = R.drawable.sc_ic_dialogue

    override fun getCommand(): String {
        fun q(s: String?) = when {
            s.isNullOrBlank() -> ""
            else -> s.encode()
        }
        val imagePart = if (dialogImage.isNullOrBlank()) "" else " image=${q(dialogImage)}"
        return "${cmdPrefix()} title=${q(dialogTitle)} message=${q(dialogMessage)}${imagePart} confirmBtn=${q(dialogConfirmBtnText)} cancelBtn=${q(dialogCancelBtnText)} countDown=$dialogCountDown"
    }

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        dialogTitle = p["title"]?.decode()?.takeIf { it.isNotBlank() }
        dialogMessage = p["message"]?.decode()?.takeIf { it.isNotBlank() }
        dialogConfirmBtnText = p["confirmBtn"]?.decode()?.takeIf { it.isNotBlank() }
        dialogCancelBtnText = p["cancelBtn"]?.decode()?.takeIf { it.isNotBlank() }
        dialogCountDown = p["countDown"]?.toIntOrNull() ?: -1
        dialogImage = p["image"]?.decode()?.takeIf { it.isNotBlank() }
    }

    override fun getPermissionRequest() = null

    private fun showConfirmDialog(onConfirm: (isConfirmed: Boolean, overExceeded: Boolean) -> Unit) {
        val alert = DialogCmdDialogConfirm(ScriptProvider.getViewContext())
        alert.setTitle(dialogTitle ?: GlobalApp.getString(com.hive.i8n.R.string.dialog_default_title))
        alert.setContent(dialogMessage ?: GlobalApp.getString(com.hive.i8n.R.string.dialog_default_message))
        val imageList = dialogImage?.split("|")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        alert.setImages(imageList)
        alert.setConfirmText(dialogConfirmBtnText ?: GlobalApp.getString(com.hive.i8n.R.string.dialog_default_confirm))
        alert.setCancelText(dialogCancelBtnText ?: GlobalApp.getString(com.hive.i8n.R.string.dialog_default_cancel))
        alert.setCountDown(dialogCountDown)
        alert.setOnDialogEventListener(object : OnDialogEventListener {
            override fun onClickEvent(
                dialog: DialogCmdDialogConfirm,
                isCancel: Boolean,
                overExceeded: Boolean
            ) {
                onConfirm.invoke(!isCancel, overExceeded)
            }
        })
        alert.show()
    }

    companion object {
        fun createCommand(
            title: String?,
            message: String?,
            image: String?,
            confirmBtn: String?,
            cancelBtn: String?,
            countDown: Int?
        ) =
            CmdDialog().apply {
                this.dialogTitle = title?: GlobalApp.getString(com.hive.i8n.R.string.dialog_default_title)
                this.dialogMessage = message?: GlobalApp.getString(com.hive.i8n.R.string.dialog_default_message)
                this.dialogImage = image?.takeIf { it.isNotBlank() }
                this.dialogConfirmBtnText = confirmBtn?: GlobalApp.getString(com.hive.i8n.R.string.dialog_default_confirm)
                this.dialogCancelBtnText = cancelBtn?: GlobalApp.getString(com.hive.i8n.R.string.dialog_default_cancel)
                this.dialogCountDown = countDown ?: -1
            }
    }
}
