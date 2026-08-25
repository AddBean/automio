// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.setting.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.hive.views.R

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/5/21
 */
class SettingInputDialog : DialogFragment() {
    private var mDescription: String? = null
    private var mTitle: String? = null
    private var mValue: Any? = null
    private var mInputType: Int? = null
    var mOnValueChangedListener: OnValueChangedListener? = null
    private var tv_btn_cancel: TextView? = null
    private var tv_btn_submit: TextView? = null
    private var title: TextView? = null
    private var des: TextView? = null
    private var edit: EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.setting_input_dialog, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_btn_cancel = view.findViewById(R.id.tv_btn_cancel)
        tv_btn_submit = view.findViewById(R.id.tv_btn_submit)
        title = view.findViewById(R.id.title)
        des = view.findViewById(R.id.des)
        edit = view.findViewById(R.id.edit)
        setTitle(mTitle)
        setDescription(mDescription)
        setInputType(mInputType)
        setValue(mValue.toString())
        tv_btn_cancel?.setOnClickListener {
            dismissAllowingStateLoss()
        }
        tv_btn_submit?.setOnClickListener {
            dismissAllowingStateLoss()
            mOnValueChangedListener?.onValueChanged(edit!!.text.toString())
        }
    }

    fun setTitle(title: String?) {
        mTitle = title
        this.title?.text = title
    }

    fun setDescription(description: String?) {
        mDescription = description
        des?.text = mDescription
    }

    fun setValue(value: Any?) {
        mValue = value
        edit?.setText(value?.toString())
    }

    fun setInputType(inputType: Int?) {
        inputType?.let {
            mInputType = inputType
            edit?.inputType = inputType
        }
    }

    interface OnValueChangedListener {
        fun onValueChanged(value: String)
    }

    fun showDialog(context: Context?) {
        if (context is FragmentActivity) {
            show(context.supportFragmentManager.beginTransaction(), "setting input dialog")

        }
    }
}