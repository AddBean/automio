// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import com.hive.utils.GlobalApp

class FloatDialog {

    fun showByToast(){
        val guidView = LayoutInflater.from(GlobalApp.getContext())
            .inflate(R.layout.xml_guid_dialog_view,null,false)
        val toast = Toast(GlobalApp.getContext()).apply {
            setGravity(Gravity.BOTTOM,0,0)
            duration = Toast.LENGTH_LONG
            view = guidView
        }
        toast.show()
    }

    fun showByActivity(){
        GlobalApp.getApp().startActivity(
            Intent(GlobalApp.getContext(), FloatDialogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
    }
}