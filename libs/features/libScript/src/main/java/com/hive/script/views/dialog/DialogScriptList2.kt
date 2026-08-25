// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.views.ScriptManagerLayout
import com.hive.script.views.ScriptManagerLayoutForQuickStart
import com.hive.script.views.widgets.BaseScriptDialog

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
class DialogScriptList2(context: Context) : BaseScriptDialog(context) {

    private var ivClose: View? = null
    private var scriptManagerLayout: ScriptManagerLayoutForQuickStart? = null

    override fun initWindow() {
        ivClose = findViewById(R.id.ivClose)
        scriptManagerLayout= findViewById(R.id.scriptManagerLayout)
        scriptManagerLayout?.dialogOptionHandler =
            object : ScriptManagerLayout.IDialogOptionInterface {
                override fun onDialogDismiss() {
                    dismiss()
                }

                override fun onDialogShow() {
//                    DialogScriptList2(context).show()
                }
            }
        ivClose?.setOnClickListener {
            dismiss()
        }
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.dialog_script_switch_list

}