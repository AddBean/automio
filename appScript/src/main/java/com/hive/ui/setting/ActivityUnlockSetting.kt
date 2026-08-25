// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import com.hive.app.script.R
import com.hive.i8n.R as i8nR
import com.hive.base.BaseFragmentActivity
import com.hive.framework.coper.ScriptManagerImpl
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.script.views.event.UnlockTaskEvent
import com.hive.script.views.tips.BaseScriptTipsHelper
import com.hive.utils.GlobalApp
import com.hive.utils.thread.UIHandlerUtils
import com.hive.utils.utils.IntentUtils
import com.hive.script.FloatDialog
import com.hive.script.ScriptProvider
import com.hive.views.widgets.CommonToast
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ActivityUnlockSetting : BaseFragmentActivity(),
    ScriptInterpreterObserver.InterpreterExecuteObserver {

    private var btnRecord: TextView? = null
    private var btnTest: View? = null

    override fun doOnCreate(p0: Bundle?) {
        btnRecord = findViewById(R.id.btnRecord)
        btnTest = findViewById(R.id.btnTest)
        ScriptInterpreterObserver.registerInterpreterObserver(this)
        EventBus.getDefault().register(this)
        updateStatus()
        btnRecord?.setOnClickListener {
            if (ScriptManagerImpl.checkService()) {
                showRecordConfirm {
                    ScriptManagerImpl.startRecordUnlock()
                }
            } else {
                ScriptProvider.startToAccessibilitySetting()
            }

        }
        btnTest?.setOnClickListener {
            if (ScriptManagerImpl.checkService()) {
                if (ScriptManagerImpl.isUnlockScriptExist()) {
                    ScriptManagerImpl.startTestUnlock()
                } else {
                    CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.unlock_setting_not_record_yet))
                }
            } else {
                ScriptProvider.startToAccessibilitySetting()
            }
        }
    }

    private fun showRecordConfirm(confirm: () -> Unit) {
        DialogScriptAlert(this)
            .setTitle(com.hive.i8n.R.string.unlock_confirm_title)
            .setContent(com.hive.i8n.R.string.unlock_confirm_msg)
            .setConfirmText(com.hive.i8n.R.string.unlock_confirm_ok)
            .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                    dialog.dismiss()
                    if (!isCancel) {
                        confirm.invoke()
                    }
                }
            }).show()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        btnRecord?.text =
            if (ScriptManagerImpl.isUnlockScriptExist()) {
                GlobalApp.getString(com.hive.i8n.R.string.setting_script_unlock_already_record)
            } else {
                GlobalApp.getString(com.hive.i8n.R.string.setting_script_unlock_not_record)
            }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnlockTaskEvent(event: UnlockTaskEvent) {
        updateStatus()
    }

    override fun getLayoutId() = R.layout.activity_unlock_setting

    override fun onInterpreterEnd(cmd: ScriptCommand) {
        UIHandlerUtils.getInstance().executeInMainThread({
            if (cmd.getRootScript()?.scriptPath?.contains(ScriptConst.Task_Screen_Unlock_Script_Name) == true &&
                !ScriptEventHelper.get()
                    .isScreenLocked()
            ) {
                BaseScriptTipsHelper.showUnlockTestSuccess()
            }
        }, 200)
    }


    override fun onDestroy() {
        super.onDestroy()
        ScriptInterpreterObserver.unRegisterInterpreterObserver(this)
        EventBus.getDefault().unregister(this)
    }

    companion object {
        fun start(context: Context) {
            IntentUtils.safeStartActivity(
                context,
                Intent(context, ActivityUnlockSetting::class.java)
            )
        }
    }
}