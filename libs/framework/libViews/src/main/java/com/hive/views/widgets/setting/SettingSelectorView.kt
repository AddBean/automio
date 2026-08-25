// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.setting

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.preference.PreferenceManager
import com.hive.utils.GlobalApp
import com.hive.views.R
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.setting.dialog.SettingSelectorDialog

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/5/21
 */
open class SettingSelectorView(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs), ISettingViewInterface, SettingSelectorDialog.OnValueChangedListener {
    private var mDialog: SettingSelectorDialog? = null
    private var mSharedPreferences: SharedPreferences? = null
    var view = LayoutInflater.from(context).inflate(R.layout.setting_selector_view, this)
    private var tv_name = view.findViewById<android.widget.TextView>(R.id.tv_name)
    private var tv_des = view.findViewById<android.widget.TextView>(R.id.tv_des)
    private var text_value = view.findViewById<android.widget.TextView>(R.id.text_value)
    var helper = SettingAttributeHelper()

    init {
        helper.initAttributeSet(context, attrs)
        mSharedPreferences = getSharedPreferences()
        setOnClickListener {
            showSelectorDialog()
        }
        tv_name?.text = helper.mTitle
        tv_des?.text = helper.mDescription
        if (TextUtils.isEmpty(helper.mDescription)) {
            tv_des?.visibility = View.GONE
        } else {
            tv_des?.visibility = View.VISIBLE
        }
        updateValue()
    }

    fun setDefaultValue(value: Any?) {
        helper.mDefValue = value?.toString()
        updateValue()
    }

    private fun updateValue() {
        var value = getPreferenceValue()?.toString()


        if (value != null) {
            var valueNames = ""
            value.split(",").forEach {
                valueNames += (helper.mValueMap?.get(it).toString() + ",")
            }
            valueNames = valueNames.removeSuffix(",")
            text_value?.text = if (valueNames.length > 16) {
                valueNames?.substring(0, 16) + "…"
            } else {
                valueNames
            }
        }
    }

    override fun getSharedPreferences(): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(GlobalApp.getContext())

    override fun getPreferenceTitle(): String? = helper.mTitle

    override fun getPreferenceDes(): String? = helper.mDescription

    override fun getPreferenceDefaultValue(): Any? = helper.mDefValue

    override fun getPreferenceKey(): String = helper.mKey

    override fun getPreferenceValue(): Any? {
        var value = mSharedPreferences?.getString(helper.mKey, helper.mDefValue)
        if (value == null) {
            value = getPreferenceDefaultValue()?.toString()
        }

        return value
    }

    fun showSelectorDialog() {
        mDialog = SettingSelectorDialog().apply {
            this.mEnableMultiSelect = helper.mMultiSelect
            this.setTitle(helper.mDialogTitle)
            var values = getPreferenceValue() as String
            this.setValue(values.replace(" ", "").split(",").toTypedArray())
            this.setValueList(helper.mValueMap)
            this.mOnValueChangedListener = this@SettingSelectorView
            this.showDialog(this@SettingSelectorView.context)
        }
    }

    override fun onValueChanged(value: Array<String>?) {
        if (helper.mMultiSelect) {
            var maxCount = helper.mMaxValue?.toInt() ?: 1000
            if (maxCount == 0) maxCount = 1000
            if ((value?.size ?: 0) < helper.mMinValue?.toInt() ?: 1) {
                CommonToast.getInstance().showToast(context.getString(com.hive.i8n.R.string.setting_selector_error_low_min, helper.mMinValue?.toInt()
                        ?: 1))
                return
            } else if ((value?.size ?: 0) > maxCount) {
                CommonToast.getInstance().showToast(context.getString(com.hive.i8n.R.string.setting_selector_error_over_max, helper.mMaxValue?.toInt()
                        ?: 1000))
                return
            }
        }
        if (!onPreferenceChanged(helper.mKey)) {
            var sb = StringBuilder()
            value?.forEachIndexed { i, it ->
                sb.append(it)
                if (i < value.size - 1)
                    sb.append(",")
            }
            mSharedPreferences?.edit()?.putString(helper.mKey, sb.toString())?.commit()
        }
        updateValue()
    }

    fun dismissInputDialog() {
        mDialog?.dismiss()
    }

    override fun onPreferenceChanged(key: String): Boolean = false
}