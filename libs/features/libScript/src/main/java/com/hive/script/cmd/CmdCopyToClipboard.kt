// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.text.TextUtils
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptThreadManager
import com.hive.utils.system.ClipboardUtil
import com.hive.utils.thread.UIHandlerUtils
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.utils.StringUtils
import com.hive.views.widgets.CommonToast
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdCopyToClipboard, name = "copyToClipboard")
class CmdCopyToClipboard : ScriptCommand(), ScriptRegularInterface {
    var content: String? = null

    override fun onExecute(): CmdExecuteResult {
        val isSuccess = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        val doCopy = {
            try {
                val cxt = ScriptProvider.getViewContext()
                if (!TextUtils.isEmpty(content)) {
                    val msg = parseParamText(content)
                    ClipboardUtil.getInstance(cxt).copyText(getString(com.hive.i8n.R.string.sc_cpoy_tag), msg)
                    CommonToast.show(com.hive.i8n.R.string.sc_copy_success)
                    isSuccess.set(true)
                }
            } finally {
                latch.countDown()
            }
        }

        if (UIHandlerUtils.isOnMainThread()) {
            doCopy.invoke()
        } else {
            UIHandlerUtils.getInstance().executeInMainThread { doCopy.invoke() }
            latch.await(5, TimeUnit.SECONDS)
        }

        ScriptThreadManager.delay(getCommandDuration())
        return if (isSuccess.get()) {
            CmdExecuteResult.success()
        } else {
            CmdExecuteResult.failure(getString(com.hive.i8n.R.string.sc_copy_fail))
        }
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommandName() = getString(com.hive.i8n.R.string.cmd_copy_name)

    override fun getCommandDescribe() = getString(com.hive.i8n.R.string.cmd_copy_des)

    override fun getCommandIcon() = R.drawable.ic_copy

    override fun getCommand() = "${cmdPrefix()} content=\"${content?.encode()}\""

    override fun parseCmd(cmd: String) {
        content = ScriptLineTokenizer.parseKeyValueParams(cmd)["content"]?.decode()
    }

    override fun getPermissionRequest() = null

    companion object {
        fun createCommand(content: String?) = CmdCopyToClipboard().apply {
            this.content = content
        }
    }
}