// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import com.hive.files.utils.XAppInfoParser
import com.hive.script.R
import com.hive.script.cmd.CmdActionHome
import com.hive.script.cmd.CmdOpenApp
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.CommomListener
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/11/21
 */
class DialogChooseScriptStart(context: Context?) : BaseScriptDialog(context) {

    var mCallback: CommomListener.Callback? = null

    private var ivClose: ImageView? = null
    private var tvBtnCurrentPage: TextView? = null
    private var tvBtnFromTargetApp: TextView? = null
    private var tvBtnStartFromHome: TextView? = null

    override fun initWindow() {
        ivClose= findViewById(R.id.ivClose)
        tvBtnCurrentPage = findViewById(R.id.tvBtnCurrentPage)
        tvBtnFromTargetApp = findViewById(R.id.tvBtnFromTargetApp)
        tvBtnStartFromHome = findViewById(R.id.tvBtnStartFromHome)
        ivClose?.setOnClickListener {
            dismiss()
        }

        tvBtnCurrentPage?.setOnClickListener {
            mCallback?.onEvent(0, null)
            dismiss()
        }

        tvBtnStartFromHome?.setOnClickListener {
            mCallback?.onEvent(0, CmdActionHome.createCommand())
            dismiss()
        }

        tvBtnFromTargetApp?.setOnClickListener {
            DialogAppSelector(context)
                .setOnAppSelectedListener(object : DialogAppSelector.OnAppSelectedListener {
                    override fun onSelected(
                        dialog: DialogAppSelector,
                        appInfo: XAppInfoParser.AppInfo?
                    ) {
                        val launchIntent =
                            GlobalApp.getApp().packageManager.getLaunchIntentForPackage(appInfo?.packageName!!)
                        val cmd = CmdOpenApp.createCommand(
                            appInfo.packageName, launchIntent?.component?.className,
                            appInfo.appName, "reopen"
                        )
                        mCallback?.onEvent(0, cmd)
                        //tvBtnFromTargetApp.setCompoundDrawables(null,null,ScriptProvider.findAppIcon(cmd?.targetAppPackage ?: ""),null)
                        dialog.dismiss()
                        dismiss()
                    }
                }).show()
        }
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.dialog_choose_script_start
}