// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.engineer

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.hive.base.BaseActivity
import com.hive.base.R
import com.hive.utils.utils.IntentUtils
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @date 2021/12/24
 */
class DialogLoggerInput : BaseActivity() {
    private var loggerView: LoggerView? = null
    private var editText: EditText? = null
    private var ivClose: View? = null
    private var tvSubmit: TextView? = null

    override fun doOnCreate() {
        loggerView = LoggerView.getInstance()
        editText= findViewById(R.id.editText)
        ivClose = findViewById(R.id.ivClose)
        tvSubmit = findViewById(R.id.tvSubmit)

        editText?.setText(intent.getStringExtra("word") ?: "")
        ivClose?.setOnClickListener { finish() }
        tvSubmit?.setOnClickListener {
            val word = editText?.text?.toString() ?: ""
            if (word.length > 40) {
                CommonToast.show(getString(com.hive.i8n.R.string.base_max_chars))
                return@setOnClickListener
            }
            loggerView?.filterText(word)
            finish()
        }
        LoggerView.detachFromWindow()
    }

    override fun onDestroy() {
        super.onDestroy()
        loggerView?.attachToWindow(this)
    }

    override fun getLayoutId() = R.layout.activity_logger_input

    companion object {
        @JvmStatic
        fun start(context: Context, word: String?) {
            IntentUtils.safeStartActivity(
                context,
                Intent(context, DialogLoggerInput::class.java).apply {
                    putExtra("word", word)
                })
        }
    }
}