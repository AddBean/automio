// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.card

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.hive.timer.AlarmEntity
import com.hive.timer.R
import com.hive.timer.utils.AlarmTimerUtils
import com.hive.timer.widget.TimeIconView
import com.hive.utils.GlobalApp
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.widgets.UIRoundCornerLinearLayout
import java.util.Date

class AlarmItemView(context: Context) : ListRecyclerItemView(context), View.OnClickListener {

    private val layout = LayoutInflater.from(context).inflate(R.layout.alarm_item_view, this)

    private var dataItem: AlarmEntity.AlarmTime? = null

    private val cardRoot = layout.findViewById<UIRoundCornerLinearLayout>(R.id.cardRoot)
    private val ivStatus = layout.findViewById<ImageView>(R.id.ivStatus)
    private val ivDelete = layout.findViewById<View>(R.id.ivDelete)
    private val timeIcon = layout.findViewById<TimeIconView>(R.id.timeIcon)
    private val tvInfo = layout.findViewById<TextView>(R.id.tvInfo)

    override fun bindData(data: Any?) {
        dataItem = data as AlarmEntity.AlarmTime
        ivStatus.setOnClickListener(this@AlarmItemView)
        ivDelete?.setOnClickListener(this@AlarmItemView)
        timeIcon?.setTime(Date(dataItem!!.triggerAtTime), false)
        tvInfo.text = AlarmTimerUtils.dateFormatHHMMSS(
            Date(dataItem!!.triggerAtTime)
        )
        updateUi()
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.ivStatus -> {
                dataItem?.enable = dataItem?.enable == false
                updateUi()
            }

            R.id.ivDelete -> {
                postEvent("delete", dataItem)
            }
        }
    }

    private fun updateUi() {
        val enabled = dataItem?.enable == true
        // Match script-design: enabled = highlight, disabled = dim + lighter bg/border
        cardRoot.alpha = if (enabled) 1.0f else 0.6f
        cardRoot.setBackgroundColor(if (enabled) 0x08FFFFFF.toInt() else 0x03FFFFFF.toInt())
        cardRoot.setBorderColor(if (enabled) 0x0FFFFFFF.toInt() else 0x08FFFFFF.toInt())

        ivStatus.setImageResource(
            if (enabled) com.hive.i8n.R.drawable.ic_toggle_right
            else com.hive.i8n.R.drawable.ic_toggle_left
        )
        ivStatus.setColorFilter(
            if (enabled) GlobalApp.getColor(com.hive.i8n.R.color.colorGreen)
            else GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary)
        )
    }
}
