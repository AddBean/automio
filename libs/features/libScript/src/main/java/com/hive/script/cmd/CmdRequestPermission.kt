// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.utils.ScriptHelper
import com.hive.script.utils.ScriptPermissionManager
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import java.util.concurrent.atomic.AtomicBoolean
import com.hive.script.base.core.ScriptLineTokenizer

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdRequestPermission, name = "requestPermission")
class CmdRequestPermission : ScriptCommand(), ScriptRegularInterface {

    var permission: String? = null

    override fun onExecute(): CmdExecuteResult {
        val isWaiting = AtomicBoolean(true)
        var isSuccess = false
        val timeoutMillis = 10000L // 10秒超时
        val startTime = System.currentTimeMillis()
        
        ScriptHelper.runInMain {
            if ("android.permission.CAPTURE" == permission) {
                ScriptPermissionManager.requestRecordingPermission(
                    GlobalApp.getAvailableActivity(),
                    {
                        isSuccess = true
                        isWaiting.set(false)
                    },
                    {
                        isSuccess = false
                        isWaiting.set(false)
                    })
            } else {
                ScriptPermissionManager.requestCommonPermission(
                    GlobalApp.getAvailableActivity(),
                    permission!!, {
                        isSuccess = true
                        isWaiting.set(false)
                    }, {
                        isSuccess = false
                        isWaiting.set(false)
                    })
            }

        }
        
        // 添加超时逻辑
        while (isWaiting.get()) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - startTime > timeoutMillis) {
                // 超时，直接返回失败
                isWaiting.set(false)
                isSuccess = false
                break
            }
            ScriptThreadManager.delay(500)
        }
        
        ScriptThreadManager.delay(1000)
        return if (isSuccess) {
            CmdExecuteResult.success(GlobalApp.getString(com.hive.i8n.R.string.sc_permission_already_granted))
        } else {
            CmdExecuteResult.failure(GlobalApp.getString(com.hive.i8n.R.string.sc_permission_grant_failed))
        }
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_request_permission)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_request_permission)

    override fun getCommand() = "${cmdPrefix()} permission=\"${permission?.encode()}\""

    override fun parseCmd(cmd: String) {
        permission = ScriptLineTokenizer.parseKeyValueParams(cmd)["permission"]?.decode()
    }

    override fun getPermissionRequest() = null

    override fun getCommandIcon() = R.drawable.sc_icon_toast

    companion object {
        fun createCommand(permission: String) = CmdRequestPermission().apply {
            this.permission = permission
        }
    }
}