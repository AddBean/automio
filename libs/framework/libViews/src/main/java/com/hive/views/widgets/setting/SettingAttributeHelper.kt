// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.setting

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import com.hive.views.R
import java.lang.Exception

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/5/21
 */
class SettingAttributeHelper {
    var mMultiSelect: Boolean = false
    var mValueArray: Array<CharSequence>? = null
    var mScale = 1f
    var mKey: String = "default_key"
    var mDialogTitle: String? = null
    var mDialogDescription: String? = null
    var mDefValue: String? = null
    var mMinValue: Float? = null
    var mMaxValue: Float? = null
    var mDescription: String? = null
    var mTitle: String? = null
    var mInputType: Int = EditorInfo.TYPE_CLASS_TEXT
    var mValueMap: MutableMap<String, String>? = mutableMapOf()

    fun initAttributeSet(context: Context?, attrs: AttributeSet?) {
        if (attrs != null) {
            val ta = context?.obtainStyledAttributes(attrs, R.styleable.Setting)
            if (ta?.getString(R.styleable.Setting_settingKey) == null) {
                throw Exception(com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.views_setting_key_required))
            }
            mKey = ta.getString(R.styleable.Setting_settingKey)!!
            mDefValue = ta.getString(R.styleable.Setting_settingDefValue)
            mValueArray = ta.getTextArray(R.styleable.Setting_settingValueArray)
            mTitle = ta.getString(R.styleable.Setting_settingTitle)
            mScale = ta.getFloat(R.styleable.Setting_settingScale, 1f)
            mDescription = ta?.getString(R.styleable.Setting_settingDes)
            mDialogTitle = ta?.getString(R.styleable.Setting_settingDialogTitle)
            mDialogDescription = ta?.getString(R.styleable.Setting_settingDialogDes)
            mMaxValue = ta?.getFloat(R.styleable.Setting_settingMaxValue, 0f)
            mMinValue = ta?.getFloat(R.styleable.Setting_settingMinValue, 1f)
            mMultiSelect = ta.getBoolean(R.styleable.Setting_settingMultiSelect, false)
            mInputType = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "inputType", EditorInfo.TYPE_CLASS_TEXT)

            ta?.recycle()
            parseValueArray(mValueArray)
        }
    }

    private fun parseValueArray(valueArray: Array<CharSequence>?) {
        if (valueArray == null) return
        valueArray?.forEach {
            var vs = it.split("|")
            var key = vs[0]
            var value = vs[0]
            if (vs.size > 1) {
                value = vs[1]
            }
            mValueMap?.put(key, value)
        }
    }


}