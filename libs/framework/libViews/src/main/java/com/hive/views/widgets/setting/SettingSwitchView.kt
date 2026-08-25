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
import com.hive.views.widgets.setting.dialog.SettingInputDialog

/**
 *
 * @author jiadou
 * @date 5/5/21
 */
open class SettingSwitchView(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs), ISettingViewInterface, SettingInputDialog.OnValueChangedListener {
    private var mDialog: SettingInputDialog? = null
    private var mSharedPreferences: SharedPreferences? = null
    var view = LayoutInflater.from(context).inflate(R.layout.setting_switch_view, this)
    private var tv_name = view.findViewById<android.widget.TextView>(R.id.tv_name)
    private var tv_des = view.findViewById<android.widget.TextView>(R.id.tv_des)
    private var switch_value = view.findViewById<android.widget.Switch>(R.id.switch_value)

    var mSettingAttributeHelper = SettingAttributeHelper()
    var mOnSwitchStatusListener: OnSwitchStatusListener? = null

    init {
        mSettingAttributeHelper.initAttributeSet(context, attrs)
        mSharedPreferences = getSharedPreferences()
        tv_name?.text = mSettingAttributeHelper.mTitle
        tv_des?.text = mSettingAttributeHelper.mDescription
        if (TextUtils.isEmpty(mSettingAttributeHelper.mDescription)) {
            tv_des?.visibility = View.GONE
        } else {
            tv_des?.visibility = View.VISIBLE
        }
        switch_value?.isChecked = mSettingAttributeHelper.mDefValue?.toBoolean() == true
        switch_value?.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.isChecked = isChecked
            onValueChanged(isChecked.toString())
        }
        updateValue()
    }

    private fun updateValue() {
        var value = getPreferenceValue()
        switch_value?.isChecked = value == true
    }

    var isChecked: Boolean = switch_value?.isChecked == true

    override fun getSharedPreferences(): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(GlobalApp.getContext())

    override fun getPreferenceTitle(): String? = mSettingAttributeHelper.mTitle

    override fun getPreferenceDes(): String? = mSettingAttributeHelper.mDescription

    override fun getPreferenceDefaultValue(): Any? = mSettingAttributeHelper.mDefValue

    override fun getPreferenceKey(): String = mSettingAttributeHelper.mKey

    override fun getPreferenceValue(): Boolean? {
        return mSharedPreferences?.getBoolean(mSettingAttributeHelper.mKey, mSettingAttributeHelper.mDefValue?.toBoolean() == true)
    }

    override fun onValueChanged(value: String) {
        mOnSwitchStatusListener?.onSwitchStatusChanged(value.toBoolean())
        if (!onPreferenceChanged(mSettingAttributeHelper.mKey)) {
            mSharedPreferences?.edit()?.putBoolean(mSettingAttributeHelper.mKey, value.toBoolean())?.commit()
        }
        updateValue()
    }

    override fun onPreferenceChanged(key: String): Boolean = false

    interface OnSwitchStatusListener {
        fun onSwitchStatusChanged(isChecked: Boolean)
    }
}