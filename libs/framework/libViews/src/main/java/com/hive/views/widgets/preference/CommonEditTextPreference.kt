// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import com.hive.views.R
import com.takisoft.preferencex.EditTextPreference

/**
 *
 * @author jiadou
 */
class CommonEditTextPreference(context: Context?, attrs: AttributeSet?) : EditTextPreference(context, attrs) {

    private var edit_text: TextView? = null
    private var tv_msg: TextView? = null
    private var tv_title: TextView? = null

    init {
        layoutResource = R.layout.pref_default_edit_text
        dialogLayoutResource= R.layout.pref_default_dialog
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        tv_title = holder?.findViewById(R.id.tv_title) as TextView?
        tv_msg = holder?.findViewById(R.id.tv_msg) as TextView?
        edit_text = holder?.findViewById(R.id.edit_text) as TextView?
        tv_title?.text = title
        tv_msg?.text = dialogMessage
        edit_text?.text = text
    }


    override fun onPrepareForRemoval() {
        super.onPrepareForRemoval()
    }
}