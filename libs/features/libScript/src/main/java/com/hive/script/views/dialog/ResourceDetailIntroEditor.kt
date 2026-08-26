// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.text.InputType

object ResourceDetailIntroEditor {

    fun show(
        context: Context,
        current: String,
        onSave: (String) -> Unit
    ) {
        DialogInputMessage(
            context,
            title = context.getString(com.hive.i8n.R.string.rp_detail_intro_edit_title),
            hint = context.getString(com.hive.i8n.R.string.rp_detail_intro_input_hint),
            txtHold = current,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            checkInputFun = null
        ) { _, input ->
            onSave(input.trim())
        }.setMultiLine().show()
    }
}
