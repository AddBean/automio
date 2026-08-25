// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.model

import android.content.SharedPreferences
import android.text.TextUtils
import androidx.preference.PreferenceManager
import com.hive.libfiles.R
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/8/21
 */
class XFileSetting : SharedPreferences.OnSharedPreferenceChangeListener {

    private var mSharedPreferences: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(GlobalApp.getContext())

    var disableRecordFile = false

    var showThumb = true

    var showHiddenFile = false

    var enableRecyclerBin = true

    init {
        mSharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        updateConfig()
    }

    private fun updateConfig() {
        onSharedPreferenceChanged(mSharedPreferences, GlobalApp.getString(R.string.x_file_setting_record_file))
        onSharedPreferenceChanged(mSharedPreferences, GlobalApp.getString(R.string.x_file_setting_show_thumb))
        onSharedPreferenceChanged(mSharedPreferences, GlobalApp.getString(R.string.x_file_setting_show_hidden_file))
        onSharedPreferenceChanged(mSharedPreferences, GlobalApp.getString(R.string.x_file_setting_enable_recycler_bin))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (TextUtils.equals(key, GlobalApp.getString(R.string.x_file_setting_record_file))) {
            disableRecordFile = sharedPreferences!!.getBoolean(key, true)
        } else if (TextUtils.equals(key, GlobalApp.getString(R.string.x_file_setting_show_thumb))) {
            showThumb = sharedPreferences!!.getBoolean(key, true)
        } else if (TextUtils.equals(key, GlobalApp.getString(R.string.x_file_setting_show_hidden_file))) {
            showHiddenFile = sharedPreferences!!.getBoolean(key, false)
        }else if (TextUtils.equals(key, GlobalApp.getString(R.string.x_file_setting_enable_recycler_bin))) {
            enableRecyclerBin = sharedPreferences!!.getBoolean(key, true)
        }
    }

    companion object {
        val instance: XFileSetting by lazy {
            XFileSetting()
        }
    }

}