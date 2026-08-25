// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.blankj.utilcode.util.VibrateUtils
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.ScriptScreenShotService
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptMate
import com.hive.script.base.ScriptRecordHelper
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.core.ScriptSaver
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.cmd.CmdDelay
import com.hive.script.cmd.CmdToast
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.driver.ServiceAccessibility
import com.hive.script.extensions.traverseCommand
import com.hive.script.setting.ScriptSetting
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.dialog.DialogInputMessage
import com.hive.script.views.dialog.DialogPlayTip
import com.hive.script.views.dialog.DialogSaveTaskConfirm
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.script.views.edit.xeditor.XCellLayout
import com.hive.script.views.menu.ScriptControlView
import com.hive.script.views.record.ScriptRecordContainerView
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.debug.DLog
import com.hive.utils.GlobalApp
import com.hive.utils.system.CommonUtils
import com.hive.views.widgets.CommonToast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat

/**
 *
 * @author jiadou
 * @date 6/10/21
 */

object ScriptManager {

    fun ctlControlView(
        action: String
    ) {
        when (action) {
            "stop" -> {
                ScriptControlView.get()?.stopRecord()
            }

            "pause" -> {
                ScriptControlView.get()?.pauseRecord()
            }

            "start" -> {
                ScriptControlView.get()?.startRecord()
            }
        }
    }

