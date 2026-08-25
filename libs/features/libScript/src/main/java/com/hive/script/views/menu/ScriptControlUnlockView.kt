// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.menu

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.utils.ScriptHelper
import com.hive.script.utils.ScriptScreenObserver
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.script.views.event.UnlockTaskEvent
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.script.views.manager.ScriptRecordManager
import org.greenrobot.eventbus.EventBus

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2021/10/14
 */
class ScriptControlUnlockView(context: Context?, attrs: AttributeSet?) :
    BaseLayout(context, attrs),
    View.OnClickListener, ScriptScreenObserver.ScreenStateListener {

    private val screenObserver = ScriptScreenObserver(context)

    private var btn_play_cancel: View? = null
    private var btn_play_finish: View? = null

    override fun initView(view: View?) {
        btn_play_cancel = findViewById(R.id.btn_play_cancel)
        btn_play_finish = findViewById(R.id.btn_play_finish)
        btn_play_finish?.setOnClickListener(this)
        btn_play_cancel?.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_play_finish -> {
                //保存解锁手势
                ScriptRecordManager.stopRecord(ScriptConst.Task_Screen_Unlock_Script_Name)
                ScriptInterpreter.getDefault().stopExecute()
                ScriptMenuManager.hiddenMenuView()
                showUnlockDialog()
            }

            R.id.btn_play_cancel -> {
                ScriptInterpreter.getDefault().stopExecute()
                ScriptRecordManager.pauseRecord()
                ScriptMenuManager.hiddenMenuView()
            }
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == View.VISIBLE) {
            screenObserver.startObserver(this)
        } else {
            screenObserver.shutdownObserver()
        }
    }

    override fun onScreenOn() {//屏幕点亮

    }

    override fun onScreenOff() {//屏幕关闭

    }

    override fun onUserPresent() {//用户解锁
        screenObserver.shutdownObserver()
        postDelayed({
            ScriptRecordManager.stopRecord(ScriptConst.Task_Screen_Unlock_Script_Name)
            ScriptMenuManager.hiddenMenuView()
            showUnlockDialog()
        }, 500)
    }


    private fun showUnlockDialog() {
        EventBus.getDefault().post(UnlockTaskEvent(false))
        DialogScriptAlert(context)
            .setTitle(com.hive.i8n.R.string.sc_unlock_success_title)
            .setContent(com.hive.i8n.R.string.sc_unlock_success_content)
            .setConfirmText(com.hive.i8n.R.string.sc_unlock_success_confirm)
            .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                    dialog.dismiss()
                    if (!isCancel) {
                        ScriptEventHelper.get().performActionLockScreen()
                        ScriptHelper.runInMain({
                            ScriptManager.startPlay(
                                ScriptConst.Task_Screen_Lock_Script_Main_Path,
                                showPlayDialog = false,
                                enableAutoUnlock = false
                            )
                        }, 100)
                    }
                }
            }).show()
    }

    override fun getLayoutId() = R.layout.script_control_unlock_view

}