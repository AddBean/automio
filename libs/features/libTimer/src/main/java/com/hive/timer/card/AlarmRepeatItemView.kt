// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.card

import android.content.Context
import android.view.LayoutInflater
import com.hive.timer.R
import com.hive.utils.utils.StringUtils
import com.hive.views.list_view.ListRecyclerItemView
import java.util.Date

class AlarmRepeatItemView(context: Context) : ListRecyclerItemView(context) {

    private val layout = LayoutInflater.from(context).inflate(R.layout.alarm_repeat_item_view, this)
    private val timeIcon = layout.findViewById<com.hive.timer.widget.TimeIconView>(R.id.timeIcon)
    private val tvInfo = layout.findViewById<android.widget.TextView>(R.id.tvInfo)

    override fun bindData(data: Any?) {
        val time = data as Long
        tvInfo.text = StringUtils.dateFormat(
            Date(time),
            "HH:mm:ss"
        )
        timeIcon?.setTime(Date(time), false)
    }
}