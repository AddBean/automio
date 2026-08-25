// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import com.hive.script.R
import com.hive.script.views.ScriptManagerLayout
import com.hive.script.views.ScriptManagerLayoutForDialog
import com.hive.script.views.widgets.BaseScriptDialog

/**
 *
 * @author jiadou
 * @date 6/11/21
 */
class DialogScriptList(context: Context) : BaseScriptDialog(context) {

    override fun initWindow() {
        findViewById<ScriptManagerLayoutForDialog>(R.id.scriptManagerLayout)?.dialogOptionHandler =
            object : ScriptManagerLayout.IDialogOptionInterface {
                override fun onDialogDismiss() {
                    dismiss()
                }

                override fun onDialogShow() {
//                    DialogScriptList(context).show()
                }
            }
    }

    override fun enableFadeAnimation() = true

    override fun getMarginParams() =
        arrayOf(0, 0, 0, 0)

    override fun getWindowLayoutId() = R.layout.dialog_script_list

}