// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.manager

import com.hive.script.ActivityRequestPermissionCapture
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.ScriptScreenShotService
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.setting.ScriptSetting
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.script.views.menu.ScriptControlView
import com.hive.utils.GlobalApp
import com.hive.utils.thread.UIHandlerUtils
import com.hive.views.DialogAlertHelper
import com.hive.views.widgets.CommonToast

object ScriptEditRunningManager {

    private var settingRunningMenuSaveState = ScriptSetting.script_setting_running_menu_on

    fun runCommand(
        dialogView: DialogScriptEdit?,
        cmd: ScriptCommand?,
        shouldExecuteNext: Boolean = false
    ) {
        cmd ?: return
        var executeCmd = cmd
        if (shouldExecuteNext) {
            executeCmd = copyCommandFollowQueue(cmd)
            if (executeCmd == null) {
                return
            }
        }
        dialogView?.hidden(true)
        dialogView?.post {
            confirmExecuteCheckPermission(executeCmd, {
                ScriptMenuManager.showMenuView()
                ScriptMenuManager.switchMenuMode(ScriptControlView.MenuMode.PLAYING_MENU)
                ScriptInsertManager.cleanInsertListener()
                val observer = object :
                    ScriptInterpreterObserver.InterpreterExecuteObserver {
                    override fun onInterpreterEnd(cmd: ScriptCommand) {
                        onEndRunning()
                        ScriptInterpreterObserver.unRegisterInterpreterObserver(this)
                        UIHandlerUtils.getInstance().postDelayed({
                            CommonToast.show(com.hive.i8n.R.string.sc_runing_edit_end)
                            ScriptMenuManager.hiddenMenuView()
                            ScriptMenuManager.switchMenuMode(ScriptControlView.MenuMode.MAIN_MENU)
                            dialogView.restore(true)
                        }, 200)
                    }
                }
                ScriptInterpreterObserver.registerInterpreterObserver(observer)
                onStartRunning()
                UIHandlerUtils.getInstance().postDelayed({
                    ScriptInterpreter.getDefault().executeCommand(executeCmd, isRecording = false)
                }, 300)
            }, {
                //取消
                dialogView.restore(true)
            })

        }
    }


    private fun onStartRunning() {
        settingRunningMenuSaveState = ScriptSetting.script_setting_running_menu_on
        ScriptSetting.script_setting_running_menu_on = true
    }

    private fun onEndRunning() {
        //恢复运菜单状态
        ScriptSetting.script_setting_running_menu_on = settingRunningMenuSaveState
    }

    /**
     * 确认执行命令时检查权限
     */
    private fun confirmExecuteCheckPermission(
        cmd: ScriptCommand,
        confirmed: () -> Unit,
        canceled: () -> Unit
    ) {
        DialogAlertHelper.showDialog(
            ScriptProvider.getViewContext(),
            GlobalApp.getString(com.hive.i8n.R.string.sc_execute_title),
            GlobalApp.getString(com.hive.i8n.R.string.sc_execute_content),
            GlobalApp.getString(com.hive.i8n.R.string.sc_execute_left_text),
            GlobalApp.getString(com.hive.i8n.R.string.sc_execute_right_text),
            object : DialogAlertHelper.OnDialogListener {
                override fun onItemClick(
                    dialog: DialogAlertHelper.DialogTipsInterface,
                    isRight: Boolean
                ) {
                    dialog.dismiss()
                    if (isRight) {
                        if (isNeedRequestPermission(cmd)) {
                            requestPermission({
                                CommonToast.show(com.hive.i8n.R.string.sc_permission_snap_screen_failure)
                                canceled.invoke()
                            }, {
                                confirmed.invoke()
                            })
                        } else {
                            confirmed.invoke()
                        }
                    } else {
                        canceled.invoke()
                    }
                }
            }
        )
    }

    /**
     * 是否需要截图权限
     */
    private fun isNeedRequestPermission(cmd: ScriptCommand): Boolean {
        return ScriptHelper.getRequiredPermissions(cmd).map { it.first }
            .contains(ScriptHelper.PERMISSION_CAPTURE)
    }

    fun requestPermission(onRequestFailed: () -> Unit, onRequestSuccess: () -> Unit) {
        val activity = GlobalApp.getAvailableActivity()
        if (ScriptScreenShotService.instance == null) {
            ActivityRequestPermissionCapture.checkOrRequestPermission(activity, true, {
                UIHandlerUtils.getInstance().postDelayed({
                    onRequestSuccess.invoke()
                }, 300)
            }, {
                onRequestFailed.invoke()
                CommonToast.show(com.hive.i8n.R.string.sc_permission_snap_screen_failure)
            })
        } else {
            UIHandlerUtils.getInstance().postDelayed({
                onRequestSuccess.invoke()
            }, 300)
        }
    }


    /**
     * 将cmd和其后的命令复制到ScriptCommandRoot的队列中。
     * 继承 source root 的 scriptMate 和 scriptPath，保证编辑态运行时 scopeId 与 getScriptBasePath 正确。
     */
    private fun copyCommandFollowQueue(cmd: ScriptCommand?): ScriptCommandRoot? {
        cmd ?: return null
        val sourceRoot = cmd.getRootScript() ?: (cmd as? ScriptCommandRoot)
        val cmdRoot = ScriptCommandRoot()
        if (cmd is ScriptCommandRoot) {
            cmd.commandQueue.forEach {
                cmdRoot.addCommandQueue(it.deepCopy())
            }
        } else {
            val queue = cmd.parentCommand?.commandQueue ?: return null
            val index = queue.indexOf(cmd)
            if (index != -1) {
                for (i in index until queue.size) {
                    cmdRoot.addCommandQueue(queue[i].deepCopy())
                }
            }
        }
        sourceRoot?.let {
            cmdRoot.scriptPath = it.scriptPath
            cmdRoot.scriptMate = it.scriptMate?.copy()
        }
        return cmdRoot
    }

    interface OnInsertListener {

        fun onHandleInsertCommand(cmdInsert: ScriptCommand?) {
            cmdInsert?.startDelay = ScriptConst.Cmd_Delay_Default
            cmdInsert?.endDelay = ScriptConst.Cmd_Delay_Default
            onInsertCommand(cmdInsert)
        }

        fun onPickImage(image: String?) {}
        fun onInsertCommand(cmdInsert: ScriptCommand?) {}

        fun onInsertCancel()
    }
}