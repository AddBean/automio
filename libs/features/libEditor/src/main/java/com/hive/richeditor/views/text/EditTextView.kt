// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.views.text

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2021/10/21
 */
class EditTextView(context: Context, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatEditText(context, attrs) {

    var onTextChangeListener: (() -> Unit)? = null

    init {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                onTextChangeListener?.invoke()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onTextChangeListener?.invoke()
            }

            override fun afterTextChanged(s: Editable?) {
                onTextChangeListener?.invoke()
            }
        })
    }

    fun setContent(content: String?) {
        setText(content)
    }

    fun getContent(): String? {
        return text?.toString()
    }


}