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
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.hive.utils.GlobalApp
import com.hive.views.R
import com.hive.views.widgets.NumberOptView
import com.hive.views.widgets.setting.dialog.SettingInputDialog

/**
 *
 * @author jiadou
 * @date 5/5/21
 */
open class SettingNumberView(context: Context?, attrs: AttributeSet?) :
    RelativeLayout(context, attrs), ISettingViewInterface {
    private var mDialog: SettingInputDialog? = null
    private var mSharedPreferences: SharedPreferences? = null
    var view = LayoutInflater.from(context).inflate(R.layout.setting_number_view, this)
    private var tv_name = view.findViewById<TextView>(R.id.tv_name)
    private var tv_des = view.findViewById<TextView>(R.id.tv_des)
    private var number_value = view.findViewById<NumberOptView>(R.id.number_value)

    var mSettingAttributeHelper = SettingAttributeHelper()

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
        number_value?.minValue = mSettingAttributeHelper.mMinValue?.toInt() ?: Int.MIN_VALUE
        number_value?.maxValue = mSettingAttributeHelper.mMaxValue?.toInt() ?: Int.MAX_VALUE
        number_value?.onValueChangedListener = object : NumberOptView.OnValueChangedListener {
            override fun onValueChanged(value: Int) {
                if (!onPreferenceChanged(mSettingAttributeHelper.mKey)) {
                    mSharedPreferences?.edit()?.putInt(mSettingAttributeHelper.mKey, value)
                        ?.commit()
                }
            }
        }
        updateValue()
    }

    fun setDefaultValue(value: Any?) {
        mSettingAttributeHelper.mDefValue = value?.toString()
        updateValue()
    }

    private fun updateValue() {
        var value = getPreferenceValue()
        if (value != null) {
            number_value?.setValue(value as Int)
        }
    }

    override fun getSharedPreferences(): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(GlobalApp.getContext())

    override fun getPreferenceTitle(): String? = mSettingAttributeHelper.mTitle

    override fun getPreferenceDes(): String? = mSettingAttributeHelper.mDescription

    override fun getPreferenceDefaultValue(): Any? = mSettingAttributeHelper.mDefValue

    override fun getPreferenceKey(): String = mSettingAttributeHelper.mKey

    override fun getPreferenceValue(): Any? {
        var value = mSharedPreferences?.getInt(
            mSettingAttributeHelper.mKey,
            mSettingAttributeHelper.mDefValue?.toInt() ?: 0
        )
        if (value == null) {
            value = getPreferenceDefaultValue() as Int
        }
        return value
    }


    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else 0.3f
        number_value.setEditEnable(enabled)
    }

    override fun onPreferenceChanged(key: String): Boolean = false
}