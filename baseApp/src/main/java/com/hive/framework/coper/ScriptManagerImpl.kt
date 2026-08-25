// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.framework.coper

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.ScriptProvider
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.driver.ServiceAccessibility
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.views.ScriptManagerLayoutForFrame
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.schedule.ScriptFragmentSchedule
import com.hive.utils.thread.UIHandlerUtils

object ScriptManagerImpl {

    fun initScreen() {
        ScriptCoordinateAdapter.get().initScreen()
    }

    fun isPanelShowing(): Boolean {
        return ScriptRecordManager.isPanelShowing()
    }

    fun tryStart() {
        ScriptManager.tryStart()
    }

    fun updateApp() {
        ScriptProvider.updateApp(null)
    }

    fun stopService() {
        ServiceAccessibility.stopServiceIntent()
    }

    fun openSetting() {
        ScriptProvider.startToSetting()
    }

    fun onTerminal() {
        ScriptProvider.getAccessService()?.stopSelf()
    }


    fun checkService(): Boolean {
        return ScriptManager.checkServerEnable()
    }

    fun onBackPressed(): Boolean {
        return ScriptManager.onBackPressed()
    }

    fun stopTask() {
        ScriptManager.stopPlay()
    }

    /**
     * 开始录制解锁工作流
     */
    fun startRecordUnlock() {
        ScriptRecordManager.startRecordUnlock()
    }

    /**
     * 开始测试执行解锁工作流
     */
    fun startTestUnlock() {
        ScriptEventHelper.get().performActionLockScreen()
        UIHandlerUtils.getInstance().executeInMainThread({
            ScriptManager.startPlay(
                ScriptConst.Task_Screen_Lock_Script_Main_Path,
                showPlayDialog = false,
                enableAutoUnlock = false
            )
        }, 500)
    }


    fun checkAccessibility(submitCallback: ((isCancel: Boolean) -> Unit)? = null): Boolean {
        return ScriptManager.checkAccessibility(submitCallback)
    }


    fun isUnlockScriptExist(): Boolean {
        return ScriptManager.isUnlockScriptExist()
    }

    fun isRunningOrRecording(): Boolean {
        return ScriptInterpreter.getDefault().isRunning()
    }

    /**
     * 脚本列表页面
     */
    fun retrieveScriptManagerView(context: Context): View = ScriptManagerLayoutForFrame(context)

    /**
     * 脚本定时管理页面
     */
    fun retrieveTimerFragment(): Fragment = ScriptFragmentSchedule()

}