// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.setting.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.hive.views.R

/**
 *
 * @author jiadou
 * @date 5/5/21
 */
class SettingSelectorDialog : DialogFragment() {
    private var mValueMap: MutableMap<String, String>? = null
    private var mTitle: String? = null
    private var mValue: Array<String>? = null
    private var mInputType: Int? = null
    var mEnableMultiSelect = false
    var mOnValueChangedListener: OnValueChangedListener? = null
    private var tv_btn_cancel: View? = null
    private var tv_btn_submit: View? = null
    private var layout_list: ViewGroup? = null
    private var title: TextView? = null
    private var des: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.setting_selector_dialog, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_btn_cancel = view.findViewById(R.id.tv_btn_cancel)
        tv_btn_submit = view.findViewById(R.id.tv_btn_submit)
        title = view.findViewById(R.id.tv_title)
        des = view.findViewById(R.id.tv_des)
        layout_list = view.findViewById(R.id.layout_list)
        setTitle(mTitle)
        setValue(mValue)
        setValueList(mValueMap)
        tv_btn_cancel?.setOnClickListener {
            dismissAllowingStateLoss()
        }
        tv_btn_submit?.setOnClickListener {
            dismissAllowingStateLoss()
            mOnValueChangedListener?.onValueChanged(getSelectedValue())
        }

    }

    fun setTitle(title: String?) {
        mTitle = title
        this.title?.text = title
    }


    fun setValue(value: Array<String>?) {
        mValue = value
    }

    fun setValueList(valueMap: MutableMap<String, String>?) {
        if (valueMap == null) return
        mValueMap = valueMap
        layout_list?.run {
            removeAllViews()
            mValueMap?.forEach {
                var itemView = ItemView(context)
                itemView.dialog = this@SettingSelectorDialog
                layout_list?.addView(itemView)
                itemView.bindData(it)
            }
        }
    }

    class ItemView(context: Context) : FrameLayout(context) {
        var itemView =
            LayoutInflater.from(context).inflate(R.layout.setting_selector_dialog_item_view, this)
        var itemData: Map.Entry<String, String>? = null
        var tvValue: TextView? = null
        var ivSelector: ImageView? = null
        var dialog: SettingSelectorDialog? = null

        init {
            tvValue = findViewById(R.id.tv_value)
            ivSelector = findViewById(R.id.iv_selector)
            setOnClickListener {
                if (dialog?.mEnableMultiSelect == true) {
                    this@ItemView.isSelected = !this@ItemView.isSelected
                } else {
                    dialog?.clearSelection()
                    this@ItemView.isSelected = true
                }
            }
        }

        fun bindData(it: Map.Entry<String, String>) {
            itemData = it
            tvValue?.text = it.value
            isSelected = dialog?.mValue?.contains(it.key) == true
        }

        override fun setSelected(selected: Boolean) {
            super.setSelected(selected)
            ivSelector?.isSelected = selected
        }
    }

    private fun clearSelection() {
        layout_list?.run {
            for (i in 0 until this.childCount) {
                this.getChildAt(i).isSelected = false
            }
        }
    }

    private fun getSelectedValue(): Array<String>? {
        var array = arrayListOf<String>()
        layout_list?.run {
            for (i in 0 until this.childCount) {
                if (this.getChildAt(i).isSelected) {
                    array.add((this.getChildAt(i) as ItemView).itemData!!.key)
                }
            }
        }
        return array.toTypedArray()
    }

    interface OnValueChangedListener {
        fun onValueChanged(value: Array<String>?)
    }

    fun showDialog(context: Context?) {
        if (context is FragmentActivity) {
            show(context.supportFragmentManager.beginTransaction(), "setting selector dialog")
        }
    }
}