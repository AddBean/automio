// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hive.views.DialogAlertHelper
import com.hive.utils.GlobalApp


object CalendarUtils {

    const val PERMISSION_REQUEST_CODE = 100001

    private val listeners = mutableListOf<CallBack>()

    fun register(callBack: CallBack) {
        if (listeners.contains(callBack).not()) {
            listeners.add(callBack)
        }
    }

    fun unregister(callBack: CallBack) {
        if (listeners.contains(callBack).not()) {
            listeners.remove(callBack)
        }
    }

    fun hasPermission(context: Context): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_CALENDAR
    ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED

    fun requestPermission(
        context: Context, success: (Boolean) -> Unit = { result ->
            listeners.forEach {
                it.onResult(result)
            }
        }, withDialog: Boolean = true
    ) {
        if (hasPermission(context).not()) {
            if (withDialog) {
                showCalenderPermissionDialog(context) {
                    ActivityCompat.requestPermissions(
                        GlobalApp.getMainActivity(),
                        arrayOf(
                            Manifest.permission.WRITE_CALENDAR,
                            Manifest.permission.READ_CALENDAR
                        ),
                        PERMISSION_REQUEST_CODE
                    )
                }
            } else {
                ActivityCompat.requestPermissions(
                    GlobalApp.getMainActivity(),
                    arrayOf(
                        Manifest.permission.WRITE_CALENDAR,
                        Manifest.permission.READ_CALENDAR
                    ),
                    PERMISSION_REQUEST_CODE
                )
            }
        } else {
            success.invoke(true)
        }
    }

    fun showCalenderPermissionDialog(context: Context, callback: () -> Unit) {
        if (hasPermission(context).not()) {
            DialogAlertHelper.showDialog(
                context,
                GlobalApp.getString(com.hive.i8n.R.string.calentar_permission_title),
                GlobalApp.getString(com.hive.i8n.R.string.calentar_permission_content),
                GlobalApp.getString(com.hive.i8n.R.string.calentar_permission_left_text),
                GlobalApp.getString(com.hive.i8n.R.string.calentar_permission_right_text),
                object : DialogAlertHelper.OnDialogListener {
                    override fun onItemClick(
                        dialog: DialogAlertHelper.DialogTipsInterface,
                        isRight: Boolean
                    ) {
                        dialog.dismiss()
                        if (isRight) {
                            callback.invoke()
                        }
                    }
                }
            )
        } else {
            callback.invoke()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            listeners.forEach {
                it.onResult(resultCode == Activity.RESULT_OK)
            }
        }
    }

    interface CallBack {
        fun onResult(success: Boolean)
    }
}