    /**
     * 是否未对齐？
     */
    fun isNeedUpdate(): Boolean {
        val top = ScriptRecordContainerView.get()?.getRealMarginTop()
        val left = ScriptRecordContainerView.get()?.getRealMarginLeft()
        if (top != null && left != null) {
            val lp = ScriptRecordContainerView.get()?.layoutParams as WindowManager.LayoutParams
            if (lp.x != -left || lp.y != -top) {
                return true
            }
        }
        return false
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun tryStart() {
        if (checkAccessibility()) return
        if (!isEditPanelShowing()) {
            //打开失败
            if (!start()) {
                GlobalScope.launch(Dispatchers.Main) {
                    delay(100)
                    start()
                }
            } else {
                ScriptMenuManager.switchMenuMode(ScriptControlView.MenuMode.MAIN_MENU)
            }
        } else {
            DialogScriptEdit.getEditDialog()?.getMiniEditView()?.startWarningAnim()
            CommonToast.getInstance().showToast(com.hive.i8n.R.string.sc_waring_edit_panel_open)
        }
    }

    fun isEditPanelShowing(): Boolean {
        return DialogScriptEdit.isShowing()
    }


    fun start(systemServerStart: Boolean = false): Boolean {
        if (checkAccessibility()) return false
        if (!checkServerEnable()) return false
        if (systemServerStart) {
            val intentToResolve =
                Intent(GlobalApp.getApp(), GlobalApp.getMainActivityClass()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            GlobalApp.getApp().startActivity(intentToResolve)
        }
        ScriptRecordManager.ensureRecordViewAdded()
        ScriptMenuManager.ensureMenuViewAdded()
        ScriptRecordManager.hiddenRecordView()
        if (!DialogScriptEdit.isShowing()) {
            ScriptMenuManager.showMenuView()
        }
        if (ScriptSetting.script_setting_running_tips_switch) {
            VibrateUtils.vibrate(100L)
        }
        ScriptHelper.runInMain({ updateViewLayout() }, 200L)
        return true
    }


    private fun showCreateNameDialog(context: Context, cb: ((name: String) -> Unit?)?) {
        val targetName = generateNewSaveName()
        DialogInputMessage(
            context,
            GlobalApp.getString(com.hive.i8n.R.string.str_task_name),
            GlobalApp.getString(com.hive.i8n.R.string.sc_dialog_name_hint),
            targetName,
            0,
            { edit_text ->
                val saveName = edit_text.text.toString()
                if (TextUtils.isEmpty(saveName)) {
                    throw Exception(GlobalApp.getString(com.hive.i8n.R.string.sc_check_input_check_empty))
                }
                if (saveName.length > 50) {
                    throw Exception(GlobalApp.getString(com.hive.i8n.R.string.sc_check_input_check_empty_3))
                }
                if (File(ScriptConst.Save_Script_Path + "/" + saveName + "/").exists()) {
                    throw Exception(GlobalApp.getString(com.hive.i8n.R.string.sc_check_input_check_empty_4))
                }
            },
            { dialog, name ->
                cb?.invoke(name)
                dialog.dismiss()
            }).show()
    }

    /**
     * 命名规则：未命名2024.3.2.1230_1，即未命名+年月日时分秒+序号
     */
    @SuppressLint("SimpleDateFormat")
    fun generateNewSaveName(prox: String? = null): String {
        val namePrx = prox ?: ScriptConst.Default_file_Name
        val targetName = "$namePrx-";
        val time = System.currentTimeMillis()
        val date = SimpleDateFormat("yyyy.MM.dd").format(time)
        var name = "$targetName${date}"
        var count = 0
        var file = File(ScriptConst.Save_Script_Path + name)
        val tempName = name
        while (file.exists()) {
            count++
            name = tempName + "_" + count
            file = File(ScriptConst.Save_Script_Path + name)
        }
        return name
    }

    fun showSaveDialog(
        root: ScriptCommandRoot,
        layout: XCellLayout?,
        cb: ((success: Boolean) -> Unit?)?
    ) {
        val menuView = ScriptMenuManager.getMenuView()
        showCreateNameDialog(menuView?.getWindowContext()!!) { name ->
            ScriptSaver.saveToLocalWithLoading(name, root, layout) {
                cb?.invoke(true)
                DialogSaveTaskConfirm(
                    context = menuView.getWindowContext(), name
                ) {
                    ScriptHelper.runInMain({
                        val targetPath =
                            "${ScriptConst.Save_Script_Path}/${name}"
                        startPlay(
                            targetPath,
                            showPlayDialog = false,
                            enableAutoUnlock = false
                        )
                    }, 500)

                }.apply {
                    setName(name)
                }.show()
            }
        }
    }

    fun createScriptDialog(context: Context, cb: ((path: String) -> Unit)?) {
        showCreateNameDialog(context) { name ->
            val script = ScriptCommandRoot()
            script.addCommandQueue(CmdDelay.createCommand(ScriptConst.Cmd_Default_Delay))
            ScriptSaver.saveToLocalWithLoading(name, script, null) {
                val targetPath =
                    "${ScriptConst.Save_Script_Path}${name}/"
                cb?.invoke(targetPath)
            }
        }
    }

    fun createTaggedTestScripts(
        tag: String,
        prefix: String,
        count: Int = 5,
        cb: ((paths: List<String>) -> Unit)? = null
    ) {
        val safeTag = tag.trim()
        val safePrefix = prefix.trim()
        if (safeTag.isEmpty() || safePrefix.isEmpty()) {
            cb?.invoke(emptyList())
            return
        }
        val total = count.coerceIn(1, 20)
        val paths = mutableListOf<String>()

        fun buildScript(index: Int): ScriptCommandRoot {
            val order = index + 1
            return ScriptCommandRoot().apply {
                scriptMate = ScriptMate().apply {
                    this.tag = safeTag
                }
                addCommandQueue(CmdDelay.createCommand(300L * order, 300L * order))
                addCommandQueue(CmdToast.createCommand().apply {
                    content = "$safeTag-$order"
                })
            }
        }

        fun createNext(index: Int) {
            if (index >= total) {
                cb?.invoke(paths)
                return
            }
            val saveName = generateNewSaveName("$safePrefix-${index + 1}")
            ScriptSaver.saveToLocalNoLoading(saveName, buildScript(index), null, null) {
                paths.add("${ScriptConst.Save_Script_Path}/$saveName/")
                createNext(index + 1)
            }
        }

        createNext(0)
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun startPlay(
        path: String? = null,
        showPlayDialog: Boolean = true,
        enableAutoUnlock: Boolean = ScriptSetting.script_setting_auto_unlock
    ) {
        if (checkAccessibility()) return
        GlobalScope.launch(Dispatchers.Main) {
            //如果需要解锁屏幕，先解锁屏幕
            if (enableAutoUnlock
                && ScriptSetting.script_setting_auto_unlock
                && ScriptEventHelper.get().checkIfNeedUnLockScreen()
            ) {
                tryUnlockScreenInWorkThread()
            }
            val cmdRoot = if (path != null) {
                ScriptCommandRoot().apply { ScriptCommandRoot.loadScript(path, this) }
            } else {
                ScriptRecordHelper.instance.rootCommand
            }
            cmdRoot.traverseCommand {
                it.enableAutoUnlock = enableAutoUnlock
            }
            if (ScriptSetting.script_setting_running_tips_switch && showPlayDialog) {
                DialogPlayTip(ScriptProvider.getViewContext()).loadCmd(cmdRoot) { _, step, count ->
                    ScriptRecordManager.hiddenRecordView()
                    ScriptHelper.checkPermissionAndRights(cmdRoot) {
                        startPlayInternal(step, count, cmdRoot)
                    }
                }.show()
            } else {
                ScriptRecordManager.hiddenRecordView()
                ScriptHelper.checkPermissionAndRights(cmdRoot) {
                    startPlayInternal(0, 1, cmdRoot)
                }
            }
        }
    }

    private suspend fun tryUnlockScreenInWorkThread() {
        return withContext(Dispatchers.IO) {
            ScriptEventHelper.get().wakeScreen()
            delay(500)
            try {
                ScriptInterpreter.getUnlockScript()?.doExecute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(1000)
        }
    }

    private fun startPlayInternal(step: Int, count: Int, cmdRoot: ScriptCommandRoot) {
        if (checkAccessibility()) return
        DLog.d("ScriptManager", "startPlayInternal() step=$step, count=$count")
        ScriptConst.scriptStepIndex = step
        ScriptConst.scriptLoopCount = count
        ScriptMenuManager.getMenuView()?.switchControlMode(ScriptControlView.MenuMode.PLAYING_MENU)
        ScriptRecordManager.updateRecordView(
            ScriptRecordViewManager.ViewState.default()
        )
        ensurePlayStop()
        ScriptInterpreter.getDefault().executeCommand(cmdRoot, false)
    }

    fun stopPlay() {
        DLog.w("ScriptManager", "stopPlay() called")
        val menuView = ScriptMenuManager.getMenuView()
        menuView?.switchControlMode(ScriptControlView.MenuMode.MAIN_MENU)
        menuView?.stopPlaybackProgress()
        ensurePlayStop()
        ScriptThreadManager.stopAll()
        ScriptRecordManager.hiddenRecordView()
        ScriptMenuManager.showMenuView()
        // 工作流结束即停止截屏服务，释放 MediaProjection，避免持续占用屏幕
//        if (ScriptScreenShotService.instance != null) {
//            ScriptScreenShotService.stop(ScriptProvider.getViewContext())
//        }
    }

    fun ensurePlayStop() {
        ScriptInterpreter.getDefault().stopExecute()
    }

    fun pauseOrResumePlay(pause: Boolean) {
        val menuView = ScriptMenuManager.getMenuView()
        if (pause) {
            menuView?.pausePlaybackProgress()
            ScriptThreadManager.pause()
        } else {
            menuView?.startPlaybackProgress()
            ScriptThreadManager.resume()
        }
    }

    fun addAndExecuteCommand(cmd: ScriptCommand, onFinished: (() -> Unit)? = null) {
        ScriptRecordHelper.instance.addCommand(cmd)
        ScriptInterpreter.getDefault().executeCommand(cmd, true,onFinished)
        ScriptRecordManager.getRecordInnerView()?.resetDataView()
        ScriptRecordManager.getRecordInnerView()?.invalidate()
    }

    fun rollBackRecordCommand(): Boolean {
        return ScriptRecordHelper.instance.removeLastCommand()
    }

    private fun executeCommand(
        cmd: ScriptCommandRoot = ScriptRecordHelper.instance.rootCommand, showDialog: Boolean = true
    ) {
        if (showDialog && ScriptSetting.script_setting_running_tips_switch) {
            DialogPlayTip(ScriptProvider.getViewContext()).loadCmd(cmd) { _, step, count ->
                ScriptRecordManager.hiddenRecordView()
                ScriptHelper.checkPermissionAndRights(cmd) {
                    startPlayInternal(step, count, cmd)
                }
            }.show()
        } else {
            ScriptRecordManager.hiddenRecordView()
            ScriptHelper.checkPermissionAndRights(cmd) {
                startPlayInternal(0, cmd.replayTimes, cmd)
            }
        }
    }


    fun getLoggerView() = ScriptMenuManager.getMenuView()?.getLoggerView()

    fun updateViewLayout() {
        val p = ScriptRecordContainerView.generateLayoutParams()
        ScriptRecordContainerView.get()?.post {
            getWindowManager()?.updateViewLayout(ScriptRecordContainerView.get(), p)
        }
    }

    fun getWindowManager(): WindowManager? {
        return ScriptProvider.getViewContext()
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager?
    }

    fun releaseAllViews() {
        ScriptRecordContainerView.get()?.release()
        ScriptControlView.get()?.release()
    }

    fun checkServerEnable(): Boolean {
        return CommonUtils.isAccessibilitySettingsOn(
            GlobalApp.getContext(), ServiceAccessibility::class.java.name
        ) && ScriptProvider.isServiceReady()
    }

    fun onBackPressed(): Boolean {
        return BaseScriptDialog.onBackPress()
    }

    fun checkAccessibility(submitCallback: ((isCancel: Boolean) -> Unit)? = null): Boolean {
        val isOn = CommonUtils.isAccessibilitySettingsOn(
            GlobalApp.getContext(), ServiceAccessibility::class.java.name
        )
        if (!isOn) {
            DialogScriptAlert(ScriptProvider.getViewContext())
                .setTitle(com.hive.i8n.R.string.sc_need_acc_title)
                .setContent(
                    GlobalApp.getString(
                        com.hive.i8n.R.string.sc_need_acc_content
                    )
                )
                .setContentGravity(Gravity.LEFT)
                .setConfirmText(com.hive.i8n.R.string.sc_need_acc_confirm)
                .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                    override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                        dialog.dismiss()
                        if (!isCancel) {
                            ScriptProvider.startToAccessibilitySetting()
                        }
                        submitCallback?.invoke(isCancel)
                    }
                }).show()
        }
        return !isOn
    }

    fun getRunningScript(): ScriptCommandRoot? {
        return ScriptInterpreter.getDefault().getRunningScript()
    }

    fun hiddenAllViews() {
        ScriptRecordManager.saveViewState()
        ScriptMenuManager.saveMenuState()
        ScriptRecordManager.hiddenRecordView()
        ScriptMenuManager.hiddenMenuView()
    }

    fun restoreAllViews() {
        ScriptRecordManager.restoreViewState()
        ScriptMenuManager.restoreMenuState()
    }

    fun isUnlockScriptExist(): Boolean {
        return File(ScriptConst.Task_Screen_Lock_Script_Main_Path).exists()
    }

}
