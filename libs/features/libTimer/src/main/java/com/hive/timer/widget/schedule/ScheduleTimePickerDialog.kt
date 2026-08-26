// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget.schedule

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.hive.timer.R
import com.hive.timer.widget.TimePickerView
import java.util.Calendar

class ScheduleTimePickerDialog(context: Context) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar) {

    var onTimeSelectedListener: OnTimeSelectedListener? = null

    private var timePickerView: TimePickerView? = null
    private var btnCancel: View? = null
    private var btnOk: View? = null
    private var mask: View? = null

    init {
        setContentView(R.layout.dialog_schedule_time_picker)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window?.setGravity(Gravity.BOTTOM)
        window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }

        mask = findViewById(R.id.mask)
        timePickerView = findViewById(R.id.timePickerView)
        btnCancel = findViewById(R.id.btn_cancel)
        btnOk = findViewById(R.id.btn_ok)

        val sheet = findViewById<View>(R.id.sheet)
        if (sheet != null) {
            val basePaddingBottom = sheet.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(sheet) { v, insets ->
                val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                v.setPadding(
                    v.paddingLeft,
                    v.paddingTop,
                    v.paddingRight,
                    basePaddingBottom + navBottom
                )
                insets
            }
            ViewCompat.requestApplyInsets(sheet)
        }

        findViewById<TextView>(R.id.tv_time_title)?.setText(com.hive.i8n.R.string.time_selector_setting_title)
        findViewById<TextView>(R.id.btn_cancel)?.setText(com.hive.i8n.R.string.cancel)
        findViewById<TextView>(R.id.btn_ok)?.setText(com.hive.i8n.R.string.pref_confirm)

        val now = Calendar.getInstance()
        timePickerView?.setSelectedTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), 0)

        mask?.setOnClickListener {
            dismiss()
            onTimeSelectedListener?.onTimeDismiss()
        }
        btnCancel?.setOnClickListener {
            dismiss()
            onTimeSelectedListener?.onTimeDismiss()
        }
        btnOk?.setOnClickListener {
            val (hour, minute, second) = timePickerView?.getSelectedTime() ?: Triple(0, 0, 0)
            dismiss()
            onTimeSelectedListener?.onTimeSelected(hour, minute, second)
        }
        setOnCancelListener {
            onTimeSelectedListener?.onTimeDismiss()
        }
    }

    interface OnTimeSelectedListener {
        fun onTimeSelected(hour: Int, minus: Int, secs: Int)
        fun onTimeDismiss()
    }
}
