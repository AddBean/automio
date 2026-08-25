// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.TextView
import com.hive.script.R
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.script.views.widgets.BaseScriptDialog
import java.io.File

/**
 *
 */
@SuppressLint("ViewConstructor")
class DialogSaveTaskConfirm(
    context: Context?,
    private val scriptName: String,
    private val leftBtnClickListener: OnClickListener
) : BaseScriptDialog(context), ScriptInterpreterObserver.InterpreterExecuteObserver {

    private var ivList: View? = null
    private var tvBtnLeft: TextView? = null
    private var tvBtnRight: TextView? = null
    private var tvEdit: TextView? = null
    private var tvName: TextView? = null
    fun setName(name: String) {
        tvName?.text = name
    }

    @SuppressLint("SetTextI18n")
    override fun initWindow() {
        ivList = findViewById(R.id.ivList)
        tvBtnLeft = findViewById(R.id.tvBtnLeft)
        tvBtnRight = findViewById(R.id.tvBtnRight)
        tvEdit = findViewById(R.id.tvEdit)
        tvName = findViewById(R.id.tvName)

        tvName?.text = scriptName

        ivList?.setOnClickListener {
            DialogScriptList2(context).show()
            dismiss()
        }

        tvBtnLeft?.setOnClickListener {
            dismiss()
        }

        tvBtnRight?.setOnClickListener {
            try {
                dismiss()
                leftBtnClickListener.onClick(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        tvEdit?.setOnClickListener {
            val targetPath =
                "${ScriptConst.Save_Script_Path}/${scriptName}/"
            DialogScriptEdit.create(null)?.setScriptPath(targetPath)
                ?.setTitleName(File(targetPath).name)
                ?.setFromSource(ScriptConst.From.FROM_SCRIPT_UNKNOWN)
                ?.show()
            dismiss()
        }

    }

    override fun enableFadeAnimation() = true

    override fun isTouchOutsideDismissed() = true

    override fun getWindowLayoutId() = R.layout.dialog_save_task


}