// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import com.hive.views.DialogAlertHelper

class DialogTimerTips(context: Context) : DialogAlertHelper.DialogTipsInterface {
    private val dialog = DialogScriptAlert(context)

    override fun setDialogTitle(text: String): DialogAlertHelper.DialogTipsInterface {
        dialog.setTitle(text)
        return this
    }

    override fun setDialogContent(text: String): DialogAlertHelper.DialogTipsInterface {
        dialog.setContent(text)
        return this
    }

    override fun setLeftText(text: String): DialogAlertHelper.DialogTipsInterface {
        dialog.setCancelText(text)
        return this
    }

    override fun setRightText(text: String): DialogAlertHelper.DialogTipsInterface {
        dialog.setConfirmText(text)
        return this
    }

    override fun setOnDialogListener(listener: DialogAlertHelper.OnDialogListener): DialogAlertHelper.DialogTipsInterface {
        dialog.setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {

            override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                listener.onItemClick(this@DialogTimerTips, !isCancel)
            }
        })
        return this
    }

    override fun show(): DialogAlertHelper.DialogTipsInterface {
        dialog.show()
        return this
    }

    override fun dismiss(): DialogAlertHelper.DialogTipsInterface {
        dialog.dismiss()
        return this
    }
}