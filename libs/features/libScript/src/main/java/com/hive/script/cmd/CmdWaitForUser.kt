// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.views.tips.ScriptWaitingForOperateView
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.thread.UIHandlerUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdWaitForUser, name = "waitForUser")
class CmdWaitForUser : ScriptCommand(), ScriptRegularInterface {

    var dialogTitle: String? = null

    var dialogMessage: String? = null

    var dialogConfirmBtnText: String? = ""

    var dialogCancelBtnText: String? = ""

    var dialogCountDown: Int = -1

    var dialogShowing: AtomicBoolean = AtomicBoolean(false)
    private var hasCallbacked = false

    override fun onExecute(): CmdExecuteResult {
        var confirmed = false
        var overExceeded = false
        hasCallbacked = false
        dialogShowing.set(true)
        UIHandlerUtils.getInstance().post {
            showWaitForOperateDialog { isConfirmed, isOverExceeded ->
                confirmed = isConfirmed
                overExceeded = isOverExceeded
                dialogShowing.set(false)
            }
        }
        while (dialogShowing.get()) {
            ScriptThreadManager.delay(1000)
        }

        return if (overExceeded) {
            CmdExecuteResult.success("overExceeded", GlobalApp.getString(com.hive.i8n.R.string.script_command_execute_may_success))
        } else {
            if (confirmed) {
                CmdExecuteResult.success("confirmed", GlobalApp.getString(com.hive.i8n.R.string.script_command_execute_success))
            } else {
                CmdExecuteResult.success("canceled", GlobalApp.getString(com.hive.i8n.R.string.script_command_execute_may_success))
            }
        }
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommandName() = getString(com.hive.i8n.R.string.cmd_wait_for_operate_name)

    override fun getCommandDescribe() = getString(com.hive.i8n.R.string.cmd_wait_for_operate_des)

    override fun getCommandIcon() = R.drawable.sc_ic_dialogue

    override fun getCommand(): String {
        fun q(s: String?) = when {
            s.isNullOrBlank() -> ""
            else -> s.encode()
        }
        return "${cmdPrefix()} title=${q(dialogTitle)} message=${q(dialogMessage)} confirmBtn=${q(dialogConfirmBtnText)} cancelBtn=${q(dialogCancelBtnText)} countDown=$dialogCountDown"
    }

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        dialogTitle = p["title"]?.decode()?.takeIf { it.isNotBlank() }
        dialogMessage = p["message"]?.decode()?.takeIf { it.isNotBlank() }
        dialogConfirmBtnText = p["confirmBtn"]?.decode()?.takeIf { it.isNotBlank() }
        dialogCancelBtnText = p["cancelBtn"]?.decode()?.takeIf { it.isNotBlank() }
        dialogCountDown = p["countDown"]?.toIntOrNull() ?: -1
    }

    override fun getPermissionRequest() = null

    private fun showWaitForOperateDialog(onConfirm: (isConfirmed: Boolean, overExceeded: Boolean) -> Unit) {
        val alert = ScriptWaitingForOperateView(ScriptProvider.getViewContext())
        alert.setTitleText(dialogTitle ?: GlobalApp.getString(com.hive.i8n.R.string.cmd_wait_for_operate_name))
        alert.setMsgText(dialogMessage ?: GlobalApp.getString(com.hive.i8n.R.string.cmd_wait_for_operate_des))
        alert.setSubmitText(dialogConfirmBtnText ?: GlobalApp.getString(com.hive.i8n.R.string.confirm))
        alert.setCancelText(dialogCancelBtnText ?: GlobalApp.getString(com.hive.i8n.R.string.sc_opt_cancel))
        alert.setCountDown(dialogCountDown)
        
        alert.setSubmitClickListener { dialog ->
            hasCallbacked = true
            onConfirm.invoke(true, false)
            dialog.dismiss()
        }
        
        alert.setCancelClickListener { dialog ->
            hasCallbacked = true
            onConfirm.invoke(false, false)
            dialog.dismiss()
        }
        
        // 监听对话框关闭事件，处理倒计时超时
        alert.setOnDismissListener {
            // 如果倒计时结束，触发超时回调
            if (dialogCountDown > 0 && !hasCallbacked) {
                onConfirm.invoke(false, true)
            }
        }
        
        alert.show()
    }

    companion object {
        fun createCommand(
            title: String?,
            message: String?,
            confirmBtn: String?,
            cancelBtn: String?,
            countDown: Int?
        ) =
            CmdWaitForUser().apply {
                this.dialogTitle = title ?: GlobalApp.getString(com.hive.i8n.R.string.cmd_wait_for_operate_name)
                this.dialogMessage = message?: GlobalApp.getString(com.hive.i8n.R.string.cmd_wait_for_operate_des)
                this.dialogConfirmBtnText = confirmBtn?: GlobalApp.getString(com.hive.i8n.R.string.confirm)
                this.dialogCancelBtnText = cancelBtn?: GlobalApp.getString(com.hive.i8n.R.string.sc_opt_cancel)
                this.dialogCountDown = countDown ?: -1
            }
    }
} 