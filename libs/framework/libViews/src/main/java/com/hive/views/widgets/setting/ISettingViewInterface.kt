// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.setting

import android.content.SharedPreferences

/**
 *
 * @author jiadou
 * @date 5/5/21
 */
interface ISettingViewInterface {

    fun getSharedPreferences(): SharedPreferences

    fun getPreferenceTitle(): String?

    fun getPreferenceDes(): String?

    fun getPreferenceDefaultValue(): Any?

    fun getPreferenceKey(): String

    fun getPreferenceValue(): Any?

    fun onPreferenceChanged(key:String):Boolean
}