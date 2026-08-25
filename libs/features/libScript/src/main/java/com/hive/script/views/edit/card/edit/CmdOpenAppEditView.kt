// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import com.hive.files.utils.XAppInfoParser
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdOpenApp
import com.hive.script.views.dialog.DialogAppSelector
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.GlobalApp
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class CmdOpenAppEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdOpenApp? = null

    private var app_action: ScriptTabSelectorView? = null

    private var app_name: ScriptValueView? = null

    override fun initView() {
        app_action = findViewById(R.id.app_action)
        app_name = findViewById(R.id.app_name)
        app_name?.onMaskClickListener = OnClickListener {
            showSelector()
        }
        app_name?.onMaskClickListener = OnClickListener { showSelector() }
        app_action?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd?.action = p?.second ?: ""
                    bindCommand(cmd!!)
                }
            }
    }

    private fun showSelector() {
        DialogAppSelector(context)
            .setOnAppSelectedListener(object : DialogAppSelector.OnAppSelectedListener {
                override fun onSelected(
                    dialog: DialogAppSelector,
                    appInfo: XAppInfoParser.AppInfo?
                ) {
                    var launchIntent =
                        GlobalApp.getApp().packageManager.getLaunchIntentForPackage(appInfo?.packageName!!)
                    cmd?.targetAppPackage = appInfo.packageName
                    cmd?.targetAppClass = launchIntent?.component?.className
                    cmd?.targetAppName = appInfo.appName
                    dialog.dismiss()
                    bindCommand(cmd!!)
                }
            }).show()
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdOpenApp
        app_name?.setValue(cmd?.targetAppName ?: cmd?.targetAppPackage ?: "")
        app_action?.setValue(cmd?.action ?: "reopen")
//        setDrawableRight(ScriptProvider.findAppIcon(cmd?.targetAppPackage ?: ""))
    }


    override fun getEditContentId() = R.layout.cmd_open_app_card

}