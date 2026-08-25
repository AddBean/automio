// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.setting

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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
open class SettingTextView(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs), ISettingViewInterface, SettingInputDialog.OnValueChangedListener {
    private var mDialog: SettingInputDialog? = null
    private var mSharedPreferences: SharedPreferences? = null
    var view = LayoutInflater.from(context).inflate(R.layout.setting_text_view, this)
    private var tv_name = view.findViewById<android.widget.TextView>(R.id.tv_name)
    private var tv_des = view.findViewById<android.widget.TextView>(R.id.tv_des)
    private var edit_value = view.findViewById<EditText>(R.id.edit_value)
    var mSettingAttributeHelper = SettingAttributeHelper()

    init {
        mSettingAttributeHelper.initAttributeSet(context, attrs)
        mSharedPreferences = getSharedPreferences()
        setOnClickListener {
            showInputDialog()
        }
        tv_name?.text = mSettingAttributeHelper.mTitle
        tv_des?.text = mSettingAttributeHelper.mDescription
        if (TextUtils.isEmpty(mSettingAttributeHelper.mDescription)) {
            tv_des?.visibility = View.GONE
        } else {
            tv_des?.visibility = View.VISIBLE
        }
        edit_value?.isEnabled = false
        edit_value?.inputType = mSettingAttributeHelper.mInputType
        edit_value?.requestFocus()
        updateValue()
    }

    fun setDefaultValue(value: Any?) {
        mSettingAttributeHelper.mDefValue = value?.toString()
        updateValue()
    }

    private fun updateValue() {
        var value = getPreferenceValue()
        if (value != null) {
            edit_value?.setText(value?.toString())
        }
    }

    fun getEditText(): EditText = edit_value

    override fun getSharedPreferences(): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(GlobalApp.getContext())

    override fun getPreferenceTitle(): String? = mSettingAttributeHelper.mTitle

    override fun getPreferenceDes(): String? = mSettingAttributeHelper.mDescription

    override fun getPreferenceDefaultValue(): Any? = mSettingAttributeHelper.mDefValue

    override fun getPreferenceKey(): String = mSettingAttributeHelper.mKey

    override fun getPreferenceValue(): Any? {
        var value: Any? = when (edit_value!!.inputType and EditorInfo.TYPE_MASK_CLASS) {

            EditorInfo.TYPE_CLASS_NUMBER -> {
                when (edit_value!!.inputType and EditorInfo.TYPE_MASK_FLAGS) {
                    EditorInfo.TYPE_NUMBER_FLAG_SIGNED -> {
                        mSharedPreferences?.getInt(mSettingAttributeHelper.mKey, mSettingAttributeHelper.mDefValue?.toInt()
                                ?: 0)
                    }
                    EditorInfo.TYPE_NUMBER_FLAG_DECIMAL -> {
                        mSharedPreferences?.getFloat(mSettingAttributeHelper.mKey, mSettingAttributeHelper.mDefValue?.toFloat()
                                ?: 0f)
                    }
                    else -> mSharedPreferences?.getInt(mSettingAttributeHelper.mKey, mSettingAttributeHelper.mDefValue?.toInt()
                            ?: 0)
                }
            }

            else -> mSharedPreferences?.getString(mSettingAttributeHelper.mKey, mSettingAttributeHelper.mDefValue)
        }
        if (value == null) {
            value = getPreferenceDefaultValue()
        }
        value?.let {
            value = getScaleDownValue(value!!)
        }

        return value
    }

    fun showInputDialog() {
        mDialog = SettingInputDialog().apply {
            this.setTitle(mSettingAttributeHelper.mDialogTitle)
            this.setDescription(mSettingAttributeHelper.mDialogDescription)
            this.setValue(getPreferenceValue())
            this.setInputType(mSettingAttributeHelper.mInputType)
            this.mOnValueChangedListener = this@SettingTextView
            this.showDialog(this@SettingTextView.context)
        }
    }


    fun dismissInputDialog() {
        mDialog?.dismiss()
    }


    override fun onValueChanged(value: String) {
        if (!onPreferenceChanged(mSettingAttributeHelper.mKey)) {
            when (edit_value!!.inputType and EditorInfo.TYPE_MASK_CLASS) {

                EditorInfo.TYPE_CLASS_NUMBER -> {
                    when (edit_value!!.inputType and EditorInfo.TYPE_MASK_FLAGS) {
                        EditorInfo.TYPE_NUMBER_FLAG_SIGNED -> {
                            mSharedPreferences?.edit()?.putInt(mSettingAttributeHelper.mKey, getScaleUpValue(value.toInt()).toInt())?.commit()
                        }
                        EditorInfo.TYPE_NUMBER_FLAG_DECIMAL -> {
                            mSharedPreferences?.edit()?.putFloat(mSettingAttributeHelper.mKey, getScaleUpValue(value.toFloat()).toFloat())?.commit()
                        }
                        else -> mSharedPreferences?.edit()?.putInt(mSettingAttributeHelper.mKey, getScaleUpValue(value.toInt()).toInt())?.commit()
                    }
                }

                else -> mSharedPreferences?.edit()?.putString(mSettingAttributeHelper.mKey, value)?.commit()
            }
        }
        updateValue()
    }

    private fun getScaleDownValue(v: Any): String {
        var value = v
        if (value is Int) {
            value = (value/mSettingAttributeHelper.mScale).toInt()
        } else if (value is Float) {
            value /= mSettingAttributeHelper.mScale
        }
        return value.toString()
    }

    private fun getScaleUpValue(v: Any): String {
        var value = v
        if (value is Int) {
            value = (value*mSettingAttributeHelper.mScale).toInt()
        } else if (value is Float) {
            value *= mSettingAttributeHelper.mScale
        }
        return value.toString()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else 0.3f
    }

    override fun onPreferenceChanged(key: String): Boolean = false
}