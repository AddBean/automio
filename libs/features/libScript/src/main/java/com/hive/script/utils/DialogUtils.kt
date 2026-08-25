// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import com.hive.script.ActivityScriptShortcut
import com.hive.script.R
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.utils.GlobalApp
import com.hive.utils.utils.BitmapUtils
import com.hive.utils.utils.ShortcutUtils
import java.io.File

object DialogUtils {

    fun tryCreateShortcut(context :Context,data: ScriptInfoModel) {
        DialogScriptAlert(context)
            .setTitle(com.hive.i8n.R.string.sc_shortcut_title)
            .setContent(com.hive.i8n.R.string.sc_shortcut_content)
            .setConfirmText(com.hive.i8n.R.string.sc_shortcut_confirm)
            .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                    dialog.dismiss()
                    if (!isCancel) {
                        val intent = Intent(context, ActivityScriptShortcut::class.java)
                        intent.putExtra("scriptPath", data.scriptPath!!)
                        val path = "${File(data.scriptPath!!)}/${data.scriptMate?.icon}"
                        val bmp = if (File(path).exists()) {
                            BitmapFactory.decodeFile(path)
                        } else {
                            BitmapUtils.drawableToBitmap(R.drawable.sc_default_icon)
                        }

                        ShortcutUtils.addShortCutCompact(
                            GlobalApp.getContext(),
                            data.scriptName,
                            bmp,
                            intent
                        )
                    }
                }
            }).show()
    }

}